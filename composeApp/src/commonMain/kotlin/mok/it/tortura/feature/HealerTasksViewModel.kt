package mok.it.tortura.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mok.it.tortura.data.supabase.dto.HealingLedgerInsertDto
import mok.it.tortura.data.supabase.mapper.toModel
import mok.it.tortura.data.supabase.repository.TorturaSupabaseRepositories
import mok.it.tortura.model.HealingTask
import mok.it.tortura.model.Team

data class HealingTaskListItem(
    val task: HealingTask,
    val isPreviouslyChosen: Boolean,
)

data class FailedTaskAttempt(
    val ledgerId: Long,
    val taskId: Long,
    val taskText: String,
)

data class HealerTasksUiState(
    val isLoading: Boolean = false,
    val team: Team? = null,
    val healingTasks: List<HealingTaskListItem> = emptyList(),
    val healableFailedTasks: List<FailedTaskAttempt> = emptyList(),
    val selectedHealingTaskId: Long? = null,
    val message: String? = null,
    val errorMessage: String? = null,
)

data class HealerScreenData(
    val team: Team,
    val healingTasks: List<HealingTaskListItem>,
    val healableFailedTasks: List<FailedTaskAttempt>,
)

interface HealerTasksDataSource {
    suspend fun load(teamId: Long): HealerScreenData

    suspend fun completeHealing(
        teamId: Long,
        healingTaskId: Long,
        healedTasksLedgerId: Long,
    )
}

class SupabaseHealerTasksDataSource(
    private val repositories: TorturaSupabaseRepositories = TorturaSupabaseRepositories(),
) : HealerTasksDataSource {

    override suspend fun load(teamId: Long): HealerScreenData {
        val team = repositories.teams.getById(teamId)?.toModel()
            ?: error("A csapat nem található")
        val teamAssignmentId = team.teamAssignmentId ?: error("A csapathoz nincs beosztás")
        val gameId = repositories.teamAssignments.getById(teamAssignmentId)?.gameId
            ?: error("A csapathoz nincs aktív játék")

        val healingHistory = repositories.healingLedger.getByTeamId(teamId)
        val usedHealingTaskIds = healingHistory.mapNotNull { it.healingTaskId }.toSet()
        val healedLedgerIds = healingHistory.mapNotNull { it.healedTasksLedgerId }.toSet()

        val healingTasks = repositories.healingTasks
            .getByGameId(gameId)
            .map { it.toModel() }
            .map { task ->
                HealingTaskListItem(
                    task = task,
                    isPreviouslyChosen = task.id != null && task.id in usedHealingTaskIds,
                )
            }
            .sortedWith(
                compareBy<HealingTaskListItem> { it.isPreviouslyChosen }
                    .thenBy { it.task.text.lowercase() },
            )

        val failedEntries = repositories.tasksLedger.getByTeamId(teamId)
            .filter { it.isSuccess == false && it.id != null && it.id !in healedLedgerIds }

        val taskTexts = failedEntries
            .map { it.taskId }
            .distinct()
            .associateWith { taskId ->
                repositories.tasks.getById(taskId)?.toModel()?.text ?: "Feladat #$taskId"
            }

        val healableFailedTasks = failedEntries
            .mapNotNull { entry ->
                val ledgerId = entry.id ?: return@mapNotNull null
                FailedTaskAttempt(
                    ledgerId = ledgerId,
                    taskId = entry.taskId,
                    taskText = taskTexts[entry.taskId] ?: "Feladat #${entry.taskId}",
                )
            }
            .sortedBy { it.taskText.lowercase() }

        return HealerScreenData(
            team = team,
            healingTasks = healingTasks,
            healableFailedTasks = healableFailedTasks,
        )
    }

    override suspend fun completeHealing(
        teamId: Long,
        healingTaskId: Long,
        healedTasksLedgerId: Long,
    ) {
        val team = repositories.teams.getById(teamId)?.toModel()
            ?: error("A csapat nem található")
        val teamAssignmentId = team.teamAssignmentId ?: error("A csapathoz nincs beosztás")
        val gameId = repositories.teamAssignments.getById(teamAssignmentId)?.gameId
            ?: error("A csapathoz nincs aktív játék")

        val healingTask = repositories.healingTasks.getById(healingTaskId)
            ?: error("A gyógyító feladat nem található")
        if (healingTask.gameId != gameId) {
            error("A gyógyító feladat nem ehhez a játékhoz tartozik")
        }

        val healedTaskEntry = repositories.tasksLedger.getById(healedTasksLedgerId)
            ?: error("A gyógyítandó feladatpróba nem található")
        if (healedTaskEntry.teamId != teamId) {
            error("A gyógyítandó feladat nem ehhez a csapathoz tartozik")
        }
        if (healedTaskEntry.isSuccess != false) {
            error("Csak elbukott feladat gyógyítható")
        }

        val existingHealing = repositories.healingLedger.getByHealedTasksLedgerId(healedTasksLedgerId)
        if (existingHealing.any()) {
            error("Ez a bukott feladat már meg lett gyógyítva")
        }

        repositories.healingLedger.create(
            HealingLedgerInsertDto(
                teamId = teamId,
                healingTaskId = healingTaskId,
                healedTasksLedgerId = healedTasksLedgerId,
            ),
        )
    }
}

class HealerTasksViewModel(
    private val teamId: Long,
    private val dataSource: HealerTasksDataSource = SupabaseHealerTasksDataSource(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(HealerTasksUiState())
    val uiState: StateFlow<HealerTasksUiState> = _uiState

    fun load() {
        runRepositoryAction(
            clearSelection = false,
            action = {
                val data = dataSource.load(teamId)
                _uiState.update { current ->
                    val selectedHealingTaskId =
                        current.selectedHealingTaskId?.takeIf { selectedId ->
                            data.healingTasks.any { it.task.id == selectedId }
                        }
                    current.copy(
                        team = data.team,
                        healingTasks = data.healingTasks,
                        healableFailedTasks = data.healableFailedTasks,
                        selectedHealingTaskId = selectedHealingTaskId,
                        message = if (current.team == null) "Adatok betöltve" else current.message,
                    )
                }
            },
        )
    }

    fun selectHealingTask(healingTaskId: Long) {
        _uiState.update { it.copy(selectedHealingTaskId = healingTaskId, message = null, errorMessage = null) }
    }

    fun completeHealing(healingTaskId: Long, healedTasksLedgerId: Long) {
        runRepositoryAction(
            clearSelection = true,
            action = {
                dataSource.completeHealing(teamId, healingTaskId, healedTasksLedgerId)
                val data = dataSource.load(teamId)
                _uiState.update {
                    it.copy(
                        team = data.team,
                        healingTasks = data.healingTasks,
                        healableFailedTasks = data.healableFailedTasks,
                        selectedHealingTaskId = null,
                        message = "Gyógyítás rögzítve",
                    )
                }
            },
        )
    }

    fun clearMessages() {
        _uiState.update { it.copy(message = null, errorMessage = null) }
    }

    private fun runRepositoryAction(
        clearSelection: Boolean,
        action: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    message = null,
                    errorMessage = null,
                    selectedHealingTaskId = if (clearSelection) null else it.selectedHealingTaskId,
                )
            }
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
