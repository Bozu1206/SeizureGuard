# Project 7A SeizureGuard



#### Example, reading from bin file - To Be Checked
```kotlin
import android.content.Context
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class DataSample(
    val data: FloatArray,
    val label: Int
)
fun loadDataAndLabels(context: Context):
Array<DataSample> {
    val assetManager = context.assets
    val inputStream = assetManager.open("data_with_labels.bin")

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


```
