package com.example.seizureguard.seizure_event

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SeizureDao {
    // Implementation of the queries to use to access the database
    @Insert
    suspend fun insert(SeizureEntityLine: SeizureEntity)

    @Query(
        "UPDATE past_seizures_table " +
                "SET seizure_type = :type, seizure_duration = :duration, seizure_severity = :severity, seizure_triggers = :triggers " +
                "WHERE seizure_timestamps = :timestamp"
    )
    suspend fun updateByTimestamp(
        type: String,
        duration: Int,
        severity: Int,
        triggers: List<String>,
        timestamp: Long
    )


    @Query("DELETE FROM past_seizures_table WHERE seizure_timestamps = :timestamp")
    suspend fun deleteByTimestamp(timestamp: Long)

    @Query("DELETE FROM past_seizures_table")
    suspend fun clear()

    @Query("SELECT * FROM past_seizures_table ORDER BY seizure_timestamps")
    fun getAllSeizureEvents(): LiveData<List<SeizureEntity>>


    @Query("SELECT COUNT(*) FROM past_seizures_table")
    suspend fun size(): Int


}