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

import com.jdt.fedlearn.core.encryption.common.Ciphertext;
import com.jdt.fedlearn.core.encryption.common.EncryptionTool;
import com.jdt.fedlearn.core.encryption.common.PrivateKey;
import com.jdt.fedlearn.core.encryption.common.PublicKey;
import com.jdt.fedlearn.core.exception.NotMatchException;
import com.jdt.fedlearn.core.exception.UnsupportedAlgorithmException;
import com.jdt.fedlearn.core.type.data.Tuple2;
import com.jdt.fedlearn.core.type.data.Tuple3;

import java.math.BigInteger;

public class AffineTool implements EncryptionTool {
    private static final int defaultScale = 6;

    public AffineKey keyGenerate(int keySize, int placeholder) {
        Tuple3<BigInteger,BigInteger,BigInteger> rawKey = AffineBase.keyGenerate(keySize);
        return new AffineKey(rawKey._1().get(), rawKey._2().get(), rawKey._3().get());
    }

    //support double type, use simple scale, will use big integer to express double to
    //implement an accuracy lossless version later
    public Ciphertext encrypt(double plaintext, PublicKey publicKey) {
        AffineKey affineKey = (AffineKey) publicKey;
        int scale = defaultScale;
        BigInteger plaintext1 = BigInteger.valueOf((long) (plaintext * (int) Math.pow(10, scale)));
        return encrypt(plaintext1, affineKey, scale);
    }

    //to support negative number, split 0-n to 0-n/3,n/3-2n/3,2n/3-n
    private AffineCiphertext encrypt(BigInteger number, AffineKey affineKey, int scale) {
        BigInteger a = affineKey.getA();
        BigInteger b = affineKey.getB();
        BigInteger n = affineKey.getN();

        if (number.compareTo(n.divide(new BigInteger("3"))) >= 0) {
            throw new UnsupportedAlgorithmException("not permit larger than max_value");
        }
        if (number.compareTo(BigInteger.ZERO) < 0) {
            number = n.add(number);
        }

        Tuple2<BigInteger, Integer> encrypt = AffineBase.encrypt(number, n, a, b);
        return new AffineCiphertext(encrypt._1(), encrypt._2(), scale);
    }


    public double decrypt(String ciphertext, PrivateKey privateKey) {
        AffineCiphertext affineCiphertext = (AffineCiphertext) restoreCiphertext(ciphertext);
        AffineKey affineKey = (AffineKey) privateKey;

        BigInteger res = decrypt(affineCiphertext, affineKey);
        double d = res.doubleValue() / (long) Math.pow(10, affineCiphertext.getScale());
        return d;
    }

    public double decrypt(Ciphertext ciphertext, PrivateKey privateKey) {
        AffineCiphertext affineCiphertext = (AffineCiphertext) ciphertext;
        AffineKey affineKey = (AffineKey) privateKey;

        BigInteger res = decrypt(affineCiphertext, affineKey);
        double d = res.doubleValue() / (long) Math.pow(10, affineCiphertext.getScale());
        return d;
    }

    //
    private BigInteger decrypt(AffineCiphertext ciphertext, AffineKey affineKey) {
        BigInteger n = affineKey.getN();
        BigInteger res = AffineBase.decrypt(new Tuple2<>(ciphertext.times, ciphertext.bias), n, affineKey.getInverseA(), affineKey.getB());

        BigInteger two = new BigInteger("2");
        BigInteger three = new BigInteger("3");
        BigInteger threshold = two.multiply(n).divide(three);
        if (res.compareTo(threshold) >= 0) {
            res = res.subtract(n);
        }
        return res;
    }

    //publicKey 实际无用，只是为了保持调用一致性
    public AffineCiphertext add(Ciphertext ciphertext1, Ciphertext ciphertext2, PublicKey publicKey) {
        AffineCiphertext c1 = (AffineCiphertext) ciphertext1;
        AffineCiphertext c2 = (AffineCiphertext) ciphertext2;

        if (c1.getScale() < c2.getScale()) {
            c1 = synScale(c1, c2.getScale());
        }

        if (c2.getScale() < c1.getScale()) {
            c2 = synScale(c2, c1.getScale());
        }

        Tuple2<BigInteger,BigInteger> rawSum = AffineBase.add(c1.times, c1.bias, c2.times, c2.bias);
        return new AffineCiphertext(rawSum._1(), rawSum._2(), c1.getScale());
    }

//    public AffineCiphertext minus(AffineCiphertext ciphertextA, AffineCiphertext ciphertextB, PublicKey publicKey) {
//        AffineCiphertext encryptedNumber1 = multiply(ciphertextB, -1, publicKey);
//        return add(ciphertextA, encryptedNumber1, publicKey);
//    }

    public AffineCiphertext multiply(Ciphertext ciphertext, double val, PublicKey publicKey) {
        AffineCiphertext c1 = (AffineCiphertext) ciphertext;
        int scale = defaultScale;
        BigInteger val1 = BigInteger.valueOf((long) (val * (int) Math.pow(10, scale)));

        Tuple2<BigInteger, BigInteger> rawProduct = AffineBase.multiply(c1.getTimes(), c1.getBias(), val1);
        return new AffineCiphertext(rawProduct._1(), rawProduct._2(), c1.getScale() + scale);
    }


    public AffineCiphertext multiply(Ciphertext ciphertext, int val, PublicKey publicKey) {
        AffineCiphertext c1 = (AffineCiphertext) ciphertext;
        BigInteger val1 = BigInteger.valueOf(val);

        Tuple2<BigInteger, BigInteger> rawProduct = AffineBase.multiply(c1.getTimes(), c1.getBias(), val1);
        return new AffineCiphertext(rawProduct._1(), rawProduct._2(), c1.getScale());
    }

//    public AffineCiphertext divide(AffineCiphertext encryptedNumber, double val, PublicKey publicKey) {
//        return multiply(encryptedNumber, 1 / val, publicKey);
//    }

    private AffineCiphertext synScale(AffineCiphertext pd1, int maxScale) {
        StringBuilder s = new StringBuilder("1");
        for (int i = 0; i < maxScale - pd1.getScale(); i++) {
            s.append("0");
        }
        BigInteger m = new BigInteger(s.toString());

        BigInteger newTimes = pd1.getTimes().multiply(m);
        BigInteger newBias = pd1.getBias().multiply(m);
        return new AffineCiphertext(newTimes, newBias, maxScale);
    }


    public PrivateKey restorePrivateKey(String strKey) {
        String[] splits = strKey.split(":");
        if (splits.length != 4) {
            throw new NotMatchException("the number of element not match");
        }
        BigInteger n = new BigInteger(splits[0]);
        BigInteger a = new BigInteger(splits[1]);
        BigInteger b = new BigInteger(splits[2]);
        BigInteger aInverse = new BigInteger(splits[3]);

        return new AffineKey(n, a, b, aInverse);
    }

    @Override
    public PublicKey restorePublicKey(String strKey) {
        return (AffineKey) restorePrivateKey(strKey);
    }

    public Ciphertext restoreCiphertext(String cipherString) {
        String[] splits = cipherString.split(":");
        if (splits.length != 3) {
            throw new NotMatchException("the number of element not match");
        }
        BigInteger times = new BigInteger(splits[0]);
        BigInteger bias = new BigInteger(splits[1]);
        int scale = Integer.parseInt(splits[2]);
        return new AffineCiphertext(times, bias, scale);
    }
}
