package com.example.orionstargazer.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stars",
    indices = [
        Index(value = ["magnitude"]),
        Index(value = ["dec"]),
        Index(value = ["ra"])
    ]
)
data class StarEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val ra: Double, // Right Ascension (degrees)
    val dec: Double, // Declination (degrees)
    val magnitude: Double,
    val distance: Double?, // light years (nullable)
    val spectralType: String?,
    val constellation: String?
)

