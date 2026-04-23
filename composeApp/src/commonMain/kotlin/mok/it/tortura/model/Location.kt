package mok.it.tortura.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class Location(
    val id: Long? = null,
    val createdAt: Instant? = null,
    val name: String? = null,
    val gameId: Long? = null,
    val tasks: List<Task> = emptyList(),
)
