package com.epfl.ch.seizureguard.dl

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class DataSample(
    val data: FloatArray,
    val label: Int
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.createFloatArray()!!,
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeFloatArray(data)
        parcel.writeInt(label)
    }

    override fun describeContents(): Int = 0
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DataSample

        if (!data.contentEquals(other.data)) return false
        if (label != other.label) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + label
        return result
    }

    companion object CREATOR : Parcelable.Creator<DataSample> {
        override fun createFromParcel(parcel: Parcel): DataSample {
            return DataSample(parcel)
        }

        override fun newArray(size: Int): Array<DataSample?> {
            return arrayOfNulls(size)
        }
    }
}

class DataLoader {
    fun loadDataAndLabels(context: Context, name: String): Array<DataSample> {
        val assetManager = context.assets
        val inputStream = assetManager.open(name)

        // Read header (4 integers)
        val headerSize = 4 * 4  // 4 integers * 4 bytes per int32
        val headerBytes = ByteArray(headerSize)
        inputStream.read(headerBytes)
        val headerBuffer = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN)
        val numArrays = headerBuffer.int
        val dim1 = headerBuffer.int
        val dim2 = headerBuffer.int
        val labelsPresent = headerBuffer.int  // 1 indicates labels are present

        // Calculate total number of floats
        val totalFloats = numArrays * dim1 * dim2
        println("numArrays: $numArrays, dim1: $dim1, dim2: $dim2, labelsPresent: $labelsPresent, totalFloats: $totalFloats")

        // Read data
        val dataSize = totalFloats * 4  // float32 is 4 bytes
        val dataBytes = ByteArray(dataSize)
        var bytesRead = 0
        while (bytesRead < dataSize) {
            val result = inputStream.read(dataBytes, bytesRead, dataSize - bytesRead)
            if (result == -1) break
            bytesRead += result
        }

        // Convert bytes to float array
        val dataBuffer = ByteBuffer.wrap(dataBytes).order(ByteOrder.LITTLE_ENDIAN)
        val floatBuffer = dataBuffer.asFloatBuffer()

        // Read labels if present
        val labels = IntArray(numArrays)
        if (labelsPresent == 1) {
            val labelsSize = numArrays * 4  // int32 is 4 bytes
            val labelsBytes = ByteArray(labelsSize)
            bytesRead = 0
            while (bytesRead < labelsSize) {
                val result = inputStream.read(labelsBytes, bytesRead, labelsSize - bytesRead)
                if (result == -1) break
                bytesRead += result
            }
            val labelsBuffer = ByteBuffer.wrap(labelsBytes).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until numArrays) {
                labels[i] = labelsBuffer.int
            }
        } else {
            // If labels are not present, you can handle accordingly
            // For this example, we'll set labels to -1
            for (i in 0 until numArrays) {
                labels[i] = -1
            }
        }

        inputStream.close()

        // Store data and labels in an array of DataSample objects
        val dataSamples = Array(numArrays) { index ->
            val sampleData = FloatArray(dim1 * dim2)
            floatBuffer.get(sampleData)
            DataSample(data = sampleData, label = labels[index])
        }

        return dataSamples
    }
}


