package com.example.orionstargazer.ar

/**
 * Visual rendering modes for stars.
 *
 * - GLOW_TEXTURE: current “glowy sprite” look (best on most devices)
 * - SOLID: simple unlit colored dot (fastest / lowest overdraw)
 * - CUSTOM_SHADER_GLOW: reserved for a future procedural glow shader
 */
enum class StarRenderMode {
    AUTO,
    GLOW_TEXTURE,
    SOLID,
    CUSTOM_SHADER_GLOW
}

