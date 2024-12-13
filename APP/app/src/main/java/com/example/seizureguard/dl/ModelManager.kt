package com.example.seizureguard.dl

import ai.onnxruntime.*
import android.content.Context
import android.util.Log
import com.example.seizureguard.dl.metrics.ComputeMetrics.computeMetrics
import com.example.seizureguard.dl.utils.utils.floatArrayToFloatBuffer
import com.example.seizureguard.dl.utils.utils.intToLongBuffer
import java.io.File
import java.nio.file.Path
import java.util.Collections


class ModelManager() {
    private var ortEnv: OrtEnvironment? = null
    private var ortTrainingSession: OrtTrainingSession? = null
    private var ortSession: OrtSession? = null

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
        inferenceModelPath: String
    ) : this() {
        ortEnv = OrtEnvironment.getEnvironment()

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
            val tensor =
                OnnxTensor.createTensor(ortEnv, floatArrayToFloatBuffer(sample.data), shape)
            tensor.use {
                val output = ortSession?.run(Collections.singletonMap("input", tensor))
                output.use {
                    @Suppress("UNCHECKED_CAST")
                    val rawOutput = ((output?.get(0)?.value) as Array<FloatArray>)[0]
                    val prediction = rawOutput.withIndex().maxByOrNull { it.value }?.index
                    return prediction!!
                }
            }
        }

        return -1 // in case of error
    }

    fun validate(context: Context) {
        var data = DataLoader().loadDataAndLabels(context = context, "data_21.bin")
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

        val metrics = computeMetrics(true_labels.toIntArray(), predictions.toIntArray())
        Log.d("Trainer", "Metrics: $metrics")
    }

    fun printFilesDirContents(context: Context) {
        val filesDir = context.filesDir // Get the directory
        val files = filesDir.listFiles() // List the files in the directory

        if (files != null && files.isNotEmpty()) {
            println("Contents of filesDir (${filesDir.absolutePath}):")
            for (file in files) {
                println(" - ${file.name}")
            }
        } else {
            println("filesDir (${filesDir.absolutePath}) is empty or inaccessible.")
        }
    }

    fun saveModel(context: Context) {
        val modelDirectory = context.filesDir
        val modelFile = File(modelDirectory, "exported_model.onnx")
        val modelPath: Path = modelFile.toPath()

        val checkpointFile = File(modelDirectory, "checkpoint")
        val checkpointPath: Path = checkpointFile.toPath()

        ortTrainingSession?.exportModelForInference(modelPath, arrayOf("output"))
        ortTrainingSession?.saveCheckpoint(checkpointPath, true)

        // update inference model with fine-tuned model
        inferenceModelPath = modelPath.toString()

        // update training session with new model
        ortTrainingSession = ortEnv?.createTrainingSession(
            checkpointPath.toString(),
            trainModelPath,
            evalModelPath,
            optimizerModelPath
        )
    }
}