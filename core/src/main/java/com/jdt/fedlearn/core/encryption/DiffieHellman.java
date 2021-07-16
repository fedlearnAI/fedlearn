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

package com.jdt.fedlearn.core.encryption;

import com.jdt.fedlearn.core.util.HashUtil;

import java.math.BigInteger;
import java.util.Random;


/**
 * 对称秘钥交换算法
 */
public class DiffieHellman {

    public static BigInteger a1(BigInteger g, BigInteger n, BigInteger x) {
        return g.multiply(x).mod(n);
    }

    public static BigInteger trans(String text, BigInteger g, BigInteger n, BigInteger randomA, BigInteger randomB)  {
        //A、B 两方各有一个随机数，
        BigInteger hashUid2 = HashUtil.md5(text);
//        BigInteger randomA = new BigInteger("19");
        hashUid2 = hashUid2.multiply(randomA);
        BigInteger a2s = a1(g, n, hashUid2);
        //B方操作
//        BigInteger randomB = new BigInteger("20");
        BigInteger res2 = a1(a2s, n, randomB);

        return res2;
    }

    public static String trans1(String singleUid, BigInteger g, BigInteger n, BigInteger random)  {
        BigInteger hashUid2 = HashUtil.md5(singleUid);
//        BigInteger randomA = new BigInteger("19");
        hashUid2 = hashUid2.multiply(random);
        BigInteger a2s = a1(g, n, hashUid2);
        return a2s.toString();
    }

    public static String[] trans1(String[] uidArray, BigInteger g, BigInteger n, BigInteger random)  {
        String[] res = new String[uidArray.length];
        for (int i = 0; i < res.length; i++) {
            String singleUid = uidArray[i];
            String singleRes = trans1(singleUid, g, n, random);
            res[i] = singleRes;
        }
        return res;
    }


    public static String trans2(String cipherUid, BigInteger g, BigInteger n, BigInteger random) {
        BigInteger c = new BigInteger(cipherUid);
        BigInteger res2 = a1(c, n, random);
        return res2.toString();
    }

    public static String[] trans2(String[] cipherUidArray, BigInteger g, BigInteger n, BigInteger random) {
        String[] res = new String[cipherUidArray.length];
        return res;
    }

    public static BigInteger generateG(int bitLength,  int certainty){
        return new BigInteger(bitLength / 2, certainty, new Random());
    }

    public static BigInteger generateG(int bitLength){
        int certainty = 64;
        return new BigInteger(bitLength / 2, certainty, new Random());
    }

    public static BigInteger generateG(){
        int bitLength = 128;
        int certainty = 64;
        return new BigInteger(bitLength / 2, certainty, new Random());
    }
}

