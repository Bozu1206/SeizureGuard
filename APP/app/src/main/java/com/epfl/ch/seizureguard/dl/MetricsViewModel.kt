package com.epfl.ch.seizureguard.dl

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.epfl.ch.seizureguard.dl.metrics.Metrics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.google.gson.Gson

class MetricsViewModel(application: Application) : AndroidViewModel(application) {
    private val _metricsBeforeTraining = MutableStateFlow(Metrics())
    val metricsBeforeTraining: StateFlow<Metrics> get() = _metricsBeforeTraining

    private val _metricsAfterTraining = MutableStateFlow(Metrics())
    val metricsAfterTraining: StateFlow<Metrics> get() = _metricsAfterTraining

    private val sharedPreferences =
        application.getSharedPreferences("seizure_guard_prefs", Context.MODE_PRIVATE)

    fun loadMetrics() {
        _metricsBeforeTraining.value = loadMetricsFromPreferences("metrics_before_training")
        _metricsAfterTraining.value = loadMetricsFromPreferences("metrics_after_training")
    }

    private fun loadMetricsFromPreferences(key: String): Metrics {
        val json = sharedPreferences.getString(key, null) ?: return Metrics()
        return Gson().fromJson(json, Metrics::class.java)
    }
}
