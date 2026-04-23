package mok.it.tortura.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class ShopEntry(
    val id: Long? = null,
    val createdAt: Instant? = null,
    val itemId: Long? = null,
    val targetId: Long? = null,
    val userId: Long? = null,
)
