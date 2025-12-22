package com.example.orionstargazer.astronomy

data class Star(
    val name: String,
    val ra: Double,      // Right Ascension, degrees
    val dec: Double,     // Declination, degrees
    val magnitude: Double
)

object DemoStarCatalog {
    // Demo: 10 brightest stars (truncated for brevity, add more as desired)
    val stars = listOf(
        Star("Sirius",    101.2875, -16.7161, -1.46),
        Star("Canopus",   95.9879, -52.6957, -0.72),
        Star("Arcturus",  213.9153, 19.1825, -0.05),
        Star("Alpha Centauri", 219.9021, -60.8339, -0.27),
        Star("Vega",      279.2347, 38.7837, 0.03),
        Star("Capella",   79.1723, 45.9979, 0.08),
        Star("Rigel",     78.6345, -8.2016, 0.12),
        Star("Procyon",   114.8255, 5.225, 0.38),
        Star("Achernar",  24.4286, -57.2368, 0.46),
        Star("Betelgeuse", 88.7929, 7.4071, 0.50),
    )
}
