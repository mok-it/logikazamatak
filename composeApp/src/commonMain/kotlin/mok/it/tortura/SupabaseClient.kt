package mok.it.tortura

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClient {

    val client by lazy {
        check(AppConfig.SUPABASE_URL.isNotBlank()) {
            "Supabase URL is blank for environment '${AppConfig.SUPABASE_ENVIRONMENT}'."
        }
        check(AppConfig.SUPABASE_KEY.isNotBlank()) {
            "Supabase key is blank for environment '${AppConfig.SUPABASE_ENVIRONMENT}'."
        }

        createSupabaseClient(
            supabaseUrl = AppConfig.SUPABASE_URL,
            supabaseKey = AppConfig.SUPABASE_KEY,
        ) {
            install(Postgrest)
        }
    }
}
