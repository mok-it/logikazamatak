package mok.it.tortura.feature

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mok.it.tortura.model.Game
import mok.it.tortura.model.TeamAssignment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class SetupViewModelTest {

    @Test
    fun loadSetupDataPopulatesGamesAndTeamAssignments() = runViewModelTest {
        val viewModel = SetupViewModel(
            FakeSetupDataSource(
                games = listOf(Game(id = 1, name = "Main game")),
                teamAssignments = listOf(TeamAssignment(id = 2, baseTeamCounter = 4)),
            )
        )

        viewModel.loadSetupData()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("Main game"), state.games.map { it.name })
        assertEquals(listOf(4L), state.teamAssignments.map { it.baseTeamCounter })
        assertEquals("Adatok betöltve", state.message)
        assertNull(state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun createGameTrimsNameCreatesRowAndClearsInput() = runViewModelTest {
        val dataSource = FakeSetupDataSource()
        val viewModel = SetupViewModel(dataSource)

        viewModel.onGameNameChange("  New game  ")
        viewModel.createGame()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("New game"), dataSource.createdGameNames)
        assertEquals(listOf("New game"), state.games.map { it.name })
        assertEquals("", state.gameName)
        assertEquals("Feladatsor létrehozva", state.message)
        assertNull(state.errorMessage)
    }

    @Test
    fun createGameRejectsBlankNameWithoutCallingDataSource() = runViewModelTest {
        val dataSource = FakeSetupDataSource()
        val viewModel = SetupViewModel(dataSource)

        viewModel.onGameNameChange("   ")
        viewModel.createGame()
        advanceUntilIdle()

        assertEquals(emptyList(), dataSource.createdGameNames)
        assertEquals("Adj nevet a feladatsornak", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun createTeamAssignmentCreatesRowAndClearsInput() = runViewModelTest {
        val dataSource = FakeSetupDataSource()
        val viewModel = SetupViewModel(dataSource)

        viewModel.onBaseTeamCounterChange("6")
        viewModel.createTeamAssignment()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(6L), dataSource.createdBaseTeamCounters)
        assertEquals(listOf(6L), state.teamAssignments.map { it.baseTeamCounter })
        assertEquals("", state.baseTeamCounter)
        assertEquals("Csapatbeosztás létrehozva", state.message)
        assertNull(state.errorMessage)
    }

    @Test
    fun createTeamAssignmentRejectsNonNumericValue() = runViewModelTest {
        val dataSource = FakeSetupDataSource()
        val viewModel = SetupViewModel(dataSource)

        viewModel.onBaseTeamCounterChange("not a number")
        viewModel.createTeamAssignment()
        advanceUntilIdle()

        assertEquals(emptyList(), dataSource.createdBaseTeamCounters)
        assertEquals("A csapatszám legyen egész szám", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun repositoryErrorIsExposedInStateAndLoadingStops() = runViewModelTest {
        val viewModel = SetupViewModel(
            FakeSetupDataSource(loadError = IllegalStateException("database unavailable"))
        )

        viewModel.loadSetupData()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("database unavailable", state.errorMessage)
        assertFalse(state.isLoading)
    }

    private fun runViewModelTest(block: suspend kotlinx.coroutines.test.TestScope.() -> Unit) = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            block()
        } finally {
            Dispatchers.resetMain()
        }
    }
}

private class FakeSetupDataSource(
    private val games: List<Game> = emptyList(),
    private val teamAssignments: List<TeamAssignment> = emptyList(),
    private val loadError: Exception? = null,
) : SetupDataSource {
    val createdGameNames = mutableListOf<String>()
    val createdBaseTeamCounters = mutableListOf<Long>()

    override suspend fun getGames(): List<Game> {
        loadError?.let { throw it }
        return games
    }

    override suspend fun getTeamAssignments(): List<TeamAssignment> {
        loadError?.let { throw it }
        return teamAssignments
    }

    override suspend fun createGame(name: String): Game {
        createdGameNames += name
        return Game(id = createdGameNames.size.toLong(), name = name)
    }

    override suspend fun createTeamAssignment(baseTeamCounter: Long): TeamAssignment {
        createdBaseTeamCounters += baseTeamCounter
        return TeamAssignment(
            id = createdBaseTeamCounters.size.toLong(),
            baseTeamCounter = baseTeamCounter,
        )
    }
}
