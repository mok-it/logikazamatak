package mok.it.logikazamatak.data.supabase

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import mok.it.logikazamatak.data.supabase.repository.LogikazamatakSupabaseRepositories

class SupabaseConnectionTest {

    @Test
    fun canQueryGamesWhenLiveSupabaseSmokeTestIsEnabled() = runTest {
        if (System.getenv("RUN_SUPABASE_CONNECTION_TEST") != "true") {
            return@runTest
        }

        val games = LogikazamatakSupabaseRepositories().games.getAll()

        assertNotNull(games)
    }
}
