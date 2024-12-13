package com.example.seizureguard.tools

import android.content.res.AssetManager
import java.io.File

fun copyAssetFile(assetManager: AssetManager, assetPath: String, dstFile: File) {
    check(!dstFile.exists() || dstFile.isFile)

    dstFile.parentFile?.mkdirs()

    val assetContents = assetManager.open(assetPath).use { assetStream ->
        val size: Int = assetStream.available()
        val buffer = ByteArray(size)
        assetStream.read(buffer)
        buffer
    }

    java.io.FileOutputStream(dstFile).use { dstStream ->
        dstStream.write(assetContents)
    }
}

fun copyAssetFileOrDir(assetManager: AssetManager, assetPath: String, dstFileOrDir: File) {
    val assets: Array<String>? = assetManager.list(assetPath)
    if (assets!!.isEmpty()) {
        copyAssetFile(assetManager, assetPath, dstFileOrDir)
    } else {
        for (i in assets.indices) {
            val assetChild = (if (assetPath.isEmpty()) "" else "$assetPath/") + assets[i]
            val dstChild = dstFileOrDir.resolve(assets[i])
            copyAssetFileOrDir(assetManager, assetChild, dstChild)
        }
    }
}
