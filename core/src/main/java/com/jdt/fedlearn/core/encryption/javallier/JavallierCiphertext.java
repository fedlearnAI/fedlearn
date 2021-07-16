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

import com.jdt.fedlearn.core.encryption.common.Ciphertext;
import com.n1analytics.paillier.EncryptedNumber;
import com.n1analytics.paillier.PaillierContext;

import java.math.BigInteger;

public final class JavallierCiphertext implements Ciphertext {
    private EncryptedNumber encryptedNumber;
//    private boolean isSafe = false;
    private BigInteger v;
    private int e;
    private boolean isSafe;


    public JavallierCiphertext( EncryptedNumber encryptedNumber) {
        this.encryptedNumber = encryptedNumber;
//        this.v = encryptedNumber.calculateCiphertext();
//        this.e = encryptedNumber.getExponent();
    }

    public JavallierCiphertext(BigInteger v, int e, boolean isSafe){
        this.v = v;
        this.e = e;
        this.isSafe = isSafe;
    }

    public EncryptedNumber getEncryptedNumber(PaillierContext context) {
        if (encryptedNumber != null){
            return encryptedNumber;
        }
        return new EncryptedNumber(context, v, e);
    }

    public boolean isSafe() {
        return isSafe;
    }

    @Override
    public String serialize() {
        if (encryptedNumber != null){
                    this.v = encryptedNumber.calculateCiphertext();
        this.e = encryptedNumber.getExponent();
        }
        return this.v + ":" + this.e;
    }
}
