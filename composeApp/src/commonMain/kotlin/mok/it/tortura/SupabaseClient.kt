package mok.it.tortura

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClient {

    val client by lazy {
        check(SupabaseConfig.URL.isNotBlank()) {
            "Supabase URL is blank for environment '${SupabaseConfig.ENVIRONMENT}'."
        }
        check(SupabaseConfig.KEY.isNotBlank()) {
            "Supabase key is blank for environment '${SupabaseConfig.ENVIRONMENT}'."
        }

        createSupabaseClient(
            supabaseUrl = SupabaseConfig.URL,
            supabaseKey = SupabaseConfig.KEY,
        ) {
            install(Postgrest)
        }
    }
}
