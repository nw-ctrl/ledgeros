package com.ledgeros.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.ledgeros.app.ui.state.SettingsUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ledgeros_settings")

class SettingsDataStore(private val context: Context) {
    private object Keys {
        val DETERMINISTIC_FIRST = booleanPreferencesKey("deterministic_first")
        val MASK_SENSITIVE = booleanPreferencesKey("mask_sensitive")
        val FALLBACK_OCR = booleanPreferencesKey("fallback_ocr")
        val INNOVATION_MODE = booleanPreferencesKey("innovation_mode")
    }

    val settingsFlow: Flow<SettingsUiState> = context.dataStore.data.map { prefs ->
        SettingsUiState(
            deterministicFirst = prefs[Keys.DETERMINISTIC_FIRST] ?: true,
            maskSensitiveIdentifiers = prefs[Keys.MASK_SENSITIVE] ?: true,
            fallbackOcrProvider = prefs[Keys.FALLBACK_OCR] ?: false,
            innovationMode = prefs[Keys.INNOVATION_MODE] ?: false,
        )
    }

    suspend fun save(settings: SettingsUiState) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DETERMINISTIC_FIRST] = settings.deterministicFirst
            prefs[Keys.MASK_SENSITIVE] = settings.maskSensitiveIdentifiers
            prefs[Keys.FALLBACK_OCR] = settings.fallbackOcrProvider
            prefs[Keys.INNOVATION_MODE] = settings.innovationMode
        }
    }
}
