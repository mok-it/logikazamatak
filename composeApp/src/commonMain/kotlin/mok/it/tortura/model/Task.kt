package mok.it.tortura.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

data class Task(
    val text: String = "",
    val solution: String = "",
    private val id: Int = -1,
)
