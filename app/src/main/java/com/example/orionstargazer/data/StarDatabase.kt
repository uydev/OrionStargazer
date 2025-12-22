package com.example.orionstargazer.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.orionstargazer.data.dao.StarDao
import com.example.orionstargazer.data.entities.StarEntity

@Database(entities = [StarEntity::class], version = 2, exportSchema = false)
abstract class StarDatabase : RoomDatabase() {
    abstract fun starDao(): StarDao
}

