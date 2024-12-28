package com.epfl.ch.seizureguard.seizure_detection

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SeizureDetectionViewModel : ViewModel() {
    private val _isSeizureDetected = MutableStateFlow(false)
    val isSeizureDetected = _isSeizureDetected.asStateFlow()

    fun onSeizureDetected() {
        _isSeizureDetected.value = true
    }

    fun onSeizureHandled() {
        _isSeizureDetected.value = false
    }
} 