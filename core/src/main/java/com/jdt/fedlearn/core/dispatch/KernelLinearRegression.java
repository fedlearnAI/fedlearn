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
import com.jdt.fedlearn.core.entity.common.*;
import com.jdt.fedlearn.core.entity.kernelLinearRegression.*;
import com.jdt.fedlearn.core.parameter.KernelLinearRegressionParameter;


import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.core.type.*;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.core.type.data.Pair;

import com.jdt.fedlearn.core.type.MetricType;

import com.jdt.fedlearn.core.entity.feature.Features;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.IntStream;
import java.util.stream.Collectors;

public class KernelLinearRegression implements Control {

    private static final AlgorithmType algorithmType = AlgorithmType.KernelBinaryClassification;
    public List<ClientInfo> clientInfoList;
    public Features localFeature;
    private KernelLinearRegressionParameter parameter;
    private static final Logger logger = LoggerFactory.getLogger(KernelLinearRegression.class);
    private Map<String, Integer> mapInferenceOrder = new HashMap<>();
    private String[] originIdArray;
    private int numSamples = -1;
    private int round = 0;
    private int numClient = 0;
    private int clientInd = 0;
    private double tr_loss = 0.0;

    private String modelToken;
    private String splitLine = "========================================================";

    private boolean isInitTrain = false;
    private boolean inferenceFinish = false;
    private boolean isminiBatch = false;
    private ArrayList<Double> prediction;
    private Map<MetricType, List<Pair<Integer, Double>>> metricMap = new HashMap<>();
    private List<Integer> sampleIndex;
    private int phase = 0;
    private int[] idIndexArray;
    private double[] predict;

    public KernelLinearRegression(KernelLinearRegressionParameter parameter) {
        this.parameter = parameter;
    }

    public KernelLinearRegression(int numSamples) {
        this.numSamples = numSamples;
    }


    public KernelLinearRegression() {
        logger.info("Parameter loading ..." + splitLine);
        this.parameter = new KernelLinearRegressionParameter();
    }

    public List<CommonRequest> initControl(List<ClientInfo> clientInfos,
                                           MatchResult idMap,
                                           Map<ClientInfo, Features> features,
                                           Map<String, Object> other) {
        logger.info(String.format("Init control, the client inputs are %s.", clientInfos));
        this.clientInfoList = clientInfos;
//        this.modelToken = modelToken.getTrainId();
        this.numClient = clientInfos.size();

        List<CommonRequest> res = new ArrayList<>();
        for (ClientInfo clientInfo : clientInfos) {
            if (numSamples == -1) {
                // 切 feature 的联邦学习可以用这种方法或缺样本数
                numSamples = idMap.getLength();
            }
        }
        logger.info(String.format("Training sample number is %s", numSamples));
        sampleIndex = IntStream.range(0, numSamples).boxed().collect(Collectors.toList());
        //Collections.shuffle(sampleIndex);
        for (ClientInfo clientInfo : clientInfos) {
            Features localFeature = features.get(clientInfo);
            other.put("sampleIndex", sampleIndex);
//            other.put("dataset", clientInfo.getDataset());
            TrainInit nested_req = new TrainInit(parameter, localFeature, idMap.getMatchId(), other);
            //KernelLinearRegressionInitReq nested_req = new KernelLinearRegressionInitReq(clientInfo, parameter, thisIdMap, sampleIndex, localFeature, modelToken);
            CommonRequest request = CommonRequest.buildTrainInitial(clientInfo, nested_req);
            res.add(request);
        }
        logger.info(String.format("Request content %s", res));
        return res;
    }


    //TODO 后续将full control 与phaseArray 结合，自动运行
    @Override
    public List<CommonRequest> control(List<CommonResponse> response) {
        phase = getNextPhase(phase, response);
        String trainId = 1 + getAlgorithmType().toString();
        logger.info(String.format("phase value %s", phase));

        switch (KernelDispatchPhaseType.valueOf(phase)) {
            case COMPUTE_LOSS_METRIC:
                return controlPhase1(trainId, response);
            case EMPTY_REQUEST:
                round += 1;
                dataShuffle(numSamples);
                return controlPhase2(trainId, response);
            default:
                throw new UnsupportedOperationException();
        }

    }

    //Phase 1: master mockSend request to ask all passive parties compute w*x on local machines.
    public List<CommonRequest> controlPhase1(String trainId, List<CommonResponse> response) {
        logger.info("Algo phase 0 on master machine" + splitLine);
        List<CommonRequest> commonRequests = new ArrayList<>();
        int batchSize = numSamples; //Math.min(parameter.getBatchSize(), numSamples);
        logger.info(String.format("Batch size: %d", batchSize));
        double[] sumvec = new double[batchSize];
        int i;
        tr_loss = 0.0;
        for (i = 0; i < batchSize; i++) {
            sumvec[i] = 0;
        }
        //int clientInd = (int) Math.floor(Math.random() * numClient);
        //logger.info(String.format("Choose machine %s", clientInd));
        clientInd += 1;
        if (clientInd >= clientInfoList.size()) {
            clientInd = 0;
        }
        logger.info(String.format("Chose client %s", clientInd));
        for (CommonResponse response_i : response) {
            TrainRes res = (TrainRes) response_i.getBody();
//            ArrayList<Double> temp = res.getVector();
            double[] temp = res.getVector();
//            logger.info(String.format("The length of vector is %s", temp.size()));
            for (i = 0; i < batchSize; i++) {
                sumvec[i] += temp[i];
            }
        }
        for (i = 0; i < batchSize; i++) {
            tr_loss += Math.pow(sumvec[i], 2);
        }
        tr_loss /= batchSize;
        round += 1;
        logger.info("====round====" + round + "=====full loss:");
        for (MetricType metricType : parameter.fetchMetric()) {
            logger.info("metric " + metricType.getMetric() + " : value = " + tr_loss);
            if (metricMap.containsKey(metricType)) {
                metricMap.get(metricType).add(new Pair<>(round, tr_loss));
            } else {
                List<Pair<Integer, Double>> metric = new ArrayList<>();
                metric.add(new Pair<>(round, tr_loss));
                metricMap.put(metricType, metric);
            }
        }
        for (CommonResponse response_i : response) {
            ClientInfo info = response_i.getClient();
            if (info.equals(clientInfoList.get(clientInd))) {
                logger.info(String.format("Find selected client %s", info));
                TrainReq req = new TrainReq(response_i.getClient(), sumvec, sampleIndex, true);
                commonRequests.add(new CommonRequest(response_i.getClient(), req, phase));
            } else {
                TrainReq req = new TrainReq(response_i.getClient(), sumvec, sampleIndex, false);
                commonRequests.add(new CommonRequest(response_i.getClient(), req, phase));
            }
        }
        logger.info("Algo phase 1 end" + splitLine);
        return commonRequests;
    }

    //Phase 1: master mockSend request to ask all passive parties compute w*x on local machines.
    public List<CommonRequest> controlPhase2(String trainId, List<CommonResponse> response) {
        logger.info("Algo phase 2 processing on master" + splitLine);
        List<CommonRequest> commonRequests = new ArrayList<>();

        // 发送kernel type 等相关参数给各机器
        for (CommonResponse response_i : response) {

            TrainReq req = new TrainReq(response_i.getClient(), sampleIndex);
            commonRequests.add(new CommonRequest(response_i.getClient(), req, phase));
        }
        logger.info("Algo phase 2 end" + splitLine);
        return commonRequests;
    }

    public List<CommonRequest> initInference(List<ClientInfo> clientInfos, String[] predictUid,Map<String, Object> others) {
        phase = -255;
        originIdArray = predictUid;
        clientInfoList = clientInfos;
        predict = new double[originIdArray.length];
        idIndexArray = IntStream.range(0, predictUid.length).toArray();
        List<CommonRequest> initRequests = new ArrayList<>();
        for (ClientInfo clientInfo : clientInfoList) {
            InferenceInit init = new InferenceInit(originIdArray);
            CommonRequest request = new CommonRequest(clientInfo, init, phase);
            initRequests.add(request);
        }
        return initRequests;
    }


    @Override
    public List<CommonRequest> inferenceControl(List<CommonResponse> response) {
        phase = getNextPhase(phase, response);
        String trainId = 1 + "_" + getAlgorithmType().getAlgorithm(); //TODO
        logger.info(String.format("Phase %s", phase));
        switch (KernelDispatchPhaseType.valueOf(phase)) {
            case INFERENCE_FILTER:
                return inferenceControlPhase0(trainId, response);
            case INFERENCE_EMPTY_REQUEST:
                return inferencecontrolPhase1(trainId, response);
            case INFERENCE_EMPTY_REQUEST_1:
                return inferenceconrolPhase2(trainId, response);
            case INFERENCE_RESULT:
                return inferenceconrolPhase2(trainId, response);
            default:
                throw new UnsupportedOperationException();
        }
    }


    private List<CommonRequest> inferenceControlPhase0(String trainId, List<CommonResponse> responses) {
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
            inferenceFinish = true;
            Arrays.fill(predict, Double.NaN);
            inferenceFinish = true;
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
            InferenceInit init = new InferenceInit(idArray);
            CommonRequest request = new CommonRequest(response.getClient(), init, phase);
            res.add(request);
        }
        return res;
    }

    public List<CommonRequest> inferencecontrolPhase1(String trainId, List<CommonResponse> response) {
        logger.info("Algo phase 1 start" + splitLine);
        List<CommonRequest> commonRequests = new ArrayList<>();
        // 发送kernel type 等相关参数给各机器
        for (CommonResponse response_i : response) {
            InferenceReqAndRes req = new InferenceReqAndRes(response_i.getClient());
            commonRequests.add(new CommonRequest(response_i.getClient(), req, phase));
        }
        logger.info("Algo phase 1 end" + splitLine);
        return commonRequests;
    }

    public List<CommonRequest> inferenceconrolPhase2(String trainId, List<CommonResponse> response) {
        List<CommonRequest> commonRequests = new ArrayList<>();
        for (CommonResponse response_i : response) {
            //InferenceReqAndRes res = new InferenceReqAndRes();
            InferenceReqAndRes req = new InferenceReqAndRes(response_i.getClient());
            commonRequests.add(new CommonRequest(response_i.getClient(), req, phase));
        }
        return commonRequests;
    }

    @Override
    public MetricValue readMetrics() {
        Map<MetricType, List<Pair<Integer, Double>>> metricMap = new HashMap<>();
        List<Pair<Integer, Double>> metric = new ArrayList<>();
        metric.add(new Pair<>(round, tr_loss));
        metricMap.put(MetricType.TRAINLOSS, metric);
        return new MetricValue(metricMap);
    }

    private void dataShuffle(int numSamples) {
        sampleIndex = IntStream.range(0, numSamples).boxed().collect(Collectors.toList());
        //Collections.shuffle(sampleIndex);
    }

    public int getNextPhase(int phaseindex, List<CommonResponse> responses) {
        if (phaseindex == -255) {
            phaseindex = -1;
            return phaseindex;
        }
        if (phaseindex == -1) {
            phaseindex = -2;
            return phaseindex;
        }
        if (phaseindex == -2) {
            phaseindex = -3;
//            inferenceFinish = true;
            return phaseindex;
        }
        if (phaseindex == -3) {
            phaseindex = -4;
            inferenceFinish = true;
            return phaseindex;
        }

        if (phaseindex == 0) {
            phaseindex = 1;
            return phaseindex;
        }
        // 传树到client = 99
        if (phaseindex == 1) {
            phaseindex = 2;
            return phaseindex;
        }
        if (phaseindex == 2) {
            phaseindex = 1;
            return phaseindex;
        }
        if (phaseindex == 3) {
            phaseindex = 2;
            return phaseindex;
        }
        return phaseindex;
    }

    public int getNextPhaseInference(int phaseindex) {
        if (phaseindex == -1) {
            phaseindex = -2;
            return phaseindex;
        }
        if (phaseindex == -2) {
            phaseindex = -3;
            inferenceFinish = true;
            return phaseindex;
        }

        return phaseindex;
    }

    public PredictRes postInferenceControl(List<CommonResponse> responses) {
        //每个预测样本一个预测值
        logger.info("Post Inference Control...");
        int i;
//        double[] predict = new double[originIdArray.length];
        logger.info(String.format("Result aggregation %s samples!", predict.length));
        for (CommonResponse response_i : responses) {
            if (idIndexArray.length != 0 && response_i.getBody() != null) {
                InferenceReqAndRes res = (InferenceReqAndRes) response_i.getBody();
                Map<String, Double> values = res.getPredict();
                List<Integer> idSet = Arrays.stream(idIndexArray).boxed().collect(Collectors.toList());
                for (i = 0; i < predict.length; i++) {
                    if (idSet.contains(i)) {
                        predict[i] += values.get(originIdArray[i]);
                    } else {
                        predict[i] = Double.NaN;
                    }
                }
            }
        }
        return new PredictRes(new String[]{"label"}, predict);
    }

    public boolean isInferenceContinue() {
        if (!inferenceFinish) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isContinue() {
        if (round <= parameter.getMaxIter()) {
            logger.info("check stop");
            logger.info("return true");
            return true;
        } else {
            logger.info("check stop");
            logger.info("return false");
            return false;
        }
    }

    public AlgorithmType getAlgorithmType() {
        return algorithmType;
    }

}
