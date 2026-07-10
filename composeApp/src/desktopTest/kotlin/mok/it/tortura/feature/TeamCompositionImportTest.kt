package mok.it.tortura.feature

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TeamCompositionImportTest {

    private val coordinator = TeamCompositionImportCoordinator()

    @Test
    fun importsTsvWithHeadersIntoTeamsAndCategories() {
        val result = coordinator.import(
            ImportedRosterPayload(
                sourceLabel = "test.tsv",
                content = """
                    name	group	team
                    Anna	A	Red
                    Bela	A	Red
                    Csenge	B	Blue
                """.trimIndent(),
                format = TeamCompositionImportFormat.DELIMITED_TEXT,
            ),
        )

        assertEquals(2L, result.draft.baseTeamCounter)
        assertEquals(2, result.draft.teams.size)
        assertEquals(3, result.preview.studentCount)
        assertEquals(listOf("Red", "Blue"), result.draft.teams.map { it.name })
    }

    @Test
    fun xlsxImportUsesNormalizedDelimitedContent() {
        val result = coordinator.import(
            ImportedRosterPayload(
                sourceLabel = "test.xlsx",
                content = """
                    name	group	team
                    Dora	C	Green
                """.trimIndent(),
                format = TeamCompositionImportFormat.XLSX,
            ),
        )

        assertEquals(1, result.draft.teams.size)
        assertEquals("Green", result.draft.teams.single().name)
    }

    @Test
    fun batkabankAssignmentImportGroupsStudentsByLectureGroupAndTeam() {
        val result = BatkabankAssignmentImportMapper().import(
            sourceLabel = "Batkabank",
            assignment = CampAssignmentDto(
                campId = "camp-1",
                assignmentId = "assignment-1",
                students = listOf(
                    CampAssignmentStudentDto(name = "Anna", group = "A", teamName = "101"),
                    CampAssignmentStudentDto(name = "Bela", group = "A", teamName = "101"),
                    CampAssignmentStudentDto(name = "Csenge", group = "B", teamName = null),
                ),
            ),
        )

        assertEquals(1L, result.draft.baseTeamCounter)
        assertEquals(1, result.draft.teams.size)
        assertEquals("101", result.draft.teams.single().name)
        assertEquals("A", result.draft.teams.single().group)
        assertEquals(2, result.draft.teams.single().students.size)
        assertTrue(result.preview.rowErrors.single().message.contains("nincs csapatba sorolva"))
    }
}
