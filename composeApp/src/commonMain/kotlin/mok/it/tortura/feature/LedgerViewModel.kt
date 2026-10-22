package mok.it.tortura.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlin.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import mok.it.tortura.SupabaseClient
import mok.it.tortura.data.supabase.SupabaseTables
import mok.it.tortura.data.supabase.dto.HealingLedgerDto
import mok.it.tortura.data.supabase.dto.ShopDto
import mok.it.tortura.data.supabase.dto.TasksLedgerDto
import mok.it.tortura.data.supabase.mapper.toModel
import mok.it.tortura.data.supabase.repository.TorturaSupabaseRepositories
import mok.it.tortura.model.Team

enum class LedgerKind(val apiValue: String) { Task("task"), Healing("healing"), Shop("shop") }

data class LedgerEntry(
    val id: Long,
    val kind: LedgerKind,
    val title: String,
    val createdAt: Instant? = null,
    val userId: Long? = null,
    val reversesId: Long? = null,
    val reversed: Boolean = false,
    val automatic: Boolean = false,
) {
    val canUndo: Boolean get() = reversesId == null && !reversed && !automatic
}

fun List<LedgerEntry>.withReversalStatus(): List<LedgerEntry> {
    val reversedKeys = mapNotNull { entry -> entry.reversesId?.let { entry.kind to it } }.toSet()
    return map { it.copy(reversed = (it.kind to it.id) in reversedKeys) }
        .sortedWith(compareByDescending<LedgerEntry> { it.createdAt }.thenByDescending { it.id })
}

interface LedgerDataSource {
    suspend fun loadTeams(gameId: Long): List<Team>
    suspend fun loadEntries(
        gameId: Long,
        teamId: Long,
    ): List<LedgerEntry>
    suspend fun undo(
        teamId: Long,
        entry: LedgerEntry,
    )
}

class SupabaseLedgerDataSource : LedgerDataSource {
    private val client = SupabaseClient.client
    private val repositories = TorturaSupabaseRepositories(client)

    override suspend fun loadTeams(gameId: Long): List<Team> = repositories.teamAssignments.getByGameId(gameId)
        .mapNotNull { it.id }
        .flatMap { repositories.teams.getByTeamAssignmentId(it) }
        .map { it.toModel() }
        .sortedBy { it.name.orEmpty() }

    override suspend fun loadEntries(
        gameId: Long,
        teamId: Long,
    ): List<LedgerEntry> {
        val tasks = repositories.tasks.getByGameId(gameId).associateBy { it.id }
        val healingTasks = repositories.healingTasks.getByGameId(gameId).associateBy { it.id }
        val items = repositories.items.getByGameId(gameId).associateBy { it.id }
        val attempts = client.from(SupabaseTables.TASKS_LEDGER).select {
            filter { eq("teamId", teamId) }
        }.decodeList<TasksLedgerDto>()
        val healing = client.from(SupabaseTables.HEALING_LEDGER).select {
            filter { eq("teamId", teamId) }
        }.decodeList<HealingLedgerDto>()
        val purchases = client.from(SupabaseTables.SHOP).select {
            filter { eq("teamId", teamId) }
        }.decodeList<ShopDto>()
        return (
            attempts.mapNotNull { row ->
                row.id?.let {
                    val status = when {
                        row.bonusSourceShopId != null -> "Bónusz"
                        row.isSuccess == true -> "Sikeres próba"
                        row.isSuccess == false -> "Sikertelen próba"
                        else -> "Ismeretlen eredményű próba"
                    }
                    LedgerEntry(
                        it,
                        LedgerKind.Task,
                        "$status: ${tasks[row.taskId]?.text ?: "#${row.taskId}"}",
                        row.createdAt,
                        row.userId,
                        row.reversesId,
                        automatic = row.bonusSourceShopId != null,
                    )
                }
            } + healing.mapNotNull { row ->
                row.id?.let {
                    LedgerEntry(
                        it,
                        LedgerKind.Healing,
                        "Gyógyítás: ${healingTasks[row.healingTaskId]?.text ?: "#${row.healingTaskId}"} (próba #${row.healedTasksLedgerId})",
                        row.createdAt,
                        row.userId,
                        row.reversesId,
                    )
                }
            } + purchases.mapNotNull { row ->
                row.id?.let {
                    val target = row.targetId?.let { targetId -> ", cél #$targetId" }.orEmpty()
                    LedgerEntry(
                        it,
                        LedgerKind.Shop,
                        "Vásárlás: ${items[row.itemId]?.name ?: "#${row.itemId}"}, ${items[row.itemId]?.price ?: 0} pont$target",
                        row.createdAt,
                        row.userId,
                        row.reversesId,
                    )
                }
            }
            ).withReversalStatus()
    }

    override suspend fun undo(
        teamId: Long,
        entry: LedgerEntry,
    ) {
        client.postgrest.rpc(
            "undo_ledger_entry",
            buildJsonObject {
                put("p_kind", entry.kind.apiValue)
                put("p_entry_id", entry.id)
                put("p_team_id", teamId)
            },
        )
    }
}

data class LedgerUiState(
    val teams: List<Team> = emptyList(),
    val selectedTeamId: Long? = null,
    val entries: List<LedgerEntry> = emptyList(),
    val busy: Boolean = false,
    val message: String? = null,
    val errorMessage: String? = null,
)

class LedgerViewModel(
    private val gameId: Long,
    initialTeamId: Long? = null,
    private val dataSource: LedgerDataSource = SupabaseLedgerDataSource(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(LedgerUiState(selectedTeamId = initialTeamId))
    val uiState: StateFlow<LedgerUiState> = _uiState

    fun load() = runAction {
        val teams = dataSource.loadTeams(gameId)
        val teamId =
            _uiState.value.selectedTeamId?.takeIf { id -> teams.any { it.id == id } } ?: teams.firstOrNull()?.id
        val entries = teamId?.let { dataSource.loadEntries(gameId, it) }.orEmpty()
        _uiState.update { it.copy(teams = teams, selectedTeamId = teamId, entries = entries) }
    }

    fun selectTeam(teamId: Long) {
        if (_uiState.value.teams.none { it.id == teamId }) return
        runAction {
            val entries = dataSource.loadEntries(gameId, teamId)
            _uiState.update { it.copy(selectedTeamId = teamId, entries = entries) }
        }
    }

    fun undo(entry: LedgerEntry) {
        val state = _uiState.value
        val teamId = state.selectedTeamId ?: return
        if (state.entries.none { it.kind == entry.kind && it.id == entry.id && it.canUndo }) return
        runAction {
            dataSource.undo(teamId, entry)
            val entries = dataSource.loadEntries(gameId, teamId)
            _uiState.update { it.copy(entries = entries, message = "Visszavonás rögzítve") }
        }
    }

    fun clearMessages() = _uiState.update { it.copy(message = null, errorMessage = null) }

    private fun runAction(action: suspend () -> Unit) {
        if (_uiState.value.busy) return
        _uiState.update { it.copy(busy = true, message = null, errorMessage = null) }
        viewModelScope.launch {
            try {
                action()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update { it.copy(errorMessage = exception.message ?: "A napló művelete sikertelen") }
            } finally {
                _uiState.update { it.copy(busy = false) }
            }
        }
    }
}
