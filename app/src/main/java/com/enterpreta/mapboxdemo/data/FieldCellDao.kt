package com.enterpreta.mapboxdemo.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface FieldCellDao {
    @Upsert
    suspend fun upsertFieldCell(fieldCell: FieldCell)

    @Delete
    suspend fun deleteFieldCell(fieldCell: FieldCell)

    @Query("SELECT * FROM FieldCell ORDER BY id ASC")
    fun getContactsOrderedByFirstName(): List<FieldCell>

    @Query("DELETE FROM FieldCell")
    fun deleteAll(): Unit

    @Query("SELECT COUNT(*) FROM FieldCell")
    fun count(): Int
}
