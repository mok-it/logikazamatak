package mok.it.tortura

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseClient {

    private const val SUPABASE_URL = "https://nnhvnqpczerqrofxkbki.supabase.co"
    private const val SUPABASE_PUBLISHABLE_KEY = "sb_publishable_NyBL91k2rrDYgnpKWvYhGA_cXhLoVAq"

    val client by lazy {
        check(!SUPABASE_URL.contains("YOUR_PROJECT")) {
            "Replace SUPABASE_URL in SupabaseClient.kt with your actual project URL."
        }
        check(SUPABASE_PUBLISHABLE_KEY != "YOUR_PUBLISHABLE_KEY") {
            "Replace SUPABASE_PUBLISHABLE_KEY in SupabaseClient.kt with your publishable key."
        }

        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_PUBLISHABLE_KEY,
        ) {
            install(Postgrest)
        }
    }
}
