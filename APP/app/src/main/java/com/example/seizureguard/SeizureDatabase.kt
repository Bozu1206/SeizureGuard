package com.example.seizuregard

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson

@Database(entities = [SeizureEntity::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class SeizureDatabase : RoomDatabase() {
    abstract val seizureDao: SeizureDao
    companion object {
        @Volatile
        private var INSTANCE: SeizureDatabase? = null
        fun getInstance(context: Context): SeizureDatabase {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        SeizureDatabase::class.java,
                        "Seizure_database"
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}

class Converters { // needed for saving the list of String trigger into a single String in the database

    // Convert a JSON string to a List<String>
    @TypeConverter
    fun fromString(value: String): List<String> {
        val listType = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }

    // Convert a List<String> to a JSON string
    @TypeConverter
    fun fromList(list: List<String>): String {
        return Gson().toJson(list)
    }
}