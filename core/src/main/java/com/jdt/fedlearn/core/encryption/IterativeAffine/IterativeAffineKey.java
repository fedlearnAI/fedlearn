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

import com.jdt.fedlearn.core.encryption.common.PrivateKey;
import com.jdt.fedlearn.core.encryption.common.PublicKey;
import com.jdt.fedlearn.core.entity.randomForest.DataUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;

/**
 * Iterative affine encryption scheme
 */
// TODO: multiple in multiply function does not work yet!
public class IterativeAffineKey implements PublicKey, PrivateKey {
    List<BigInteger> n, a, a_inv;
    BigInteger g, h, x, encodedPrecision;
    BigDecimal encodedPrecisionDecimal;
    int key_round;

    public IterativeAffineKey(List<BigInteger> n,
                              List<BigInteger> a,
                              BigInteger g,
                              BigInteger x) {
        if (!(n.size() == a.size())) {
            throw new IllegalArgumentException("Length of n must be aligned with length of a");
        }
        this.n = n;
        this.a = a;
        this.g = g;
        this.x = x;
        this.h = g.multiply(x).mod(n.get(0));
        this.key_round = a.size();
        this.encodedPrecision = new BigInteger("1267650600228229401496703205376"); // 2 ** 100
        this.encodedPrecisionDecimal = new BigDecimal("1267650600228229401496703205376");
        this.a_inv = modInverse();
    }

    public IterativeAffineKey(List<BigInteger> n,
                              List<BigInteger> a,
                              BigInteger g,
                              BigInteger x,
                              BigInteger encodedPrecision) {
        if (!(n.size() == a.size())) {
            throw new IllegalArgumentException("Length of n must be aligned with length of a");
        }
        this.n = n;
        this.a = a;
        this.g = g;
        this.x = x;
        this.h = g.multiply(x).mod(n.get(0));
        this.key_round = a.size();
        this.encodedPrecision = encodedPrecision;
        this.encodedPrecisionDecimal = new BigDecimal(encodedPrecision);
        this.a_inv = modInverse();
    }

    public IterativeAffineCiphertext encrypt(double plaintext) {
        if (plaintext < 0) {
            throw new IllegalArgumentException("IterativeAffine encryption scheme only support positive number!");
        }
        BigDecimal plaintext1 = new BigDecimal(plaintext);
        plaintext1 = plaintext1.multiply(encodedPrecisionDecimal);
        return rawEncrypt(plaintext1.toBigInteger());
    }

    private IterativeAffineCiphertext rawEncrypt(BigInteger plaintext) {

        // random encode function
        BigInteger y = IterativeAffineTool.randBit(160);

        BigInteger cipher1 = y.multiply(g).mod(n.get(0));
        BigInteger cipher2 = plaintext.add(y.multiply(h)).mod(n.get(0));
        // GMP version
//        BigInteger cipher1 = multModGMP(y, g, n.get(0));
//        BigInteger cipher2 = GMP.add(plaintext, multModGMP(y, h, n.get(0)));

        // raw_encrypt_round
        for (int i = 0; i < key_round; i++) {
            cipher2 = cipher2.multiply(a.get(i)).mod(n.get(i));
            // GMP version
//            cipher2 = multModGMP(cipher2, a.get(i), n.get(i));
        }

        IterativeAffineCiphertext ciphertext = new IterativeAffineCiphertext(cipher1, cipher2, n.get(n.size() - 1));
        return ciphertext;
    }

    public double decrypt(IterativeAffineCiphertext ciphertext) {
        return rawDecrypt(ciphertext).divide(encodedPrecisionDecimal).doubleValue();
    }

    private BigDecimal rawDecrypt(IterativeAffineCiphertext ciphertext) {
        BigInteger cipher1 = ciphertext.getCipher1();
        BigInteger cipher2 = ciphertext.getCipher2();

        // raw_decrypt_round
        for (int i = key_round - 1; i > -1; i--) {
            cipher1 = checkAndSubtract(cipher1, n.get(i));
            cipher2 = checkAndSubtract(cipher2, n.get(i));

            cipher1 = cipher1.mod(n.get(i));
            cipher2 = a_inv.get(i).multiply(cipher2.mod(n.get(i))).mod(n.get(i));
            // GMP version
//            cipher1 = GMP.remainder(cipher1, n.get(i));
//            cipher2 = multModGMP(a_inv.get(i), GMP.remainder(cipher2, n.get(i)), n.get(i));

        }
        return decode(new IterativeAffineCiphertext(cipher1,
                cipher2,
                ciphertext.getN_final(),
                ciphertext.getLongMultiple(),
                ciphertext.getMult_times()));
    }

    private BigDecimal decode(IterativeAffineCiphertext ciphertext) {
        BigInteger tmp = ciphertext.getCipher2().subtract(x.multiply(ciphertext.getCipher1())).mod(n.get(0));
        // GMP version
//        BigInteger tmp = GMP.subtract(ciphertext.getCipher2(), multModGMP(x, ciphertext.getCipher1(), n.get(0)));
        //BigDecimal numerator = new BigDecimal(checkAndSubtract(tmp, n.get(0)));
        BigDecimal numerator = new BigDecimal(tmp);
        BigDecimal denominator = new BigDecimal(ciphertext.getMultiple().pow(ciphertext.getMult_times()));
        return numerator.divide(denominator, 100, RoundingMode.HALF_UP);
    }

    private List<BigInteger> modInverse() {
        List<BigInteger> inv = new ArrayList<>();
        for (int i = 0; i < key_round; i++) {
            inv.add(a.get(i).modInverse(n.get(i)));
        }
        return inv;
    }

    private BigInteger checkAndSubtract(BigInteger val, BigInteger n) {
        // if val / n > 0.9 then subtract
        // first compare, length of string
        if (n.toString().length() > val.toString().length()) {
            // n >> val
            return val;
        } else if (n.toString().length() < val.toString().length()) {
            // val >> n
            return val.subtract(n);
        } else {
            // val and n are comparable do division
            BigDecimal tmp = new BigDecimal(val);
            double ratio = tmp.divide(new BigDecimal(n), 2, RoundingMode.HALF_UP).doubleValue();
            if (ratio > 0.9) {
                return val.subtract(n);
            } else {
                return val;
            }
        }
    }

    // getter
    public BigInteger getEncodedPrecision() {
        return encodedPrecision;
    }

    public BigDecimal getEncodedPrecisionDecimal() {
        return encodedPrecisionDecimal;
    }

    public BigInteger getG() {
        return g;
    }

    public BigInteger getH() {
        return h;
    }

    public BigInteger getX() {
        return x;
    }

    public int getKey_round() {
        return key_round;
    }

    public List<BigInteger> getA() {
        return a;
    }

    public List<BigInteger> getA_inv() {
        return a_inv;
    }

    public List<BigInteger> getN() {
        return n;
    }

    // serialization and deserialization
    public String toJson() {
        Map<String, String> map = new HashMap<>();
        map.put("key_round", String.valueOf(key_round));
        map.put("g", g.toString());
        map.put("x", x.toString());
        map.put("encodedPrecision", encodedPrecision.toString());
        for (int i = 0; i < key_round; i++) {
            map.put("n" + i, n.get(i).toString());
            map.put("a" + i, a.get(i).toString());
            map.put("ainv" + i, a_inv.get(i).toString());
        }
        return DataUtils.mapToString(map);
    }

    public static IterativeAffineKey parseJson(String jsonStr) {
        Map<String, String> map = DataUtils.stringToMap(jsonStr, true, true);
        int tmp_key_round = Integer.parseInt(map.get("key_round"));
        BigInteger tmp_g = new BigInteger(map.get("g"));
        BigInteger tmp_x = new BigInteger(map.get("x"));
        BigInteger tmp_encodedPrecision = new BigInteger(map.get("encodedPrecision"));
        List<BigInteger> tmp_ns = new ArrayList<>();
        List<BigInteger> tmp_as = new ArrayList<>();
        for (int i = 0; i < tmp_key_round; i++) {
            tmp_ns.add(new BigInteger(map.get("n" + i)));
            tmp_as.add(new BigInteger(map.get("a" + i)));
        }
        return new IterativeAffineKey(tmp_ns, tmp_as, tmp_g, tmp_x, tmp_encodedPrecision);
    }

    @Override
    public PublicKey generatePublicKey() {
        return null;
    }

    @Override
    public String serialize() {
        return toJson();
    }

    public PublicKey deserialize(String strKey) {
        return parseJson(strKey);
    }
}
