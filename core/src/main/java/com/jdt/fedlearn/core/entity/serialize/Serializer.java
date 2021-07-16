package com.jdt.fedlearn.core.entity.serialize;

import com.jdt.fedlearn.core.entity.Message;

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
