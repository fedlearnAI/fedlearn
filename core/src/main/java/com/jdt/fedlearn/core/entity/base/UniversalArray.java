package com.jdt.fedlearn.core.entity.base;

import com.jdt.fedlearn.core.entity.Message;

public class UniversalArray<T>  implements Message {
    private final T[] data;

    public UniversalArray(T[] data) {
        this.data = data;
    }

    public T[] getData() {
        return data;
    }
}
