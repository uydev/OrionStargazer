package com.example.orionstargazer.astronomy

import android.content.Context
import org.json.JSONArray
import java.io.BufferedReader

object ConstellationCatalog {
    data class ConstellationLine(val aStarId: Int, val bStarId: Int)

    data class Constellation(
        val name: String,
        val lines: List<ConstellationLine>
    )

    @Volatile
    private var cached: List<Constellation>? = null

    fun load(context: Context): List<Constellation> {
        cached?.let { return it }
        return synchronized(this) {
            cached?.let { return it }
            val json = context.assets.open("constellations.json").bufferedReader().use(BufferedReader::readText)
            val arr = JSONArray(json)
            val constellations = buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val name = o.getString("name")
                    val linesArr = o.getJSONArray("lines")
                    val lines = buildList {
                        for (j in 0 until linesArr.length()) {
                            val pair = linesArr.getJSONArray(j)
                            add(ConstellationLine(pair.getInt(0), pair.getInt(1)))
                        }
                    }
                    add(Constellation(name, lines))
                }
            }
            cached = constellations
            constellations
        }
    }
}


