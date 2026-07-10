package mok.it.tortura.model

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Location(
    val id: Long? = null,
    val createdAt: Instant? = null,
    val name: String? = null,
    val gameId: Long? = null,
    val tasks: List<Task> = emptyList(),
) {
    val isShop: Boolean
        get() = id == SHOP_ID

    companion object {
        const val SHOP_ID: Long = -1L
        const val SHOP_NAME: String = "Bolt"

        fun shop(gameId: Long?): Location = Location(
            id = SHOP_ID,
            name = SHOP_NAME,
            gameId = gameId,
        )
    }
}
