package com.enterpreta.mapboxdemo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

//This class makes a singleton of the database to avoid waste of memory when accessing it
@Database(entities = [FieldCell::class], version = 1, exportSchema = false)
abstract class FieldDatabase : RoomDatabase() {
    abstract fun fieldCellDao(): FieldCellDao
    companion object {
        @Volatile
        private var INSTANCE: FieldDatabase? = null

        fun getDatabase(context: Context): FieldDatabase{
            val tempInstance = INSTANCE
            if(tempInstance != null){
                return tempInstance
            }
            synchronized(this){
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FieldDatabase::class.java,
                    "field_database"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}
