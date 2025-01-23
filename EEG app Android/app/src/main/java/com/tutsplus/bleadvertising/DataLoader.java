package com.tutsplus.bleadvertising;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class DataLoader {

    /**
     * Expects a binary file in assets: a 16-byte header + float data + optional label data.
     * - header (little-endian):
     *    int numArrays
     *    int dim1
     *    int dim2
     *    int labelsPresent (1 or 0)
     * - Next: numArrays * dim1 * dim2 floats (4 bytes each)
     * - If labelsPresent == 1, then numArrays * 4 bytes for int labels.
     *
     * Example for 1024 samples, each 18 floats:
     *    numArrays = 1024
     *    dim1 = 18
     *    dim2 = 1   (or 1 if it’s just 1 row)
     *    labelsPresent = 1 or 0
     */
    public DataSample[] loadDataAndLabels(Context context, String assetFileName) throws IOException {
        InputStream inputStream = context.getAssets().open(assetFileName);

        // 4 integers in header => 4 * 4 = 16 bytes
        int headerSize = 4 * 4;
        byte[] headerBytes = new byte[headerSize];
        inputStream.read(headerBytes);

        ByteBuffer headerBuffer = ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN);

        int numArrays     = headerBuffer.getInt();  // number of DataSample
        int dim1          = headerBuffer.getInt();  // first dimension
        int dim2          = headerBuffer.getInt();  // second dimension
        int labelsPresent = headerBuffer.getInt();  // 1 means labels exist

        int totalFloats = numArrays * dim1 * dim2;

        // Read float data
        int dataSize = totalFloats * 4; // float32 => 4 bytes
        byte[] dataBytes = new byte[dataSize];
        int bytesRead = 0;
        while (bytesRead < dataSize) {
            int result = inputStream.read(dataBytes, bytesRead, dataSize - bytesRead);
            if (result == -1) break;
            bytesRead += result;
        }
        ByteBuffer dataBuffer = ByteBuffer.wrap(dataBytes).order(ByteOrder.LITTLE_ENDIAN);

        // If labels are present, read them
        int[] labels = new int[numArrays];
        if (labelsPresent == 1) {
            int labelsSize = numArrays * 4;
            byte[] labelsBytes = new byte[labelsSize];
            bytesRead = 0;
            while (bytesRead < labelsSize) {
                int result = inputStream.read(labelsBytes, bytesRead, labelsSize - bytesRead);
                if (result == -1) break;
                bytesRead += result;
            }
            ByteBuffer labelsBuffer = ByteBuffer.wrap(labelsBytes).order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < numArrays; i++) {
                labels[i] = labelsBuffer.getInt();
            }
        } else {
            // if no labels, just set them to -1
            for (int i = 0; i < numArrays; i++) {
                labels[i] = -1;
            }
        }

        inputStream.close();

        // Build the DataSample array
        DataSample[] dataSamples = new DataSample[numArrays];
        for (int i = 0; i < numArrays; i++) {
            float[] sampleData = new float[dim1 * dim2];
            for (int j = 0; j < (dim1 * dim2); j++) {
                sampleData[j] = dataBuffer.getFloat();
            }
            dataSamples[i] = new DataSample(sampleData, labels[i]);
        }
        return dataSamples;
    }
}
