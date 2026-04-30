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
import mok.it.tortura.model.Game
import mok.it.tortura.model.Location
import mok.it.tortura.model.ItemEffect

@OptIn(ExperimentalCoroutinesApi::class)
class GameSelectionViewModelTest {

    @Test
    fun loadGamesPopulatesExistingGames() = runGameSelectionViewModelTest {
        val viewModel = GameSelectionViewModel(
            FakeGameSelectionDataSource(
                games = listOf(Game(id = 1, name = "Main game")),
                itemEffects = listOf(ItemEffect(id = 10, description = "Double points")),
            ),
        )

        viewModel.loadGames()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("Main game"), state.games.map { it.name })
        assertEquals(listOf("Double points"), state.itemEffects.map { it.description })
        assertEquals("Játékok betöltve", state.message)
        assertNull(state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun createGameWithOnlyNameCreatesGameAndNoChildRows() = runGameSelectionViewModelTest {
        val dataSource = FakeGameSelectionDataSource()
        val viewModel = GameSelectionViewModel(dataSource)

        viewModel.onGameNameChange("  New game  ")
        viewModel.createGame()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("New game"), dataSource.createdGameNames)
        assertEquals(emptyList(), dataSource.createdSetups.single().locations)
        assertEquals(emptyList(), dataSource.createdSetups.single().tasks)
        assertEquals(emptyList(), dataSource.createdSetups.single().healingTasks)
        assertEquals(emptyList(), dataSource.createdSetups.single().shopItems)
        assertEquals(listOf("New game"), state.games.map { it.name })
        assertEquals("New game", state.selectedGame?.name)
        assertEquals("", state.gameName)
        assertEquals("Játék létrehozva", state.message)
        assertNull(state.errorMessage)
    }

    @Test
    fun createGameRejectsBlankNameWithoutCallingDataSource() = runGameSelectionViewModelTest {
        val dataSource = FakeGameSelectionDataSource()
        val viewModel = GameSelectionViewModel(dataSource)

        viewModel.onGameNameChange("   ")
        viewModel.createGame()
        advanceUntilIdle()

        assertEquals(emptyList(), dataSource.createdGameNames)
        assertEquals("Adj nevet a játéknak", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun createGamePersistsUserAddedSetupRows() = runGameSelectionViewModelTest {
        val dataSource = FakeGameSelectionDataSource()
        val viewModel = GameSelectionViewModel(dataSource)

        viewModel.onGameNameChange("Configured game")
        viewModel.addLocation()
        val locationId = viewModel.uiState.value.draftLocations.single().localId
        viewModel.updateLocationName(locationId, "Library")
        viewModel.addTask(locationId)
        val taskId = viewModel.uiState.value.draftTasks.single().localId
        viewModel.updateTaskText(taskId, "2 + 2")
        viewModel.updateTaskSolution(taskId, "4")
        viewModel.updateTaskMiniBoss(taskId, true)
        viewModel.addHealingTask()
        val healingTaskId = viewModel.uiState.value.draftHealingTasks.single().localId
        viewModel.updateHealingTaskText(healingTaskId, "Reverse LOGIC")
        viewModel.updateHealingTaskSolution(healingTaskId, "CIGOL")
        viewModel.addShopItem()
        val itemId = viewModel.uiState.value.draftShopItems.single().localId
        viewModel.updateShopItemName(itemId, "Hint")
        viewModel.updateShopItemPrice(itemId, "3")
        viewModel.updateShopItemMaxPerTeam(itemId, "2")
        viewModel.updateShopItemEffectId(itemId, "9")

        viewModel.createGame()
        advanceUntilIdle()

        val setup = dataSource.createdSetups.single()
        assertEquals("Configured game", setup.name)
        assertEquals(listOf("Library"), setup.locations.map { it.name })
        assertEquals(listOf("2 + 2"), setup.tasks.map { it.text })
        assertEquals(listOf("4"), setup.tasks.map { it.solution })
        assertEquals(listOf(true), setup.tasks.map { it.isMiniBoss })
        assertEquals(listOf("Reverse LOGIC"), setup.healingTasks.map { it.text })
        assertEquals(listOf("Hint"), setup.shopItems.map { it.name })
        assertEquals(listOf("3"), setup.shopItems.map { it.price })
        assertEquals(listOf("2"), setup.shopItems.map { it.maxPerTeam })
        assertEquals(listOf("9"), setup.shopItems.map { it.itemEffectId })
    }

    @Test
    fun createGameRejectsInvalidShopNumbers() = runGameSelectionViewModelTest {
        val dataSource = FakeGameSelectionDataSource()
        val viewModel = GameSelectionViewModel(dataSource)

        viewModel.onGameNameChange("Game")
        viewModel.addShopItem()
        val itemId = viewModel.uiState.value.draftShopItems.single().localId
        viewModel.updateShopItemName(itemId, "Hint")
        viewModel.updateShopItemPrice(itemId, "abc")
        viewModel.updateShopItemMaxPerTeam(itemId, "2")

        viewModel.createGame()
        advanceUntilIdle()

        assertEquals(emptyList(), dataSource.createdSetups)
        assertEquals("A bolti tárgy ára legyen nem negatív egész szám", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun shopNumericFieldsIgnoreNonDigits() = runGameSelectionViewModelTest {
        val viewModel = GameSelectionViewModel(FakeGameSelectionDataSource())

        viewModel.addShopItem()
        val itemId = viewModel.uiState.value.draftShopItems.single().localId
        viewModel.updateShopItemPrice(itemId, "12abc3")
        viewModel.updateShopItemMaxPerTeam(itemId, "x2y")

        val item = viewModel.uiState.value.draftShopItems.single()
        assertEquals("123", item.price)
        assertEquals("2", item.maxPerTeam)
    }

    @Test
    fun selectGameStoresExistingGame() = runGameSelectionViewModelTest {
        val game = Game(id = 3, name = "Existing game")
        val viewModel = GameSelectionViewModel(
            FakeGameSelectionDataSource(
                locationsByGameId = mapOf(
                    3L to listOf(Location(id = 10, name = "Castle", gameId = 3)),
                ),
            ),
        )

        viewModel.selectGame(game)
        advanceUntilIdle()

        assertEquals(3L, viewModel.uiState.value.locationSelectionGameId)
        assertNull(viewModel.uiState.value.selectedGame)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun clearSelectionRemovesConsumedSelectedGame() = runGameSelectionViewModelTest {
        val game = Game(id = 3, name = "Existing game")
        val viewModel = GameSelectionViewModel(FakeGameSelectionDataSource())

        viewModel.selectGame(game)
        viewModel.clearSelection()

        assertNull(viewModel.uiState.value.selectedGame)
    }

    @Test
    fun selectGameRejectsGameWithoutId() = runGameSelectionViewModelTest {
        val viewModel = GameSelectionViewModel(FakeGameSelectionDataSource())

        viewModel.selectGame(Game(name = "Incomplete"))

        assertNull(viewModel.uiState.value.selectedGame)
        assertEquals("A játék azonosítója hiányzik", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun selectGameRejectsGameWithoutLocations() = runGameSelectionViewModelTest {
        val viewModel = GameSelectionViewModel(FakeGameSelectionDataSource())

        viewModel.selectGame(Game(id = 3, name = "Existing game"))
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.locationSelectionGameId)
        assertEquals(
            "Ehhez a játékhoz még nincs választható állomás",
            viewModel.uiState.value.errorMessage,
        )
    }

    @Test
    fun repositoryErrorIsExposedInStateAndLoadingStops() = runGameSelectionViewModelTest {
        val viewModel = GameSelectionViewModel(
            FakeGameSelectionDataSource(loadError = IllegalStateException("database unavailable")),
        )

        viewModel.loadGames()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("database unavailable", state.errorMessage)
        assertFalse(state.isLoading)
    }
}

private class FakeGameSelectionDataSource(
    private val games: List<Game> = emptyList(),
    private val locationsByGameId: Map<Long, List<Location>> = emptyMap(),
    private val itemEffects: List<ItemEffect> = emptyList(),
    private val loadError: Exception? = null,
) : GameSelectionDataSource {
    val createdGameNames = mutableListOf<String>()
    val createdSetups = mutableListOf<CreateGameSetup>()

    override suspend fun getGames(): List<Game> {
        loadError?.let { throw it }
        return games
    }

    override suspend fun getItemEffects(): List<ItemEffect> {
        loadError?.let { throw it }
        return itemEffects
    override suspend fun getLocations(gameId: Long): List<Location> {
        loadError?.let { throw it }
        return locationsByGameId[gameId].orEmpty()
    }
    }

    override suspend fun createGame(setup: CreateGameSetup): Game {
        createdSetups += setup
        createdGameNames += setup.name
        return Game(id = createdGameNames.size.toLong(), name = setup.name)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun runGameSelectionViewModelTest(
    block: suspend kotlinx.coroutines.test.TestScope.() -> Unit,
) = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    try {
        block()
    } finally {
        Dispatchers.resetMain()
    }
}
