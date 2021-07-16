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

package com.jdt.fedlearn.core.encryption.javallier;

import com.n1analytics.paillier.PaillierPrivateKey;
import com.n1analytics.paillier.PaillierPublicKey;
import com.n1analytics.paillier.util.BigIntegerUtil;

import java.math.BigInteger;

public class JSerializer implements PaillierPrivateKey.Serializer {
    private String serialized;
    private final boolean isSafe;

    public JSerializer(boolean isSafe) {
        this.isSafe = isSafe;
    }

    public String getSerialized() {
        return serialized;
    }

    public boolean isSafe() {
        return isSafe;
    }

    @Override
    public void serialize(PaillierPublicKey publicKey, BigInteger p, BigInteger q) {
        BigInteger modulus = publicKey.getModulus();
        BigInteger lambda = (p.subtract(BigInteger.ONE)).multiply(q.subtract(BigInteger.ONE));
        BigInteger mu = BigIntegerUtil.modInverse(lambda, modulus);

        serialized = modulus.toString() + ":" + lambda.toString() + ":" + mu.toString() + ":" + isSafe;
    }
}
