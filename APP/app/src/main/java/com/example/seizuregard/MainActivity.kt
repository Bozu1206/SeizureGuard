package com.example.seizuregard

import ModelLoader
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import android.os.Bundle
import androidx.constraintlayout.compose.ConstraintLayout
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.seizuregard.dl.DataLoader
import com.example.seizuregard.ui.theme.SeizuregardTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.FloatBuffer

data class Metrics(
    val precision: Double,
    val recall: Double,
    val f1: Double,
    val fpr: Double
)

class MainActivity : ComponentActivity() {

    // metrics for model validation
    private var metrics by mutableStateOf(Metrics(-1.0, -1.0, -1.0, -1.0))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            SeizuregardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    InferenceHomePage(metrics,
                        onPerformInference = {
                            GlobalScope.launch(Dispatchers.IO) {// inference run in global scope and IO dispatcher
                                // Load input data
                                val dataLoader = DataLoader()
                                val dataSamples = dataLoader.loadDataAndLabels(context)
                                val totalSamples = dataSamples.size
                                // Load the model
                                val env = OrtEnvironment.getEnvironment()
                                val modelBytes = context.resources.openRawResource(R.raw.base_pat_02).readBytes()
                                val session = env.createSession(modelBytes)
                                val inputName = session.inputNames.iterator().next() // Get the input name from the model
                                // Collect predictions and batch sizes
                                val allPredictions = mutableListOf<Float>()
                                val outputsPerBatchList = mutableListOf<Int>()
                                val allTrueLabels = mutableListOf<Int>()
                                val batchSize = 32  // Desired batch size
                                val totalBatches = (totalSamples + batchSize - 1) / batchSize  // Ceiling division
                                for (batchIndex in 0 until totalBatches) { // iterate over batches
                                    val startIndex = batchIndex * batchSize
                                    val endIndex = minOf(startIndex + batchSize, totalSamples)
                                    val batchDataSamples = dataSamples.slice(startIndex until endIndex) // get data for current batch
                                    val batchData = batchDataSamples.map { it.data }.toTypedArray()
                                    val inputTensorData = flattenBatchData(batchData) // put batch data in correct shape
                                    val currentBatchSize = batchDataSamples.size
                                    val batchTrueLabels = batchDataSamples.map { it.label }// Collect true labels for this batch (for validation)
                                    allTrueLabels.addAll(batchTrueLabels)
                                    try {
                                        val tensorShape = longArrayOf(currentBatchSize.toLong(), 18L, 1024L) // define shape of input tensor
                                        val tensor = OnnxTensor.createTensor(
                                            env,
                                            FloatBuffer.wrap(inputTensorData),
                                            tensorShape
                                        )
                                        // Prepare input map
                                        val inputMap = mapOf<String, OnnxTensor>(inputName to tensor)
                                        // Run the model inference
                                        val results = session.run(inputMap)
                                        // Get the output tensor
                                        val outputTensor = results[0].value as Array<FloatArray>
                                        // Flatten and collect predictions
                                        val predictions = flattenArray(outputTensor)
                                        allPredictions.addAll(predictions.toList())
                                        outputsPerBatchList.add(currentBatchSize)
                                        logModelOutputs(predictions, batchSize * 2) // print all predictions in logcat
                                        // Clean up resources
                                        tensor.close()
                                        results.close()
                                    } catch (e: Exception) {
                                        Log.e(
                                            "ModelInference",
                                            "Error during model inference: ${e.message}"
                                        )
                                    }
                                }
                                // Close the session and environment
                                session.close()
                                env.close()
                                // Convert predictions to FloatArray
                                val predictionsArray = allPredictions.toFloatArray()
                                val trueLabelsArray = allTrueLabels.toIntArray()
                                // Validate and compute metrics
                                withContext(Dispatchers.Main) {
                                    metrics = validate(predictionsArray, trueLabelsArray)
                                }
                                // Logcat the metrics
                                Log.d("ValidationMetrics", "F1 Score: ${metrics.f1}, Precision: ${metrics.precision}, Recall: ${metrics.recall}, FPR: ${metrics.fpr}"
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

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

fun computeMetrics(trueLabels: IntArray, predLabels: IntArray): Metrics  {
    var tp = 0  // True Positives
    var tn = 0  // True Negatives
    var fp = 0  // False Positives
    var fn = 0  // False Negatives
    for (i in trueLabels.indices) {
        val trueLabel = trueLabels[i]
        val predLabel = predLabels[i]
        when {
            trueLabel == 1 && predLabel == 1 -> tp += 1
            trueLabel == 0 && predLabel == 0 -> tn += 1
            trueLabel == 0 && predLabel == 1 -> fp += 1
            trueLabel == 1 && predLabel == 0 -> fn += 1
        }
    }
    val precision = if (tp + fp > 0) tp.toDouble() / (tp + fp) else 0.0
    val recall = if (tp + fn > 0) tp.toDouble() / (tp + fn) else 0.0
    val f1 = if (precision + recall > 0) 2 * precision * recall / (precision + recall) else 0.0
    val fpr = if (fp + tn > 0) fp.toDouble() / (fp + tn) else 0.0
    return Metrics(
        precision = precision,
        recall = recall,
        f1 = f1,
        fpr = fpr
    )
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
    val metrics = computeMetrics(trueLabels, predLabels)
    return metrics
}

@Composable
fun InferenceHomePage(
    metrics: Metrics,
    onPerformInference: () -> Unit,
    modifier: Modifier = Modifier
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Create references for the composables to position
        val (titleText, performInferenceButton, metricsColumn, notComputedText) = createRefs()
        Button(
            onClick = onPerformInference,
            modifier = Modifier
                .constrainAs(performInferenceButton) {
                    top.linkTo(titleText.bottom, margin = 16.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                .fillMaxWidth(0.8f)  // Button fills 80% of the width
        ) {
            Text(text = "Perform Inference")
        }
        if (metrics.f1 != -1.0) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.constrainAs(metricsColumn) {
                    top.linkTo(performInferenceButton.bottom, margin = 24.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
            ) {
                Text(text = "F1 Score: ${"%.4f".format(metrics.f1)}")
                Text(text = "Precision: ${"%.4f".format(metrics.precision)}")
                Text(text = "Recall: ${"%.4f".format(metrics.recall)}")
                Text(text = "FPR: ${"%.4f".format(metrics.fpr)}")
            }
        } else {
            Text(
                text = "Not yet computed",
                modifier = Modifier.constrainAs(notComputedText) {
                    top.linkTo(performInferenceButton.bottom, margin = 24.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun InferenceHomePagePreview() {
    SeizuregardTheme {
        InferenceHomePage( Metrics(0.0,0.0,0.0,0.0), {})
    }
}
