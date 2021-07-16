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
package com.jdt.fedlearn.core.model.mixLinear;

import com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillierNative.signedByteArray;
import com.jdt.fedlearn.core.encryption.distributedPaillier.HomoEncryptionDebugUtil;
import com.jdt.fedlearn.core.encryption.distributedPaillier.HomoEncryptionUtil;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.base.Double2dArray;
import com.jdt.fedlearn.core.entity.base.EmptyMessage;
import com.jdt.fedlearn.core.entity.base.StringArray;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.entity.mixedLinearRegression.*;
import com.jdt.fedlearn.core.entity.psi.MatchResourceLinReg;
import com.jdt.fedlearn.core.exception.NotMatchException;
import com.jdt.fedlearn.core.loader.common.InferenceData;
import com.jdt.fedlearn.core.loader.common.TrainData;
import com.jdt.fedlearn.core.loader.linearRegression.LinearInferenceData;
import com.jdt.fedlearn.core.loader.linearRegression.LinearTrainData;
import com.jdt.fedlearn.core.math.MathExt;
import com.jdt.fedlearn.core.model.Model;
import com.jdt.fedlearn.core.model.serialize.LinearModelSerializer;
import com.jdt.fedlearn.core.optimizer.bfgs.WeightedLinRegLossNonprivClient;
import com.jdt.fedlearn.core.optimizer.bfgs.WeightedLinRegLossPriv;
import com.jdt.fedlearn.core.parameter.LinearParameter;
import com.jdt.fedlearn.core.parameter.SuperParameter;
import com.jdt.fedlearn.core.preprocess.InferenceFilter;
import com.jdt.fedlearn.core.psi.MappingResult;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.core.type.data.Tuple2;
import com.jdt.fedlearn.core.util.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ujmp.core.Matrix;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.IntStream;

import static com.jdt.fedlearn.core.util.TypeConvUtils.*;
import static java.lang.Math.sqrt;

/**
 * Client of MixedLinearRegression. A Client processes its own data, encrypts and sends intermediate result
 * to Master for aggregation and broadcast.
 */
public class LinearRegressionModel implements Model {

    boolean useFakeDec = true;
    boolean debugMode = false;

    // model variables
    private LinearParameter p;
    public int numP;
    private int M; // number of non-private features among all clients. M >= mPriv
    private int N; // number of non-private data instances among all clients
    private int nPriv; // number of private data on this client
    private int mPriv; // number of private feature on this client
    private int fullM; // fullM == M
    private int fullN; // fullN == N
    public double[] weight;
    public double[] weightWithMask;
    public double[] lastWeightWithMask;
    public double[] maskedG;
    private double[] deltaG;
    public double[] weightPriv;
    private double[][] phiNonpriv;
    private double[][] phiPriv;
    private LinearInferenceData inferData;
    private Map<Integer, Integer> featMap;
    private Map<Integer, Integer> idMap;
    private int[][] K;
    private double[] h;
    private int[] dataCategory;
    private int isPrivIdx;
    private double[] yTrue;
    private String[] clientPortList;
    private String selfPort;
    private String[] predictUid;

    private signedByteArray[] maskedWEnc; // 本方进行主方解密时的中间结果。
    private signedByteArray[] maskedWDec; // 本方进行主方解密时的中间结果。
    private signedByteArray[] maskedGDec; // 本方进行主方解密时的中间结果。
    private signedByteArray[] maskedGEnc; // 本方进行主方解密时的中间结果。
    private signedByteArray[] yHatDec;
    private signedByteArray[] yHatEnc;

    private boolean phiReady = false;

    public WeightedLinRegLossPriv privLoss;
    public WeightedLinRegLossNonprivClient nonPrivLoss;
    public HomoEncryptionUtil pheKeys;

    private int iterNum;

    // control variables
    private static final Logger logger = LoggerFactory.getLogger(LinearRegressionModel.class);
    public String modelToken;

    HomoEncryptionDebugUtil decHelper;

    public LinearRegressionModel() {
    }

    @Override
    public TrainData trainInit(String[][] rawData, String[] uids, int[] testIndex, SuperParameter parameter,
                               Features features, Map<String, Object> others) {
        this.p = (LinearParameter) parameter;
        numP = p.getNump();
        // Standalone version, master generates Keys, clients receive.
        this.pheKeys = new HomoEncryptionUtil(numP, p.getEncBits(), useFakeDec);
        if (!useFakeDec) {
            this.pheKeys.setPk((String) others.get("pkStr"));
            this.pheKeys.setSk((String) others.get("skStr"));
        }
        this.clientPortList = (String[]) others.get("clientList");
        this.selfPort = (String) others.get("selfPort");
        return new LinearTrainData(rawData, features);
    }


    /**
     * Training part
     **/
    @Override
    public Message train(int phase, Message masterReturnedMsg, TrainData inputTrainData) {
        LinearTrainData trainData = (LinearTrainData) inputTrainData;  // TODO ??
        // 此参数train时没用
        this.yTrue = null;
        Message ret;
        double start = System.currentTimeMillis();

        // ================== 1. IdMapping =======================
        // if : no idmapping result, do idmapping
        // else : idmapping is done, return empty request.
        if (phase == 1) { // TODO: use enum instead of 1, 2, 3... because 1, 2, 3, has no meanining
            // send Empty request, wait client to return MatchResourceLinReg
            ret = trainIdMatchingPhase1SendMatchingArgs(trainData);
        } else if (phase == 2) {
            ret = trainIdMatchingPhase2RecvMatchingRes(masterReturnedMsg);
        }
        // =================== 2. Training =====================
        else if (phase == 3) {
            // getLocalYhat, 计算本地的y_hat
            ret = trainPhase1ComputeLocalYHat(trainData);
        } else if (phase == 4) {
            // get d_final from master compute compute_g_part and send back, master聚合各方y_hat计算残差并返回后，在
            // client 计算本地的g
            ret = trainPhase2ComputeLocalG(masterReturnedMsg);
        } else if (phase == 5) {
            // compute_g_A_final and update weight, master 聚合各方梯度解密后返回。
            ret = trainPhase3PartialDecG(masterReturnedMsg);
        } else if (phase == 6) {
            ret = trainPhase4FinalDecGComputeST(masterReturnedMsg);
        } else if (phase == 7) {
            ret = trainPhase5PartialDecW(masterReturnedMsg);
        } else if (phase == 8) {
            ret = trainPhase6FinalDecUpdateW(masterReturnedMsg);
        } else if (phase == 9) {
            iterNum += 1;
            ret = new EmptyMessage();
        } else {
            throw new UnsupportedOperationException();
        }
        reportClientPhaseTime(start, phase, iterNum);
        return ret;
    }

    /**
     * client prepare labels, uid and featName, then send to Master
     *
     * @param trainData LinearTrainData
     * @return MatchResourceLinReg
     */
    private Message trainIdMatchingPhase1SendMatchingArgs(LinearTrainData trainData) {
        // client 准备 labels, uid, featName
        double[] labels = trainData.getLabel();
        String[] uid = trainData.getUid();
        String[] featName = trainData.getFeatureName();
        return new MatchResourceLinReg(featName, uid, labels);
    }

    /**
     * client receive id matching results and initialize params
     *
     * @param matchRes2Client an LinearRegressionTrainInitOthers object
     * @return an EmptyMessage object
     */
    private Message trainIdMatchingPhase2RecvMatchingRes(Message matchRes2Client) {
        LinearRegressionTrainInitOthers matchingRes = (LinearRegressionTrainInitOthers) matchRes2Client; // TODO ??
        this.featMap = matchingRes.featMap;
        this.idMap = matchingRes.idMap_LinReg;
        this.M = matchingRes.m;
        this.N = matchingRes.n;
        this.fullM = matchingRes.fullM;
        this.fullN = matchingRes.fullN;
        this.mPriv = matchingRes.m_priv;
        this.nPriv = matchingRes.n_priv;
        this.numP = matchingRes.numP;
        this.h = matchingRes.h;
        this.K = matchingRes.k;
        this.dataCategory = matchingRes.dataCategory;
        this.weightWithMask = matchingRes.weight.clone();
        this.weight = matchingRes.weight.clone();
        this.weightPriv = new double[mPriv];

        this.maskedG = new double[M];
        this.lastWeightWithMask = weightWithMask.clone();
        this.deltaG = new double[M];
        this.phiNonpriv = new double[this.N][this.M];
        this.phiPriv = new double[this.nPriv][this.mPriv];

        // Init dec_helper for debug
        if (!useFakeDec && debugMode) {
            decHelper = new HomoEncryptionDebugUtil();
            decHelper.loadClassFromFile("dist_pai_keys_3_1024");
            decHelper.maxNegAbs = Long.MAX_VALUE;

            String tmp4Check = toJsons(decHelper.dec(pheKeys.getAllZero(M)));
            assert (tmp4Check.equals(toJsons(new long[M])));
        }
        // 此参数train时没用
        this.yTrue = null;

        if (clientPortList.length != numP) {
            throw new NotMatchException("clientList.length needs to be " + numP + ", got " + clientPortList.length);
        }
        return new EmptyMessage(); // return an empty String
    }

    private void preparePhi(LinearTrainData trainData, double[] h) {
        double[][] X = trainData.getFeature();
        double[] hPriv = new double[nPriv];
        double[] hNonPriv = new double[N];
        isPrivIdx = -1;

        // get phi and phi_priv
        int cntGlob = 0;
        int cntPriv = 0;

        // 初始化 w_priv
        // 判断w_priv是否初始化，否则初始化，置 w_priv_ready 为true
        for (int i = 0; i < K.length; i++) {
            if (dataCategory[i] == 1) {
                int cntWIdx = 0;
                int j;
                for (j = 0; j < M - 1; j++) {
                    if (K[i][j] == 1) {
                        weightPriv[cntWIdx] = weight[j];
                        cntWIdx++;
                    }
                }
                weightPriv[cntWIdx] = weight[j]; // 常数t项系数
                cntWIdx++;
                assert (cntWIdx == mPriv);
                isPrivIdx = i;
                break;
            }
        }

        for (int i = 0; i < K.length; i++) {
            if (dataCategory[i] == 1) {
                int cntM = 0;
                // 填入phi的值
                for (int j = 0; j < M - 1; j++) {
                    if (K[i][j] == 1) {
                        phiPriv[cntPriv][cntM] = X[idMap.get(i)][featMap.get(j)];
                        cntM += 1;
                    }
                }
                phiPriv[cntPriv][cntM] = 0.5d; // 添加常数项

                // 填入 hPriv
                hPriv[cntPriv] = h[i];

                cntPriv += 1;
                assert (cntM + 1 == mPriv);
            }
            if (dataCategory[i] == 2) {
                int j;
                for (j = 0; j < M - 1; j++) {
                    if (K[i][j] == 0) {
                        phiNonpriv[cntGlob][j] = 0.0d;
                    } else {
                        try {
                            phiNonpriv[cntGlob][j] = X[idMap.get(i)][featMap.get(j)] * (1.0 / ((double) K[i][j] + Double.MIN_VALUE));
                        } catch (NullPointerException e) {
                            String msg = "featMap or idMap does not match given data.\n";
                            msg += "X.shape = " + X.length + ", " + X[0].length + "\n";
                            msg += "featMap is " + featMap;
                            throw new NullPointerException(e.getMessage() + "\n" + msg);
                        } catch (ArrayIndexOutOfBoundsException e) {
                            String msg = "featMap or idMap does not match given data.\n";
                            msg += "X.shape = " + X.length + ", " + X[0].length + "\n";
                            msg += "featMap is " + featMap;
                            throw new ArrayIndexOutOfBoundsException(e.getMessage() + "\n" + msg);
                        }
                    }
                }
                phiNonpriv[cntGlob][j] = 0.5d / (numP + Double.MIN_VALUE);  // 添加常数项

                // 填入 hPriv
                hNonPriv[cntGlob] = h[i];
                cntGlob += 1;
            }
        }
        iterNum = 0;

        if (nPriv == 0) {
            privLoss = new WeightedLinRegLossPriv(true);
        } else {
            privLoss = new WeightedLinRegLossPriv(phiPriv, weightPriv, hPriv, fullM, K[isPrivIdx], p.getEta(), p.getLambda());
        }

        if (N == 0) {
            nonPrivLoss = null;
        } else {
            nonPrivLoss = new WeightedLinRegLossNonprivClient(phiNonpriv, weight, hNonPriv, p.getEta(), BigInteger.ONE, p.getLambda());
        }
    }

    /**
     * Client compute local y_hat then do encryption and send it to Master
     *
     * @param data a LinearTrainData object
     * @return encrypted y_hat
     */
    private Message trainPhase1ComputeLocalYHat(TrainData data) {
        if (!phiReady) {
            LinearTrainData trainData = (LinearTrainData) data; // TODO ??
            preparePhi(trainData, this.h);
            phiReady = true;
        }
        if (N == 0) {
            return trainPhase2ComputeLocalG(new EmptyMessage());
        } else {
            nonPrivLoss.forward();
            signedByteArray[] yHatPEnc = pheKeys.encryption(
                    Arrays.stream(nonPrivLoss.getYHat()).toArray(),
                    pheKeys.getPk());
            return new CypherMessage(yHatPEnc);
        }
    }

    /**
     * Client compute local g, then sand g_priv and g_nonPriv to Master
     *
     * @param yHatRecvStr encrypted y - sum(y_hat_p)
     * @return a TwoCypherMessage containing g_nonPriv and g_priv
     */
    private Message trainPhase2ComputeLocalG(Message yHatRecvStr) {

        double[] gPrivHdim = new double[fullM];
        double[] privSumGrad = privLoss.computeG();
        privLoss.lowDim2HighDim1DArr(privSumGrad, gPrivHdim);

        signedByteArray[] gPrivEnc = Arrays.stream(gPrivHdim).parallel()
                .mapToObj(x -> pheKeys.encryption(x * nPriv, pheKeys.getPk()))
                .toArray(signedByteArray[]::new);

        if (N == 0) {
            return new TwoCypherMessage(new CypherMessage(), new CypherMessage(gPrivEnc));
        } else {
            signedByteArray[] gNonprivPart = computeGPartEnc(yHatRecvStr);
            return new TwoCypherMessage(new CypherMessage(gNonprivPart), new CypherMessage(gPrivEnc));
        }
    }

    private signedByteArray[] computeGPartEnc(Message yHatRecvStr) {
        signedByteArray[] gPart = new signedByteArray[M];

        signedByteArray[] dRcevEnc = ((CypherMessage) yHatRecvStr).getBody();
        IntStream.range(0, M).parallel()
                .forEach(j -> accumulateGjEnc(j, dRcevEnc, gPart));
        return gPart;
    }

    /**
     * Compute local g
     *
     * @param j        j-th entry of g
     * @param dRcevEnc y - sum(y_hat_p)
     * @param gPart    local g
     */
    private void accumulateGjEnc(int j, signedByteArray[] dRcevEnc, signedByteArray[] gPart) {
        assert phiNonpriv.length == N;
        // initializing g_j as 0
        double[] tmp = new double[phiNonpriv.length];
        for (int i = 0; i < phiNonpriv.length; i++) {
            tmp[i] = phiNonpriv[i][j];
        }
        assert dRcevEnc.length == phiNonpriv.length;
        gPart[j] = pheKeys.innerProduct(dRcevEnc, tmp, pheKeys.getPk());
    }

    /**
     * Do partial decryption for g
     * All client do partial decryption locally, then return then intermediate result to Master for broadcast.
     *
     * @param fullGEncMsg a CypherMessage object containing encrypted g from Master
     * @return a PartialDecMessage object.
     */
    private Message trainPhase3PartialDecG(Message fullGEncMsg) {
        maskedGEnc = ((CypherMessage) fullGEncMsg).getBody();
        maskedGDec = pheKeys.decryptPartial(maskedGEnc, pheKeys.getSk());

        // debug
        if (debugMode) {
            logger.info(Arrays.toString(clientPortList));
        }

        Map<String, signedByteArray[]> gInfo = new HashMap<>();
        for (String otherClient : clientPortList) {
            if (!otherClient.equals(selfPort)) {
                gInfo.put(otherClient, maskedGDec);

                // debug
                if (debugMode) {
                    logger.info("sending to : " + otherClient);
                }
            }
        }
        if (gInfo.size() != numP - 1) {
            throw new NotMatchException("client " + selfPort + " needs to send out " + (numP - 1) +
                    ", but only got " + gInfo.size());
        }
        return new PartialDecMessage(gInfo);
    }

    /**
     * Client do Final decryption for g
     *
     * @param fullGMsg a CypherMessageList object of encrypted g
     */
    private Message trainPhase4FinalDecGComputeST(Message fullGMsg) {
        if (((CypherMessageList) fullGMsg).getBody().size() != (numP - 1)) {  // TODO  ??
            throw new NotMatchException("Decryption error. Need " + (numP - 1) + " parts. Got " +
                    ((CypherMessageList) fullGMsg).getBody().size() + " parts.");
        }
        signedByteArray[][] gOthersDec = ((CypherMessageList) fullGMsg).getBody().toArray(new signedByteArray[0][]);

        double[] g_dec;
        signedByteArray[][] decodeList = new signedByteArray[gOthersDec.length + 1][gOthersDec[0].length];
        decodeList[0] = maskedGDec;
        int cnt = 1;
        for (signedByteArray[] imRes : gOthersDec) {
            assert (maskedGDec.length == imRes.length);
            decodeList[cnt++] = imRes; // TODO split
        }
        g_dec = pheKeys.decryptFinal(decodeList, maskedGEnc, pheKeys.getSk());
        return getBFGSImRes(toJsons(g_dec));
    }

    /**
     * <p>
     * compute intermediate value for bfgs.
     * returning a 6-element String array, with each entry containing the following info:
     * :</p>
     * <code>
     * res[0] = String.valueOf(rho_vec);
     * res[1] = toJsons(s_yTranspose);
     * res[2] = toJsons(y_sTranspose);
     * res[3] = toJsons(s_sTranspose);
     * res[4] = toJsons(vec_value_map);
     * res[5] = toJsons(mat_value_map);
     * </code>
     *
     * @param delta_g delta g
     * @param delta_w delta w
     */
    private String[] computeBFGSImRes(double[] delta_w, double[] delta_g) {
        double[] rho_vec = new double[M];
        double[][] s_yTranspose = new double[M][M];
        double[][] y_sTranspose = new double[M][M];
        double[][] s_sTranspose = new double[M][M];
        int[] vecValueMap = new int[M];
        int[][] matValueMap = new int[M][M];

        Matrix s = Matrix.Factory.linkToArray(delta_w);
        Matrix y = Matrix.Factory.linkToArray(delta_g);

        for (int i = 0; i < M; i++) {
            rho_vec[i] = delta_w[i] * delta_g[i];
            if (i != M - 1) {
                if ((featMap.get(i) >= 0)) {
                    vecValueMap[i] = 1;
                } else {
                    vecValueMap[i] = 0;
                }
            } else {
                vecValueMap[i] = 1;
            }
        }
        for (int i = 0; i < M; i++) {
            for (int j = 0; j < M; j++) {
                if (i != M - 1 && j != M - 1) {
                    if ((featMap.get(i) >= 0) && (featMap.get(j) >= 0)) {  // todo: check feat_map
                        matValueMap[i][j] = 1;
                    } else {
                        matValueMap[i][j] = 0;
                    }
                } else if (i == M - 1 && j != M - 1) {
                    if ((featMap.get(j) >= 0)) {
                        matValueMap[i][j] = 1;
                    } else {
                        matValueMap[i][j] = 0;
                    }
                } else if (i != M - 1 && j == M - 1) {
                    if ((featMap.get(i) >= 0)) {
                        matValueMap[i][j] = 1;
                    } else {
                        matValueMap[i][j] = 0;
                    }
                } else {
                    matValueMap[i][j] = 1;
                }
            }
        }
        matrixToarray(s.mtimes(y.transpose()), s_yTranspose);
        matrixToarray(y.mtimes(s.transpose()), y_sTranspose);
        matrixToarray(s.mtimes(s.transpose()), s_sTranspose);

        String[] res = new String[6];
        res[0] = toJsons(rho_vec);
        res[1] = toJsons(s_yTranspose);
        res[2] = toJsons(y_sTranspose);
        res[3] = toJsons(s_sTranspose);
        res[4] = toJsons(vecValueMap);
        res[5] = toJsons(matValueMap);
        return res;
    }

    /**
     * Client compute deltaG and delta w, then compute intermediate value for bfgs
     *
     * @param full_g_str g in plaintext
     * @return a StringArray object containing bfgs intermediate results and the averaged g
     */
    private Message getBFGSImRes(String full_g_str) {
        double[] newMaskedG = parse1dDouble(full_g_str);
        newMaskedG = MathExt.elemwiseInvMul(newMaskedG, fullN);

        assert (lastWeightWithMask.length == newMaskedG.length && weightWithMask.length == newMaskedG.length);

        /*
         *  1) compute BFGS im result
         *  NOTE: delta_g for i+1-th round is already computed in i-th round, no need to update!
         *        But delta_w needs!
         */
        double[] delta_w = MathExt.elemwiseSub(weightWithMask, lastWeightWithMask);

        // 2) now update new g!
        deltaG = MathExt.elemwiseSub(newMaskedG, maskedG);
        maskedG = newMaskedG.clone();

        String[] im_res = computeBFGSImRes(delta_w, deltaG);

        // update bfgs im results and the averaged g.
        String[] ret = new String[2];
        ret[0] = toJsons(im_res);
        ret[1] = toJsons(pheKeys.encryption(newMaskedG, pheKeys.getPk()));
        return new StringArray(ret);
    }

    /**
     * partially decrypt w
     *
     * @param fullWMsg a CypherMessageList object of encrypted w
     * @return local partial decryption result of w
     */
    private Message trainPhase5PartialDecW(Message fullWMsg) {
        maskedWEnc = ((CypherMessage) fullWMsg).getBody();
        maskedWDec = pheKeys.decryptPartial(maskedWEnc, pheKeys.getSk());

        // client 从方解密 w
        Map<String, signedByteArray[]> weightInfo = new HashMap<>();
        for (String otherClient : clientPortList) {
            if (!otherClient.equals(selfPort)) {
                weightInfo.put(otherClient, maskedWDec);
            }
        }
        return new PartialDecMessage(weightInfo);
    }

    /**
     * final decryption of w and param update
     *
     * @param fullWMsg partial decryption results of w
     */
    private Message trainPhase6FinalDecUpdateW(Message fullWMsg) {
        signedByteArray[][] wOthersDec = ((CypherMessageList) fullWMsg).getBody().toArray(new signedByteArray[0][]);

        double[] wWithMaskDec;
        // 解密 w
        signedByteArray[][] decodeList = new signedByteArray[wOthersDec.length + 1][wOthersDec[0].length];
        decodeList[0] = maskedWDec;
        int cnt = 1;
        for (signedByteArray[] imres : wOthersDec) {
            assert (maskedWDec.length == imres.length);
            decodeList[cnt++] = imres;
        }
        wWithMaskDec = pheKeys.decryptFinal(decodeList, maskedWEnc, pheKeys.getSk());
        if (!useFakeDec) {
            wWithMaskDec = MathExt.elemwiseInvMul(wWithMaskDec, 10);
        }
        return updateLocalParams(toJsons(wWithMaskDec));
    }

    private Message updateLocalParams(String full_w_str) {
        double[] full_w = parse1dDouble(full_w_str);
        for (int i = 0; i < full_w.length; i++) {
            lastWeightWithMask[i] = weightWithMask[i];
            weightWithMask[i] = full_w[i];
            if (i != full_w.length - 1) {
                if (featMap.get(i) >= 0) {
                    weight[i] = weightWithMask[i];
                } else {
                    weight[i] = 0d;
                }
            } else {
                weight[i] = weightWithMask[i];
            }
        }
        // 更新 weight_priv
        if (nPriv != 0) {
            int j;
            int cntWIdx = 0;
            for (j = 0; j < M - 1; j++) {
                if (K[isPrivIdx][j] == 1) {
                    weightPriv[cntWIdx++] = weight[j];
                }
            }
            weightPriv[cntWIdx] = weight[j]; // 常数项系数
        }

        //更新 weight, g
        privLoss.updateWFinal(weightPriv);
        privLoss.updateDeltG();

        if (N > 0) {
            nonPrivLoss.updateW(weight);
            nonPrivLoss.forward();
            nonPrivLoss.updateLoss();
        }
        privLoss.forward();
        privLoss.updateLoss();


        int[] vecValueMap = new int[M];

        for (int i = 0; i < M; i++) {
            if (i != M - 1) {
                if ((featMap.get(i) >= 0)) {
                    vecValueMap[i] = 1;
                } else {
                    vecValueMap[i] = 0;
                }
            } else {
                vecValueMap[i] = 1;
            }
        }

        // 传输 weight 和 private loss 到 master
        double[] selfG = new double[M];
        for (int i = 0; i < M; i++) {
            if (i != M - 1) {
                if ((featMap.get(i) >= 0)) {
                    selfG[i] = maskedG[i];
                }
            } else {
                selfG[i] = maskedG[i];
            }
        }

        String[] ret = new String[5];
        ret[0] = toJsons(weight);
        ret[1] = toJsons(selfG);
        ret[2] = toJsons(vecValueMap);
        ret[3] = toJsons(pheKeys.encryption(weight, pheKeys.getPk()));
        ret[4] = toJsons(privLoss.getLoss());
        if (nPriv != 0) {
            logger.info("privLoss = " + privLoss.getLoss() / nPriv + "\tprivRMSE = " + sqrt(privLoss.getLoss() / nPriv)
                    + "\nweight_with_mask & weight & g_with_mask" +
                    "\n\tweight_with_mask = " + toJsons(Tool.roundTo4(weightWithMask)) +
                    "\n\tweight = " + toJsons(Tool.roundTo4(weight)) +
                    "\n\tg_with_mask = " + toJsons(Tool.roundTo4(maskedG))
            );
        }

        return new StringArray(ret);
    }

    /*
     **********************
     **** Inference part***
     **********************
     **/

    @Override
    public Message inferenceInit(String[] uidList, String[][] inferenceCacheFile, Map<String, Object> Others) {

        this.p = new LinearParameter();
        this.p.setEncBits((Integer) Others.get("encBits"));
        this.p.setNump((Integer) Others.get("numP"));
        // Standalone version, master generates Keys, clients receive.
        this.pheKeys = new HomoEncryptionUtil(p.getNump(), p.getEncBits(), useFakeDec);
        if (!useFakeDec) {
            this.pheKeys.setPk((String) Others.get("pkStr"));
            this.pheKeys.setSk((String) Others.get("skStr"));
        }
        this.clientPortList = (String[]) Others.get("clientList");
        this.selfPort = (String) Others.get("selfPort");
        this.predictUid = uidList;

        return InferenceFilter.filter(uidList, inferenceCacheFile);
    }


    @Override
    public Message inference(int phase, Message jsonData, InferenceData data) {
        Message ret;
        if (phase == -2) {//TODO enum
            ret = inferIdMatchingPhase1((LinearInferenceData) data);
        } else if (phase == -3) {
            ret = inferIdMatchingPhase2(jsonData);
        } else if (phase == -4) {
            ret = inferPhase1();
        } else if (phase == -5) {
            ret = inferPartialDecYHat(jsonData);
        } else if (phase == -6) {
            if (N == 0) {
                return inferPhase2("do not need data from master");
            }
            ret = inferFinalDecYHat(jsonData);
        } else {
            throw new UnsupportedOperationException();
        }
        return ret;
    }

    /**
     * client prepare labels, uid and featName, then send to Master
     *
     * @param data a LinearInferenceData object
     * @return a MatchResourceLinReg object
     */
    private Message inferIdMatchingPhase1(LinearInferenceData data) {
        this.inferData = data;
        // client 准备 labels, uid, featName
        String[] uid = inferData.getUid();
        String[] featName = inferData.getFeatureName();
        double[] ground_truth = inferData.getGroudtruth();
        return new MatchResourceLinReg(featName, uid, ground_truth);
    }

    /**
     * client receive id matching results and initialize params
     *
     * @param masterRetMsg an LinearRegressionInferInitOthers object
     * @return an EmptyMessage object
     */
    private Message inferIdMatchingPhase2(Message masterRetMsg) {
        LinearRegressionInferInitOthers other_info = ((LinearRegressionInferInitOthers) masterRetMsg); // TODO ??
        this.N = other_info.n;
        this.K = other_info.k;
        this.featMap = other_info.featMap;
        this.idMap = other_info.idMapLinReg;
        this.M = other_info.m;
        this.fullM = other_info.fullM;
        this.fullN = other_info.fullN;
        this.mPriv = other_info.mPriv;
        this.nPriv = other_info.nPriv;
        this.dataCategory = other_info.dataCategory;
        this.numP = other_info.numP;

        // debug utils
        if (!useFakeDec && debugMode) {
            decHelper = new HomoEncryptionDebugUtil();
            decHelper.loadClassFromFile("dist_pai_keys_3_1024");
            decHelper.maxNegAbs = Long.MAX_VALUE;
        }

        this.phiNonpriv = new double[this.N][this.M];
        this.phiPriv = new double[this.nPriv][this.mPriv];
        preparePhi(inferData.getX_inference());
        return new EmptyMessage();  // return an empty String
    }

    private void preparePhi(double[][] X_infer) {
        int cntPriv = 0;
        int cntGlob = 0;

        if(X_infer.length != 0) {
            for (int i = 0; i < K.length; i++) {
                if (dataCategory[i] == 1) {
                    int cnt_M = 0;
                    // 填入phi的值
                    for (int j = 0; j < M - 1; j++) {
                        if (K[i][j] == 1) {
                            phiPriv[cntPriv][cnt_M] = X_infer[idMap.get(i)][featMap.get(j)];
                            cnt_M += 1;
                        }
                    }
                    phiPriv[cntPriv][cnt_M] = 0.5d; // fixme: may be wrong. check it out 添加常数项

                    cntPriv += 1;
                    assert (cnt_M + 1 == mPriv);
                }
                if (dataCategory[i] == 2) {
                    int j;
                    for (j = 0; j < M - 1; j++) {
                        if (K[i][j] == 0) {
                            phiNonpriv[cntGlob][j] = 0.0d;
                        } else {
                            phiNonpriv[cntGlob][j] = X_infer[idMap.get(i)][featMap.get(j)] * (1.0 / ((double) K[i][j] + Double.MIN_VALUE));
                        }
                    }
                    phiNonpriv[cntGlob][j] = 0.5d / (numP + Double.MIN_VALUE);  // 添加常数项
                    cntGlob += 1;
                }
            }
        }
        if (nPriv == 0) {
            privLoss = new WeightedLinRegLossPriv(true);
        } else {
            privLoss = new WeightedLinRegLossPriv(phiPriv, weightPriv);
        }
        if (N == 0) {
            nonPrivLoss = null;
        } else {
            nonPrivLoss = new WeightedLinRegLossNonprivClient(phiNonpriv, weight, BigInteger.ONE);
        }
    }

    /**
     * Client computes local y_hat, then encrypt
     *
     * @return a CypherMessage object containing encrypted y_hat
     */
    private Message inferPhase1() {
        CypherMessage body = null;
        if (nonPrivLoss != null) {
            body = new CypherMessage(pheKeys.encryption(nonPrivLoss.getYHat(), pheKeys.getPk()));
        }
        return body;
    }

    /**
     * client partially decrypt y_hat
     */
    private Message inferPartialDecYHat(Message yHatNonPrivStr) {
        yHatEnc = ((CypherMessage) yHatNonPrivStr).getBody();
        yHatDec = pheKeys.decryptPartial(yHatEnc, pheKeys.getSk());

        Map<String, signedByteArray[]> y_hat_Info = new HashMap<>();
        for (String other_client : clientPortList) {
            if (!other_client.equals(selfPort)) {
                y_hat_Info.put(other_client, yHatDec);
            }
        }
        return new PartialDecMessage(y_hat_Info);
    }

    /**
     * Client do final decryption of y_hat, then return.
     */
    private Message inferFinalDecYHat(Message yHatLstStr) {
        signedByteArray[][] yHatOthersDec = ((CypherMessageList) yHatLstStr).getBody().toArray(new signedByteArray[0][]);
        // 解密 y_hat
        signedByteArray[][] decodeList = new signedByteArray[yHatOthersDec.length + 1][yHatOthersDec[0].length];
        decodeList[0] = yHatDec;
        int cnt = 1;
        for (signedByteArray[] imres : yHatOthersDec) {
            assert (yHatDec.length == imres.length);
            decodeList[cnt++] = imres;
        }
        double[] y_hat = pheKeys.decryptFinal(decodeList, yHatEnc, pheKeys.getSk());
        return inferPhase2(toJsons(y_hat));
    }

    private Message inferPhase2(String yHatFinalNonPrivStr) {
        double[] yHatFinal;
        if (nonPrivLoss != null) {
            double[] yHatFinalNonPriv = parse1dDouble(yHatFinalNonPrivStr);
            yHatFinal = combinePrivNonPrivYHat(yHatFinalNonPriv, privLoss.getYHat());
        } else {
            yHatFinal = combinePrivNonPrivYHat(privLoss.getYHat());
        }
        // for debug
        // compute local loss w.r.t local_Y_groudtruth
        if (inferData.getGroudtruth() != null && debugMode) {
            yTrue = inferData.getGroudtruth();
            double localLoss = 0d;
            int cnt = 0;
            for (int i = 0; i < yTrue.length; i++) {
                if (!Double.isNaN(yHatFinal[i])) {
                    localLoss += (yHatFinal[i] - yTrue[i]) * (yHatFinal[i] - yTrue[i]);
                    cnt += 1;
                }
            }
            if (cnt != 0) {
                logger.info("local loss = " + localLoss / cnt + "\tlocal RMSE = " + sqrt(localLoss / cnt));
            }
        }
        assert (dataCategory.length == yHatFinal.length);

        int cntSamples = 0;
        for (Map.Entry<Integer, Integer> entry : idMap.entrySet()) {
            if (entry.getValue() != -1) {
                cntSamples++;
            }
        }
        double[] predRes = new double[cntSamples];
        int cnt = 0;
        for (int i = 0; i < yHatFinal.length; i++) {
            if (idMap.get(i) != -1) {
                predRes[cnt++] = yHatFinal[i];
            }
        }
        String[] actualUid = this.inferData.getUid();
        Map<String, Double> res = new HashMap<>();
        for (String uid : predictUid) {
            res.put(uid, Double.NaN);
        }
        for (int i = 0; i < predRes.length; i++) {
            res.put(actualUid[i], predRes[i]);
        }
        double[] outRes = Arrays.stream(this.predictUid).mapToDouble(res::get).toArray();
        List<double[]> list = new ArrayList<>();
        list.add(inferData.getGroudtruth());
        list.add(predRes);
        list.add(outRes);
        return new Double2dArray(list);
    }

    private double[] combinePrivNonPrivYHat(double[] yHatFinalNonpriv, double[] yHatFinalPriv) {
        double[] yHatFinal = new double[dataCategory.length];
        assert (nPriv + N <= dataCategory.length);
        int privCount = 0;
        int nonPrivCount = 0;

        for (int i = 0; i < dataCategory.length; i++) {
            if (dataCategory[i] == 1) {
                yHatFinal[i] = yHatFinalPriv[privCount];
                privCount += 1;
            } else if (dataCategory[i] == 2) {
                yHatFinal[i] = yHatFinalNonpriv[nonPrivCount];
                nonPrivCount += 1;
            } else {
                yHatFinal[i] = Double.NaN;
            }
        }
        return yHatFinal;
    }

    private double[] combinePrivNonPrivYHat(double[] yHatFinalPriv) {
        double[] yHatFinal = new double[dataCategory.length];
        assert (nPriv + N <= dataCategory.length);
        int privCount = 0;
        for (int i = 0; i < dataCategory.length; i++) {
            assert (dataCategory[i] != 2);
            if (dataCategory[i] == 1) {
                yHatFinal[i] = yHatFinalPriv[privCount];
                privCount += 1;
            } else {
                yHatFinal[i] = Double.NaN;
            }
        }
        return yHatFinal;
    }

    @Override
    public String serialize() {
        return "modelToken=" + modelToken + "\n" +
                "weight=" + toJsons(weight) + "\n" +
                "M=" + M + "\n" +
                "numP=" + numP + "\n" +
                "M_priv=" + mPriv + "\n" +
                "weight_priv=" + toJsons(weightPriv) + "\n";
    }

    @Override
    public void deserialize(String content) {
        LinearRegressionModel model = LinearModelSerializer.loadLinearRegressionModel(content);
        this.modelToken = model.modelToken;
        this.weight = model.weight;
        this.weightPriv = model.weightPriv;
        this.numP = model.numP;
    }

    @Override
    public AlgorithmType getModelType() {
        return AlgorithmType.LinearRegression;
    }

    public void setMaskedGDec(signedByteArray[] maskedGDec) {
        this.maskedGDec = maskedGDec;
    }

    public void setWeightWithMask(double[] weightWithMask) {
        this.weightWithMask = weightWithMask;
    }

    public int[] getDataCategory() {
        return dataCategory;
    }

    public void setDataCategory(int[] dataCat) {
        dataCategory = dataCat;
    }

    private static void reportClientPhaseTime(double start, int phase, int iterNum) {
        LinearRegressionModel.logger.info("Client " + "\tphase = " + phase + "\titernum = " + iterNum);
//        LinearRegressionModel.logger.info("\t data transferred = " + len + " KB");
        LinearRegressionModel.logger.info("\t time spent = " + (System.currentTimeMillis() - start) / 1000.0);
    }
}

