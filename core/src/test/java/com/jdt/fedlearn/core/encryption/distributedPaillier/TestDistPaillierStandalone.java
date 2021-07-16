package com.jdt.fedlearn.core.encryption.distributedPaillier;

import com.jdt.fedlearn.core.exception.WrongValueException;
import com.jdt.fedlearn.core.math.MathExt;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.IntStream;

import static com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillier.SCALE;
import static com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillier.enc;
import static com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillierNative.signedByteArray;
import static com.jdt.fedlearn.core.encryption.distributedPaillier.HomoEncryptionUtil.*;
import static com.jdt.fedlearn.core.math.MathExt.*;
import static com.jdt.fedlearn.core.util.TypeConvUtils.toJsons;

public class TestDistPaillierStandalone {
    int n = 3;
    int l = 1024;
    long maxNegAbs = Long.MAX_VALUE;

    private final Random prg = new Random();
    HomoEncryptionUtil key = new HomoEncryptionUtil(n, l, false);
    final private String keyFilePath = "dist_pai_keys_3_1024";

    static class utils {
        public static void __printArray__(long[] inputArray ) {
            System.out.print("list len = " + inputArray.length + " ");
            for ( long element : inputArray ){
                System.out.print( element + " " );
            }
        }
    }
    public void test_debug_util() {
        key.generateKeys();
        HomoEncryptionDebugUtil decHelper = new HomoEncryptionDebugUtil(key.getPk(), key.getSkAll(), key.n, maxNegAbs);
        decHelper.saveToFile(keyFilePath);
        HomoEncryptionDebugUtil dec_helper2 = new HomoEncryptionDebugUtil();
        dec_helper2.loadClassFromFile(keyFilePath);
        assert(toJsons(dec_helper2).equals(toJsons(decHelper)));
    }

    /*
    验证Java和cpp互传结果正确
     */
    public void testJniIo() {
        int N = (int) 1E10;
        int M = (int) 1E3;

        for(int i = 0; i < N; i++) {
            boolean bll;
            bll = i % 2 != 0;
            byte[] bb = new byte[2048/8];
            prg.nextBytes(bb);
            signedByteArray j_in = new signedByteArray(bb, bll);
            signedByteArray c_out = new signedByteArray();
            DistributedPaillierNative.test_IO(j_in, c_out);
            assert(toJsons(c_out).equals(toJsons(j_in)));

            signedByteArray[] java_in = new signedByteArray[M];
            signedByteArray[] cout = new signedByteArray[M];
            for(int j = 0; j < M; j++) {
                boolean bl;
                bl = j % 2 != 0;
                byte[] b = new byte[2048/8];
                prg.nextBytes(b);
                java_in[j] = new signedByteArray(b, bl);
                cout[i] = new signedByteArray();
            }
            DistributedPaillierNative.test_IO(java_in, cout);
            assert(toJsons(cout).equals(toJsons(java_in)));
        }
    }

    public void testMore() {
        int num_sample_per_set = 20;
        for(int i = 0; i < 100000; i++) {
            test_double_type(num_sample_per_set);
            System.out.println("passed samples " + (i+1)*num_sample_per_set);
        }
    }

    private int parallelHelper(int i){
        testLongType(1000);
        System.out.println("passed i = " + i);
        return 1;
    }

    private void initKeyByUtil(HomoEncryptionDebugUtil dec_helper){
        this.key.setPk(dec_helper.pk);
        this.key.setSkAll(dec_helper.sk_lst);
    }

    public void testMoreThreadParallel() {
        IntStream.range(0, 1000000).parallel().mapToObj(this::parallelHelper);
    }

    public void testLongType(int numSample) {

        long [] a = new long[numSample];
        long [] b = new long[numSample];

        for(int i = 0; i< numSample; i++) {
            a[i] = prg.nextInt((int) 1E7);
            b[i] = prg.nextInt((int) 1E7);
        }
        if(dotMultiply(a, b) < 0){
            throw new WrongValueException("a and b are too large. Inner product larger than Long.Max...");
        }

        key.generateKeys();
        HomoEncryptionDebugUtil decHelper = new HomoEncryptionDebugUtil(key.getPk(), key.getSkAll(), key.n, maxNegAbs);
//        HomoEncryptionDebugUtil decHelper = new HomoEncryptionDebugUtil();
//        decHelper.loadClassFromFile(key_file_path);
//        decHelper.max_neg_abs = max_neg_abs;
//        init_key_by_util(decHelper);

        // enc list
        signedByteArray[] a_enc = key.encryption(a, key.getPk());
        signedByteArray[] b_enc = key.encryption(b, key.getPk());

        // enc one by one
        signedByteArray[] a_enc_1 = new signedByteArray[numSample];
        signedByteArray[] b_enc_1 = new signedByteArray[numSample];
        for(int i = 0; i < numSample; i++) {
            a_enc_1[i] = enc(a[i], key.getPk());
            b_enc_1[i] = enc(b[i], key.getPk());

            // enc 过程中有随机数，相同的明文得到的密文 *一般* 不同
//            assert(!toJsons(a_enc_1[i]).equals(toJsons(a_enc[i])));
//            assert(!toJsons(b_enc_1[i]).equals(toJsons(b_enc[i])));

            // 但是解密后一定相同
            assert(decHelper.dec(a_enc_1[i]) == decHelper.dec(a_enc[i]));
            assert(decHelper.dec(a_enc_1[i]) == a[i]);
            assert(decHelper.dec(b_enc_1[i]) == decHelper.dec(b_enc[i]));
            assert(decHelper.dec(b_enc_1[i]) == b[i]);
        }

        // add
        signedByteArray[] add_ab = key.add(a_enc, b_enc, key.getPk());
        signedByteArray[] add_ab_1 = key.add(a_enc_1, b_enc_1, key.getPk());
        // 不同的密文，只要明文相同，做运算解密后的结果一定一致;
        // 并且 enc_list 和 enc 可以混用
        assert(toJsons(decHelper.dec(add_ab)).equals(toJsons(decHelper.dec(add_ab_1))));
        assert(toJsons(decHelper.dec(add_ab)).equals(toJsons( MathExt.add(a, b) )));

        // sub
        long [] neg_one = new long[b_enc.length];
        Arrays.fill(neg_one, -1);
        add_ab = key.add(a_enc, key.mul(b_enc, neg_one, key.getPk()), key.getPk());
        add_ab_1 = key.add(a_enc_1, key.mul(b_enc, neg_one, key.getPk()), key.getPk());
        // 不同的密文，只要明文相同，做运算解密后的结果一定一致;
        // 并且 enc_list 和 enc 可以混用
        assert(toJsons(decHelper.dec(add_ab)).equals(toJsons(decHelper.dec(add_ab_1))));
        assert(toJsons(decHelper.dec(add_ab)).equals(toJsons( sub(a, b) )));

        // mul positive
        add_ab = key.mul(a_enc, b, key.getPk());
        add_ab_1 = key.mul(a_enc_1, b, key.getPk());
        // 不同的密文，只要明文相同，做运算解密后的结果一定一致;
        // 并且 enc_list 和 enc 可以混用
        assert(toJsons(decHelper.dec(add_ab)).equals(toJsons(decHelper.dec(add_ab_1))));
        assert(toJsons(decHelper.dec(add_ab)).equals(toJsons( elementwiseMul(a, b) )));

        // mul positive
        add_ab = key.mul(a_enc, b, key.getPk());
        add_ab_1 = key.mul(a_enc_1, b, key.getPk());
        // 不同的密文，只要明文相同，做运算解密后的结果一定一致;
        // 并且 enc_list 和 enc 可以混用
        assert(toJsons(decHelper.dec(add_ab)).equals(toJsons(decHelper.dec(add_ab_1))));
        assert(toJsons(decHelper.dec(add_ab)).equals(toJsons( elementwiseMul(a, b) )));

        // mul  negative
        add_ab = key.mul(a_enc, elementwiseMul(b, -1), key.getPk());
        add_ab_1 = key.mul(a_enc_1, elementwiseMul(b, -1), key.getPk());
        assert(toJsons(decHelper.dec(add_ab)).equals(toJsons(decHelper.dec(add_ab_1))));
        assert(toJsons(decHelper.dec(add_ab)).equals(toJsons(elementwiseMul(a, elementwiseMul(b, -1)) )));

        // inner product
        signedByteArray inner_ab = key.innerProduct(a_enc, b, key.getPk());
        signedByteArray inner_ab_1 = key.innerProduct(a_enc_1, b, key.getPk());
        assert(toJsons(decHelper.dec(inner_ab_1)).equals(toJsons(decHelper.dec(inner_ab))));
        assert(toJsons(decHelper.dec(inner_ab_1)).equals(toJsons( dotMultiply(a, b) )));

//         div positive -- 能整除 ( a*b / b ), b is positive
        long [] a_div = elementwiseMul(a, b);
        signedByteArray[] a_div_enc = key.encryption(a_div, key.getPk());
        add_ab = key.div(a_div_enc, b, key.getPk());
        assert(toJsons(decHelper.dec(add_ab)).equals(toJsons( a )));
//
        // div positive -- 能整除 ( a*b / b ), b is negative
        neg_one = new long[b_enc.length];
        Arrays.fill(neg_one, -1);
        a_div = elementwiseMul(a, elementwiseMul(b, neg_one));
        a_div_enc = key.encryption(a_div, key.getPk());

        add_ab = key.div(a_div_enc, b, key.getPk());
        assert(toJsons(decHelper.dec(add_ab)).equals(toJsons( elementwiseMul(a, neg_one) )));

        // div positive -- 不能整除 ( (a * b + 1) / b  ) Paillier does not support this.
//        long [] one = new long[b.length];
//        Arrays.fill(one, 1);
//         a_div = elementwise_mul(a, b);
//        a_div = MathExt.add(a_div, one);
//        a_div_enc = key.encryption(a_div, key.getPk());
//        add_ab = key.div(a_div_enc, b, key.getPk());
//        assert(toJsons(decHelper.dec(add_ab)).equals(toJsons( a )));

        // test decryption
        signedByteArray[][] partial = new signedByteArray[key.getSkAll().length][];
        for(int j = 0; j < key.getSkAll().length; j++) {
            key.getSkAll()[j].setRank(j+1);
            partial[j] = key.decryptPartial(add_ab, key.getSkAll()[j]);
        }
        decHelper.dec(add_ab);
        long [] add_ab_dec = key.decryptFinalLong(partial, add_ab, key.getSkAll()[0]);
        assert (toJsons(add_ab_dec).equals(toJsons( elementwiseMul(a, neg_one) )));
    }

    public void test_double_type(int num_sample) {

        double [] a = new double[num_sample];
        double [] b = new double[num_sample];

        for(int i = 0; i< num_sample; i++) {
            a[i] = prg.nextInt((int) 1E7) / 1000d;
            b[i] = prg.nextInt((int) 1E7) / 1000d;
        }
        if(dotMultiply(a, b) < 0){
            throw new WrongValueException("a and b are too large. Inner product larger than Long.Max...");
        }

        double precision = (1d/SCALE)*(1E7/1000);
        key.generateKeys();
        HomoEncryptionDebugUtil dec_helper = new HomoEncryptionDebugUtil(key.getPk(), key.getSkAll(), key.n, maxNegAbs);

        // enc list
        signedByteArray[] a_enc = key.encryption(a, key.getPk());
        signedByteArray[] b_enc = key.encryption(b, key.getPk());

        // add
        signedByteArray[] add_ab = key.add(a_enc, b_enc, key.getPk());
        assert( doubleLstEqual(dec_helper.decDouble(add_ab),  MathExt.add(a, b) ));

        // sub
        long [] neg_one = new long[b_enc.length];
        Arrays.fill(neg_one, -1);
        add_ab = key.add(a_enc, key.mul(b_enc, neg_one, key.getPk()), key.getPk());
        assert( doubleLstEqual(dec_helper.decDouble(add_ab),  sub(a, b) ));

        // mul positive
        add_ab = key.mul(a_enc, b, key.getPk());
        assert(
                listAnyPrecisionEqual(
                        dec_helper.decDouble(add_ab),
                        elementwiseMul(a, b) , precision*precision)
        );

        // mul  negative
        add_ab = key.mul(a_enc, elementwiseMul(b, -1d), key.getPk());
        assert(
                listAnyPrecisionEqual(
                        dec_helper.decDouble(add_ab),
                        elementwiseMul(a, elementwiseMul(b, -1)),
                        precision*precision)
                );

        // inner product
        signedByteArray inner_ab = key.innerProduct(a_enc, b, key.getPk());
        assert( anyPrecisionEqual(
                dec_helper.decDouble(inner_ab),
                dotMultiply(a, b),
                precision*precision*num_sample )
        );

        // test decryption
        signedByteArray[][] partial = new signedByteArray[key.getSkAll().length][];
        for(int j = 0; j < key.getSkAll().length; j++) {
            key.getSkAll()[j].setRank(j+1);
            partial[j] = key.decryptPartial(add_ab, key.getSkAll()[j]);
        }
        dec_helper.dec(add_ab);
        long [] add_ab_dec = key.decryptFinalLong(partial, add_ab, key.getSkAll()[0]);
        assert(
                listAnyPrecisionEqual(
                        add_ab_dec,
                        elementwiseMul(a, elementwiseMul(b, -1)),
                        precision*precision)
        );
    }

    public void test_special_numbers() {

        key.generateKeys();
        HomoEncryptionDebugUtil dec_helper = new HomoEncryptionDebugUtil(key.getPk(), key.getSkAll(), key.n, maxNegAbs);

        int N = 20;
        long [] a = new long[N];
        long [] b = new long[N];
        for(int i = 0; i< N; i++) {
            a[i] = prg.nextLong();
            b[i] = prg.nextLong();
        }
        utils.__printArray__(a);
        utils.__printArray__(b);

        signedByteArray[] a_enc = key.encryption(a, key.getPk());
        signedByteArray[] b_enc = key.encryption(b, key.getPk());

        signedByteArray[] add_ab = key.add(a_enc, b_enc, key.getPk());
        signedByteArray inner_ab = key.innerProduct(a_enc, b, key.getPk());

        long dec_inner_ab = dec_helper.dec(inner_ab);
        long[] dec_add_ab = dec_helper.dec(add_ab);

        assert(toJsons(dec_add_ab).equals(toJsons(add(a, b))));
        assert(toJsons(dec_inner_ab).equals(toJsons(dotMultiply(a, b))));
    }

    public static void main(String[] args) {
        TestDistPaillierStandalone newTest = new TestDistPaillierStandalone();
//        newTest.test_debug_util();
        newTest.testMore();
    }
}
