package mok.it.tortura.feature

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mok.it.tortura.model.Game
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class GameSelectionViewModelTest {

    @Test
    fun loadGamesPopulatesExistingGames() = runGameSelectionViewModelTest {
        val viewModel = GameSelectionViewModel(
            FakeGameSelectionDataSource(
                games = listOf(Game(id = 1, name = "Main game")),
            )
        )

        viewModel.loadGames()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("Main game"), state.games.map { it.name })
        assertEquals("Játékok betöltve", state.message)
        assertNull(state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun createGameTrimsNameCreatesRowAndSelectsCreatedGame() = runGameSelectionViewModelTest {
        val dataSource = FakeGameSelectionDataSource()
        val viewModel = GameSelectionViewModel(dataSource)

        viewModel.onGameNameChange("  New game  ")
        viewModel.createGame()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("New game"), dataSource.createdGameNames)
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
    fun selectGameStoresExistingGame() = runGameSelectionViewModelTest {
        val game = Game(id = 3, name = "Existing game")
        val viewModel = GameSelectionViewModel(FakeGameSelectionDataSource())

        viewModel.selectGame(game)

        assertEquals(game, viewModel.uiState.value.selectedGame)
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
    fun repositoryErrorIsExposedInStateAndLoadingStops() = runGameSelectionViewModelTest {
        val viewModel = GameSelectionViewModel(
            FakeGameSelectionDataSource(loadError = IllegalStateException("database unavailable"))
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
    private val loadError: Exception? = null,
) : GameSelectionDataSource {
    val createdGameNames = mutableListOf<String>()

    override suspend fun getGames(): List<Game> {
        loadError?.let { throw it }
        return games
    }

    override suspend fun createGame(name: String): Game {
        createdGameNames += name
        return Game(id = createdGameNames.size.toLong(), name = name)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun runGameSelectionViewModelTest(block: suspend kotlinx.coroutines.test.TestScope.() -> Unit) = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    try {
        block()
    } finally {
        Dispatchers.resetMain()
    }
}
