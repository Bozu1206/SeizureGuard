package com.tutsplus.bleadvertising;

public class DataSample {
    private float[] data;
    private int label;

    public DataSample(float[] data, int label) {
        this.data = data;
        this.label = label;
    }

    public float[] getData() {
        return data;
    }

    public int getLabel() {
        return label;
    }

    public void setData(float[] data) {
        this.data = data;
    }

    public void setLabel(int label) {
        this.label = label;
    }
}
