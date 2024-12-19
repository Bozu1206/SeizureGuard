package com.epfl.ch.seizureguard.dl.metrics

import android.content.Context
import com.google.gson.Gson


data class Metrics (
    val accuracy: Double = -1.0,
    val precision: Double = -1.0,
    val recall: Double = -1.0,
    val f1: Double = -1.0,
    val fpr: Double = -1.0
)

object MetricsUtils {
    private const val PREFERENCES_NAME = "seizure_guard_prefs"

    fun saveMetrics(context: Context, key: String, metrics: Metrics) {
        val sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val json = Gson().toJson(metrics)
        sharedPreferences.edit().putString(key, json).apply()
    }

    fun loadMetrics(context: Context, key: String): Metrics {
        val sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val json = sharedPreferences.getString(key, null) ?: return Metrics()
        return Gson().fromJson(json, Metrics::class.java)
    }
}