package com.jdt.fedlearn.core.encryption;

import com.jdt.fedlearn.core.encryption.IterativeAffineNew.IterativeAffineToolNew;
import com.jdt.fedlearn.core.encryption.common.Ciphertext;
import com.jdt.fedlearn.core.encryption.common.PrivateKey;
import com.jdt.fedlearn.core.encryption.common.PublicKey;
import org.testng.Assert;
import org.testng.annotations.Test;



public class TestIterativeAffineNew {
    IterativeAffineToolNew affineToolNew = new IterativeAffineToolNew();
    //TODO 此处公私钥实际一样，主要为方便传输以及系统API统一
    PrivateKey key = affineToolNew.keyGenerate(1024, 2);
    PublicKey pubKey = key.generatePublicKey();

    @Test
    public void add() {
        double a = 12.35;
        double b = 33.1;
        Ciphertext ciphertextA = affineToolNew.encrypt(a, pubKey);
        Ciphertext ciphertextB = affineToolNew.encrypt(b, pubKey);
        double decryptedA = affineToolNew.decrypt(ciphertextA, key);
        Assert.assertEquals(a, decryptedA);
        Ciphertext sum = affineToolNew.add(ciphertextA, ciphertextB, pubKey);
        double decryptedSum = affineToolNew.decrypt(sum, key);
        Assert.assertEquals(a+b, decryptedSum);
    }

    @Test
    public void multiply() {
        double a = 12.35;
        double b = 2.2;

        Ciphertext ciphertextA = affineToolNew.encrypt(a, pubKey);
        Ciphertext product =  affineToolNew.multiply(ciphertextA, b, pubKey);

        double decryptedProduct = affineToolNew.decrypt(product, key);
        Assert.assertEquals(a*b, decryptedProduct);
    }

    @Test
    public void multiplyThenAdd() {
        double x = 12.7;
        double y = 65.1;
        double z = 4.9;
        Ciphertext cx = affineToolNew.encrypt(x, pubKey);
        Ciphertext cy = affineToolNew.encrypt(y, pubKey);
        Ciphertext product = affineToolNew.multiply(cx, z, pubKey);
        Ciphertext sum =  affineToolNew.add(product, cy, pubKey);
        double res = affineToolNew.decrypt(sum, key);
        Assert.assertEquals(x*z+y, res);
    }

    @Test
    public void AddThenMultiply() {
        double x = 12.7;
        double y = 65.1;
        double z = 4.9;
        Ciphertext cx = affineToolNew.encrypt(x, pubKey);
        Ciphertext cy = affineToolNew.encrypt(y, pubKey);
        Ciphertext sum = affineToolNew.add(cx, cy, pubKey);
        Ciphertext product =  affineToolNew.multiply(sum,z, pubKey);
        double res = affineToolNew.decrypt(product, key);
        Assert.assertEquals((x+y)*z, res, 1e-8);
    }

    @Test
    public void chainAdd() {
        double x = 12.7;
        double y = 65.1;
        double z = 4.9;
        Ciphertext cx = affineToolNew.encrypt(x, pubKey);
        Ciphertext cy = affineToolNew.encrypt(y, pubKey);
        Ciphertext cz = affineToolNew.encrypt(z, pubKey);
        Ciphertext sumXY = affineToolNew.add(cx, cy, pubKey);
        Ciphertext sumXYZ = affineToolNew.add(sumXY, cz, pubKey);
        double res = affineToolNew.decrypt(sumXYZ, key);
        Assert.assertEquals(x+y+z, res, 1e-8);
    }

    @Test
    public void chainMultiply() {
        double x = 12.7;
        double y = 65.1;
        double z = 4.9;
        Ciphertext cx = affineToolNew.encrypt(x, pubKey);
        Ciphertext productXY = affineToolNew.multiply(cx, y, pubKey);
        Ciphertext productXYZ = affineToolNew.multiply(productXY, z, pubKey);
        double res = affineToolNew.decrypt(productXYZ, key);
        Assert.assertEquals(x*y*z, res, 1e-8);
    }

    @Test
    public void keySerialize() {
        double num1 = 3.1415;
        String pubKeyStr = pubKey.serialize();
        PublicKey restorePublicKey = affineToolNew.restorePublicKey(pubKeyStr);

        Ciphertext ciphertext1 = affineToolNew.encrypt(num1, restorePublicKey);

        String priKeyStr = key.serialize();
        PrivateKey restorePrivateKey = affineToolNew.restorePrivateKey(priKeyStr);
        double deNum = affineToolNew.decrypt(ciphertext1, restorePrivateKey);
        Assert.assertEquals(num1, deNum);
    }

    @Test
    public void ciphertextSerialize() {
        double num1 = 3.1415;
        Ciphertext ciphertext1 = affineToolNew.encrypt(num1, pubKey);
        String strCiphertext = ciphertext1.serialize();
//        Ciphertext restoreCiphertext = encryptionTool.restoreCiphertext(strCiphertext);
        double deNum = affineToolNew.decrypt(strCiphertext, key);
        Assert.assertEquals(num1, deNum);
    }


}
