package mok.it.tortura.model

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class HealingTask(
    val id: Long? = null,
    val createdAt: Instant? = null,
    val text: String = "",
    val solution: String? = null,
    val gameId: Long? = null,
)
