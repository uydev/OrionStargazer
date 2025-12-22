package com.example.orionstargazer.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.orionstargazer.data.entities.StarEntity

@Dao
interface StarDao {
    @Query("SELECT * FROM stars WHERE magnitude <= :maxMagnitude AND dec > :minDec AND dec < :maxDec ORDER BY magnitude ASC")
    fun getVisibleStarsByDec(maxMagnitude: Double = 6.0, minDec: Double = -90.0, maxDec: Double = 90.0): List<StarEntity>

    @Query("SELECT * FROM stars ORDER BY magnitude ASC")
    fun getAll(): List<StarEntity>

    @Query("SELECT COUNT(*) FROM stars")
    fun countAll(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(stars: List<StarEntity>)

    @Query("DELETE FROM stars")
    fun deleteAll()

    @Query("SELECT * FROM stars WHERE id = :id")
    fun getStarById(id: Int): StarEntity?
    
    @Query("SELECT * FROM stars WHERE name LIKE :name LIMIT 1")
    fun getStarByName(name: String): StarEntity?
}

