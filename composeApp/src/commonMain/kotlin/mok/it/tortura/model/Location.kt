package mok.it.tortura.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Location(
    @Transient
    val tasks: List<Task> = listOf(),
    val miniBoss: Task,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    private val id: Int = -1,
)
