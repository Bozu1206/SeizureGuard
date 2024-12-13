package com.example.seizureguard.dl

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.example.seizureguard.tools.copyAssetFileOrDir

class ModelService : Service() {
    private val binder = LocalBinder()
    private lateinit var modelManager: ModelManager

    override fun onCreate() {
        super.onCreate()
        modelManager = makeOrtTrainerAndCopyAssets()
        Log.d("ModelService", "modelManager created: $modelManager")
        Log.d("ModelService", "ModelService created")
    }

    inner class LocalBinder : Binder() {
        fun getService(): ModelService = this@ModelService
    }

    fun getModelManager(): ModelManager {
        return modelManager
    }

    override fun onBind(intent: Intent?): IBinder {
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
            inferenceModelPath
        )
    }

    private fun copyFileOrDir(path: String): String {
        val dst = java.io.File("$cacheDir/$path")
        copyAssetFileOrDir(assets, path, dst)
        return dst.path
    }
}
