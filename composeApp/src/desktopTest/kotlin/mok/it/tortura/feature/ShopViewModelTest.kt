package mok.it.tortura.feature

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mok.it.tortura.model.Item
import mok.it.tortura.model.ItemEffect
import mok.it.tortura.model.ItemEffectCode
import mok.it.tortura.model.ShopEntry
import mok.it.tortura.model.Task
import mok.it.tortura.model.TaskEvent
import mok.it.tortura.model.Team
import mok.it.tortura.model.TeamScoreCalculator

@OptIn(ExperimentalCoroutinesApi::class)
class ShopViewModelTest {

    @Test
    fun loadSelectsFirstTeamAndBuildsStockForThatTeam() = runShopViewModelTest {
        val dataSource = FakeShopDataSource(
            catalog = sampleCatalog(),
            purchasesByTeamId = mapOf(1L to listOf(ShopEntry(itemId = 101, teamId = 1))),
            taskEventsByTeamId = mapOf(
                1L to List(12) {
                    TaskEvent(teamId = 1, isSuccess = true, taskId = it.toLong())
                },
            ),
        )
        val viewModel = ShopViewModel(activeGameId = 44, dataSource = dataSource)

        viewModel.load()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(44L, dataSource.loadedGameIds.single())
        assertEquals(1L, state.selectedTeamId)
        assertEquals(1, state.itemRows.first { it.item.id == 101L }.purchasedCount)
        assertEquals(1, state.itemRows.first { it.item.id == 101L }.remainingStock)
        assertEquals(12, state.selectedTeamScore)
        assertEquals(5, state.selectedTeamSpent)
        assertEquals(7, state.selectedTeamBudget)
        assertNull(state.message)
        assertNull(state.errorMessage)
    }

    @Test
    fun applyScoreAdjustmentUpdatesDerivedBudgetAndClearsInput() = runShopViewModelTest {
        val dataSource = FakeShopDataSource(
            catalog = sampleCatalog(),
            taskEventsByTeamId = mapOf(
                1L to List(12) {
                    TaskEvent(teamId = 1, isSuccess = true, taskId = it.toLong())
                },
            ),
        )
        val viewModel = ShopViewModel(activeGameId = 44, dataSource = dataSource)

        viewModel.load()
        advanceUntilIdle()
        viewModel.onScoreAdjustmentInputChange("-3a")
        viewModel.applyScoreAdjustment()
        advanceUntilIdle()

        assertEquals(listOf(1L to -3), dataSource.scoreAdjustments)
        assertEquals("", viewModel.uiState.value.scoreAdjustmentInput)
        assertEquals(9, viewModel.uiState.value.selectedTeamScore)
        assertEquals(9, viewModel.uiState.value.selectedTeamBudget)
        assertEquals("Pontkorrekció rögzítve", viewModel.uiState.value.message)
    }

    @Test
    fun purchaseRequiresTargetForTargetedItems() = runShopViewModelTest {
        val viewModel = ShopViewModel(
            activeGameId = 44,
            dataSource = FakeShopDataSource(catalog = sampleCatalog()),
        )

        viewModel.load()
        advanceUntilIdle()
        viewModel.purchaseItem(101)
        advanceUntilIdle()

        assertEquals("Ehhez a tárgyhoz célpontot kell választani", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun purchaseRefreshesDerivedBudgetFromScoreAndPurchases() = runShopViewModelTest {
        val dataSource = FakeShopDataSource(
            catalog = sampleCatalog(),
            taskEventsByTeamId = mapOf(
                1L to List(12) {
                    TaskEvent(teamId = 1, isSuccess = true, taskId = it.toLong())
                },
            ),
        )
        val viewModel = ShopViewModel(activeGameId = 44, dataSource = dataSource)

        viewModel.load()
        advanceUntilIdle()
        viewModel.selectTarget(101, 201)
        viewModel.purchaseItem(101)
        advanceUntilIdle()

        assertEquals(listOf(Triple(1L, 101L, 201L as Long?)), dataSource.purchaseCalls)
        assertEquals(12, viewModel.uiState.value.selectedTeamScore)
        assertEquals(5, viewModel.uiState.value.selectedTeamSpent)
        assertEquals(7, viewModel.uiState.value.selectedTeamBudget)
        assertEquals("Vásárlás rögzítve", viewModel.uiState.value.message)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun teamScoreCalculatorDerivesPointsSpentAndMoney() {
        val taskEvents = listOf(
            TaskEvent(teamId = 1, taskId = 1, isSuccess = true),
            TaskEvent(teamId = 1, taskId = 2, isSuccess = false),
            TaskEvent(teamId = 1, taskId = 3, isSuccess = true),
        )
        val purchases = listOf(
            ShopEntry(itemId = 101, teamId = 1),
            ShopEntry(itemId = 102, teamId = 1),
        )
        val items = listOf(
            Item(id = 101, price = 5),
            Item(id = 102, price = 10),
        )

        assertEquals(5, TeamScoreCalculator.calculateFinalPoints(taskEvents, additionalScoreAwarded = 3))
        assertEquals(15, TeamScoreCalculator.calculateSpentPoints(purchases, items))
        assertEquals(-10, TeamScoreCalculator.calculateCurrentMoney(taskEvents, 3, purchases, items))
    }
}

private class FakeShopDataSource(
    private val catalog: ShopCatalog,
    private val purchasesByTeamId: Map<Long, List<ShopEntry>> = emptyMap(),
    private val taskEventsByTeamId: Map<Long, List<TaskEvent>> = emptyMap(),
) : ShopDataSource {
    val loadedGameIds = mutableListOf<Long>()
    val purchaseCalls = mutableListOf<Triple<Long, Long, Long?>>()
    val scoreAdjustments = mutableListOf<Pair<Long, Int>>()
    private val mutableTeams = catalog.teams.map { it.copy() }.toMutableList()
    private val mutablePurchasesByTeamId = purchasesByTeamId.mapValues { it.value.toMutableList() }.toMutableMap()

    override suspend fun loadCatalog(gameId: Long): ShopCatalog {
        loadedGameIds += gameId
        return catalog.copy(teams = mutableTeams.map { it.copy() })
    }

    override suspend fun getPurchases(teamId: Long): List<ShopEntry> = mutablePurchasesByTeamId[teamId].orEmpty()

    override suspend fun getTaskEvents(teamId: Long): List<TaskEvent> = taskEventsByTeamId[teamId].orEmpty()

    override suspend fun adjustAdditionalScoreAwarded(
        teamId: Long,
        delta: Int,
    ): Team {
        scoreAdjustments += teamId to delta
        val index = mutableTeams.indexOfFirst { it.id == teamId }
        val updatedTeam = mutableTeams[index].copy(
            additionalScoreAwarded =
                mutableTeams[index].additionalScoreAwarded + delta,
        )
        mutableTeams[index] = updatedTeam
        return updatedTeam
    }

    override suspend fun purchaseItem(
        teamId: Long,
        itemId: Long,
        targetId: Long?,
    ) {
        purchaseCalls += Triple(teamId, itemId, targetId)
        mutablePurchasesByTeamId.getOrPut(teamId) { mutableListOf() }
            .add(ShopEntry(itemId = itemId, teamId = teamId, targetId = targetId))
    }
}

private fun sampleCatalog(): ShopCatalog = ShopCatalog(
    teams = listOf(
        Team(id = 1, name = "Alfa"),
        Team(id = 2, name = "Béta"),
    ),
    items = listOf(
        Item(
            id = 101,
            name = "Feladatduplázó",
            price = 5,
            itemEffectId = 501,
            gameId = 44,
            maxPerTeam = 2,
        ),
        Item(
            id = 102,
            name = "Minibosslelőhely",
            price = 40,
            itemEffectId = 502,
            gameId = 44,
            maxPerTeam = 1,
        ),
    ),
    itemEffects = mapOf(
        501L to ItemEffect(id = 501, code = ItemEffectCode.TASK_SCORE_MULTIPLIER, description = "feladatduplázó"),
        502L to ItemEffect(id = 502, code = ItemEffectCode.MINIBOSS_UNLOCK, description = "minibosslelőhely"),
    ),
    tasks = listOf(
        Task(id = 201, text = "Első feladat", isMiniBoss = false, gameId = 44, locationId = 301),
        Task(id = 202, text = "Miniboss feladat", isMiniBoss = true, gameId = 44, locationId = 301),
    ),
    locations = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
private fun runShopViewModelTest(block: suspend kotlinx.coroutines.test.TestScope.() -> Unit) = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    try {
        block()
    } finally {
        Dispatchers.resetMain()
    }
}
