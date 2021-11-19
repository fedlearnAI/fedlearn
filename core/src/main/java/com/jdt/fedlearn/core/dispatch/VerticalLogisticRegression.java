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

package com.jdt.fedlearn.core.dispatch;

import com.jdt.fedlearn.core.dispatch.common.Control;
import com.jdt.fedlearn.core.encryption.common.Ciphertext;
import com.jdt.fedlearn.core.encryption.common.EncryptionTool;
import com.jdt.fedlearn.core.encryption.common.PrivateKey;
import com.jdt.fedlearn.core.encryption.common.PublicKey;
import com.jdt.fedlearn.core.encryption.paillier.PaillierTool;
import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.core.entity.base.DoubleArray;
import com.jdt.fedlearn.core.entity.base.EmptyMessage;
import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.core.entity.base.SingleElement;
import com.jdt.fedlearn.core.entity.base.StringArray;
import com.jdt.fedlearn.core.entity.common.*;
import com.jdt.fedlearn.common.entity.core.feature.Features;
import com.jdt.fedlearn.core.entity.verticalLinearRegression.*;
import com.jdt.fedlearn.core.model.common.loss.LogisticLoss;
import com.jdt.fedlearn.core.optimizer.BatchGD;
import com.jdt.fedlearn.core.optimizer.Newton;
import com.jdt.fedlearn.core.optimizer.Optimizer;
import com.jdt.fedlearn.core.optimizer.StochasticGD;
import com.jdt.fedlearn.core.parameter.VerticalLRParameter;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.core.type.*;
import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.OptimizerType;
import com.jdt.fedlearn.core.type.data.Pair;
import com.jdt.fedlearn.core.util.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.jdt.fedlearn.core.type.MetricType.CROSS_ENTRO;
import static com.jdt.fedlearn.core.type.MetricType.G_L2NORM;


/**
 * 随机全梯度下降方法
 * 改进：进行到一部分的时候即更新权重
 * TODO 全部使用向量操作
 */
public class VerticalLogisticRegression implements Control {
    private static final Logger logger = LoggerFactory.getLogger(VerticalLogisticRegression.class);
    private static final AlgorithmType algorithmType = AlgorithmType.VerticalLR;
    private static final int[] PHASE_ARRAY = new int[]{1, 2, 3, 4};
    private boolean isInferenceContinue = true;
    private double fullLoss = Double.MAX_VALUE;
    private int epoch = 0;
    private final VerticalLRParameter parameter;
    EncryptionTool encryptionTool = new PaillierTool();
    private PrivateKey privateKey = encryptionTool.keyGenerate(256, 64);
    private final PublicKey publicKey = privateKey.generatePublicKey();
    private final Optimizer optimizer;
    private final Map<MetricType, List<Pair<Integer, Double>>> metricMap = new HashMap<>();
    private double gL2Norm;
    private int phase = 0;
    private int[] idIndexArray;
    private String[] originIdArray;
    private List<ClientInfo> clientInfoList;
    private double[] predict;

    public VerticalLogisticRegression(VerticalLRParameter parameter) {
        this.parameter = parameter;
        if (parameter.getOptimizer().equals(OptimizerType.NEWTON)) {
            optimizer = new Newton();
        } else if (parameter.getOptimizer().equals(OptimizerType.StochasticGD)) {
            optimizer = new StochasticGD(parameter.getEta());
        } else {
            optimizer = new BatchGD(parameter.getEta());
        }
    }

    @Override
    public List<CommonRequest> initControl(List<ClientInfo> clientInfos, MatchResult idMap,
                                           Map<ClientInfo, Features> features,
                                           Map<String, Object> other) {
        List<CommonRequest> initRequests = new ArrayList<>();
        for (ClientInfo clientInfo : clientInfos) {
            if (parameter.getOptimizer().equals(OptimizerType.StochasticGD)) {
                other.put("pubKey", publicKey.serialize());
//                other.put("dataset", clientInfo.getDataset());
                TrainInit trainInit = new TrainInit(parameter, features.get(clientInfo), idMap.getMatchId(), other);
                CommonRequest request = CommonRequest.buildTrainInitial(clientInfo, trainInit);
                initRequests.add(request);
            } else {
                other.put("pubKey", publicKey.serialize());
//                other.put("dataset", clientInfo.getDataset());
                TrainInit trainInit = new TrainInit(parameter, features.get(clientInfo), idMap.getMatchId(), other);
                CommonRequest request = CommonRequest.buildTrainInitial(clientInfo, trainInit);
                initRequests.add(request);
            }
        }
        return initRequests;
    }

    //TODO 后续将full control 与phaseArray 结合，自动运行
    @Override
    public List<CommonRequest> control(List<CommonResponse> response) {
        phase = getNextPhase(phase);
        String trainId = 1 + getAlgorithmType().toString();
        switch (VerLRDispatchPhaseType.valueOf(phase)) {
            case UPDATE_METRIC:
                return controlPhase1(trainId, response);
            case SEND_LOSS:
                return controlPhase2(trainId, response);
            case SEND_GRADIENTS:
                return controlPhase3(trainId, response);
            case UPDATE_GRADIENTS:
                return controlPhase4(trainId, response);
            default:
                throw new UnsupportedOperationException();
        }
    }

    public int getNextPhase(int old) {
        //推理阶段
        if (old < 0) {
            return old - 1;
        } else if (old > 0) {
            if (old < PHASE_ARRAY[PHASE_ARRAY.length - 1]) {
                return old + 1;
            } else {
                return 1;
            }
        }
        return 1;
    }

    private List<CommonRequest> controlPhase0(String trainId, List<CommonResponse> responses) {
        List<CommonRequest> requests = new ArrayList<>();
        for (CommonResponse response : responses) {
            LinearP1Request req = new LinearP1Request(response.getClient(), true, publicKey.serialize());
            CommonRequest request = new CommonRequest(response.getClient(), req, phase);
            requests.add(request);
        }
        return requests;
    }

    private List<CommonRequest> controlPhase1(String trainId, List<CommonResponse> responses) {
        Message message = responses.stream().findAny().get().getBody();
        SingleElement singleElement = new SingleElement("asdf ");
        if (message instanceof SingleElement) {
            singleElement = (SingleElement) responses.stream().findAny().get().getBody();
            if (singleElement.getElement().contains("init_success")) {
                return controlPhase0(trainId, responses);
            }
        }

        List<CommonRequest> requests = new ArrayList<>();
        //TODO 目前先用检查是否有pubKey，以后改为更好的判断首次请求和后续请求的方式
        if (singleElement.getElement().contains("pubKey")) {
            for (CommonResponse response : responses) {
                CommonRequest request = new CommonRequest(response.getClient(), singleElement, phase);
                requests.add(request);
            }
        } else {
            Map<MetricType, Double> allLoss = null;
            for (CommonResponse respons : responses) {
                GradientsMetric se = (GradientsMetric) respons.getBody();
                allLoss = se.getMetric();
                CommonRequest req = new CommonRequest(respons.getClient(), new EmptyMessage(), phase);
                requests.add(req);
            }

            //可替换为 avg loss
            this.epoch += 1;
            logger.info("====epoch====" + epoch + "=====" +
                    "\nfull loss:" + fullLoss + "\ngL2Norm : " + gL2Norm);
            for (MetricType metricType : parameter.getMetricType()) {
                assert allLoss != null;
                logger.info("metric " + metricType.getMetric() + " : value = " + allLoss.get(metricType));
                if (metricMap.containsKey(metricType)) {
                    metricMap.get(metricType).add(new Pair<>(epoch, allLoss.get(metricType)));
                } else {
                    List<Pair<Integer, Double>> metric = new ArrayList<>();
                    metric.add(new Pair<>(epoch, allLoss.get(metricType)));
                    metricMap.put(metricType, metric);
                }
            }
        }
        return requests;
    }

    //phase 2, parameter broadcast
    private List<CommonRequest> controlPhase2(String trainId, List<CommonResponse> phase1) {
        List<CommonRequest> res = new ArrayList<>();

        List<LinearP1Response> body = phase1.stream()
                .map(x -> ((LinearP1Response) x.getBody())).collect(Collectors.toList());
        for (CommonResponse entry : phase1) {
            ClientInfo client = entry.getClient();

            LinearP2Request request2 = new LinearP2Request(client, body);
//            request2.setClient(client);
//            request2.setBodies(body);
            CommonRequest request = new CommonRequest(client, request2, phase);
            res.add(request);
        }
        return res;
    }

    //
    private List<CommonRequest> controlPhase3(String trainId, List<CommonResponse> response2) {
        List<CommonRequest> res = new ArrayList<>();

        LossGradients response = response2
                .stream()
                .map(x -> (LossGradients) (x.getBody()))
                .filter(x -> (x.getGradient() != null)).findFirst().get();

        double[] lossXi = Arrays.stream(response.getLoss())
                .mapToDouble(e -> encryptionTool.decrypt(encryptionTool.restoreCiphertext(e), privateKey)).toArray();
        double fullLossTmp = 0d;
        for (int i = 0; i < lossXi.length; i++) {
            if (lossXi[i] >= 1) {
                lossXi[i] = 1d;
            }
            if (lossXi[i] <= 0) {
                lossXi[i] = 1E-12; // TODO: may need to adjust this
            }
            fullLossTmp += -1 * Math.log(lossXi[i]);
        }
        fullLoss = fullLossTmp;

        // clapping sigmoidWxMinusY between -1 and 1
        double[] sigmoidWxMinusY = Arrays.stream(response.getGradient())
                .mapToDouble(e -> encryptionTool.decrypt(encryptionTool.restoreCiphertext(e), privateKey)).toArray();
        Ciphertext[] sigmoidWxMinusYEnc = new Ciphertext[sigmoidWxMinusY.length];
        for (int i = 0; i < sigmoidWxMinusY.length; i++) {
            if (sigmoidWxMinusY[i] >= 1) {
                sigmoidWxMinusY[i] = 1d;
            }
            if (sigmoidWxMinusY[i] <= -1) {
                sigmoidWxMinusY[i] = -1d;
            }
            sigmoidWxMinusYEnc[i] = encryptionTool.encrypt(sigmoidWxMinusY[i], publicKey);
        }
        String[] yMinusSigmoidWxStr = IntStream.range(0, sigmoidWxMinusY.length).boxed().map(i -> sigmoidWxMinusYEnc[i].serialize()).toArray(String[]::new);


        for (CommonResponse b : response2) {
            ClientInfo client = b.getClient();
            LossGradients request3 = new LossGradients(b.getClient(), null, yMinusSigmoidWxStr);
//            request3.setClient(b.getClient());
//            request3.setDifference(yMinusSigmoidWxStr); // request3.difference = response.gradient()
//            request3.setLoss(null);
            CommonRequest request = new CommonRequest(client, request3, phase);
            res.add(request);
        }
        return res;
    }

    //
    private List<CommonRequest> controlPhase4(String trainId, List<CommonResponse> response3) {
        List<CommonRequest> res = new ArrayList<>();
        double[][] allGradients = new double[response3.size()][];
        for (int i = 0; i < response3.size(); i++) {
            CommonResponse b = response3.get(i);
            LossGradients response = (LossGradients) (b.getBody());
            String[] secGrad = response.getGradient();
            double[] gradients = new double[secGrad.length];
            for (int j = 0; j < gradients.length; j++) {
                gradients[j] = encryptionTool.decrypt(encryptionTool.restoreCiphertext(secGrad[j]), privateKey);
            }
            gL2Norm = Tool.L2Norm(gradients);
            allGradients[i] = gradients;
        }
        Map<MetricType, Double> trainMetric = new HashMap<>();
        trainMetric.put(CROSS_ENTRO, fullLoss);
        trainMetric.put(G_L2NORM, gL2Norm);
        allGradients = optimizer.getGlobalUpdate(allGradients);
        for (int i = 0; i < response3.size(); i++) {
            CommonResponse b = response3.get(i);
            ClientInfo client = b.getClient();
            GradientsMetric request4 = new GradientsMetric(b.getClient(), allGradients[i], trainMetric);
//            request4.setClient(b.getClient());
//            request4.setGradients(allGradients[i]);
//            request4.setMetric(trainMetric);
            CommonRequest request = new CommonRequest(client, request4, phase);
            res.add(request);
        }
        return res;
    }


    public List<CommonRequest> initInference(List<ClientInfo> clientInfos, String[] predictUid,Map<String, Object> others) {
        phase = -1;
        originIdArray = predictUid;
        predict = new double[originIdArray.length];
        idIndexArray = IntStream.range(0, predictUid.length).toArray();
        clientInfoList = clientInfos;
        InferenceInit init = new InferenceInit(originIdArray);
        return clientInfoList.parallelStream().map(clientInfo -> new CommonRequest(clientInfo, init, phase)).collect(Collectors.toList());

    }

    @Override
    public List<CommonRequest> inferenceControl(List<CommonResponse> responses) {
        phase = getNextPhase(phase);
        String trainId = 1 + "_" + getAlgorithmType().getAlgorithm();
        switch (VerLRDispatchPhaseType.valueOf(phase)) {
            case PREDICT_RESULT:
                return inferenceControlPhase1(trainId, responses);
            case EMPTY_REQUEST:
                return inferenceControlPhase2(trainId, responses);
            default:
                throw new UnsupportedOperationException();
        }
    }

    private List<CommonRequest> inferenceControlPhase1(String trainId, List<CommonResponse> responses) {
//构造请求
        //tep3 推断过程预处理，返回无需预测的uid索引
        Set<Integer> blacklist = new HashSet<>();
        for (CommonResponse response : responses) {
            InferenceInitRes inferenceInitRes = (InferenceInitRes) (response.getBody());
            //TODO 根据 isAllowList判断
            final List<Integer> result = Arrays.stream(inferenceInitRes.getUid()).boxed().collect(Collectors.toList());
            blacklist.addAll(result);
        }
        // 判断需要预测的uid数量
        final int existUidSize = originIdArray.length - blacklist.size();
        // 特殊情况，所有的ID都不需要预测
        if (existUidSize == 0) {
            isInferenceContinue = true;
            Arrays.fill(predict, Double.NaN);
        }

        // 过滤不需要预测的uid, filterSet返回的位置，所以根据位置过滤
        List<Integer> queryIdHasFiltered = new ArrayList<>();
        for (int i = 0; i < originIdArray.length; i++) {
            if (!blacklist.contains(i)) {
                queryIdHasFiltered.add(i);
            }
        }
        idIndexArray = queryIdHasFiltered.stream().mapToInt(x -> x).toArray();
        String[] idArray = Arrays.stream(idIndexArray).mapToObj(x -> originIdArray[x]).toArray(String[]::new);
        //构造请求
        List<CommonRequest> res = new ArrayList<>();
        for (CommonResponse response : responses) {
            StringArray init = new StringArray(idArray);
            CommonRequest request = new CommonRequest(response.getClient(), init, phase);
            res.add(request);
        }
        return res;
    }

    private List<CommonRequest> inferenceControlPhase2(String modelId, List<CommonResponse> responses1) {
        List<CommonRequest> commonRequests = new ArrayList<>();
        for (CommonResponse response : responses1) {
            DoubleArray linearN1Response = (DoubleArray) response.getBody();

            CommonRequest request = new CommonRequest(response.getClient(), linearN1Response, phase);
            commonRequests.add(request);
        }
        isInferenceContinue = false;
        return commonRequests;
    }

    @Override
    public PredictRes postInferenceControl(List<CommonResponse> responses1) {
        for (CommonResponse response : responses1) {
            DoubleArray r = (DoubleArray) response.getBody();
            double[] uArray = r.getData();
            List<Integer> idSet = Arrays.stream(idIndexArray).boxed().collect(Collectors.toList());
            for (int i = 0; i < predict.length; i++) {
                if (idSet.contains(i)) {
                    int index = idSet.indexOf(i);
                    predict[i] += uArray[index];
                } else {
                    predict[i] = Double.NaN;
                }
            }
        }
        LogisticLoss logisticLoss = new LogisticLoss();
        for (int i = 0; i < predict.length; i++) {
            if (!Double.isNaN(predict[i])) {
                predict[i] = logisticLoss.sigmoidApprox(predict[i]);
            }
        }
        return new PredictRes(new String[]{"label"}, predict);
    }

    @Override
    public boolean isContinue() {
        return fullLoss >= parameter.getMinLoss() && epoch < parameter.getMaxEpoch();
    }

    @Override
    public MetricValue readMetrics() {
        return new MetricValue(metricMap);
    }

    @Override
    public boolean isInferenceContinue() {
        return isInferenceContinue;
    }

    public void setKeyPair(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    public AlgorithmType getAlgorithmType() {
        return algorithmType;
    }

    public void setPhase(int phase) {
        this.phase = phase;
    }
}
