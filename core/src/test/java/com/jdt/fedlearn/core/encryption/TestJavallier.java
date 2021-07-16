package com.jdt.fedlearn.core.encryption;

import com.jdt.fedlearn.core.encryption.common.Ciphertext;
import com.jdt.fedlearn.core.encryption.common.EncryptionTool;
import com.jdt.fedlearn.core.encryption.common.PrivateKey;
import com.jdt.fedlearn.core.encryption.common.PublicKey;
import com.jdt.fedlearn.core.encryption.javallier.JavallierTool;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestJavallier {
    EncryptionTool encryptionTool = new JavallierTool();
    PrivateKey priKey = encryptionTool.keyGenerate(1024, 64);
    PublicKey pubKey = priKey.generatePublicKey();

    @Test
    public void privateKeySerialize(){
        EncryptionTool encryptionTool = new JavallierTool();
        PrivateKey paillierPriKey = encryptionTool.keyGenerate(1024, 64);

        String strKey = paillierPriKey.serialize();
        PrivateKey restoreKey = encryptionTool.restorePrivateKey(strKey);

        PublicKey paillierPubKey = restoreKey.generatePublicKey();

        double m1 = 20.11121;
        Ciphertext em1 = encryptionTool.encrypt(m1, paillierPubKey);
        System.out.println("ciphertext size:" + em1.serialize().length());
        double dm1 = encryptionTool.decrypt(em1, paillierPriKey);
        System.out.println("dm1:" + dm1);
        Assert.assertEquals(m1, dm1);
    }

    @Test
    public void publicKeySerialize(){
        EncryptionTool encryptionTool = new JavallierTool();
        PrivateKey privateKey = encryptionTool.keyGenerate(1024, 64);
        PublicKey publicKey = privateKey.generatePublicKey();

        double m1 = 60.5121;
        String strPublicKey = publicKey.serialize();
        PublicKey restorePubicKey = encryptionTool.restorePublicKey(strPublicKey);
        Ciphertext em1 = encryptionTool.encrypt(m1, restorePubicKey);

        double dm1 = encryptionTool.decrypt(em1, privateKey);
        System.out.println("dm1:" + dm1);
        Assert.assertEquals(m1, dm1);
    }

    @Test
    public void ciphertextSerialize(){
        EncryptionTool encryptionTool = new JavallierTool();
        PrivateKey privateKey = encryptionTool.keyGenerate(1024, 64);
        PublicKey publicKey = privateKey.generatePublicKey();

        double m1 = 60.5121;
        Ciphertext em1 = encryptionTool.encrypt(m1, publicKey);
        String strEm1 = em1.serialize();

        double dm1 = encryptionTool.decrypt(strEm1, privateKey);
        System.out.println("dm1:" + dm1);
        Assert.assertEquals(m1, dm1);
    }

    @Test
    public void add(){
        double num1 = 3.1415;
        Ciphertext ciphertext1 = encryptionTool.encrypt(num1, pubKey);
        double num2 = 2.568;
        Ciphertext ciphertext2 = encryptionTool.encrypt(num2, pubKey);
        Ciphertext ciphertext = encryptionTool.add(ciphertext1, ciphertext2, pubKey);
        String cipherTextStr = ciphertext.serialize();
        Ciphertext ciphertextRes = encryptionTool.restoreCiphertext(cipherTextStr);
        double deNum = encryptionTool.decrypt(ciphertextRes, priKey);
        Assert.assertEquals(deNum, num1 + num2);
    }

    @Test
    public void multiply(){
        double num1 = 3.1415;
        Ciphertext ciphertext1 = encryptionTool.encrypt(num1, pubKey);
        double num2 = 2.568;
        Ciphertext ciphertext = encryptionTool.multiply(ciphertext1, num2, pubKey);
        String cipherTextStr = ciphertext.serialize();
        Ciphertext ciphertextRes = encryptionTool.restoreCiphertext(cipherTextStr);
        double deNum = encryptionTool.decrypt(ciphertextRes, priKey);
        Assert.assertEquals(num1 * num2, deNum);
    }

    @Test
    public void multiplyInteger(){
        double num1 = 3.1415;
        Ciphertext ciphertext1 = encryptionTool.encrypt(num1, pubKey);
        int num2 = 2;
        Ciphertext ciphertext = encryptionTool.multiply(ciphertext1, num2, pubKey);
        String cipherTextStr = ciphertext.serialize();
        Ciphertext ciphertextRes = encryptionTool.restoreCiphertext(cipherTextStr);
        double deNum = encryptionTool.decrypt(ciphertextRes, priKey);
        Assert.assertEquals(num1 * num2, deNum);
    }

    @Test
    public void addThenMultiply() {
        double d1 = 0.5;
        double d2 = 1.7;
        double d3 = 5.5;

        Ciphertext ed1 = encryptionTool.encrypt(d1, pubKey);
        Ciphertext ed2 = encryptionTool.encrypt(d2, pubKey);

        Ciphertext sum = encryptionTool.add(ed1, ed2, pubKey);
        Ciphertext re1 = encryptionTool.multiply(sum, d3, pubKey);

        double res = encryptionTool.decrypt(re1, priKey);

        double realRes = ((d1 + d2) * d3);
        Assert.assertEquals(res, realRes, 1e-3);
    }

    @Test
    public void multiplyThenAdd() {
        double a = 1.5;
        double b = 5;
        double c = -2.21;

        Ciphertext enA = encryptionTool.encrypt(a, pubKey);
        Ciphertext enC = encryptionTool.encrypt(c, pubKey);

        Ciphertext multiply = encryptionTool.multiply(enA, b, pubKey);
        Ciphertext enRes = encryptionTool.add(multiply, enC, pubKey);

        double deRes = encryptionTool.decrypt(enRes, priKey);
        double res = (a * b) + c;
        Assert.assertEquals(res, deRes, 1e-3);
    }
}