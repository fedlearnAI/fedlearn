package com.jdt.fedlearn.core.encryption;

import com.jdt.fedlearn.core.encryption.IterativeAffine.IterativeAffineCiphertext;
import com.jdt.fedlearn.core.encryption.IterativeAffine.IterativeAffineKey;
import com.jdt.fedlearn.core.encryption.IterativeAffine.IterativeAffineTool;
import com.jdt.fedlearn.core.model.RandomForestModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.*;
import java.util.*;

public class TestIterativeAffine {
    private static final Logger logger = LoggerFactory.getLogger(RandomForestModel.class);

    double[] a;
    int n;
    IterativeAffineKey key;
    IterativeAffineCiphertext[] ciphertext;

    String fail_path = "./src/test/resources/iterativeAffineFail/failed1.txt";

    @BeforeMethod
    public void setUp() {
        n = 1000;
        a = new double[n];
        ciphertext = new IterativeAffineCiphertext[n];
//        key = IterativeAffineUtil.generateKeyPair();
        key = IterativeAffineTool.generateKeyPair(1024, 2);
    }

    @Test(priority = 1)
    public void testEncryptDecrypt() {
        logger.info("Testing encryption decryption");
        for (int i=0; i<n; i++) {
//            logger.info(String.format("Test %sth number", i));
            a[i] = Math.random() * 1e3;
//            if (Math.abs(tmp - a[i]) > 1e-8) {
//                System.out.println(String.format("tmp: %s, ai: %s", tmp, a[i]));
//                System.out.println("X: " + key.getX());
//                System.out.println("H: " + key.getH());
//                System.out.println("G: " + key.getG());
//                List<BigInteger> ns = key.getN();
//                List<BigInteger> as = key.getA();
//                List<BigInteger> ainv = key.getA_inv();
//                System.out.println("Ns: ");
//                for (BigInteger ni: ns) {
//                    System.out.println(ni);
//                }
//                System.out.println("As: ");
//                for (BigInteger ai: as) {
//                    System.out.println(ai);
//                }
//                System.out.println("Ainv: ");
//                for (BigInteger ai: ainv) {
//                    System.out.println(ai);
//                }
//                String jsonStr = key.toJson();
//                try {
//                    BufferedWriter writer = new BufferedWriter(new FileWriter(fail_path));
//                    writer.write(jsonStr);
//                    writer.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
        }
        long start = System.nanoTime();
        for (int i = 0; i < n; i++) {
            ciphertext[i] = key.encrypt(a[i]);
        }
        long end = System.nanoTime();
        System.out.println("1000 encryption time = " + (end-start));
        start = System.nanoTime();
        for (int i = 0; i < n; i++) {
            double tmp = key.decrypt(ciphertext[i]);
//            assert Math.abs(tmp - a[i]) < 1e-8;
        }
        end = System.nanoTime();
        System.out.println("1000 decryption time = " + (end-start));
    }

    @Test(priority = 2)
    public void testHomomorphicEncryptionSum() {
        logger.info("Testing homomorphic encryption sum");

        int numbers = 2;
        for (int i=0; i<n; i++) {
            a[i] = Math.random() * 1e3;
//            logger.info(String.format("number %s: %s", i, a[i]));
            ciphertext[i] = key.encrypt(a[i]);
            double tmp = key.decrypt(ciphertext[i]);
            assert tmp == a[i];
            }

        double sum = Arrays.stream(a).sum();
        IterativeAffineCiphertext cipher = ciphertext[0];
        System.out.println(ciphertext.length);
        long start = System.nanoTime();
        for (int i=1; i<ciphertext.length; i++) {
            IterativeAffineCiphertext tmp = ciphertext[i];
//            System.out.println(i);
            cipher.add(ciphertext[i]);
        }
        long end = System.nanoTime();
        double sum_decrypt = key.decrypt(cipher);
        assert Math.abs(sum - sum_decrypt) < 1e-8;
        System.out.println(String.format("Encryption sum pass, sum time %s", end - start));
    }

    public void testHomomorphicEncryptionMean() {
        logger.info("Testing homomorphic encryption mean");

        int numbers = 2;
        for (int i=0; i<n; i++) {
            a[i] = Math.random() * 1e3;
//            logger.info(String.format("number %s: %s", i, a[i]));
            ciphertext[i] = key.encrypt(a[i]);
            double tmp = key.decrypt(ciphertext[i]);
            assert tmp == a[i];
        }

        double mean = Arrays.stream(a).sum() / a.length;
        IterativeAffineCiphertext cipher = ciphertext[0];
        System.out.println(ciphertext.length);
        for (int i=1; i<ciphertext.length; i++) {
            IterativeAffineCiphertext tmp = ciphertext[i];
//            System.out.println(i);
            cipher.add(ciphertext[i]);
        }
        cipher.divide(a.length);
        double mean_decrypt = key.decrypt(cipher);
//        System.out.println("As: " + Arrays.toString(a));
//        System.out.println(String.format("Sum orig: %s, sum decrypt: %s", sum ,sum_decrypt));
//
//        List<BigInteger> ns = key.getN();
//        System.out.println(String.format("Gcd of n0 and n1: %s", ns.get(0).gcd(ns.get(1)).toString()));
//        logger.info("n: " + key.getN().get(0));
//        logger.info("a: " + key.getA().get(0));
//        logger.info("ainv: " + key.getA_inv().get(0));
//        logger.info("array length: " + a.length);
        logger.info(String.format("Mean: %s, mean decrypt: %s, diff: %s", mean, mean_decrypt, mean - mean_decrypt));
        assert Math.abs(mean - mean_decrypt) < 1e-8;
    }

    @Test
    public void testMul() {
        logger.info("Testing homomorphic encryption scalar multiplication");

        int numbers = 10;
        for (int i=0; i<n; i++) {
            a[i] = Math.random() * 1e3;
//            logger.info(String.format("number %s: %s", i, a[i]));
            ciphertext[i] = key.encrypt(a[i]);
            double tmp = key.decrypt(ciphertext[i]);
            assert tmp == a[i];
        }

        long start = System.nanoTime();
        for (int i=1; i<ciphertext.length; i++) {
            ciphertext[i].multiply(numbers);
        }
        long end = System.nanoTime();
        System.out.println(String.format("Encryption sum pass, sum time %s", end - start));
    }

    //@Test(priority = 1)
    public void failCase() {
        // load failed test
        String st = "";
        try {
            BufferedReader br = new BufferedReader(new FileReader(fail_path));
            st = br.readLine();
        } catch (FileNotFoundException e) {
            System.out.println("filenotfoundexception:"+e);
        } catch (IOException e) {
            System.out.println("ioexception:"+e);
        }
        key = IterativeAffineKey.parseJson(st);
        double ai = -380.42445535564104;
        IterativeAffineCiphertext cipher = key.encrypt(ai);
        double ai1 = key.decrypt(cipher);
        System.out.println(String.format("Ai: %s, Ai1: %s", ai, ai1));
    }

//    @Test(priority = 1)
    public void test() {
        int round = 10;
        for (int i=0; i<round; i++) {
            setUp();
            testEncryptDecrypt();
            testHomomorphicEncryptionSum();
            testHomomorphicEncryptionMean();
        }
    }

    @Test(priority = 1)
    public void testAdd() {
        long start = System.nanoTime();
        int round = 10000;
        int c = 0;
        for (int i = 0; i < round; i++) {
            c = 1 + 2;
        }
        double time = (System.nanoTime() - start) / 10000.;
        System.out.println(c);
        System.out.println(time);

    }

    @Test(priority = 1)
    public void testMulAdd() {
        double x = 1.;
        double y = 1.;
        double z = 1.;
        IterativeAffineCiphertext cx = key.encrypt(x);
        IterativeAffineCiphertext cy = key.encrypt(y);
        cx.multiply(z);
        cx.add(cy);
        double res = key.decrypt(cx);
        System.out.println(res - (x * z + y));
        assert Math.abs(res - (x * z + y)) < 1e-8;
    }
}
