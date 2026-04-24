package mok.it.tortura.data.supabase.repository

import io.github.jan.supabase.SupabaseClient
import mok.it.tortura.SupabaseClient as AppSupabaseClient

class TorturaSupabaseRepositories(
    client: SupabaseClient = AppSupabaseClient.client,
) {
    val games = GameRepository(client)
    val healingLedger = HealingLedgerRepository(client)
    val healingTasks = HealingTaskRepository(client)
    val itemEffects = ItemEffectRepository(client)
    val items = ItemRepository(client)
    val locations = LocationRepository(client)
    val shop = ShopRepository(client)
    val students = StudentRepository(client)
    val tasks = TaskRepository(client)
    val tasksLedger = TasksLedgerRepository(client)
    val teamAssignments = TeamAssignmentRepository(client)
    val teams = TeamRepository(client)
}
