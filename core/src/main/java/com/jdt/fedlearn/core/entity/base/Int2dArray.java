package com.jdt.fedlearn.core.entity.base;

import com.jdt.fedlearn.common.entity.core.Message;

import java.util.List;

public class Int2dArray implements Message {
    private final int[][] data;

    public Int2dArray() {
        this.data = new int[0][0];
    }

    public Int2dArray(int[][] data) {
        this.data = data;
    }

    public Int2dArray(List<int[]> data) {
        this.data = data.toArray(new int[0][]);
    }

    public int[][] getData() {
        return data;
    }
}
