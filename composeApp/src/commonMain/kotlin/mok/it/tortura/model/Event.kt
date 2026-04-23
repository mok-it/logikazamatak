package mok.it.tortura.model

import kotlin.time.Instant

sealed class Event(
    val id: Long? = null,
    val createdAt: Instant? = null,
    val userId: Long? = null,
)

class TaskEvent(
    id: Long? = null,
    createdAt: Instant? = null,
    userId: Long? = null,
    val taskId: Long,
    val isSuccess: Boolean? = null,
    val task: Task? = null,
) : Event(
    id = id,
    createdAt = createdAt,
    userId = userId,
)

class HealerEvent(
    id: Long? = null,
    createdAt: Instant? = null,
    userId: Long? = null,
    val taskId: Long? = null,
    val healedTaskId: Long? = null,
    val task: Task? = null,
    val healedTask: Task? = null,
) : Event(
    id = id,
    createdAt = createdAt,
    userId = userId,
)

// class ShopEvent(
//    id: Int = -1,
//    time: LocalDateTime,
//    userId: String,
//    val item:
// )
