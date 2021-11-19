package com.jdt.fedlearn.core.entity.base;

import com.jdt.fedlearn.common.entity.core.Message;

public class DoubleArray implements Message {
    private final double[] data;

    public DoubleArray(double[] data) {
        this.data = data;
    }

    public double[] getData() {
        return data;
    }
}
