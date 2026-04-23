package mok.it.tortura.data.supabase.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class GameDto(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: Long? = null,
    @SerialName("created_at")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val createdAt: Instant? = null,
    val name: String? = null,
)

@Serializable
data class GameInsertDto(
    val name: String? = null,
)

@Serializable
data class GameUpdateDto(
    val name: String? = null,
)

@Serializable
data class HealingLedgerDto(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: Long? = null,
    @SerialName("created_at")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val createdAt: Instant? = null,
    val taskId: Long? = null,
    val healedTaskId: Long? = null,
    val userId: Long? = null,
)

@Serializable
data class HealingLedgerInsertDto(
    val taskId: Long? = null,
    val healedTaskId: Long? = null,
    val userId: Long? = null,
)

@Serializable
data class HealingLedgerUpdateDto(
    val taskId: Long? = null,
    val healedTaskId: Long? = null,
    val userId: Long? = null,
)

@Serializable
data class ItemDto(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: Long? = null,
    @SerialName("created_at")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val createdAt: Instant? = null,
    val name: String? = null,
    val price: Int? = null,
    val itemEffectId: Long? = null,
)

@Serializable
data class ItemInsertDto(
    val name: String? = null,
    val price: Int? = null,
    val itemEffectId: Long? = null,
)

@Serializable
data class ItemUpdateDto(
    val name: String? = null,
    val price: Int? = null,
    val itemEffectId: Long? = null,
)

@Serializable
data class ItemEffectDto(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: Long? = null,
    @SerialName("created_at")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val createdAt: Instant? = null,
    val description: String? = null,
)

@Serializable
data class ItemEffectInsertDto(
    val description: String? = null,
)

@Serializable
data class ItemEffectUpdateDto(
    val description: String? = null,
)

@Serializable
data class LocationDto(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: Long? = null,
    @SerialName("created_at")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val createdAt: Instant? = null,
    val name: String? = null,
    val gameId: Long? = null,
)

@Serializable
data class LocationInsertDto(
    val name: String? = null,
    val gameId: Long? = null,
)

@Serializable
data class LocationUpdateDto(
    val name: String? = null,
    val gameId: Long? = null,
)

@Serializable
data class ShopDto(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: Long? = null,
    @SerialName("created_at")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val createdAt: Instant? = null,
    val itemId: Long? = null,
    val targetId: Long? = null,
    val userId: Long? = null,
)

@Serializable
data class ShopInsertDto(
    val itemId: Long? = null,
    val targetId: Long? = null,
    val userId: Long? = null,
)

@Serializable
data class ShopUpdateDto(
    val itemId: Long? = null,
    val targetId: Long? = null,
    val userId: Long? = null,
)

@Serializable
data class StudentDto(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: Long? = null,
    @SerialName("created_at")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val createdAt: Instant? = null,
    val name: String? = null,
    val group: String? = null,
    val klass: String? = null,
    val teamId: Long? = null,
)

@Serializable
data class StudentInsertDto(
    val name: String? = null,
    val group: String? = null,
    val klass: String? = null,
    val teamId: Long? = null,
)

@Serializable
data class StudentUpdateDto(
    val name: String? = null,
    val group: String? = null,
    val klass: String? = null,
    val teamId: Long? = null,
)

@Serializable
data class TaskDto(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: Long? = null,
    @SerialName("created_at")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val createdAt: Instant? = null,
    val text: String? = null,
    val solution: String? = null,
    val isMiniBoss: Boolean? = null,
    val gameId: Long? = null,
    val locationId: Long? = null,
)

@Serializable
data class TaskInsertDto(
    val text: String? = null,
    val solution: String? = null,
    val isMiniBoss: Boolean? = null,
    val gameId: Long? = null,
    val locationId: Long? = null,
)

@Serializable
data class TaskUpdateDto(
    val text: String? = null,
    val solution: String? = null,
    val isMiniBoss: Boolean? = null,
    val gameId: Long? = null,
    val locationId: Long? = null,
)

@Serializable
data class TasksLedgerDto(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: Long? = null,
    @SerialName("created_at")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val createdAt: Instant? = null,
    val taskId: Long,
    val userId: Long? = null,
    val isSuccess: Boolean? = null,
)

@Serializable
data class TasksLedgerInsertDto(
    val taskId: Long,
    val userId: Long? = null,
    val isSuccess: Boolean? = null,
)

@Serializable
data class TasksLedgerUpdateDto(
    val taskId: Long? = null,
    val userId: Long? = null,
    val isSuccess: Boolean? = null,
)

@Serializable
data class TeamAssignmentDto(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: Long? = null,
    @SerialName("created_at")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val createdAt: Instant? = null,
    val baseTeamCounter: Long? = null,
    val gameId: Long? = null,
)

@Serializable
data class TeamAssignmentInsertDto(
    val baseTeamCounter: Long? = null,
    val gameId: Long? = null,
)

@Serializable
data class TeamAssignmentUpdateDto(
    val baseTeamCounter: Long? = null,
    val gameId: Long? = null,
)

@Serializable
data class TeamDto(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val id: Long? = null,
    @SerialName("created_at")
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val createdAt: Instant? = null,
    val name: String? = null,
    val teamAssignmentId: Long? = null,
)

@Serializable
data class TeamInsertDto(
    val name: String? = null,
    val teamAssignmentId: Long? = null,
)

@Serializable
data class TeamUpdateDto(
    val name: String? = null,
    val teamAssignmentId: Long? = null,
)
