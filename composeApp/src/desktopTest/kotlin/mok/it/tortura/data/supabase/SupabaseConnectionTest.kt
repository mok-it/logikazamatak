package mok.it.tortura.data.supabase

import kotlinx.coroutines.test.runTest
import mok.it.tortura.data.supabase.repository.TorturaSupabaseRepositories
import kotlin.test.Test
import kotlin.test.assertNotNull

class SupabaseConnectionTest {

    @Test
    fun canQueryGamesWhenLiveSupabaseSmokeTestIsEnabled() = runTest {
        if (System.getenv("RUN_SUPABASE_CONNECTION_TEST") != "true") {
            return@runTest
        }

        val games = TorturaSupabaseRepositories().games.getAll()

        assertNotNull(games)
    }
}
