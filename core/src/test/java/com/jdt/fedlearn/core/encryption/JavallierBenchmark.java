package com.jdt.fedlearn.core.encryption;

import com.jdt.fedlearn.core.encryption.common.Ciphertext;
import com.jdt.fedlearn.core.encryption.javallier.JavallierCiphertext;
import com.jdt.fedlearn.core.encryption.javallier.JavallierPriKey;
import com.jdt.fedlearn.core.encryption.javallier.JavallierPubKey;
import com.jdt.fedlearn.core.encryption.javallier.JavallierTool;
import com.n1analytics.paillier.PaillierContext;

/**
 * 全流程测试
 */
public class JavallierBenchmark {

    private double[] generate(int n) {
        double[] res = new double[n];
        for (int i = 0; i < n; i++) {
            //-10~10
            res[i] = (Math.random() - 0.5) * 20;
        }
        return res;
    }

    public long[] testFull(int length, boolean isSafe) {
        long[] res = new long[3];
        double[] data1 = generate(length);
        double[] data2 = generate(length);
        System.out.println("data size is:" + length + ", isSafe is:" + isSafe);
        JavallierTool javallierTool = new JavallierTool();
        // create(var0) var0 is bitLength
        JavallierPriKey privateKey = javallierTool.keyGenerate(512, 64);
        JavallierPubKey publicKey = (JavallierPubKey) privateKey.generatePublicKey();


        long start1 = System.currentTimeMillis();
        Ciphertext[] m1s = new Ciphertext[data1.length];
        for (int i = 0; i < data1.length; i++) {
            m1s[i]= javallierTool.encrypt(data1[i], publicKey);
        }
        System.out.println("encrypt size:" + data1.length);
        Ciphertext[] m2s = new Ciphertext[data2.length];
        for (int i = 0; i < data2.length; i++) {
            m2s[i] = javallierTool.encrypt(data2[i], publicKey);
        }

        long end1 = System.currentTimeMillis();
        System.out.println("encrypt time:" + (end1-start1) + " ms");
        res[0] = end1-start1;


        long start4 = System.currentTimeMillis();
        String[] emRes = addA(publicKey, m1s, m2s);
        long end4 = System.currentTimeMillis();
        System.out.println("operation:add time:" + (end4-start4) + " ms");
        res[1] = (end4-start4);

        long start5 = System.currentTimeMillis();
        double[] deemss = new double[data1.length];
        for(int i=0; i<data1.length; i++){
            deemss[i] = javallierTool.decrypt(javallierTool.restoreCiphertext(emRes[i]), privateKey);
        }
//        double[] deemss = Arrays.stream(emRes).parallel().map(x->PaillierTool.decryption(x, privateKey)).mapToDouble(x-> x).toArray();

        long end5 = System.currentTimeMillis();
        System.out.println("decrypt time:" + (end5-start5) + " ms");
        res[2] = (end5-start5);

        System.out.println("full time:" + (end5-start1) + " ms");

//        System.out.println("" + emRes.get(0));
        System.out.println("deemss : " + deemss[1]);
        System.out.println("add: " + (data1[1]+data2[1]));
        System.out.println("diff: " + (data1[1]+data2[1]-deemss[1]));
        return res;
    }

    private String[] addA(JavallierPubKey publicKey, Ciphertext[] m1s, Ciphertext[] m2s){
        JavallierTool javallierTool = new JavallierTool();
        long start = System.currentTimeMillis();
        PaillierContext context = publicKey.getPaillierPublicKey().createSignedContext();
        Ciphertext[] m1SCiphertext = new JavallierCiphertext[m1s.length];
        Ciphertext[] m2SCiphertext = new JavallierCiphertext[m2s.length];
        long s2 = System.currentTimeMillis();
        System.out.println("1== " + (s2 - start));
        Ciphertext[] ciphertexts = new Ciphertext[m1s.length];
        for(int i=0; i<m1s.length; i++){
            m1SCiphertext[i] = javallierTool.restoreCiphertext(m1s[i].serialize());
            m2SCiphertext[i] = javallierTool.restoreCiphertext(m2s[i].serialize());
            ciphertexts[i] = javallierTool.add(m1SCiphertext[i], m2SCiphertext[i],publicKey);
        }
        long s3 = System.currentTimeMillis();
        System.out.println("2== " + (s3 - s2));
        String[] emRes = new String[m1s.length];
        for(int i=0; i<m1s.length; i++){
            emRes[i] = ciphertexts[i].serialize();
        }
        long s4 = System.currentTimeMillis();
        System.out.println("3== " + (s4 - s3));
        return emRes;
    }

    public static void main(String[] args) {
        JavallierBenchmark tool = new JavallierBenchmark();
        tool.testFull(5000, false);
    }
}