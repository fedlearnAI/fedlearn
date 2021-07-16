package com.jdt.fedlearn.core.entity.base;

import com.jdt.fedlearn.core.entity.Message;

public class StringArray implements Message {
    private final String[] data;

    public StringArray(String[] data) {
        this.data = data;
    }

    public String[] getData() {
        return data;
    }
}
