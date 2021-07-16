package com.jdt.fedlearn.core.encryption;

import com.jdt.fedlearn.core.encryption.affine.AffineTool;
import com.jdt.fedlearn.core.encryption.common.Ciphertext;
import com.jdt.fedlearn.core.encryption.common.EncryptionTool;
import com.jdt.fedlearn.core.encryption.common.PrivateKey;
import com.jdt.fedlearn.core.encryption.common.PublicKey;
import org.testng.Assert;
import org.testng.annotations.Test;


public class TestAffine {
    @Test
    public void add(){
        EncryptionTool encryptionTool = new AffineTool();

        PrivateKey affineKey = encryptionTool.keyGenerate(1024, 2);
        PublicKey publicKey = affineKey.generatePublicKey();

        Ciphertext ciphertextA = encryptionTool.encrypt(-3.3, publicKey);
        double restoreA = encryptionTool.decrypt(ciphertextA, affineKey);
        Assert.assertEquals(restoreA, -3.3);

        Ciphertext ciphertextB = encryptionTool.encrypt(6.6, publicKey);
        Ciphertext sum = encryptionTool.add(ciphertextA, ciphertextB, null);

        double restoreSum = encryptionTool.decrypt(sum, affineKey);
        Assert.assertEquals(restoreSum, 3.3, 1e-8);
    }

    @Test
    public void multiply(){
        EncryptionTool encryptionTool = new AffineTool();

        PrivateKey affineKey = encryptionTool.keyGenerate(1024, 2);
        PublicKey publicKey = affineKey.generatePublicKey();

        Ciphertext ciphertextA = encryptionTool.encrypt(-2.0, publicKey);

        Ciphertext product = encryptionTool.multiply(ciphertextA, 3.3, null);

        double restoreSum = encryptionTool.decrypt(product, affineKey);
        Assert.assertEquals(restoreSum, -6.6, 1e-8);
    }

    @Test
    public void multiplyInteger(){
        EncryptionTool encryptionTool = new AffineTool();

        PrivateKey affineKey = encryptionTool.keyGenerate(1024, 2);
        PublicKey publicKey = affineKey.generatePublicKey();

        Ciphertext ciphertextA = encryptionTool.encrypt(-2.0, publicKey);

        Ciphertext product = encryptionTool.multiply(ciphertextA, 2, publicKey);

        double restoreSum = encryptionTool.decrypt(product, affineKey);
        Assert.assertEquals(restoreSum, -4.0 ,1e-8);
    }

    @Test //(a+b)*c
    public void addThenMultiply(){
        EncryptionTool encryptionTool = new AffineTool();

        PrivateKey affineKey = encryptionTool.keyGenerate(1024, 2);
        PublicKey publicKey = affineKey.generatePublicKey();

        Ciphertext ciphertextA = encryptionTool.encrypt(-3.3, publicKey);
        Ciphertext ciphertextB = encryptionTool.encrypt(7.6, publicKey);
        Ciphertext sum = encryptionTool.add(ciphertextA, ciphertextB, null);
        double restoreSum = encryptionTool.decrypt(sum, affineKey);
        Assert.assertEquals(restoreSum, 4.3, 1e-8);

        Ciphertext product = encryptionTool.multiply(sum, 2.0, null);

        double restoreProduct = encryptionTool.decrypt(product, affineKey);
        Assert.assertEquals(restoreProduct, 8.6, 1e-8);
    }
//
//    //TODO
    @Test //(a*b)+c
    public void multiplyThenAdd(){
        EncryptionTool encryptionTool = new AffineTool();

        PrivateKey affineKey = encryptionTool.keyGenerate(1024, 2);
        PublicKey publicKey = affineKey.generatePublicKey();

        Ciphertext ciphertextA = encryptionTool.encrypt(2.0, publicKey);
        Ciphertext product = encryptionTool.multiply(ciphertextA, 3.33333, null);
        double restoreProduct = encryptionTool.decrypt(product, affineKey);
        Assert.assertEquals(restoreProduct, 6.66666, 1e-8);

        Ciphertext ciphertextB = encryptionTool.encrypt(4.0, publicKey);
        Ciphertext sum = encryptionTool.add(product, ciphertextB, null);

        double restoreSum = encryptionTool.decrypt(sum, affineKey);
        Assert.assertEquals(restoreSum, 10.66666, 1e-8);
    }
//
//    //TODO
    @Test
    public void chainAdd(){
        EncryptionTool encryptionTool = new AffineTool();

        PrivateKey affineKey = encryptionTool.keyGenerate(1024, 2);
        PublicKey publicKey = affineKey.generatePublicKey();

        Ciphertext ciphertextA = encryptionTool.encrypt(3.3, publicKey);
        Ciphertext ciphertextB = encryptionTool.encrypt(4.2, publicKey);

        Ciphertext sumA = encryptionTool.add(ciphertextA, ciphertextB, null);
        double restoreSumA = encryptionTool.decrypt(sumA, affineKey);
        Assert.assertEquals(restoreSumA, 7.5);

        Ciphertext ciphertextC = encryptionTool.encrypt(6.2, publicKey);
        Ciphertext sumB = encryptionTool.add(sumA, ciphertextC, null);

        double restoreSumB = encryptionTool.decrypt(sumB, affineKey);
        Assert.assertEquals(restoreSumB, 13.7, 1e-8);
    }

    @Test
    public void chainMultiply(){
        EncryptionTool encryptionTool = new AffineTool();

        PrivateKey affineKey = encryptionTool.keyGenerate(1024, 2);
        PublicKey publicKey = affineKey.generatePublicKey();

        Ciphertext ciphertextA = encryptionTool.encrypt(3.0, publicKey);
        Ciphertext productA = encryptionTool.multiply(ciphertextA, 2.1, null);
        double restoreProductA = encryptionTool.decrypt(productA, affineKey);
        Assert.assertEquals(restoreProductA, 6.3);

        Ciphertext productB = encryptionTool.multiply(productA, 1.5, null);
        double restoreSum = encryptionTool.decrypt(productB, affineKey);
        Assert.assertEquals(restoreSum, 9.45, 1e-8);
    }

    @Test
    public void keySerialize(){
        EncryptionTool encryptionTool = new AffineTool();

        PrivateKey affineKey = encryptionTool.keyGenerate(1024, 2);
        PublicKey publicKey = affineKey.generatePublicKey();

        String strPublicKey = publicKey.serialize();
        PublicKey restorePublicKey = encryptionTool.restorePublicKey(strPublicKey);
        Ciphertext ciphertextA = encryptionTool.encrypt(3.0, restorePublicKey);

        String strAffineKey = affineKey.serialize();
        PrivateKey restoreAffineKey = encryptionTool.restorePrivateKey(strAffineKey);
        double restoreProductA = encryptionTool.decrypt(ciphertextA, restoreAffineKey);
        Assert.assertEquals(restoreProductA, 3.0);
    }

    @Test
    public void ciphertextSerialize(){
        EncryptionTool encryptionTool = new AffineTool();

        PrivateKey affineKey = encryptionTool.keyGenerate(1024, 2);
        PublicKey publicKey = affineKey.generatePublicKey();

        Ciphertext ciphertextA = encryptionTool.encrypt(-3.3, publicKey);
        String strCiphertextA = ciphertextA.serialize();
//        Ciphertext restoreCiphertextA = encryptionTool.restoreCiphertext(strCiphertextA);
        double restoreA = encryptionTool.decrypt(strCiphertextA, affineKey);
        Assert.assertEquals(restoreA, -3.3);
    }

}
