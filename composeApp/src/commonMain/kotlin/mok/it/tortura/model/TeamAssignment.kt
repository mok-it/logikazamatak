package mok.it.tortura.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class TeamAssignment(
    @Transient
    val teams: List<Team> = emptyList(),
    val baseTeamId: Int = 100,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: Int = -1,
)
