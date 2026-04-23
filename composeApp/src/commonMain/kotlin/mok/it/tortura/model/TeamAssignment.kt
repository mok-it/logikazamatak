package mok.it.tortura.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.time.Instant

@Serializable
data class TeamAssignment(
    val id: Long? = null,
    val createdAt: Instant? = null,
    val baseTeamCounter: Long? = null,
    @Transient
    val teams: List<Team> = emptyList(),
)
