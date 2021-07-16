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

package com.jdt.fedlearn.core.encryption.paillier;

import com.jdt.fedlearn.core.encryption.common.PrivateKey;
import com.jdt.fedlearn.core.encryption.common.PublicKey;

import java.math.BigInteger;

public class PaillierPriKey implements PrivateKey {
    private final BigInteger lambda;
    private final BigInteger n;

    public PaillierPriKey(BigInteger lambda, BigInteger n) {
        this.lambda = lambda;
        this.n = n;
    }

    public BigInteger getLambda() {
        return lambda;
    }

    public BigInteger getN() {
        return n;
    }

    public String serialize() {
        return n.toString() + ":" + lambda.toString();
    }

    @Override
    public PublicKey generatePublicKey() {
        return new PaillierPubKey(n);
    }


}

