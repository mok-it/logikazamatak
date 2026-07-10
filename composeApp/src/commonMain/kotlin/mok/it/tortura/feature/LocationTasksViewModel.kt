package mok.it.tortura.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mok.it.tortura.data.supabase.dto.TasksLedgerInsertDto
import mok.it.tortura.data.supabase.mapper.toModel
import mok.it.tortura.data.supabase.repository.TorturaSupabaseRepositories
import mok.it.tortura.model.Task
import mok.it.tortura.model.Team

data class TaskSubmissionResult(
    val isSuccess: Boolean,
    val submittedAnswer: String,
)

data class LocationTasksUiState(
    val isLoading: Boolean = false,
    val tasks: List<Task> = emptyList(),
    val teams: List<Team> = emptyList(),
    val selectedTeamId: Long? = null,
    val answerDrafts: Map<Long, String> = emptyMap(),
    val latestSubmissionByTaskId: Map<Long, TaskSubmissionResult> = emptyMap(),
    val message: String? = null,
    val errorMessage: String? = null,
)

data class LocationTasksScreenData(
    val tasks: List<Task>,
    val teams: List<Team>,
)

interface LocationTasksDataSource {
    suspend fun load(
        gameId: Long,
        locationId: Long,
    ): LocationTasksScreenData

    suspend fun recordAttempt(
        teamId: Long,
        taskId: Long,
        isSuccess: Boolean,
    )
}

class SupabaseLocationTasksDataSource(
    private val repositories: TorturaSupabaseRepositories = TorturaSupabaseRepositories(),
) : LocationTasksDataSource {
    override suspend fun load(
        gameId: Long,
        locationId: Long,
    ): LocationTasksScreenData {
        val tasks = repositories.tasks
            .getByLocationId(locationId)
            .map { it.toModel() }
            .sortedWith(
                compareBy<Task> { it.isMiniBoss != true }
                    .thenBy { it.text.lowercase() },
            )
        val teams = repositories.teamAssignments.getByGameId(gameId)
            .mapNotNull { it.id }
            .flatMap { repositories.teams.getByTeamAssignmentId(it) }
            .map { it.toModel() }
            .sortedBy { (it.name ?: "zzz").lowercase() }

        return LocationTasksScreenData(
            tasks = tasks,
            teams = teams,
        )
    }

    override suspend fun recordAttempt(
        teamId: Long,
        taskId: Long,
        isSuccess: Boolean,
    ) {
        repositories.tasksLedger.create(
            TasksLedgerInsertDto(
                taskId = taskId,
                teamId = teamId,
                isSuccess = isSuccess,
            ),
        )
    }
}

class LocationTasksViewModel(
    private val activeGameId: Long,
    private val locationId: Long,
    private val dataSource: LocationTasksDataSource = SupabaseLocationTasksDataSource(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationTasksUiState())
    val uiState: StateFlow<LocationTasksUiState> = _uiState

    fun load() {
        runRepositoryAction {
            val data = dataSource.load(activeGameId, locationId)
            val selectedTeamId = resolveSelectedTeamId(data.teams)
            _uiState.update { current ->
                current.copy(
                    tasks = data.tasks,
                    teams = data.teams,
                    selectedTeamId = selectedTeamId,
                    message = null,
                )
            }
        }
    }

    fun selectTeam(teamId: Long) {
        _uiState.update {
            it.copy(
                selectedTeamId = teamId,
                message = null,
                errorMessage = null,
            )
        }
    }

    fun onAnswerChange(
        taskId: Long,
        value: String,
    ) {
        _uiState.update {
            it.copy(
                answerDrafts = it.answerDrafts + (taskId to value),
                message = null,
                errorMessage = null,
            )
        }
    }

    fun submitAnswer(taskId: Long) {
        val selectedTeamId = _uiState.value.selectedTeamId
        if (selectedTeamId == null) {
            _uiState.update { it.copy(errorMessage = "Előbb válassz csapatot") }
            return
        }

        val task = _uiState.value.tasks.firstOrNull { it.id == taskId }
        if (task?.id == null) {
            _uiState.update { it.copy(errorMessage = "A kiválasztott feladat nem található") }
            return
        }

        val answer = _uiState.value.answerDrafts[taskId]?.trim().orEmpty()
        if (answer.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Adj meg egy választ a beküldéshez") }
            return
        }

        val isSuccess = normalizeAnswer(answer) == normalizeAnswer(task.solution)

        runRepositoryAction {
            dataSource.recordAttempt(
                teamId = selectedTeamId,
                taskId = task.id,
                isSuccess = isSuccess,
            )
            _uiState.update {
                it.copy(
                    answerDrafts = it.answerDrafts - taskId,
                    latestSubmissionByTaskId = it.latestSubmissionByTaskId + (
                        taskId to TaskSubmissionResult(
                            isSuccess = isSuccess,
                            submittedAnswer = answer,
                        )
                    ),
                    message = if (isSuccess) "Helyes válasz rögzítve" else "Sikertelen próbálkozás rögzítve",
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(message = null, errorMessage = null) }
    }

    private fun resolveSelectedTeamId(teams: List<Team>): Long? {
        val currentSelection = _uiState.value.selectedTeamId
        return currentSelection?.takeIf { selectedId -> teams.any { it.id == selectedId } }
            ?: teams.firstOrNull()?.id
    }

    private fun runRepositoryAction(action: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null, errorMessage = null) }
            try {
                action()
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(errorMessage = exception.message ?: "Supabase művelet sikertelen")
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}

private fun normalizeAnswer(value: String): String = value
    .trim()
    .lowercase()
    .replace(Regex("\\s+"), " ")
