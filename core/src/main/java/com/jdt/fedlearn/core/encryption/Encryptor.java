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

package com.jdt.fedlearn.core.encryption;

import com.n1analytics.paillier.EncodedNumber;
import com.n1analytics.paillier.EncryptedNumber;
import com.n1analytics.paillier.PaillierContext;
import com.n1analytics.paillier.PaillierPublicKey;

import java.math.BigInteger;

public class Encryptor {

    private final PaillierPublicKey paillierPublicKey;
    private final PaillierContext paillierContext;

    public Encryptor(PaillierPublicKey publicKey) {
        this.paillierPublicKey = publicKey;
        paillierContext = paillierPublicKey.createSignedContext();
    }

    public EncryptedNumber encrypt(double d) {
        EncodedNumber encoded = paillierContext.encode(d);
        BigInteger value = encoded.getValue();
        BigInteger ciphertext = paillierPublicKey.raw_encrypt_without_obfuscation(value);
        return new EncryptedNumber(paillierContext, ciphertext, encoded.getExponent(), true);
    }

    public EncryptedNumber encrypt(double d, boolean issafe) {
        if (issafe) {
            return encrypt(d);
        } else {
            return paillierContext.encrypt(d).getSafeEncryptedNumber();
        }
    }

    public PaillierPublicKey getPaillierPublicKey() {
        return paillierPublicKey;
    }

}
