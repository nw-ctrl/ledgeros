package com.ledgeros.app.core

object AppConfig {
    const val SUPABASE_URL = "SUPABASE_URL"
    const val SUPABASE_ANON_KEY = "SUPABASE_ANON_KEY"
    const val GROQ_API_KEY = "GROQ_API_KEY"
    const val FIREBASE_SERVER_KEY = "FIREBASE_SERVER_KEY"

    val hasSupabaseConfig: Boolean
        get() = SUPABASE_URL.isNotBlank() && SUPABASE_ANON_KEY.isNotBlank()
}
