package mok.it.tortura.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

@Serializable
data class Task(
    val text: String = "",
    val solution: String = "",
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    private val id: Int = -1,
)
