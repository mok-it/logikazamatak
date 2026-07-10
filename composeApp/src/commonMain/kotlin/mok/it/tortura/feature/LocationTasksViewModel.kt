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
import mok.it.tortura.model.Item
import mok.it.tortura.model.ShopEntry
import mok.it.tortura.model.Task
import mok.it.tortura.model.TaskEvent
import mok.it.tortura.model.Team
import mok.it.tortura.model.TeamProgressSummary
import mok.it.tortura.model.TeamProgressSummaryCalculator

data class TaskSubmissionResult(
    val isSuccess: Boolean,
    val submittedAnswer: String,
)

data class LocationTasksUiState(
    val isLoading: Boolean = false,
    val tasks: List<Task> = emptyList(),
    val teams: List<Team> = emptyList(),
    val selectedTeamId: Long? = null,
    val selectedTeamProgress: TeamProgressSummary? = null,
    val answerDrafts: Map<Long, String> = emptyMap(),
    val latestSubmissionByTaskId: Map<Long, TaskSubmissionResult> = emptyMap(),
    val message: String? = null,
    val errorMessage: String? = null,
)

data class LocationTasksScreenData(
    val tasks: List<Task>,
    val teams: List<Team>,
    val allGameTasks: List<Task>,
    val allItems: List<Item>,
)

interface LocationTasksDataSource {
    suspend fun load(
        gameId: Long,
        locationId: Long,
    ): LocationTasksScreenData

    suspend fun getTaskEvents(teamId: Long): List<TaskEvent>

    suspend fun getPurchases(teamId: Long): List<ShopEntry>

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
            .map { teamDto ->
                val teamId = teamDto.id
                val students = if (teamId == null) {
                    emptyList()
                } else {
                    repositories.students.getByTeamId(teamId).map { it.toModel() }
                }
                teamDto.toModel(students = students)
            }
            .sortedBy { (it.name ?: "zzz").lowercase() }
        val allGameTasks = repositories.tasks
            .getByGameId(gameId)
            .map { it.toModel() }
        val allItems = repositories.items
            .getByGameId(gameId)
            .map { it.toModel() }

        return LocationTasksScreenData(
            tasks = tasks,
            teams = teams,
            allGameTasks = allGameTasks,
            allItems = allItems,
        )
    }

    override suspend fun getTaskEvents(teamId: Long): List<TaskEvent> =
        repositories.tasksLedger.getByTeamId(teamId).map { it.toModel() }

    override suspend fun getPurchases(teamId: Long): List<ShopEntry> =
        repositories.shop.getByTeamId(teamId).map { it.toModel() }

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
    private var allGameTasks: List<Task> = emptyList()
    private var allItems: List<Item> = emptyList()
    private var selectedTeamTaskEvents: List<TaskEvent> = emptyList()
    private var selectedTeamPurchases: List<ShopEntry> = emptyList()

    fun load() {
        runRepositoryAction {
            val data = dataSource.load(activeGameId, locationId)
            allGameTasks = data.allGameTasks
            allItems = data.allItems
            val selectedTeamId = resolveSelectedTeamId(data.teams)
            refreshSelectedTeamState(
                teams = data.teams,
                teamId = selectedTeamId,
            )
            _uiState.update { current ->
                current.copy(
                    tasks = data.tasks,
                    teams = data.teams,
                    selectedTeamId = selectedTeamId,
                    selectedTeamProgress = buildTeamProgressSummary(data.teams, selectedTeamId),
                    message = null,
                )
            }
        }
    }

    fun selectTeam(teamId: Long) {
        runRepositoryAction {
            val teams = _uiState.value.teams
            refreshSelectedTeamState(
                teams = teams,
                teamId = teamId,
            )
            _uiState.update {
                it.copy(
                    selectedTeamId = teamId,
                    selectedTeamProgress = buildTeamProgressSummary(teams, teamId),
                    message = null,
                    errorMessage = null,
                )
            }
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
            refreshSelectedTeamState(
                teams = _uiState.value.teams,
                teamId = selectedTeamId,
            )
            _uiState.update {
                it.copy(
                    selectedTeamProgress = buildTeamProgressSummary(it.teams, selectedTeamId),
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

    private suspend fun refreshSelectedTeamState(
        teams: List<Team>,
        teamId: Long?,
    ) {
        if (teamId == null || teams.none { it.id == teamId }) {
            selectedTeamTaskEvents = emptyList()
            selectedTeamPurchases = emptyList()
            return
        }
        selectedTeamTaskEvents = dataSource.getTaskEvents(teamId)
        selectedTeamPurchases = dataSource.getPurchases(teamId)
    }

    private fun buildTeamProgressSummary(
        teams: List<Team>,
        teamId: Long?,
    ): TeamProgressSummary? {
        val team = teams.firstOrNull { it.id == teamId } ?: return null
        return TeamProgressSummaryCalculator.calculate(
            team = team,
            allGameTasks = allGameTasks,
            allItems = allItems,
            taskEvents = selectedTeamTaskEvents,
            purchases = selectedTeamPurchases,
        )
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
