package com.example.orionstargazer.data

import android.content.Context
import com.example.orionstargazer.data.entities.StarEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader

object StarAssetLoader {
    suspend fun loadAssetsAndSeedDb(context: Context, repo: StarRepository) = withContext(Dispatchers.IO) {
        val json = context.assets.open("stars.json").bufferedReader().use(BufferedReader::readText)
        val arr = JSONArray(json)
        val stars = mutableListOf<StarEntity>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            stars.add(
                StarEntity(
                    id = o.getInt("id"),
                    name = o.getString("name"),
                    ra = o.getDouble("ra"),
                    dec = o.getDouble("dec"),
                    magnitude = o.getDouble("magnitude"),
                    distance = o.optDouble("distance", Double.NaN).let { if (it.isNaN()) null else it },
                    spectralType = if (o.has("spectralType")) o.getString("spectralType") else null,
                    constellation = if (o.has("constellation")) o.getString("constellation") else null
                )
            )
        }
        repo.insertAll(stars)
    }
}

