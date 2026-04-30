package mok.it.tortura.feature

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mok.it.tortura.model.Game
import mok.it.tortura.model.Location
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class LocationSelectionViewModelTest {

    @Test
    fun loadPopulatesGameAndLocations() = runLocationSelectionViewModelTest {
        val viewModel = LocationSelectionViewModel(
            gameId = 7,
            dataSource = FakeLocationSelectionDataSource(
                game = Game(id = 7, name = "Main game"),
                locations = listOf(
                    Location(id = 1, name = "Castle", gameId = 7),
                    Location(id = 2, name = "Forest", gameId = 7),
                ),
            ),
        )

        viewModel.load()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Main game", state.game?.name)
        assertEquals(listOf("Castle", "Forest"), state.locations.map { it.name })
        assertNull(state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun loadReportsMissingLocations() = runLocationSelectionViewModelTest {
        val viewModel = LocationSelectionViewModel(
            gameId = 7,
            dataSource = FakeLocationSelectionDataSource(
                game = Game(id = 7, name = "Main game"),
            ),
        )

        viewModel.load()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Main game", state.game?.name)
        assertEquals(emptyList(), state.locations)
        assertEquals("Ehhez a játékhoz még nincs választható állomás", state.errorMessage)
        assertFalse(state.isLoading)
    }
}

private class FakeLocationSelectionDataSource(
    private val game: Game? = null,
    private val locations: List<Location> = emptyList(),
) : LocationSelectionDataSource {

    override suspend fun getGame(gameId: Long): Game? = game

    override suspend fun getLocations(gameId: Long): List<Location> = locations
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun runLocationSelectionViewModelTest(
    block: suspend kotlinx.coroutines.test.TestScope.() -> Unit,
) = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    try {
        block()
    } finally {
        Dispatchers.resetMain()
    }
}
