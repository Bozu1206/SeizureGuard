package com.epfl.ch.seizureguard.dl

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.epfl.ch.seizureguard.tools.copyAssetFileOrDir

// Android Service whose main job is to create and manage a single instance of ModelManager
class ModelService() : Service() {
    private val binder = LocalBinder()
    private lateinit var modelManager: ModelManager

    private var isDebugEnabled: Boolean = false

    override fun onCreate() {
        super.onCreate()
    }

    inner class LocalBinder : Binder() {
        fun getService(): ModelService = this@ModelService
    }

    fun getModelManager(): ModelManager {
        return modelManager
    }

    override fun onBind(intent: Intent?): IBinder {
        // 1) Read debug flag from the Intent
        isDebugEnabled = intent?.getBooleanExtra("IS_DEBUG_ENABLED", false) ?: false
        // 2) Create the modelManager with the correct debug flag
        modelManager = makeOrtTrainerAndCopyAssets()
        Log.d("ModelService", "modelManager created: $modelManager (debug=$isDebugEnabled)")

        return binder
    }

    private fun makeOrtTrainerAndCopyAssets(): ModelManager {
        val trainingModelPath = copyFileOrDir("training_artifacts/training_model.onnx")
        val evalModelPath = copyFileOrDir("training_artifacts/eval_model.onnx")
        val checkpointPath = copyFileOrDir("training_artifacts/checkpoint")
        val optimizerModelPath = copyFileOrDir("training_artifacts/optimizer_model.onnx")
        val inferenceModelPath = copyFileOrDir("inference_artifacts/inference.onnx")
        return ModelManager(
            checkpointPath,
            trainingModelPath,
            evalModelPath,
            optimizerModelPath,
            inferenceModelPath,
            isDebugEnabled
        )
    }

    fun copyFileOrDir(path: String): String {
        val dst = java.io.File("$cacheDir/$path")
        copyAssetFileOrDir(assets, path, dst)
        return dst.path
    }
}
