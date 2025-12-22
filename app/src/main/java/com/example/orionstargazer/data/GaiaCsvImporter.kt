package com.example.orionstargazer.data

import android.content.Context
import com.example.orionstargazer.data.entities.StarEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GaiaCsvImporter {
    suspend fun importCsvToRoom(context: Context, repo: StarRepository) = withContext(Dispatchers.IO) {
        context.assets.open("gaia_brightest.csv").bufferedReader().use { reader ->
            val header = reader.readLine() ?: return@withContext
            val colIndex = header.split(',').mapIndexed { i, name -> name.trim() to i }.toMap()
            val stars = mutableListOf<StarEntity>()

            reader.forEachLine { line ->
                if (line.isBlank()) return@forEachLine
                val cols = line.split(',')
                val idStr = cols.getOrNull(colIndex["source_id"] ?: -1)?.trim()
                if (idStr.isNullOrEmpty()) return@forEachLine

                try {
                    // Parse required Gaia CSV cols
                    val sourceId = idStr.toLongOrNull() ?: return@forEachLine
                    val ra = cols.getOrNull(colIndex["ra"] ?: -1)?.trim()?.toDoubleOrNull() ?: return@forEachLine
                    val dec = cols.getOrNull(colIndex["dec"] ?: -1)?.trim()?.toDoubleOrNull() ?: return@forEachLine
                    val mag =
                        cols.getOrNull(colIndex["phot_g_mean_mag"] ?: -1)?.trim()?.toDoubleOrNull() ?: return@forEachLine

                    // Gaia `source_id` doesn't fit in Int; fold Long -> Int deterministically.
                    val folded = (sourceId xor (sourceId ushr 32)).toInt()
                    var id = folded and Int.MAX_VALUE
                    // Avoid collisions with small hand-curated catalog IDs (1..9999).
                    if (id < 10_000) id += 10_000

                    stars.add(
                        StarEntity(
                            id = id,
                            name = "Gaia-$sourceId",
                            ra = ra,
                            dec = dec,
                            magnitude = mag,
                            distance = null,
                            spectralType = null,
                            constellation = null
                        )
                    )
                } catch (_: Exception) {
                    // Skip malformed rows
                    return@forEachLine
                }
            }

            if (stars.isNotEmpty()) repo.insertAll(stars)
        }
    }
}
