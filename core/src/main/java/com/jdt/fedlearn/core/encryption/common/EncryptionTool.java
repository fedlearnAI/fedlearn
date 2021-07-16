/* Copyright 2020 The FedLearn Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.jdt.fedlearn.core.encryption.common;

import com.jdt.fedlearn.core.entity.serialize.Serializer;

public interface EncryptionTool {

    /**
     *
     * @param bitLengthVal 比特长度
     * @param other 其他需要的参数
     * @return 公私钥
     */
    PrivateKey keyGenerate(int bitLengthVal, int other);

    /**
     *
     * @param number 需要加密的数
     * @param pubKey 公钥
     * @return 密文
     */
    Ciphertext encrypt(double number, PublicKey pubKey);

    /**
     *
     * @param ciphertext1 密文1
     * @param ciphertext2 密文2
     * @param publicKey 公钥
     * @return 密文的和
     */
    Ciphertext add(Ciphertext ciphertext1, Ciphertext ciphertext2, PublicKey publicKey);

    /**
     *
     * @param cipherText 密文
     * @param number double 浮点数
     * @param pubKey 公钥
     * @return 相乘的密文结果
     */
    Ciphertext multiply(Ciphertext cipherText, double number, PublicKey pubKey);

    /**
     *
     * @param cipherText 密文
     * @param number 整数
     * @param pubKey 公钥
     * @return 相乘的密文结果
     */
    Ciphertext multiply(Ciphertext cipherText, int number, PublicKey pubKey);

    /**
     *
     * @param cipherText 密文
     * @param privateKey 私钥
     * @return 解密后的数
     */
    double decrypt(Ciphertext cipherText, PrivateKey privateKey);


    /**
     *
     * @param strCipherText 字符串类型的密文
     * @param privateKey 私钥
     * @return 解密后的明文
     */
    double decrypt(String strCipherText, PrivateKey privateKey);

    /**
     *
     * @param strKey 字符串类型的秘钥
     * @return 私钥
     */
    PrivateKey restorePrivateKey(String strKey);

    /**
     *
     * @param strKey 字符串类型的公钥
     * @return 公钥
     */
    PublicKey restorePublicKey(String strKey);

    /**
     *
     * @param cipherString 字符串类型的密文
     * @return 密文对象
     */
    Ciphertext restoreCiphertext(String cipherString);
}
