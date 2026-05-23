package com.ledgeros.app.data.remote

import com.ledgeros.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

/**
 * Lazy Supabase client singleton.
 *
 * Returns null if SUPABASE_URL or SUPABASE_ANON_KEY are not configured in
 * local.properties — the app then runs in local-only mode (Room + FastAPI only).
 * All callers must null-check before use.
 */
object SupabaseClientProvider {

    val client: SupabaseClient? by lazy {
        val url = BuildConfig.SUPABASE_URL
        val key = BuildConfig.SUPABASE_ANON_KEY
        if (url.isBlank() || key.isBlank()) return@lazy null

        createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = key,
        ) {
            install(Auth)
            install(Postgrest)
            install(Storage)
        }
    }

    /** True only when both keys are present in BuildConfig. */
    val isConfigured: Boolean get() = client != null
}
