package com.epfl.ch.seizureguard.dl.metrics

import android.content.Context
import com.epfl.ch.seizureguard.profile.ProfileViewModel
import com.google.gson.Gson

data class Metrics (
    val accuracy: Double = -1.0,
    val precision: Double = -1.0,
    val recall: Double = -1.0,
    val f1: Double = -1.0,
    val fpr: Double = -1.0
) {
    companion object {
        fun defaultsModelMetrics(): Metrics {
            return Metrics(
                accuracy = 0.944,
                precision = 1.0,
                recall = 0.3170731707317073,
                f1 = 0.48148148148148145,
                fpr = 0.0
            )
        }
    }
}