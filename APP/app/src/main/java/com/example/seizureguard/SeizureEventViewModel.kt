package com.example.seizureguard

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class SeizureEventViewModel (application: Application) : AndroidViewModel(application) {

    private val database: SeizureDao = SeizureDatabase.getInstance(application).seizureDao

    // Function to save a new seizure
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

    // Function to log all past seizures
    fun logAllPastSeizures() {
        viewModelScope.launch {
            val seizures = database.getAllSeizureEvents()
            seizures.forEach {
                Log.d("SeizureEvent", it.toString())
            }
        }
    }
}
