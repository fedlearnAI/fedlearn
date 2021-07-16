package com.jdt.fedlearn.core.encryption.common;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GeneralUtil {
    // 2 ^ 100
    public static final BigInteger number100 = new BigInteger("1267650600228229401496703205376");

    // rand number by bit
    public static BigInteger randBit(int bitLength) {
        return new BigInteger(bitLength, new Random());
    }

    // rand number by bit with minimum integer
    public static BigInteger randBit(int bitLength, BigInteger minimum) {
        BigInteger n = randBit(bitLength);
        while (n.compareTo(minimum.multiply(minimum)) < 0) {
            n = randBit(bitLength);
        }
        return n;
    }

    public static List<BigInteger> modInverse(int key_round, List<BigInteger> n, List<BigInteger> a) {
        List<BigInteger> inv = new ArrayList<>();
        for (int i = 0; i < key_round; i++) {
            inv.add(a.get(i).modInverse(n.get(i)));
        }
        return inv;
    }
}
