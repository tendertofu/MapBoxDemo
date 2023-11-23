package com.enterpreta.mapboxdemo.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface ControlPointDao {
    @Upsert
    fun upsert(controlPoint: ControlPoint)

    @Delete
    fun delete(controlPoint: ControlPoint)

    @Query("SELECT * FROM ControlPoint ORDER BY id ASC")
    fun getAllControlPoints(): List<ControlPoint>

    @Query("DELETE FROM ControlPoint")
    fun deleteAll(): Unit

    @Query("SELECT COUNT(*) FROM ControlPoint")
    fun count(): Int
}