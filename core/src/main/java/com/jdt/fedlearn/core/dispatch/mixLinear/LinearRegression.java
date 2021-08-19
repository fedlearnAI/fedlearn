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

package com.jdt.fedlearn.core.dispatch.mixLinear;

import com.jdt.fedlearn.core.dispatch.common.Control;
import com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillier;
import com.jdt.fedlearn.core.encryption.distributedPaillier.HomoEncryptionDebugUtil;
import com.jdt.fedlearn.core.encryption.distributedPaillier.HomoEncryptionUtil;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.base.Double2dArray;
import com.jdt.fedlearn.core.entity.base.IntArray;
import com.jdt.fedlearn.core.entity.base.StringArray;
import com.jdt.fedlearn.core.entity.common.*;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.entity.mixedLinearRegression.*;
import com.jdt.fedlearn.core.exception.NotMatchException;
import com.jdt.fedlearn.core.math.MathExt;
import com.jdt.fedlearn.core.model.mixLinear.idmatcher.LinRegMatcher;
import com.jdt.fedlearn.core.model.mixLinear.idmatcher.LinregMatchAlg;
import com.jdt.fedlearn.core.parameter.LinearParameter;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.data.Pair;
import com.jdt.fedlearn.core.util.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ujmp.core.DenseMatrix;
import org.ujmp.core.Matrix;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillierNative.signedByteArray;
import static com.jdt.fedlearn.core.encryption.distributedPaillier.HomoEncryptionUtil.arrayCopy;
import static com.jdt.fedlearn.core.encryption.distributedPaillier.HomoEncryptionUtil.toDouble;
import static com.jdt.fedlearn.core.math.MathExt.elemwiseAdd;
import static com.jdt.fedlearn.core.math.MathExt.elemwiseInvMul;
import static com.jdt.fedlearn.core.util.Tool.geneEmptyReq;
import static com.jdt.fedlearn.core.util.TypeConvUtils.*;


public class LinearRegression implements Control {

    boolean useFakeDec = false;
    boolean debugMode = false;

    private static final AlgorithmType algorithmType = AlgorithmType.LinearRegression;
    private final Map<MetricType, List<Pair<Integer, Double>>> metricMap = new HashMap<>();
    private final LinearParameter p;

    // model variables
    private final int numP; // number of parties 参与方个数
    private int M;  // total number of different features of ALL parties. 各方总的feature数
    private int fullN; // total number of training/inference instances 各方总的数据个数
    public signedByteArray[] weightEnc; // encrypted weight. w的密文
    public double[] g; // plaintext of g. g的明文
    public signedByteArray[] gEnc; // ciphertext of g. g的密文
    private double fullLoss = Double.MAX_VALUE;
    private double[][] hessInv; // inverse of Hessian matrix. Hessian 矩阵的逆
    public int iterNum;
    private double[] h; // averaged yTrue. 求平均后的yTrue

    // A vector of length M. Indicates which feature does this client have. 1 means true.
    private Map<ClientInfo, double[]> clientFeatMap;

    // when computing hessInv, some intermediate results (s and y vectors) need to be
    // computed in PLAINTEXT on CLIENT. This is not secure because Master has to mockSend
    // FULL g to all other parties who will decrypt g and get the plaintext.
    // In contrast, a secure way is to mockSend specific entries of g to those parties who
    // OWNs those features or adding random mask. To make the computation secure, Master mask encrypted g
    // with  clientFeatRandWeight, and mockSend the masked g to all clients. Then clients compute on
    // these masked values and then mockSend the "wrong" s and t to master. Master then recovers "true"
    // value of s and y using clientFeatRandWeight.
    private Map<String, double[]> clientFeatRandWeight; // g的随机掩码

    private boolean idMatchDone = false; // tells if IDMatching is Done successfully
    private final HomoEncryptionUtil pheKeys; // interface for all encryption-related operations
    private int N; // number of NonPriv data

    // debug util
    private HomoEncryptionDebugUtil decHelper;

    // control variables
    private static final Logger logger = LoggerFactory.getLogger(LinearRegression.class);
    private static final int[] trainPhaseArray = new int[]{ 1, 2, 3, 4, 5, 6, 7, 8, 9};
    private static final int[] inferPhaseArray = new int[]{ -2, -3, -4, -5, -6, -7};
    private int phase;

    public LinearRegression(LinearParameter tmp) {
        this.p = tmp;
        this.numP = p.getNump();
        // Key initialization
        this.pheKeys = new HomoEncryptionUtil(numP, p.getEncBits(), useFakeDec);
    }

    public void trainParamInit(LinRegTrainInitParams linRegParam) {
        this.iterNum = 0;
        this.M = linRegParam.M;
        this.g = new double[this.M];

        this.N = linRegParam.N;
        this.fullN = linRegParam.fullN;

        this.h = linRegParam.h;
        this.clientFeatMap = linRegParam.clientFeatMap;
        this.hessInv = DenseMatrix.Factory.eye(M, M).toDoubleArray();

        this.gEnc = pheKeys.getAllZero(M);
        this.weightEnc = pheKeys.getAllZero(M);
        if(debugMode) {
            // debug. 一个small test, 验证加解密可运行.
            String checkTmp = toJsons(decHelper.dec(gEnc));
            assert (checkTmp.equals(toJsons(new long[M])));
        }
    }

    /**
     * initControl for master
     * @param clientInfos 客户端列表
     * @param other : contains LinearRegressionTrainInitOthers for each client.
     */
    public List<CommonRequest> initControl(List<ClientInfo> clientInfos,
                                           MatchResult idMap,
                                           Map<ClientInfo, Features> features,
                                           Map<String, Object> other) {
        if(!useFakeDec) {
            DistributedPaillier.DistPaillierPubkey pubkey = new DistributedPaillier.DistPaillierPubkey();
            pubkey.parseJson((String)other.get("pubKeyStr"));
            this.pheKeys.setPk(pubkey);
        }
        // Debug mode Key initialization
        if(debugMode && !useFakeDec) {
            // 生成一份 priv/pubkey 后, 保存为文件, 然后从文件加载. 方便debug.
            decHelper = new HomoEncryptionDebugUtil(pheKeys.getPk(), pheKeys.getSkAll(), numP, Long.MAX_VALUE);
            decHelper.saveToFile("dist_pai_keys_3_1024");
        }

        List<CommonRequest> initRequests = new ArrayList<>();
        String[] clientList = clientInfos.stream().map(x-> x.getIp()+x.getPort()).toArray(String[]::new);

        int cnt = 1;
        for (ClientInfo clientInfo : clientInfos) {
            Map<String, Object> extraParamsFromMaster = new HashMap<>();
            extraParamsFromMaster.put("trainDataHasLabel", !(features.get(clientInfo).getLabel()==null));
            extraParamsFromMaster.put("clientList", clientList);
            extraParamsFromMaster.put("selfPort", clientInfo.getIp()+clientInfo.getPort());
            extraParamsFromMaster.put("thisPartyID", cnt);
            cnt += 1;

            TrainInit trainInit = new TrainInit(p, features.get(clientInfo), null, extraParamsFromMaster);
            CommonRequest request = CommonRequest.buildTrainInitial(clientInfo, trainInit);
            initRequests.add(request);
        }
        phase = 0;
        return initRequests;
    }


    public List<CommonRequest> control(List<CommonResponse> response) {

        List<CommonRequest> res;
        this.phase = getNextPhase(phase);
        double start = System.currentTimeMillis();
        // ================== 1. IdMapping =======================
        // if no idmapping result, do idmapping
        // if idmapping is done, return empty request.
        if (phase == 1) {
            // mockSend Empty request, wait client to return MatchResourceLinReg
            res = geneEmptyReq(response, phase);
        } else if (phase == 2) {
            res = trainIdMatchingPhase2GetIdMatchingRes(response);
            idMatchDone = true;
        }
        // =================== 2. Training =====================
          else if (phase == 3 ) {
            res = geneEmptyReq(response, phase);
        } else if (phase == 4 ) {
              // get loacalYhat from clients, compute d_final, and mockSend back
            res = controlPhase2GetPartialYHat(response);
        } else if (phase == 5 ) {
              // compute gradient
            res = controlPhase3ComputeGEnc(response);
        } else if (phase == 6 ) {
              // 转发多方解密中间结果
            res = bCastPartialDecRes(response);
        } else if (phase == 7 ) {
              // compute Hessian inverse matrix
            res = controlPhase5UpdateHessInv(response);
        } else if (phase == 8 ) {
            res = bCastPartialDecRes(response);
        } else if (phase == 9 ) {
            res = controlPhase7ReportTrainRes(response);
        } else {
            throw new UnsupportedOperationException();
        }
        reportMasterPhaseTime(start, phase, iterNum);
        return res;
    }

    private List<CommonRequest> trainIdMatchingPhase2GetIdMatchingRes(List<CommonResponse> responses) {
        List<CommonRequest> res = new ArrayList<>();
        Map<ClientInfo, LinregMatchAlg.MixedLinRegIdMappingRes> matchRes;

        LinRegMatcher matcher = new LinRegMatcher(true, p);
        try {
            matchRes  = matcher.masterMatchingPhase(responses);
        } catch (Exception e) {
            logger.error("trainIdMatchingPhase2GetIdMatchingRes error",e);
            throw new NotMatchException("trainIdMatchingPhase2GetIdMatchingRes error",e);
        }
        Map<String, Map<String,Object>> masterAndClientParams = matcher.collectTrainParams(matchRes);

        trainParamInit( (LinRegTrainInitParams) masterAndClientParams.get("master").get("0") );

        for (CommonResponse entry : responses) {
            ClientInfo client = entry.getClient();
            CommonRequest request = new CommonRequest(client,
                    ((LinearRegressionTrainInitOthers) masterAndClientParams.get("client")
                            .get(client.getIp()+client.getPort())));
            request.setPhase(phase);
            res.add(request);
        }
        return res;
    }

    /**
     * <p> training phase 2 </p>
     * Master 从各方接收发过来的 <code>y_hat_part</code>，将它们相加计算出最终的<code>y_hat</code>
     * 再和<code>h</code>做差并返回.
     */
    private List<CommonRequest> controlPhase2GetPartialYHat(List<CommonResponse> phase1Responses) {
        List<CommonRequest> res = new ArrayList<>();
        Message body;

        signedByteArray[][] yHatPartEncLst = new signedByteArray[phase1Responses.size()][];

        int cnt = 0;
        for (CommonResponse response : phase1Responses) {
            yHatPartEncLst[cnt++] = ((CypherMessage) response.getBody()).getBody();
        }

        // broadcast, single d_final for all clients
        body = new CypherMessage(computeDEnc(yHatPartEncLst));

        for (CommonResponse entry : phase1Responses) {
            ClientInfo client = entry.getClient();
            CommonRequest request = new CommonRequest(client, body);
            request.setPhase(phase);
            res.add(request);
        }
        return res;
    }

    private signedByteArray[] computeDEnc(signedByteArray[][] yHatPartLst) {
        signedByteArray[] yHatAll = pheKeys.getAllZero(yHatPartLst[0].length);

        for (signedByteArray[] signedByteArrays : yHatPartLst) {
            yHatAll = pheKeys.add(signedByteArrays, yHatAll, pheKeys.getPk());
        }
        signedByteArray[] d = pheKeys.add(yHatAll,
                IntStream.range(0, yHatAll.length)
//                        .parallel()
                        .mapToDouble(i -> -1*h[i]).toArray(),
                pheKeys.getPk());

        if(useFakeDec) {
            fullLoss = 0;
            for(signedByteArray elem : d) {
                fullLoss += toDouble(elem)*toDouble(elem);
            }
        }
        return d;
    }

    /**
     * Training phase 3: compute final gradient
     */
    private List<CommonRequest> controlPhase3ComputeGEnc(List<CommonResponse> response2) {
        List<CommonRequest> res = new ArrayList<>();
        signedByteArray[][] gNonPrivPartLstEnc;
        signedByteArray[][] gPrivLstEnc;
        int cnt = 0;

        gNonPrivPartLstEnc = new signedByteArray[response2.size()][];
        gPrivLstEnc = new signedByteArray[response2.size()][];
        for (CommonResponse response : response2) {
            // 得到 client 返回的  g_AB_part
            if(!((TwoCypherMessage) response.getBody()).getFirst().isEmpty()) {
                gNonPrivPartLstEnc[cnt] = ((TwoCypherMessage) response.getBody()).getFirst().getBody();
            }
            // 得到 client 返回的 g_priv
            gPrivLstEnc[cnt] = ((TwoCypherMessage) response.getBody()).getSecond().getBody();
            cnt += 1;
        }
        // 1) Master 先计算全局的 g_nonPriv
        signedByteArray[] gABEnc  = computeNonPrivGEnc(gNonPrivPartLstEnc);

        // 2) Master 再将 全局的 g_nonPriv 和 全局的 g_priv 加在一起
        for (signedByteArray[] gTmp : gPrivLstEnc) {
            gABEnc = pheKeys.add(gABEnc, gTmp, pheKeys.getPk());
        }

        // 3) a) 由于paillier无法做div(暂不支持任意精度小数，精度不够),聚合后不对最终的梯度求平均, 而是返回各个参与方解密后在求。
        //    b) 在phase3 加入 clientFeatRandWeight 的初始化. 与非加密版保持一致。
        //    c) 对g进行mask，返回各方.
        if (clientFeatRandWeight == null) {
            clientFeatRandWeight = new HashMap<>();
            for (CommonResponse entry : response2) {
                double[] tmpp = new double[M];
                int i;
                for (i = 0; i < M - 1; i++) {
                    // if this client does NOT OWN this feature, mask with a random number;
                    // if this client OWNs this feature, then mockSend it back with NO change.
                    tmpp[i] = 1d; // FIXME: should make this a random value. To make debug easier, I use a fixed value here.
                }
                tmpp[i] = 1d;
                clientFeatRandWeight.put(entry.getClient().getIp()+entry.getClient().getPort(), tmpp);
            }
        }

        for (CommonResponse entry : response2) {
            // 4) Mask each returned g with its owner's weight
            signedByteArray[] retG = new signedByteArray[gABEnc.length];
            arrayCopy(gABEnc, 0, retG, 0, gABEnc.length);
            retG = pheKeys.mul(
                    retG,
                    clientFeatRandWeight.get(entry.getClient().getIp()+entry.getClient().getPort()),
                    pheKeys.getPk()
            );
            // 对每个client返回所有feature对应的值
            ClientInfo client = entry.getClient();
            CommonRequest request = new CommonRequest(client, new CypherMessage(retG));
            request.setPhase(phase);
            res.add(request);
        }
        return res;
    }

    private signedByteArray[] computeNonPrivGEnc(signedByteArray[][] gABPartLst) {
        if (N ==0) {
            if(M==0) {
                try {
                    throw new Exception("N and M are both 0.");
                } catch (Exception e) {
                    logger.error("error", e);
                }
            }else {
                return pheKeys.getAllZero(M);
            }
        }
        signedByteArray[] gABEnc = pheKeys.getAllZero(gABPartLst[0].length);
        for (signedByteArray[] gABPart : gABPartLst) {
            gABEnc = pheKeys.add(gABEnc, gABPart, pheKeys.getPk());
        }
        for (int j = 0; j < M; j++) {
            gABEnc[j] = pheKeys.mul(gABEnc[j], 2d , pheKeys.getPk());
        }
        return gABEnc;
    }

    private void updateHessInv(String[][] bfgsImResAll, String[] retClientLst) {
        Matrix hInvMat;
        if (iterNum != 0) {
            // parse bfgs im_res and update Hessian matrix
            double rho = 0d;
            double[][] s_yTranspose = new double[M][M];
            double[][] y_sTranspose = new double[M][M];
            double[][] s_sTranspose = new double[M][M];

            /*
             * NOTE:each client have complete masked info, so adding them up is not necessary.
             */
            int cnt = 0;
            for (String[] bfgs_im_res : bfgsImResAll) {
                double[] rho_tmp = parse1dDouble(bfgs_im_res[0]);
                double[][] s_yTranspose_tmp = parse2dDouble(bfgs_im_res[1]);
                double[][] y_sTranspose_tmp = parse2dDouble(bfgs_im_res[2]);
                double[][] s_sTranspose_tmp = parse2dDouble(bfgs_im_res[3]);

                assert (s_yTranspose_tmp.length == s_yTranspose.length && s_yTranspose_tmp[0].length == s_yTranspose[0].length &&
                        y_sTranspose_tmp.length == y_sTranspose.length && y_sTranspose_tmp[0].length == y_sTranspose[0].length &&
                        s_sTranspose_tmp.length == s_sTranspose.length && s_sTranspose_tmp[0].length == s_sTranspose[0].length);

                /*
                 * weight_matrix_y_sTranspose and weight_matrix_s_sTranspose are identical when
                 * using the following equation (e.x. when rand_weight_of_g == rand_weight_of_w).
                 * but when rand_weight_of_g and rand_weight_of_w are different, they will be different.
                 */
                double[][] weight_matrix_y_sTranspose = new double[M][M];
                double[][] weight_matrix_s_yTranspose = new double[M][M];
                double[][] weight_matrix_s_sTranspose = new double[M][M];
                for (int i = 0; i < M; i++) {
                    for (int j = 0; j < M; j++) {
                        weight_matrix_y_sTranspose[i][j] = clientFeatRandWeight.get(retClientLst[cnt])[i] * clientFeatRandWeight.get(retClientLst[cnt])[j];
                    }
                }
                for (int i = 0; i < M; i++) {
                    for (int j = 0; j < M; j++) {
                        weight_matrix_s_yTranspose[i][j] = clientFeatRandWeight.get(retClientLst[cnt])[j] * clientFeatRandWeight.get(retClientLst[cnt])[i];
                    }
                }
                for (int i = 0; i < M; i++) {
                    for (int j = 0; j < M; j++) {
                        weight_matrix_s_sTranspose[i][j] = clientFeatRandWeight.get(retClientLst[cnt])[i] * clientFeatRandWeight.get(retClientLst[cnt])[j];
                    }
                }
                double[] weight_vec = new double[M];
                for (int i = 0; i < M; i++) {
                    weight_vec[i] = clientFeatRandWeight.get(retClientLst[cnt])[i] * clientFeatRandWeight.get(retClientLst[cnt])[i];
                }
                s_yTranspose = MathExt.elemwiseInvMul(s_yTranspose_tmp, weight_matrix_s_yTranspose);
                y_sTranspose = MathExt.elemwiseInvMul(y_sTranspose_tmp, weight_matrix_y_sTranspose);
                s_sTranspose = MathExt.elemwiseInvMul(s_sTranspose_tmp, weight_matrix_s_sTranspose);

                for (int i = 0; i < weight_vec.length; i++) {
                    rho += rho_tmp[i] * (1d / weight_vec[i]);
                }
                break;
            }

            rho = 1d / (rho + 1E-80);
            Matrix I = DenseMatrix.Factory.eye(M, M);
            Matrix dLeft = I.minus(Matrix.Factory.linkToArray(s_yTranspose).times(rho));
            Matrix dRight = I.minus(Matrix.Factory.linkToArray(y_sTranspose).times(rho));
            Matrix dAdd = Matrix.Factory.linkToArray(s_sTranspose).times(rho);
            hInvMat = dLeft.mtimes(Matrix.Factory.linkToArray(hessInv)).mtimes(dRight).plus(dAdd);

            // only for debug
//            hInvMat = DenseMatrix.Factory.eye(M, M);
        } else {
            hInvMat = DenseMatrix.Factory.eye(M, M);
        }
        hessInv = hInvMat.toDoubleArray();
    }

    private List<CommonRequest> controlPhase5UpdateHessInv(List<CommonResponse> responses) {
        String[] retClientLst = new String[responses.size()];
        String[][] bfgsImResAll = new String[responses.size()][];

        signedByteArray[] gAvg = new signedByteArray[gEnc.length];
        if(N==0) {
            String[] responseBody = ((StringArray) responses.get(0).getBody()).getData();
            signedByteArray[] retedSb = parse1dSByteArr(responseBody[1]);
            for (int j = 0; j < retedSb.length; j++) {
                gAvg[j] = retedSb[j].deep_copy();
            }
            for (int i = 0; i < responses.size(); i++) {
                retClientLst[i] = responses.get(i).getClient().getIp()+responses.get(i).getClient().getPort();
                bfgsImResAll[i] = parse1dString(responseBody[0]);
            }
        } else {
            for (int i = 0; i < responses.size(); i++) {
                String[] responseBody = ((StringArray) responses.get(i).getBody()).getData();

                double[] featMap = clientFeatMap.get(responses.get(i).getClient()).clone();
                signedByteArray[] retedSB = parse1dSByteArr(responseBody[1]);
                for (int j = 0; j < retedSB.length; j++) {
                    if ((featMap[j] > 0) && (gAvg[j] == null)) {
                        gAvg[j] = retedSB[j].deep_copy();
                    }
                }
                retClientLst[i] = responses.get(i).getClient().getIp()+responses.get(i).getClient().getPort();
                bfgsImResAll[i] = parse1dString(responseBody[0]);
            }
        }

        arrayCopy(gAvg, 0, gEnc, 0, gEnc.length);
        updateHessInv(bfgsImResAll, retClientLst);
        signedByteArray[] deltW = pheKeys.getAllZero(M);
        for (int j = 0; j < M; j++) {
            deltW[j] = pheKeys.innerProduct(gEnc, hessInv[j], pheKeys.getPk());
            if(!useFakeDec) {
                deltW[j] = pheKeys.mul(deltW[j], -1d * p.getEta() * 10, pheKeys.getPk());
                weightEnc[j] = pheKeys.mul(weightEnc[j], 10, pheKeys.getPk()); // increase precision of weight_enc by one decimal place.
            } else {
                deltW[j] = pheKeys.mul(deltW[j], -1d * p.getEta(), pheKeys.getPk());
            }
        }
        weightEnc = pheKeys.add(weightEnc, deltW, pheKeys.getPk());

        List<CommonRequest> res = new ArrayList<>();
        for (CommonResponse entry : responses) {

            // NOTE : Mask each returned w with its owner's weight!!!
            signedByteArray[] retW = new signedByteArray[weightEnc.length];
            arrayCopy(weightEnc, 0, retW, 0, weightEnc.length);
            retW = pheKeys.mul(retW, clientFeatRandWeight.get(entry.getClient().getIp()+entry.getClient().getPort()), pheKeys.getPk());

            // 对每个client返回所有feature对应的值
            ClientInfo client = entry.getClient();
            CommonRequest request = new CommonRequest(client, new CypherMessage(retW));
            request.setPhase(phase);
            res.add(request);
        }
        return res;
    }

    private List<CommonRequest> controlPhase7ReportTrainRes(List<CommonResponse> responses) {
        // do a check first: to ensure each weight entry is returned at least once.
        int[] vecValueMapCheck = new int[M];
        double privLoss = 0d;

        double[] fullG = new double[M];
        double[] fullW = new double[M];
        for (CommonResponse response : responses) {
            String[] msg = ((StringArray)response.getBody()).getData();
            double[] wPart = parse1dDouble(msg[0]);
            double[] gPart = parse1dDouble(msg[1]);
            int[] vec_value_map_tmp = parse1dInt(msg[2]);
            fullG = MathExt.elemwiseAdd(fullG, gPart);
            fullW = MathExt.elemwiseAdd(fullW, wPart);
            vecValueMapCheck = elemwiseAdd(vecValueMapCheck, vec_value_map_tmp);

            privLoss += Double.parseDouble(msg[4]);
        }
        fullG = elemwiseInvMul(fullG, vecValueMapCheck);
        fullW = elemwiseInvMul(fullW, vecValueMapCheck);

        for (int elem : vecValueMapCheck) {assert elem > 0;}
        assert (weightEnc.length == M );

        // compute full loss
        // get re-encrypted weight from clients.
        for (CommonResponse response : responses) {
            int[] vec_value_map = parse1dInt(((StringArray)response.getBody()).getData()[2]);
            signedByteArray[] tmp = parse1dSByteArr(((StringArray)response.getBody()).getData()[3]);
            for (int i = 0; i < M; i++) {
                if (vec_value_map[i] != 0) {
                    weightEnc[i] = tmp[i].deep_copy();
                }
            }
        }
        String res  = "\niterNum = " + iterNum;
        if(useFakeDec){
            fullLoss += privLoss;
            fullLoss /= fullN;
            res = "\n\tTotal loss = " + fullLoss;

        } else {
            res +=" \n\tTotal loss not applicable,";
        }
        res  =  res
                + "\tG_maganituide =  " + Tool.L2Norm(fullG)
                + "\n\tg = " + toJsons(Tool.roundTo4( (fullG) ))
                + "\n\tweight = " + toJsons(Tool.roundTo4( (fullW) ));

        Map<MetricType, Double> allMetrics = new HashMap<>();
        allMetrics.put(MetricType.G_L2NORM, Tool.L2Norm(fullG));
        for (MetricType metricType : p.getMetricType()) {
            logger.info(res);
            if (metricMap.containsKey(metricType)) {
                metricMap.get(metricType).add(new Pair<>(iterNum, allMetrics.get(metricType)));
            } else {
                List<Pair<Integer, Double>> metric = new ArrayList<>();
                metric.add(new Pair<>(iterNum, allMetrics.get(metricType)));
                metricMap.put(metricType, metric);
            }
        }

        iterNum += 1;
        logger.info(res);
        return geneEmptyReq(responses, phase);
    }

    public List<CommonRequest> initInference(List<ClientInfo> clientInfos, String[] predictUid, Map<String, Object> others) {
        if(!useFakeDec) {
            DistributedPaillier.DistPaillierPubkey pubkey = new DistributedPaillier.DistPaillierPubkey();
            pubkey.parseJson((String)others.get("pubKeyStr"));
            this.pheKeys.setPk(pubkey);
        }
        // Debug mode Key initialization
        if(debugMode && !useFakeDec) {
            // 生成一份 priv/pubkey 后, 保存为文件, 然后从文件加载. 方便debug.
            decHelper = new HomoEncryptionDebugUtil(pheKeys.getPk(), pheKeys.getSkAll(), numP, Long.MAX_VALUE);
            decHelper.saveToFile("dist_pai_keys_3_1024");
        }

        this.phase = -1;
        int cnt = 1;
        List<CommonRequest> initRequests = new ArrayList<>();
        String[] clientList = clientInfos.stream().map(x-> x.getIp()+x.getPort()).toArray(String[]::new);
        for (ClientInfo clientInfo : clientInfos) {

            Map<String, Object> extraParamsFromMaster = new HashMap<>();

            extraParamsFromMaster.put("clientList", clientList);
            extraParamsFromMaster.put("selfPort", clientInfo.getIp()+clientInfo.getPort());

            extraParamsFromMaster.put("numP", p.getNump());
            extraParamsFromMaster.put("encBits", p.getEncBits());
            extraParamsFromMaster.put("thisPartyID", cnt);

            InferenceInit inferenceInit = new InferenceInit(predictUid, extraParamsFromMaster);
            CommonRequest request = new CommonRequest(clientInfo,  inferenceInit, phase);
            initRequests.add(request);

            cnt += 1;
        }
        return initRequests;
    }

    /**
     * Inference Part
     */
    public List<CommonRequest> inferenceControl(List<CommonResponse> response) {
        List<CommonRequest> ret;
        phase = getNextPhase(phase);
        if(phase == -2) {
            ret =  inferIdMatchingPhase1GetIllegalData(response);
        } else if (phase == -3) {
            ret = inferIdMatchingPhase2(response);
        }
        else if (phase == -4) {
            ret = geneEmptyReq(response, phase);
        } else if (phase == -5) {
            ret = inferPhase2SumOverAllYHatP(response);
        } else if (phase == -6) {
            if(N==0) {
                return geneEmptyReq(response, phase);
            }
            ret = bCastPartialDecRes(response);
        } else if (phase == -7) {
            ret = geneEmptyReq(response, phase);
        }
        else {
            throw new UnsupportedOperationException();
        }
        return ret;
    }

    /**
     * In MixedLinReg, if an ID does NOT exist in ANY PARTY, it is illegal. Will return NAN as
     * final result.
     *
     * @param responses instance ID that does not exist on certain party
     */
    private List<CommonRequest> inferIdMatchingPhase1GetIllegalData(List<CommonResponse> responses) {
        Set<Integer> overlap = new HashSet<>();
        int cnt = 0;
        for (CommonResponse response : responses) {
            InferenceInitRes init = (InferenceInitRes) response.getBody();
            Set<Integer> uid = Arrays.stream(init.getUid()).boxed().collect(Collectors.toSet());
            if(cnt == 0) {
                overlap.addAll(uid);
            } else {
                overlap.retainAll(uid);
            }
            cnt ++;
        }
        int[] overlapInt = overlap.stream().mapToInt(Number::intValue).toArray();
        List<CommonRequest> res = new ArrayList<>();
        for (CommonResponse entry : responses) {
            ClientInfo client = entry.getClient();
            CommonRequest request = new CommonRequest(client, new IntArray(overlapInt), phase);
            res.add(request);
        }
        return res;
    }

    private List<CommonRequest> inferIdMatchingPhase2(List<CommonResponse> responses) {
        List<CommonRequest> res = new ArrayList<>();
        Map<ClientInfo, LinregMatchAlg.MixedLinRegIdMappingRes> matchRes;

        LinRegMatcher matcher = new LinRegMatcher(false, p);
        try {
            matchRes  = matcher.masterMatchingPhase(responses);
        } catch (Exception e) {
            matchRes = new HashMap<>();
            logger.error("inferIdMatchingPhase2 error", e);
        }

        Map<String, Map<String,Object>> masterAndClientParams = matcher.collectInferParams(matchRes);
        this.N = (int) masterAndClientParams.get("master").get("N") ;
        for (CommonResponse entry : responses) {
            ClientInfo client = entry.getClient();
            CommonRequest request = new CommonRequest(client,
                    ((LinearRegressionInferInitOthers) masterAndClientParams.get("client")
                            .get(client.getIp()+client.getPort())),
                    phase);
            res.add(request);
        }
        return res;
    }

    private List<CommonRequest> inferPhase2SumOverAllYHatP(List<CommonResponse> responses) {

        List<CommonRequest> res = new ArrayList<>();
        signedByteArray[] yHatEnc = null;
        if(N>0) {
            for (CommonResponse response : responses) {
                signedByteArray[] yHatPartEnc = ((CypherMessage) response.getBody()).getBody();
                if (yHatEnc == null) {
                    yHatEnc = pheKeys.getAllZero(yHatPartEnc.length);
                }
                yHatEnc = pheKeys.add(yHatEnc, yHatPartEnc, pheKeys.getPk());
            }
        }

        for (CommonResponse entry : responses) {
            ClientInfo client = entry.getClient();
            CommonRequest request = new CommonRequest(client, new CypherMessage(yHatEnc), phase);
            request.setPhase(phase);
            res.add(request);
        }
        return res;
    }

    @Override
    public PredictRes postInferenceControl(List<CommonResponse> responses) {
        // TODO: 这个函数从所有方手机 yHat 并返回给master，这是错的。因为在混合数据的情况下各方得到的y值不一样(存在 PrivData )
        // TODO: 需要在Client端加一个postInferenceControl并返回各自的 yHat
        logger.debug("postInferenceControl");
        List<Double>  yHat = new ArrayList<>();
        for (CommonResponse response : responses) {
            Double2dArray res = (Double2dArray) response.getBody();
            for(int i = 0; i < res.getListData().get(2).length; i++) {
                yHat.add(res.getListData().get(2)[i]);
            }
        }
        return new PredictRes( new String[]{"label"}, yHat.stream().mapToDouble(d -> d).toArray());
    }

    /**
     * Some util functions
     */
    public int getNextPhase(int old) {
        //推理阶段
        if (old < 0) {
            if(phase == -3 && N==0) {
                return -6;
            }
            return old - 1;
        } else {
            // 训练阶段
            if(idMatchDone) {
                if(phase == 3 && N ==0){
                    return 5;
                }
                if (old < trainPhaseArray[trainPhaseArray.length - 1]) {
                    return old + 1;
                } else {
                    return trainPhaseArray[0]+2;
                }
            } else {
                if (old < trainPhaseArray[trainPhaseArray.length - 1]) {
                    return old + 1;
                } else {
                    return trainPhaseArray[0];
                }
            }
        }
    }

    private List<CommonRequest> bCastPartialDecRes(List<CommonResponse> responses) {
        // 每个client会返回两个List（其他client的解密) 这一步提取信息
        // imDecResToOneParty.shape = [response2.size(), 2, numDim]
        Map<String, List<signedByteArray[]>> imDecResToOneParty = new HashMap<>();

        // 对于发来消息的client
        for (CommonResponse response : responses) {
            Map<String, signedByteArray[]> imDecResFromOneParty = ((PartialDecMessage) response.getBody()).getBody();

            // 对于每个密文接收方（主方）
            for (CommonResponse entry : responses) {
                String key = entry.getClient().getIp() + entry.getClient().getPort();
                List<signedByteArray[]> encList = imDecResToOneParty.getOrDefault(key, new ArrayList<>());
                // 加入其他方传来的密文
                if (imDecResFromOneParty.get(key) != null) {
                    encList.add(imDecResFromOneParty.get(key));
                }
                imDecResToOneParty.put(key, encList);
            }
        }
        // 对每个client转发两个List（其他client的解密）给到主方解密， 这一步做转发
        List<CommonRequest> res = new ArrayList<>();
        for (CommonResponse entry : responses) {
            ClientInfo client = entry.getClient();
            List<signedByteArray[]> sendMsg_2 = new ArrayList<>(imDecResToOneParty
                    .get(client.getIp()+client.getPort()));
            if(sendMsg_2.size()!=numP-1) {
                throw new NotMatchException("Need to mockSend " + (numP-1) + " to client " + client.getIp()+client.getPort() +
                        ", but got " + sendMsg_2.size());
            }
            CommonRequest request = new CommonRequest(client, new CypherMessageList(sendMsg_2), phase);
            res.add(request);
        }
        return res;
    }

    // only used for unit test
    public void setPhase(int p){
        this.phase = p;
    }

    public boolean isInferenceContinue() {
        return phase > inferPhaseArray[inferPhaseArray.length - 1];
    }

    public MetricValue readMetrics() {
        return  new MetricValue(metricMap);
    }

    public boolean isContinue() {
        if (fullLoss < this.p.getMinLoss()) {
            return false;
        }
        return iterNum < p.getMaxEpoch();
    }


    public String serialize() {
        // master端无modelToken，暂时填一个定值
        return "modelToken=" + 1 + "\n" +
                "M=" + M + "\n" +
                "numP=" + numP + "\n";
    }

    public static class LinRegTrainInitParams {

        public final LinearParameter p;
        public final int numP;
        public final int M;
        public final int N;
        public final int fullN;
        public final double[] h;
        public final int N_priv;
        public final Map<ClientInfo, double[]> clientFeatMap;
        public final int encMode;
        public final boolean isInitialized;

        public LinRegTrainInitParams(int numP, int M, int N, int N_priv,
                                     int encMode,
                                     LinearParameter params, double[] h,
                                     int full_N, Map<ClientInfo, double[]> clientFeatMap) {
            this.p = params;
            this.M = M + 1; //传入的M仍然为原始数据的维度，此处的M应当是 原始数据的维度 + 1
            this.N = N;
            this.numP = numP;
            this.isInitialized = false;
            this.encMode = encMode;
            this.fullN = full_N;
            this.h = h;
            this.N_priv = N_priv;
            this.clientFeatMap = clientFeatMap;
        }
    }

    public AlgorithmType getAlgorithmType(){
        return algorithmType;
    }

    public void setClientFeatRandWeight(Map<String, double[]> clientFeatRandWeight) {
        this.clientFeatRandWeight = clientFeatRandWeight;
    }

    public void setIterNum(int iterNum) {
        this.iterNum = iterNum;
    }

    private static void reportMasterPhaseTime(double start, int phase, int iterNum) {
        LinearRegression.logger.info("Master " + "\tphase = " + phase + "\titernum = "+iterNum);
        LinearRegression.logger.info("\t data transferred = " + (double) 0 + " KB");
        LinearRegression.logger.info("\t time spent = " + (System.currentTimeMillis() - start )/ 1000.0);
    }

    public void setKeyForTest(DistributedPaillier.DistPaillierPubkey pk,
                              DistributedPaillier.DistPaillierPrivkey sk,
                              DistributedPaillier.DistPaillierPrivkey[] skAll) {
        this.pheKeys.setPk(pk);
        this.pheKeys.setSk(sk);
        this.pheKeys.setSkAll(skAll);
        // Debug mode Key initialization
        if(debugMode && !useFakeDec) {
            decHelper = new HomoEncryptionDebugUtil(pheKeys.getPk(), pheKeys.getSkAll(), numP, Long.MAX_VALUE);
        }
    }
}
