package com.example.seizureguard

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SeizureDao {
    // Implementation of the queries to use to access the database
    @Insert
    suspend fun insert(SeizureEntityLine: SeizureEntity)
    @Query("DELETE FROM past_seizures_table")
    suspend fun clear()
    @Query("SELECT * FROM past_seizures_table ORDER BY seizure_timestamps")
    suspend fun getAllSeizureEvents(): List<SeizureEntity>
    @Query("SELECT COUNT(*) FROM past_seizures_table")
    suspend fun size(): Int
}