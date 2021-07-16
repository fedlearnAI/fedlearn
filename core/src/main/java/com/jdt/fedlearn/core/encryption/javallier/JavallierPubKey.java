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

import com.jdt.fedlearn.core.encryption.common.PublicKey;
import com.jdt.fedlearn.core.exception.NotMatchException;
import com.n1analytics.paillier.PaillierPublicKey;

import java.math.BigInteger;

public class JavallierPubKey implements PublicKey {
    private final BigInteger modulus;
    private final boolean isSafe;

    public JavallierPubKey(BigInteger modulus, boolean isSafe) {
        this.modulus = modulus;
        this.isSafe = isSafe;
    }

    public PaillierPublicKey getPaillierPublicKey() {
        return new PaillierPublicKey(modulus);
    }

    public boolean isSafe() {
        return isSafe;
    }

    @Override
    public String serialize() {
        return modulus.toString() + ":" + isSafe;
    }

}
