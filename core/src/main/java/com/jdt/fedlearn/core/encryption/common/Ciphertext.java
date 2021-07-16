package com.jdt.fedlearn.core.encryption.common;

public interface Ciphertext {
    /**
     * 将对象类型的密文序列化
     * @return 字符串类型的密文
     */
    String serialize();
}
