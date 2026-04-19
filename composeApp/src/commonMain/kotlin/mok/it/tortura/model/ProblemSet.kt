package mok.it.tortura.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class ProblemSet(
    @Transient
    val locations: List<Location> = emptyList(),
    val mainBoss: Task,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: Int = -1,
)
