package mok.it.tortura.feature

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mok.it.tortura.model.TeamAssignment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class SetupViewModelTest {

    @Test
    fun loadSetupDataPopulatesTeamAssignmentsForActiveGame() = runViewModelTest {
        val dataSource = FakeSetupDataSource(
            teamAssignments = listOf(TeamAssignment(id = 2, baseTeamCounter = 4, gameId = 7)),
        )
        val viewModel = SetupViewModel(
            activeGameId = 7,
            dataSource = dataSource,
        )

        viewModel.loadSetupData()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(4L), state.teamAssignments.map { it.baseTeamCounter })
        assertEquals(listOf(7L), state.teamAssignments.map { it.gameId })
        assertEquals(listOf(7L), dataSource.loadedGameIds)
        assertEquals("Adatok betöltve", state.message)
        assertNull(state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun createTeamAssignmentCreatesRowForActiveGameAndClearsInput() = runViewModelTest {
        val dataSource = FakeSetupDataSource()
        val viewModel = SetupViewModel(activeGameId = 7, dataSource = dataSource)

        viewModel.onBaseTeamCounterChange("6")
        viewModel.createTeamAssignment()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(7L to 6L), dataSource.createdTeamAssignments)
        assertEquals(listOf(6L), state.teamAssignments.map { it.baseTeamCounter })
        assertEquals(listOf(7L), state.teamAssignments.map { it.gameId })
        assertEquals("", state.baseTeamCounter)
        assertEquals("Csapatbeosztás létrehozva", state.message)
        assertNull(state.errorMessage)
    }

    @Test
    fun createTeamAssignmentRejectsNonNumericValue() = runViewModelTest {
        val dataSource = FakeSetupDataSource()
        val viewModel = SetupViewModel(activeGameId = 7, dataSource = dataSource)

        viewModel.onBaseTeamCounterChange("not a number")
        viewModel.createTeamAssignment()
        advanceUntilIdle()

        assertEquals(emptyList(), dataSource.createdTeamAssignments)
        assertEquals("A csapatszám legyen egész szám", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun repositoryErrorIsExposedInStateAndLoadingStops() = runViewModelTest {
        val viewModel = SetupViewModel(
            activeGameId = 7,
            FakeSetupDataSource(loadError = IllegalStateException("database unavailable"))
        )

        viewModel.loadSetupData()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("database unavailable", state.errorMessage)
        assertFalse(state.isLoading)
    }
}

private class FakeSetupDataSource(
    private val teamAssignments: List<TeamAssignment> = emptyList(),
    private val loadError: Exception? = null,
) : SetupDataSource {
    val loadedGameIds = mutableListOf<Long>()
    val createdTeamAssignments = mutableListOf<Pair<Long, Long>>()

    override suspend fun getTeamAssignments(gameId: Long): List<TeamAssignment> {
        loadError?.let { throw it }
        loadedGameIds += gameId
        return teamAssignments
    }

    override suspend fun createTeamAssignment(gameId: Long, baseTeamCounter: Long): TeamAssignment {
        createdTeamAssignments += gameId to baseTeamCounter
        return TeamAssignment(
            id = createdTeamAssignments.size.toLong(),
            baseTeamCounter = baseTeamCounter,
            gameId = gameId,
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun runViewModelTest(block: suspend kotlinx.coroutines.test.TestScope.() -> Unit) = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    try {
        block()
    } finally {
        Dispatchers.resetMain()
    }
}
