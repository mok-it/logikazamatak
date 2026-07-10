package mok.it.tortura.model

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class ItemEffect(
    val id: Long? = null,
    val createdAt: Instant? = null,
    val code: String? = null,
    val description: String? = null,
)
