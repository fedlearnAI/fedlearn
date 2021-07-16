package com.jdt.fedlearn.core.encryption;

import com.jdt.fedlearn.core.encryption.common.Ciphertext;
import com.jdt.fedlearn.core.encryption.common.EncryptionTool;
import com.jdt.fedlearn.core.encryption.common.PrivateKey;
import com.jdt.fedlearn.core.encryption.common.PublicKey;
import com.jdt.fedlearn.core.encryption.paillier.PaillierTool;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestPaillier {
    EncryptionTool encryptionTool = new PaillierTool();
    PrivateKey privateKey = encryptionTool.keyGenerate(1024,64);
    PublicKey publicKey = privateKey.generatePublicKey();

    @Test
    public void addInteger() {
        double m1 =20;
        double m2 = 60;
        String strKey = publicKey.serialize();
        Ciphertext em1 = encryptionTool.encrypt(m1,encryptionTool.restorePublicKey(strKey) );
        System.out.println("ciphertext size:" + em1.serialize().length());
        Ciphertext em2 = encryptionTool.encrypt(m2,publicKey );
        String em1Str = em1.serialize();
        double dm1 = encryptionTool.decrypt(encryptionTool.restoreCiphertext(em1Str),privateKey);
        double dm2= encryptionTool.decrypt(em2,privateKey);
        Assert.assertEquals(m1, dm1);
        Assert.assertEquals(m2, dm2);

        Ciphertext sunEm1Em2 = encryptionTool.add(em1, em2, publicKey);
        String sunEm1Em2Str = sunEm1Em2.serialize();
        double decryptedSum = encryptionTool.decrypt(encryptionTool.restoreCiphertext(sunEm1Em2Str), privateKey);
        System.out.println("decrypted sum: " + decryptedSum);
        Assert.assertEquals(m1+m2, decryptedSum);
    }

    @Test
    public void addDouble() {
        double m1 = -100.7777;
        double m2 = 60.6666;
        Ciphertext em1 = encryptionTool.encrypt(m1,publicKey );
        Ciphertext em2 = encryptionTool.encrypt(m2,publicKey );
        double dm1 = encryptionTool.decrypt(em1, privateKey);
        double dm2 = encryptionTool.decrypt(em2, privateKey);
        Assert.assertEquals(m1, dm1);
        Assert.assertEquals(m2, dm2);

        Ciphertext sumEm1Em2 = encryptionTool.add(em1, em2, publicKey);

        double decrypted = encryptionTool.decrypt(encryptionTool.restoreCiphertext(sumEm1Em2.serialize()), privateKey);
        System.out.println("test paillier decrypted sum: " + decrypted);
        Assert.assertEquals(m1 + m2, decrypted, 1e-4);
    }


    @Test
    public void testMultiplyNumber() {
        //整数乘以明文数字
        double m1 =20.252;
        double m2 = 60;
        Ciphertext em1 = encryptionTool.encrypt(m1,publicKey );
        Ciphertext multiplyEm1M2 = encryptionTool.multiply(em1, m2,publicKey);
        double decryptedSum = encryptionTool.decrypt(multiplyEm1M2, privateKey);
        System.out.println("decrypted sum: " + decryptedSum);
        Assert.assertEquals(m1*(m2), decryptedSum);
    }



    @Test
    public void testMultiplyDouble() {
        double m1 = -100.7777;
        double m2 = 60.1111;

        Ciphertext em1 = encryptionTool.encrypt(m1,publicKey );

        Ciphertext productEm1Em2 = encryptionTool.multiply(em1, m2, publicKey);
        double decrypted = encryptionTool.decrypt(encryptionTool.restoreCiphertext(productEm1Em2.serialize()), privateKey);
        System.out.println("decrypted sum: " + decrypted);
        Assert.assertEquals(decrypted, m1 * m2, 1e-3);
    }

    @Test
    public void testZero() {
        double m1 = 0;
        double m2 = 0;

        Ciphertext em1 = encryptionTool.encrypt(m1,publicKey );
        Ciphertext em2 = encryptionTool.encrypt(m2,publicKey );

        double decrypted1 = encryptionTool.decrypt(em1, privateKey);
        double decrypted2 = encryptionTool.decrypt(em2, privateKey);

        Assert.assertEquals(0.0, decrypted1);
        Assert.assertEquals(0.0, decrypted2);
    }


    @Test
    public void testZeroArray() {
        double m1 = 0;
        double m2 = 0;
        Ciphertext[] ems = new Ciphertext[2];
        ems[0] = encryptionTool.encrypt(0,encryptionTool.restorePublicKey(publicKey.serialize()) );
        ems[1] = encryptionTool.encrypt(0,encryptionTool.restorePublicKey(publicKey.serialize()) );

        double decrypted1 = encryptionTool.decrypt(ems[0], privateKey);
        double decrypted2 = encryptionTool.decrypt(ems[1], privateKey);

        Assert.assertEquals(0.0, decrypted1);
        Assert.assertEquals(0.0, decrypted2);
    }



    @Test
    public void addAndMultiply() {

        double m1 = 0.5;
        double m2 = 1.7;
        double m3 = 5.5;


        Ciphertext ed1 = encryptionTool.encrypt(m1,publicKey );
        Ciphertext ed2 = encryptionTool.encrypt(m2,publicKey );

        Ciphertext sum = encryptionTool.add(ed1, ed2, publicKey);
        Ciphertext re1 = encryptionTool.multiply(sum, m3, publicKey);

        double res = encryptionTool.decrypt(encryptionTool.restoreCiphertext(re1.serialize()), encryptionTool.restorePrivateKey(privateKey.serialize()));


        double realRes = ((m1 + m2) * m3);
        System.out.println(res);
        Assert.assertEquals(res, realRes, 1e-3);
    }

    @Test
    public void multiplyThenAdd() {

        double a = 1.5;
        double b = 5;
        double c = -2.21;

        Ciphertext enA = encryptionTool.encrypt(a, publicKey);
        Ciphertext enC = encryptionTool.encrypt(c, publicKey);

        Ciphertext multiply = encryptionTool.multiply(enA, b, publicKey);
        Ciphertext enRes = encryptionTool.add(multiply, enC, publicKey);

        double deRes = encryptionTool.decrypt(encryptionTool.restoreCiphertext(enRes.serialize()), privateKey);
        System.out.println("deRes: " + deRes);
        double res = (a*b)+c;
        System.out.println("res: " + res);
        Assert.assertEquals(res,deRes,1e-3);
    }

}