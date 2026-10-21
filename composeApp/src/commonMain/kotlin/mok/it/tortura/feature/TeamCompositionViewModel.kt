package mok.it.tortura.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.postgrest.postgrest
import kotlin.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone.Companion.currentSystemDefault
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import mok.it.tortura.SupabaseClient
import mok.it.tortura.data.supabase.mapper.toModel
import mok.it.tortura.data.supabase.repository.TorturaSupabaseRepositories
import mok.it.tortura.ui.components.sanitizeNumericInput

data class TeamCompositionUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val baseTeamCounter: String = "",
    val teams: List<TeamDraft> = emptyList(),
    val persistedSnapshot: TeamCompositionSnapshot? = null,
    val importText: String = "",
    val importPreview: TeamCompositionImportPreview? = null,
    val message: String? = null,
    val errorMessage: String? = null,
    val clipboardSupported: Boolean = false,
    val fileImportSupported: Boolean = false,
    val batkabankEnabled: Boolean = false,
    val batkabankYearInput: String = "",
    val batkabankAvailableCamps: List<CampSearchResultDto> = emptyList(),
    val batkabankSelectedCampId: String? = null,
    val isBatkabankLoading: Boolean = false,
)

interface TeamCompositionDataSource {
    suspend fun loadTeamComposition(
        gameId: Long,
        assignmentId: Long? = null,
    ): TeamCompositionSnapshot
    suspend fun saveTeamComposition(
        gameId: Long,
        draft: TeamCompositionDraft,
    ): TeamCompositionSnapshot
}

@Serializable
private data class SaveTeamCompositionParams(
    val p_game_id: Long,
    val p_base_team_counter: Long?,
    val p_teams: List<TeamDraft>,
    val p_assignment_id: Long? = null,
)

class SupabaseTeamCompositionDataSource(
    private val repositories: TorturaSupabaseRepositories = TorturaSupabaseRepositories(),
) : TeamCompositionDataSource {
    override suspend fun loadTeamComposition(
        gameId: Long,
        assignmentId: Long?,
    ): TeamCompositionSnapshot {
        val assignments = repositories.teamAssignments
            .getByGameId(gameId)
            .map { it.toModel() }
            .sortedByDescending { it.id ?: Long.MIN_VALUE }
        val assignment = if (assignmentId == null) {
            assignments.firstOrNull()
        } else {
            requireNotNull(assignments.firstOrNull { it.id == assignmentId }) {
                "A beosztás nem ehhez a játékhoz tartozik."
            }
        }
            ?: return TeamCompositionSnapshot()

        val teams = repositories.teams.getByTeamAssignmentId(
            assignment.id ?: return TeamCompositionSnapshot(assignment),
        )
            .map { teamDto ->
                val students =
                    (teamDto.id?.let { repositories.students.getByTeamId(it) } ?: emptyList())
                        .map { it.toModel() }
                teamDto.toModel(students = students)
            }

        return TeamCompositionSnapshot(
            assignment = assignment,
            teams = teams,
            assignments = assignments,
        )
    }

    override suspend fun saveTeamComposition(
        gameId: Long,
        draft: TeamCompositionDraft,
    ): TeamCompositionSnapshot {
        SupabaseClient.client.postgrest.rpc(
            "save_team_composition",
            Json.encodeToJsonElement(
                SaveTeamCompositionParams(gameId, draft.baseTeamCounter, draft.teams, draft.assignmentId),
            ).jsonObject,
        )

        return loadTeamComposition(gameId, draft.assignmentId)
    }
}

class TeamCompositionViewModel(
    private val activeGameId: Long,
    private val platformBridge: TeamCompositionPlatformBridge = teamCompositionPlatformBridge(),
    private val dataSource: TeamCompositionDataSource = SupabaseTeamCompositionDataSource(),
    private val importCoordinator: TeamCompositionImportCoordinator =
        TeamCompositionImportCoordinator(),
    private val batkabankSource: BatkabankTeamCompositionSource =
        FirebaseBatkabankTeamCompositionSource(),
    private val batkabankImportMapper: BatkabankAssignmentImportMapper = BatkabankAssignmentImportMapper(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TeamCompositionUiState(
            clipboardSupported = platformBridge.supportsClipboardRead,
            fileImportSupported = platformBridge.supportsFileImport,
            batkabankEnabled = batkabankSource.isConfigured,
        ),
    )
    val uiState: StateFlow<TeamCompositionUiState> = _uiState

    fun load() {
        loadAssignment(null)
    }

    fun selectAssignment(assignmentId: Long) {
        loadAssignment(assignmentId)
    }

    private fun loadAssignment(assignmentId: Long?) {
        runAction(loading = true) {
            val snapshot = dataSource.loadTeamComposition(activeGameId, assignmentId)
            val draft = snapshot.toDraft()
            val currentYear = Clock.System.now().toLocalDateTime(currentSystemDefault()).year

            _uiState.update {
                it.copy(
                    baseTeamCounter = draft.baseTeamCounter?.toString().orEmpty(),
                    teams = draft.teams,
                    persistedSnapshot = snapshot,
                    importPreview = null,
                    importText = "",
                    batkabankYearInput = currentYear.toString(),
                    message = "Csapatbeosztás betöltve",
                )
            }

            if (batkabankSource.isConfigured) {
                val camps = batkabankSource.getImportableCamps(currentYear).getOrElse { throw it }
                _uiState.update { it.copy(batkabankAvailableCamps = camps) }
            }
        }
    }

    fun onBaseTeamCounterChange(value: String) {
        _uiState.update { it.copy(baseTeamCounter = sanitizeNumericInput(value), message = null, errorMessage = null) }
    }

    fun onImportTextChange(value: String) {
        _uiState.update { it.copy(importText = value, message = null, errorMessage = null) }
    }

    fun importFromText() {
        val text = uiState.value.importText.trim()
        if (text.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = "Illessz be CSV/TSV tartalmat vagy olvasd be a vágólapról.")
            }
            return
        }

        runAction {
            applyImport(
                ImportedRosterPayload(
                    sourceLabel = "Beillesztett szöveg",
                    content = text,
                    format = TeamCompositionImportFormat.DELIMITED_TEXT,
                ),
            )
        }
    }

    fun readClipboard() {
        runAction(loading = true) {
            val clipboardText = platformBridge.readClipboardText().getOrElse { throw it }
            _uiState.update { it.copy(importText = clipboardText) }
            applyImport(
                ImportedRosterPayload(
                    sourceLabel = "Vágólap",
                    content = clipboardText,
                    format = TeamCompositionImportFormat.DELIMITED_TEXT,
                ),
            )
        }
    }

    fun importFromFile() {
        runAction(loading = true) {
            val payload = platformBridge.pickRosterFile().getOrElse { throw it }
            applyImport(payload)
        }
    }

    fun onBatkabankYearInputChange(value: String) {
        _uiState.update {
            it.copy(
                batkabankYearInput = sanitizeNumericInput(value),
                message = null,
                errorMessage = null,
            )
        }
    }

    fun refreshBatkabankCamps() {
        val year = uiState.value.batkabankYearInput.trim().toIntOrNull()
        if (uiState.value.batkabankYearInput.isNotBlank() && year == null) {
            _uiState.update {
                it.copy(errorMessage = "A Batkabank év mezőben adj meg egy érvényes évet.")
            }
            return
        }

        loadBatkabankCamps(year)
    }

    private fun loadBatkabankCamps(year: Int?) {
        runAction(batkabankLoading = true) {
            val camps = batkabankSource.getImportableCamps(year).getOrElse { throw it }
            _uiState.update {
                it.copy(
                    batkabankAvailableCamps = camps,
                    batkabankSelectedCampId = null,
                    message = if (camps.isEmpty() && year != null) {
                        "Nincs importálható Batkabank tábor a(z) $year. évben."
                    } else if (camps.isEmpty()) {
                        "Nincs importálható Batkabank tábor az alapértelmezett évben."
                    } else {
                        "${camps.size} importálható Batkabank tábor érhető el."
                    },
                )
            }
        }
    }

    fun selectBatkabankCamp(campId: String?) {
        _uiState.update {
            it.copy(
                batkabankSelectedCampId = campId,
                message = null,
                errorMessage = null,
            )
        }
    }

    fun importFromBatkabank(
        camp: CampSearchResultDto,
        assignment: CampSearchAssignmentDto,
    ) {
        runAction(batkabankLoading = true) {
            val campAssignment = batkabankSource.getCampAssignment(
                campId = camp.id,
                assignmentId = assignment.id,
            ).getOrElse { throw it }

            applyImport(
                batkabankImportMapper.import(
                    sourceLabel = "Batkabank: ${camp.name} / ${assignment.name}",
                    assignment = campAssignment,
                ),
            )
        }
    }

    fun addTeam() {
        _uiState.update {
            it.copy(
                teams = it.teams + TeamDraft(students = listOf(StudentDraft())),
                message = null,
                errorMessage = null,
            )
        }
    }

    fun removeTeam(index: Int) {
        _uiState.update { state ->
            state.copy(
                teams = state.teams.filterIndexed { currentIndex, _ -> currentIndex != index },
                message = null,
                errorMessage = null,
            )
        }
    }

    fun updateTeamName(
        index: Int,
        value: String,
    ) = updateTeam(index) { it.copy(name = value) }

    fun updateTeamGroup(
        index: Int,
        value: String,
    ) = updateTeam(index) { it.copy(group = value) }

    fun addStudent(teamIndex: Int) {
        updateTeam(teamIndex) { team ->
            team.copy(students = team.students + StudentDraft())
        }
    }

    fun removeStudent(
        teamIndex: Int,
        studentIndex: Int,
    ) {
        updateTeam(teamIndex) { team ->
            team.copy(
                students = team.students.filterIndexed { currentIndex, _ ->
                    currentIndex !=
                        studentIndex
                },
            )
        }
    }

    fun updateStudentName(
        teamIndex: Int,
        studentIndex: Int,
        value: String,
    ) {
        updateTeam(teamIndex) { team ->
            team.copy(
                students = team.students.mapIndexed { currentIndex, student ->
                    if (currentIndex == studentIndex) student.copy(name = value) else student
                },
            )
        }
    }

    fun save() {
        if (uiState.value.isLoading || uiState.value.isSaving || uiState.value.isBatkabankLoading) return
        val validationError = validate(uiState.value)
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        val draft = TeamCompositionDraft(
            assignmentId = uiState.value.persistedSnapshot?.assignment?.id,
            baseTeamCounter = uiState.value.baseTeamCounter.trim().toLongOrNull(),
            teams = uiState.value.teams.map { team ->
                team.copy(
                    name = team.name.trim(),
                    group = team.group.trim(),
                    students = team.students.map { it.copy(name = it.name.trim()) },
                )
            },
        )

        runAction(saving = true) {
            val snapshot = dataSource.saveTeamComposition(activeGameId, draft)
            val savedDraft = snapshot.toDraft()
            _uiState.update {
                it.copy(
                    baseTeamCounter = savedDraft.baseTeamCounter?.toString().orEmpty(),
                    teams = savedDraft.teams,
                    persistedSnapshot = snapshot,
                    message = "Csapatbeosztás elmentve",
                    errorMessage = null,
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(message = null, errorMessage = null) }
    }

    private fun applyImport(payload: ImportedRosterPayload) {
        val result = importCoordinator.import(payload)
        applyImport(
            result = result,
            importText = payload.content,
        )
    }

    private fun applyImport(
        result: TeamCompositionImportResult,
        importText: String? = null,
    ) {
        if (result.preview.rowErrors.isNotEmpty() || result.draft.teams.isEmpty()) {
            _uiState.update {
                it.copy(
                    importPreview = result.preview,
                    errorMessage = if (result.draft.teams.isEmpty()) {
                        "Az import nem tartalmaz érvényes csapatokat."
                    } else {
                        "Az import hibás sorokat tartalmaz. Javítsd a forrást, majd importáld újra."
                    },
                    message = null,
                )
            }
            return
        }
        _uiState.update {
            it.copy(
                baseTeamCounter = result.draft.baseTeamCounter?.toString().orEmpty(),
                teams = result.draft.teams,
                importPreview = result.preview,
                importText = importText ?: it.importText,
                message = "Import feldolgozva: ${result.preview.sourceLabel}",
                errorMessage = null,
            )
        }
    }

    private fun validate(state: TeamCompositionUiState): String? {
        val baseTeamCounter = state.baseTeamCounter.trim().toLongOrNull()
        if (baseTeamCounter == null || baseTeamCounter < 1) {
            return "Az alap csapatszám legyen pozitív egész szám."
        }
        if (state.teams.isEmpty()) {
            return "Adj meg legalább egy csapatot."
        }
        state.teams.forEachIndexed { teamIndex, team ->
            if (team.name.isBlank()) return "A(z) ${teamIndex + 1}. csapat neve hiányzik."
            if (team.group.isBlank()) {
                return "A(z) ${
                    team.name.ifBlank {
                        "${teamIndex + 1}. csapat"
                    }
                } csoportja hiányzik."
            }
            if (team.students.isEmpty()) return "A(z) ${team.name} csapatban nincs tanuló."
            team.students.forEachIndexed { studentIndex, student ->
                if (student.name.isBlank()) {
                    return "A(z) ${team.name} csapat ${studentIndex + 1}. tanulójának neve hiányzik."
                }
            }
        }
        return null
    }

    private fun updateTeam(
        index: Int,
        transform: (TeamDraft) -> TeamDraft,
    ) {
        _uiState.update { state ->
            state.copy(
                teams = state.teams.mapIndexed { currentIndex, team ->
                    if (currentIndex == index) transform(team) else team
                },
                message = null,
                errorMessage = null,
            )
        }
    }

    private fun runAction(
        loading: Boolean = false,
        saving: Boolean = false,
        batkabankLoading: Boolean = false,
        action: suspend () -> Unit,
    ) {
        if (uiState.value.isLoading || uiState.value.isSaving || uiState.value.isBatkabankLoading) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = loading,
                    isSaving = saving,
                    isBatkabankLoading = batkabankLoading,
                    message = null,
                    errorMessage = null,
                )
            }
            try {
                action()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(errorMessage = exception.message ?: "A művelet nem sikerült.")
                }
            } finally {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isSaving = false,
                        isBatkabankLoading = false,
                    )
                }
            }
        }
    }
}
