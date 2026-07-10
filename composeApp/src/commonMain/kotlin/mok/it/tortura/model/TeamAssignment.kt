package mok.it.tortura.model

import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class TeamAssignment(
    val id: Long? = null,
    val createdAt: Instant? = null,
    val baseTeamCounter: Long? = null,
    val gameId: Long? = null,
    @Transient
    val teams: List<Team> = emptyList(),
)
