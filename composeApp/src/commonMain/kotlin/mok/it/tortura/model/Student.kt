package mok.it.tortura.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class Student(
    val id: Long? = null,
    val createdAt: Instant? = null,
    val name: String = "",
    val group: String = "",
    val klass: String = "",
    val teamId: Long? = null,
)
