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

import com.jdt.fedlearn.core.entity.Message;

/**
 * Java interface for native code
 */
public class DistributedPaillierNative  {
    /*
     * ===============================
     *  JAVA-side data structures
     * ===============================
     */

    /**
     * 密文数据存储单元，一个 signedByteArray 是 一个数
     *
     * Params:
     *  byteArr: 无符号的 byte array (虽然Java的byte为带符号类型，此处将符号位也看做数据位的一部分)
     *  isNeg: 这个数据的符号
     */
    public static class signedByteArray implements Message{
        public byte[] byteArr;
        public boolean isNeg;
        public long scale;

        signedByteArray() {
            this.byteArr = null;
            this.isNeg = false;
            this.scale = 1;
        }

        signedByteArray(byte[] in, boolean isNeg) {
            this.byteArr = in;
            this.isNeg = isNeg;
            this.scale = 1;
        }

        signedByteArray(byte[] in, boolean isNeg, long scale) {
            this.byteArr = in;
            this.isNeg = isNeg;
            this.scale = scale;
        }

        /**
         * 生成当前对象的深拷贝
         * @return signed_byteArray[]
         */
        public signedByteArray deep_copy() {
            return new signedByteArray(byteArr.clone(), isNeg, scale);
        }
    }

    /*
     * ===============================
     *  C/CPP native functions
     * ===============================
     */

    /**
     * 秘钥生成时进行的秘密分享(secret sharing), 生成各方的shares
     *
     * @param out:
     * @param secret secret for sharing
     * @param t : 2*t+1 是解密最少需要的参与方数量
     * @param n : 参与解密的party数量
     */
    public static native void __create_share__(signedByteArray[] out, byte[] secret, int t, int n);

    /**
     * 生成各方秘钥
     * debug version: 模拟没有trust dealer的情况。假设p,q,N由trust dealer获得。
     * hi = (lambda * beta)_(2t+1) evaluated at point i, i.e.
     * sum(pi_share)*sum(qi_share)*beta
     *
     * @param len: 秘钥长度 i.e. 1024
     * @param t: 参与解密的party数量
     * @param out: 各方秘钥
     */
    public static native void __generate_privpub_key__(signedByteArray[] out,
                                                       int len,
                                                       int t,
                                                       int n,
                                                       boolean is_dealer);

    /**
     * paillier enc
     *
     * @param pubkey: signed_byteArray, n;
     * @param bit_len: 秘钥长度
     *
     * 注意: 当前仅支持 JAVA long, 即数据范围为 -+2^63
     * FIXME: 加入BigInteger支持
     */
    public static native void __enc__(signedByteArray out,
                                      long in,
                                      int bit_len,
                                      signedByteArray pubkey);

    public static native void __enc_list__(signedByteArray[] out,
                                           long[] in,
                                           int bit_len,
                                           signedByteArray pubkey);

    /**
     * paillier local decryption
     *
     * @param privkey: 长度为3的signed_byteArray[], 分别是hi, n, theta_InvMod
     * @param bit_len: 秘钥长度
     *
     * 注意: 当前仅支持 JAVA long, 即数据范围为 -+2^63
     * FIXME: 加入BigInteger支持
     */
    public static native void __partial_dec__(signedByteArray out,
                                              signedByteArray in,
                                              int bit_len,
                                              int party_id,
                                              int t,
                                              long n_fact,
                                              signedByteArray[] privkey);

    public static native void __partial_dec_lst__(signedByteArray[] out,
                                                  signedByteArray[] in,
                                                  int bit_len,
                                                  int party_id,
                                                  int t,
                                                  long n_fact,
                                                  signedByteArray[] privkey);

    /**
     * paillier final decryption
     *
     * @param im_res: 各方的中间解密结果.
     * @param max_neg_abs:
     * 注意: 当前仅支持 JAVA long, 即数据范围为 -+2^63
     * FIXME: 加入BigInteger支持
     */
    public static native long __final_dec__(signedByteArray[] im_res,
                                            signedByteArray cypher_text,
                                            int bit_len,
                                            int t,
                                            long n_fact,
                                            long max_neg_abs,
                                            signedByteArray[] privkey);

    /**
     * paillier add
     */
    public static native void __add__(signedByteArray out,
                                      signedByteArray a,
                                      signedByteArray b,
                                      int bit_len,
                                      signedByteArray pubkey);

    public static native void __add_vec__(signedByteArray[] out,
                                          signedByteArray[] a,
                                          signedByteArray[] b,
                                          int bit_len,
                                          signedByteArray pubkey);

    public static native void __sum_vec__(signedByteArray out,
                                          signedByteArray[] a,
                                          int bit_len,
                                          signedByteArray pubkey);

    /**
     * paillier mult
     */
    public static native void __mul__(signedByteArray out,
                                      signedByteArray a,
                                      long b,
                                      int bit_len,
                                      signedByteArray pubkey);

    public static native void __mul_elementWise__(signedByteArray[] out,
                                                  signedByteArray[] a,
                                                  long[] b,
                                                  int bit_len,
                                                  signedByteArray pubkey);

    /**
     * paillier div
     */
    public static native void __div__(signedByteArray out,
                                      signedByteArray a,
                                      long b,
                                      int bit_len,
                                      signedByteArray pubkey);

    public static native void __div_elementWise__(signedByteArray[] out,
                                                  signedByteArray[] a,
                                                  long[] b,
                                                  int bit_len,
                                                  signedByteArray pubkey);

    /**
     * only for debug and test
     */
    public static native void test_generate_priv_key(signedByteArray out,
                                                     int len,
                                                     int t,
                                                     int n,
                                                     int plain_text_num);

    public static native void test_IO(signedByteArray out,
                                      signedByteArray in);

    public static native void test_IO(signedByteArray[] out,
                                      signedByteArray[] in);

}
