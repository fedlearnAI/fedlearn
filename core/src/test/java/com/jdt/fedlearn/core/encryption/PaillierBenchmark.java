package com.jdt.fedlearn.core.encryption;

import com.jdt.fedlearn.core.fake.DataGenerate;
import com.jdt.fedlearn.grpc.federatedlearning.Vector;
import com.n1analytics.paillier.EncryptedNumber;
import com.n1analytics.paillier.PaillierContext;
import com.n1analytics.paillier.PaillierPrivateKey;
import com.n1analytics.paillier.PaillierPublicKey;
import org.testng.annotations.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PaillierBenchmark {

//    @Test
//    public void testPaillier() {
//        String pubKey = keyPair.getPublicKey().toString();
//        final PaillierCiphertext zero = PaillierCore.encryption(0.0, keyPair.getPublicKey().toString());
//        final PaillierCiphertext one = PaillierCore.encryption(1.0, keyPair.getPublicKey().toString());
//        PaillierCiphertext res = PaillierCore.add(zero, one, pubKey);
//        System.out.println("zero + one = " + PaillierCore.decryption(res, keyPair));
//        PaillierCiphertext[] nums = new PaillierCiphertext[100];
//        Arrays.fill(nums, one);
//        for (int i = 0; i < nums.length; i++) {
//            nums[i] = PaillierCore.add(nums[i], one, pubKey);
//        }
//        for (int i = 0; i < nums.length; i++) {
//            System.out.println("nums " + i+ " = " + PaillierCore.decryption(nums[i], keyPair));
//        }
//    }
    @Test
    public void testAdd() {
        // create(var0) var0 is bitLength
        PaillierPrivateKey privateKey = PaillierPrivateKey.create(1024);
        PaillierPublicKey publicKey = privateKey.getPublicKey();
        Encryptor encryptor = new Encryptor(publicKey);
        Decryptor decryptor = new Decryptor(privateKey);
        Vector.Builder vectorOrBuilder = Vector.newBuilder();
        double value = 1;
        double value2 = 2;
        vectorOrBuilder.addValues(value);
        vectorOrBuilder.addValues(value2);
        Vector y_vec = vectorOrBuilder.build();
        EncryptedNumber m1 = encryptor.encrypt(y_vec.getValues(0));
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            m1 = encryptor.encrypt(y_vec.getValues(0));
        }
        long end = System.currentTimeMillis();
        System.out.println("1000 en time 1 = " + (end-start));

        start = System.currentTimeMillis();
        EncryptedNumber m2 = encryptor.encrypt(y_vec.getValues(1));
        end = System.currentTimeMillis();
        System.out.println("en time 2 = " + (end-start));

        start = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            EncryptedNumber m1f = encryptor.encrypt(y_vec.getValues(0), false);
        }
        end = System.currentTimeMillis();
        System.out.println("en time 3 = " + (end-start));

        start = System.currentTimeMillis();
        EncryptedNumber m2f = encryptor.encrypt(y_vec.getValues(1), false);
        end = System.currentTimeMillis();
        System.out.println("en time 4 = " + (end-start));

        /* printout encrypted text*/
        System.out.println("em2:" + m2);
        System.out.println("em2 is_not_safe:" + m2f);
        /* printout decrypted text */

//        System.out.println(decryptor.decrypt(m1));
        start = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            double x = decryptor.decrypt(m2);
        }
        end = System.currentTimeMillis();
        System.out.println("1000 de time = " + (end-start));

        start = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            double x = decryptor.decrypt(m2f);
        }
        end = System.currentTimeMillis();
        System.out.println("de time = " + (end-start));

        System.out.println(decryptor.decrypt(m2));
        System.out.println(decryptor.decrypt(m2f));
        EncryptedNumber res = m1.add(m2);
        start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            res = m1.add(m2);
        }
        end = System.nanoTime();
        System.out.println("1000 add time = " + (end-start));
        System.out.println(decryptor.decrypt(res));
        /* test homomorphic properties -> D(E(m1)*E(m2) mod n^2) = (m1 + m2) mod n */
        PaillierContext paillierContext = new PaillierContext(publicKey, true, 1024, 16);

//        System.out.println(product_em1em2);
//        BigInteger decrypted = Paillier.decryption(product_em1em2, keyPair);
//        System.out.println("decrypted sum: " + decrypted.toString());
//        Assert.assertEquals(m1.add(m2), decrypted);
    }

    @Test
    public void testEnc(){
        PaillierPrivateKey privateKey = PaillierPrivateKey.create(1024);
        PaillierPublicKey publicKey = privateKey.getPublicKey();
        Encryptor encryptor = new Encryptor(publicKey);
        Decryptor decryptor = new Decryptor(privateKey);
        Vector.Builder vectorOrBuilder = Vector.newBuilder();
        double value = 1;
        double value2 = 2;
        vectorOrBuilder.addValues(value);
        vectorOrBuilder.addValues(value2);
        Vector y_vec = vectorOrBuilder.build();
        EncryptedNumber m1 = encryptor.encrypt(y_vec.getValues(0));
        int n = 1000;
        long start = System.nanoTime();
        for (int i = 0; i < n; i++) {
            m1 = encryptor.encrypt(y_vec.getValues(0));
        }
        long end = System.nanoTime();
        System.out.println("1000 encryption time = " + (end-start));

        start = System.nanoTime();
        for (int i = 0; i < n; i++) {
            value = decryptor.decrypt(m1);
        }
        end = System.nanoTime();
        System.out.println("1000 decryption time = " + (end-start));

    }

    @Test
    public void testAdd1() {
        PaillierPrivateKey privateKey = PaillierPrivateKey.create(1024);
        PaillierPublicKey publicKey = privateKey.getPublicKey();
        Encryptor encryptor = new Encryptor(publicKey);
        Decryptor decryptor = new Decryptor(privateKey);
        Vector.Builder vectorOrBuilder = Vector.newBuilder();
        double value = 1;
        double value2 = 2;
        vectorOrBuilder.addValues(value);
        vectorOrBuilder.addValues(value2);
        Vector y_vec = vectorOrBuilder.build();
        EncryptedNumber m1 = encryptor.encrypt(y_vec.getValues(0));
        EncryptedNumber m2 = encryptor.encrypt(y_vec.getValues(1));

        EncryptedNumber res = m1;
        BigInteger c1 = m1.calculateCiphertext();
        BigInteger c2 = m2.calculateCiphertext();
        PaillierContext pc = m1.getContext();
        BigInteger n2 = pc.getPublicKey().getModulusSquared();
        BigInteger res1 = new BigInteger("1");
        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            res = res.add(m2);
//            res1 = c1.multiply(c2).modPow(new BigInteger("1"), n2);
        }
        long end = System.nanoTime();
        System.out.println("1000 add time = " + (end-start));
        System.out.println(decryptor.decrypt(res));
    }

    @Test
    public void testMul() {
        PaillierPrivateKey privateKey = PaillierPrivateKey.create(1024);
        PaillierPublicKey publicKey = privateKey.getPublicKey();
        Encryptor encryptor = new Encryptor(publicKey);
        Vector.Builder vectorOrBuilder = Vector.newBuilder();
        double value = 1;
        long value2 = 10;
        vectorOrBuilder.addValues(value);
        Vector y_vec = vectorOrBuilder.build();
        EncryptedNumber m1 = encryptor.encrypt(y_vec.getValues(0));

        EncryptedNumber res = m1.multiply(value2);
        BigInteger c1 = m1.calculateCiphertext();
        PaillierContext pc = m1.getContext();
        BigInteger n2 = pc.getPublicKey().getModulusSquared();
        BigInteger res1 = new BigInteger("1");
        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
//            res = m1.multiply(value2);
//            double x = 10 * 1;
            res1 = c1.modPow(new BigInteger("10"), n2);
        }
        long end = System.nanoTime();
        System.out.println("1000 multiplication time = " + (end-start));

    }


    public long[] testAdd(int length, boolean issafe) {
        long[] res = new long[3];
        double[] data1 = DataGenerate.generate(length);
        double[] data2 = DataGenerate.generate(length);
        System.out.println("data size is:" + length + ", isSafe is:" + issafe);
        // create(var0) var0 is bitLength
        PaillierPrivateKey privateKey = PaillierPrivateKey.create(512);
        PaillierPublicKey publicKey = privateKey.getPublicKey();
//        PaillierUtil.privateKeyToFile(privateKey, PRIVATE_KEY_FILE);
//        PaillierUtil.publicKeyToFile(publicKey, PUBLIC_KEY_FILE);
        Encryptor encryptor = new Encryptor(publicKey);
        Decryptor decryptor = new Decryptor(privateKey);

        List<EncryptedNumber> m1s = new ArrayList<>();
        long start1 = System.currentTimeMillis();
        for (int i = 0; i < data1.length; i++) {
            m1s.add(encryptor.encrypt(data1[i],issafe));
        }
        List<EncryptedNumber> m2s = new ArrayList<>();
        for (int i = 0; i < data2.length; i++) {
            m2s.add(encryptor.encrypt(data2[i],issafe));
        }
        long end1 = System.currentTimeMillis();
        System.out.println("encrypt time:" + (end1-start1) + " ms");
        res[0] = end1-start1;


        long start4 = System.currentTimeMillis();
        List<EncryptedNumber> emRes = new ArrayList<>();
        for(int i=0; i<data1.length; i++){
            EncryptedNumber m1= m1s.get(i);
            EncryptedNumber m2 = m2s.get(i);
            emRes.add(m1.add(m2));
        }
        long end4 = System.currentTimeMillis();
        System.out.println("operation:add time:" + (end4-start4) + " ms");
        res[1] = (end4-start4);


        long start5 = System.currentTimeMillis();
        double[] deemss = new double[data1.length];
        for(int i=0; i<data1.length; i++){
            deemss[i] = decryptor.decrypt(emRes.get(i));
        }
        long end5 = System.currentTimeMillis();
        System.out.println("decrypt time:" + (end5-start5) + " ms");
        res[2] = (end5-start5);

        System.out.println("full time:" + (end5-start1) + " ms");

        System.out.println("" + emRes.get(0));
        System.out.println("deemss : " + deemss[1]);
        System.out.println("add: " + (data1[1]+data2[1]));
        System.out.println("diff: " + (data1[1]+data2[1]-deemss[1]));
        /* test homomorphic properties -> D(E(m1)*E(m2) mod n^2) = (m1 + m2) mod n */
//        PaillierContext paillierContext = new PaillierContext(publicKey, true, 1024, 16);

//        System.out.println(product_em1em2);
//        BigInteger decrypted = Paillier.decryption(product_em1em2, keyPair);
//        System.out.println("decrypted sum: " + decrypted.toString());
//        Assert.assertEquals(m1.add(m2), decrypted);
        return res;
    }


    @Test
    public void testAddRe(){
        int[] n = new int[]{10,100,1000,10000,50000};
//        int[] n = new int[]{10,20,30,50,100,200,500,1000,2000,3000,5000};
        long[][] result = new long[n.length][3];
        for(int i=0; i<n.length;i++){
            long[] r =  testAdd(n[i],true);
            result[i] = r;
        }
        System.out.println("res:" + Arrays.deepToString(result));
    }



    @Test
    public void testAddDouble() {
        // create(var0) var0 is bitLength
        PaillierPrivateKey privateKey = PaillierPrivateKey.create(1024);
        PaillierPublicKey publicKey = privateKey.getPublicKey();
//        PaillierUtil.privateKeyToFile(privateKey, PRIVATE_KEY_FILE);
//        PaillierUtil.publicKeyToFile(publicKey, PUBLIC_KEY_FILE);
        Encryptor encryptor = new Encryptor(publicKey);
        Decryptor decryptor = new Decryptor(privateKey);
        Vector.Builder vectorOrBuilder = Vector.newBuilder();
        vectorOrBuilder.addValues(20.5);
        vectorOrBuilder.addValues(60.5);
        Vector y_vec = vectorOrBuilder.build();

        EncryptedNumber m1 = encryptor.encrypt(y_vec.getValues(0));
        EncryptedNumber m2 = encryptor.encrypt(y_vec.getValues(1));

        EncryptedNumber m1f = encryptor.encrypt(y_vec.getValues(0), false);
        EncryptedNumber m2f = encryptor.encrypt(y_vec.getValues(1), false);

        /* printout encrypted text*/
        System.out.println("em1:" + m1.getContext());
        System.out.println("em2:" + m2);
        System.out.println("em1 is_not_safe:" + m1f);
        System.out.println("em2 is_not_safe:" + m2f);
        /* printout decrypted text */

        System.out.println(decryptor.decrypt(m1));
        System.out.println(decryptor.decrypt(m2));
        System.out.println(decryptor.decrypt(m1f));
        System.out.println(decryptor.decrypt(m2f));
    }

    @Test
    public void testZero() {
        // create(var0) var0 is bitLength
        PaillierPrivateKey privateKey = PaillierPrivateKey.create(1024);
        PaillierPublicKey publicKey = privateKey.getPublicKey();
//        PaillierUtil.privateKeyToFile(privateKey, PRIVATE_KEY_FILE);
//        PaillierUtil.publicKeyToFile(publicKey, PUBLIC_KEY_FILE);
        Encryptor encryptor = new Encryptor(publicKey);
        Decryptor decryptor = new Decryptor(privateKey);
        Vector.Builder vectorOrBuilder = Vector.newBuilder();
        vectorOrBuilder.addValues(20);
        vectorOrBuilder.addValues(60);
        Vector y_vec = vectorOrBuilder.build();

        EncryptedNumber m1 = encryptor.encrypt(y_vec.getValues(0));
        EncryptedNumber m2 = encryptor.encrypt(y_vec.getValues(1));

        EncryptedNumber m1f = encryptor.encrypt(y_vec.getValues(0), false);
        EncryptedNumber m2f = encryptor.encrypt(y_vec.getValues(1), false);

        /* printout encrypted text*/
        System.out.println("em1:" + m1.getContext());
        System.out.println("em2:" + m2);
        System.out.println("em1 is_not_safe:" + m1f);
        System.out.println("em2 is_not_safe:" + m2f);
        /* printout decrypted text */

        System.out.println(decryptor.decrypt(m1));
        System.out.println(decryptor.decrypt(m2));
        System.out.println(decryptor.decrypt(m1f));
        System.out.println(decryptor.decrypt(m2f));
    }

    public static void main(String[] args) {
        PaillierBenchmark util = new PaillierBenchmark();
        util.testAdd(5000, false);
    }
}
