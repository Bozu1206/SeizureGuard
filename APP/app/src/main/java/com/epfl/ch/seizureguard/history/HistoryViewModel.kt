package com.example.seizureguard.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.seizureguard.seizure_event.SeizureDao
import com.example.seizureguard.seizure_event.SeizureDatabase
import com.example.seizureguard.seizure_event.SeizureEvent
import kotlinx.coroutines.launch

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val database: SeizureDao = SeizureDatabase.getInstance(application).seizureDao

    val seizures: LiveData<List<SeizureEvent>> = database.getAllSeizureEvents().map { entities ->
        entities.map { entity ->
            SeizureEvent(
                entity.type,
                entity.duration,
                entity.severity,
                entity.triggers,
                entity.timestamp
            )
        }
    }

    fun removeSeizure(event: SeizureEvent) {
        viewModelScope.launch {
            database.deleteByTimestamp(event.timestamp)
        }
    }

    fun editSeizure(newSeizure: SeizureEvent, oldSeizure: SeizureEvent) {
        viewModelScope.launch {
            database.updateByTimestamp(
                newSeizure.type,
                newSeizure.duration,
                newSeizure.severity,
                newSeizure.triggers,
                oldSeizure.timestamp
            )
        }
    }
}
