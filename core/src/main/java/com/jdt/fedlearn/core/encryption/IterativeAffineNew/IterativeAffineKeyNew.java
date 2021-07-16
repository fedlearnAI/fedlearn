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

import com.jdt.fedlearn.core.encryption.common.GeneralUtil;
import com.jdt.fedlearn.core.encryption.common.PrivateKey;
import com.jdt.fedlearn.core.encryption.common.PublicKey;
import com.jdt.fedlearn.core.entity.randomForest.DataUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Iterative affine encryption scheme
 */
// TODO: multiple in multiply function does not work yet!
public final class IterativeAffineKeyNew implements PublicKey, PrivateKey {
    private final List<BigInteger> n; //modulus
    private final List<BigInteger> a; // key
    private final List<BigInteger> a_inv; //
    private final BigInteger g;
    private final BigInteger h;
    private final BigInteger x;
    private final BigInteger encodedPrecision;
    private final BigDecimal encodedPrecisionDecimal;
    private final int key_round;

    public IterativeAffineKeyNew(List<BigInteger> n,
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
        this.a_inv = GeneralUtil.modInverse(key_round, n, a);
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


    @Override
    public PublicKey generatePublicKey() {
        return this;
    }

    @Override
    public String serialize() {
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
}
