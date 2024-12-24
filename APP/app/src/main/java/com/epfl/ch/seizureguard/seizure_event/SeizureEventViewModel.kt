package com.epfl.ch.seizureguard.seizure_event

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.epfl.ch.seizureguard.profile.ProfileRepository
import kotlinx.coroutines.launch

class SeizureEventViewModel(application: Application):
    AndroidViewModel(application) {
    private val database: SeizureDao = SeizureDatabase.getInstance(application).seizureDao

    fun saveNewSeizure(event: SeizureEvent) = viewModelScope.launch {
        val seizure = SeizureEntity(
            type = event.type,
            duration = event.duration,
            severity = event.severity,
            triggers = event.triggers,
            timestamp = event.timestamp
        )

        database.insert(seizure)
    }

    fun logAllPastSeizures() {
        viewModelScope.launch {
            val seizures = database.getAllSeizureEvents().value
            seizures?.forEach {
                Log.d("SeizureEvent", it.toString())
            }
        }
    }
}
