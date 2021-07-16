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

import com.jdt.fedlearn.core.encryption.common.PrivateKey;
import com.jdt.fedlearn.core.encryption.common.PublicKey;

import java.math.BigInteger;

/**
 * Iterative affine encryption scheme
 */
public final class AffineKey implements PublicKey, PrivateKey {
    private final BigInteger n;
    private final BigInteger a;
    private final BigInteger b;
    private final BigInteger inverseA;


    public AffineKey(BigInteger n, BigInteger a, BigInteger b) {
        this.n = n;
        this.a = a;
        this.b = b;
        this.inverseA = a.modInverse(n);
    }

    public AffineKey(BigInteger n, BigInteger a, BigInteger b, BigInteger inverseA) {
        this.n = n;
        this.a = a;
        this.b = b;
        this.inverseA = inverseA;
    }

    public BigInteger getA() {
        return a;
    }

    public BigInteger getB() {
        return b;
    }

    public BigInteger getInverseA() {
        return inverseA;
    }

    public BigInteger getN() {
        return n;
    }

    // serialization and deserialization
    public String serialize() {
        return n.toString() + ":" + a.toString() + ":" + b.toString() + ":" + inverseA.toString();
    }

    public AffineKey generatePublicKey() {
        return new AffineKey(this.n, this.a, this.b, this.inverseA);
    }
}
