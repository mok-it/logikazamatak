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
    val teamId: Long? = null,
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
    val teamId: Long? = null,
    val healingTaskId: Long? = null,
    val healedTasksLedgerId: Long? = null,
    val healingTask: HealingTask? = null,
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
