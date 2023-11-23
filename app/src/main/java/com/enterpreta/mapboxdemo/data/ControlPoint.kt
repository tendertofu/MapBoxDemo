package com.enterpreta.mapboxdemo.data
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ControlPoint (
    val longitude: Double,
    val latitude: Double,
    val altitude: Double,
    @PrimaryKey(autoGenerate = true)
    val id: Int= 0
        )

