package com.epfl.ch.seizureguard.seizure_event

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "past_seizures_table")
data class SeizureEntity(
    @PrimaryKey(autoGenerate = true)
    var seizureKey: Long = 0L,
    @ColumnInfo(name = "seizure_type")
    val type: String = "Unknown",
    @ColumnInfo(name = "seizure_duration")
    val duration: Int = 0,
    @ColumnInfo(name = "seizure_severity")
    val severity: Int = -1,
    @ColumnInfo(name = "seizure_triggers")
    val triggers: List<String>,
    @ColumnInfo(name = "seizure_timestamps")
    val timestamp: Long,
)

