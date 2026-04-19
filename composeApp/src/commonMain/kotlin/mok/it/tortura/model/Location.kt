package mok.it.tortura.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

data class Location(
    val tasks: Set<Task> = setOf(),
    val miniBoss: Task,
    private val id: Int = -1,
)
