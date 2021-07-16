package com.jdt.fedlearn.core.encryption;

import com.jdt.fedlearn.core.util.HashUtil;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class TestDiffieHellman {
    public static void main(String[] args) throws NoSuchAlgorithmException {
        int bitLength = 1024;
        int certainty = 64;
        BigInteger g = new BigInteger(bitLength / 2, certainty, new Random());
        BigInteger n = new BigInteger(bitLength / 2, certainty, new Random());
//        KeyPairGenerator kpGen = KeyPairGenerator.getInstance("DH");
//        kpGen.initialize(512);
//        KeyPair kp = kpGen.generateKeyPair();
//        System.out.println(new String((kp.getPrivate().getEncoded())));
//        String x = "aabbcc";
//        String y = "ddeeff";
//        generateSecretKey(kp.getPrivate().getEncoded(),kp.getPublic().getEncoded());
        String uid1 = "akks";
        BigInteger hashUid1 = HashUtil.md5(uid1);
        //party 1 执行变换
        BigInteger a1s = DiffieHellman.a1(g, n, hashUid1);
        BigInteger res1 = DiffieHellman.a1(a1s, n, new BigInteger("20"));
        System.out.println(res1);

        String uid2 = "akks";
        BigInteger hashUid2 = HashUtil.md5(uid2);
        BigInteger a2s = DiffieHellman.a1(g, n, hashUid2);
        BigInteger res2 = DiffieHellman.a1(a2s, n, new BigInteger("20"));
        System.out.println(res2);

        DiffieHellman.trans(uid1, g, n, new BigInteger("100"), new BigInteger("99"));
        DiffieHellman.trans(uid1, g, n, new BigInteger("99"), new BigInteger("100"));
    }
}
