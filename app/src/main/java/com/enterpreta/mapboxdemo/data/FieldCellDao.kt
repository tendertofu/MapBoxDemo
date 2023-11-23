package com.enterpreta.mapboxdemo.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface FieldCellDao {
    @Upsert
    fun upsert(fieldCell: FieldCell)

    @Delete
    fun delete(fieldCell: FieldCell)

    @Query("SELECT * FROM FieldCell ORDER BY id ASC")
    fun getAllFieldCells(): List<FieldCell>

    @Query("DELETE FROM FieldCell")
    fun deleteAll(): Unit

    @Query("SELECT COUNT(*) FROM FieldCell")
    fun count(): Int
}
