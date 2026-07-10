package mok.it.tortura.model

import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Team(
    val id: Long? = null,
    val createdAt: Instant? = null,
    val name: String? = null,
    val teamAssignmentId: Long? = null,
    val additionalScoreAwarded: Int = 0,
    @Transient
    val students: List<Student> = emptyList(),
)
