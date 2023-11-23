package com.enterpreta.mapboxdemo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class FieldCell (
    val northWest: Double,
    val widthHeight: Double,
    @PrimaryKey(autoGenerate = true)
    val id: Int= 0
    )