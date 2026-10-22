package mok.it.tortura.feature

import kotlin.test.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import mok.it.tortura.model.HealingTask
import mok.it.tortura.model.Team
import mok.it.tortura.model.TeamProgressSummary

@OptIn(ExperimentalCoroutinesApi::class)
class HealerTasksViewModelTest {
    @Test
    fun reloadKeepsAValidSelectionAndClearsARemovedTask() = runHealerTasksTest {
        val source = FakeHealerTasksDataSource()
        val viewModel = HealerTasksViewModel(7, source)
        viewModel.load()
        advanceUntilIdle()
        viewModel.selectHealingTask(20)
        viewModel.load()
        advanceUntilIdle()
        assertEquals(20L, viewModel.uiState.value.selectedHealingTaskId)
        source.tasks = emptyList()
        viewModel.load()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.selectedHealingTaskId)
    }

    @Test
    fun completedHealingRefreshesFailedAttemptsAndTaskHistory() = runHealerTasksTest {
        val source = FakeHealerTasksDataSource()
        val viewModel = HealerTasksViewModel(7, source)
        viewModel.load()
        advanceUntilIdle()
        viewModel.selectHealingTask(20)
        viewModel.completeHealing(20, 30)
        advanceUntilIdle()
        assertEquals(listOf(Triple(7L, 20L, 30L)), source.calls)
        val state = viewModel.uiState.value
        assertTrue(state.healableFailedTasks.isEmpty())
        assertTrue(state.healingTasks.single().isPreviouslyChosen)
        assertNull(state.selectedHealingTaskId)
        assertEquals("Gyógyítás rögzítve", state.message)
        assertFalse(state.isLoading)
    }

    @Test
    fun rejectedHealingKeepsTheFailedAttemptAvailable() = runHealerTasksTest {
        val source = FakeHealerTasksDataSource(failWrites = true)
        val viewModel = HealerTasksViewModel(7, source)
        viewModel.load()
        advanceUntilIdle()
        viewModel.completeHealing(20, 30)
        advanceUntilIdle()
        assertEquals(30L, viewModel.uiState.value.healableFailedTasks.single().ledgerId)
        assertFalse(viewModel.uiState.value.healingTasks.single().isPreviouslyChosen)
        assertEquals("Healing rejected", viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.message)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun repeatedClicksWhileHealingWriteOnce() = runHealerTasksTest {
        val source = FakeHealerTasksDataSource()
        val viewModel = HealerTasksViewModel(7, source)
        viewModel.load()
        advanceUntilIdle()
        viewModel.completeHealing(20, 30)
        viewModel.completeHealing(20, 30)
        advanceUntilIdle()
        assertEquals(1, source.calls.size)
    }

    @Test
    fun loadFailureReportsAnErrorAndStopsLoading() = runHealerTasksTest {
        val source = FakeHealerTasksDataSource(failLoads = true)
        val viewModel = HealerTasksViewModel(7, source)
        viewModel.load()
        advanceUntilIdle()
        assertEquals("Read failed", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}

private class FakeHealerTasksDataSource(
    private val failWrites: Boolean = false,
    private val failLoads: Boolean = false,
) : HealerTasksDataSource {
    var tasks = listOf(HealingTask(id = 20, text = "Sing a song"))
    val calls = mutableListOf<Triple<Long, Long, Long>>()
    private var healed = false
    override suspend fun load(teamId: Long): HealerScreenData {
        assertEquals(7L, teamId)
        if (failLoads) error("Read failed")
        return HealerScreenData(
            Team(id = teamId, name = "Red"),
            tasks.map { HealingTaskListItem(it, healed) },
            if (healed) emptyList() else listOf(FailedTaskAttempt(30, 10, "Failed task")),
            TeamProgressSummary(points = 5, money = 3),
        )
    }
    override suspend fun completeHealing(
        teamId: Long,
        healingTaskId: Long,
        healedTasksLedgerId: Long,
    ) {
        if (failWrites) error("Healing rejected")
        calls += Triple(teamId, healingTaskId, healedTasksLedgerId)
        healed = true
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun runHealerTasksTest(block: suspend TestScope.() -> Unit) = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    try {
        block()
    } finally {
        Dispatchers.resetMain()
    }
}
