package com.example.seizureguard.dl

import org.jetbrains.kotlinx.dl.api.core.Sequential
import org.jetbrains.kotlinx.dl.api.core.activation.Activations
import org.jetbrains.kotlinx.dl.api.core.layer.activation.ReLU
import org.jetbrains.kotlinx.dl.api.core.layer.convolutional.Conv1D
import org.jetbrains.kotlinx.dl.api.core.layer.convolutional.ConvPadding
import org.jetbrains.kotlinx.dl.api.core.layer.core.Input
import org.jetbrains.kotlinx.dl.api.core.layer.normalization.BatchNorm
import org.jetbrains.kotlinx.dl.api.core.layer.pooling.MaxPool1D
import org.jetbrains.kotlinx.dl.api.core.layer.reshaping.Flatten

private val NUM_CHANNELS = 22L
private val IMAGE_WIDTH  = 1024L
private val IMAGE_HEIGHT = 18L

val model = Sequential.of(
    Input(
        IMAGE_WIDTH,
        IMAGE_HEIGHT,
        NUM_CHANNELS
    ),

    Conv1D(
        filters = 128,
        kernelLength = 3,
        strides = 1,
        activation = Activations.Linear,
        padding = ConvPadding.SAME
    ),
    BatchNorm(),
    ReLU(),
    MaxPool1D(
        poolSize = intArrayOf(4),
        strides = intArrayOf(4),
        padding = ConvPadding.VALID
    ),

    // 2nd conv layer
    Conv1D(
        filters = 128,
        kernelLength = 3,
        strides = 1,
        activation = Activations.Linear,
        padding = ConvPadding.SAME
    ),
    BatchNorm(),
    ReLU(),
    MaxPool1D(
        poolSize = intArrayOf(4),
        strides = intArrayOf(4),
        padding = ConvPadding.VALID
    ),

    // 3rd conv layer
    Conv1D(
        filters = 128,
        kernelLength = 3,
        strides = 1,
        activation = Activations.Linear,
        padding = ConvPadding.SAME
    ),
    BatchNorm(),
    ReLU(),
    MaxPool1D(
        poolSize = intArrayOf(4),
        strides = intArrayOf(4),
        padding = ConvPadding.VALID
    ),

    // Classifier (MLP)
    Conv1D(
        filters = 128,
        kernelLength = 3,
        strides = 1,
        activation = Activations.Linear,
        padding = ConvPadding.VALID
    ),
    Conv1D(
        filters = 100,
        kernelLength = 1,
        strides = 1,
        activation = Activations.Linear,
        padding = ConvPadding.VALID
    ),
    Flatten(),
)

class SeizureModel {
    fun train(train: SeizureDataset) {
        TODO("Not implemented")
    }
}

