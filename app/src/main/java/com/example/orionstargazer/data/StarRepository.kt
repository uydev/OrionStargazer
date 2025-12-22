package com.example.orionstargazer.data

import android.content.Context
import androidx.room.Room
import com.example.orionstargazer.data.dao.StarDao
import com.example.orionstargazer.data.entities.StarEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StarRepository private constructor(context: Context) {
    private val db = Room.databaseBuilder(
        context.applicationContext,
        StarDatabase::class.java,
        "stars.db"
    )
        // This app seeds from assets on startup; destructive migration is acceptable for MVP.
        .fallbackToDestructiveMigration()
        .build()
    private val starDao: StarDao = db.starDao()

    suspend fun getAllStarsAsync(): List<StarEntity> = withContext(Dispatchers.IO) {
        starDao.getVisibleStarsByDec()
    }

    suspend fun getAllStars(): List<StarEntity> = withContext(Dispatchers.IO) {
        starDao.getAll()
    }

    suspend fun countStars(): Int = withContext(Dispatchers.IO) {
        starDao.countAll()
    }

    suspend fun getCandidates(
        maxMagnitude: Double,
        minDec: Double,
        maxDec: Double
    ): List<StarEntity> = withContext(Dispatchers.IO) {
        starDao.getVisibleStarsByDec(
            maxMagnitude = maxMagnitude,
            minDec = minDec,
            maxDec = maxDec
        )
    }

    suspend fun insertAll(stars: List<StarEntity>) = withContext(Dispatchers.IO) {
        starDao.insertAll(stars)
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        starDao.deleteAll()
    }

    suspend fun getByName(name: String): StarEntity? = withContext(Dispatchers.IO) {
        starDao.getStarByName(name)
    }

    companion object {
        @Volatile private var instance: StarRepository? = null
        fun getInstance(context: Context): StarRepository =
            instance ?: synchronized(this) {
                instance ?: StarRepository(context).also { instance = it }
            }
    }
}

