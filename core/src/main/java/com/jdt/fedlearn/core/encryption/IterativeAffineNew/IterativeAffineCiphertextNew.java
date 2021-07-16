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


import com.jdt.fedlearn.core.encryption.common.Ciphertext;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * RandomizedIterativeAffineCiphertext
 */
public class IterativeAffineCiphertextNew implements Ciphertext {
    public BigInteger cipher1, cipher2, n_final, multiple;
    public BigDecimal decimalMultiple;
    public int mult_times;
    public long longMultiple;

    // 构造函数
    public IterativeAffineCiphertextNew(BigInteger cipher1, BigInteger cipher2, BigInteger n_final) {
        this.cipher1 = cipher1;
        this.cipher2 = cipher2;
        this.n_final = n_final;
        this.longMultiple = (long) Math.pow(2, 50);
        this.multiple = BigInteger.valueOf(longMultiple);
        this.decimalMultiple = new BigDecimal(this.multiple);
        this.mult_times = 0;
    }

    public IterativeAffineCiphertextNew(BigInteger cipher1, BigInteger cipher2, BigInteger n_final, int mult_times) {
        this.cipher1 = cipher1;
        this.cipher2 = cipher2;
        this.n_final = n_final;
        this.longMultiple = (long) Math.pow(2, 50);
        this.multiple = BigInteger.valueOf(longMultiple);
        this.decimalMultiple = new BigDecimal(this.multiple);
        this.mult_times = mult_times;
    }

    public IterativeAffineCiphertextNew(BigInteger cipher1, BigInteger cipher2, BigInteger n_final, long longMultiple, int mult_times) {
        this.cipher1 = cipher1;
        this.cipher2 = cipher2;
        this.n_final = n_final;
        this.longMultiple = longMultiple;
        this.multiple = BigInteger.valueOf(longMultiple);
        this.decimalMultiple = new BigDecimal(this.multiple);
        this.mult_times = mult_times;
    }

    public IterativeAffineCiphertextNew(BigInteger cipher1, BigInteger cipher2, BigInteger n_final, BigInteger multiple, int mult_times) {
        this.cipher1 = cipher1;
        this.cipher2 = cipher2;
        this.n_final = n_final;
        this.multiple = multiple;
        this.longMultiple = multiple.longValue();
        this.decimalMultiple = new BigDecimal(this.multiple);
        this.mult_times = mult_times;
    }

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
        String[] array = new String[]{
                cipher1.toString(),
                cipher2.toString(),
                n_final.toString(),
                multiple.toString(),
                decimalMultiple.toString(),
                mult_times + "",
                longMultiple + ""
        } ;

        return String.join(":", array);
    }
}
