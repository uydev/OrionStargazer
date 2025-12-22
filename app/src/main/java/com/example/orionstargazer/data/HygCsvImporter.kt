package com.example.orionstargazer.data

import android.content.Context
import com.example.orionstargazer.data.entities.StarEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.PriorityQueue

object HygCsvImporter {
    private const val ASSET_CSV = "hygdata_v42.csv"

    /**
     * Imports the ~[limit] brightest stars from the HYG database into Room.
     *
     * HYG fields we use:
     * - ra (hours) -> degrees
     * - dec (degrees)
     * - mag (apparent magnitude)
     * - dist (parsec) -> light years
     * - spect (spectral type)
     * - con (constellation abbreviation)
     * - proper/bf/bayer/flam for names
     */
    suspend fun importTopBrightest(context: Context, repo: StarRepository, limit: Int = 3000) =
        withContext(Dispatchers.IO) {
            val top = PriorityQueue<StarEntity>(compareByDescending { it.magnitude }) // worst mag on top

            context.assets.open(ASSET_CSV).use { raw ->
                raw.bufferedReader().use { reader ->
                    val headerLine = reader.readLine() ?: return@withContext
                    val headers = parseCsvLine(headerLine)
                    val idx = headers.withIndex().associate { it.value to it.index }

                    fun col(cols: List<String>, name: String): String? =
                        cols.getOrNull(idx[name] ?: -1)?.trim()?.takeIf { it.isNotEmpty() }

                    reader.lineSequence().forEach { line ->
                        if (line.isBlank()) return@forEach
                        val cols = parseCsvLine(line)

                        val hygId = col(cols, "id")?.toIntOrNull() ?: return@forEach
                        val raHours = col(cols, "ra")?.toDoubleOrNull() ?: return@forEach
                        val decDeg = col(cols, "dec")?.toDoubleOrNull() ?: return@forEach
                        val mag = col(cols, "mag")?.toDoubleOrNull() ?: return@forEach

                        val proper = col(cols, "proper")
                        val bf = col(cols, "bf")
                        val bayer = col(cols, "bayer")
                        val flam = col(cols, "flam")
                        val con = col(cols, "con")
                        val spect = col(cols, "spect")

                        val name = when {
                            !proper.isNullOrBlank() -> proper
                            !bf.isNullOrBlank() -> bf
                            !bayer.isNullOrBlank() && !con.isNullOrBlank() -> "$bayer $con"
                            !flam.isNullOrBlank() && !con.isNullOrBlank() -> "$flam $con"
                            else -> "HYG-$hygId"
                        }

                        val distPc = col(cols, "dist")?.toDoubleOrNull()
                        val distLy = distPc?.takeIf { it.isFinite() && it > 0.0 }?.let { it * 3.26156 }

                        // Avoid collisions with your hand-curated IDs (1..9999). Keep stable IDs.
                        val id = 20_000 + hygId

                        val star = StarEntity(
                            id = id,
                            name = name,
                            ra = raHours * 15.0,
                            dec = decDeg,
                            magnitude = mag,
                            distance = distLy,
                            spectralType = spect,
                            constellation = con
                        )

                        if (top.size < limit) {
                            top.add(star)
                        } else {
                            val worst = top.peek() ?: return@forEach
                            if (star.magnitude < worst.magnitude) {
                                top.poll()
                                top.add(star)
                            }
                        }
                    }
                }
            }

            val stars = top.toList().sortedBy { it.magnitude }
            if (stars.isNotEmpty()) repo.insertAll(stars)
        }

    /**
     * Minimal CSV parser supporting quotes.
     * HYG doesn't usually embed commas in fields, but this keeps us safe.
     */
    private fun parseCsvLine(line: String): List<String> {
        val out = ArrayList<String>(40)
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    // Handle escaped quotes: ""
                    val nextIsQuote = i + 1 < line.length && line[i + 1] == '"'
                    if (inQuotes && nextIsQuote) {
                        sb.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    out.add(sb.toString())
                    sb.setLength(0)
                }
                else -> sb.append(c)
            }
            i++
        }
        out.add(sb.toString())
        return out
    }
}


