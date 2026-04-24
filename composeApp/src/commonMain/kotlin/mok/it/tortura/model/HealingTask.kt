package mok.it.tortura.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class HealingTask(
    val id: Long? = null,
    val createdAt: Instant? = null,
    val text: String = "",
    val solution: String? = null,
    val gameId: Long? = null,
)
