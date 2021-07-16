package com.jdt.fedlearn.core.encryption;

import com.jdt.fedlearn.core.encryption.common.Ciphertext;
import com.jdt.fedlearn.core.encryption.common.EncryptionTool;
import com.jdt.fedlearn.core.encryption.common.PrivateKey;
import com.jdt.fedlearn.core.encryption.common.PublicKey;
import com.jdt.fedlearn.core.encryption.fake.FakeTool;
import org.testng.Assert;
import org.testng.annotations.Test;


public class TestFake {
    EncryptionTool encryptionTool = new FakeTool();
    PrivateKey priKey = encryptionTool.keyGenerate(1024, 64);
    PublicKey pubKey = priKey.generatePublicKey();

    @Test
    public void encryptAndDecrypt() {
        double num = 3.1415;
        Ciphertext ciphertext = encryptionTool.encrypt(num, pubKey);
        double deNum = encryptionTool.decrypt(ciphertext, priKey);
        Assert.assertEquals(num, deNum);
    }

    @Test
    public void add() {
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
    public void multiply() {
        double num1 = 3.1415;
        Ciphertext ciphertext1 = encryptionTool.encrypt(num1, pubKey);
        double num2 = 2.568;
        Ciphertext ciphertext = encryptionTool.multiply(ciphertext1, num2, pubKey);
        String cipherTextStr = ciphertext.serialize();
        Ciphertext ciphertextRes = encryptionTool.restoreCiphertext(cipherTextStr);
        double deNum = encryptionTool.decrypt(ciphertextRes, priKey);
        System.out.println("deNum is: " + deNum);
        Assert.assertEquals(num1 * num2, deNum);
    }

    @Test
    public void multiplyInteger() {
        double num1 = 3.1415;
        Ciphertext ciphertext1 = encryptionTool.encrypt(num1, pubKey);
        int num2 = 2;
        Ciphertext ciphertext = encryptionTool.multiply(ciphertext1, num2, pubKey);
        double deNum = encryptionTool.decrypt(ciphertext, priKey);
        Assert.assertEquals(num1 * num2, deNum);
    }

    @Test
    public void multiplyAndAdd() {
        String pubKeyStr = pubKey.serialize();
        PublicKey publicKey = encryptionTool.restorePublicKey(pubKeyStr);
        double num1 = 3.1415;
        Ciphertext ciphertext1 = encryptionTool.encrypt(num1, publicKey);
        double num2 = 2.568;
        Ciphertext ciphertext = encryptionTool.multiply(ciphertext1, num2, publicKey);
        double num3 = 1.107;
        Ciphertext ciphertext3 = encryptionTool.encrypt(num3, publicKey);
        Ciphertext mutiAndAdd = encryptionTool.add(ciphertext, ciphertext3, publicKey);
        String cipherTextStr = mutiAndAdd.serialize();
        Ciphertext ciphertextRes = encryptionTool.restoreCiphertext(cipherTextStr);
        double deNum = encryptionTool.decrypt(ciphertextRes, priKey);
        Assert.assertEquals(num1 * num2 + num3, deNum);
    }

    @Test
    public void addAndMultiply() {
        String pubKeyStr = pubKey.serialize();
        PublicKey publicKey = encryptionTool.restorePublicKey(pubKeyStr);
        double num1 = 3.1415;
        Ciphertext ciphertext1 = encryptionTool.encrypt(num1, publicKey);
        double num2 = 2.568;
        Ciphertext ciphertext2 = encryptionTool.encrypt(num2, publicKey);
        Ciphertext addRes = encryptionTool.add(ciphertext1, ciphertext2, publicKey);
        double num3 = 1.107;
        Ciphertext addAndMulti = encryptionTool.multiply(addRes, num3, publicKey);
        String cipherTextStr = addAndMulti.serialize();
        Ciphertext ciphertextRes = encryptionTool.restoreCiphertext(cipherTextStr);
        double deNum = encryptionTool.decrypt(ciphertextRes, priKey);
        Assert.assertEquals((num1 + num2) * num3, deNum);
    }

    @Test
    public void keySerialize() {
        double num1 = 3.1415;
        String pubKeyStr = pubKey.serialize();
        PublicKey restorePublicKey = encryptionTool.restorePublicKey(pubKeyStr);

        Ciphertext ciphertext1 = encryptionTool.encrypt(num1, restorePublicKey);

        String priKeyStr = priKey.serialize();
        PrivateKey restorePrivateKey = encryptionTool.restorePrivateKey(priKeyStr);
        double deNum = encryptionTool.decrypt(ciphertext1, restorePrivateKey);
        Assert.assertEquals(num1, deNum);
    }

    @Test
    public void ciphertextSerialize() {
        double num1 = 3.1415;
        Ciphertext ciphertext1 = encryptionTool.encrypt(num1, pubKey);
        String strCiphertext = ciphertext1.serialize();
//        Ciphertext restoreCiphertext = encryptionTool.restoreCiphertext(strCiphertext);
        double deNum = encryptionTool.decrypt(strCiphertext, priKey);
        Assert.assertEquals(num1, deNum);
    }
}