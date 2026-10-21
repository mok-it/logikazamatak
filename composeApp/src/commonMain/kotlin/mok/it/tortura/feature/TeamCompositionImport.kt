package mok.it.tortura.feature

private data class ParsedImportHeader(
    val isHeaderRow: Boolean,
    val nameIndex: Int?,
    val groupIndex: Int?,
    val teamIndex: Int?,
)

class DelimitedTeamCompositionImportSource : TeamCompositionImportSource {

    override fun import(payload: ImportedRosterPayload): TeamCompositionImportResult {
        val delimiter = detectDelimiter(payload.content)
        val rows = parseRows(payload.content.removePrefix("\uFEFF"), delimiter)
            .filter { row -> row.any { it.isNotBlank() } }

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
        val teamBuckets = linkedMapOf<Pair<String, String>, MutableList<StudentDraft>>()

        dataRows.forEachIndexed { index, cells ->
            val rowNumber = if (header.isHeaderRow) index + 2 else index + 1
            if (cells.size != rows.first().size) {
                rowErrors += TeamCompositionRowError(rowNumber, "Eltérő oszlopszám a forrásban.")
                return@forEachIndexed
            }
            val name = header.readValue(cells, Column.NAME)
            val group = header.readValue(cells, Column.GROUP)
            val teamName = header.readValue(cells, Column.TEAM)

            val missingFields = buildList {
                if (name.isBlank()) add("tanuló neve")
                if (group.isBlank()) add("csoport")
                if (teamName.isBlank()) add("csapat")
            }

            if (missingFields.isNotEmpty()) {
                rowErrors += TeamCompositionRowError(
                    rowNumber = rowNumber,
                    message = "Hiányzó mezők: ${missingFields.joinToString()}",
                )
                return@forEachIndexed
            }

            val key = Pair(group, teamName)
            val students = teamBuckets.getOrPut(key) { mutableListOf() }
            students += StudentDraft(name = name)
        }

        val teams = teamBuckets.entries.map { (key, students) ->
            TeamDraft(
                name = key.second,
                group = key.first,
                students = students.toList(),
            )
        }

        val categories = teams
            .map { TeamCategory(group = it.group) }
            .distinct()
            .sortedWith(compareBy { it.group })

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
        val teamIndex = normalized.indexOfFirst { it in TEAM_HEADERS }.takeIf { it >= 0 }

        val hasRecognizedHeaders = listOf(nameIndex, groupIndex, teamIndex).any { it != null }
        if (hasRecognizedHeaders) {
            return ParsedImportHeader(
                isHeaderRow = true,
                nameIndex = nameIndex,
                groupIndex = groupIndex,
                teamIndex = teamIndex,
            )
        }

        return ParsedImportHeader(
            isHeaderRow = false,
            nameIndex = 0,
            groupIndex = 1,
            teamIndex = if (cells.size >= 4) 3 else 2,
        )
    }

    private fun ParsedImportHeader.readValue(
        cells: List<String>,
        column: Column,
    ): String {
        val index = when (column) {
            Column.NAME -> nameIndex
            Column.GROUP -> groupIndex
            Column.TEAM -> teamIndex
        } ?: return ""

        return cells.getOrNull(index)?.trim().orEmpty()
    }

    private fun parseRows(
        text: String,
        delimiter: Char,
    ): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val cells = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var index = 0
        while (index < text.length) {
            val char = text[index]
            when {
                char == '"' && quoted && text.getOrNull(index + 1) == '"' -> {
                    cell.append('"')
                    index++
                }

                char == '"' -> quoted = !quoted

                char == delimiter && !quoted -> {
                    cells += cell.toString().trim()
                    cell.clear()
                }

                (char == '\n' || char == '\r') && !quoted -> {
                    cells += cell.toString().trim()
                    rows += cells.toList()
                    cells.clear()
                    cell.clear()
                    if (char == '\r' && text.getOrNull(index + 1) == '\n') index++
                }

                else -> cell.append(char)
            }
            index++
        }
        require(!quoted) { "Lezáratlan idézőjel az importban." }
        cells += cell.toString().trim()
        rows += cells.toList()
        return rows
    }

    private fun detectDelimiter(text: String): Char {
        val candidates = listOf('\t', ';', ',')
        val counts = IntArray(candidates.size)
        var quoted = false
        for (char in text.trimStart()) {
            if (char == '"') quoted = !quoted
            if (!quoted && (char == '\n' || char == '\r')) break
            if (!quoted) {
                val index = candidates.indexOf(char)
                if (index >= 0) counts[index]++
            }
        }
        return candidates[counts.indices.maxByOrNull { counts[it] } ?: 0]
    }

    private fun normalizeHeader(value: String): String = value
        .trim().removePrefix("\uFEFF")
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
        TEAM,
    }

    private companion object {
        val NAME_HEADERS = setOf("name", "student", "studentname", "tanulo", "diak", "nev")
        val GROUP_HEADERS = setOf("group", "csoport")
        val TEAM_HEADERS = setOf("team", "teamname", "csapat", "csapatnev")
    }
}

class TeamCompositionImportCoordinator(
    private val delimitedSource: TeamCompositionImportSource = DelimitedTeamCompositionImportSource(),
) {
    // The browser converts XLSX worksheets to TSV before passing them to shared code.
    fun import(payload: ImportedRosterPayload): TeamCompositionImportResult = delimitedSource.import(payload)
}
