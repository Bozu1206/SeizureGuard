package com.epfl.ch.seizureguard.seizure_event

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SeizureEventViewModel(application: Application) :
    AndroidViewModel(application) {

    private val _showSeizureLoggingDialog = MutableStateFlow(false)
    val showSeizureLoggingDialog = _showSeizureLoggingDialog.asStateFlow()

    private val _showGuidelinesDialog = MutableStateFlow(false)
    val showGuidelinesDialog = _showGuidelinesDialog.asStateFlow()

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
