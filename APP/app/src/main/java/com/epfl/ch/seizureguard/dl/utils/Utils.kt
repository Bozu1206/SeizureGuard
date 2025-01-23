package com.epfl.ch.seizureguard.dl.utils

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.LongBuffer

object utils {

    fun intToLongBuffer(vararg values: Int): LongBuffer {
        val buffer = LongBuffer.allocate(values.size)
        for (value in values) {
            buffer.put(value.toLong())
        }
        buffer.flip()
        return buffer
    }

    fun floatArrayToFloatBuffer(floatArray: FloatArray): FloatBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(floatArray.size * 4)
        byteBuffer.order(ByteOrder.nativeOrder())
        val floatBuffer = byteBuffer.asFloatBuffer()
        floatBuffer.put(floatArray)
        floatBuffer.position(0)
        return floatBuffer
    }

}