package com.example.seizureguard.dl.metrics

data class Metrics(
    val accuracy: Double,
    val precision: Double,
    val recall: Double,
    val f1: Double,
    val fpr: Double
)