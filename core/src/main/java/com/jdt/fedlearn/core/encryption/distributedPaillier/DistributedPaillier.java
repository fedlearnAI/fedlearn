/* Copyright 2020 The FedLearn Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.jdt.fedlearn.core.encryption.distributedPaillier;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.core.encryption.nativeLibLoader;
import com.jdt.fedlearn.core.exception.SerializeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import static com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillierNative.*;


public class DistributedPaillier {
    private static final Logger logger = LoggerFactory.getLogger(DistributedPaillier.class);

    // float point arithmetic is not support yet. To do float number computation, we multiply
    // them by a scalar, i.e SCALE, to turn them into integers. SCALE=100 means precision is
    // rounded to 2 decimal places. E.g. 4.7654321 is rounded to 4.76.
    public static final long SCALE = 10000;

    public static long factorial(long n) {
        if (n == 0) {
            return 1;
        } else {
            return (n * factorial(n - 1));
        }
    }

    /**
     * Public Key
     */
    public static class DistPaillierPubkey {
        int bitLen; /* e.g., 1024 */
        int t;
        signedByteArray n; /* public modulus n = p q */

        public DistPaillierPubkey() {
        }

        public DistPaillierPubkey(String jsonStr) {
            parseJson(jsonStr);
        }

        public String toJson() {
            String jsonStr;
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                jsonStr = objectMapper.writeValueAsString(this);
            } catch (Exception e) {
                logger.error("Distributed Paillier pubkey to json", e);
                throw new SerializeException("Distributed Paillier pubkey to json");
            }
            return jsonStr;
        }

        public void parseJson(String jsonStr) {
            ObjectMapper mapper = new ObjectMapper();
            DistPaillierPubkey p3r;
            try {
                p3r = mapper.readValue(jsonStr, DistPaillierPubkey.class);
                this.n = p3r.n.deep_copy();
                this.bitLen = p3r.bitLen;
                this.t = p3r.t;
            } catch (IOException e) {
                logger.error("DistributedPaillier parseJson error: ", e);
            }
        }

        public int getT() {
            return t;
        }

        public void setT(int t) {
            this.t = t;
        }

        public signedByteArray getN() {
            return n;
        }

        public void setN(signedByteArray n) {
            this.n = n;
        }

        public void setBitLen(int bitLen) {
            this.bitLen = bitLen;
        }

        public void saveToFile(String path){
            try {
                Files.write(Paths.get(path), this.toJson().getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                logger.error("PubKey ioexception",e);
            }
        }

        public void loadClassFromFile(String path){
            try {
                String str = new String(Files.readAllBytes(Paths.get(path)),StandardCharsets.UTF_8) ;
                parseJson(str);
            } catch (IOException e) {
                logger.error("PubKey ioexception",e);
            }
        }
    }

    /**
     * Private Key
     */
    public static class DistPaillierPrivkey {
        int t; // dec threshold is 2 * t + 1
        int bitLen;
        int rank; // the rank of this key
        signedByteArray hi; // <lambda * beta>_(2t+1) evaluated at point i;
        signedByteArray n;
        signedByteArray thetaInvmod;
        long nFact;

        public DistPaillierPrivkey(byte[] hi,
                            boolean hiIsNeg,
                            byte[] n,
                            boolean n_isNeg,
                            byte[] thetaInvmod,
                            boolean thetaInvIsNeg) {
            this.hi = new signedByteArray();
            this.hi.byteArr = hi.clone();
            this.hi.isNeg = hiIsNeg;
            this.n = new signedByteArray();
            this.n.byteArr = n.clone();
            this.n.isNeg = n_isNeg;
            this.thetaInvmod = new signedByteArray();
            this.thetaInvmod.byteArr = thetaInvmod.clone();
            this.thetaInvmod.isNeg = thetaInvIsNeg;
        }

        public DistPaillierPrivkey(signedByteArray hi,
                            signedByteArray n,
                            signedByteArray thetaInvmod,
                            int rank,
                            int t,
                            int bitLen,
                            long nFact) {
            this.hi = hi.deep_copy();
            this.n = n.deep_copy();
            this.thetaInvmod = thetaInvmod.deep_copy();
            this.rank = rank;
            this.t = t;
            this.bitLen = bitLen;
            this.nFact = nFact;
        }

        public DistPaillierPrivkey() {
        }

        public DistPaillierPrivkey(String jsonStr) {
            parseJson(jsonStr);
        }

        public String toJson() {
            String jsonStr;
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                jsonStr = objectMapper.writeValueAsString(this);
            } catch (Exception e) {
                logger.error("Distributed Paillier priv to json: ", e);
                throw new SerializeException("Distributed Paillier priv to json");
            }
            return jsonStr;
        }

        public void parseJson(String jsonStr) {
            ObjectMapper mapper = new ObjectMapper();
            DistPaillierPrivkey p3r;
            try {
                p3r = mapper.readValue(jsonStr, DistPaillierPrivkey.class);
                this.t = p3r.t;
                this.bitLen = p3r.bitLen;
                this.hi = p3r.hi.deep_copy();
                this.n = p3r.n.deep_copy();
                this.thetaInvmod = p3r.thetaInvmod.deep_copy();
                this.nFact = p3r.nFact;
                this.rank = p3r.rank;
            } catch (IOException e) {
                logger.error("DistributedPaillier parseJson error: ", e);
                try {
                    throw new Exception("parse error");
                } catch (Exception exception) {
                    logger.error("parseJson error", exception);
                }
            }
        }

        public int getBitLen() {
            return bitLen;
        }

        public void setBitLen(int bitLen) {
            this.bitLen = bitLen;
        }

        public signedByteArray getHi() {
            return hi;
        }

        public void setHi(signedByteArray hi) {
            this.hi = hi;
        }

        public int getT() {
            return t;
        }

        public void setT(int t) {
            this.t = t;
        }

        public signedByteArray getN() {
            return n;
        }

        public void setN(signedByteArray n) {
            this.n = n;
        }

        public signedByteArray getThetaInvmod() {
            return thetaInvmod;
        }

        public void setThetaInvmod(signedByteArray thetaInvmod) {
            this.thetaInvmod = thetaInvmod;
        }

        public long getnFact() {
            return nFact;
        }

        public void setnFact(long nFact) {
            this.nFact = nFact;
        }

        public int getRank() {
            return rank;
        }

        public void setRank(int rank) {
            this.rank = rank;
        }

        public void saveToFile(String path){
            try {
                Files.write(Paths.get(path), this.toJson().getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                logger.error("PrivKey ioexception",e);
            }
        }

        public void loadClassFromFile(String path){
            try {
                String str = new String(Files.readAllBytes(Paths.get(path)),StandardCharsets.UTF_8) ;
                parseJson(str);
            } catch (IOException e) {
                logger.error("PrivKey ioexception",e);
            }
        }
    }

    /*
     * ===============================
     *  JAVA functions
     * ===============================
     */

    static {
        try {
//            System.loadLibrary("Distpaillier"); // debug nativeLibLoader 时用
            nativeLibLoader.load();
        } catch (UnsatisfiedLinkError e) {
            logger.error("library: " + System.getProperty("java.library.path"));
            logger.error("Native code library failed to load.  ", e);
            System.exit(1);
        }
    }

    /**
     * standalone 生成公钥私钥
     *
     * @param outPrivKeys: dist_paillier_privkey n
     * @param outPubkey    : generated public key
     * @param len          : bits of the key
     * @param t            : decryption threshold
     * @param n            : total number of parites
     */
    public static void genPrivpubKeysStandalone(DistPaillierPrivkey[] outPrivKeys,
                                                DistPaillierPubkey outPubkey,
                                                int len,
                                                int t,
                                                int n) {
        signedByteArray[] byteArray_lst = new signedByteArray[n * 3];
        for (int i = 0; i < byteArray_lst.length; i++) {
            byteArray_lst[i] = new signedByteArray();
        }
        __generate_privpub_key__(byteArray_lst, len, t, n, true);
        long n_fat = factorial(n);
        for (int i = 0; i < n; i++) {
            assert (outPrivKeys[i] != null);
            outPrivKeys[i] = new DistPaillierPrivkey(
                    byteArray_lst[3 * i].byteArr, byteArray_lst[3 * i].isNeg,
                    byteArray_lst[3 * i + 1].byteArr, byteArray_lst[3 * i + 1].isNeg,
                    byteArray_lst[3 * i + 2].byteArr, byteArray_lst[3 * i + 2].isNeg
            );
            outPrivKeys[i].nFact = n_fat;
            outPrivKeys[i].bitLen = len;
            outPrivKeys[i].t = t;
        }
        outPubkey.n = new signedByteArray();
        outPubkey.n.byteArr = outPrivKeys[0].n.byteArr;
        outPubkey.n.isNeg = outPrivKeys[0].n.isNeg;
        outPubkey.bitLen = len;
        outPubkey.t = t;
    }

    /*
     * ====================================
     *  Paillier Encryption / Decryption
     * ====================================
     */

    /**
     * Encryption of double
     *
     * @param plain:  a double
     * @param pubKey: public key
     * @return res: encrypted value of plain in signedByteArray form
     */
    public static signedByteArray encDouble(double plain, DistPaillierPubkey pubKey) {
        signedByteArray res = new signedByteArray();
        res.scale = SCALE;
        plain = plain * SCALE;
        __enc__(res, (long) plain, pubKey.bitLen, pubKey.n);
        return res;
    }

    public static signedByteArray[] encDoubleList(double[] plain, DistPaillierPubkey pub_key) {
        signedByteArray[] res = new signedByteArray[plain.length];
        long[] plain_long = new long[plain.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = new signedByteArray();
            res[i].scale = SCALE;
            plain_long[i] = (long) (plain[i] * SCALE);
        }
        __enc_list__(res, plain_long, pub_key.bitLen, pub_key.n);
        return res;
    }

    public static signedByteArray enc(long plain, DistPaillierPubkey pub_key) {
        signedByteArray res = new signedByteArray();
        __enc__(res, plain, pub_key.bitLen, pub_key.n);
        return res;
    }

    public static signedByteArray[] enc_list(long[] plain, DistPaillierPubkey pub_key) {
        signedByteArray[] res = new signedByteArray[plain.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = new signedByteArray();
        }
        __enc_list__(res, plain, pub_key.bitLen, pub_key.n);
        return res;
    }

    /**
     * Partial Decryption
     *
     * @param c:       ciphertext to be decrypted
     * @param privkey: private key
     * @return intermediate decryption result
     */
    public static signedByteArray decPartial(signedByteArray c,
                                             int party_id,
                                             DistPaillierPrivkey privkey) {
        signedByteArray res = new signedByteArray();
        __partial_dec__(res, c, privkey.bitLen, party_id, privkey.t, privkey.nFact, __get_privkey_as_input__(privkey));
        return res;
    }

    /**
     * Partial Decryption
     *
     * @param c:       ciphertext to be decrypted
     * @param privKey: private key
     * @param partyId: unique ID of this party, choosing from [1, n]
     * @return intermediate decryption result
     */
    public static signedByteArray[] decLstPartial(signedByteArray[] c, int partyId, DistPaillierPrivkey privKey) {
        signedByteArray[] res = new signedByteArray[c.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = new signedByteArray();
        }
        __partial_dec_lst__(res, c, privKey.bitLen, partyId, privKey.t, privKey.nFact, __get_privkey_as_input__(privKey));
        return res;
    }

    /**
     * Final Decryption
     *
     * @param im_res:      At least 2t+1 intermediate decryption results returned by decPartial()
     * @param cypher_text: 密文
     * @param max_neg_abs: The greatest value of abs(to_be_decrypted) when to_be_decrypted
     *                     is negative.
     *                     <p>
     *                     Some Notes about this parameter:
     *                     The primitive Paillier only supports non-negative value output because it takes mod.
     *                     Its final decrypted result is always between 0 and n. When decrypting a negative value
     *                     (its plaintext is negative), mod n is taken at the last step. A negative value, say x,
     *                     will become n+x in the final result.
     *                     Since that n is often a very large value, 2^1024, by carefully choosing
     *                     a "max_negative_abs", we can make n+x and normal positive results
     *                     very easy to distinguish.
     *                     For example, say we set the largest value no larger than 2^64 which is way too
     *                     less than  2^1024, if we happen to get a value between (2^1024-2^64, 2^1024), then it
     *                     should be a negative value.
     * @param privKey:     秘钥
     * @return 解密后的数，最大为 MAX_LONG, 最小为 -MAX_LONG
     */
    private static long decFinal(signedByteArray[] im_res,
                                 signedByteArray cypher_text,
                                 long max_neg_abs,
                                 DistPaillierPrivkey privKey) {
        assert (im_res.length >= 2 * privKey.t + 1);
        return __final_dec__(im_res, cypher_text, privKey.bitLen, privKey.t, privKey.nFact, max_neg_abs, __get_privkey_as_input__(privKey));
    }

    public static long decFinalLong(signedByteArray[] im_res,
                                    signedByteArray cypher_text,
                                    DistPaillierPrivkey priv_key) {
        long max_neg_abs = Long.MAX_VALUE; // maximum value of long in java
        long res = decFinal(im_res, cypher_text, max_neg_abs, priv_key);
        return res / cypher_text.scale;
    }

    public static double decFinalDouble(signedByteArray[] im_res,
                                        signedByteArray cypher_text,
                                        DistPaillierPrivkey priv_key) {
        long max_neg_abs = Long.MAX_VALUE; // maximum value of long in java
        long res = decFinal(im_res, cypher_text, max_neg_abs, priv_key);
        return (double) res / cypher_text.scale;
    }


    /**
     * do scaling when dealing with float data -- add
     *
     * @param a:   ciphertext adding number a
     * @param b:   ciphertext adding number b
     * @param res: ciphertext result
     */
    private static void __add_scale_helper__(signedByteArray a, signedByteArray b,
                                             signedByteArray res, DistPaillierPubkey pub_key) {
        long scale = Long.max(a.scale, b.scale);
        if (a.scale < scale) {
            long factor = scale / a.scale;
            assert (scale % a.scale == 0);
            __mul__(a, a, factor, pub_key.bitLen, pub_key.n);
        } else if (b.scale < scale) {
            long factor = scale / b.scale;
            assert (scale % b.scale == 0);
            __mul__(b, b, factor, pub_key.bitLen, pub_key.n);
        }
        res.scale = scale;
    }

    /**
     * do scaling when dealing with float data -- mul
     *
     * @param a:       ciphertext adding number a
     * @param res:     result
     * @param b_scale: the scaled long value
     */
    private static void __mul_scale_helper__(signedByteArray a, long b_scale, signedByteArray res) {
        res.scale = a.scale * b_scale;
    }

    /**
     * do scaling when dealing with float data -- inner product
     *
     * @param a   : adding number a
     * @param res : result
     */
    private static void __inner_prod_scale_helper__(signedByteArray[] a, long b_scale,
                                                    signedByteArray[] imres, signedByteArray res) {
        long scale = a[0].scale * b_scale;
        res.scale = scale;
        for (int i = 0; i < a.length; i++) {
            assert (a[0].scale == a[i].scale);
            imres[i].scale = scale;
        }
    }

    /**
     * Add
     *
     * @param a:       ciphertext
     * @param b:       ciphertext
     * @param pub_key: public key
     * @return ciphertext result
     */
    public static signedByteArray add(signedByteArray a,
                                      signedByteArray b,
                                      DistPaillierPubkey pub_key) {
        signedByteArray res = new signedByteArray();
        signedByteArray a_copy = a.deep_copy();
        signedByteArray b_copy = b.deep_copy();

        __add_scale_helper__(a_copy, b_copy, res, pub_key);

        __add__(res, a_copy, b_copy, pub_key.bitLen, pub_key.n);
        return res;
    }

    public static signedByteArray[] add(signedByteArray[] a,
                                        signedByteArray[] b,
                                        DistPaillierPubkey pub_key) {
        signedByteArray[] res = new signedByteArray[a.length];
        signedByteArray[] a_copy = new signedByteArray[a.length];
        signedByteArray[] b_copy = new signedByteArray[a.length];
        HomoEncryptionUtil.arrayCopy(a, 0, a_copy, 0, a.length);
        HomoEncryptionUtil.arrayCopy(b, 0, b_copy, 0, b.length);

        for (int i = 0; i < res.length; i++) {
            res[i] = new signedByteArray();
            __add_scale_helper__(a_copy[i], b_copy[i], res[i], pub_key);
        }
        __add_vec__(res, a_copy, b_copy, pub_key.bitLen, pub_key.n);
        return res;
    }

    /**
     * Mul
     *
     * @param a:       ciphertext
     * @param b:       plaintext
     * @param pub_key: public key
     * @return ciphertext result
     */
    public static signedByteArray mulLong(signedByteArray a,
                                          long b,
                                          DistPaillierPubkey pub_key) {
        signedByteArray res = new signedByteArray();
        __mul_scale_helper__(a, 1, res);
        __mul__(res, a, b, pub_key.bitLen, pub_key.n);
        return res;
    }

    public static signedByteArray mulDouble(signedByteArray a,
                                            double b,
                                            DistPaillierPubkey pub_key) {
        signedByteArray res = new signedByteArray();
        __mul_scale_helper__(a, SCALE, res);
        __mul__(res, a, toLong(b), pub_key.bitLen, pub_key.n);
        return res;
    }

    /**
     * do element-wise vector mul
     */
    public static signedByteArray[] mulLongElementwise(signedByteArray[] a,
                                                       long[] b,
                                                       DistPaillierPubkey pub_key) {
        assert (a.length == b.length);

        signedByteArray[] res = new signedByteArray[a.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = new signedByteArray();
            __mul_scale_helper__(a[i], 1, res[i]);
        }
        __mul_elementWise__(res, a, b, pub_key.bitLen, pub_key.n);
        return res;
    }

    public static signedByteArray[] mulDoubleElementwise(signedByteArray[] a,
                                                         double[] b,
                                                         DistPaillierPubkey pub_key) {
        assert (a.length == b.length);

        long[] b_long = toLong(b);

        signedByteArray[] res = new signedByteArray[a.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = new signedByteArray();
            __mul_scale_helper__(a[i], SCALE, res[i]);
        }
        __mul_elementWise__(res, a, b_long, pub_key.bitLen, pub_key.n);
        return res;
    }

    /**
     * Div (try not to use this method, DIV is not fully supported in Paillier)
     */
    @Deprecated
    public static signedByteArray divLong(signedByteArray a,
                                          long b,
                                          DistPaillierPubkey pub_key) {
        signedByteArray res = new signedByteArray();
        __div__(res, a, b, pub_key.bitLen, pub_key.n);
        return res;
    }

    /**
     * div_elementWise (try not to use this method, DIV is not fully supported in Paillier)
     */
    @Deprecated
    public static signedByteArray[] divLongElementwise(signedByteArray[] a,
                                                       long[] b,
                                                       DistPaillierPubkey pub_key) {
        assert (a.length == b.length);

        signedByteArray[] res = new signedByteArray[a.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = new signedByteArray();
        }
        __div_elementWise__(res, a, b, pub_key.bitLen, pub_key.n);
        return res;
    }

    /**
     * inner product *密文* 数组和 *明文* 数组的内积运算
     *
     * @param a: 密文数组
     * @param b: 明文数组
     * @return cypher text results
     */
    public static signedByteArray innerProductLong(signedByteArray[] a,
                                                   long[] b,
                                                   DistPaillierPubkey pub_key) {
        signedByteArray[] im_res = new signedByteArray[a.length];
        signedByteArray res = new signedByteArray();
        for (int i = 0; i < im_res.length; i++) {
            im_res[i] = new signedByteArray();
        }

        __inner_prod_scale_helper__(a, 1, im_res, res);

        __mul_elementWise__(im_res, a, b, pub_key.bitLen, pub_key.n);
        __sum_vec__(res, im_res, pub_key.bitLen, pub_key.n);
        return res;
    }

    public static signedByteArray innerProductDouble(signedByteArray[] a,
                                                     double[] b,
                                                     DistPaillierPubkey pub_key) {
        signedByteArray[] im_res = new signedByteArray[a.length];
        signedByteArray res = new signedByteArray();
        for (int i = 0; i < im_res.length; i++) {
            im_res[i] = new signedByteArray();
        }

        long[] b_long = toLong(b);
        __inner_prod_scale_helper__(a, SCALE, im_res, res);

        __mul_elementWise__(im_res, a, b_long, pub_key.bitLen, pub_key.n);
        __sum_vec__(res, im_res, pub_key.bitLen, pub_key.n);
        return res;
    }

    /*
     * ===============================
     *  Some Util functions
     * ===============================
     */

    public static signedByteArray[] __get_privkey_as_input__(final DistPaillierPrivkey priv_key) {
        signedByteArray[] privkey_as_input = new signedByteArray[3];
        privkey_as_input[0] = priv_key.hi;
        privkey_as_input[1] = priv_key.n;
        privkey_as_input[2] = priv_key.thetaInvmod;
        return privkey_as_input;
    }

    public static void __print_byteArray_lst__(signedByteArray[] byteArray_lst) {
        System.out.print("byteArray_lst len = " + byteArray_lst.length + "\n");
        for (signedByteArray ret : byteArray_lst) {
            for (int j = 0; j < ret.byteArr.length; j++) {
                System.out.print(ret.byteArr[j] + " ");
            }
            System.out.print(" #bytes used = " + ret.byteArr.length + "\n");
        }
    }

    public static void __print_byteArray__(signedByteArray byteArray) {

        for (int j = 0; j < byteArray.byteArr.length; j++) {
            System.out.print(byteArray.byteArr[j] + " ");
        }
        System.out.print(" isNeg = " + byteArray.isNeg + "\n");
    }


    private static long toLong(double in) {
        return (long) (in * SCALE);
    }

    private static long[] toLong(double[] in) {
        return Arrays.stream(in).mapToLong(DistributedPaillier::toLong).toArray();
    }
}

