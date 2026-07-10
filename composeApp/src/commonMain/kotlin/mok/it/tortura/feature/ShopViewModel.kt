package mok.it.tortura.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mok.it.tortura.data.supabase.dto.ShopInsertDto
import mok.it.tortura.data.supabase.mapper.toModel
import mok.it.tortura.data.supabase.mapper.toUpdateDto
import mok.it.tortura.data.supabase.repository.TorturaSupabaseRepositories
import mok.it.tortura.model.Item
import mok.it.tortura.model.ItemEffect
import mok.it.tortura.model.ItemEffectCode
import mok.it.tortura.model.Location
import mok.it.tortura.model.ShopEntry
import mok.it.tortura.model.Task
import mok.it.tortura.model.TaskEvent
import mok.it.tortura.model.Team

enum class ShopTargetType {
    NONE,
    TASK,
    LOCATION,
    MINIBOSS_TASK,
}

data class ShopTargetOption(
    val id: Long,
    val label: String,
)

data class ShopItemRow(
    val item: Item,
    val effectCode: String? = null,
    val effectDescription: String? = null,
    val targetType: ShopTargetType = ShopTargetType.NONE,
    val targetOptions: List<ShopTargetOption> = emptyList(),
    val selectedTargetId: Long? = null,
    val purchasedCount: Int = 0,
    val remainingStock: Int? = null,
)

data class ShopUiState(
    val isLoading: Boolean = false,
    val teams: List<Team> = emptyList(),
    val selectedTeamId: Long? = null,
    val itemRows: List<ShopItemRow> = emptyList(),
    val scoreAdjustmentInput: String = "",
    val selectedTeamScore: Int? = null,
    val selectedTeamSpent: Int? = null,
    val selectedTeamBudget: Int? = null,
    val message: String? = null,
    val errorMessage: String? = null,
)

data class ShopCatalog(
    val teams: List<Team>,
    val items: List<Item>,
    val itemEffects: Map<Long, ItemEffect>,
    val tasks: List<Task>,
    val locations: List<Location>,
)

interface ShopDataSource {
    suspend fun loadCatalog(gameId: Long): ShopCatalog

    suspend fun getPurchases(teamId: Long): List<ShopEntry>

    suspend fun getTaskEvents(teamId: Long): List<TaskEvent>

    suspend fun adjustAdditionalScoreAwarded(teamId: Long, delta: Int): Team

    suspend fun purchaseItem(
        teamId: Long,
        itemId: Long,
        targetId: Long?,
    )
}

class SupabaseShopDataSource(
    private val repositories: TorturaSupabaseRepositories = TorturaSupabaseRepositories(),
) : ShopDataSource {

    override suspend fun loadCatalog(gameId: Long): ShopCatalog {
        val teams = repositories.teamAssignments.getByGameId(gameId)
            .mapNotNull { it.id }
            .flatMap { repositories.teams.getByTeamAssignmentId(it) }
            .map { it.toModel() }
            .sortedBy { (it.name ?: "zzz").lowercase() }
        val items = repositories.items.getByGameId(gameId)
            .map { it.toModel() }
            .sortedBy { (it.name ?: "zzz").lowercase() }
        val itemEffects = repositories.itemEffects.getAll()
            .map { it.toModel() }
            .mapNotNull { effect -> effect.id?.let { id -> id to effect } }
            .toMap()
        val tasks = repositories.tasks.getByGameId(gameId)
            .map { it.toModel() }
            .sortedBy { it.text.lowercase() }
        val locations = repositories.locations.getByGameId(gameId)
            .map { it.toModel() }
            .sortedBy { (it.name ?: "zzz").lowercase() }

        return ShopCatalog(
            teams = teams,
            items = items,
            itemEffects = itemEffects,
            tasks = tasks,
            locations = locations,
        )
    }

    override suspend fun getPurchases(teamId: Long): List<ShopEntry> =
        repositories.shop.getByTeamId(teamId).map { it.toModel() }

    override suspend fun getTaskEvents(teamId: Long): List<TaskEvent> =
        repositories.tasksLedger.getByTeamId(teamId).map { it.toModel() }

    override suspend fun adjustAdditionalScoreAwarded(teamId: Long, delta: Int): Team {
        val team = repositories.teams.getById(teamId)?.toModel() ?: error("A csapat nem található")
        return repositories.teams.update(
            id = teamId,
            team = team.copy(additionalScoreAwarded = team.additionalScoreAwarded + delta).toUpdateDto(),
        ).toModel()
    }

    override suspend fun purchaseItem(
        teamId: Long,
        itemId: Long,
        targetId: Long?,
    ) {
        repositories.shop.create(
            ShopInsertDto(
                itemId = itemId,
                targetId = targetId,
                teamId = teamId,
            ),
        )
    }

}

class ShopViewModel(
    private val activeGameId: Long,
    private val dataSource: ShopDataSource = SupabaseShopDataSource(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShopUiState())
    val uiState: StateFlow<ShopUiState> = _uiState

    private var catalog: ShopCatalog? = null
    private var purchases: List<ShopEntry> = emptyList()
    private var selectedTeamScore: Int = 0
    private var selectedTargets: Map<Long, Long> = emptyMap()

    fun load() {
        runRepositoryAction {
            val loadedCatalog = dataSource.loadCatalog(activeGameId)
            catalog = loadedCatalog
            val selectedTeamId = resolveSelectedTeamId(loadedCatalog.teams)
            refreshSelectedTeamState(selectedTeamId)
            updateUiState(
                teams = loadedCatalog.teams,
                selectedTeamId = selectedTeamId,
                message = "Bolt betöltve",
            )
        }
    }

    fun selectTeam(teamId: Long) {
        runRepositoryAction {
            refreshSelectedTeamState(teamId)
            updateUiState(
                teams = catalog?.teams.orEmpty(),
                selectedTeamId = teamId,
            )
        }
    }

    fun onScoreAdjustmentInputChange(value: String) {
        _uiState.update {
            it.copy(
                scoreAdjustmentInput = sanitizeSignedIntegerInput(value),
                message = null,
                errorMessage = null,
            )
        }
    }

    fun applyScoreAdjustment() {
        val selectedTeamId = uiState.value.selectedTeamId
        if (selectedTeamId == null) {
            _uiState.update { it.copy(errorMessage = "Előbb válassz csapatot") }
            return
        }

        val delta = uiState.value.scoreAdjustmentInput.toIntOrNull()
        if (delta == null) {
            _uiState.update { it.copy(errorMessage = "Adj meg egy egész pontértéket, például -3 vagy 5") }
            return
        }

        runRepositoryAction {
            dataSource.adjustAdditionalScoreAwarded(selectedTeamId, delta)
            val loadedCatalog = dataSource.loadCatalog(activeGameId)
            catalog = loadedCatalog
            refreshSelectedTeamState(selectedTeamId)
            updateUiState(
                teams = loadedCatalog.teams,
                selectedTeamId = selectedTeamId,
                scoreAdjustmentInput = "",
                message = "Pontkorrekció rögzítve",
            )
        }
    }

    fun selectTarget(
        itemId: Long,
        targetId: Long,
    ) {
        selectedTargets = selectedTargets + (itemId to targetId)
        updateUiState(
            teams = catalog?.teams.orEmpty(),
            selectedTeamId = uiState.value.selectedTeamId,
        )
    }

    fun purchaseItem(itemId: Long) {
        val selectedTeamId = uiState.value.selectedTeamId
        if (selectedTeamId == null) {
            _uiState.update { it.copy(errorMessage = "Előbb válassz csapatot") }
            return
        }

        val itemRow = uiState.value.itemRows.firstOrNull { it.item.id == itemId }
        if (itemRow?.item?.id == null) {
            _uiState.update { it.copy(errorMessage = "A kiválasztott tárgy nem található") }
            return
        }

        if (itemRow.targetType != ShopTargetType.NONE && itemRow.selectedTargetId == null) {
            _uiState.update { it.copy(errorMessage = "Ehhez a tárgyhoz célpontot kell választani") }
            return
        }

        runRepositoryAction {
            dataSource.purchaseItem(selectedTeamId, itemRow.item.id, itemRow.selectedTargetId)
            val loadedCatalog = dataSource.loadCatalog(activeGameId)
            catalog = loadedCatalog
            refreshSelectedTeamState(selectedTeamId)
            updateUiState(
                teams = loadedCatalog.teams,
                selectedTeamId = selectedTeamId,
                message = "Vásárlás rögzítve",
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(message = null, errorMessage = null) }
    }

    private fun resolveSelectedTeamId(teams: List<Team>): Long? {
        val currentSelectedTeamId = uiState.value.selectedTeamId
        return currentSelectedTeamId?.takeIf { selectedId -> teams.any { it.id == selectedId } }
            ?: teams.firstOrNull()?.id
    }

    private fun updateUiState(
        teams: List<Team>,
        selectedTeamId: Long?,
        scoreAdjustmentInput: String = uiState.value.scoreAdjustmentInput,
        message: String? = null,
    ) {
        val spent = spentForCurrentSelection()
        _uiState.update {
            it.copy(
                teams = teams,
                selectedTeamId = selectedTeamId,
                itemRows = buildItemRows(),
                scoreAdjustmentInput = scoreAdjustmentInput,
                selectedTeamScore = selectedTeamId?.let { selectedTeamScore },
                selectedTeamSpent = selectedTeamId?.let { spent },
                selectedTeamBudget = selectedTeamId?.let { selectedTeamScore - spent },
                message = message,
                errorMessage = null,
            )
        }
    }

    private suspend fun refreshSelectedTeamState(teamId: Long?) {
        if (teamId == null) {
            purchases = emptyList()
            selectedTeamScore = 0
            return
        }

        purchases = dataSource.getPurchases(teamId)
        val taskScore = dataSource.getTaskEvents(teamId).count { it.isSuccess == true }
        val manualAdjustment = catalog?.teams.orEmpty().firstOrNull { it.id == teamId }?.additionalScoreAwarded ?: 0
        selectedTeamScore = taskScore + manualAdjustment
    }

    private fun spentForCurrentSelection(): Int {
        val itemsById = catalog?.items.orEmpty().associateBy { it.id }
        return purchases.sumOf { purchase ->
            val itemId = purchase.itemId ?: return@sumOf 0
            itemsById[itemId]?.price ?: 0
        }
    }

    private fun buildItemRows(): List<ShopItemRow> {
        val loadedCatalog = catalog ?: return emptyList()
        return loadedCatalog.items.map { item ->
            val effect = item.itemEffectId?.let(loadedCatalog.itemEffects::get)
            val targetType = targetTypeFor(effect?.code)
            val targetOptions = targetOptionsFor(targetType, loadedCatalog)
            val selectedTargetId = selectedTargets[item.id]
                ?.takeIf { targetId -> targetOptions.any { it.id == targetId } }
            val purchasedCount = item.id?.let { currentItemId ->
                purchases.count { purchase -> purchase.itemId == currentItemId }
            } ?: 0
            val remainingStock = item.maxPerTeam?.minus(purchasedCount)?.coerceAtLeast(0)

            ShopItemRow(
                item = item,
                effectCode = effect?.code,
                effectDescription = effect?.description,
                targetType = targetType,
                targetOptions = targetOptions,
                selectedTargetId = selectedTargetId,
                purchasedCount = purchasedCount,
                remainingStock = remainingStock,
            )
        }
    }

    private fun targetTypeFor(effectCode: String?): ShopTargetType = when (effectCode) {
        ItemEffectCode.TASK_SCORE_MULTIPLIER,
        ItemEffectCode.RETROACTIVE_TASK_SCORE_MULTIPLIER,
        -> ShopTargetType.TASK

        ItemEffectCode.RETROACTIVE_LOCATION_SCORE_MULTIPLIER,
        ItemEffectCode.BOSS_LOCATION_UNLOCK,
        -> ShopTargetType.LOCATION

        ItemEffectCode.MINIBOSS_UNLOCK,
        ItemEffectCode.MINIBOSS_REWIND,
        -> ShopTargetType.MINIBOSS_TASK

        else -> ShopTargetType.NONE
    }

    private fun targetOptionsFor(
        targetType: ShopTargetType,
        loadedCatalog: ShopCatalog,
    ): List<ShopTargetOption> = when (targetType) {
        ShopTargetType.NONE -> emptyList()

        ShopTargetType.TASK -> loadedCatalog.tasks.mapNotNull { task ->
            val taskId = task.id ?: return@mapNotNull null
            ShopTargetOption(
                id = taskId,
                label = task.text,
            )
        }

        ShopTargetType.LOCATION -> loadedCatalog.locations.mapNotNull { location ->
            val locationId = location.id ?: return@mapNotNull null
            ShopTargetOption(
                id = locationId,
                label = location.name ?: "Helyszín #$locationId",
            )
        }

        ShopTargetType.MINIBOSS_TASK ->
            loadedCatalog.tasks
                .filter { it.isMiniBoss == true }
                .mapNotNull { task ->
                    val taskId = task.id ?: return@mapNotNull null
                    ShopTargetOption(
                        id = taskId,
                        label = task.text,
                    )
                }
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

private fun sanitizeSignedIntegerInput(value: String): String {
    if (value.isEmpty()) return ""
    val isNegative = value.first() == '-'
    val digits = value.filter(Char::isDigit)
    return when {
        isNegative && digits.isEmpty() -> "-"
        isNegative -> "-$digits"
        else -> digits
    }
}
