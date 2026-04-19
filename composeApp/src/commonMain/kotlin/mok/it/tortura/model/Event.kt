package mok.it.tortura.model

import androidx.lifecycle.Lifecycle
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

sealed class Event(
    val id: Int = -1,
    val time: LocalDateTime,
    val userId: String,
)

class TaskEvent(
    id: Int = -1,
    time: LocalDateTime,
    userId: String,
    val task: Task,
    val newStatus: Boolean,
) : Event(
    id = id,
    time = time,
    userId = userId,
)

class HealerEvent(
    id: Int = -1,
    time: LocalDateTime,
    userId: String,
    val task: Task,
    val healedTask: Task,
) : Event(
    id = id,
    time = time,
    userId = userId,
)

// class ShopEvent(
//    id: Int = -1,
//    time: LocalDateTime,
//    userId: String,
//    val item:
// )
