package mok.it.tortura.feature

import kotlin.test.Test
import kotlin.test.assertEquals

class TeamCompositionImportTest {

    private val coordinator = TeamCompositionImportCoordinator()

    @Test
    fun importsTsvWithHeadersIntoTeamsAndCategories() {
        val result = coordinator.import(
            ImportedRosterPayload(
                sourceLabel = "test.tsv",
                content = """
                    name	group	klass	team
                    Anna	A	7	Red
                    Bela	A	7	Red
                    Csenge	B	8	Blue
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
                    name	group	klass	team
                    Dora	C	9	Green
                """.trimIndent(),
                format = TeamCompositionImportFormat.XLSX,
            ),
        )

        assertEquals(1, result.draft.teams.size)
        assertEquals("Green", result.draft.teams.single().name)
    }
}
