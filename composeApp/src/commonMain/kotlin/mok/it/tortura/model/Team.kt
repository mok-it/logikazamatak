package mok.it.tortura.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Team(
    @Transient
    val students: List<Student> = emptyList(),
    val name: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    private val id: Int = -1,
)
