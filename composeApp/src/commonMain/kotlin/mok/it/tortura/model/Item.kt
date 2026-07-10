package mok.it.tortura.model

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Item(
    val id: Long? = null,
    val createdAt: Instant? = null,
    val name: String? = null,
    val price: Int? = null,
    val itemEffectId: Long? = null,
    val gameId: Long? = null,
    val maxPerTeam: Int? = null,
)
