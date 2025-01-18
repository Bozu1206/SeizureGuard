package com.epfl.ch.seizureguard.seizure_event

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.epfl.ch.seizureguard.profile.ProfileRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.ViewModel
import com.epfl.ch.seizureguard.widgets.SeizureCountWidget

class SeizureEventViewModel(application: Application):
    AndroidViewModel(application) {
    private val database: SeizureDao = SeizureDatabase.getInstance(application).seizureDao

    private val _showSeizureLoggingDialog = MutableStateFlow(false)
    val showSeizureLoggingDialog = _showSeizureLoggingDialog.asStateFlow()

    private val _showGuidelinesDialog = MutableStateFlow(false)
    val showGuidelinesDialog = _showGuidelinesDialog.asStateFlow()

    fun saveNewSeizure(event: SeizureEvent) = viewModelScope.launch {
        val seizure = SeizureEntity(
            type = event.type,
            duration = event.duration,
            severity = event.severity,
            triggers = event.triggers,
            timestamp = event.timestamp
        )

        database.insert(seizure)
        
        // Mettre à jour le widget après l'ajout d'une nouvelle crise
        SeizureCountWidget.updateWidget(getApplication())
    }

    fun logAllPastSeizures() {
        viewModelScope.launch {
            val seizures = database.getAllSeizureEvents().value
            seizures?.forEach {
                Log.d("SeizureEvent", it.toString())
            }
        }
    }

    fun showSeizureLoggingDialog() {
        _showSeizureLoggingDialog.value = true
    }

    fun hideSeizureLoggingDialog() {
        _showSeizureLoggingDialog.value = false
    }

    fun showGuidelinesDialog() {
        _showGuidelinesDialog.value = true
    }

    fun hideGuidelinesDialog() {
        _showGuidelinesDialog.value = false
    }
}
