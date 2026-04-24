package mok.it.tortura.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

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
