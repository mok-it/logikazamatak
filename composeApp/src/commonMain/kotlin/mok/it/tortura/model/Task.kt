package mok.it.tortura.model

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Task(
    val id: Long? = null,
    val createdAt: Instant? = null,
    val text: String = "",
    val solution: String = "",
    val isMiniBoss: Boolean? = null,
    val gameId: Long? = null,
    val locationId: Long? = null,
)
