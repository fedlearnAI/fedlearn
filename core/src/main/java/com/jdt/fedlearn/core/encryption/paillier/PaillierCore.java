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

package com.jdt.fedlearn.core.encryption.paillier;

import com.jdt.fedlearn.core.type.data.KeyPair;

import java.math.*;
import java.util.*;


/**
 * Paillier核心算法，参考维基百科和Pascal Paillier 的相关论文
 * 公钥是n, 私钥是lambda 和 n
 */
public class PaillierCore {

    /**
     * a random integer in Z*_{n^2} where gcd (L(g^lambda mod n^2), n) = 1.
     */
    private static BigInteger g = new BigInteger("2");
    /**
     * number of bits of modulus, default value is 1024
     */
    private static int bitLength = 1024;

    /**
     * generate the public key and private key.
     * <p>
     *
     * @param bitLengthVal number of bits of modulus from user
     * @param certainty    The probability that the new BigInteger represents a prime number will exceed (1 - 2^(-certainty)).
     *                     The execution time of this constructor is proportional to the value of this parameter.
     */
    public static KeyPair keyGenerate(int bitLengthVal, int certainty) {
        BigInteger gVal = new BigInteger("2");
        return keyGenerate(bitLengthVal, certainty, gVal);
    }

    /**
     * generate the public key and private key.
     * <p>
     *
     * @param bitLengthVal 密钥长度，比特数，一般选512/1024/2048/4096等2的整数倍
     * @param certainty    The probability that the new BigInteger represents a prime number will exceed (1 - 2^(-certainty)).
     *                     The execution time of this constructor is proportional to the value of this parameter.
     * @param gVal         a random integer g
     */
    public static KeyPair keyGenerate(int bitLengthVal, int certainty, BigInteger gVal) {
        bitLength = bitLengthVal;
        g = gVal;
        // 生成两个大整数 p 和 q
        BigInteger p = new BigInteger(bitLength / 2, certainty, new Random());
        BigInteger q = new BigInteger(bitLength / 2, certainty, new Random());

        // 计算 n = p*q，并提前计算 n的平方
        BigInteger n = p.multiply(q);
        BigInteger nSquare = n.multiply(n);

        //  * lambda = lcm(p-1, q-1) = (p-1)*(q-1)/gcd(p-1, q-1).
        BigInteger lambda = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE)).divide(
                p.subtract(BigInteger.ONE).gcd(q.subtract(BigInteger.ONE)));

        // check whether g is good
        if (g.modPow(lambda, nSquare).subtract(BigInteger.ONE).divide(n).gcd(n).intValue() != 1) {
            System.exit(1);
        }
        return new KeyPair(n, lambda);
    }

    /**
     *
     * @param m         需要加密的整数
     * @param publicKey 公钥
     * @param r         随机整数
     * @return 密文
     */
    public static BigInteger encrypt(BigInteger m, BigInteger publicKey, BigInteger r) {
        BigInteger nSquare = publicKey.multiply(publicKey);
        return g.modPow(m, nSquare).multiply(r.modPow(publicKey, nSquare)).mod(nSquare);
    }

    /**
     * 生成随机数后，调用同名加密函数完成加密
     * @param m         明文
     * @param publicKey 公钥
     * @return 密文
     */
    public static BigInteger encrypt(BigInteger m, BigInteger publicKey) {
        return encrypt(m, publicKey, new BigInteger(bitLength, new Random()));
    }


    /**
     * @param em1       密文1
     * @param em2       密文2
     * @param publicKey 公钥
     * @return 密文和
     */
    public static BigInteger add(BigInteger em1, BigInteger em2, BigInteger publicKey) {
        BigInteger nSquare = publicKey.multiply(publicKey);
        return em1.multiply(em2).mod(nSquare);
    }

    /**
     * @param em1 密文1
     * @param m2  明文乘数
     * @return 密文乘积
     */
    public static BigInteger multiply(BigInteger em1, BigInteger m2, BigInteger publicKey) {
        return em1.modPow(m2, publicKey.multiply(publicKey));
    }


    /**
     * lambda 和 publicKey共同组出私钥
     *
     * @param em1       密文
     * @param lambda    密钥
     * @param publicKey 公钥
     * @return 解密后的明文
     */
    public static BigInteger decrypt(BigInteger em1, BigInteger lambda, BigInteger publicKey) {
        BigInteger nSquare = publicKey.multiply(publicKey);
        BigInteger u = g.modPow(lambda, nSquare).subtract(BigInteger.ONE).divide(publicKey).modInverse(publicKey);
        return em1.modPow(lambda, nSquare).subtract(BigInteger.ONE).divide(publicKey).multiply(u).mod(publicKey);
    }
}

