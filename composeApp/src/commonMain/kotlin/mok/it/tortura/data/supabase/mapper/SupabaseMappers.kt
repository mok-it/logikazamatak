package mok.it.tortura.data.supabase.mapper

import mok.it.tortura.data.supabase.dto.GameDto
import mok.it.tortura.data.supabase.dto.GameInsertDto
import mok.it.tortura.data.supabase.dto.GameUpdateDto
import mok.it.tortura.data.supabase.dto.HealingLedgerDto
import mok.it.tortura.data.supabase.dto.HealingLedgerInsertDto
import mok.it.tortura.data.supabase.dto.HealingTaskDto
import mok.it.tortura.data.supabase.dto.HealingTaskInsertDto
import mok.it.tortura.data.supabase.dto.HealingTaskUpdateDto
import mok.it.tortura.data.supabase.dto.ItemDto
import mok.it.tortura.data.supabase.dto.ItemEffectDto
import mok.it.tortura.data.supabase.dto.ItemEffectInsertDto
import mok.it.tortura.data.supabase.dto.ItemEffectUpdateDto
import mok.it.tortura.data.supabase.dto.ItemInsertDto
import mok.it.tortura.data.supabase.dto.ItemUpdateDto
import mok.it.tortura.data.supabase.dto.LocationDto
import mok.it.tortura.data.supabase.dto.LocationInsertDto
import mok.it.tortura.data.supabase.dto.LocationUpdateDto
import mok.it.tortura.data.supabase.dto.ShopDto
import mok.it.tortura.data.supabase.dto.ShopInsertDto
import mok.it.tortura.data.supabase.dto.ShopUpdateDto
import mok.it.tortura.data.supabase.dto.StudentDto
import mok.it.tortura.data.supabase.dto.StudentInsertDto
import mok.it.tortura.data.supabase.dto.StudentUpdateDto
import mok.it.tortura.data.supabase.dto.TaskDto
import mok.it.tortura.data.supabase.dto.TaskInsertDto
import mok.it.tortura.data.supabase.dto.TaskUpdateDto
import mok.it.tortura.data.supabase.dto.TasksLedgerDto
import mok.it.tortura.data.supabase.dto.TasksLedgerInsertDto
import mok.it.tortura.data.supabase.dto.TeamAssignmentDto
import mok.it.tortura.data.supabase.dto.TeamAssignmentInsertDto
import mok.it.tortura.data.supabase.dto.TeamAssignmentUpdateDto
import mok.it.tortura.data.supabase.dto.TeamDto
import mok.it.tortura.data.supabase.dto.TeamInsertDto
import mok.it.tortura.data.supabase.dto.TeamUpdateDto
import mok.it.tortura.model.Game
import mok.it.tortura.model.HealerEvent
import mok.it.tortura.model.HealingTask
import mok.it.tortura.model.Item
import mok.it.tortura.model.ItemEffect
import mok.it.tortura.model.Location
import mok.it.tortura.model.ShopEntry
import mok.it.tortura.model.Student
import mok.it.tortura.model.Task
import mok.it.tortura.model.TaskEvent
import mok.it.tortura.model.Team
import mok.it.tortura.model.TeamAssignment

fun GameDto.toModel(
    locations: List<Location> = emptyList(),
    tasks: List<Task> = emptyList(),
): Game = Game(
    id = id,
    createdAt = createdAt,
    name = name,
    locations = locations,
    tasks = tasks,
)

fun Game.toInsertDto(): GameInsertDto = GameInsertDto(
    name = name,
)

fun Game.toUpdateDto(): GameUpdateDto = GameUpdateDto(
    name = name,
)

fun HealingTaskDto.toModel(): HealingTask = HealingTask(
    id = id,
    createdAt = createdAt,
    text = text.orEmpty(),
    solution = solution,
    gameId = gameId,
)

fun HealingTask.toInsertDto(): HealingTaskInsertDto = HealingTaskInsertDto(
    text = text,
    solution = solution,
    gameId = gameId,
)

fun HealingTask.toUpdateDto(): HealingTaskUpdateDto = HealingTaskUpdateDto(
    text = text,
    solution = solution,
    gameId = gameId,
)

fun StudentDto.toModel(): Student = Student(
    id = id,
    createdAt = createdAt,
    name = name.orEmpty(),
    group = group.orEmpty(),
    klass = klass.orEmpty(),
    teamId = teamId,
)

fun Student.toInsertDto(): StudentInsertDto = StudentInsertDto(
    name = name,
    group = group,
    klass = klass,
    teamId = teamId,
)

fun Student.toUpdateDto(): StudentUpdateDto = StudentUpdateDto(
    name = name,
    group = group,
    klass = klass,
    teamId = teamId,
)

fun TaskDto.toModel(): Task = Task(
    id = id,
    createdAt = createdAt,
    text = text.orEmpty(),
    solution = solution.orEmpty(),
    isMiniBoss = isMiniBoss,
    gameId = gameId,
    locationId = locationId,
)

fun Task.toInsertDto(): TaskInsertDto = TaskInsertDto(
    text = text,
    solution = solution,
    isMiniBoss = isMiniBoss,
    gameId = gameId,
    locationId = locationId,
)

fun Task.toUpdateDto(): TaskUpdateDto = TaskUpdateDto(
    text = text,
    solution = solution,
    isMiniBoss = isMiniBoss,
    gameId = gameId,
    locationId = locationId,
)

fun TeamDto.toModel(
    students: List<Student> = emptyList(),
): Team = Team(
    id = id,
    createdAt = createdAt,
    name = name,
    teamAssignmentId = teamAssignmentId,
    students = students,
)

fun Team.toInsertDto(): TeamInsertDto = TeamInsertDto(
    name = name,
    teamAssignmentId = teamAssignmentId,
)

fun Team.toUpdateDto(): TeamUpdateDto = TeamUpdateDto(
    name = name,
    teamAssignmentId = teamAssignmentId,
)

fun TeamAssignmentDto.toModel(
    teams: List<Team> = emptyList(),
): TeamAssignment = TeamAssignment(
    id = id,
    createdAt = createdAt,
    baseTeamCounter = baseTeamCounter,
    gameId = gameId,
    teams = teams,
)

fun TeamAssignment.toInsertDto(): TeamAssignmentInsertDto = TeamAssignmentInsertDto(
    baseTeamCounter = baseTeamCounter,
    gameId = gameId,
)

fun TeamAssignment.toUpdateDto(): TeamAssignmentUpdateDto = TeamAssignmentUpdateDto(
    baseTeamCounter = baseTeamCounter,
    gameId = gameId,
)

fun LocationDto.toModel(
    tasks: List<Task> = emptyList(),
): Location = Location(
    id = id,
    createdAt = createdAt,
    name = name,
    gameId = gameId,
    tasks = tasks,
)

fun Location.toInsertDto(): LocationInsertDto = LocationInsertDto(
    name = name,
    gameId = gameId,
)

fun Location.toUpdateDto(): LocationUpdateDto = LocationUpdateDto(
    name = name,
    gameId = gameId,
)

fun ItemDto.toModel(): Item = Item(
    id = id,
    createdAt = createdAt,
    name = name,
    price = price,
    itemEffectId = itemEffectId,
)

fun Item.toInsertDto(): ItemInsertDto = ItemInsertDto(
    name = name,
    price = price,
    itemEffectId = itemEffectId,
)

fun Item.toUpdateDto(): ItemUpdateDto = ItemUpdateDto(
    name = name,
    price = price,
    itemEffectId = itemEffectId,
)

fun ItemEffectDto.toModel(): ItemEffect = ItemEffect(
    id = id,
    createdAt = createdAt,
    description = description,
)

fun ItemEffect.toInsertDto(): ItemEffectInsertDto = ItemEffectInsertDto(
    description = description,
)

fun ItemEffect.toUpdateDto(): ItemEffectUpdateDto = ItemEffectUpdateDto(
    description = description,
)

fun TasksLedgerDto.toModel(
    task: Task? = null,
): TaskEvent = TaskEvent(
    id = id,
    createdAt = createdAt,
    userId = userId,
    teamId = teamId,
    taskId = taskId,
    isSuccess = isSuccess,
    task = task,
)

fun TaskEvent.toInsertDto(): TasksLedgerInsertDto = TasksLedgerInsertDto(
    taskId = taskId,
    teamId = teamId,
    userId = userId,
    isSuccess = isSuccess,
)

fun HealingLedgerDto.toModel(
    healingTask: HealingTask? = null,
    healedTask: Task? = null,
): HealerEvent = HealerEvent(
    id = id,
    createdAt = createdAt,
    userId = userId,
    teamId = teamId,
    healingTaskId = healingTaskId,
    healedTasksLedgerId = healedTasksLedgerId,
    healingTask = healingTask,
    healedTask = healedTask,
)

fun HealerEvent.toInsertDto(): HealingLedgerInsertDto = HealingLedgerInsertDto(
    teamId = teamId,
    healingTaskId = healingTaskId,
    healedTasksLedgerId = healedTasksLedgerId,
    userId = userId,
)

fun ShopDto.toModel(): ShopEntry = ShopEntry(
    id = id,
    createdAt = createdAt,
    itemId = itemId,
    targetId = targetId,
    userId = userId,
)

fun ShopEntry.toInsertDto(): ShopInsertDto = ShopInsertDto(
    itemId = itemId,
    targetId = targetId,
    userId = userId,
)

fun ShopEntry.toUpdateDto(): ShopUpdateDto = ShopUpdateDto(
    itemId = itemId,
    targetId = targetId,
    userId = userId,
)
