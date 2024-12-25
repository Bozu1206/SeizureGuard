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
)
