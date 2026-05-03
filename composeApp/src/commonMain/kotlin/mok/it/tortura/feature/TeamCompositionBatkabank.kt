package mok.it.tortura.feature

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mok.it.tortura.AppConfig

@Serializable
data class CampSearchResultDto(
    val id: String,
    val name: String,
    val startsAt: String? = null,
    val endsAt: String? = null,
    val assignments: List<CampSearchAssignmentDto> = emptyList(),
)

@Serializable
data class CampSearchAssignmentDto(
    val id: String,
    val name: String,
    val groupCount: Int,
)

@Serializable
data class CampRosterStudentDto(
    val name: String,
    val group: String,
    val teamName: String? = null,
)

@Serializable
data class CampRosterDto(
    val campId: String,
    val assignmentId: String,
    val students: List<CampRosterStudentDto> = emptyList(),
)

@Serializable
data class ApiErrorResponse(
    val error: ApiErrorPayload,
)

@Serializable
data class ApiErrorPayload(
    val code: String,
    val message: String,
    val details: ApiErrorDetails? = null,
)

@Serializable
data class ApiErrorDetails(
    val parameter: String? = null,
    val groupId: String? = null,
    val errors: ZodError? = null,
    val campId: String? = null,
    val assignmentId: String? = null,
)

@Serializable
data class ZodError(
    val _errors: List<String> = emptyList(),
    val name: ZodFieldError? = null,
)

@Serializable
data class ZodFieldError(
    val _errors: List<String> = emptyList(),
)

@Serializable
private data class CampSearchResponseDto(
    val camps: List<CampSearchResultDto> = emptyList(),
)

@Serializable
private data class CampYearsResponseDto(
    val years: List<Int> = emptyList(),
)

interface BatkabankTeamCompositionSource {
    val isConfigured: Boolean

    suspend fun getAvailableYears(): Result<List<Int>>
    suspend fun getCamps(year: Int): Result<List<CampSearchResultDto>>
    suspend fun getCampRoster(
        campId: String,
        assignmentId: String,
    ): Result<CampRosterDto>
}

class FirebaseBatkabankTeamCompositionSource(
    private val baseUrl: String = AppConfig.BATKABANK_API_BASE_URL,
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val client: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    },
) : BatkabankTeamCompositionSource {

    override val isConfigured: Boolean = baseUrl.isNotBlank()

    override suspend fun getAvailableYears(): Result<List<Int>> = runCatching {
        checkConfiguration()
        val response = client.get(url("getCampYears"))
        val body = response.body<String>()
        ensureSuccess(response.status, body)
        json.decodeFromString<CampYearsResponseDto>(body).years
    }

    override suspend fun getCamps(year: Int): Result<List<CampSearchResultDto>> = runCatching {
        checkConfiguration()
        val response = client.post(url("getCampsByYear")) {
            contentType(ContentType.Application.Json)
            setBody(mapOf("year" to year))
        }
        val body = response.body<String>()
        ensureSuccess(response.status, body)
        json.decodeFromString<CampSearchResponseDto>(body).camps
    }

    override suspend fun getCampRoster(
        campId: String,
        assignmentId: String,
    ): Result<CampRosterDto> = runCatching {
        checkConfiguration()
        val response = client.post(url("getCampRoster")) {
            contentType(ContentType.Application.Json)
            setBody(mapOf("campId" to campId, "assignmentId" to assignmentId))
        }
        val body = response.body<String>()
        ensureSuccess(response.status, body)
        json.decodeFromString<CampRosterDto>(body)
    }

    private fun checkConfiguration() {
        check(isConfigured) {
            "A Batkabank API nincs beállítva ehhez a buildhez."
        }
    }

    private fun url(path: String): String = "${baseUrl.trimEnd('/')}/$path"

    private fun ensureSuccess(
        status: HttpStatusCode,
        body: String,
    ) {
        if (status.value in 200..299) return

        val apiError = runCatching { json.decodeFromString<ApiErrorResponse>(body) }.getOrNull()
        if (apiError != null) {
            val message = buildString {
                append(apiError.error.message)
                val details = apiError.error.details
                if (details != null) {
                    val errors = mutableListOf<String>()
                    details.parameter?.let { errors.add("Parameter: $it") }
                    details.campId?.let { errors.add("Camp ID: $it") }
                    details.assignmentId?.let { errors.add("Assignment ID: $it") }
                    details.groupId?.let { errors.add("Group ID: $it") }
                    details.errors?.let { zodError ->
                        zodError._errors.forEach { errors.add(it) }
                        zodError.name?._errors?.forEach { errors.add("Name: $it") }
                    }

                    if (errors.isNotEmpty()) {
                        append(" (")
                        append(errors.joinToString(", "))
                        append(")")
                    }
                }
            }
            throw IllegalStateException(message)
        }

        throw IllegalStateException("A Batkabank API ${status.value} hibával válaszolt.")
    }
}

class BatkabankRosterImportMapper {

    fun import(
        sourceLabel: String,
        roster: CampRosterDto,
    ): TeamCompositionImportResult {
        val rowErrors = mutableListOf<TeamCompositionRowError>()
        val teamBuckets = linkedMapOf<Pair<String, String>, MutableList<StudentDraft>>()

        roster.students.forEachIndexed { index, student ->
            val rowNumber = index + 1
            val normalizedName = student.name.trim()
            val normalizedGroup = student.group.trim()
            val normalizedTeamName = student.teamName?.trim().orEmpty()

            if (normalizedName.isBlank()) {
                rowErrors += TeamCompositionRowError(rowNumber, "Hiányzó mező: tanuló neve")
                return@forEachIndexed
            }
            if (normalizedGroup.isBlank()) {
                rowErrors += TeamCompositionRowError(rowNumber, "Hiányzó mező: csoport")
                return@forEachIndexed
            }
            if (normalizedTeamName.isBlank()) {
                rowErrors += TeamCompositionRowError(
                    rowNumber,
                    "A tanuló nincs csapatba sorolva a kiválasztott Batkabank feladatban.",
                )
                return@forEachIndexed
            }

            val students = teamBuckets.getOrPut(normalizedGroup to normalizedTeamName) {
                mutableListOf()
            }
            students += StudentDraft(name = normalizedName)
        }

        val teams = teamBuckets.entries.map { (key, students) ->
            TeamDraft(
                name = key.second,
                group = key.first,
                klass = "",
                students = students.toList(),
            )
        }

        val categories = teams
            .map { TeamCategory(group = it.group, klass = it.klass) }
            .distinct()
            .sortedBy { it.group }

        return TeamCompositionImportResult(
            draft = TeamCompositionDraft(
                baseTeamCounter = teams.size.toLong(),
                teams = teams,
            ),
            preview = TeamCompositionImportPreview(
                sourceLabel = sourceLabel,
                teamCount = teams.size,
                studentCount = teams.sumOf { it.students.size },
                categories = categories,
                rowErrors = rowErrors,
            ),
        )
    }
}
