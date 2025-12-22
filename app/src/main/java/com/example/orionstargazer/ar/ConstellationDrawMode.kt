package com.example.orionstargazer.ar

/**
 * How constellation lines should be rendered.
 *
 * - DETECTED: only the best-matching constellation (clean UX)
 * - NEARBY: nearby lines from any constellation (atlas/exploration UX)
 * - HYBRID: detected constellation highlighted + faint nearby context
 */
enum class ConstellationDrawMode {
    DETECTED,
    NEARBY,
    HYBRID
}

