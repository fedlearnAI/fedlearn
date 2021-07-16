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

package com.jdt.fedlearn.core.encryption.IterativeAffine;

import com.jdt.fedlearn.core.encryption.LibGMP.GMP;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class IterativeAffineTool {

    // rand number by bit
    public static BigInteger randBit(int bitLength) {
        Random randNum = new Random();
        return new BigInteger(bitLength, randNum);
    }

    // rand number by bit with minimum integer
    public static BigInteger randBit(int bitLength, BigInteger minimum) {
        BigInteger n = randBit(bitLength);
        while (n.compareTo(minimum.multiply(minimum)) < 0) {
            n = randBit(bitLength);
        }
        return n;
    }

    public static List<Integer> getBitsArray(int startBit, int endBit, int bitsDiff, int length) {
        int[] bits = new int[length];
        bits[length - 1] = endBit;
        for (int i = 1; i < length; i++) {
            bits[length - 1 - i] = bits[length - i] - bitsDiff - 1;
        }
        // check start bit
        if (bits[0] < startBit) {
            throw new IllegalArgumentException("Cannot create bits array, check arguments");
        }
        return Arrays.stream(bits).boxed().collect(Collectors.toList());
    }

    public static IterativeAffineKey generateKeyPair() {
        int keySize = 1024;
        int keyRound = 2;
        BigInteger encodePrecision = new BigInteger("9223372036854775808"); // 2 ^ 63
        return generateKeyPair(keySize, keyRound, encodePrecision);
    }

    public static IterativeAffineKey generateKeyPair(int keySize) {
        int keyRound = 2;
        BigInteger encodePrecision = new BigInteger("9223372036854775808"); // 2 ^ 63
        return generateKeyPair(keySize, keyRound, encodePrecision);
    }

    public static IterativeAffineKey generateKeyPair(int keySize, int keyRound) {
        BigInteger encodePrecision = new BigInteger("9223372036854775808"); // 2 ^ 63
        return generateKeyPair(keySize, keyRound, encodePrecision);
    }

    // TODO: This encryption has problem, the failed case is checked in test/resource/ folder, see unit test for more information
    public static IterativeAffineKey generateKeyPair(int keySize, int keyRound, BigInteger encodePrecision) {
        List<BigInteger> ns = new ArrayList<>();
        List<BigInteger> a = new ArrayList<>();

        List<Integer> bits = getBitsArray((int) keySize / 2, keySize, 64 * 3, keyRound); // 2 ** 63

        List<Integer> index = IntStream.range(0, keyRound).boxed().collect(Collectors.toList());

        BigInteger one = new BigInteger("1");

        BigInteger minimum = new BigInteger("1267650600228229401496703205376"); // 2 ** 100
        BigInteger gap = encodePrecision.multiply(encodePrecision);

        for (int i = 0; i < keyRound; i++) {
            //BigInteger ni = randBit(keySize, minimum);
            BigInteger ni = randBit(bits.get(i), minimum);
            minimum = minimum.multiply(gap);
            while (true) {
                double aRatio = Math.random();
                int aSize = Math.max(1, (int) (keySize * aRatio));
                BigInteger ai = IterativeAffineTool.randBit(aSize);
                if (ni.gcd(ai).compareTo(one) == 0) { // gcd(ni, ai) == 1
                    ns.add(ni);
                    a.add(ai);
                    break;
                }
            }
        }

        // sort n and a
        List<BigInteger> finalNs = ns;
        List<BigInteger> finalA = a;
        index.sort(Comparator.comparing(item -> finalNs.get(item)));
        ns = IntStream.range(0, keyRound).mapToObj(i -> finalNs.get(index.get(i))).collect(Collectors.toList());
        a = IntStream.range(0, keyRound).mapToObj(i -> finalA.get(index.get(i))).collect(Collectors.toList());

        BigInteger g = IterativeAffineTool.randBit((int) (keySize / 10));
        BigInteger x = IterativeAffineTool.randBit(160);

        IterativeAffineKey key = new IterativeAffineKey(ns, a, g, x, encodePrecision);
        // check if this key works
        double test = Math.random();
        double test1 = key.decrypt(key.encrypt(test));
        if (Math.abs(test - test1) < 1e-8) {
            return key;
        } else {
            // generate another key
            return generateKeyPair(keySize, keyRound, encodePrecision);
        }
    }

    /**
     * GMP version of (a * b) mod n
     */
    public static BigInteger multModGMP(BigInteger a, BigInteger b, BigInteger n) {
        return GMP.remainder(GMP.multiply(a, b), n);
    }

    /**
     * GMP version of (a ^ b) mod n
     */
    public static BigInteger powModGMP(BigInteger a, BigInteger b, BigInteger n) {
        return GMP.modPowSecure(a, b, n);
    }
}
