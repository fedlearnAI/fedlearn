package com.jdt.fedlearn.core.entity.base;

import com.jdt.fedlearn.common.entity.core.Message;

public class EmptyMessage implements Message {
    private static final EmptyMessage defaultMessage = new EmptyMessage();

    public static EmptyMessage message() {
        return defaultMessage;
    }
}
