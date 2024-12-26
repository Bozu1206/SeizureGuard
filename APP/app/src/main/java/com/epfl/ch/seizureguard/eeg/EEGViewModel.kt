package com.epfl.ch.seizureguard.eeg

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.epfl.ch.seizureguard.dl.DataSample
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope

class EEGViewModel : ViewModel() {
    private val _eegData = MutableStateFlow<List<List<Float>>>(emptyList())
    val eegData = _eegData.asStateFlow()

    private val _pointsToShow = MutableStateFlow(1024)
    val pointsToShow = _pointsToShow.asStateFlow()

    private val amplificationFactor = 1000f

    private val selectedChannels = listOf(
        0,  // FP1-F7
        1,  // F7-T7
        4,  // FP1-F3
        16, // FZ-CZ
        8,  // FP2-F4
        12  // FP2-F8
    )

    // Garder tous les samples
    private val buffers = List(6) { mutableListOf<Float>() }

    // Pour le scrolling
    private val _scrollOffset = MutableStateFlow(0f)
    val scrollOffset = _scrollOffset.asStateFlow()

    private val sampleReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val sample = intent.getParcelableExtra("EXTRA_SAMPLE", DataSample::class.java)
                if (sample != null) {
                    updateEEGData(sample)
                }
            }
        }
    }

    fun registerReceiver(context: Context) {
        val filter = IntentFilter("com.example.seizureguard.NEW_SAMPLE")
        ContextCompat.registerReceiver(
            context,
            sampleReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    fun unregisterReceiver(context: Context) {
        context.unregisterReceiver(sampleReceiver)
    }

    private fun updateEEGData(sample: DataSample) {
        try {
            // Ajouter le nouveau sample à la fin des buffers
            selectedChannels.forEachIndexed { bufferIndex, channelIndex ->
                val buffer = buffers[bufferIndex]
                
                // Ajouter le nouveau sample
                for (i in 0 until 1024) {
                    val value = sample.data[channelIndex * 1024 + i] * amplificationFactor
                    buffer.add(value)
                }
            }

            _eegData.value = buffers.map { it.toList() }

            // Animation du dessin progressif
            viewModelScope.launch {
                val currentSize = buffers[0].size
                val startPoint = currentSize - 1024
                _pointsToShow.value = startPoint

                val sampleDuration = 4000L
                val updateInterval = 40L
                val pointsPerUpdate = (1024 * updateInterval) / sampleDuration
                
                var currentPoints = 0
                while (currentPoints < 1024) {
                    currentPoints = (currentPoints + pointsPerUpdate).toInt().coerceAtMost(1024)
                    _pointsToShow.value = startPoint + currentPoints
                    delay(updateInterval)
                }
            }
        } catch (e: Exception) {
            Log.e("EEGViewModel", "Error updating EEG data", e)
        }
    }

    fun updateScrollOffset(delta: Float, graphWidth: Float) {
        val totalSamples = buffers[0].size / 1024f
        val sampleWidth = graphWidth / 4f
        val maxScroll = 0f
        val minScroll = -(totalSamples * sampleWidth) + graphWidth

        _scrollOffset.value = (_scrollOffset.value + delta).coerceIn(minScroll, maxScroll)
    }
}