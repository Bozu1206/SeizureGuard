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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class EEGViewModel : ViewModel() {
    private val _eegData = MutableStateFlow<List<List<Float>>>(emptyList())
    val eegData = _eegData.asStateFlow()

    private val _pointsToShow = MutableStateFlow(1024)
    val pointsToShow = _pointsToShow.asStateFlow()

    private val amplificationFactor = 1000f

    // Channels of Interest
    private val selectedChannels = listOf(
        0,  // FP1-F7
        1,  // F7-T7
        4,  // FP1-F3
        16, // FZ-CZ
        8,  // FP2-F4
        12  // FP2-F8
    )

    private val _samplesPerChannel = MutableStateFlow(1024)
    val samplesPerChannel = _samplesPerChannel.asStateFlow()

    private val buffers = List(6) { mutableListOf<Float>() }

    private val _scrollOffset = MutableStateFlow(0f)
    val scrollOffset = _scrollOffset.asStateFlow()

    private var animationJob: Job? = null
    private val MAX_SAMPLES = 10_000

    private val sampleReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val sample = intent.getParcelableExtra("EXTRA_SAMPLE", DataSample::class.java)
                if (sample != null && sample.data.size != 0) {
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
            _samplesPerChannel.value = sample.data.size / 18
            val spc = _samplesPerChannel.value

            selectedChannels.forEachIndexed { bufferIndex, channelIndex ->
                val buffer = buffers[bufferIndex]

                for (i in 0 until spc) {
                    val value = sample.data[channelIndex * spc + i] * amplificationFactor
                    buffer.add(value)
                }

                if (buffer.size > MAX_SAMPLES) {
                    val toRemove = buffer.size - MAX_SAMPLES
                    for (i in 0 until toRemove) {
                        buffer.removeAt(0)
                    }
                }
            }
            _eegData.value = buffers.map { it.toList() }

            // Animate from previous end to new end
            val newSize = buffers[0].size
            val startPoint = newSize - spc
            val endPoint = newSize
            animatePoints(startPoint, endPoint)

        } catch (e: Exception) {
            Log.e("EEGViewModel", "Error updating EEG data", e)
        }
    }
    private fun animatePoints(startPoint: Int, endPoint: Int) {
        animationJob?.cancel()
        animationJob = viewModelScope.launch {
            val sampleDuration = 3500L      // total animate time
            val updateInterval = 40L        // ms between UI updates
            val totalPoints = endPoint - startPoint
            val pointsPerUpdate = (totalPoints * updateInterval) / sampleDuration
            var currentPoints = 0
            while (currentPoints < totalPoints) {
                currentPoints = (currentPoints + pointsPerUpdate.toInt())
                    .coerceAtMost(totalPoints)
                _pointsToShow.value = startPoint + currentPoints
                delay(updateInterval)
            }
        }
    }

    fun updateScrollOffset(delta: Float, graphWidth: Float, sampleWidthRatio: Float) {
        val totalSamples = buffers[0].size / samplesPerChannel.value
        val sampleWidth = graphWidth / sampleWidthRatio
        val maxScroll = 0f
        val minScroll = -(totalSamples * sampleWidth) + graphWidth
         _scrollOffset.value = (_scrollOffset.value + delta).coerceIn(minScroll, maxScroll)
    }
}