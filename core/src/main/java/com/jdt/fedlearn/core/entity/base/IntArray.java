package com.jdt.fedlearn.core.entity.base;

import com.jdt.fedlearn.common.entity.core.Message;


public class IntArray implements Message {
    private final int[] data;

    public IntArray(int[] data) {
        this.data = data;
    }

    public int[] getData() {
        return data;
    }

}
