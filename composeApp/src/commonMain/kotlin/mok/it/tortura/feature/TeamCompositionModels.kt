package mok.it.tortura.feature

import kotlinx.serialization.Serializable
import mok.it.tortura.model.Student
import mok.it.tortura.model.Team
import mok.it.tortura.model.TeamAssignment

data class StudentDraft(
    val id: Long? = null,
    val name: String = "",
)

data class TeamDraft(
    val id: Long? = null,
    val name: String = "",
    val group: String = "",
    val students: List<StudentDraft> = emptyList(),
)

data class TeamCompositionSnapshot(
    val assignment: TeamAssignment? = null,
    val teams: List<Team> = emptyList(),
)

data class TeamCompositionDraft(
    val baseTeamCounter: Long? = null,
    val teams: List<TeamDraft> = emptyList(),
)

data class TeamCategory(
    val group: String,
)

data class TeamCompositionRowError(
    val rowNumber: Int,
    val message: String,
)

data class TeamCompositionImportPreview(
    val sourceLabel: String,
    val teamCount: Int,
    val studentCount: Int,
    val categories: List<TeamCategory>,
    val rowErrors: List<TeamCompositionRowError>,
)

data class TeamCompositionImportResult(
    val draft: TeamCompositionDraft,
    val preview: TeamCompositionImportPreview,
)

@Serializable
enum class TeamCompositionImportFormat {
    DELIMITED_TEXT,
    XLSX,
}

@Serializable
data class ImportedRosterPayload(
    val sourceLabel: String,
    val content: String,
    val format: TeamCompositionImportFormat,
)

interface TeamCompositionImportSource {
    fun import(payload: ImportedRosterPayload): TeamCompositionImportResult
}

interface TeamCompositionPlatformBridge {
    val supportsClipboardRead: Boolean
    val supportsFileImport: Boolean

    suspend fun readClipboardText(): Result<String>
    suspend fun pickRosterFile(): Result<ImportedRosterPayload>
}

object UnsupportedTeamCompositionPlatformBridge : TeamCompositionPlatformBridge {
    override val supportsClipboardRead: Boolean = false
    override val supportsFileImport: Boolean = false

    override suspend fun readClipboardText(): Result<String> =
        Result.failure(IllegalStateException("A vágólap olvasása ezen a platformon nem támogatott."))

    override suspend fun pickRosterFile(): Result<ImportedRosterPayload> =
        Result.failure(IllegalStateException("A fájlimport ezen a platformon nem támogatott."))
}

expect fun teamCompositionPlatformBridge(): TeamCompositionPlatformBridge

fun TeamCompositionSnapshot.toDraft(): TeamCompositionDraft = TeamCompositionDraft(
    baseTeamCounter = assignment?.baseTeamCounter,
    teams = teams.map { it.toDraft() },
)

private fun Team.toDraft(): TeamDraft {
    val firstStudent = students.firstOrNull()
    return TeamDraft(
        id = id,
        name = name.orEmpty(),
        group = firstStudent?.group.orEmpty(),
        students = students.map { student ->
            StudentDraft(
                id = student.id,
                name = student.name,
            )
        },
    )
}

fun TeamDraft.toStudents(): List<Student> = students.map { student ->
    Student(
        id = student.id,
        name = student.name.trim(),
        group = group.trim(),
    )
}
