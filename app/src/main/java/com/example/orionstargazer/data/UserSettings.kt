package com.example.orionstargazer.data

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_settings")

object UserSettings {
    private val KEY_MAX_MAGNITUDE = doublePreferencesKey("max_magnitude")

    /** Default roughly matches naked-eye suburban-ish sky. */
    const val DEFAULT_MAX_MAGNITUDE: Double = 6.0

    fun maxMagnitudeFlow(context: Context): Flow<Double> =
        context.dataStore.data.map { prefs ->
            prefs[KEY_MAX_MAGNITUDE] ?: DEFAULT_MAX_MAGNITUDE
        }

    suspend fun setMaxMagnitude(context: Context, value: Double) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MAX_MAGNITUDE] = value
        }
    }
}


