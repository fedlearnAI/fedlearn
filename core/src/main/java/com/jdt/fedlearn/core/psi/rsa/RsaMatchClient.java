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

package com.jdt.fedlearn.core.psi.rsa;

import com.jdt.fedlearn.core.entity.base.EmptyMessage;
import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.core.psi.PrepareClient;
import com.jdt.fedlearn.core.type.data.Tuple2;
import com.jdt.fedlearn.core.util.BigIntegerUtil;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.jdt.fedlearn.core.entity.psi.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**　RSA 仅支持两方进行对齐
 * @author zhangwenxi
 */
public class RsaMatchClient implements PrepareClient {
    private static final Logger logger = LoggerFactory.getLogger(RsaMatchClient.class);
    private RSAPublicKey rsaPublicKey;
    private BigInteger N;
    private BigInteger e;
    private BigInteger d;
    private BigInteger blindFactor;
    private String hash1;
    private String hash2;
    private RSAPrivateKey rsaPrivateKey;
    private String[] commonIds = null;

    @Override
    public String[] getCommonIds() {
        return commonIds;
    }
    /**
     * 指定key的大小
     */
    private static final int KEYSIZE = 2048;
    public static final String KEY_ALGORITHM = "RSA";

    @Override
    public Message init(String[] uid, Map<String,Object> others) {
        hash1 = (String) others.get("hash1");
        hash2 = (String) others.get("hash2");
        try {
            generateRSAKey();
        } catch (Exception exception) {
            logger.error("initMatch error", exception);
        }
        byte[][] pk = new byte[2][];
//        BigInteger e = rsaPublicKey.getPublicExponent();
//        BigInteger N = rsaPublicKey.getModulus();
        e = rsaPublicKey.getPublicExponent();
        N = rsaPublicKey.getModulus();
        d = rsaPrivateKey.getPrivateExponent();
        pk[0] = BigIntegerUtil.bigIntegerToBytes(e, false);
        pk[1] = BigIntegerUtil.bigIntegerToBytes(N, false);
        return new MatchRSA1(pk);
    }

    private static BigInteger generateBlindingFactor(BigInteger N) {
        final BigInteger ZERO = BigInteger.valueOf(0);
        final BigInteger ONE = BigInteger.valueOf(1);
        int length = N.bitLength() - 1;
        BigInteger gcd;
        BigInteger blindFactor;
        do {
            blindFactor = new BigInteger(length, new SecureRandom());
            gcd = blindFactor.gcd(N);
        } while (blindFactor.equals(ZERO) || blindFactor.equals(ONE) || !gcd.equals(ONE));
        return blindFactor;
    }

    @Override
    public Message client(int phase, Message parameterData, String[] trainData) {
        if (phase == 1) {
            return computeClientFirstLayer(parameterData, trainData);
        }
        if (phase == 2) {
            return computeClientSecondLayer(parameterData, trainData);
        }
        if (phase == 3) {
            return computeClientIntersection(parameterData, trainData);
        }
        if (phase == 4) {
            if (!(parameterData instanceof MatchTransit)) {
                throw new UnsupportedOperationException("RSA Phase 4 should be instance of MatchTransit");
            }
            // 结束了之后收取由master发过来的commonIds并赋值全局变量
            commonIds = ((MatchTransit)parameterData).getIds().values().toArray(new String[0]);
            return computeMasterIntersection(parameterData, trainData);
        }
        return EmptyMessage.message();
    }


    private void generateRSAKey() throws Exception {
        /* RSA算法要求有一个可信任的随机数源 */
        SecureRandom sr = new SecureRandom();
        /* 为RSA算法创建一个KeyPairGenerator对象 */
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(KEY_ALGORITHM);
        /* 利用上面的随机数据源初始化这个KeyPairGenerator对象 */
        kpg.initialize(KEYSIZE, sr);
        /* 生成密匙对 */
        KeyPair kp = kpg.generateKeyPair();
        /* 得到公钥 */
        rsaPublicKey = (RSAPublicKey) kp.getPublic();
        /* rsaPrivateCrtKey = (RSAPrivateCrtKey) kp.getPrivate(); */
        rsaPrivateKey = (RSAPrivateKey) kp.getPrivate();
    }

    /**
     * phase1: RSA clients, compute first layer ids
     *
     * @return first layer ids from RSA clients
     */
    private Message computeClientFirstLayer(Message initValues, String[] uid) {
        logger.info("compute uid " + Arrays.toString(uid));
        MatchRSA1 reqRSA = (MatchRSA1)(initValues);
        // update RSA pk pair
        byte[][] pk = reqRSA.getPk();
        e = BigIntegerUtil.bytesToBigInteger(pk[0], 0, pk[0].length);
        N = BigIntegerUtil.bytesToBigInteger(pk[1], 0, pk[1].length);

        try {
            rsaPublicKey = getPublicKey(N, e);
        } catch (Exception exception) {
            logger.error("computeClientFirstLayer error", exception);
        }
        // generate blinding factor
        blindFactor = generateBlindingFactor(N);
//        BigInteger factor = blindFactor.modPow(e, N);
        BigInteger factor = BigInteger.valueOf(1);

        // compute local first layer encryption
        BigInteger[] fdh = Arrays.stream(uid).parallel()
                .map(id -> fullDomainHash(hash1, id))
                .toArray(BigInteger[]::new);

        BigInteger[] multiplyRes = encryptor(fdh, factor, N);
//        byte[][] multiplyRes = Arrays.stream(fdh).parallel()
//        .map(x -> x.multiply(factor).mod(N))
//        .map(bI -> BigIntegerUtil.bigIntegerToBytes(bI, false))
//        .toArray(byte[][]::new);
        String[] encId = Arrays.stream(multiplyRes).map(BigInteger::toString).toArray(String[]::new);
        return new MatchRSA2(encId);
    }

    /**
     * phase2: RSA server, compute second layer ids(signed layer) for ids from RSA clients
     * compute signed ids of local ids
     *
     * @return second layer ids for ids from RSA clients, signed ids of RSA server
     */
    private Message computeClientSecondLayer(Message initValues, String[] uid) {
        MatchRSA2 reqRSA = (MatchRSA2) initValues;
        // get RSA encId by pk as the first layer
        String[] encIdBytes = reqRSA.getEncId();
        BigInteger[] encId = Arrays.stream(encIdBytes).map(BigInteger::new).toArray(BigInteger[]::new);
        // signed layer
        BigInteger[] signId = signer(encId, d, N);

        // server local signed fdh
        BigInteger[] firstFdh = Arrays.stream(uid).parallel().map(id -> fullDomainHash(hash1, id)).toArray(BigInteger[]::new);

        BigInteger[] signFdh = signer(firstFdh, d, N);

        byte[][] signFdhBytes = BigIntegerUtil.bigIntegerToBytes(signFdh, false);
        String[] signSecondFdh = Arrays.stream(signFdhBytes).map(x -> fullDomainHashToBigInteger(hash2, x).toString()).toArray(String[]::new);

        MatchRSA3 res = new MatchRSA3(Arrays.stream(signId).map(x -> x.toString()).toArray(String[]::new), signSecondFdh, reqRSA.getClientInfo());
        return res;
    }

    /**
     * phase3: RSA clients, compute intersection set
     *
     * @return intersection set of signed ids of RSA server
     */
    private Message computeClientIntersection(Message initValues, String[] uid) {
        MatchRSA3 reqRSA3 = (MatchRSA3)(initValues);
        String[] signIds = reqRSA3.getSignId();
        String[] serverIds = reqRSA3.getSvId();

//        BigInteger factor = blindFactor.modPow(e, N);
        BigInteger factor = BigInteger.valueOf(1);

        byte[][] signIdDivideFactorBytes = divide(signIds, factor);
        String[] clientIds = Arrays.stream(signIdDivideFactorBytes).map(id -> fullDomainHashToString(hash2, id)).toArray(String[]::new);

        String[] interSet = intersection(clientIds, serverIds, uid);
        MatchResRSA4 res = new MatchResRSA4(interSet);
        return res;
    }

    /**
     * phase4: RSA server, receive and decrypt intersection set
     *
     * @return intersection set of ids
     */
    private Message computeMasterIntersection(Message initValues, String[] uid) {
        return EmptyMessage.message();
    }

    private static BigInteger fullDomainHash(String hash, String str) {
        byte[] res = fullDomainHash(hash, str.getBytes(StandardCharsets.UTF_8));
        return BigIntegerUtil.bytesToBigInteger(res, 0, res.length);
    }

    private static String fullDomainHashToString(String hash, byte[] str) {
        byte[] res = fullDomainHash(hash, str);
        return BigIntegerUtil.bytesToBigInteger(res, 0, res.length).toString();
    }

    private static BigInteger fullDomainHashToBigInteger(String hash, byte[] str) {
        byte[] res = fullDomainHash(hash, str);
        return BigIntegerUtil.bytesToBigInteger(res, 0, res.length);
    }

    private static byte[] fullDomainHash(String hash, byte[] str) {
        try {
            MessageDigest md = MessageDigest.getInstance(hash);
            //使用指定的字节来更新摘要
            md.update(str);
            //获取密文  （完成摘要计算）
            byte[] b2 = md.digest();

            //16进制字符串
            String add = "0123456789abcdef";
            //把字符串转为字符串数组
            char[] ch = add.toCharArray();
            //获取计算的长度
            int len = b2.length;
            //创建一个 2*len 长度的字节数组
            char[] chs = new char[len * 2];
            //循环 len 次
            for (int i = 0, k = 0; i < len; i++) {
                //获取摘要计算后的字节数组中的每个字节
                byte b3 = b2[i];
                // >>>:无符号右移
                // &:按位与
                //0xf:0-15的数字
                chs[k++] = ch[b3 >>> 4 & 0xf];
                chs[k++] = ch[b3 & 0xf];
            }
            //字符数组转为字符串bytesToBigInteger
            String res = new String(chs);
            return res.getBytes(StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException e) {
            logger.error("fullDomainHash error", e);
            return new byte[0];
        }
    }

    public static String[] intersection(String[] mixed1, String[] mixed2, String[] origin) {
        if (mixed1 == null || mixed2 == null) {
            return null;
        }
        assert mixed1.length == origin.length;
        List<Tuple2<String, Integer>> mixedList = IntStream.range(0, mixed1.length).parallel().boxed()
                .map(i -> new Tuple2<>(mixed1[i], i)).collect(Collectors.toList());
        List<Tuple2<String, Integer>> uidList = IntStream.range(0, mixed2.length).parallel().boxed()
                .map(i -> new Tuple2<>(mixed2[i], i)).collect(Collectors.toList());
        mixedList.sort(Comparator.comparing(Tuple2::_1));
        uidList.sort(Comparator.comparing(Tuple2::_1));
        List<String> res = new ArrayList<>();
        int i = 0, j = 0;
        String m = "";
        String u = "";

        while (i < mixedList.size() && j < uidList.size()) {
            m = mixedList.get(i)._1();
            u = uidList.get(j)._1();
            if (m == null) {
                i++;
                continue;
            }
            if (u == null) {
                j++;
                continue;
            }
            if (m.equals(u)) {
                res.add(origin[mixedList.get(i)._2()]);
                i++;
                j++;
            } else if (m.compareTo(u) < 0) {
                i++;
            } else {
                j++;
            }
        }
        return res.toArray(new String[0]);
    }

    private static String[] signerToString(BigInteger[] ids, BigInteger d, BigInteger N) {
        return Arrays.stream(ids).parallel().map(id -> id.modPow(d, N)).map(BigInteger::toString).toArray(String[]::new);
    }

    private static BigInteger[] signer(BigInteger[] ids, BigInteger d, BigInteger N) {
        return Arrays.stream(ids).parallel().map(id -> id.modPow(d, N)).toArray(BigInteger[]::new);
    }

    private static BigInteger[] encryptor(BigInteger[] fdh, BigInteger factor, BigInteger N) {
        return Arrays.stream(fdh).parallel()
                .map(x -> x.multiply(factor).mod(N))
                .toArray(BigInteger[]::new);
    }

//    private static String[] encryptor(BigInteger[] fdh, BigInteger factor, BigInteger N) {
//        return Arrays.stream(fdh).map(x -> x.multiply(factor).mod(N)).map(BigInteger::toString).toArray(String[]::new);
//    }

//    private static byte[][] divide(byte[][] secondLayer, BigInteger factor) {
//        BigInteger[] res = Arrays.stream(secondLayer).map(x -> BigIntegerUtil.bytesToBigInteger(x, 0, x.length))
//                .map(x -> x.divide(factor)).toArray(BigInteger[]::new);
//        return BigIntegerUtil.bigIntegerToBytes(res, false);
//    }

    private static byte[][] divide(String[] secondLayer, BigInteger factor) {
        BigInteger[] res = Arrays.stream(secondLayer).map(BigInteger::new)
                .map(x -> x.divide(factor)).toArray(BigInteger[]::new);
        return BigIntegerUtil.bigIntegerToBytes(res, false);
    }

    public static RSAPublicKey getPublicKey(BigInteger N, BigInteger e) throws Exception {
        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(N, e);
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
        return (RSAPublicKey) keyFactory.generatePublic(keySpec);
    }
}
