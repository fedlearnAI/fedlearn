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
import com.jdt.fedlearn.core.encryption.common.EncryptionTool;
import com.jdt.fedlearn.core.encryption.common.PrivateKey;
import com.jdt.fedlearn.core.encryption.javallier.JavallierTool;
import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.core.entity.base.DoubleArray;
import com.jdt.fedlearn.core.entity.base.SingleElement;
import com.jdt.fedlearn.core.entity.base.StringArray;
import com.jdt.fedlearn.core.entity.common.*;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.core.type.*;
import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;
import com.jdt.fedlearn.core.type.data.Pair;
import com.jdt.fedlearn.core.metrics.Metric;
import com.jdt.fedlearn.core.optimizer.BatchGD;
import com.jdt.fedlearn.core.optimizer.Newton;
import com.jdt.fedlearn.core.optimizer.Optimizer;
import com.jdt.fedlearn.core.optimizer.StochasticGD;
import com.jdt.fedlearn.common.entity.core.feature.Features;
import com.jdt.fedlearn.core.parameter.VerticalLinearParameter;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.OptimizerType;
import com.jdt.fedlearn.core.entity.verticalLinearRegression.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * 随机全梯度下降方法
 * 改进：进行到一部分的时候即更新权重
 * TODO 全部使用向量操作
 */
public class VerticalLinearRegression implements Control {
    private static final Logger logger = LoggerFactory.getLogger(VerticalLinearRegression.class);
    private static final AlgorithmType algorithmType = AlgorithmType.VerticalLinearRegression;
    private static final int[] PHASE_ARRAY = new int[]{1, 2, 3, 4};
    private boolean isInferenceContinue = true;
    private double fullLoss = Double.MAX_VALUE;
    private int epoch = 0;
    private VerticalLinearParameter parameter;
    private EncryptionTool encryptionTool = new JavallierTool();
    private PrivateKey privateKey = encryptionTool.keyGenerate(256, 64);
    private String publicKey = privateKey.generatePublicKey().serialize();
    private Optimizer optimizer;
    private Map<MetricType, List<Pair<Integer, Double>>> metricMap = new HashMap<>();
    private int[] idIndexArray;
    private String[] originIdArray;
    private List<ClientInfo> clientInfoList;
    private int phase = 0;
    private String modelToken;
    private double[] predict;


    public void setPhase(int phase) {
        this.phase = phase;
    }

    public VerticalLinearRegression(VerticalLinearParameter parameter) {
        this.parameter = parameter;
        if (parameter.getOptimizer().equals(OptimizerType.NEWTON)) {
            optimizer = new Newton();
        } else if (parameter.getOptimizer().equals(OptimizerType.StochasticGD)) {
            optimizer = new StochasticGD(parameter.getEta());
        } else {
            optimizer = new BatchGD(parameter.getEta());
        }
    }

    public VerticalLinearRegression(int epoch, VerticalLinearParameter parameter, Map<MetricType, List<Pair<Integer, Double>>> metricMap) {
        // 仅仅用于测试
        this.epoch = epoch;
        this.parameter = parameter;
        this.metricMap = metricMap;
    }


    public VerticalLinearRegression(EncryptionTool encryptionTool, PrivateKey privateKey, VerticalLinearParameter parameter) {
        this.encryptionTool = encryptionTool;
        this.privateKey = privateKey;
        this.parameter = parameter; // 仅仅用于测试 fix optimizer
        optimizer = new BatchGD(parameter.getEta());
    }

    public VerticalLinearRegression(VerticalLinearParameter parameter, int[] idIndexArray, int phase, String[] originIdArray,
                                    double[] predict) {
        // 仅仅用于测试
        this.parameter = parameter;
        this.idIndexArray = idIndexArray;
        this.phase = phase;
        this.originIdArray = originIdArray;
        this.predict = predict;
    }

    /**
     * 为每一个client生成TrainInit的Request
     *
     * @param clientInfos 客户端列表
     * @param idMap       共有id的index_id map
     * @param features    客户和对应feature map
     * @param other       其他自定义参数
     * @return
     */
    @Override
    public List<CommonRequest> initControl(List<ClientInfo> clientInfos, MatchResult idMap, Map<ClientInfo, Features> features, Map<String, Object> other) {
        List<CommonRequest> initRequests = new ArrayList<>();
        for (ClientInfo clientInfo : clientInfos) {
            // StochasticGD Optimizer不需要setPhase?
            if (parameter.getOptimizer().equals(OptimizerType.StochasticGD)) {
                other.put("pubKey", publicKey);
//                other.put("dataset", clientInfo.getDataset());
                TrainInit trainInit = new TrainInit(parameter, features.get(clientInfo), idMap.getMatchId(), other);
                CommonRequest request = CommonRequest.buildTrainInitial(clientInfo, trainInit);
                initRequests.add(request);
            } else {
                other.put("pubKey", publicKey);
//                other.put("dataset", clientInfo.getDataset());
                TrainInit trainInit = new TrainInit(parameter, features.get(clientInfo), idMap.getMatchId(), other);
                CommonRequest request = CommonRequest.buildTrainInitial(clientInfo, trainInit);
                request.setPhase(phase);
                initRequests.add(request);
            }
        }
        return initRequests;
    }

    //TODO 后续将full control 与phaseArray 结合，自动运行
    @Override
    public List<CommonRequest> control(List<CommonResponse> response) {
        phase = getNextPhase(phase, response);
        String trainId = 1 + getAlgorithmType().toString();
        switch (VerLinDispatchPhaseType.valueOf(phase)) {
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

    public int getNextPhase(int old, List<CommonResponse> responses) {
        //推理阶段
        if (old == -255) {
            return -1;
        } else if (old == -1) {
            return -2;
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
            LinearP1Request req = new LinearP1Request(response.getClient(), true, publicKey);
//            req.setNewIter(true);
//            req.setPubKey();
            CommonRequest request = new CommonRequest(response.getClient(), req, phase);
            requests.add(request);
        }
        return requests;
    }


    private List<CommonRequest> controlPhase1(String trainId, List<CommonResponse> responses) {
        Message message = responses.stream().findAny().get().getBody();
        if (message instanceof SingleElement && ((SingleElement) message).getElement().contains("init_success")) {
            return controlPhase0(trainId, responses);
        }
        List<CommonRequest> requests = new ArrayList<>();
        Map<MetricType, Double> allLoss = null;
        for (CommonResponse respons : responses) {
            Message str = respons.getBody();
            allLoss = ((GradientsMetric) (str)).getMetric();
            CommonRequest req = new CommonRequest(respons.getClient(), new LinearP1Request(respons.getClient(), false, publicKey), phase);
            requests.add(req);
        }

        //可替换为 avg loss
        this.epoch += 1;
        logger.info("====epoch====" + epoch + "=====full loss:");
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
        return requests;
    }

    //phase 2, parameter broadcast
    private List<CommonRequest> controlPhase2(String trainId, List<CommonResponse> phase1) {
        List<CommonRequest> res = new ArrayList<>();

        List<LinearP1Response> body = phase1.stream().map(x -> (LinearP1Response) x.getBody()).collect(Collectors.toList());
        for (CommonResponse entry : phase1) {
            ClientInfo client = entry.getClient();
            LinearP2Request request2 = new LinearP2Request(client, body);
            CommonRequest request = new CommonRequest(client, request2, phase);
            res.add(request);
        }
        return res;
    }

    //将[[d]], [[L]]分发给各个客户端
    private List<CommonRequest> controlPhase3(String trainId, List<CommonResponse> response2) {
        List<CommonRequest> res = new ArrayList<>();

        LossGradients response = response2.stream().map(x -> (LossGradients) x.getBody()).filter(x -> (x.getGradient() != null)).findFirst().get();
        for (CommonResponse b : response2) {
            ClientInfo client = b.getClient();
            LossGradients request3 = new LossGradients(b.getClient(), response.getLoss(), response.getGradient());
            CommonRequest request = new CommonRequest(client, request3, phase);
            res.add(request);
        }
        return res;
    }

    //
    private List<CommonRequest> controlPhase4(String trainId, List<CommonResponse> response3) {
        List<CommonRequest> res = new ArrayList<>();
        double[][] allGradients = new double[response3.size()][];
        Map<MetricType, Double> trainMetric = new HashMap<>();
        for (int i = 0; i < response3.size(); i++) {
            CommonResponse b = response3.get(i);
            LossGradients response = (LossGradients) (b.getBody());
            String[] secDiff = response.getLoss();
            double[] globalDiff = Arrays.stream(secDiff).mapToDouble(e -> encryptionTool.decrypt(encryptionTool.restoreCiphertext(e), privateKey)).toArray();
//            logger.info("globalDiff: " + Arrays.toString(globalDiff));
            trainMetric = Metric.calculateMetricFromGlobalDiff(parameter.fetchMetric(), globalDiff);
            String[] secGrad = response.getGradient();
            double[] gradients = new double[secGrad.length];
            for (int j = 0; j < gradients.length; j++) {
                gradients[j] = encryptionTool.decrypt(encryptionTool.restoreCiphertext(secGrad[j]), privateKey);
            }
            allGradients[i] = gradients;
        }
        allGradients = optimizer.getGlobalUpdate(allGradients);
        for (int i = 0; i < response3.size(); i++) {
            CommonResponse b = response3.get(i);
            ClientInfo client = b.getClient();
            GradientsMetric request4 = new GradientsMetric(b.getClient(), allGradients[i], trainMetric);
            CommonRequest request = new CommonRequest(client, request4, phase);
            res.add(request);
        }
        return res;
    }

    /**
     * broadcast需要predict的uid到所有客户端
     *
     * @param clientInfos 客户端列表，包含是否有label，
     * @param predictUid  需要推理的uid
     * @return
     */
    public List<CommonRequest> initInference(List<ClientInfo> clientInfos, String[] predictUid,Map<String, Object> others) {
        phase = -255;
        originIdArray = predictUid;
        predict = new double[originIdArray.length];
        idIndexArray = IntStream.range(0, predictUid.length).toArray();
        clientInfoList = clientInfos;
        InferenceInit init = new InferenceInit(originIdArray);
        return clientInfoList.parallelStream().map(clientInfo -> new CommonRequest(clientInfo, init, phase)).collect(Collectors.toList());
    }


    @Override
    public List<CommonRequest> inferenceControl(List<CommonResponse> responses) {
        phase = getNextPhase(phase, responses);
        String trainId = 1 + "_" + getAlgorithmType().getAlgorithm();
        switch (VerLinDispatchPhaseType.valueOf(phase)) {
            case PREDICT_RESULT:
                return inferenceControlPhase1(trainId, responses);
            case EMPTY_REQUEST:
                return inferenceControlPhase2(trainId, responses);
            default:
                throw new UnsupportedOperationException();
        }
    }


    private List<CommonRequest> inferenceControlPhase1(String trainId, List<CommonResponse> responses) {
        // 推断过程预处理，返回无需预测的uid索引
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
        // 需要预测的uid对应index
        idIndexArray = queryIdHasFiltered.stream().mapToInt(x -> x).toArray();
        // 需要预测的uid的原始id
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


    private List<CommonRequest> inferenceControlPhase2(String modelToken, List<CommonResponse> responses1) {
        List<CommonRequest> commonRequests = new ArrayList<>();
        for (CommonResponse response : responses1) {
            CommonRequest request = new CommonRequest(response.getClient(), response.getBody(), phase);
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
            // 需要预测的uid对应index
            List<Integer> idSet = Arrays.stream(idIndexArray).boxed().collect(Collectors.toList());
            for (int i = 0; i < predict.length; i++) {
                if (idSet.contains(i)) {
                    int index = idSet.indexOf(i);
                    predict[i] += uArray[index];
                } else {
                    predict[i] = Double.NaN; // 只要有一方没有这个推理样本，则pred=NaN
                }
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

//    public void setKeyPair(PrivateKey privateKey) {
//        this.privateKey = privateKey;
//    }

    public AlgorithmType getAlgorithmType() {
        return algorithmType;
    }

    public void setForTest(Map<MetricType, List<Pair<Integer, Double>>> metricMap, List<ClientInfo> clientInfos) {
        this.metricMap = metricMap;
        this.clientInfoList = clientInfos;
    }

}
