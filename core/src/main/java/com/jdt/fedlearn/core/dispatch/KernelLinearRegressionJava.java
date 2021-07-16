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
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.base.SingleElement;
import com.jdt.fedlearn.core.entity.common.*;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.entity.kernelLinearRegression.*;
import com.jdt.fedlearn.core.math.MathExt;
import com.jdt.fedlearn.core.model.KernelLinearRegressionJavaModel;
import com.jdt.fedlearn.core.parameter.KernelLinearRegressionParameter;
import com.jdt.fedlearn.core.preprocess.TrainTestSplit;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.core.type.*;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.data.Pair;
import com.jdt.fedlearn.core.type.data.Tuple2;
import com.jdt.fedlearn.core.util.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class KernelLinearRegressionJava implements Control {

    private static final AlgorithmType algorithmType = AlgorithmType.KernelBinaryClassificationJava;
    public List<ClientInfo> clientInfoList;
    private KernelLinearRegressionParameter parameter;
    private static final Logger logger = LoggerFactory.getLogger(KernelLinearRegressionJava.class);
    private Map<String, Integer> mapInferenceOrder = new HashMap<>();
    private String[] originIdArray;
    private int numSamples = -1;
    private int round = 0;
    private int numClient = 0;
    private int clientInd = 0;
    private String modelToken;
    private String splitLine = "========================================================";

    private boolean isInitTrain = false;
    private boolean inferenceFinish = false;
    private boolean isminiBatch = false;
    private Map<MetricType, List<Pair<Integer, Double>>> metricMap = new HashMap<>();
    private Map<MetricType, List<Pair<Integer, String>>> metricMapArr = new HashMap<>();
    private Map<MetricType, List<Pair<Integer, Double>>> metricMapVali = new HashMap<>();
    private Map<MetricType, List<Pair<Integer, String>>> metricMapArrVali = new HashMap<>();
    private List<Integer> sampleIndex;
    private int phase = 0;
    private int[] idIndexArray;
    private double[] predict;
    private double[][] predicts;
    private int numClass;
    private int numClassRound = 0;
    private List<Integer> testUId = new ArrayList<>();  // TODO 是否需要改回String待确定
    private int bestRound = 0;
    private MetricValue metricValue;
    List<String> headerList = new ArrayList<>();
    List<Double> multiClassUniqueLabelList;
    private int earlyStoppingRounds;

    public KernelLinearRegressionJava(KernelLinearRegressionParameter parameter) {
        this.parameter = parameter;
    }


    public KernelLinearRegressionJava() {
        logger.info("Parameter loading ..." + splitLine);
        this.parameter = new KernelLinearRegressionParameter();
    }

    public List<CommonRequest> initControl(List<ClientInfo> clientInfos,
                                           MatchResult idMap,
                                           Map<ClientInfo, Features> features,
                                           Map<String, Object> other) {
        logger.info(String.format("Init control, the client inputs are %s.", clientInfos));
        this.clientInfoList = clientInfos;
        this.numClient = clientInfos.size();
        this.numClass = parameter.getNumClass();
        this.earlyStoppingRounds = parameter.getEarlyStoppingRounds();
        double splitRatio = (double) other.get("splitRatio");
        if (numClass > 1) {
            clientInd = 1;
        }
        Tuple2<List<Integer>, List<Integer>> trainTestSplit = TrainTestSplit.trainTestSplit(idMap.getLength(), splitRatio, 666);
        assert trainTestSplit != null;
        // test index id
        // TODO 这里修改之前储存的是加密后的validation的ID list；后面匹配需要加密还是非加密状态
        testUId = trainTestSplit._2();
        List<CommonRequest> res = new ArrayList<>();
        if (numSamples == -1) {
            numSamples = trainTestSplit._1().size();
        }
        logger.info(String.format("Training sample number is %s", numSamples));
        sampleIndex = IntStream.range(0, numSamples).boxed().collect(Collectors.toList());
        int[] testIndex = testUId.stream().mapToInt(Integer::valueOf).toArray();
        for (ClientInfo clientInfo : clientInfos) {
            Features localFeature = features.get(clientInfo);
            other.put("sampleIndex", sampleIndex);
            other.put("testUid", testUId);
            TrainInit nested_req = new TrainInit(parameter, localFeature, idMap.getMatchId(), other, testIndex);
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
        logger.info(String.format("phase value %s", phase));
        switch (KernelDispatchJavaPhaseType.valueOf(phase)) {
            case UPDATE_METRIC:
                dataShuffle(numSamples);
                return controlPhase1(response);
            case COMPUTE_LOSS:
                return controlPhase2(response);
            case VALIDATION_INIT:
                return controlPhase3(response);
            case VALIDATION_FILTER:
                return inferenceControlPhase0(response);
            case EMPTY_REQUEST:
                return inferencecontrolPhase1(response);
            case EMPTY_REQUEST_1:
                return inferencecontrolPhase1(response);
            case VALIDATION_RESULT:
                return inferencecontrolPhase99(response);
            default:
                throw new UnsupportedOperationException();
        }
    }

    //Phase 1: master send request to ask all passive parties compute w*x on local machines.
    public List<CommonRequest> controlPhase1(List<CommonResponse> responses) {
        logger.info("Algo phase 2 processing on master" + splitLine);
        List<CommonRequest> commonRequests = new ArrayList<>();
        for (CommonResponse response : responses) {
            Message message = response.getBody();
            if (message instanceof SingleElement || !((TrainRes) message).getActive()) {
                for (CommonResponse res : responses) {
                    TrainReq req = new TrainReq(res.getClient(), sampleIndex, numClassRound, bestRound);
                    commonRequests.add(new CommonRequest(res.getClient(), req, phase));
                }
                return commonRequests;
            }
            TrainRes res = (TrainRes) message;
            numClassRound = res.getNumClassRound();
            Map<MetricType, List<Double>> metric = res.getMetric();
            Map<MetricType, List<Double>> metricVali = res.getMetricVali();
            Map<MetricType, List<Double[][]>> metricArr = res.getMetricArr();
            Map<MetricType, List<Double[][]>> metricArrVali = res.getMetricArrVali();
            if (metric == null || metric.size() == 0) {
                continue;
            }
            metricMap = getMetricValue(metric);
            StringBuilder trainMetric = new StringBuilder(String.format("KernelJava round %d,%n", round - 1));
            for (Map.Entry<MetricType, List<Pair<Integer, Double>>> e : metricMap.entrySet()) {
                trainMetric.append(String.format("                train-%s:%.15f%n", e.getKey(), e.getValue().get(round - 1).getValue()));
            }
            logger.info(trainMetric.toString());
            if (metricArr != null && !metricArr.isEmpty()) {
                metricMapArr = getArrMetricValue(metricArr);
            }
            if (metricVali == null || metricVali.size() == 0) {
                continue;
            }
            metricMapVali = getMetricValue(metricVali);
            StringBuilder metricOutput = new StringBuilder(String.format("KernelJava vali round %d,%n", round - 1));
            for (Map.Entry<MetricType, List<Pair<Integer, Double>>> e : metricMapVali.entrySet()) {
                metricOutput.append(String.format("                train-%s:%.15f%n", e.getKey(), e.getValue().get(round - 1).getValue()));
            }
            if (numClass <= 2) {
                logger.info(metricOutput.toString());
            }
            if (metricArrVali != null && !metricArrVali.isEmpty()) {
                metricMapArrVali = getArrMetricValue(metricArrVali);
            }
            //TODO erlystopping round and metrictype
            for (Map.Entry<MetricType, List<Double>> entry : metricVali.entrySet()) {
                int tmpround = entry.getValue().size();
                if (tmpround > earlyStoppingRounds + 1 && entry.getKey().equals(MetricType.RMSE)) {
                    List<Double> lossMetric = entry.getValue();
                    bestRound = Tool.earlyStopping(lossMetric, earlyStoppingRounds);
                    logger.info("maxIn " + bestRound);
                }
            }
        }
        for (CommonResponse response : responses) {
            TrainReq req = new TrainReq(response.getClient(), sampleIndex, numClassRound, bestRound);
            commonRequests.add(new CommonRequest(response.getClient(), req, phase));
        }
        logger.info("Algo phase 2 end" + splitLine);
        return commonRequests;
    }

    /**
     * 获取一维指标
     *
     * @param metricMap 一维指标
     * @return 指标结果
     */
    private Map<MetricType, List<Pair<Integer, Double>>> getMetricValue(Map<MetricType, List<Double>> metricMap) {
        Map<MetricType, List<Pair<Integer, Double>>> resMap = new HashMap<>();
        for (Map.Entry<MetricType, List<Double>> entry : metricMap.entrySet()) {
            round = entry.getValue().size();
            List<Pair<Integer, Double>> tmpRoundMetric = IntStream.range(0, round).boxed().parallel().map(i ->
                    new Pair<>(i, entry.getValue().get(i))).collect(Collectors.toList());
            resMap.put(entry.getKey(), tmpRoundMetric);
        }
        return resMap;
    }

    /**
     * 获取二维指标
     *
     * @param metricArrMap 二维指标
     * @return 指标结果
     */
    private Map<MetricType, List<Pair<Integer, String>>> getArrMetricValue(Map<MetricType, List<Double[][]>> metricArrMap) {
        Map<MetricType, List<Pair<Integer, String>>> resArrMap = new HashMap<>();
        for (Map.Entry<MetricType, List<Double[][]>> entry : metricArrMap.entrySet()) {
            round = entry.getValue().size();
            List<Pair<Integer, String>> tmpRoundMetric = IntStream.range(0, round).boxed().parallel().map(i ->
                    new Pair<>(i, "[" + Tool.getMetricArr(entry.getValue().get(i)) + "]")).collect(Collectors.toList());
            resArrMap.put(entry.getKey(), tmpRoundMetric);
        }
        return resArrMap;
    }

    //Phase 1: master send request to ask all passive parties compute w*x on local machines.
    public List<CommonRequest> controlPhase2(List<CommonResponse> responses) {
        List<CommonRequest> commonRequests = new ArrayList<>();
        int batchSize = numSamples;//Math.min(parameter.getBatchSize(), numSamples);
        double[][] sumvec = new double[numClass][batchSize];

        for (CommonResponse response : responses) {
            TrainRes res = (TrainRes) response.getBody();
            if (res.getRound() == parameter.getMaxIter() + 1) {
                round = res.getRound();
                responses.forEach(x -> commonRequests.add(new CommonRequest(x.getClient(), new SingleElement("earlyStopping success"), phase)));
                return commonRequests;
            }
            double[][] temp = res.getVectors();
            for (int j = 0; j < numClass; j++) {
                for (int i = 0; i < batchSize; i++) {
                    sumvec[j][i] += temp[j][i];
                }
            }
        }
        for (CommonResponse response : responses) {
            ClientInfo info = response.getClient();
            if (info.equals(clientInfoList.get(clientInd))) {
                logger.info(String.format("Find selected client %s", info));
                TrainReq req = new TrainReq(response.getClient(), sumvec, sampleIndex, true);
                commonRequests.add(new CommonRequest(response.getClient(), req, phase));
            } else {
                TrainReq req = new TrainReq(response.getClient(), sumvec, sampleIndex, false);
                commonRequests.add(new CommonRequest(response.getClient(), req, phase));
            }
        }
        if (numClassRound + 1 == numClass) {
            clientInd += 1;
            if (clientInd >= clientInfoList.size()) {
                clientInd = 0;
            }
        }
        logger.info("Algo phase 1 end" + splitLine);
        return commonRequests;
    }


    public List<CommonRequest> controlPhase3(List<CommonResponse> responses) {
        phase = 3;
        // TODO 待修改，需要由client端将testUId的真实值/加密值传输过来
        originIdArray = testUId.stream().map(String::valueOf).toArray(String[]::new);
        predict = new double[originIdArray.length];
        predicts = new double[originIdArray.length][numClass];
        idIndexArray = IntStream.range(0, testUId.size()).toArray();
        List<CommonRequest> initRequests = new ArrayList<>();
        for (CommonResponse commonResponse : responses) {
            InferenceInit init = new InferenceInit(new String[0]);
            CommonRequest request = new CommonRequest(commonResponse.getClient(), init, phase);
            initRequests.add(request);
        }
        return initRequests;
    }

    public List<CommonRequest> initInference(List<ClientInfo> clientInfos, String[] predictUid) {
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
        logger.info(String.format("Phase %s", phase));
        switch (KernelDispatchJavaPhaseType.valueOf(phase)) {
            case INFERENCE_FILTER:
                return inferenceControlPhase0(response);
            case INFERENCE_EMPTY_REQUEST:
                return inferencecontrolPhase1(response);
            case INFERENCE_EMPTY_REQUEST_1:
                return inferencecontrolPhase1(response);
            case INFERENCE_RESULT:
                return inferencecontrolPhase1(response);
            default:
                throw new UnsupportedOperationException();
        }
    }

    public List<CommonRequest> inferenceControlPhase0(List<CommonResponse> responses) {
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
            headerList.add("label");
            inferenceFinish = true;
            //TODO predicts [numclass]
            predicts = new double[originIdArray.length][1];
            IntStream.range(0, predicts.length).forEach(x -> predicts[x][0] = Double.NaN);
        }
        // 过滤不需要预测的uid, filterSet返回的位置，所以根据位置过滤
        List<Integer> queryIdHasFiltered = new ArrayList<>();
        for (int i = 0; i < originIdArray.length; i++) {
            if (!blacklist.contains(i)) {
                queryIdHasFiltered.add(i);
            }
        }
        idIndexArray = queryIdHasFiltered.stream().mapToInt(x -> x).toArray();
        //构造请求
        List<CommonRequest> res = new ArrayList<>();
        for (CommonResponse response : responses) {
            InferenceInit init = new InferenceInit(new String[0]);
            CommonRequest request = new CommonRequest(response.getClient(), init, phase);
            res.add(request);
        }
        return res;
    }

    public List<CommonRequest> inferencecontrolPhase1(List<CommonResponse> response) {
        logger.info("Algo phase 1 start" + splitLine);
        List<CommonRequest> commonRequests = new ArrayList<>();
        // 发送kernel type 等相关参数给各机器
        for (CommonResponse response_i : response) {
            if (response_i.getBody() != null) {
                InferenceReqAndRes res = (InferenceReqAndRes) response_i.getBody();
                if (res.getMultiClassUniqueLabelList() != null && res.getMultiClassUniqueLabelList().size() > 1) {
                    numClass = res.getNumClass();
                    multiClassUniqueLabelList = res.getMultiClassUniqueLabelList();
                    if (numClass <= 2) {
                        headerList.add("label");
                    } else {
                        IntStream.range(0, multiClassUniqueLabelList.size()).forEach(x -> headerList.add(String.valueOf(multiClassUniqueLabelList.get(x))));
                    }
                }
            }
            InferenceReqAndRes req = new InferenceReqAndRes(response_i.getClient());
            commonRequests.add(new CommonRequest(response_i.getClient(), req, phase));
        }
        predicts = new double[originIdArray.length][numClass];
        logger.info("Algo phase 1 end" + splitLine);
        return commonRequests;
    }

    public List<CommonRequest> inferencecontrolPhase99(List<CommonResponse> responses) {
        logger.info("Algo phase 1 start" + splitLine);
        List<CommonRequest> commonRequests = new ArrayList<>();
        //每个预测样本一个预测值
        logger.info("Post Inference Control...");
        logger.info(String.format("Result aggregation %s samples!", predict.length));
        for (CommonResponse response : responses) {
            if (idIndexArray.length != 0 && response.getBody() != null) {
                InferenceReqAndRes res = (InferenceReqAndRes) response.getBody();
                double[] values = res.getPredictA();
                double[][] allPre = res.getPredicts();
                // 需要预测的uid对应index
                List<Integer> idSet = Arrays.stream(idIndexArray).boxed().collect(Collectors.toList());
                for (int n = 0; n < allPre[0].length; n++) {
                    for (int p = 0; p < predict.length; p++) {
                        if (idSet.contains(p)) {
                            int index = idSet.indexOf(p);
                            predict[p] += values[index];
                            predicts[p][n] += allPre[index][n];
                        } else {
                            predict[p] = Double.NaN; // 只要有一方没有这个推理样本，则pred=NaN
                            predicts[p][n] = Double.NaN;
                        }
                    }
                }
            }
        }
        for (CommonResponse response : responses) {
            TrainReq req;
            if (((InferenceReqAndRes) response.getBody()).isActive()) {
                req = new TrainReq(response.getClient(), new PredictRes(headerList.toArray(new String[0]), predicts));
            } else {
                req = new TrainReq(response.getClient(), (PredictRes) null);
            }
            commonRequests.add(new CommonRequest(response.getClient(), req, phase));
        }
        return commonRequests;
    }

    @Override
    public MetricValue readMetrics() {
        if (bestRound != 0) {
            metricValue = new MetricValue(metricMap, metricMapArr, metricMapVali, metricMapArrVali, new HashMap<>(), bestRound);
        } else {
            // -1即代表当前轮效果最优
            metricValue = new MetricValue(metricMap, metricMapArr, metricMapVali, metricMapArrVali, new HashMap<>(), -1);
        }
        return metricValue;
    }


    private void dataShuffle(int numSamples) {
        sampleIndex = IntStream.range(0, numSamples).boxed().collect(Collectors.toList());
    }

    public int getNextPhase(int phaseindex, List<CommonResponse> responses) {
        if (phaseindex == -255) {
            phaseindex = -1;
        } else if (phaseindex < 0) {
            phaseindex = phaseindex - 1;
        } else if (phaseindex == 2) {
            if (numClassRound + 1 == numClass) {
                phaseindex = 3;
            } else {
                phaseindex = 1;
            }
        } else if (phaseindex == 7) {
            phaseindex = 1;
        } else {
            phaseindex = phaseindex + 1;
        }
        if (phaseindex == -4) {
            inferenceFinish = true;
        }
        return phaseindex;
    }


    public PredictRes postInferenceControl(List<CommonResponse> responses) {
        //每个预测样本一个预测值
        logger.info("Post Inference Control...");
        int i;
        logger.info(String.format("Result aggregation %s samples!", predict.length));
        for (CommonResponse response : responses) {
            if (idIndexArray.length != 0 && response.getBody() != null) {
                InferenceReqAndRes res = (InferenceReqAndRes) response.getBody();
                double[] values = res.getPredictA();
                double[][] allPre = res.getPredicts();
                // 需要预测的uid对应index
                List<Integer> idSet = Arrays.stream(idIndexArray).boxed().collect(Collectors.toList());
                for (int n = 0; n < allPre[0].length; n++) {
                    for (int p = 0; p < predict.length; p++) {
                        if (idSet.contains(p)) {
                            int index = idSet.indexOf(p);
                            predict[p] += values[index];
                            predicts[p][n] += allPre[index][n];
                        } else {
                            predict[p] = Double.NaN; // 只要有一方没有这个推理样本，则pred=NaN
                            predicts[p][n] = Double.NaN;
                        }
                    }
                }
            }
        }
        double[][] predictsClip = KernelLinearRegressionJavaModel.predTrans(predicts, numClass);
        IntStream.range(0, predictsClip.length).forEach(index -> predict[index] = MathExt.max(predictsClip[index]));
        if (numClass == 2) {
            double[][] pred = MathExt.transpose(MathExt.transpose(predictsClip)[1]);
            return new PredictRes(headerList.toArray(new String[0]), pred);
        }
        return new PredictRes(headerList.toArray(new String[0]), predictsClip);
    }

    public boolean isInferenceContinue() {
        return !inferenceFinish;
    }

    public boolean isContinue() {
        if (round <= parameter.getMaxIter()) {
            logger.info("check stop and return true");
            return true;
        } else {
            logger.info("check stop and return false");
            return false;
        }
    }

    public AlgorithmType getAlgorithmType() {
        return algorithmType;
    }

    public void setForUnitTest(int numSamples, int phase, List<ClientInfo> clientInfos, String modelToken, int numClient) {
        this.numSamples = numSamples;
        this.phase = phase;
        this.clientInfoList = clientInfos;
        this.numClient = numClient;
        this.sampleIndex = IntStream.range(0, numSamples).boxed().collect(Collectors.toList());
        this.modelToken = modelToken;
    }

    public void setForUnitTestInfer(String[] originIdArray, int[] idIndexArray, int phase, List<ClientInfo> clientInfos, String modelToken) {
        this.originIdArray = originIdArray;
        this.idIndexArray = idIndexArray;
        this.phase = phase;
        this.clientInfoList = clientInfos;
        this.sampleIndex = IntStream.range(0, numSamples).boxed().collect(Collectors.toList());
        this.modelToken = modelToken;
    }


}
