package mok.it.tortura.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class ItemEffect(
    val id: Long? = null,
    val createdAt: Instant? = null,
    val description: String? = null,
)
