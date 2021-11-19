package com.jdt.fedlearn.tools.serializer;

import com.jdt.fedlearn.common.entity.core.Message;

public interface Serializer {
    /**
     *
     * @param message Message 类型的实体
     * @return 字符串
     */
     String serialize(Message message);

    /**
     *
     * @param str 字符串
     * @return 对象
     */
     Message deserialize(String str);
}
