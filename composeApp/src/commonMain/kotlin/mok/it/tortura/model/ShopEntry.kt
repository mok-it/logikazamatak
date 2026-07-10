package mok.it.tortura.model

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class ShopEntry(
    val id: Long? = null,
    val createdAt: Instant? = null,
    val itemId: Long? = null,
    val targetId: Long? = null,
    val teamId: Long? = null,
    val userId: Long? = null,
)
