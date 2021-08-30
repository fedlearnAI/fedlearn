package com.jdt.fedlearn.core.encryption.distributedPaillier;

import com.jdt.fedlearn.core.dispatch.DistributedKeyGeneCoordinator;
import com.jdt.fedlearn.core.encryption.nativeLibLoader;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.example.CommonRunKeyGene;
import com.jdt.fedlearn.core.exception.WrongValueException;
import com.jdt.fedlearn.core.math.MathExt;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillier.SCALE;
import static com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillier.enc;
import static com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillierNative.signedByteArray;
import static com.jdt.fedlearn.core.encryption.distributedPaillier.HomoEncryptionUtil.*;
import static com.jdt.fedlearn.core.math.MathExt.*;
import static com.jdt.fedlearn.core.util.TypeConvUtils.toJsons;

public class TestDistPaillierDistribute {
    private final Random prg = new Random();
    int n = 3;
    int l = 128;
    long maxNegAbs = Long.MAX_VALUE;
    HomoEncryptionUtil key = new HomoEncryptionUtil(n, l, false);
    HomoEncryptionDebugUtil decHelper;

    @BeforeMethod
    private void setUp() throws IOException {

        List<ClientInfo> clientList;
        String[] allAddr;
        final String testLogFileName = "GeneKeyInTestDistPaillierDistribute";
        final int bitLen = l;
        final int batchSize = 100;

        try {
            nativeLibLoader.load();
        } catch (UnsatisfiedLinkError e) {
            System.exit(1);
        }

        ClientInfo party1 = new ClientInfo("127.0.0.1", 80, "http", "", "0");
        ClientInfo party2 = new ClientInfo("127.0.0.2", 80, "http", "", "1");
        ClientInfo party3 = new ClientInfo("127.0.0.3", 80, "http", "", "2");
        clientList = Arrays.asList(party1, party2, party3);
        allAddr = new String[clientList.size()];
        int cnt = 0;
        for (ClientInfo client : clientList) {
            allAddr[cnt++] = client.getIp() + client.getPort();
        }


        DistributedKeyGeneCoordinator coordinator = new DistributedKeyGeneCoordinator(batchSize, clientList.size(), bitLen, allAddr, true, testLogFileName);
        CommonRunKeyGene.generate(coordinator, clientList.toArray(new ClientInfo[0]));


        DistributedPaillier.DistPaillierPubkey pubkey = new DistributedPaillier.DistPaillierPubkey();
        DistributedPaillier.DistPaillierPrivkey privkey1 = new DistributedPaillier.DistPaillierPrivkey();
        DistributedPaillier.DistPaillierPrivkey privkey2 = new DistributedPaillier.DistPaillierPrivkey();
        DistributedPaillier.DistPaillierPrivkey privkey3 = new DistributedPaillier.DistPaillierPrivkey();

        pubkey.loadClassFromFile("pubKey");
        privkey1.loadClassFromFile("privKey-" + 1);
        privkey2.loadClassFromFile("privKey-" + 2);
        privkey3.loadClassFromFile("privKey-" + 3);
        DistributedPaillier.DistPaillierPrivkey[] allSk = new DistributedPaillier.DistPaillierPrivkey[3];
        allSk[0] = privkey1;
        allSk[1] = privkey2;
        allSk[2] = privkey3;
        key.setSkAll(allSk);
        key.setPk(pubkey);
        decHelper = new HomoEncryptionDebugUtil(key.getPk(), key.getSkAll(), key.n, maxNegAbs);
    }

    @Test
    public void testMultipleDouble() {
        int num_sample_per_set = 100;
        for (int i = 0; i < 1; i++) {
            testDoubleType(num_sample_per_set);
            System.out.println("passed samples " + (i + 1) * num_sample_per_set);
        }
    }

    @Test
    public void testMultipleLong() {
        int num_sample_per_set = 100;
        for (int i = 0; i < 1; i++) {
            testLongType(num_sample_per_set);
            System.out.println("passed samples " + (i + 1) * num_sample_per_set);
        }
    }

    public void testLongType(int numSample) {

        long[] a = new long[numSample];
        long[] b = new long[numSample];

        for (int i = 0; i < numSample; i++) {
            a[i] = prg.nextInt((int) 1E7);
            b[i] = prg.nextInt((int) 1E7);
        }
        if (dotMultiply(a, b) < 0) {
            throw new WrongValueException("a and b are too large. Inner product larger than Long.Max...");
        }

        // enc list
        signedByteArray[] a_enc = key.encryption(a, key.getPk());
        signedByteArray[] b_enc = key.encryption(b, key.getPk());

        // enc one by one
        signedByteArray[] a_enc_1 = new signedByteArray[numSample];
        signedByteArray[] b_enc_1 = new signedByteArray[numSample];
        for (int i = 0; i < numSample; i++) {
            a_enc_1[i] = enc(a[i], key.getPk());
            b_enc_1[i] = enc(b[i], key.getPk());

            // enc 过程中有随机数，相同的明文得到的密文 *一般* 不同
//            Assert.assertTrue(!toJsons(a_enc_1[i]).equals(toJsons(a_enc[i])));
//            Assert.assertTrue(!toJsons(b_enc_1[i]).equals(toJsons(b_enc[i])));

            // 但是解密后一定相同
            Assert.assertEquals(decHelper.dec(a_enc[i]), decHelper.dec(a_enc_1[i]));
            Assert.assertEquals(a[i], decHelper.dec(a_enc_1[i]));
            Assert.assertEquals(decHelper.dec(b_enc[i]), decHelper.dec(b_enc_1[i]));
            Assert.assertEquals(b[i], decHelper.dec(b_enc_1[i]));
        }

        // add
        signedByteArray[] add_ab = key.add(a_enc, b_enc, key.getPk());
        signedByteArray[] add_ab_1 = key.add(a_enc_1, b_enc_1, key.getPk());
        // 不同的密文，只要明文相同，做运算解密后的结果一定一致;
        // 并且 enc_list 和 enc 可以混用
        Assert.assertEquals(toJsons(decHelper.dec(add_ab_1)), toJsons(decHelper.dec(add_ab)));
        Assert.assertEquals(toJsons(add(a, b)), toJsons(decHelper.dec(add_ab)));

        // sub
        long[] neg_one = new long[b_enc.length];
        Arrays.fill(neg_one, -1);
        add_ab = key.add(a_enc, key.mul(b_enc, neg_one, key.getPk()), key.getPk());
        add_ab_1 = key.add(a_enc_1, key.mul(b_enc, neg_one, key.getPk()), key.getPk());
        // 不同的密文，只要明文相同，做运算解密后的结果一定一致;
        // 并且 enc_list 和 enc 可以混用
        Assert.assertEquals(toJsons(decHelper.dec(add_ab_1)), toJsons(decHelper.dec(add_ab)));
        Assert.assertEquals(toJsons(sub(a, b)), toJsons(decHelper.dec(add_ab)));

        // mul positive
        add_ab = key.mul(a_enc, b, key.getPk());
        add_ab_1 = key.mul(a_enc_1, b, key.getPk());
        // 不同的密文，只要明文相同，做运算解密后的结果一定一致;
        // 并且 enc_list 和 enc 可以混用
        Assert.assertEquals(toJsons(decHelper.dec(add_ab_1)), toJsons(decHelper.dec(add_ab)));
        Assert.assertEquals(toJsons(elementwiseMul(a, b)), toJsons(decHelper.dec(add_ab)));

        // mul positive
        add_ab = key.mul(a_enc, b, key.getPk());
        add_ab_1 = key.mul(a_enc_1, b, key.getPk());
        // 不同的密文，只要明文相同，做运算解密后的结果一定一致;
        // 并且 enc_list 和 enc 可以混用
        Assert.assertEquals(toJsons(decHelper.dec(add_ab_1)), toJsons(decHelper.dec(add_ab)));
        Assert.assertEquals(toJsons(elementwiseMul(a, b)), toJsons(decHelper.dec(add_ab)));

        // mul  negative
        add_ab = key.mul(a_enc, elementwiseMul(b, -1), key.getPk());
        add_ab_1 = key.mul(a_enc_1, elementwiseMul(b, -1), key.getPk());
        Assert.assertEquals(toJsons(decHelper.dec(add_ab_1)), toJsons(decHelper.dec(add_ab)));
        Assert.assertEquals(toJsons(elementwiseMul(a, elementwiseMul(b, -1))), toJsons(decHelper.dec(add_ab)));

        // inner product
        signedByteArray inner_ab = key.innerProduct(a_enc, b, key.getPk());
        signedByteArray inner_ab_1 = key.innerProduct(a_enc_1, b, key.getPk());
        Assert.assertEquals(toJsons(decHelper.dec(inner_ab)), toJsons(decHelper.dec(inner_ab_1)));
        Assert.assertEquals(toJsons(dotMultiply(a, b)), toJsons(decHelper.dec(inner_ab_1)));

//         div positive -- 能整除 ( a*b / b ), b is positive
        long[] a_div = elementwiseMul(a, b);
        signedByteArray[] a_div_enc = key.encryption(a_div, key.getPk());
        add_ab = key.div(a_div_enc, b, key.getPk());
        Assert.assertEquals(toJsons(a), toJsons(decHelper.dec(add_ab)));
//
        // div positive -- 能整除 ( a*b / b ), b is negative
        neg_one = new long[b_enc.length];
        Arrays.fill(neg_one, -1);
        a_div = elementwiseMul(a, elementwiseMul(b, neg_one));
        a_div_enc = key.encryption(a_div, key.getPk());

        add_ab = key.div(a_div_enc, b, key.getPk());
        Assert.assertEquals(toJsons(elementwiseMul(a, neg_one)), toJsons(decHelper.dec(add_ab)));

        // test decryption
        signedByteArray[][] partial = new signedByteArray[key.getSkAll().length][];
        for (int j = 0; j < key.getSkAll().length; j++) {
            key.getSkAll()[j].setRank(j + 1);
            partial[j] = key.decryptPartial(add_ab, key.getSkAll()[j]);
        }
        decHelper.dec(add_ab);
        long[] add_ab_dec = key.decryptFinalLong(partial, add_ab, key.getSkAll()[0]);
        Assert.assertEquals(toJsons(elementwiseMul(a, neg_one)), toJsons(add_ab_dec));
    }

    public void testDoubleType(int num_sample) {

        double[] a = new double[num_sample];
        double[] b = new double[num_sample];

        for (int i = 0; i < num_sample; i++) {
            a[i] = prg.nextInt((int) 1E7) / 1000d;
            b[i] = prg.nextInt((int) 1E7) / 1000d;
        }
        if (dotMultiply(a, b) < 0) {
            throw new WrongValueException("a and b are too large. Inner product larger than Long.Max...");
        }

        double precision = (1d / SCALE) * (1E7 / 1000);

        // enc list
        signedByteArray[] a_enc = key.encryption(a, key.getPk());
        signedByteArray[] b_enc = key.encryption(b, key.getPk());

        // add
        signedByteArray[] add_ab = key.add(a_enc, b_enc, key.getPk());
        Assert.assertTrue(doubleLstEqual(decHelper.decDouble(add_ab), MathExt.add(a, b)));

        // sub
        long[] neg_one = new long[b_enc.length];
        Arrays.fill(neg_one, -1);
        add_ab = key.add(a_enc, key.mul(b_enc, neg_one, key.getPk()), key.getPk());
        Assert.assertTrue(doubleLstEqual(decHelper.decDouble(add_ab), sub(a, b)));

        // mul positive
        add_ab = key.mul(a_enc, b, key.getPk());
        Assert.assertTrue(
                listAnyPrecisionEqual(
                        decHelper.decDouble(add_ab),
                        elementwiseMul(a, b), precision * precision)
        );

        // mul  negative
        add_ab = key.mul(a_enc, elementwiseMul(b, -1d), key.getPk());
        Assert.assertTrue(
                listAnyPrecisionEqual(
                        decHelper.decDouble(add_ab),
                        elementwiseMul(a, elementwiseMul(b, -1)),
                        precision * precision)
        );

        // inner product
        signedByteArray inner_ab = key.innerProduct(a_enc, b, key.getPk());
        Assert.assertTrue(anyPrecisionEqual(
                decHelper.decDouble(inner_ab),
                dotMultiply(a, b),
                precision * precision * num_sample)
        );

        // test decryption
        signedByteArray[][] partial = new signedByteArray[key.getSkAll().length][];
        for (int j = 0; j < key.getSkAll().length; j++) {
            key.getSkAll()[j].setRank(j + 1);
            partial[j] = key.decryptPartial(add_ab, key.getSkAll()[j]);
        }
        decHelper.dec(add_ab);
        long[] add_ab_dec = key.decryptFinalLong(partial, add_ab, key.getSkAll()[0]);
        Assert.assertTrue(
                listAnyPrecisionEqual(
                        add_ab_dec,
                        elementwiseMul(a, elementwiseMul(b, -1)),
                        precision * precision)
        );
    }

    public void test_special_numbers() {

        int N = 20;
        long[] a = new long[N];
        long[] b = new long[N];
        for (int i = 0; i < N; i++) {
            a[i] = prg.nextLong();
            b[i] = prg.nextLong();
        }
        utils.__printArray__(a);
        utils.__printArray__(b);

        signedByteArray[] a_enc = key.encryption(a, key.getPk());
        signedByteArray[] b_enc = key.encryption(b, key.getPk());

        signedByteArray[] add_ab = key.add(a_enc, b_enc, key.getPk());
        signedByteArray inner_ab = key.innerProduct(a_enc, b, key.getPk());

        long dec_inner_ab = decHelper.dec(inner_ab);
        long[] dec_add_ab = decHelper.dec(add_ab);

        Assert.assertEquals(toJsons(add(a, b)), toJsons(dec_add_ab));
        Assert.assertEquals(toJsons(dotMultiply(a, b)), toJsons(dec_inner_ab));
    }

    static class utils {
        public static void __printArray__(long[] inputArray) {
            System.out.print("list len = " + inputArray.length + " ");
            for (long element : inputArray) {
                System.out.print(element + " ");
            }
        }
    }
}
