package com.jdt.fedlearn.core.encryption;

import com.jdt.fedlearn.core.encryption.paillier.PaillierCore;
import com.jdt.fedlearn.core.type.data.KeyPair;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.math.BigInteger;

public class TestPaillierCore {

    @Test
    public void addInteger() {
        KeyPair keyPair = PaillierCore.keyGenerate(1024, 64);
        BigInteger m1 = new BigInteger("20");
        BigInteger m2 = new BigInteger("60");

        BigInteger em1 = PaillierCore.encrypt(m1, keyPair.getPublicKey());
        BigInteger em2 = PaillierCore.encrypt(m2, keyPair.getPublicKey());

        BigInteger dm1 = PaillierCore.decrypt(em1, keyPair.getSecretKey(), keyPair.getPublicKey());
        BigInteger dm2 = PaillierCore.decrypt(em2, keyPair.getSecretKey(), keyPair.getPublicKey());
        Assert.assertEquals(m1, dm1);
        Assert.assertEquals(m2, dm2);

        BigInteger sunEm1Em2 = PaillierCore.add(em1, em2, keyPair.getPublicKey());
        BigInteger decryptedSum = PaillierCore.decrypt(sunEm1Em2, keyPair.getSecretKey(), keyPair.getPublicKey());
        System.out.println("decrypted sum: " + decryptedSum.toString());
        Assert.assertEquals(m1.add(m2), decryptedSum);
    }

    @Test
    public void multiplyNumber() {
        //整数乘以明文数字
        KeyPair keyPair = PaillierCore.keyGenerate(1024, 64);
        BigInteger m1 = new BigInteger("20");
        BigInteger m2 = new BigInteger("60");

        BigInteger em1 = PaillierCore.encrypt(m1, keyPair.getPublicKey());

        BigInteger dm1 = PaillierCore.decrypt(em1, keyPair.getSecretKey(), keyPair.getPublicKey());
        Assert.assertEquals(m1, dm1);

        BigInteger multiplyEm1M2 = PaillierCore.multiply(em1, m2, keyPair.getPublicKey());
        BigInteger decryptedSum = PaillierCore.decrypt(multiplyEm1M2, keyPair.getSecretKey(), keyPair.getPublicKey());
        System.out.println("decrypted sum: " + decryptedSum.toString());
        Assert.assertEquals(m1.multiply(m2), decryptedSum);
    }

//    @Test
//    public void testZero() {
//        KeyPair keyPair = PaillierCore.keyGeneration(1024, 64);
//        double m1 = 0;
//        double m2 = 0;
//
//        PaillierCiphertext em1 = PaillierCore.encryption(m1, keyPair.getPublicKey());
//        PaillierCiphertext em2 = PaillierCore.encryption(m2, keyPair.getPublicKey());
//
//        double decrypted1 = PaillierCore.decryption(em1, keyPair);
//        double decrypted2 = PaillierCore.decryption(em2, keyPair);
//
//        Assert.assertEquals(0.0, decrypted1);
//        Assert.assertEquals(0.0, decrypted2);
//    }


//    @Test
//    public static void addAndMultiply() {
//        KeyPair keyPair = PaillierCore.keyGeneration(256,64);
//
//        double d1 = 0.5;
//        double d2 = 1.7;
//        double d3 = 5.5;
//
//
//        PaillierCiphertext ed1 = PaillierCore.encryption(d1, keyPair.getPublicKey());
//        PaillierCiphertext ed2 = PaillierCore.encryption(d2, keyPair.getPublicKey());
//
//        PaillierCiphertext sum = PaillierCore.add(ed1, ed2, keyPair.getPublicKey().toString());
//        PaillierCiphertext re1 = PaillierCore.multiply(sum, d3, keyPair.getPublicKey().toString());
//
//        double res = PaillierCore.decryption(re1, keyPair);
//
//
//        double realRes = ((d1 + d2) * d3);
//        System.out.println(res);
//        Assert.assertEquals(res, realRes, 1e-3);
//    }

//    @Test
//    public void multiplyAndAdd() {
//        KeyPair keyPair = PaillierCore.keyGeneration(256, 64);
//        BigInteger pubKey = keyPair.getPublicKey();
//        double a = 1.5;
//        double b = 5;
//        double c = -2.21;
//
//        PaillierCiphertext enA = PaillierCore.encryption(a, pubKey);
//        PaillierCiphertext enC = PaillierCore.encryption(c, pubKey);
//
//        PaillierCiphertext multiply = PaillierCore.multiply(enA, b, pubKey);
//        PaillierCiphertext enRes = PaillierCore.add(multiply, enC, pubKey);
//
//        double deRes = PaillierCore.decryption(enRes, keyPair);
//        System.out.println("deRes: " + deRes);
//        double res = (a*b)+c;
//        System.out.println("res: " + res);
//        Assert.assertEquals(res,deRes,1e-3);
//    }
}
