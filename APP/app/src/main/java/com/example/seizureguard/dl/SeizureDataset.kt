package com.example.seizureguard.dl

import org.jetbrains.kotlinx.dl.dataset.DataBatch
import org.jetbrains.kotlinx.dl.dataset.Dataset

class SeizureDataset(private val data: Array<DataSample>, private val transform: ((FloatArray) -> FloatArray)? = null) : Dataset() {
    override fun createDataBatch(batchStart: Int, batchLength: Int): DataBatch {
        val x = Array(batchLength) { FloatArray(data[0].data.size) }
        val y = FloatArray(batchLength)
        for (i in 0 until batchLength) {
            x[i] = getX(batchStart + i)
            y[i] = getY(batchStart + i)
        }
        return DataBatch(x, y, batchLength)
    }

    override fun getX(idx: Int): FloatArray {
        return data[idx].data
    }

    override fun getY(idx: Int): Float {
        return data.get(idx).label.toFloat()
    }

    override fun shuffle(): Dataset {
        data.shuffle()
        return this
    }

    override fun split(splitRatio: Double): Pair<Dataset, Dataset> {
        val splitIndex = (data.size * splitRatio).toInt()
        val first = data.copyOfRange(0, splitIndex)
        val second = data.copyOfRange(splitIndex, data.size)
        return Pair(SeizureDataset(first, transform), SeizureDataset(second, transform))
    }

    override fun xSize(): Int {
        return data[0].data.size
    }
}