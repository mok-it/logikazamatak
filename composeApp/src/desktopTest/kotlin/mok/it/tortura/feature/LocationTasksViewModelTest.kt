package mok.it.tortura.feature

import kotlin.test.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import mok.it.tortura.model.*

@OptIn(ExperimentalCoroutinesApi::class)
class LocationTasksViewModelTest {
    @Test
    fun correctAnswerIgnoresCaseAndWhitespaceAndRefreshesMoney() = runLocationTasksTest {
        val source = FakeLocationTasksDataSource()
        val viewModel = locationTasksViewModel(source)
        viewModel.load()
        advanceUntilIdle()
        viewModel.onAnswerChange(10, "  FoRtY   TwO  ")
        viewModel.submitAnswer(10)
        advanceUntilIdle()
        assertEquals(listOf(Attempt(1, 10, true)), source.attempts)
        val state = viewModel.uiState.value
        assertEquals(6, state.selectedTeamProgress?.points)
        assertEquals(4, state.selectedTeamProgress?.money)
        assertEquals(1, state.selectedTeamProgress?.solvedTasks)
        assertEquals(TaskSubmissionResult(true, "FoRtY   TwO"), state.latestSubmissionByTaskId[10])
        assertFalse(10L in state.answerDrafts)
        assertFalse(state.isLoading)
    }

    @Test
    fun incorrectAnswerRecordsFailureWithoutAwardingPoints() = runLocationTasksTest {
        val source = FakeLocationTasksDataSource()
        val viewModel = locationTasksViewModel(source)
        viewModel.load()
        advanceUntilIdle()
        viewModel.onAnswerChange(10, "wrong")
        viewModel.submitAnswer(10)
        advanceUntilIdle()
        assertEquals(listOf(Attempt(1, 10, false)), source.attempts)
        assertEquals(5, viewModel.uiState.value.selectedTeamProgress?.points)
        assertEquals(false, viewModel.uiState.value.latestSubmissionByTaskId[10]?.isSuccess)
    }

    @Test
    fun submissionRequiresATeamKnownTaskAndNonBlankAnswer() = runLocationTasksTest {
        val source = FakeLocationTasksDataSource()
        val viewModel = locationTasksViewModel(source)
        viewModel.submitAnswer(10)
        assertNotNull(viewModel.uiState.value.errorMessage)
        viewModel.load()
        advanceUntilIdle()
        viewModel.submitAnswer(999)
        assertNotNull(viewModel.uiState.value.errorMessage)
        viewModel.onAnswerChange(10, " \n ")
        viewModel.submitAnswer(10)
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(source.attempts.isEmpty())
    }

    @Test
    fun failedPersistenceKeepsDraftAndDoesNotClaimSuccess() = runLocationTasksTest {
        val source = FakeLocationTasksDataSource(failWrites = true)
        val viewModel = locationTasksViewModel(source)
        viewModel.load()
        advanceUntilIdle()
        viewModel.onAnswerChange(10, "forty two")
        viewModel.submitAnswer(10)
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals("forty two", state.answerDrafts[10])
        assertTrue(state.latestSubmissionByTaskId.isEmpty())
        assertEquals("Write failed", state.errorMessage)
        assertFalse(state.isLoading)
    }

    @Test
    fun changingTeamsClearsThePreviousTeamsDraftsAndResults() = runLocationTasksTest {
        val viewModel = locationTasksViewModel(FakeLocationTasksDataSource())
        viewModel.load()
        advanceUntilIdle()
        viewModel.onAnswerChange(10, "forty two")
        viewModel.submitAnswer(10)
        advanceUntilIdle()
        viewModel.onAnswerChange(10, "new draft")
        viewModel.selectTeam(2)
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(2L, state.selectedTeamId)
        assertEquals(0, state.selectedTeamProgress?.money)
        assertTrue(state.answerDrafts.isEmpty())
        assertTrue(state.latestSubmissionByTaskId.isEmpty())
    }

    @Test
    fun refreshingTheSameTeamPreservesItsUnsavedAnswer() = runLocationTasksTest {
        val viewModel = locationTasksViewModel(FakeLocationTasksDataSource())
        viewModel.load()
        advanceUntilIdle()
        viewModel.onAnswerChange(10, "unfinished")
        viewModel.selectTeam(1)
        advanceUntilIdle()
        assertEquals("unfinished", viewModel.uiState.value.answerDrafts[10])
    }

    @Test
    fun reloadFallsBackWhenTheSelectedTeamDisappearsAndClearsItsDraft() = runLocationTasksTest {
        val source = FakeLocationTasksDataSource()
        val viewModel = locationTasksViewModel(source)
        viewModel.load()
        advanceUntilIdle()
        viewModel.onAnswerChange(10, "old team's draft")
        source.teams = listOf(Team(id = 2))
        viewModel.load()
        advanceUntilIdle()
        assertEquals(2L, viewModel.uiState.value.selectedTeamId)
        assertTrue(viewModel.uiState.value.answerDrafts.isEmpty())
    }

    @Test
    fun invalidTeamSelectionCannotRecordAnAttemptForAnotherTeam() = runLocationTasksTest {
        val source = FakeLocationTasksDataSource()
        val viewModel = locationTasksViewModel(source)
        viewModel.load()
        advanceUntilIdle()
        viewModel.selectTeam(999)
        advanceUntilIdle()
        assertEquals(1L, viewModel.uiState.value.selectedTeamId)
        viewModel.onAnswerChange(10, "forty two")
        viewModel.submitAnswer(10)
        advanceUntilIdle()
        assertEquals(1L, source.attempts.single().teamId)
    }

    @Test
    fun repeatedClicksWhileSavingRecordOneAttempt() = runLocationTasksTest {
        val source = FakeLocationTasksDataSource()
        val viewModel = locationTasksViewModel(source)
        viewModel.load()
        advanceUntilIdle()
        viewModel.onAnswerChange(10, "forty two")
        viewModel.submitAnswer(10)
        viewModel.submitAnswer(10)
        advanceUntilIdle()
        assertEquals(1, source.attempts.size)
    }
}

private data class Attempt(val teamId: Long, val taskId: Long, val success: Boolean)

private class FakeLocationTasksDataSource(private val failWrites: Boolean = false) : LocationTasksDataSource {
    val attempts = mutableListOf<Attempt>()
    var teams = listOf(Team(id = 1, additionalScoreAwarded = 5), Team(id = 2))
    override suspend fun load(
        gameId: Long,
        locationId: Long,
    ): LocationTasksScreenData {
        assertEquals(42L, gameId)
        assertEquals(7L, locationId)
        val tasks = listOf(Task(id = 10, text = "Answer?", solution = "forty two"))
        return LocationTasksScreenData(
            tasks,
            teams,
            tasks,
            listOf(Item(id = 3, price = 2)),
        )
    }
    override suspend fun getTaskEvents(teamId: Long) = attempts.filter { it.teamId == teamId }.map {
        TaskEvent(taskId = it.taskId, teamId = teamId, isSuccess = it.success)
    }
    override suspend fun getPurchases(teamId: Long) = if (teamId == 1L) listOf(ShopEntry(itemId = 3)) else emptyList()
    override suspend fun recordAttempt(
        teamId: Long,
        taskId: Long,
        isSuccess: Boolean,
    ) {
        if (failWrites) error("Write failed")
        attempts += Attempt(teamId, taskId, isSuccess)
    }
}

private fun locationTasksViewModel(source: LocationTasksDataSource) = LocationTasksViewModel(42, 7, source)

@OptIn(ExperimentalCoroutinesApi::class)
private fun runLocationTasksTest(block: suspend TestScope.() -> Unit) = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    try {
        block()
    } finally {
        Dispatchers.resetMain()
    }
}
