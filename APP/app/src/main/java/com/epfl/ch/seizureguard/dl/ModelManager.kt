package com.epfl.ch.seizureguard.dl

import ai.onnxruntime.*
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.epfl.ch.seizureguard.dl.metrics.ComputeMetrics.computeMetrics
import com.epfl.ch.seizureguard.dl.metrics.Metrics
import com.epfl.ch.seizureguard.dl.utils.utils.floatArrayToFloatBuffer
import com.epfl.ch.seizureguard.dl.utils.utils.intToLongBuffer
import com.epfl.ch.seizureguard.profile.ProfileViewModel
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Path
import java.util.Collections

class ModelManager() {
    private var ortEnv: OrtEnvironment? = null
    private var ortTrainingSession: OrtTrainingSession? = null
    private var ortSession: OrtSession? = null

    private var isDebugEnabled: Boolean = false

    private var checkpointPath: String = ""
    private var trainModelPath: String = ""
    private var evalModelPath: String = ""
    private var optimizerModelPath: String = ""
    private var inferenceModelPath: String = ""

    constructor(
        checkpointPath: String,
        trainModelPath: String,
        evalModelPath: String,
        optimizerModelPath: String,
        inferenceModelPath: String,
        isDebugEnabled: Boolean
    ) : this() {
        ortEnv = OrtEnvironment.getEnvironment()

        this.isDebugEnabled = isDebugEnabled
        this.checkpointPath = checkpointPath
        this.trainModelPath = trainModelPath
        this.evalModelPath = evalModelPath
        this.optimizerModelPath = optimizerModelPath
        this.inferenceModelPath = inferenceModelPath

        ortTrainingSession = ortEnv?.createTrainingSession(
            checkpointPath,
            trainModelPath,
            evalModelPath,
            optimizerModelPath
        )
    }

    constructor(inferenceModelPath: String, isDebugEnabled: Boolean ): this() {
        // Create a new instance of ModelManager
        ortEnv = OrtEnvironment.getEnvironment()

        this.isDebugEnabled = isDebugEnabled
        this.checkpointPath = ""
        this.trainModelPath = ""
        this.evalModelPath = ""
        this.optimizerModelPath = ""
        this.inferenceModelPath = inferenceModelPath

        ortSession = ortEnv?.createSession(inferenceModelPath)
    }

    fun performTrainingEpoch(data: Array<DataSample>) {
        ortSession = null
        var loss = -1.0f

        ortEnv.use {
            val dataShape = longArrayOf(1, 18, 1024)
            for (sample in data) {
                val inputTensor =
                    OnnxTensor.createTensor(ortEnv, floatArrayToFloatBuffer(sample.data), dataShape)
                val labelsShape = longArrayOf(1)
                val labelsTensor =
                    OnnxTensor.createTensor(ortEnv, intToLongBuffer(sample.label), labelsShape)
                inputTensor.use {
                    labelsTensor.use {
                        val ortInputMap: MutableMap<String, OnnxTensor> = HashMap()
                        ortInputMap["input"] = inputTensor
                        ortInputMap["labels"] = labelsTensor
                        val output = ortTrainingSession?.trainStep(ortInputMap)
                        output.use {
                            loss += ((output?.get(0)?.value) as Float)
                        }
                    }
                }
            }
            Log.d("Trainer", "Loss: $loss")
            ortTrainingSession?.optimizerStep()
            ortTrainingSession?.lazyResetGrad()
        }
    }

    fun performInference(sample: DataSample): Int {
        ortSession = ortEnv?.createSession(inferenceModelPath)
        ortEnv?.use {
            val shape = longArrayOf(1, 18, 1024)
            val tensor = OnnxTensor.createTensor(ortEnv, floatArrayToFloatBuffer(sample.data), shape)
            tensor.use {
                val output = ortSession?.run(Collections.singletonMap("input", tensor))
                output.use {
                    @Suppress("UNCHECKED_CAST")
                    val rawOutput = ((output?.get(0)?.value) as Array<FloatArray>)[0]
                    val prediction = rawOutput.withIndex().maxByOrNull { it.value }?.index
                    Log.d("ModelManager","Prediction: $prediction")
                    return prediction!!
                }
            }
        }

        return -1 // in case of error
    }

    fun validate(context: Context): Metrics {
        // Test set
        val data = DataLoader().loadDataAndLabels(context = context, "data_21.bin")
        val predictions = mutableListOf<Int>()
        val true_labels = mutableListOf<Int>()

        ortSession = ortEnv?.createSession(inferenceModelPath)
        ortEnv?.use {
            val shape = longArrayOf(1, 18, 1024)
            for (sample in data) {
                val tensor =
                    OnnxTensor.createTensor(ortEnv, floatArrayToFloatBuffer(sample.data), shape)
                tensor.use {
                    val output = ortSession?.run(Collections.singletonMap("input", tensor))
                    output.use {
                        @Suppress("UNCHECKED_CAST")
                        val rawOutput = ((output?.get(0)?.value) as Array<FloatArray>)[0]
                        val prediction = rawOutput.withIndex().maxByOrNull { it.value }?.index
                        true_labels.add(sample.label)
                        predictions.add(prediction!!)
                    }
                }
            }
        }
        return computeMetrics(true_labels.toIntArray(), predictions.toIntArray())
    }

    fun saveModel(context: Context, profileViewModel: ProfileViewModel? = null) {
        val currentMetrics = validate(context)

        val modelDirectory = context.filesDir
        modelDirectory.listFiles()?.forEach {
            Log.d("Trainer", "File: ${it.name}")
        }

        val modelFile = File(modelDirectory, "exported_model.onnx")
        val modelPath: Path = modelFile.toPath()

        val checkpointFile = File(modelDirectory, "checkpoint")
        val checkpointPath: Path = checkpointFile.toPath()

        ortTrainingSession?.exportModelForInference(modelPath, arrayOf("output"))
        ortTrainingSession?.saveCheckpoint(checkpointPath, true)

        inferenceModelPath = modelPath.toString()
        val newMetrics = validate(context)

        ortTrainingSession = ortEnv?.createTrainingSession(
            checkpointPath.toString(),
            trainModelPath,
            evalModelPath,
            optimizerModelPath
        )

        if (newMetrics.f1 > currentMetrics.f1) {
            Log.d("Trainer", "Saving model to Firebase: $modelFile, $profileViewModel")
            profileViewModel?.saveModelToFirebase(modelFile)
            runBlocking {
                profileViewModel?.updateMetricsFromUI(newMetrics)
            }
        } else {
            Log.d("Trainer", "New model is not better, discarding")
            runBlocking {
                profileViewModel?.updateMetricsFromUI(currentMetrics)
            }
        }
    }

    fun updateInferenceModel(modelFile: File) {
        try {
            inferenceModelPath = modelFile.absolutePath
            ortSession?.close()
            ortSession = ortEnv?.createSession(inferenceModelPath)
            Log.d("ModelManager", "Updated inference model to: ${modelFile.absolutePath}")
        } catch (e: Exception) {
            Log.e("ModelManager", "Error updating inference model", e)
        }
    }
}