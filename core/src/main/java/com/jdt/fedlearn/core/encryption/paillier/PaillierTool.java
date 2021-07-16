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

import com.jdt.fedlearn.core.encryption.common.Ciphertext;
import com.jdt.fedlearn.core.encryption.common.EncryptionTool;
import com.jdt.fedlearn.core.encryption.common.PrivateKey;
import com.jdt.fedlearn.core.encryption.common.PublicKey;
import com.jdt.fedlearn.core.type.data.KeyPair;
import com.jdt.fedlearn.core.exception.UnsupportedAlgorithmException;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;

/**
 * pure java  paillier encryption tool
 */
public class PaillierTool implements EncryptionTool {
    private static final int defaultBitLength = 1024;
//    private static final int defaultCertainty = 64;
    private static final int defaultScale = 6;

//    private int bitLength;

    public PaillierPriKey keyGenerate(int bitLengthVal, int certainty) {
//        this.bitLength = bitLengthVal;
        KeyPair keyPair = PaillierCore.keyGenerate(bitLengthVal, certainty);
        return new PaillierPriKey(keyPair.getSecretKey(),keyPair.getPublicKey());
    }

    public PaillierCiphertext encrypt(double number, PublicKey publicKey) {
        PaillierPubKey pubKey = (PaillierPubKey)publicKey;
        return encryption(number, pubKey.getN());
    }

    //below is extension of base paillier to support double number
    //use method to support negative number
    public PaillierCiphertext encryption(double dm, BigInteger pubKey) {
        int scale = defaultScale;
        BigInteger m = BigInteger.valueOf((long) (dm * (int) Math.pow(10, scale)));
        BigInteger r = new BigInteger(defaultBitLength, new Random());

        BigInteger n = pubKey;
        //m < n/3
        if (m.compareTo(n.divide(new BigInteger("3"))) >= 0) {
            throw new UnsupportedAlgorithmException("not permit larger than max_value");
        }
        if (m.compareTo(BigInteger.ZERO) < 0) {
            m = n.add(m);
        }
        BigInteger res = PaillierCore.encrypt(m, pubKey, r);
        return new PaillierCiphertext(res.toString(), scale);
    }

    private void synScale(PaillierCiphertext pd1, PaillierCiphertext pd2, int maxScope, String pubKey) {
        if (maxScope > pd1.getScale()) {
            StringBuilder s = new StringBuilder("1");
            for (int i = 0; i < maxScope - pd1.getScale(); i++) {
                s.append("0");
            }
            BigInteger m = new BigInteger(s.toString());
            BigInteger em1 = new BigInteger(pd1.getSecData());
            pd1.setSecData(PaillierCore.multiply(em1, m, new BigInteger(pubKey)).toString());
            pd1.setScale(maxScope);
        }
        if (maxScope > pd2.getScale()) {
            StringBuilder s = new StringBuilder("1");
            for (int i = 0; i < maxScope - pd2.getScale(); i++) {
                s.append("0");
            }
            BigInteger m = new BigInteger(s.toString());
            BigInteger em2 = new BigInteger(pd2.getSecData());
            pd2.setSecData(PaillierCore.multiply(em2, m, new BigInteger(pubKey)).toString());
            pd2.setScale(maxScope);
        }
    }

    public PaillierCiphertext add(Ciphertext ciphertext1, Ciphertext ciphertext2, PublicKey pubKey) {
        //TODO check scale is equal
        PaillierCiphertext ems1 = (PaillierCiphertext) ciphertext1;
        PaillierCiphertext ems2 = (PaillierCiphertext) ciphertext2;
        if (ems1.getScale() != ems2.getScale()) {
            int maxScale = Math.max(ems1.getScale(), ems2.getScale());
            synScale(ems1, ems2, maxScale, pubKey.serialize());
        }
        BigInteger em1 = new BigInteger(ems1.getSecData());
        BigInteger em2 = new BigInteger(ems2.getSecData());
        BigInteger n = new BigInteger(pubKey.serialize());
        BigInteger nSquare = n.multiply(n);
        BigInteger product_em1em2 = em1.multiply(em2).mod(nSquare);
        return new PaillierCiphertext(product_em1em2.toString(), ems1.getScale());
    }

    public PaillierCiphertext multiply(Ciphertext cipherText, double ms2, PublicKey pubKey) {
        PaillierCiphertext ems1 = (PaillierCiphertext) cipherText;
        int scale = 6;
        BigInteger m = BigInteger.valueOf((long) (ms2 * (int) Math.pow(10, scale)));

        BigInteger n = new BigInteger(pubKey.serialize());
        BigInteger em1 = new BigInteger(ems1.getSecData());

        BigInteger numRes = em1.modPow(m, n.multiply(n));
        return new PaillierCiphertext(numRes.toString(), ems1.getScale() + scale);
    }


    public PaillierCiphertext multiply(Ciphertext cipherText, int ms2, PublicKey pubKey) {
        PaillierCiphertext ems1 = (PaillierCiphertext) cipherText;
        BigInteger m = BigInteger.valueOf(ms2);
        BigInteger n = new BigInteger(pubKey.serialize());
        BigInteger em1 = new BigInteger(ems1.getSecData());

        BigInteger numRes = em1.modPow(m, n.multiply(n));
        return new PaillierCiphertext(numRes.toString(), ems1.getScale());
    }

    public PaillierCiphertext sum(PaillierCiphertext[] ems, PublicKey pubKey) {
        PaillierCiphertext gSum = encrypt(0, pubKey);
        return Arrays.stream(ems).parallel().reduce((a, b) -> add(a, b, pubKey)).orElse(gSum);
    }


    public double decrypt(String cStr, PrivateKey priKey) {
        Ciphertext cipherText = restoreCiphertext(cStr);
        return decrypt(cipherText, priKey);
    }

    public double decrypt(Ciphertext cStr, PrivateKey priKey) {
        PaillierCiphertext ciphertext = (PaillierCiphertext) cStr;
        PaillierPriKey privateKey = (PaillierPriKey) priKey;

        BigInteger c = new BigInteger(ciphertext.getSecData());
        BigInteger res = decryption(c, new KeyPair(privateKey.getN(), privateKey.getLambda()));
        return res.doubleValue() / (long) Math.pow(10, ciphertext.getScale());
    }

    public BigInteger decryption(BigInteger c, KeyPair keyPair) {
        BigInteger n = keyPair.getPublicKey();
        BigInteger res = PaillierCore.decrypt(c, keyPair.getSecretKey(), keyPair.getPublicKey());
        //res > 2n/3
        BigInteger two = new BigInteger("2");
        BigInteger three = new BigInteger("3");
        BigInteger threshold = two.multiply(n).divide(three);
        if (res.compareTo(threshold) >= 0) {
            res = res.subtract(n);
        }
        return res;
    }


    @Override
    public PrivateKey restorePrivateKey(String strKey) {
        String[] splits = strKey.split(":");
        BigInteger lambda = new BigInteger(splits[1]);
        BigInteger n = new BigInteger(splits[0]);
        return new PaillierPriKey(lambda, n);
    }

    @Override
    public PublicKey restorePublicKey(String strKey) {
        BigInteger n = new BigInteger(strKey);
        return new PaillierPubKey(n);
    }

    @Override
    public Ciphertext restoreCiphertext(String strCipher) {
        String[] strings = strCipher.split(":");
        String secData = strings[0];
        int scale = Integer.parseInt(strings[1]);
        return new PaillierCiphertext(secData, scale);
    }
}
