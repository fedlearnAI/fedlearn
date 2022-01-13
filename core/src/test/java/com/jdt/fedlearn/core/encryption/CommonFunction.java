package com.jdt.fedlearn.core.encryption;

import com.jdt.fedlearn.core.encryption.common.Ciphertext;
import com.jdt.fedlearn.core.encryption.common.EncryptionTool;
import com.jdt.fedlearn.core.encryption.common.PrivateKey;
import com.jdt.fedlearn.core.encryption.common.PublicKey;
import com.jdt.fedlearn.core.encryption.javallier.JavallierTool;
import org.testng.Assert;
import org.testng.annotations.Test;

public class CommonFunction {

    public void testAddDouble(EncryptionTool encryptionTool ) {
        PrivateKey paillierPriKey = encryptionTool.keyGenerate(1024, 64);
        PublicKey paillierPubKey = paillierPriKey.generatePublicKey();

        double m1 = 20.1112;
        double m2 = 60.5121;
        Ciphertext em1 = encryptionTool.encrypt(m1, paillierPubKey);
        Ciphertext em2 = encryptionTool.encrypt(m2, paillierPubKey);

        double dm1 = encryptionTool.decrypt(em1, paillierPriKey);
        double dm2 = encryptionTool.decrypt(em2, paillierPriKey);
        System.out.println("dm1" + dm1);
        Assert.assertEquals(m1, dm1);
        Assert.assertEquals(m2, dm2);

        Ciphertext sunEm1Em2 = encryptionTool.add(em1, em2, paillierPubKey);
        double decryptedSum = encryptionTool.decrypt(sunEm1Em2, paillierPriKey);
        System.out.println("decrypted sum: " + decryptedSum);
        Assert.assertEquals(m1 + m2, decryptedSum);
    }

    @Test
    public void testMultiplyDouble() {
        EncryptionTool encryptionTool = new JavallierTool();
        PrivateKey paillierPriKey = encryptionTool.keyGenerate(1024, 64);
        PublicKey paillierPubKey = paillierPriKey.generatePublicKey();
        double m1 = -100.7777;
        double m2 = 60.1111;

        Ciphertext em1 = encryptionTool.encrypt(m1, paillierPubKey);
        double dm1 = encryptionTool.decrypt(em1, paillierPriKey);
        Assert.assertEquals(dm1, m1);

        Ciphertext productEm1Em2 = encryptionTool.multiply(em1, m2, paillierPubKey);
        double decrypted = encryptionTool.decrypt(productEm1Em2, paillierPriKey);
        System.out.println("decrypted sum: " + decrypted);
        Assert.assertEquals(decrypted, m1 * m2, 1e-3);
    }


    @Test
    public static void addThenMultiply() {
        EncryptionTool encryptionTool = new JavallierTool();
        PrivateKey paillierPriKey = encryptionTool.keyGenerate(1024, 64);
        PublicKey paillierPubKey = paillierPriKey.generatePublicKey();

        double d1 = 0.5;
        double d2 = 1.7;
        double d3 = 5.5;


        Ciphertext ed1 = encryptionTool.encrypt(d1, paillierPubKey);
        Ciphertext ed2 = encryptionTool.encrypt(d2, paillierPubKey);

        Ciphertext sum = encryptionTool.add(ed1, ed2, paillierPubKey);
        Ciphertext re1 = encryptionTool.multiply(sum, d3, paillierPubKey);

        double res = encryptionTool.decrypt(re1, paillierPriKey);

        double realRes = ((d1 + d2) * d3);
        System.out.println(res);
        Assert.assertEquals(res, realRes, 1e-3);
    }

    @Test
    public void multiplyThenAdd() {
        EncryptionTool encryptionTool = new JavallierTool();
        PrivateKey paillierPriKey = encryptionTool.keyGenerate(1024, 64);
        PublicKey paillierPubKey = paillierPriKey.generatePublicKey();

        double a = 1.5;
        double b = 5;
        double c = -2.21;

        Ciphertext enA = encryptionTool.encrypt(a, paillierPubKey);
        Ciphertext enC = encryptionTool.encrypt(c, paillierPubKey);

        Ciphertext multiply = encryptionTool.multiply(enA, b, paillierPubKey);
        Ciphertext enRes = encryptionTool.add(multiply, enC, paillierPubKey);

        double deRes = encryptionTool.decrypt(enRes, paillierPriKey);
        System.out.println("deRes: " + deRes);
        double res = (a * b) + c;
        System.out.println("res: " + res);
        Assert.assertEquals(res, deRes, 1e-3);
    }
}
