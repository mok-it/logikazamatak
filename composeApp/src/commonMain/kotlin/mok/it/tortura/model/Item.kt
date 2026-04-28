package mok.it.tortura.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

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
