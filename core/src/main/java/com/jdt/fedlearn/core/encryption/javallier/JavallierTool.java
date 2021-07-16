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
import com.jdt.fedlearn.core.encryption.common.EncryptionTool;
import com.jdt.fedlearn.core.encryption.common.PrivateKey;
import com.jdt.fedlearn.core.encryption.common.PublicKey;
import com.jdt.fedlearn.core.exception.NotMatchException;
import com.n1analytics.paillier.*;

import java.math.BigInteger;

public class JavallierTool implements EncryptionTool {
    private boolean isSafe = true;

    //TODO 新增isSafe参数
    public JavallierPriKey keyGenerate(int bitLengthVal, int a) {
        this.isSafe = true;
        PaillierPrivateKey paillierPrivateKey = PaillierPrivateKey.create(bitLengthVal);
        return new JavallierPriKey(paillierPrivateKey, this.isSafe);
    }

    public Ciphertext encrypt(double d, PublicKey pubKey) {
        JavallierPubKey publicKey = (JavallierPubKey) pubKey;
        if (this.isSafe) {
            PaillierContext paillierContext = publicKey.getPaillierPublicKey().createSignedContext();
            EncodedNumber encoded = paillierContext.encode(d);
            BigInteger value = encoded.getValue();
            BigInteger ciphertext = publicKey.getPaillierPublicKey().raw_encrypt_without_obfuscation(value);
            return new JavallierCiphertext(new EncryptedNumber(paillierContext, ciphertext, encoded.getExponent(), isSafe));
        } else {
            PaillierContext paillierContext = publicKey.getPaillierPublicKey().createSignedContext();
            return new JavallierCiphertext(paillierContext.encrypt(d).getSafeEncryptedNumber());
        }
    }

    public Ciphertext add(Ciphertext ciphertext1, Ciphertext ciphertext2, PublicKey publicKey) {
        //TODO  assert c1's PaillierContext equal to c2's
        JavallierCiphertext d = (JavallierCiphertext) ciphertext1;
        JavallierCiphertext e = (JavallierCiphertext) ciphertext2;
        PaillierPublicKey paillierPublicKey = ((JavallierPubKey)publicKey).getPaillierPublicKey();
        PaillierContext paillierContext = paillierPublicKey.createSignedContext();
        EncryptedNumber dm = d.getEncryptedNumber(paillierContext);
        EncryptedNumber em = e.getEncryptedNumber(paillierContext);
        return new JavallierCiphertext(dm.add(em));
    }

    public Ciphertext multiply(Ciphertext ciphertext1, double number, PublicKey publicKey) {
        JavallierCiphertext d = (JavallierCiphertext) ciphertext1;
        PaillierPublicKey paillierPublicKey = ((JavallierPubKey)publicKey).getPaillierPublicKey();
        PaillierContext paillierContext = paillierPublicKey.createSignedContext();
        EncryptedNumber dm = d.getEncryptedNumber(paillierContext);
        return new JavallierCiphertext(dm.multiply(number));
    }

    public Ciphertext multiply(Ciphertext ciphertext1, int number, PublicKey publicKey) {
        JavallierCiphertext d = (JavallierCiphertext) ciphertext1;
        PaillierPublicKey paillierPublicKey = ((JavallierPubKey)publicKey).getPaillierPublicKey();
        PaillierContext paillierContext = paillierPublicKey.createSignedContext();
        EncryptedNumber dm = d.getEncryptedNumber(paillierContext);
//        d.getPaillierContext();
        return new JavallierCiphertext(dm.multiply(number));
    }

    public double decrypt(Ciphertext cipherText, PrivateKey privateKey) {
        JavallierCiphertext jCiphertext = (JavallierCiphertext) cipherText;
        JavallierPriKey javallierPriKey = (JavallierPriKey) privateKey;

        PaillierPublicKey paillierPublicKey = ((JavallierPubKey)privateKey.generatePublicKey()).getPaillierPublicKey();
        PaillierContext paillierContext = paillierPublicKey.createSignedContext();

        return javallierPriKey.getPrivateKey().decrypt(jCiphertext.getEncryptedNumber(paillierContext)).decodeDouble();
    }

    public double decrypt(String strCipherText, PrivateKey privateKey) {
        JavallierCiphertext jCiphertext = (JavallierCiphertext) restoreCiphertext(strCipherText);
        return decrypt(jCiphertext, privateKey);
    }


    public JavallierPriKey restorePrivateKey(String strKey) {
        String[] splits = strKey.split(":");
        if (splits.length != 4) {
            throw new NotMatchException("the number of element not match");
        }
        BigInteger n = new BigInteger(splits[0]);
        BigInteger lambda = new BigInteger(splits[1]);
//        BigInteger mu = new BigInteger(splits[2]);
        boolean isSafe = Boolean.parseBoolean(splits[3]);

        return new JavallierPriKey(n, lambda, isSafe);
    }

    public JavallierPubKey restorePublicKey(String strKey) {
        String[] splits = strKey.split(":");
        if (splits.length != 2) {
            throw new NotMatchException("the number of element not match");
        }
        BigInteger modulus = new BigInteger(splits[0]);
        boolean isSafe = Boolean.parseBoolean(splits[1]);

        return new JavallierPubKey(modulus, isSafe);
    }

    public Ciphertext restoreCiphertext(String cipherString) {
        String[] splitStr = cipherString.split(":");
        if (splitStr.length != 2) {
            throw new NotMatchException("the number of element not match");
        }
        BigInteger v = new BigInteger(splitStr[0]);
        int e = Integer.parseInt(splitStr[1]);
//        BigInteger modulus = new BigInteger(splitStr[2]);
//        PaillierPublicKey pub = new PaillierPublicKey(modulus);
//        PaillierContext context = pub.createSignedContext();
        return new JavallierCiphertext(v, e, isSafe);
    }

    //TODO 以下两个函数主要是历史遗留，部分算法仍在用，后续会优化
    @Deprecated
    public static EncryptedNumber encryptionInner(double d, PaillierPublicKey publicKey, boolean isSafe) {
        PaillierContext paillierContext = publicKey.createSignedContext();
        if (isSafe) {
            EncodedNumber encoded = paillierContext.encode(d);
            BigInteger value = encoded.getValue();
            BigInteger ciphertext = publicKey.raw_encrypt_without_obfuscation(value);
            return new EncryptedNumber(paillierContext, ciphertext, encoded.getExponent(), true);
        } else {
            return paillierContext.encrypt(d).getSafeEncryptedNumber();
        }
    }

    @Deprecated
    public static double decryptionInner(EncryptedNumber number, PaillierPrivateKey paillierPrivateKey) {
        return paillierPrivateKey.decrypt(number).decodeDouble();
    }
}
