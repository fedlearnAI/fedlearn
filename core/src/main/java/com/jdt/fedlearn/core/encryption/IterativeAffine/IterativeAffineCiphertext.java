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
import com.jdt.fedlearn.core.encryption.common.Ciphertext;

import java.math.BigDecimal;
import java.math.BigInteger;

//import static com.jdt.fedlearn.core.encryption.IterativeAffineUtil.multModGMP;

/**
 * RandomizedIterativeAffineCiphertext
 */
public class IterativeAffineCiphertext implements Ciphertext {
    private BigInteger cipher1, cipher2, n_final, multiple;
//    private BigInteger zero = BigInteger.valueOf(0);
    private BigDecimal decimalMultiple;
    private int mult_times;
    private long longMultiple;

    // 构造函数
    public IterativeAffineCiphertext(BigInteger cipher1, BigInteger cipher2, BigInteger n_final) {
        this.cipher1 = cipher1;
        this.cipher2 = cipher2;
        this.n_final = n_final;
        this.longMultiple = (long) Math.pow(2, 50);
        this.multiple = BigInteger.valueOf(longMultiple);
        this.decimalMultiple = new BigDecimal(this.multiple);
        this.mult_times = 0;
    }

    public IterativeAffineCiphertext(BigInteger cipher1, BigInteger cipher2, BigInteger n_final, int mult_times) {
        this.cipher1 = cipher1;
        this.cipher2 = cipher2;
        this.n_final = n_final;
        this.longMultiple = (long) Math.pow(2, 50);
        this.multiple = BigInteger.valueOf(longMultiple);
        this.decimalMultiple = new BigDecimal(this.multiple);
        this.mult_times = mult_times;
    }

    public IterativeAffineCiphertext(BigInteger cipher1, BigInteger cipher2, BigInteger n_final, long longMultiple, int mult_times) {
        this.cipher1 = cipher1;
        this.cipher2 = cipher2;
        this.n_final = n_final;
        this.longMultiple = longMultiple;
        this.multiple = BigInteger.valueOf(longMultiple);
        this.decimalMultiple = new BigDecimal(this.multiple);
        this.mult_times = mult_times;
    }

    public void add(IterativeAffineCiphertext encryptedNumber) {
        // check multiple and n_final
//        BigInteger tmp = encryptedNumber.getN_final();
        boolean flag = (this.n_final.compareTo(encryptedNumber.getN_final()) == 0)
                && (this.multiple.compareTo(encryptedNumber.getMultiple()) == 0);
        if (!flag) {
            throw new IllegalArgumentException("Two addends must have equal multiples and n_finals");
        }
        int mult_times_diff = this.mult_times - encryptedNumber.getMult_times();
        if (mult_times_diff > 0) {
            cipher1 = cipher1.add(encryptedNumber.getCipher1().multiply(encryptedNumber.getMultiple())
                    .multiply(BigInteger.valueOf(mult_times_diff))).mod(n_final);
            cipher2 = cipher2.add(encryptedNumber.getCipher2().multiply(encryptedNumber.getMultiple())
                    .multiply(BigInteger.valueOf(mult_times_diff))).mod(n_final);
            // GMP version
        } else if (mult_times_diff < 0) {
            cipher1 = cipher1.multiply(multiple).multiply(BigInteger.valueOf(-mult_times_diff))
                    .add(encryptedNumber.getCipher1()).mod(n_final);
            cipher2 = cipher2.multiply(multiple).multiply(BigInteger.valueOf(-mult_times_diff))
                    .add(encryptedNumber.getCipher2()).mod(n_final);
            // GMP version
//            cipher1 = GMP.addMod(GMP.multiply(GMP.multiply(cipher1, multiple),
//                    BigInteger.valueOf(-mult_times_diff)), encryptedNumber.getCipher1(),n_final);
//            cipher2 = GMP.addMod(GMP.multiply(GMP.multiply(cipher2, multiple),
//                    BigInteger.valueOf(-mult_times_diff)), encryptedNumber.getCipher2(),n_final);
            mult_times = encryptedNumber.getMult_times();
        } else {

            cipher1 = cipher1.add(encryptedNumber.getCipher1()).mod(n_final);
            cipher2 = cipher2.add(encryptedNumber.getCipher2()).mod(n_final);
            // GMP version
//            cipher1 = GMP.remainder(GMP.add(cipher1, encryptedNumber.getCipher1()), n_final);
//            cipher2 = GMP.remainder(GMP.add(cipher2, encryptedNumber.getCipher2()), n_final);
        }
    }

    public void minus(IterativeAffineCiphertext encryptedNumber) {
        IterativeAffineCiphertext encryptedNumber1 = encryptedNumber.copy();
        encryptedNumber1.multiply(-1);
        add(encryptedNumber1);
    }

    // TODO: multiple in multiply function does not work yet!
    public void multiply(double val) {
//        throw new NotImplementedException("Not implemented yet!");
        BigDecimal tmp = new BigDecimal(val);
        BigInteger val1 = tmp.multiply(decimalMultiple).toBigInteger();
        cipher1 = cipher1.multiply(val1).mod(n_final);
        cipher2 = cipher2.multiply(val1).mod(n_final);
        mult_times = mult_times + 1;
    }

    public void multiply(int val) {
//        throw new NotImplementedException("Not implemented yet!");
        BigInteger val1 = BigInteger.valueOf(val);
        cipher1 = cipher1.multiply(val1).mod(n_final);
        cipher2 = cipher2.multiply(val1).mod(n_final);
    }

    public void divide(double val) {
        multiply(1 / val);
    }

    public IterativeAffineCiphertext copy() {
        return new IterativeAffineCiphertext(cipher1, cipher2, n_final, longMultiple, mult_times);
    }

    // getter and setter

    public int getMult_times() {
        return mult_times;
    }

    public long getLongMultiple() {
        return longMultiple;
    }

    public BigInteger getMultiple() {
        return multiple;
    }

    public BigInteger getCipher1() {
        return cipher1;
    }

    public BigInteger getCipher2() {
        return cipher2;
    }

    public BigInteger getN_final() {
        return n_final;
    }

    @Override
    public String serialize() {
        return null;
    }
}
