package com.jdt.fedlearn.core.entity.base;

import com.jdt.fedlearn.core.entity.Message;


public class IntArray implements Message {
    private final int[] data;

    public IntArray(int[] data) {
        this.data = data;
    }

    public int[] getData() {
        return data;
    }

}
