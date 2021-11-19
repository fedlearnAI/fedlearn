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

package com.jdt.fedlearn.core.psi.freedman;

import com.jdt.fedlearn.core.encryption.common.Ciphertext;
import com.jdt.fedlearn.core.encryption.common.EncryptionTool;
import com.jdt.fedlearn.core.encryption.common.PrivateKey;
import com.jdt.fedlearn.core.encryption.common.PublicKey;
import com.jdt.fedlearn.core.encryption.javallier.JavallierTool;
import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.core.entity.base.EmptyMessage;
import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.core.entity.psi.*;
import com.jdt.fedlearn.core.math.MathExt;
import com.jdt.fedlearn.core.psi.PrepareClient;
import com.jdt.fedlearn.core.psi.md5.Md5Match;
import com.jdt.fedlearn.core.type.FreedmanType;
import com.jdt.fedlearn.core.util.LagrangeInterpolation;
import org.apache.commons.lang3.ArrayUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

/**
 * 基于Freedman协议的ID对齐Client端算法, 当前仅支持数字类型id，如"124545"；不支持字符类型，如"jd1232_dsf";且当前算法要求各方ID相乘不能超过Double的最大值；
 * 基于 Freedman 协议的 ID 对齐算法一共分为五个步骤
 * <p>1：初始化阶段</p>
 * <p>各个client对各自需要对齐的 ID 长度加上一个小的随机数用于模糊真实长度。</p>
 * <p>2：主动方求解系数阶段</p>
 * <p>主动方收到master请求后使用拉格朗日插值法经过化解后求解得到多项式系数ß<sub>0</sub>, ß<sub>1</sub>, ..., ß<sub>n</sub>，使用Paillier加密算法对各个系数进行加密后，将加密后系数和公钥发送给master</p>
 * <p>3：非主动方计算阶段</p>
 * <p>各个非主动收到加密系数及公钥后，生成一个随机数 r，并对本地的每一个 ID y<sub>i</sub>计算 r * f(y<sub>i</sub>) + y<sub>i</sub>，在所有本地 ID 的多项式计算结束后，将结果发送回master</p>
 * <p>4：主动方对齐及 ID 储存阶段</p>
 * <p>主动方解密后进行对齐，将对齐好的结果储存在本地客户端后，将其余各方对应的对齐好的索引发回给master。</p>
 * <p>5：非主动方 ID 储存阶段</p>
 * <p>非主动方根据索引找到对应的原始 ID 之后，将对齐好的 ID 储存在本地客户端。</p>
 * @author lijingxi
 */
public class FreedmanMatchClient implements PrepareClient {
    private final Random r = new Random();
    private String[] commonIds;
    private EncryptionTool encryptionTool = new JavallierTool();// new FakeTool();//new PaillierTool();//new JavallierTool();
    private PublicKey publicKey; // 主动方会生成publicKey
    private PrivateKey privateKey; // 主动方会生成privateKey
    private double random;

    @Override
    public String[] getCommonIds() {
        return commonIds;
    }

    public FreedmanMatchClient() {
    }

    public FreedmanMatchClient(EncryptionTool encryptionTool, PublicKey publicKey, PrivateKey privateKey) {
        this.encryptionTool = encryptionTool;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    public void setEncryptionTool(EncryptionTool encryptionTool) {
        this.encryptionTool = encryptionTool;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    @Override
    public Message init(String[] uid, Map<String, Object> others) {
        int randInt = r.nextInt(30); // 加一个小的随机数 模糊真实长度
        int uidLength = uid.length + randInt;
        return new MatchInitRes(null, uidLength);
    }


    public Message client(int phase, Message message, String[] uid) {
        if (phase == FreedmanType.SolvePolynomial.getPhase()) {
            return solvePolynomial(message, uid);
        } if (phase == FreedmanType.CalculatePassivePolynomial.getPhase()) {
            return passiveOperatePolynomial(message, uid);
        } if (phase == FreedmanType.Match.getPhase()) {
            return activeMatch(message, uid);
        } if (phase == FreedmanType.Distribute.getPhase()) {
            return saveResult(message, uid);
        }
        return EmptyMessage.message();
    }

    /**
     * 主动方操作
     * @param message
     * @param uid
     * @return
     */
    private Message solvePolynomial(Message message, String[] uid) {
        if (!(message instanceof EmptyMessage)) {
            throw new UnsupportedOperationException("Freedman id match client phase 1 should be of empty message");
        }
        double[] uidInt = convertUid(uid);
        boolean ContinueOrNot = checkUid(uidInt);
        if (!ContinueOrNot) {
            throw new UnsupportedOperationException("ID exceeds maximum capacity for Freedman.");
        }
        double[] lagrangeInput = Arrays.stream(uidInt).map(i -> i * (-1.0)).toArray();
        LagrangeInterpolation lagrangeInterpolation = new LagrangeInterpolation(lagrangeInput);
        double[] coeffcients = lagrangeInterpolation.generateBigCoefficients();
        if (coeffcients.length != uid.length + 1) {
            throw new UnsupportedOperationException("Error from lagrange interpolation");
        }
        this.privateKey = encryptionTool.keyGenerate(1024, 64);
        this.publicKey = privateKey.generatePublicKey();
        // 随机生成一个大于最大的uid的数字

        this.random = MathExt.max(uidInt) + (r.nextInt(Integer.MAX_VALUE));
        // 加密后的多项式系数
        String[] encryptedCoefs = Arrays.stream(coeffcients).parallel().mapToObj(i -> encryptionTool.encrypt(i, publicKey))
                .map(Ciphertext::serialize).collect(toList()).toArray(new String[coeffcients.length]);
        String strPubKey = publicKey.serialize();
        return new FreedmanEncryption(encryptedCoefs, strPubKey);
    }

    /**
     * 非主动方操作，计算r * f(x<sub>i</sub>) + x<sub>i</sub>
     * @param message
     * @param uid 本地uid，需为数字，不可为字符
     * @return
     */
    private Message passiveOperatePolynomial(Message message, String[] uid) {
        if (!(message instanceof FreedmanEncryption)) {
            throw new UnsupportedOperationException("client phase 2 should be of FreedmanEncryption type");
        }
        FreedmanEncryption freedmanRes1 = (FreedmanEncryption) message;
        List<Ciphertext> encryptedCoefficients = Arrays.stream(freedmanRes1.getEncryptedCoefficients())
                .map(encryptionTool::restoreCiphertext).collect(Collectors.toList());
        String strPublicKey = freedmanRes1.getPublicKey();
        PublicKey publicKey = encryptionTool.restorePublicKey(strPublicKey);
        double[] uidInt = convertUid(uid);
        String[] passiveRes = Arrays.stream(uidInt).parallel().mapToObj(i -> encryptionTool.add(
                encryptionTool.multiply(polynomialCalculation(encryptedCoefficients, (int) Math.round(i), encryptionTool, publicKey), random, publicKey),
                encryptionTool.encrypt(i, publicKey), publicKey).serialize()).collect(toList()).toArray(new String[uidInt.length]);
        //todo 打乱顺序之后发送
        return new FreedmanPassiveResult(passiveRes);
    }

    /**
     * 主动方接收并进行ID对齐操作，并给各个客户端返回其可对齐ID的索引
     * @param message
     * @param uid
     * @return
     */
    private Message activeMatch(Message message, String[] uid) {
        if (!(message instanceof FreedmanPassiveUidMap)) {
            throw new UnsupportedOperationException("client phase 3 should be of FreedmanPassiveUidMap type");
        }
        FreedmanPassiveUidMap freedmanPassiveUidMap = (FreedmanPassiveUidMap) message;
        double[] uidInt = convertUid(uid);
        Map<ClientInfo, String[]> passiveResultMap = freedmanPassiveUidMap.getPassiveUidMap();
        String[] intersection = Arrays.stream(uidInt).mapToObj(String::valueOf).toArray(String[]::new);
        Map<ClientInfo, double[]> decodedPassiveMap = new HashMap<>();
        Map<ClientInfo, int[]> indexResMap = new HashMap<>();
        for (Map.Entry<ClientInfo, String[]> entry : passiveResultMap.entrySet()) {
            // decode passive方发来的结果
            double[] passiveResDe = Arrays.stream(entry.getValue()).parallel().map(encryptionTool::restoreCiphertext)
                    .mapToDouble(c -> encryptionTool.decrypt(c, privateKey)).toArray();
            String[] passiveResCopy = Arrays.stream(passiveResDe).mapToObj(String::valueOf).toArray(String[]::new);
            intersection = Md5Match.mix(intersection, passiveResCopy);
            decodedPassiveMap.put(entry.getKey(), passiveResDe);
        }
        commonIds = Arrays.stream(intersection).parallel().map(s -> s.split("\\.")[0]).toArray(String[]::new);
        String[] finalIntersection = intersection;
        // 获得每个客户端的index
        for (Map.Entry<ClientInfo, double[]> entry : decodedPassiveMap.entrySet()) {
            ClientInfo clientInfo = entry.getKey();
            double[] decodedUid = entry.getValue();
            int[] index = IntStream.range(0, decodedUid.length).filter(i -> ArrayUtils.contains(finalIntersection, String.valueOf(decodedUid[i]))).toArray();
            indexResMap.put(clientInfo, index);
        }
        return new FreedmanPassiveIdxMap(indexResMap);
    }

    private Message saveResult(Message message, String[] uid) {
        if (!(message instanceof FreedmanPassiveIdx)) {
            throw new UnsupportedOperationException("Wrong type in distribute phase: Freedman id match");
        }
        FreedmanPassiveIdx freedmanPassiveIdx = (FreedmanPassiveIdx) message;
        int[] index = freedmanPassiveIdx.getPassiveIndex();
        commonIds = Arrays.stream(index).parallel().mapToObj(i -> uid[i]).map(s -> s.split("\\.")[0]).toArray(String[]::new);
        return EmptyMessage.message();
    }


    /**
     * 将读取进来的String类型的uid转化为double，但是uid本身必须为数字才可以
     * @param uid
     * @return
     */
    private double[] convertUid(String[] uid) {
        double[] uidInt;
        try {
            uidInt = Arrays.stream(uid).mapToDouble(s -> Double.parseDouble(s.split("\\.")[0])).toArray();
        } catch (Exception e) {
            throw new UnsupportedOperationException(String.format("cannot convert string uid to int uid. first uid is: %s", uid[0]));
        }
        return uidInt;
    }

    // 确保active方的ID相乘<Double的最大值
    private boolean checkUid(double[] uid) {
        double res = 1.0;
        for (double d : uid) {
            res = res * d;
        }
        if (res >= Double.MAX_VALUE) {
            return false;
        } else {
            return true;
        }
    }


//    /**
//     * 将读取进来的String类型的uid转化为double，通过MD5加密为40位的double，todo 后续考虑转化为16进制加快速度
//     * @param uid
//     * @return
//     */
//    private double[] convertUid(String[] uid) {
//        double[] uidInt;
//        try {
//            uidInt = Arrays.stream(uid).mapToDouble(s -> Double.parseDouble(s.split("\\.")[0])).toArray();
//        } catch (Exception e) {
//            uidInt = Arrays.stream(uid).mapToDouble(s -> Double.parseDouble(StringToIntUtil.MD5Encode(s))).toArray();
//        }
//        return uidInt;
//    }

    /**
     * 使用Horner's rule在密文的情况下计算多项式f(x)结果
     * @param coefficients 多项式系数，从alpha<sub>0</sub>开始
     * @param x 变量的值
     * @return f(x)
     */
    public static Ciphertext polynomialCalculation(List<Ciphertext> coefficients, int x, EncryptionTool encryptionTool, PublicKey publicKey) {
        int n = coefficients.size();
        Ciphertext solution = coefficients.get(n - 1);
        for (int i = n - 2; i >= 0; i--) {
            solution = encryptionTool.add(encryptionTool.multiply(solution, x, publicKey), coefficients.get(i), publicKey);
        }
        return solution;
    }



}
