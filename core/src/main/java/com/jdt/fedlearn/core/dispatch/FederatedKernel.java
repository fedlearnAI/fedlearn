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
import com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillier;
import com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillierNative;
import com.jdt.fedlearn.core.encryption.distributedPaillier.HomoEncryptionUtil;
import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.core.entity.base.SingleElement;
import com.jdt.fedlearn.core.entity.common.*;
import com.jdt.fedlearn.common.entity.core.feature.Features;
import com.jdt.fedlearn.core.entity.kernelLinearRegression.*;
import com.jdt.fedlearn.core.entity.mixedLinearRegression.CypherMessage2D;
import com.jdt.fedlearn.core.entity.mixedLinearRegression.CypherMessage2DList;
import com.jdt.fedlearn.core.math.MathExt;
import com.jdt.fedlearn.core.model.FederatedKernelModel;
import com.jdt.fedlearn.core.parameter.FederatedKernelParameter;
import com.jdt.fedlearn.core.preprocess.TrainTestSplit;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.core.type.*;
import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.data.Pair;
import com.jdt.fedlearn.core.type.data.Tuple2;
import com.jdt.fedlearn.core.util.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FederatedKernel implements Control {
    private static final Logger logger = LoggerFactory.getLogger(FederatedKernel.class);

    private static final AlgorithmType algorithmType = AlgorithmType.FederatedKernel;
    private final FederatedKernelParameter parameter;
    private int numClass;

    private List<Double> multiClassUniqueLabelList;
    private int round = 0;
    private Map<MetricType, List<Pair<Integer, Double>>> metricMap = new HashMap<>();
    private Map<MetricType, List<Pair<Integer, String>>> metricMapArr = new HashMap<>();
    private Map<MetricType, List<Pair<Integer, Double>>> metricMapVali = new HashMap<>();
    private Map<MetricType, List<Pair<Integer, String>>> metricMapArrVali = new HashMap<>();

    private int phase = 0;
    private int[] idIndexArray;
    private String[] originIdArray;
    private double[][] predicts;
    private DistributedPaillierNative.signedByteArray[][] partialSum;
    private boolean inferenceFinish = false;
    private List<String> headerList = new ArrayList<>();

    private HomoEncryptionUtil pheKeys;
    private boolean useDistributedPailler = false;
    private final boolean useFakeDec = false;
    private static final int ENC_BITS = 1024;

    public FederatedKernel(FederatedKernelParameter parameter) {
        this.parameter = parameter;
    }

    /**
     * 训练初始化：初始化参数，训练集验证集切分，构造客户端请求
     *
     * @param clientInfos 客户端列表
     * @param idMap       id对照表,以及其他自动化预处理信息
     * @param features    各客户端训练特征
     * @param other       其他自定义参数
     * @return 客户端请求
     */
    public List<CommonRequest> initControl(List<ClientInfo> clientInfos,
                                           MatchResult idMap,
                                           Map<ClientInfo, Features> features,
                                           Map<String, Object> other) {
        logger.info(String.format("Init control, the client inputs are %s.", clientInfos));
        this.numClass = parameter.getNumClass();
        double splitRatio = Double.parseDouble(other.get("splitRatio").toString());
        int clientInd = 0;
        if (numClass > 1) {
            clientInd = 1;
        }
        Tuple2<List<Integer>, List<Integer>> trainTestSplit = TrainTestSplit.trainTestSplit(idMap.getLength(), splitRatio, 666);
        assert trainTestSplit != null;
        List<Integer> testUId = trainTestSplit._2();
        List<CommonRequest> res = new ArrayList<>();
        int numSamples = trainTestSplit._1().size();
        logger.info(String.format("Training sample number is %s", numSamples));
        List<Integer> sampleIndex = IntStream.range(0, numSamples).boxed().collect(Collectors.toList());
        int[] testIndex = testUId.stream().mapToInt(Integer::valueOf).toArray();
        for (ClientInfo clientInfo : clientInfos) {
            Features localFeature = features.get(clientInfo);
            other.put("sampleIndex", sampleIndex);
            other.put("testUid", testUId);
            other.put("clientInfoList", clientInfos);
            other.put("clientInd", clientInd);
            TrainInit nested_req = new TrainInit(parameter, localFeature, idMap.getMatchId(), other, testIndex);
            CommonRequest request = CommonRequest.buildTrainInitial(clientInfo, nested_req);
            res.add(request);
        }
        logger.info(String.format("Request content %s", res));
        return res;
    }

    /**
     * 训练阶段
     *
     * @param response 客户端返回结果
     * @return 各阶段客户端请求
     */
    @Override
    public List<CommonRequest> control(List<CommonResponse> response) {
        KernelDispatchPhaseType kernelDispatchPhaseType = KernelDispatchPhaseType.UPDATE_METRIC;
        Message message = response.get(0).getBody();
        if (message instanceof TrainRes) {
            kernelDispatchPhaseType = ((TrainRes) message).getKernelDispatchJavaPhaseType();
        } else if (message instanceof InferenceInitRes) {
            kernelDispatchPhaseType = KernelDispatchPhaseType.VALIDATION_FILTER;
        } else if (message instanceof InferenceReqAndRes) {
            kernelDispatchPhaseType = ((InferenceReqAndRes) message).getKernelDispatchJavaPhaseType();
        } else if (message == null) {
            kernelDispatchPhaseType = KernelDispatchPhaseType.EMPTY_REQUEST;
        }
        switch (kernelDispatchPhaseType) {
            case UPDATE_METRIC:
//                dataShuffle(numSamples);
                return controlPhase1(response);
            case COMPUTE_LOSS:
                return controlPhase2(response);
            case VALIDATION_INIT:
            case VALIDATION_FILTER:
                return validateInit(response, kernelDispatchPhaseType);
            case EMPTY_REQUEST:
                return emptyRequest(response, message);
            case VALIDATION_RESULT:
                return validateResult(response);
            default:
                throw new UnsupportedOperationException("unsupported message type in control");
        }
    }

    /**
     * 训练步骤1：判断是否时初始化
     * 初始化：向客户端发送样本、当前轮数等
     * 非初始化：从客户端获取metric信息并判断是否符合早停条件
     *
     * @param responses 客户端返回
     * @return 客户端请求
     */
    public List<CommonRequest> controlPhase1(List<CommonResponse> responses) {
        logger.info("Algo phase 1 processing on coordinator");
        List<CommonRequest> commonRequests = new ArrayList<>();
        int numClassRound = 0;
        int bestRound = 0;
        for (CommonResponse response : responses) {
            Message message = response.getBody();
            if (message instanceof SingleElement || !((TrainRes) message).getActive()) {
                for (CommonResponse res : responses) {
                    TrainReq req = new TrainReq(res.getClient(), numClassRound, bestRound);
                    if (message instanceof TrainRes) {
                        req = new TrainReq(res.getClient(), numClassRound, bestRound);
                    }
                    commonRequests.add(new CommonRequest(res.getClient(), req, 1));
                }
                return commonRequests;
            }
            TrainRes res = (TrainRes) message;
            numClassRound = res.getNumClassRound();
            bestRound = getAllMetrics(res, bestRound);
        }
        for (CommonResponse response : responses) {
            TrainReq req = new TrainReq(response.getClient(), numClassRound, bestRound);
            commonRequests.add(new CommonRequest(response.getClient(), req, 1));
        }
        logger.info("Algo phase 1 end");
        return commonRequests;
    }

    /**
     * 获取全部metric信息：训练的一维/二维指标，验证的一维/二维指标【改成那个对象对象】
     *
     * @param res 客户端返回
     */
    private int getAllMetrics(TrainRes res, int bestRound) {
        Map<MetricType, List<Double>> metric = res.getMetric();
        Map<MetricType, List<Double>> metricVali = res.getMetricVali();
        Map<MetricType, List<Double[][]>> metricArr = res.getMetricArr();
        Map<MetricType, List<Double[][]>> metricArrVali = res.getMetricArrVali();
        if (metric == null || metric.size() == 0) {
            return bestRound;
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
            return bestRound;
        }
        metricMapVali = getMetricValue(metricVali);
        StringBuilder metricOutput = new StringBuilder(String.format("KernelJava vali round %d,%n", round - 1));
        for (Map.Entry<MetricType, List<Pair<Integer, Double>>> e : metricMapVali.entrySet()) {
            metricOutput.append(String.format("                train-%s:%.15f%n", e.getKey(), e.getValue().get(round - 1).getValue()));
        }
        if (parameter.getNumClass() <= 2) {
            logger.info(metricOutput.toString());
        }
        if (metricArrVali != null && !metricArrVali.isEmpty()) {
            metricMapArrVali = getArrMetricValue(metricArrVali);
        }
        //TODO erlystopping round and metrictype
        for (Map.Entry<MetricType, List<Double>> entry : metricVali.entrySet()) {
            int tmpround = entry.getValue().size();
            if (tmpround >= parameter.getEarlyStoppingRounds() + 1 && entry.getKey().equals(MetricType.RMSE)) {
                List<Double> lossMetric = entry.getValue();
                bestRound = Tool.earlyStopping(lossMetric, parameter.getEarlyStoppingRounds());
                logger.info("maxIn " + bestRound);
            }
        }
        return bestRound;
    }


    private void updateAllMetrics(InferenceReqAndRes res) {
        if (res.isActive()) {
            Map<MetricType, List<Double>> metric = res.getMetric();
            Map<MetricType, List<Double>> metricVali = res.getMetricVali();
            Map<MetricType, List<Double[][]>> metricArr = res.getMetricArr();
            Map<MetricType, List<Double[][]>> metricArrVali = res.getMetricArrVali();
            if (metric != null && metric.size() > 0) {
                metricMap = getMetricValue(metric);
            }
            if (metricArr != null && !metricArr.isEmpty()) {
                metricMapArr = getArrMetricValue(metricArr);
            }
            if (metricVali != null && metricVali.size() > 0) {
                metricMapVali = getMetricValue(metricVali);
            }
            if (metricArrVali != null && !metricArrVali.isEmpty()) {
                metricMapArrVali = getArrMetricValue(metricArrVali);
            }
        }
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

    /**
     * 汇总客户端信息，早停，选取下一次更新的客户端
     *
     * @param responses 客户端返回
     * @return 客户端请求
     */
    public List<CommonRequest> controlPhase2(List<CommonResponse> responses) {
        List<CommonRequest> commonRequests = new ArrayList<>();
        this.numClass = parameter.getNumClass();
        TrainRes trainRes = (TrainRes) responses.get(0).getBody();
        if (trainRes.getRound() == parameter.getMaxIter() + 1) {
            round = trainRes.getRound();
            responses.forEach(x -> commonRequests.add(new CommonRequest(x.getClient(), new SingleElement("earlyStopping success"), phase)));
            return commonRequests;
        }
        int batchSize = trainRes.getVectors()[0].length;//Math.min(parameter.getBatchSize(), numSamples);
        int clientInd = trainRes.getClientInd();
        int numClassRound = trainRes.getNumClassRound();
        List<ClientInfo> clientInfoList = trainRes.getClientInfoList();
        double[][] sumvec = new double[numClass][batchSize];
        int bestRound = 0;
        for (CommonResponse response : responses) {
            TrainRes res = (TrainRes) response.getBody();
            double[][] temp = res.getVectors();
            for (int j = 0; j < numClass; j++) {
                for (int i = 0; i < batchSize; i++) {
                    sumvec[j][i] += temp[j][i];
                }
            }
            bestRound = getAllMetrics(res, bestRound);
        }
        for (CommonResponse response : responses) {
            ClientInfo info = response.getClient();
            if (info.equals(clientInfoList.get(clientInd))) {
                logger.info(String.format("Find selected client %s", info));
                TrainReq req = new TrainReq(response.getClient(), sumvec, true);
                commonRequests.add(new CommonRequest(response.getClient(), req, 2));
            } else {
                TrainReq req = new TrainReq(response.getClient(), sumvec, false);
                commonRequests.add(new CommonRequest(response.getClient(), req, 2));
            }
        }
        if (numClassRound + 1 == numClass) {
            clientInd += 1;
            if (clientInd >= clientInfoList.size()) {
                clientInd = 0;
            }
        }
        int finalClientInd = clientInd;
        commonRequests.forEach(x -> ((TrainReq) x.getBody()).setClientInd(finalClientInd));
        logger.info("Algo phase 2 end");
        return commonRequests;
    }

    /**
     * 验证过滤
     *
     * @param responses 验证过滤
     * @return 客户端请求
     */
    public List<CommonRequest> validateInit(List<CommonResponse> responses, KernelDispatchPhaseType kernelDispatchPhaseType) {
        int phase = 3;
        if (kernelDispatchPhaseType.equals(KernelDispatchPhaseType.VALIDATION_FILTER)) {
            phase = 4;
        }
        List<CommonRequest> initRequests = new ArrayList<>();
        for (CommonResponse commonResponse : responses) {
            InferenceInit init = new InferenceInit(new String[0]);
            CommonRequest request = new CommonRequest(commonResponse.getClient(), init, phase);
            initRequests.add(request);
        }
        return initRequests;
    }


    /**
     * 获取推理类别及详情，构造推理结果信息
     *
     * @param response 客户端返回
     * @return 客户端空请求
     */
    public List<CommonRequest> emptyRequest(List<CommonResponse> response, Message message) {
        int phase = 5;
        if (message == null) {
            phase = 6;
        } else if (message instanceof InferenceReqAndRes) {
            response.forEach(x -> updateAllMetrics(((InferenceReqAndRes) x.getBody())));
        }
        final int phaseFinal = phase;
        logger.info("Algo phase " + phase + " start");
        List<CommonRequest> commonRequests = new ArrayList<>();
        response.forEach(x -> commonRequests.add(new CommonRequest(x.getClient(), new InferenceReqAndRes(x.getClient()), phaseFinal)));
        return commonRequests;
    }

    /**
     * 验证结果
     *
     * @param responses 计算验证结果
     * @return 计算结果
     */
    public List<CommonRequest> validateResult(List<CommonResponse> responses) {
        this.numClass = parameter.getNumClass();

        InferenceReqAndRes reqAndRes = (InferenceReqAndRes) responses.get(0).getBody();
        List<Integer> testUid = reqAndRes.getTestUid();
        // TODO 待修改，需要由client端将testUId的真实值/加密值传输过来 testUid需要传过来
        predicts = new double[testUid.size()][numClass];
        logger.info("Algo phase 4 start");
        originIdArray = testUid.stream().map(String::valueOf).toArray(String[]::new);
        idIndexArray = IntStream.range(0, testUid.size()).toArray();
        List<CommonRequest> commonRequests = new ArrayList<>();
        //每个预测样本一个预测值
        logger.info("Post Inference Control...");
        logger.info(String.format("Result aggregation %s samples!", predicts.length));
        for (CommonResponse response : responses) {
            if (idIndexArray.length != 0 && response.getBody() != null) {
                InferenceReqAndRes res = (InferenceReqAndRes) response.getBody();
                updateAllMetrics(res);
                double[][] allPre = res.getPredicts();
                // 需要预测的uid对应index
                List<Integer> idSet = Arrays.stream(idIndexArray).boxed().collect(Collectors.toList());
                for (int n = 0; n < allPre[0].length; n++) {
                    for (int p = 0; p < predicts.length; p++) {
                        if (idSet.contains(p)) {
                            int index = idSet.indexOf(p);
                            predicts[p][n] += allPre[index][n];
                        }
                    }
                }
            }
        }
        for (CommonResponse response : responses) {
            TrainReq req;
            if (((InferenceReqAndRes) response.getBody()).isActive()) {
                req = new TrainReq(response.getClient(), new PredictRes(new String[0], predicts));
            } else {
                req = new TrainReq(response.getClient(), (PredictRes) null);
            }
            commonRequests.add(new CommonRequest(response.getClient(), req, 7));
        }
        return commonRequests;
    }


    /**
     * 推理初始化
     * 将原始id列表（安全推理信息）发送给客户端
     *
     * @param clientInfos 客户端列表
     * @param predictUid  需要推理的uid
     * @param others      安全推理模式
     * @return 客户端请求
     */
    public List<CommonRequest> initInference(List<ClientInfo> clientInfos, String[] predictUid, Map<String, Object> others) {
        originIdArray = predictUid;
        idIndexArray = IntStream.range(0, predictUid.length).toArray();
        List<CommonRequest> initRequests = new ArrayList<>();
        phase = CommonRequest.inferenceInitialPhase;
        if (!others.containsKey("pubKeyStr")) {
            useDistributedPailler = false;
            for (ClientInfo clientInfo : clientInfos) {
                Map<String, Object> extraParamsFromMaster = new HashMap<>();
                extraParamsFromMaster.put("secureMode", false);
                InferenceInit init = new InferenceInit(originIdArray, extraParamsFromMaster);
                CommonRequest request = CommonRequest.buildInferenceInitial(clientInfo, init);
                initRequests.add(request);
            }
        } else {
            String pubKeyStr = others.get("pubKeyStr").toString();
            useDistributedPailler = true;
            DistributedPaillier.DistPaillierPubkey pubkey = new DistributedPaillier.DistPaillierPubkey();
            pubkey.parseJson(pubKeyStr);
            this.pheKeys = new HomoEncryptionUtil(clientInfos.size(), ENC_BITS, useFakeDec);
            this.pheKeys.setPk(pubkey);
            for (int i = 0; i < clientInfos.size(); i++) {
                Map<String, Object> other = new HashMap<>();
                other.put("secureMode", useDistributedPailler);
                // Standalone version. Master transfer keys to all parties. Master is TRUSTED.
                other.put("numP", clientInfos.size());
                other.put("ENC_BITS", ENC_BITS);
                other.put("thisPartyID", i + 1);
                InferenceInit initString = new InferenceInit(originIdArray, other);
                initRequests.add(CommonRequest.buildInferenceInitial(clientInfos.get(i), initString));
            }
        }
        return initRequests;
    }


    /**
     * 推理过程：包括过滤无需预测的uid，构造推理结果的表头，获取推理结果
     *
     * @param response 返回数据
     * @return 各阶段客户端请求
     */
    @Override
    public List<CommonRequest> inferenceControl(List<CommonResponse> response) {
        phase = getNextPhase(phase, response);
        logger.info(String.format("Phase %s", phase));
        switch (KernelDispatchPhaseType.valueOf(phase)) {
            case INFERENCE_FILTER:
                return inferenceFilter(response);
            case INFERENCE_EMPTY_REQUEST:
                return constructHeaders(response);
            case INFERENCE_RESULT:
                return inferenceResult(response);
            default:
                throw new UnsupportedOperationException();
        }
    }

    /**
     * 过滤无需预测的uid，并将需要预测的uid发送给客户端
     *
     * @param responses 客户端初始化结果
     * @return 客户端请求
     */
    public List<CommonRequest> inferenceFilter(List<CommonResponse> responses) {
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
            if (KernelDispatchPhaseType.valueOf(phase) == KernelDispatchPhaseType.INFERENCE_FILTER) {
                int[] idIndexArrayU = queryIdHasFiltered.stream().mapToInt(x -> x).toArray();
                String[] idArray = Arrays.stream(idIndexArrayU).mapToObj(x -> originIdArray[x]).toArray(String[]::new);
                init = new InferenceInit(idArray);
            }
            CommonRequest request = new CommonRequest(response.getClient(), init, phase);
            res.add(request);
        }
        return res;
    }

    /**
     * 获取推理类别及详情，构造推理结果信息
     *
     * @param response 客户端返回
     * @return 客户端空请求
     */
    public List<CommonRequest> constructHeaders(List<CommonResponse> response) {
        logger.info("Algo phase " + phase + " start");
        List<CommonRequest> commonRequests = new ArrayList<>();
        // 发送kernel type 等相关参数给各机器
        for (CommonResponse response_i : response) {
            if (response_i.getBody() != null) {
                InferenceReqAndRes res = (InferenceReqAndRes) response_i.getBody();
                numClass = res.getNumClass();
                if (res.getMultiClassUniqueLabelList() != null && res.getMultiClassUniqueLabelList().size() > 1) {
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
        logger.info("Algo phase 1 end");
        return commonRequests;
    }


    /**
     * 推理结果汇总
     *
     * @param responses 客户端推理结果（安全模式时为解密结果）
     * @return 推理结果（安全模式时为加密状态结果）
     */
    public List<CommonRequest> inferenceResult(List<CommonResponse> responses) {
        List<CommonRequest> commonRequests = new ArrayList<>();
        //每个预测样本一个预测值
        logger.info(String.format("Result aggregation %s samples!", predicts.length));
        partialSum = new DistributedPaillierNative.signedByteArray[predicts.length][predicts[0].length];
        if (responses.get(0).getBody() != null && responses.get(0).getBody() instanceof CypherMessage2DList) {
            List<DistributedPaillierNative.signedByteArray[][]> forwardMessages = responses.parallelStream()
                    .map(response -> ((CypherMessage2DList) response.getBody()).getBody().get(0)).collect(Collectors.toList());
            for (int i = 0; i < responses.size(); i++) {
                ClientInfo clientInfo = responses.get(i).getClient();
                List<DistributedPaillierNative.signedByteArray[][]> forwardToSingleClient = new ArrayList<>(forwardMessages);
                forwardToSingleClient.remove(i);
                commonRequests.add(new CommonRequest(clientInfo, new CypherMessage2DList(forwardToSingleClient), -3));
            }
//            inferenceFinish = true;
            return commonRequests;
        }
        for (int i = 0; i < responses.size(); i++) {
            CommonResponse response = responses.get(i);
            if (idIndexArray.length != 0 && response.getBody() != null) {
                Message message = response.getBody();
                if (message instanceof InferenceReqAndRes) {
                    if (((InferenceReqAndRes) message).getClient() == null) {
                        inferenceFinish = true;
                        break;
                    } else {
                        predictSum(response);
                    }
                } else if (message instanceof CypherMessage2D) {
                    CypherMessage2D res = (CypherMessage2D) response.getBody();
                    DistributedPaillierNative.signedByteArray[][] partialScores = res.getBody();
                    if (i == 0) {
                        partialSum = partialScores;
                    } else {
                        encryptedPredictSum(partialScores);
                    }
                    // 1. broadcast, single sum for all clients
                    Message body = new CypherMessage2D(partialSum);
                    commonRequests.add(new CommonRequest(response.getClient(), body, -3));
                }
            }
        }
        if (useDistributedPailler) {
            return commonRequests;
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
        inferenceFinish = true;
        return commonRequests;
    }

    /**
     * 加密推理结果求和
     *
     * @param partialScores 各客户端部分加密结果
     */
    private void encryptedPredictSum(DistributedPaillierNative.signedByteArray[][] partialScores) {
        for (int n = 0; n < partialScores[0].length; n++) {
            for (int p = 0; p < partialScores.length; p++) {
                partialSum[p][n] = pheKeys.add(partialSum[p][n], partialScores[p][n], pheKeys.getPk());
            }
        }
    }


    /**
     * 客户端推理结果求和
     *
     * @param response 客户端推理结果
     */
    private void predictSum(CommonResponse response) {
        InferenceReqAndRes res = (InferenceReqAndRes) response.getBody();
        double[][] allPre = res.getPredicts();
        // 需要预测的uid对应index
        List<Integer> idSet = Arrays.stream(idIndexArray).boxed().collect(Collectors.toList());
        for (int n = 0; n < allPre[0].length; n++) {
            for (int p = 0; p < predicts.length; p++) {
                if (idSet.contains(p)) {
                    int index = idSet.indexOf(p);
                    predicts[p][n] += allPre[index][n];
                } else {
                    predicts[p][n] = Double.NaN;
                }
            }
        }
    }

    /**
     * 推理结果匹配：将推理结果对应到uid实际位置
     *
     * @param response 推理结果
     */
    private void matchPredict(CommonResponse response) {
        InferenceReqAndRes res = (InferenceReqAndRes) response.getBody();
        double[][] allPre = res.getPredicts();
        List<Integer> idSet = Arrays.stream(idIndexArray).boxed().collect(Collectors.toList());
        for (int n = 0; n < allPre[0].length; n++) {
            for (int p = 0; p < predicts.length; p++) {
                if (idSet.contains(p)) {
                    int index = idSet.indexOf(p);
                    predicts[p][n] = allPre[index][n];
                } else {
                    predicts[p][n] = Double.NaN;
                }
            }
        }
    }

    @Override
    public MetricValue readMetrics() {
        // -1即代表当前轮效果最优
        return new MetricValue(metricMap, metricMapArr, metricMapVali, metricMapArrVali, new HashMap<>(), -1);
    }
//
//
//    private void dataShuffle(int numSamples) {
//        sampleIndex = IntStream.range(0, numSamples).boxed().collect(Collectors.toList());
//    }

    public int getNextPhase(int phase, List<CommonResponse> commonResponses) {
        if (phase == -255) {
            phase = -1;
        } else if (phase == -4) {
            phase = -4;
        } else if (phase < 0) {
            phase = phase - 1;
        } else if (phase == 2) {
            TrainRes res = (TrainRes) commonResponses.get(0).getBody();
            int numClassRound = res.getNumClassRound();
            // todo 从model侧传过来phaseType
            if (numClassRound + 1 == parameter.getNumClass()) {
                phase = 3;
            } else {
                phase = 1;
            }
        } else if (phase == 7) {
            phase = 1;
        } else {
            phase = phase + 1;
        }
        return phase;
    }


    /**
     * 结果匹配和转换
     *
     * @param responses 客户端推理结果
     * @return 推理结果
     */
    public PredictRes postInferenceControl(List<CommonResponse> responses) {
        //每个预测样本一个预测值
        logger.info("Post Inference Control...");
        logger.info(String.format("Result aggregation %s samples!", predicts.length));
        if (idIndexArray.length != 0 && responses.get(0).getBody() != null && useDistributedPailler) {
            matchPredict(responses.get(0));
        }
        double[][] predictsClip = FederatedKernelModel.predictTrans(predicts, numClass);
        if (numClass == 2 && predictsClip[0].length != 1) {
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

    public void setForUnitTest(int phase) {
        this.phase = phase;
    }

    public void setForUnitTestInfer(String[] originIdArray, int[] idIndexArray, int phase) {
        this.originIdArray = originIdArray;
        this.idIndexArray = idIndexArray;
        this.phase = phase;
    }


}
