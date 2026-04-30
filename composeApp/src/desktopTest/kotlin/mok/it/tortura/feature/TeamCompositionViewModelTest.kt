package mok.it.tortura.feature

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mok.it.tortura.model.Student
import mok.it.tortura.model.Team
import mok.it.tortura.model.TeamAssignment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class TeamCompositionViewModelTest {

    @Test
    fun loadPopulatesDraftFromSavedSnapshot() = runTeamCompositionTest {
        val dataSource = FakeTeamCompositionDataSource(
            snapshot = TeamCompositionSnapshot(
                assignment = TeamAssignment(id = 1, baseTeamCounter = 2, gameId = 42),
                teams = listOf(
                    Team(
                        id = 10,
                        name = "Red",
                        teamAssignmentId = 1,
                        students = listOf(
                            Student(id = 100, name = "Anna", group = "A", klass = "7", teamId = 10),
                        ),
                    ),
                ),
            ),
        )
        val viewModel = TeamCompositionViewModel(
            activeGameId = 42,
            platformBridge = UnsupportedTeamCompositionPlatformBridge,
            dataSource = dataSource,
        )

        viewModel.load()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("2", state.baseTeamCounter)
        assertEquals(1, state.teams.size)
        assertEquals("Red", state.teams.single().name)
        assertEquals("A", state.teams.single().group)
        assertEquals("7", state.teams.single().klass)
        assertEquals("Csapatbeosztás betöltve", state.message)
        assertFalse(state.isLoading)
    }

    @Test
    fun importFromTextCreatesDraftTeams() = runTeamCompositionTest {
        val viewModel = TeamCompositionViewModel(
            activeGameId = 42,
            platformBridge = UnsupportedTeamCompositionPlatformBridge,
            dataSource = FakeTeamCompositionDataSource(),
        )

        viewModel.onImportTextChange(
            """
                name	group	klass	team
                Anna	A	7	Red
                Bela	A	7	Red
            """.trimIndent(),
        )
        viewModel.importFromText()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("1", state.baseTeamCounter)
        assertEquals(1, state.teams.size)
        assertEquals(2, state.teams.single().students.size)
        assertEquals("Beillesztett szöveg", state.importPreview?.sourceLabel)
    }

    @Test
    fun savePersistsValidatedDraft() = runTeamCompositionTest {
        val dataSource = FakeTeamCompositionDataSource()
        val viewModel = TeamCompositionViewModel(
            activeGameId = 42,
            platformBridge = UnsupportedTeamCompositionPlatformBridge,
            dataSource = dataSource,
        )

        viewModel.onBaseTeamCounterChange("1")
        viewModel.addTeam()
        viewModel.updateTeamName(0, "Red")
        viewModel.updateTeamGroup(0, "A")
        viewModel.updateTeamKlass(0, "7")
        viewModel.updateStudentName(0, 0, "Anna")
        viewModel.save()
        advanceUntilIdle()

        assertEquals(42L, dataSource.savedGameId)
        assertEquals(1L, dataSource.savedDraft?.baseTeamCounter)
        assertEquals("Csapatbeosztás elmentve", viewModel.uiState.value.message)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun saveRejectsInvalidDraft() = runTeamCompositionTest {
        val dataSource = FakeTeamCompositionDataSource()
        val viewModel = TeamCompositionViewModel(
            activeGameId = 42,
            platformBridge = UnsupportedTeamCompositionPlatformBridge,
            dataSource = dataSource,
        )

        viewModel.save()
        advanceUntilIdle()

        assertEquals("Az alap csapatszám legyen pozitív egész szám.", viewModel.uiState.value.errorMessage)
        assertNull(dataSource.savedDraft)
    }
}

private class FakeTeamCompositionDataSource(
    private val snapshot: TeamCompositionSnapshot = TeamCompositionSnapshot(),
) : TeamCompositionDataSource {
    var savedGameId: Long? = null
    var savedDraft: TeamCompositionDraft? = null

    override suspend fun loadTeamComposition(gameId: Long): TeamCompositionSnapshot = snapshot

    override suspend fun saveTeamComposition(
        gameId: Long,
        draft: TeamCompositionDraft,
    ): TeamCompositionSnapshot {
        savedGameId = gameId
        savedDraft = draft
        return TeamCompositionSnapshot(
            assignment = TeamAssignment(id = 9, baseTeamCounter = draft.baseTeamCounter, gameId = gameId),
            teams = draft.teams.mapIndexed { index, team ->
                Team(
                    id = index.toLong() + 1,
                    name = team.name,
                    students = team.toStudents(),
                )
            },
        )
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun runTeamCompositionTest(block: suspend kotlinx.coroutines.test.TestScope.() -> Unit) = runTest {
    Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    try {
        block()
    } finally {
        Dispatchers.resetMain()
    }
}
