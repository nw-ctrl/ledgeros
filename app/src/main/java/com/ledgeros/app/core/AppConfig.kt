package com.ledgeros.app.core

import com.ledgeros.app.BuildConfig

/**
 * Central access point for compile-time config values sourced from local.properties.
 * All keys are injected via BuildConfig — never hardcoded here.
 */
object AppConfig {
    val supabaseUrl: String get() = BuildConfig.SUPABASE_URL
    val supabaseAnonKey: String get() = BuildConfig.SUPABASE_ANON_KEY
    val backendUrl: String get() = BuildConfig.BACKEND_URL

    // Future: add GROQ_API_KEY and FIREBASE_SERVER_KEY here when needed

    val hasSupabaseConfig: Boolean
        get() = supabaseUrl.isNotBlank() && supabaseAnonKey.isNotBlank()
}
