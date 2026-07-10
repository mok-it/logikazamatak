package mok.it.tortura.model

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Student(
    val id: Long? = null,
    val createdAt: Instant? = null,
    val name: String = "",
    val group: String = "",
    val teamId: Long? = null,
)
