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

package com.jdt.fedlearn.core.encryption.IterativeAffineNew;

import com.jdt.fedlearn.core.encryption.common.*;
import com.jdt.fedlearn.core.entity.randomForest.DataUtils;
import com.jdt.fedlearn.core.exception.NotMatchException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class IterativeAffineToolNew implements EncryptionTool {
    private static final BigInteger number63 = new BigInteger("9223372036854775808"); //2 ^ 63
    private static final BigInteger number100 = new BigInteger("1267650600228229401496703205376"); // 2 ^ 100
    private static final int defaultSize = 1024;
    private static final int defaultRound = 2;

    public List<Integer> getBitsArray(int startBit, int endBit, int bitsDiff, int length) {
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

    public IterativeAffineKeyNew keyGenerate(int keySize, int keyRound) {
        //default encode precision is 2 ^ 63
        return keyGenerate(keySize, keyRound, number63);
    }

    // TODO: This encryption has problem, the failed case is checked in test/resource/ folder, see unit test for more information
    public IterativeAffineKeyNew keyGenerate(int keySize, int keyRound, BigInteger encodePrecision) {
        List<BigInteger> ns = new ArrayList<>();
        List<BigInteger> a = new ArrayList<>();

        List<Integer> bits = getBitsArray((int) keySize / 2, keySize, 64 * 3, keyRound); // 2 ** 63

        List<Integer> index = IntStream.range(0, keyRound).boxed().collect(Collectors.toList());

        BigInteger one = new BigInteger("1");

        BigInteger minimum = number100; // 2 ** 100
        BigInteger gap = encodePrecision.multiply(encodePrecision);

        for (int i = 0; i < keyRound; i++) {
            //BigInteger ni = randBit(keySize, minimum);
            BigInteger ni = GeneralUtil.randBit(bits.get(i), minimum);
            minimum = minimum.multiply(gap);
            while (true) {
                double aRatio = Math.random();
                int aSize = Math.max(1, (int) (keySize * aRatio));
                BigInteger ai = GeneralUtil.randBit(aSize);
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
        index.sort(Comparator.comparing(finalNs::get));
        ns = IntStream.range(0, keyRound).mapToObj(i -> finalNs.get(index.get(i))).collect(Collectors.toList());
        a = IntStream.range(0, keyRound).mapToObj(i -> finalA.get(index.get(i))).collect(Collectors.toList());

        BigInteger g = GeneralUtil.randBit((int) (keySize / 10));
        BigInteger x = GeneralUtil.randBit(160);

        IterativeAffineKeyNew key = new IterativeAffineKeyNew(ns, a, g, x, encodePrecision);
        // check if this key works
        double test = Math.random();
        double test1 = decrypt(encrypt(test, key), key);
        if (Math.abs(test - test1) < 1e-8) {
            return key;
        } else {
            // generate another key
            return keyGenerate(keySize, keyRound, encodePrecision);
        }
    }

    public IterativeAffineCiphertextNew encrypt(double plaintext, PublicKey publicKey) {
        IterativeAffineKeyNew affineKey = (IterativeAffineKeyNew) publicKey;
        if (plaintext < 0) {
            throw new IllegalArgumentException("IterativeAffine encryption scheme only support positive number!");
        }
        BigDecimal plaintext1 = new BigDecimal(plaintext);
        plaintext1 = plaintext1.multiply(affineKey.getEncodedPrecisionDecimal());
        return rawEncrypt(plaintext1.toBigInteger(), affineKey);
    }

    private IterativeAffineCiphertextNew rawEncrypt(BigInteger plaintext, IterativeAffineKeyNew affineKey) {
        List<BigInteger> a = affineKey.getA();
        List<BigInteger> n = affineKey.getN();
        BigInteger g = affineKey.getG();
        BigInteger h = affineKey.getH();

        // random encode function
        BigInteger y = GeneralUtil.randBit(160);

        BigInteger cipher1 = y.multiply(g).mod(n.get(0));
        BigInteger cipher2 = plaintext.add(y.multiply(h)).mod(n.get(0));

        // raw_encrypt_round
        for (int i = 0; i <affineKey.getKey_round(); i++) {
            cipher2 = cipher2.multiply( a.get(i)).mod(n.get(i));
        }

        return new IterativeAffineCiphertextNew(cipher1, cipher2, n.get(n.size() - 1));
    }


    public double decrypt(String strCiphertext, PrivateKey privateKey) {
        IterativeAffineKeyNew affineKeyNew = (IterativeAffineKeyNew) privateKey;
        Ciphertext ciphertextNew = restoreCiphertext(strCiphertext);
        return decrypt(ciphertextNew, affineKeyNew);
    }


    public double decrypt(Ciphertext ciphertext, PrivateKey privateKey) {
        IterativeAffineKeyNew affineKeyNew = (IterativeAffineKeyNew) privateKey;
        IterativeAffineCiphertextNew ciphertextNew = (IterativeAffineCiphertextNew) ciphertext;
        return decrypt(ciphertextNew, affineKeyNew);
    }

    public double decrypt(IterativeAffineCiphertextNew ciphertext, IterativeAffineKeyNew affineKeyNew) {
        BigDecimal encodedPrecisionDecimal = affineKeyNew.getEncodedPrecisionDecimal();
        return rawDecrypt(ciphertext, affineKeyNew).divide(encodedPrecisionDecimal, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    private BigDecimal rawDecrypt(IterativeAffineCiphertextNew ciphertext, IterativeAffineKeyNew affineKey ) {
        int key_round = affineKey.getKey_round();
        List<BigInteger> a_inv = affineKey.getA_inv();
        List<BigInteger> n = affineKey.getN();
        BigInteger x = affineKey.getX();

        BigInteger cipher1 = ciphertext.getCipher1();
        BigInteger cipher2 = ciphertext.getCipher2();

        // raw_decrypt_round
        for (int i = key_round - 1; i > -1; i--) {
            cipher1 = checkAndSubtract(cipher1, n.get(i));
            cipher2 = checkAndSubtract(cipher2, n.get(i));

            cipher1 = cipher1.mod(n.get(i));
            cipher2 = a_inv.get(i).multiply(cipher2.mod(n.get(i))).mod(n.get(i));
        }
        IterativeAffineCiphertextNew res = new IterativeAffineCiphertextNew(
                cipher1,
                cipher2,
                ciphertext.getN_final(),
                ciphertext.getLongMultiple(),
                ciphertext.getMult_times());
        return decode(res, x, n);
    }

    private BigDecimal decode(IterativeAffineCiphertextNew ciphertext, BigInteger x, List<BigInteger> n) {
        BigInteger tmp = ciphertext.getCipher2().subtract(x.multiply(ciphertext.getCipher1())).mod(n.get(0));
        BigDecimal numerator = new BigDecimal(tmp);
        BigDecimal denominator = new BigDecimal(ciphertext.getMultiple().pow(ciphertext.getMult_times()));
        return numerator.divide(denominator, 100, RoundingMode.HALF_UP);
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
    public Ciphertext add(Ciphertext this1, Ciphertext encryptedNumber, PublicKey publicKey) {
        IterativeAffineCiphertextNew ciphertextNew = (IterativeAffineCiphertextNew)this1;
        IterativeAffineCiphertextNew ciphertextNew2 = (IterativeAffineCiphertextNew)encryptedNumber;
        return add(ciphertextNew, ciphertextNew2);
    }

    private IterativeAffineCiphertextNew add(IterativeAffineCiphertextNew this1, IterativeAffineCiphertextNew encryptedNumber) {
        // check multiple and n_final
//        BigInteger tmp = encryptedNumber.getN_final();
        boolean flag = (this1.n_final.compareTo(encryptedNumber.getN_final()) == 0)
                && (this1.multiple.compareTo(encryptedNumber.getMultiple()) == 0);
        if (!flag) {
            throw new IllegalArgumentException("Two addends must have equal multiples and n_finals");
        }
        int mult_times_diff = this1.mult_times - encryptedNumber.getMult_times();

        BigInteger n_final = this1.n_final;
        BigInteger multiple = this1.multiple;
        int mult_times = this1.mult_times;
        BigInteger cipher1, cipher2;

        if (mult_times_diff > 0) {
            cipher1 = this1.cipher1.add(encryptedNumber.getCipher1().multiply(encryptedNumber.getMultiple())
                    .multiply(BigInteger.valueOf(mult_times_diff))).mod(n_final);
            cipher2 = this1.cipher2.add(encryptedNumber.getCipher2().multiply(encryptedNumber.getMultiple())
                    .multiply(BigInteger.valueOf(mult_times_diff))).mod(n_final);
        } else if (mult_times_diff < 0) {
            cipher1 = this1.cipher1.multiply(multiple).multiply(BigInteger.valueOf(-mult_times_diff))
                    .add(encryptedNumber.getCipher1()).mod(n_final);
            cipher2 = this1.cipher2.multiply(multiple).multiply(BigInteger.valueOf(-mult_times_diff))
                    .add(encryptedNumber.getCipher2()).mod(n_final);
            mult_times = encryptedNumber.getMult_times();
        } else {

            cipher1 = this1.cipher1.add(encryptedNumber.getCipher1()).mod(n_final);
            cipher2 = this1.cipher2.add(encryptedNumber.getCipher2()).mod(n_final);
        }
        return new IterativeAffineCiphertextNew(cipher1,cipher2, n_final, multiple, mult_times);
    }


    // TODO: multiple in multiply function does not work yet!
    public Ciphertext multiply(Ciphertext ciphertext, double val, PublicKey publicKey) {
        IterativeAffineCiphertextNew this1 = (IterativeAffineCiphertextNew)ciphertext;
        BigDecimal tmp = new BigDecimal(val);
        BigInteger val1 = tmp.multiply(this1.decimalMultiple).toBigInteger();
        BigInteger cipher1 = this1.cipher1.multiply(val1).mod(this1.n_final);
        BigInteger cipher2 = this1.cipher2.multiply(val1).mod(this1.n_final);
        int mult_times = this1.mult_times + 1;
        return new IterativeAffineCiphertextNew(cipher1, cipher2, this1.n_final, this1.multiple, mult_times);
    }

    public Ciphertext multiply(Ciphertext ciphertext, int val, PublicKey publicKey) {
        IterativeAffineCiphertextNew this1 = (IterativeAffineCiphertextNew)ciphertext;
        BigInteger tmp = BigInteger.valueOf(val);
        BigInteger val1 = tmp.multiply(this1.multiple);
        BigInteger cipher1 = this1.cipher1.multiply(val1).mod(this1.n_final);
        BigInteger cipher2 = this1.cipher2.multiply(val1).mod(this1.n_final);
        int mult_times = this1.mult_times + 1;
        return new IterativeAffineCiphertextNew(cipher1, cipher2, this1.n_final, this1.multiple, mult_times);
    }

    @Override
    public PrivateKey restorePrivateKey(String strKey) {
        Map<String, String> map = DataUtils.stringToMap(strKey, true, true);
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
        return new IterativeAffineKeyNew(tmp_ns, tmp_as, tmp_g, tmp_x, tmp_encodedPrecision);
    }

    @Override
    public PublicKey restorePublicKey(String strKey) {
        return restorePrivateKey(strKey).generatePublicKey();
    }

    @Override
    public Ciphertext restoreCiphertext(String cipherString) {
        String[] splits = cipherString.split(":");
        if (splits.length != 7) {
            throw new NotMatchException("the number of element not match");
        }
        BigInteger cipher1 = new BigInteger(splits[0]) ;
        BigInteger cipher2  = new BigInteger(splits[1]) ;
        BigInteger      n_final= new BigInteger(splits[2]) ;
        BigInteger multiple= new BigInteger(splits[3]) ;
//        BigDecimal decimalMultiple = new BigDecimal(splits[4]) ;
        int mult_times = Integer.parseInt(splits[5]);
//        long longMultiple = Long.parseLong(splits[6]);

        return new IterativeAffineCiphertextNew(cipher1, cipher2, n_final, multiple, mult_times);
    }
}
