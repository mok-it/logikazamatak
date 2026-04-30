package mok.it.tortura.feature

private data class ParsedImportHeader(
    val isHeaderRow: Boolean,
    val nameIndex: Int?,
    val groupIndex: Int?,
    val klassIndex: Int?,
    val teamIndex: Int?,
)

class DelimitedTeamCompositionImportSource : TeamCompositionImportSource {

    override fun import(payload: ImportedRosterPayload): TeamCompositionImportResult {
        val delimiter = detectDelimiter(payload.content)
        val rows = payload.content
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { row -> splitRow(row, delimiter) }
            .toList()

        if (rows.isEmpty()) {
            return TeamCompositionImportResult(
                draft = TeamCompositionDraft(),
                preview = TeamCompositionImportPreview(
                    sourceLabel = payload.sourceLabel,
                    teamCount = 0,
                    studentCount = 0,
                    categories = emptyList(),
                    rowErrors = listOf(TeamCompositionRowError(0, "A forrás nem tartalmazott beolvasható sorokat.")),
                ),
            )
        }

        val header = resolveHeader(rows.first())
        val dataRows = if (header.isHeaderRow) rows.drop(1) else rows
        val rowErrors = mutableListOf<TeamCompositionRowError>()
        val teamBuckets = linkedMapOf<Triple<String, String, String>, MutableList<StudentDraft>>()

        dataRows.forEachIndexed { index, cells ->
            val rowNumber = if (header.isHeaderRow) index + 2 else index + 1
            val name = header.readValue(cells, Column.NAME)
            val group = header.readValue(cells, Column.GROUP)
            val klass = header.readValue(cells, Column.KLASS)
            val teamName = header.readValue(cells, Column.TEAM)

            val missingFields = buildList {
                if (name.isBlank()) add("tanuló neve")
                if (group.isBlank()) add("csoport")
                if (klass.isBlank()) add("osztály")
                if (teamName.isBlank()) add("csapat")
            }

            if (missingFields.isNotEmpty()) {
                rowErrors += TeamCompositionRowError(
                    rowNumber = rowNumber,
                    message = "Hiányzó mezők: ${missingFields.joinToString()}",
                )
                return@forEachIndexed
            }

            val key = Triple(group, klass, teamName)
            val students = teamBuckets.getOrPut(key) { mutableListOf() }
            students += StudentDraft(name = name)
        }

        val teams = teamBuckets.entries.map { (key, students) ->
            TeamDraft(
                name = key.third,
                group = key.first,
                klass = key.second,
                students = students.toList(),
            )
        }

        val categories = teams
            .map { TeamCategory(group = it.group, klass = it.klass) }
            .distinct()
            .sortedWith(compareBy<TeamCategory> { it.klass }.thenBy { it.group })

        val draft = TeamCompositionDraft(
            baseTeamCounter = teams.size.toLong(),
            teams = teams,
        )

        return TeamCompositionImportResult(
            draft = draft,
            preview = TeamCompositionImportPreview(
                sourceLabel = payload.sourceLabel,
                teamCount = teams.size,
                studentCount = teams.sumOf { it.students.size },
                categories = categories,
                rowErrors = rowErrors,
            ),
        )
    }

    private fun resolveHeader(cells: List<String>): ParsedImportHeader {
        val normalized = cells.map(::normalizeHeader)
        val nameIndex = normalized.indexOfFirst { it in NAME_HEADERS }.takeIf { it >= 0 }
        val groupIndex = normalized.indexOfFirst { it in GROUP_HEADERS }.takeIf { it >= 0 }
        val klassIndex = normalized.indexOfFirst { it in KLASS_HEADERS }.takeIf { it >= 0 }
        val teamIndex = normalized.indexOfFirst { it in TEAM_HEADERS }.takeIf { it >= 0 }

        val hasRecognizedHeaders = listOf(nameIndex, groupIndex, klassIndex, teamIndex).any { it != null }
        if (hasRecognizedHeaders) {
            return ParsedImportHeader(
                isHeaderRow = true,
                nameIndex = nameIndex,
                groupIndex = groupIndex,
                klassIndex = klassIndex,
                teamIndex = teamIndex,
            )
        }

        return ParsedImportHeader(
            isHeaderRow = false,
            nameIndex = 0,
            groupIndex = 1,
            klassIndex = 2,
            teamIndex = 3,
        )
    }

    private fun ParsedImportHeader.readValue(
        cells: List<String>,
        column: Column,
    ): String {
        val index = when (column) {
            Column.NAME -> nameIndex
            Column.GROUP -> groupIndex
            Column.KLASS -> klassIndex
            Column.TEAM -> teamIndex
        } ?: return ""

        return cells.getOrNull(index)?.trim().orEmpty()
    }

    private fun splitRow(
        row: String,
        delimiter: Char,
    ): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false

        row.forEach { char ->
            when {
                char == '"' -> inQuotes = !inQuotes
                char == delimiter && !inQuotes -> {
                    result += current.toString().trim()
                    current.clear()
                }
                else -> current.append(char)
            }
        }

        result += current.toString().trim()
        return result.map { it.removeSurrounding("\"") }
    }

    private fun detectDelimiter(text: String): Char {
        val firstLine = text.lineSequence().firstOrNull().orEmpty()
        val candidates = listOf('\t', ';', ',')
        return candidates.maxByOrNull { candidate -> firstLine.count { it == candidate } } ?: ','
    }

    private fun normalizeHeader(value: String): String = value
        .trim()
        .lowercase()
        .replace("á", "a")
        .replace("é", "e")
        .replace("í", "i")
        .replace("ó", "o")
        .replace("ö", "o")
        .replace("ő", "o")
        .replace("ú", "u")
        .replace("ü", "u")
        .replace("ű", "u")
        .replace(" ", "")
        .replace("_", "")

    private enum class Column {
        NAME,
        GROUP,
        KLASS,
        TEAM,
    }

    private companion object {
        val NAME_HEADERS = setOf("name", "student", "studentname", "tanulo", "diak", "nev")
        val GROUP_HEADERS = setOf("group", "csoport")
        val KLASS_HEADERS = setOf("klass", "class", "grade", "osztaly", "evfolyam")
        val TEAM_HEADERS = setOf("team", "teamname", "csapat", "csapatnev")
    }
}

class XlsxTeamCompositionImportSource(
    private val delegate: DelimitedTeamCompositionImportSource = DelimitedTeamCompositionImportSource(),
) : TeamCompositionImportSource {
    override fun import(payload: ImportedRosterPayload): TeamCompositionImportResult = delegate.import(payload)
}

class TeamCompositionImportCoordinator(
    private val delimitedSource: TeamCompositionImportSource = DelimitedTeamCompositionImportSource(),
    private val xlsxSource: TeamCompositionImportSource = XlsxTeamCompositionImportSource(),
) {
    fun import(payload: ImportedRosterPayload): TeamCompositionImportResult = when (payload.format) {
        TeamCompositionImportFormat.DELIMITED_TEXT -> delimitedSource.import(payload)
        TeamCompositionImportFormat.XLSX -> xlsxSource.import(payload)
    }
}
