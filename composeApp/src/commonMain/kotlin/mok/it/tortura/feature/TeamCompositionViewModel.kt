package mok.it.tortura.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone.Companion.currentSystemDefault
import kotlinx.datetime.toLocalDateTime
import mok.it.tortura.data.supabase.mapper.toInsertDto
import mok.it.tortura.data.supabase.mapper.toModel
import mok.it.tortura.data.supabase.mapper.toUpdateDto
import mok.it.tortura.data.supabase.repository.TorturaSupabaseRepositories
import mok.it.tortura.model.Team
import mok.it.tortura.model.TeamAssignment

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
    val batkabankAvailableYears: List<Int> = emptyList(),
    val batkabankSelectedYear: Int? = null,
    val batkabankAvailableCamps: List<CampSearchResultDto> = emptyList(),
    val batkabankSelectedCampId: String? = null,
    val isBatkabankLoading: Boolean = false,
)

interface TeamCompositionDataSource {
    suspend fun loadTeamComposition(gameId: Long): TeamCompositionSnapshot
    suspend fun saveTeamComposition(
        gameId: Long,
        draft: TeamCompositionDraft,
    ): TeamCompositionSnapshot
}

class SupabaseTeamCompositionDataSource(
    private val repositories: TorturaSupabaseRepositories = TorturaSupabaseRepositories(),
) : TeamCompositionDataSource {
    override suspend fun loadTeamComposition(gameId: Long): TeamCompositionSnapshot {
        val assignment = repositories.teamAssignments
            .getByGameId(gameId)
            .maxByOrNull { it.id ?: Long.MIN_VALUE }
            ?.toModel()
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
        )
    }

    override suspend fun saveTeamComposition(
        gameId: Long,
        draft: TeamCompositionDraft,
    ): TeamCompositionSnapshot {
        val existingAssignment = repositories.teamAssignments
            .getByGameId(gameId)
            .maxByOrNull { it.id ?: Long.MIN_VALUE }
            ?.toModel()

        val assignment = if (existingAssignment?.id != null) {
            repositories.teamAssignments.update(
                id = existingAssignment.id,
                teamAssignment = TeamAssignment(
                    id = existingAssignment.id,
                    baseTeamCounter = draft.baseTeamCounter,
                    gameId = gameId,
                ).toUpdateDto(),
            ).toModel()
        } else {
            repositories.teamAssignments.create(
                TeamAssignment(
                    baseTeamCounter = draft.baseTeamCounter,
                    gameId = gameId,
                ).toInsertDto(),
            ).toModel()
        }

        val assignmentId =
            assignment.id ?: error("A csapatbeosztás mentése nem adott vissza azonosítót.")
        val existingTeams = repositories.teams.getByTeamAssignmentId(assignmentId)

        existingTeams.forEach { teamDto ->
            val teamId = teamDto.id ?: return@forEach
            repositories.students.getByTeamId(teamId).forEach { studentDto ->
                val studentId = studentDto.id ?: return@forEach
                repositories.students.delete(studentId)
            }
            repositories.teams.delete(teamId)
        }

        draft.teams.forEach { teamDraft ->
            val team = repositories.teams.create(
                Team(
                    name = teamDraft.name.trim(),
                    teamAssignmentId = assignmentId,
                ).toInsertDto(),
            ).toModel()

            val teamId = team.id ?: error("A csapat mentése nem adott vissza azonosítót.")
            teamDraft.toStudents().forEach { student ->
                repositories.students.create(student.copy(teamId = teamId).toInsertDto())
            }
        }

        return loadTeamComposition(gameId)
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
    private val batkabankImportMapper: BatkabankRosterImportMapper = BatkabankRosterImportMapper(),
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
        runAction(loading = true) {
            val snapshot = dataSource.loadTeamComposition(activeGameId)
            val draft = snapshot.toDraft()
            val availableYears = if (batkabankSource.isConfigured) {
                batkabankSource.getAvailableYears().getOrElse { throw it }.sortedDescending()
            } else {
                emptyList()
            }

            val currentYear = Clock.System.now().toLocalDateTime(currentSystemDefault()).year
            val selectedYear = if (currentYear in availableYears) currentYear else null

            _uiState.update {
                it.copy(
                    baseTeamCounter = draft.baseTeamCounter?.toString().orEmpty(),
                    teams = draft.teams,
                    persistedSnapshot = snapshot,
                    batkabankAvailableYears = availableYears,
                    batkabankSelectedYear = selectedYear,
                    message = "Csapatbeosztás betöltve",
                )
            }

            if (selectedYear != null) {
                selectBatkabankYear(selectedYear)
            }
        }
    }

    fun onBaseTeamCounterChange(value: String) {
        _uiState.update { it.copy(baseTeamCounter = value, message = null, errorMessage = null) }
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

        applyImport(
            ImportedRosterPayload(
                sourceLabel = "Beillesztett szöveg",
                content = text,
                format = TeamCompositionImportFormat.DELIMITED_TEXT,
            ),
        )
    }

    fun readClipboard() {
        runAction {
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
        runAction {
            val payload = platformBridge.pickRosterFile().getOrElse { throw it }
            applyImport(payload)
        }
    }

    fun selectBatkabankYear(year: Int?) {
        if (year == null) {
            _uiState.update {
                it.copy(
                    batkabankSelectedYear = null,
                    batkabankAvailableCamps = emptyList(),
                    batkabankSelectedCampId = null,
                    message = null,
                    errorMessage = null,
                )
            }
            return
        }

        runAction(batkabankLoading = true) {
            val camps = batkabankSource.getCamps(year).getOrElse { throw it }
            _uiState.update {
                it.copy(
                    batkabankSelectedYear = year,
                    batkabankAvailableCamps = camps,
                    batkabankSelectedCampId = null,
                    message = if (camps.isEmpty()) {
                        "Nincs Batkabank tábor a(z) $year. évben."
                    } else {
                        "${camps.size} Batkabank tábor érhető el a(z) $year. évben."
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
            val roster = batkabankSource.getCampRoster(
                campId = camp.id,
                assignmentId = assignment.id,
            ).getOrElse { throw it }

            applyImport(
                batkabankImportMapper.import(
                    sourceLabel = "Batkabank: ${camp.name} / ${assignment.name}",
                    roster = roster,
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
        val validationError = validate(uiState.value)
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        val draft = TeamCompositionDraft(
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
