package com.jdt.fedlearn.core.encryption.common;

public interface PublicKey {
    /**
     * 将对象类型的秘钥序列化
     * @return 字符串类型的秘钥
     */
    String serialize();

}
