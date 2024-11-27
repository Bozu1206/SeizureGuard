package com.example.seizureguard.dl.utils

import android.content.Context
import android.util.Log
import com.example.seizureguard.dl.DataSample
import com.example.seizureguard.dl.InferenceProcessor
import com.example.seizureguard.dl.metrics.ComputeMetrics
import com.example.seizureguard.dl.metrics.Metrics

object utils {
    fun flattenArray(arr: Array<FloatArray>): FloatArray {
        val totalSize = arr.sumOf { it.size }
        val result = FloatArray(totalSize)
        var index = 0
        for (subArray in arr) {
            subArray.copyInto(result, index)
            index += subArray.size
        }
        return result
    }

    fun flattenBatchData(batchData: Array<FloatArray>): FloatArray { // flatten the batch data into a single FloatArray
        val totalSize = batchData.sumOf { it.size }
        val inputTensor = FloatArray(totalSize)
        var index = 0
        for (data in batchData) {
            System.arraycopy(data, 0, inputTensor, index, data.size)
            index += data.size
        }
        return inputTensor
    }

    fun logModelOutputs(predictions: FloatArray, outputsPerBatch: Int) {
        if (predictions.size % outputsPerBatch != 0) {
            Log.e("ModelOutputs", "Predictions size is not divisible by outputsPerBatch")
            return
        }
        val numBatches = predictions.size / outputsPerBatch
        val numRowsPerBatch = outputsPerBatch / 2
        val outputBuilder = StringBuilder()
        outputBuilder.append("Model predictions:\n")
        for (batchIndex in 0 until numBatches) {
            outputBuilder.append("[\n")
            val batchStartIndex = batchIndex * outputsPerBatch
            for (i in 0 until numRowsPerBatch) {
                val index = batchStartIndex + i * 2
                val value1 = predictions[index]
                val value2 = predictions[index + 1]
                val formattedValue1 = "%10.6f".format(value1)
                val formattedValue2 = "%10.6f".format(value2)
                val rowString = " [ $formattedValue1 $formattedValue2]"
                outputBuilder.append(rowString)
                if (i < numRowsPerBatch - 1) {
                    outputBuilder.append("\n")
                }
            }
            outputBuilder.append("\n]")
            if (batchIndex < numBatches - 1) {
                outputBuilder.append("\n") // Separate batches with a newline
            }
        }
        val output = outputBuilder.toString()
        Log.d("ModelOutputs", output)
    }

    fun validate(predictions: FloatArray, trueLabels: IntArray): Metrics {
        // Ensure that predictions size is twice the number of samples (since there are two outputs per sample)
        if (predictions.size != trueLabels.size * 2) {
            throw IllegalArgumentException("Predictions size (${predictions.size}) does not match expected size (${trueLabels.size * 2})")
        }
        val predLabels = IntArray(trueLabels.size)
        // Extract predicted labels
        for (i in trueLabels.indices) {
            val index = i * 2
            val output1 = predictions[index]
            val output2 = predictions[index + 1]
            // Predicted label is the index of the max value
            predLabels[i] = if (output1 > output2) 0 else 1
        }
        val metrics = ComputeMetrics.computeMetrics(trueLabels, predLabels)
        return metrics
    }

    fun generateRandomSample(inputShape: LongArray): FloatArray {
        val size = inputShape.fold(1L) { acc, dim -> acc * dim }.toInt()
        return FloatArray(size) { (0..100).random() / 100f }
    }

    fun generateRandomInputAndRunInference(
        context: Context,
        inferenceProcessor: InferenceProcessor,
        inputShape: LongArray = longArrayOf(1, 18, 1024),
        threshold: Float = 0.5f,
        callback: (Int) -> Unit
    ) {
        val randomInputData = generateRandomSample(inputShape)
        val randomDataSample = DataSample(data = randomInputData, label = -1)

        inferenceProcessor.runInferenceForSample(randomDataSample) { predictions ->
            val outputLabel = if (predictions[0] > threshold) 1 else 0
            callback(outputLabel)
        }
    }
}