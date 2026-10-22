package mok.it.tortura.feature

import kotlin.test.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import mok.it.tortura.model.Team

@OptIn(ExperimentalCoroutinesApi::class)
class LedgerViewModelTest {
    @Test
    fun reversalStatusKeepsKindsSeparateAndPreservesHistory() {
        val task = LedgerEntry(1, LedgerKind.Task, "Task")
        val shop = LedgerEntry(1, LedgerKind.Shop, "Shop")
        val reversal = LedgerEntry(2, LedgerKind.Task, "Undo task", reversesId = 1)
        val result = listOf(task, shop, reversal).withReversalStatus()
        assertEquals(3, result.size)
        assertFalse(result.single { it.kind == LedgerKind.Task && it.id == 1L }.canUndo)
        assertTrue(result.single { it.kind == LedgerKind.Shop }.canUndo)
        assertFalse(result.single { it.reversesId != null }.canUndo)
        assertFalse(task.copy(automatic = true).canUndo)
    }

    @Test
    fun undoRefreshesHistoryAndRejectsASecondClickWhileBusy() = runLedgerTest {
        val source = FakeLedgerDataSource()
        val viewModel = LedgerViewModel(42, dataSource = source)
        viewModel.load()
        advanceUntilIdle()
        val entry = viewModel.uiState.value.entries.single()
        viewModel.undo(entry)
        viewModel.undo(entry)
        advanceUntilIdle()
        assertEquals(1, source.undoCalls)
        assertEquals(2, viewModel.uiState.value.entries.size)
        assertFalse(viewModel.uiState.value.entries.first { it.id == 1L }.canUndo)
        assertEquals("Visszavonás rögzítve", viewModel.uiState.value.message)
        assertFalse(viewModel.uiState.value.busy)
    }

    @Test
    fun rejectedUndoPreservesEntriesAndReportsError() = runLedgerTest {
        val source = FakeLedgerDataSource(failUndo = true)
        val viewModel = LedgerViewModel(42, dataSource = source)
        viewModel.load()
        advanceUntilIdle()
        viewModel.undo(viewModel.uiState.value.entries.single())
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.entries.size)
        assertTrue(viewModel.uiState.value.entries.single().canUndo)
        assertEquals("Dependent purchase", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.busy)
    }

    @Test
    fun selectingAnUnknownTeamDoesNotReadItsLedger() = runLedgerTest {
        val source = FakeLedgerDataSource()
        val viewModel = LedgerViewModel(42, initialTeamId = 999, dataSource = source)
        viewModel.load()
        advanceUntilIdle()
        viewModel.selectTeam(999)
        advanceUntilIdle()
        assertEquals(7L, viewModel.uiState.value.selectedTeamId)
        assertEquals(listOf(7L), source.loadedTeamIds)
    }
}

private class FakeLedgerDataSource(private val failUndo: Boolean = false) : LedgerDataSource {
    var undoCalls = 0
    val loadedTeamIds = mutableListOf<Long>()
    private var entries = listOf(LedgerEntry(1, LedgerKind.Task, "Task"))

    override suspend fun loadTeams(gameId: Long) = listOf(Team(id = 7, name = "Red"))
    override suspend fun loadEntries(
        gameId: Long,
        teamId: Long,
    ): List<LedgerEntry> {
        loadedTeamIds += teamId
        return entries.withReversalStatus()
    }
    override suspend fun undo(
        teamId: Long,
        entry: LedgerEntry,
    ) {
        undoCalls++
        if (failUndo) error("Dependent purchase")
        entries = entries + LedgerEntry(2, LedgerKind.Task, "Undo", reversesId = entry.id)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun runLedgerTest(block: suspend TestScope.() -> Unit) = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    try {
        block()
    } finally {
        Dispatchers.resetMain()
    }
}
