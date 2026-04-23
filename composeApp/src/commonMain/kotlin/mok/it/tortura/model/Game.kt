package mok.it.tortura.model

import kotlin.time.Instant

data class Game(
    val id: Long? = null,
    val createdAt: Instant? = null,
    val name: String? = null,
    val locations: List<Location> = emptyList(),
    val tasks: List<Task> = emptyList(),
)
