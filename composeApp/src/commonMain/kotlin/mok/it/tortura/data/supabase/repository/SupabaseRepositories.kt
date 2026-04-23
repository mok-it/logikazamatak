package mok.it.tortura.data.supabase.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import mok.it.tortura.data.supabase.SupabaseTables
import mok.it.tortura.data.supabase.dto.GameDto
import mok.it.tortura.data.supabase.dto.GameInsertDto
import mok.it.tortura.data.supabase.dto.GameUpdateDto
import mok.it.tortura.data.supabase.dto.HealingLedgerDto
import mok.it.tortura.data.supabase.dto.HealingLedgerInsertDto
import mok.it.tortura.data.supabase.dto.HealingLedgerUpdateDto
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
import mok.it.tortura.data.supabase.dto.TasksLedgerUpdateDto
import mok.it.tortura.data.supabase.dto.TeamAssignmentDto
import mok.it.tortura.data.supabase.dto.TeamAssignmentInsertDto
import mok.it.tortura.data.supabase.dto.TeamAssignmentUpdateDto
import mok.it.tortura.data.supabase.dto.TeamDto
import mok.it.tortura.data.supabase.dto.TeamInsertDto
import mok.it.tortura.data.supabase.dto.TeamUpdateDto

class GameRepository(
    private val client: SupabaseClient,
) {
    suspend fun getAll(): List<GameDto> =
        client.from(SupabaseTables.GAMES).select().decodeList()

    suspend fun getById(id: Long): GameDto? =
        client.from(SupabaseTables.GAMES).select {
            filter { eq("id", id) }
        }.decodeSingleOrNull()

    suspend fun create(game: GameInsertDto): GameDto =
        client.from(SupabaseTables.GAMES).insert(game) { select() }.decodeSingle()

    suspend fun update(id: Long, game: GameUpdateDto): GameDto =
        client.from(SupabaseTables.GAMES).update(game) {
            select()
            filter { eq("id", id) }
        }.decodeSingle()

    suspend fun delete(id: Long) {
        client.from(SupabaseTables.GAMES).delete {
            filter { eq("id", id) }
        }
    }
}

class HealingLedgerRepository(
    private val client: SupabaseClient,
) {
    suspend fun getAll(): List<HealingLedgerDto> =
        client.from(SupabaseTables.HEALING_LEDGER).select().decodeList()

    suspend fun getById(id: Long): HealingLedgerDto? =
        client.from(SupabaseTables.HEALING_LEDGER).select {
            filter { eq("id", id) }
        }.decodeSingleOrNull()

    suspend fun create(entry: HealingLedgerInsertDto): HealingLedgerDto =
        client.from(SupabaseTables.HEALING_LEDGER).insert(entry) { select() }.decodeSingle()

    suspend fun update(id: Long, entry: HealingLedgerUpdateDto): HealingLedgerDto =
        client.from(SupabaseTables.HEALING_LEDGER).update(entry) {
            select()
            filter { eq("id", id) }
        }.decodeSingle()

    suspend fun delete(id: Long) {
        client.from(SupabaseTables.HEALING_LEDGER).delete {
            filter { eq("id", id) }
        }
    }
}

class ItemRepository(
    private val client: SupabaseClient,
) {
    suspend fun getAll(): List<ItemDto> =
        client.from(SupabaseTables.ITEM).select().decodeList()

    suspend fun getById(id: Long): ItemDto? =
        client.from(SupabaseTables.ITEM).select {
            filter { eq("id", id) }
        }.decodeSingleOrNull()

    suspend fun create(item: ItemInsertDto): ItemDto =
        client.from(SupabaseTables.ITEM).insert(item) { select() }.decodeSingle()

    suspend fun update(id: Long, item: ItemUpdateDto): ItemDto =
        client.from(SupabaseTables.ITEM).update(item) {
            select()
            filter { eq("id", id) }
        }.decodeSingle()

    suspend fun delete(id: Long) {
        client.from(SupabaseTables.ITEM).delete {
            filter { eq("id", id) }
        }
    }
}

class ItemEffectRepository(
    private val client: SupabaseClient,
) {
    suspend fun getAll(): List<ItemEffectDto> =
        client.from(SupabaseTables.ITEM_EFFECT).select().decodeList()

    suspend fun getById(id: Long): ItemEffectDto? =
        client.from(SupabaseTables.ITEM_EFFECT).select {
            filter { eq("id", id) }
        }.decodeSingleOrNull()

    suspend fun create(itemEffect: ItemEffectInsertDto): ItemEffectDto =
        client.from(SupabaseTables.ITEM_EFFECT).insert(itemEffect) { select() }.decodeSingle()

    suspend fun update(id: Long, itemEffect: ItemEffectUpdateDto): ItemEffectDto =
        client.from(SupabaseTables.ITEM_EFFECT).update(itemEffect) {
            select()
            filter { eq("id", id) }
        }.decodeSingle()

    suspend fun delete(id: Long) {
        client.from(SupabaseTables.ITEM_EFFECT).delete {
            filter { eq("id", id) }
        }
    }
}

class LocationRepository(
    private val client: SupabaseClient,
) {
    suspend fun getAll(): List<LocationDto> =
        client.from(SupabaseTables.LOCATIONS).select().decodeList()

    suspend fun getById(id: Long): LocationDto? =
        client.from(SupabaseTables.LOCATIONS).select {
            filter { eq("id", id) }
        }.decodeSingleOrNull()

    suspend fun create(location: LocationInsertDto): LocationDto =
        client.from(SupabaseTables.LOCATIONS).insert(location) { select() }.decodeSingle()

    suspend fun update(id: Long, location: LocationUpdateDto): LocationDto =
        client.from(SupabaseTables.LOCATIONS).update(location) {
            select()
            filter { eq("id", id) }
        }.decodeSingle()

    suspend fun delete(id: Long) {
        client.from(SupabaseTables.LOCATIONS).delete {
            filter { eq("id", id) }
        }
    }
}

class ShopRepository(
    private val client: SupabaseClient,
) {
    suspend fun getAll(): List<ShopDto> =
        client.from(SupabaseTables.SHOP).select().decodeList()

    suspend fun getById(id: Long): ShopDto? =
        client.from(SupabaseTables.SHOP).select {
            filter { eq("id", id) }
        }.decodeSingleOrNull()

    suspend fun create(shopEntry: ShopInsertDto): ShopDto =
        client.from(SupabaseTables.SHOP).insert(shopEntry) { select() }.decodeSingle()

    suspend fun update(id: Long, shopEntry: ShopUpdateDto): ShopDto =
        client.from(SupabaseTables.SHOP).update(shopEntry) {
            select()
            filter { eq("id", id) }
        }.decodeSingle()

    suspend fun delete(id: Long) {
        client.from(SupabaseTables.SHOP).delete {
            filter { eq("id", id) }
        }
    }
}

class StudentRepository(
    private val client: SupabaseClient,
) {
    suspend fun getAll(): List<StudentDto> =
        client.from(SupabaseTables.STUDENTS).select().decodeList()

    suspend fun getById(id: Long): StudentDto? =
        client.from(SupabaseTables.STUDENTS).select {
            filter { eq("id", id) }
        }.decodeSingleOrNull()

    suspend fun getByTeamId(teamId: Long): List<StudentDto> =
        client.from(SupabaseTables.STUDENTS).select {
            filter { eq("teamId", teamId) }
        }.decodeList()

    suspend fun create(student: StudentInsertDto): StudentDto =
        client.from(SupabaseTables.STUDENTS).insert(student) { select() }.decodeSingle()

    suspend fun update(id: Long, student: StudentUpdateDto): StudentDto =
        client.from(SupabaseTables.STUDENTS).update(student) {
            select()
            filter { eq("id", id) }
        }.decodeSingle()

    suspend fun delete(id: Long) {
        client.from(SupabaseTables.STUDENTS).delete {
            filter { eq("id", id) }
        }
    }
}

class TaskRepository(
    private val client: SupabaseClient,
) {
    suspend fun getAll(): List<TaskDto> =
        client.from(SupabaseTables.TASKS).select().decodeList()

    suspend fun getById(id: Long): TaskDto? =
        client.from(SupabaseTables.TASKS).select {
            filter { eq("id", id) }
        }.decodeSingleOrNull()

    suspend fun getByGameId(gameId: Long): List<TaskDto> =
        client.from(SupabaseTables.TASKS).select {
            filter { eq("gameId", gameId) }
        }.decodeList()

    suspend fun getByLocationId(locationId: Long): List<TaskDto> =
        client.from(SupabaseTables.TASKS).select {
            filter { eq("locationId", locationId) }
        }.decodeList()

    suspend fun create(task: TaskInsertDto): TaskDto =
        client.from(SupabaseTables.TASKS).insert(task) { select() }.decodeSingle()

    suspend fun update(id: Long, task: TaskUpdateDto): TaskDto =
        client.from(SupabaseTables.TASKS).update(task) {
            select()
            filter { eq("id", id) }
        }.decodeSingle()

    suspend fun delete(id: Long) {
        client.from(SupabaseTables.TASKS).delete {
            filter { eq("id", id) }
        }
    }
}

class TasksLedgerRepository(
    private val client: SupabaseClient,
) {
    suspend fun getAll(): List<TasksLedgerDto> =
        client.from(SupabaseTables.TASKS_LEDGER).select().decodeList()

    suspend fun getById(id: Long): TasksLedgerDto? =
        client.from(SupabaseTables.TASKS_LEDGER).select {
            filter { eq("id", id) }
        }.decodeSingleOrNull()

    suspend fun getByTaskId(taskId: Long): List<TasksLedgerDto> =
        client.from(SupabaseTables.TASKS_LEDGER).select {
            filter { eq("taskId", taskId) }
        }.decodeList()

    suspend fun create(entry: TasksLedgerInsertDto): TasksLedgerDto =
        client.from(SupabaseTables.TASKS_LEDGER).insert(entry) { select() }.decodeSingle()

    suspend fun update(id: Long, entry: TasksLedgerUpdateDto): TasksLedgerDto =
        client.from(SupabaseTables.TASKS_LEDGER).update(entry) {
            select()
            filter { eq("id", id) }
        }.decodeSingle()

    suspend fun delete(id: Long) {
        client.from(SupabaseTables.TASKS_LEDGER).delete {
            filter { eq("id", id) }
        }
    }
}

class TeamAssignmentRepository(
    private val client: SupabaseClient,
) {
    suspend fun getAll(): List<TeamAssignmentDto> =
        client.from(SupabaseTables.TEAM_ASSIGNMENT).select().decodeList()

    suspend fun getById(id: Long): TeamAssignmentDto? =
        client.from(SupabaseTables.TEAM_ASSIGNMENT).select {
            filter { eq("id", id) }
        }.decodeSingleOrNull()

    suspend fun create(teamAssignment: TeamAssignmentInsertDto): TeamAssignmentDto =
        client.from(SupabaseTables.TEAM_ASSIGNMENT).insert(teamAssignment) { select() }.decodeSingle()

    suspend fun update(id: Long, teamAssignment: TeamAssignmentUpdateDto): TeamAssignmentDto =
        client.from(SupabaseTables.TEAM_ASSIGNMENT).update(teamAssignment) {
            select()
            filter { eq("id", id) }
        }.decodeSingle()

    suspend fun delete(id: Long) {
        client.from(SupabaseTables.TEAM_ASSIGNMENT).delete {
            filter { eq("id", id) }
        }
    }
}

class TeamRepository(
    private val client: SupabaseClient,
) {
    suspend fun getAll(): List<TeamDto> =
        client.from(SupabaseTables.TEAMS).select().decodeList()

    suspend fun getById(id: Long): TeamDto? =
        client.from(SupabaseTables.TEAMS).select {
            filter { eq("id", id) }
        }.decodeSingleOrNull()

    suspend fun getByTeamAssignmentId(teamAssignmentId: Long): List<TeamDto> =
        client.from(SupabaseTables.TEAMS).select {
            filter { eq("teamAssignmentId", teamAssignmentId) }
        }.decodeList()

    suspend fun create(team: TeamInsertDto): TeamDto =
        client.from(SupabaseTables.TEAMS).insert(team) { select() }.decodeSingle()

    suspend fun update(id: Long, team: TeamUpdateDto): TeamDto =
        client.from(SupabaseTables.TEAMS).update(team) {
            select()
            filter { eq("id", id) }
        }.decodeSingle()

    suspend fun delete(id: Long) {
        client.from(SupabaseTables.TEAMS).delete {
            filter { eq("id", id) }
        }
    }
}
