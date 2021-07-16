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
package com.jdt.fedlearn.core.encryption.affine;



import com.jdt.fedlearn.core.encryption.common.GeneralUtil;
import com.jdt.fedlearn.core.type.data.Tuple2;
import com.jdt.fedlearn.core.type.data.Tuple3;

import java.math.BigInteger;

/**
 * core affine homomorphic encryption, just support positive integer.
 * pure big integer version, do not support negative or float number
 */
public class AffineBase {

    public static Tuple3<BigInteger,BigInteger,BigInteger> keyGenerate(int keySize) {
        BigInteger minimum = GeneralUtil.number100;
        BigInteger n = GeneralUtil.randBit(keySize, minimum);
        BigInteger one = BigInteger.ONE;
        BigInteger a = GeneralUtil.randBit(keySize, minimum);
        // gcd(n, a) == 1
        while (n.gcd(a).compareTo(one) != 0) {
            a = GeneralUtil.randBit(keySize, minimum);
        }
        BigInteger b = GeneralUtil.randBit(keySize, minimum);
        return new Tuple3<>(n, a, b);
    }

    /**
     *
     * @param number the number need to encrypt
     * @param n modulus
     * @param a key
     * @param b bias
     * @return encrypted number expressed by number and count of bias
     * only support positive integer
     */
    public static Tuple2<BigInteger, Integer> encrypt(BigInteger number, BigInteger n, BigInteger a, BigInteger b) {
        BigInteger cipher1 = a.multiply(number).add(b).mod(n);
        return new Tuple2<>(cipher1, 1);
    }

    /**
     *
     * @param ciphertext 密文
     * @param n modulus
     * @param aInverse inverse of a
     * @param b bias
     * @return 加密结果
     */
    public static BigInteger decrypt(Tuple2<BigInteger, BigInteger> ciphertext, BigInteger n, BigInteger aInverse, BigInteger b) {
        BigInteger cipher1 = ciphertext._1();
        BigInteger bias = ciphertext._2();
        BigInteger middle = cipher1.subtract(b.multiply(bias));

        return aInverse.multiply(middle).mod(n);
    }


    public static Tuple2<BigInteger,BigInteger> add(BigInteger aTimes, BigInteger aBias, BigInteger bTimes, BigInteger bBias) {
        BigInteger cipher = aTimes.add(bTimes);
        BigInteger bias = aBias.add(bBias);
        return new Tuple2<>(cipher, bias);
    }

    /**
     *
     * @param times one of element in cipher text
     * @param bias bias in cipher text
     * @param val1 乘数
     * @return 结果
     */
    public static Tuple2<BigInteger,BigInteger> multiply(BigInteger times, BigInteger bias, BigInteger val1) {
        BigInteger cipher1 = times.multiply(val1);
        BigInteger bias1 = bias.multiply(val1);
        return new Tuple2<>(cipher1, bias1);
    }
}
