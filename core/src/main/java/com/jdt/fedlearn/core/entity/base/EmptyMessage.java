package com.jdt.fedlearn.core.entity.base;

import com.jdt.fedlearn.core.entity.Message;

public class EmptyMessage implements Message {
    private static final EmptyMessage defaultMessage = new EmptyMessage();

    public static EmptyMessage message() {
        return defaultMessage;
    }
}
