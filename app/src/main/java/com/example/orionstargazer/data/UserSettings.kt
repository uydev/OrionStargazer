package com.example.orionstargazer.data

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.example.orionstargazer.ar.ConstellationDrawMode
import com.example.orionstargazer.ar.StarRenderMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_settings")

object UserSettings {
    private val KEY_MAX_MAGNITUDE = doublePreferencesKey("max_magnitude")
    private val KEY_STAR_RENDER_MODE = stringPreferencesKey("star_render_mode")
    private val KEY_SHADER_MAX_STARS = intPreferencesKey("shader_max_stars")
    private val KEY_CONSTELLATION_DRAW_MODE = stringPreferencesKey("constellation_draw_mode")

    /** Default roughly matches naked-eye suburban-ish sky. */
    const val DEFAULT_MAX_MAGNITUDE: Double = 6.0
    val DEFAULT_STAR_RENDER_MODE: StarRenderMode = StarRenderMode.GLOW_TEXTURE
    const val DEFAULT_SHADER_MAX_STARS: Int = 1200
    val DEFAULT_CONSTELLATION_DRAW_MODE: ConstellationDrawMode = ConstellationDrawMode.DETECTED

    fun maxMagnitudeFlow(context: Context): Flow<Double> =
        context.dataStore.data.map { prefs ->
            prefs[KEY_MAX_MAGNITUDE] ?: DEFAULT_MAX_MAGNITUDE
        }

    suspend fun setMaxMagnitude(context: Context, value: Double) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MAX_MAGNITUDE] = value
        }
    }

    fun starRenderModeFlow(context: Context): Flow<StarRenderMode> =
        context.dataStore.data.map { prefs ->
            val raw = prefs[KEY_STAR_RENDER_MODE]
            raw?.let {
                runCatching { StarRenderMode.valueOf(it) }.getOrNull()
            } ?: DEFAULT_STAR_RENDER_MODE
        }

    suspend fun setStarRenderMode(context: Context, mode: StarRenderMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_STAR_RENDER_MODE] = mode.name
        }
    }

    fun shaderMaxStarsFlow(context: Context): Flow<Int> =
        context.dataStore.data.map { prefs ->
            (prefs[KEY_SHADER_MAX_STARS] ?: DEFAULT_SHADER_MAX_STARS)
                .coerceIn(200, 2500)
        }

    suspend fun setShaderMaxStars(context: Context, value: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SHADER_MAX_STARS] = value.coerceIn(200, 2500)
        }
    }

    fun constellationDrawModeFlow(context: Context): Flow<ConstellationDrawMode> =
        context.dataStore.data.map { prefs ->
            val raw = prefs[KEY_CONSTELLATION_DRAW_MODE]
            runCatching { raw?.let { ConstellationDrawMode.valueOf(it) } }.getOrNull()
                ?: DEFAULT_CONSTELLATION_DRAW_MODE
        }

    suspend fun setConstellationDrawMode(context: Context, mode: ConstellationDrawMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CONSTELLATION_DRAW_MODE] = mode.name
        }
    }
}


