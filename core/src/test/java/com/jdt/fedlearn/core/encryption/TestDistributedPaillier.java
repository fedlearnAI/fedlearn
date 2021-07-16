package com.jdt.fedlearn.core.encryption;

import com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillierNative;
import com.jdt.fedlearn.core.encryption.distributedPaillier.HomoEncryptionDebugUtil;
import com.jdt.fedlearn.core.encryption.distributedPaillier.HomoEncryptionUtil;
import com.jdt.fedlearn.core.exception.WrongValueException;
import com.jdt.fedlearn.core.math.MathExt;
import org.apache.commons.lang3.SystemUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Random;

import static com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillier.SCALE;
import static com.jdt.fedlearn.core.encryption.distributedPaillier.HomoEncryptionUtil.*;
import static com.jdt.fedlearn.core.math.MathExt.*;
import static com.jdt.fedlearn.core.util.TypeConvUtils.toJsons;

public class TestDistributedPaillier {

    int n = 3;
    int l = 1024;
    long maxNegAbs = Long.MAX_VALUE;

    private final Random prg = new Random();
    HomoEncryptionUtil key ;
    private boolean usingFakeEnc;
    private final String osName = SystemUtils.OS_NAME;

    @BeforeMethod
    private void setUp() {
        usingFakeEnc = !osName.equals("Linux");
        // if OS is not Linux, do not run this test.
        if(usingFakeEnc) {
            return ;
        }
        key = new HomoEncryptionUtil(n, l, usingFakeEnc);
    }


    static class utils {
        public static void __printArray__(long[] inputArray ) {
            System.out.print("list len = " + inputArray.length + " ");
            for ( long element : inputArray ){
                System.out.print( element + " " );
            }
        }
    }

    public void testMoreDouble() {

        for(int i = 0; i < 1000; i++) {
            testDoubleType();
            System.out.println("passed samples " + (i+1)*20);
        }
    }

    public void testMoreLong() {

        for(int i = 0; i < 1000; i++) {
            testLongType();
            System.out.println("passed samples " + (i+1)*20);
        }
    }

    private void initKeyByUtil(HomoEncryptionDebugUtil dec_helper){
        this.key.setPk(dec_helper.pk);
        this.key.setSkAll(dec_helper.sk_lst);
    }

    @Test
    public void testLongType() {
        if(usingFakeEnc) {
            return ;
        }
        int numSample = 20;
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
        HomoEncryptionDebugUtil decHelper;
        if(usingFakeEnc){
            decHelper = new HomoEncryptionDebugUtil(true) ;
        } else {
            decHelper = new HomoEncryptionDebugUtil(key.getPk(), key.getSkAll(), key.getN(), maxNegAbs);
        }
        // enc list
        DistributedPaillierNative.signedByteArray[] a_enc = key.encryption(a, key.getPk());
        DistributedPaillierNative.signedByteArray[] b_enc = key.encryption(b, key.getPk());

        // enc one by one
        DistributedPaillierNative.signedByteArray[] a_enc_1 = new DistributedPaillierNative.signedByteArray[numSample];
        DistributedPaillierNative.signedByteArray[] b_enc_1 = new DistributedPaillierNative.signedByteArray[numSample];

        for(int i = 0; i < numSample; i++) {
            a_enc_1[i] = key.encryption(a[i], key.getPk());
            b_enc_1[i] = key.encryption(b[i], key.getPk());

            // 解密后一定相同
            assert(decHelper.dec(a_enc_1[i]) == decHelper.dec(a_enc[i]));
            assert(decHelper.dec(a_enc_1[i]) == a[i]);
            assert(decHelper.dec(b_enc_1[i]) == decHelper.dec(b_enc[i]));
            assert(decHelper.dec(b_enc_1[i]) == b[i]);
        }

        // add
        DistributedPaillierNative.signedByteArray[] add_ab = key.add(a_enc, b_enc, key.getPk());
        DistributedPaillierNative.signedByteArray[] add_ab_1 = key.add(a_enc_1, b_enc_1, key.getPk());
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
        assert(toJsons(decHelper.dec(add_ab)).equals(toJsons(elementwiseMul(a, b) )));

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
        DistributedPaillierNative.signedByteArray inner_ab = key.innerProduct(a_enc, b, key.getPk());
        DistributedPaillierNative.signedByteArray inner_ab_1 = key.innerProduct(a_enc_1, b, key.getPk());
        assert(toJsons(decHelper.dec(inner_ab_1)).equals(toJsons(decHelper.dec(inner_ab))));
        assert(toJsons(decHelper.dec(inner_ab_1)).equals(toJsons( dotMultiply(a, b) )));

//       div positive -- 能整除 ( a*b / b ), b is positive
        long [] a_div = elementwiseMul(a, b);
        DistributedPaillierNative.signedByteArray[] a_div_enc = key.encryption(a_div, key.getPk());
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

        // test decryption
        DistributedPaillierNative.signedByteArray[][] partial = new DistributedPaillierNative.signedByteArray[key.getSkAll().length][];
        for(int j = 0; j < key.getSkAll().length; j++) {
            key.getSkAll()[j].setRank(j+1);
            partial[j] = key.decryptPartial(add_ab, key.getSkAll()[j]);
        }
        decHelper.dec(add_ab);
        long [] add_ab_dec = key.decryptFinalLong(partial, add_ab, key.getSkAll()[0]);
        assert (toJsons(add_ab_dec).equals(toJsons( elementwiseMul(a, neg_one) )));
    }

    @Test
    public void testDoubleType() {
        if(usingFakeEnc) {
            return ;
        }
        int num_sample = 20;
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
        HomoEncryptionDebugUtil decHelper;
        if(usingFakeEnc){
            decHelper = new HomoEncryptionDebugUtil(true) ;
        } else {
            decHelper = new HomoEncryptionDebugUtil(key.getPk(), key.getSkAll(), key.getN(), maxNegAbs);
        }

        // enc list
        DistributedPaillierNative.signedByteArray[] a_enc = key.encryption(a, key.getPk());
        DistributedPaillierNative.signedByteArray[] b_enc = key.encryption(b, key.getPk());
        // add
        DistributedPaillierNative.signedByteArray[] add_ab = key.add(a_enc, b_enc, key.getPk());
        assert( doubleLstEqual(decHelper.decDouble(add_ab),  MathExt.add(a, b) ));
        // sub
        long [] neg_one = new long[b_enc.length];
        Arrays.fill(neg_one, -1);
        add_ab = key.add(a_enc, key.mul(b_enc, neg_one, key.getPk()), key.getPk());
        assert( doubleLstEqual(decHelper.decDouble(add_ab),  sub(a, b) ));
        // mul positive
        add_ab = key.mul(a_enc, b, key.getPk());
        assert(
                listAnyPrecisionEqual(
                        decHelper.decDouble(add_ab),
                        elementwiseMul(a, b) , precision*precision)
        );
        // mul  negative
        add_ab = key.mul(a_enc, elementwiseMul(b, -1d), key.getPk());
        assert(
                listAnyPrecisionEqual(
                        decHelper.decDouble(add_ab),
                        elementwiseMul(a, elementwiseMul(b, -1)),
                        precision*precision)
        );
        // inner product
        DistributedPaillierNative.signedByteArray inner_ab = key.innerProduct(a_enc, b, key.getPk());
        assert( anyPrecisionEqual(
                decHelper.decDouble(inner_ab),
                dotMultiply(a, b),
                precision*precision*num_sample )
        );
        // test decryption
        DistributedPaillierNative.signedByteArray[][] partial = new DistributedPaillierNative.signedByteArray[key.getSkAll().length][];
        for(int j = 0; j < key.getSkAll().length; j++) {
            key.getSkAll()[j].setRank(j+1);
            partial[j] = key.decryptPartial(add_ab, key.getSkAll()[j]);
        }
        decHelper.dec(add_ab);
        long [] add_ab_dec = key.decryptFinalLong(partial, add_ab, key.getSkAll()[0]);
        assert(
                listAnyPrecisionEqual(
                        add_ab_dec,
                        elementwiseMul(a, elementwiseMul(b, -1)),
                        precision*precision)
        );
    }

    // when results larger than 2^64, will raise error.
    public void test_special_numbers() {
        key.generateKeys();
        HomoEncryptionDebugUtil decHelper = new HomoEncryptionDebugUtil(key.getPk(), key.getSkAll(), key.getN(), maxNegAbs);
        int N = 20;
        long [] a = new long[N];
        long [] b = new long[N];
        for(int i = 0; i< N; i++) {
            a[i] = prg.nextLong();
            b[i] = prg.nextLong();
        }
        utils.__printArray__(a);
        utils.__printArray__(b);
        DistributedPaillierNative.signedByteArray[] a_enc = key.encryption(a, key.getPk());
        DistributedPaillierNative.signedByteArray[] b_enc = key.encryption(b, key.getPk());
        DistributedPaillierNative.signedByteArray[] add_ab = key.add(a_enc, b_enc, key.getPk());
        DistributedPaillierNative.signedByteArray inner_ab = key.innerProduct(a_enc, b, key.getPk());
        long dec_inner_ab = decHelper.dec(inner_ab);
        long[] dec_add_ab = decHelper.dec(add_ab);
        assert(toJsons(dec_add_ab).equals(toJsons(add(a, b))));
        assert(toJsons(dec_inner_ab).equals(toJsons(dotMultiply(a, b))));
    }

}
