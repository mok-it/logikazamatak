package mok.it.tortura.feature

import kotlin.test.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import mok.it.tortura.model.Student
import mok.it.tortura.model.Team
import mok.it.tortura.model.TeamAssignment

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
                            Student(id = 100, name = "Anna", group = "A", teamId = 10),
                        ),
                    ),
                ),
            ),
        )
        val viewModel = TeamCompositionViewModel(
            activeGameId = 42,
            platformBridge = UnsupportedTeamCompositionPlatformBridge,
            dataSource = dataSource,
            batkabankSource = DisabledBatkabankTeamCompositionSource,
        )

        viewModel.load()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("2", state.baseTeamCounter)
        assertEquals(1, state.teams.size)
        assertEquals("Red", state.teams.single().name)
        assertEquals("A", state.teams.single().group)
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
                name	group	team
                Anna	A	Red
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

    @Test
    fun batkabankSelectionAndImportPopulateDraft() = runTeamCompositionTest {
        val viewModel = TeamCompositionViewModel(
            activeGameId = 42,
            platformBridge = UnsupportedTeamCompositionPlatformBridge,
            dataSource = FakeTeamCompositionDataSource(),
            batkabankSource = FakeBatkabankTeamCompositionSource(),
        )

        viewModel.load()
        advanceUntilIdle()

        assertEquals("2026", viewModel.uiState.value.batkabankYearInput)
        assertEquals(1, viewModel.uiState.value.batkabankAvailableCamps.size)

        val camp = viewModel.uiState.value.batkabankAvailableCamps.single()
        viewModel.selectBatkabankCamp(camp.id)
        val assignment = camp.assignments.single()
        viewModel.importFromBatkabank(camp, assignment)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("1", state.baseTeamCounter)
        assertEquals(1, state.teams.size)
        assertEquals("101", state.teams.single().name)
        assertEquals("A", state.teams.single().group)
        assertTrue(state.message?.contains("Batkabank") == true)
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

private class FakeBatkabankTeamCompositionSource : BatkabankTeamCompositionSource {
    override val isConfigured: Boolean = true

    override suspend fun getImportableCamps(year: Int?): Result<List<CampSearchResultDto>> = Result.success(
        listOf(
            CampSearchResultDto(
                id = "camp-1",
                name = "Sástó 2.",
                startsAt = "2026-07-14",
                endsAt = "2026-07-20",
                assignments = listOf(
                    CampSearchAssignmentDto(
                        id = "lecture",
                        name = "Foglalkozás csoportok",
                        groupCount = 8,
                    ),
                ),
            ),
        ),
    )

    override suspend fun getCampAssignment(
        campId: String,
        assignmentId: String,
    ): Result<CampAssignmentDto> = Result.success(
        CampAssignmentDto(
            campId = campId,
            assignmentId = assignmentId,
            students = listOf(
                CampAssignmentStudentDto(
                    name = "Anna",
                    group = "A",
                    teamName = "101",
                ),
            ),
        ),
    )
}

private object DisabledBatkabankTeamCompositionSource : BatkabankTeamCompositionSource {
    override val isConfigured: Boolean = false

    override suspend fun getImportableCamps(year: Int?): Result<List<CampSearchResultDto>> = Result.success(emptyList())

    override suspend fun getCampAssignment(
        campId: String,
        assignmentId: String,
    ): Result<CampAssignmentDto> = Result.failure(IllegalStateException("disabled"))
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
