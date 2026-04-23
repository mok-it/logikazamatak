package mok.it.tortura.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.time.Instant

@Serializable
data class Team(
    val id: Long? = null,
    val createdAt: Instant? = null,
    val name: String? = null,
    val teamAssignmentId: Long? = null,
    @Transient
    val students: List<Student> = emptyList(),
)
