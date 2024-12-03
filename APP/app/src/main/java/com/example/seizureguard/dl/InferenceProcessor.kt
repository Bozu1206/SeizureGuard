package com.example.seizureguard.dl

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import com.example.seizureguard.R
import com.example.seizureguard.dl.metrics.Metrics
import com.example.seizureguard.dl.utils.utils.flattenArray
import com.example.seizureguard.dl.utils.utils.flattenBatchData
import com.example.seizureguard.dl.utils.utils.validate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer
import kotlin.math.exp

class InferenceProcessor(
    private val context: Context,
    private val dataLoader: DataLoader,
    private val onnxHelper: OnnxHelper,
    private val onSeizureDetected: () -> Unit
) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val predictedLabels = mutableListOf<Int>()

    fun runInference(callback: (Metrics) -> Unit) {
        coroutineScope.launch {
            try {
                // Load data
                val dataSamples = loadData()

                // Load pre-trained model
                val (session, env, inputName) = onnxHelper.loadModel(context, R.raw.base_pat_02)

                // Perform inference on a single sample starting from samples number 740
                for (i in 740 until 780) {
                    runInferenceForSample(dataSamples[i]) { predictions ->
                        // Compute softmax of predictions to get probabilities
                        val softmax = softmax(predictions)
                        Log.d(
                            "InferenceProcessor",
                            "Predictions: ${predictions.joinToString()} | ($i) Softmax: ${softmax.joinToString()} | True label: ${dataSamples[i].label}"
                        )

                        // Return biggest number in predictions
                        val predictedLabel = predictions.withIndex().maxByOrNull { it.value }?.index
                        predictedLabels.add(predictedLabel ?: -1) // -1 is a placeholder for error

                        // If seizure is detected (label = 1), invoke the callback
                        if (predictedLabel == 1) {
                            coroutineScope.launch {
                                onSeizureDetected() // Pass the sample index or any other context
                            }
                        }
                    }

                    // sleep for 4 seconds
                    withContext(Dispatchers.IO) {
                        Thread.sleep(4000)
                    }
                }

                // Perform inference on entire dataset
                val predictions = performInference(dataSamples, session, env, inputName)

                // Evaluate results
                val metrics = computeMetrics(predictions.first, predictions.second)
                withContext(Dispatchers.Main) {
                    callback(metrics)
                }
            } catch (e: Exception) {
                Log.e("InferenceProcessor", "Erreur : ${e.message}", e)
            }
        }
    }

    private fun softmax(predictions: FloatArray): List<Double> {
        val probabilities = predictions.map { exp(it.toDouble()) }
        val sum = probabilities.sum()
        return probabilities.map { it / sum }
    }

    fun runInferenceForSample(
        dataSample: DataSample,
        onResult: (FloatArray) -> Unit
    ) {
        coroutineScope.launch {
            try {
                val (session, env, inputName) = onnxHelper.loadModel(context, R.raw.base_pat_02)
                val inputTensorData = flattenBatchData(arrayOf(dataSample.data))
                val tensorShape = longArrayOf(1, 18, 1024)
                val tensor =
                    OnnxTensor.createTensor(env, FloatBuffer.wrap(inputTensorData), tensorShape)

                val predictions = session.run(mapOf(inputName to tensor)).use { results ->
                    val outputTensor = results[0].value as Array<FloatArray>
                    flattenArray(outputTensor)
                }

                withContext(Dispatchers.Main) {
                    onResult(predictions)
                }

                tensor.close()
                session.close()
                env.close()
            } catch (e: Exception) {
                Log.e("InferenceProcessor", "Error during single sample inference: ${e.message}", e)
            }
        }
    }

    private suspend fun loadData(): Array<DataSample> = withContext(Dispatchers.IO) {
        dataLoader.loadDataAndLabels(context)
    }

    private suspend fun performInference(
        dataSamples: Array<DataSample>,
        session: OrtSession,
        env: OrtEnvironment,
        inputName: String
    ): Pair<FloatArray, IntArray> {
        val allPredictions = mutableListOf<Float>()
        val allTrueLabels = mutableListOf<Int>()
        val batchSize = 32

        val totalBatches = (dataSamples.size + batchSize - 1) / batchSize
        for (batchIndex in 0 until totalBatches) {
            val batchDataSamples = dataSamples.getBatch(batchIndex, batchSize)
            val batchTrueLabels = batchDataSamples.map { it.label }
            allTrueLabels.addAll(batchTrueLabels)

            try {
                val predictions = onnxHelper.runBatchInference(
                    env, session, inputName, batchDataSamples
                )
                allPredictions.addAll(predictions)
            } catch (e: Exception) {
                Log.e("InferenceProcessor", "Erreur dans l'inférence du modèle : ${e.message}", e)
            }
        }

        return allPredictions.toFloatArray() to allTrueLabels.toIntArray()
    }

    private suspend fun computeMetrics(
        predictions: FloatArray,
        trueLabels: IntArray
    ): Metrics = withContext(Dispatchers.Default) {
        validate(predictions, trueLabels)
    }

    fun cancelCoroutine() {
        coroutineScope.cancel() // Cancel all coroutines running in the processor
    }
}

private fun Array<DataSample>.getBatch(batchIndex: Int, batchSize: Int): List<DataSample> {
    val startIndex = batchIndex * batchSize
    val endIndex = minOf(startIndex + batchSize, size)
    return slice(startIndex until endIndex)
}

class OnnxHelper {
    fun loadModel(context: Context, modelResId: Int): Triple<OrtSession, OrtEnvironment, String> {
        val env = OrtEnvironment.getEnvironment()
        val modelBytes = context.resources.openRawResource(modelResId).readBytes()
        val session = env.createSession(modelBytes)
        val inputName = session.inputNames.iterator().next()
        return Triple(session, env, inputName)
    }

    fun runBatchInference(
        env: OrtEnvironment,
        session: OrtSession,
        inputName: String,
        batchDataSamples: List<DataSample>
    ): List<Float> {
        val batchData = batchDataSamples.map { it.data }.toTypedArray()
        val inputTensorData = flattenBatchData(batchData)
        val tensorShape = longArrayOf(batchDataSamples.size.toLong(), 18L, 1024L)
        val tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(inputTensorData), tensorShape)

        val predictions = session.run(mapOf(inputName to tensor)).use { results ->
            val outputTensor = results[0].value as Array<FloatArray>
            flattenArray(outputTensor)
        }

        tensor.close()
        return predictions.toList()
    }
}
