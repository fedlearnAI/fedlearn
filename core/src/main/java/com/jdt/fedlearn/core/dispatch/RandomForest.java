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
import com.jdt.fedlearn.core.entity.base.SingleElement;
import com.jdt.fedlearn.core.entity.common.*;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.entity.randomForest.DataUtils;
import com.jdt.fedlearn.core.entity.randomForest.RandomForestInferMessage;
import com.jdt.fedlearn.core.entity.randomForest.RandomForestTrainReq;
import com.jdt.fedlearn.core.entity.randomForest.RandomForestTrainRes;
import com.jdt.fedlearn.core.exception.NotMatchException;
import com.jdt.fedlearn.core.parameter.RandomForestParameter;
import com.jdt.fedlearn.core.preprocess.TrainTestSplit;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.RFDispatchPhaseType;
import com.jdt.fedlearn.core.type.data.Pair;
import com.jdt.fedlearn.core.type.data.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 随机森林，协调端部分
 *
 * @author Peng Zhengyang, Wang Jiazhou
 */
public class RandomForest implements Control {

    private AlgorithmType algorithmType = AlgorithmType.RandomForest;
    // log
    private static final Logger logger = LoggerFactory.getLogger(RandomForest.class);
    private final String splitLine = "========================================================";
    // parameter
    private final RandomForestParameter parameter;
    private Map<MetricType, List<Pair<Integer, Double>>> metricMap = new HashMap<>();
    private Map<MetricType, List<Pair<Integer, String>>> metric2DimMap = new HashMap<>();
    private final Map<MetricType, List<Pair<Integer, Double>>> validateMetricMap = new HashMap<>();
    private final Map<MetricType, List<Pair<Integer, String>>> validateMetric2DimMap = new HashMap<>();
    private Map<String, Double> featureImportance = new HashMap<>();
    private boolean isForestSent = false;
    // inference
    private boolean finishInference = false;
    private int inferencePhase = -255;
    public List<ClientInfo> clientInfoList;
    private String[] inferenceDataUid;
    private String[] originIdArray;
    private double[] inferenceRes;
    private final Map<String, Integer> mapInferenceOrder = new HashMap<>();

    public RandomForest(RandomForestParameter parameter) {
        this.parameter = parameter;
    }

    public RandomForest(RandomForestParameter parameter, AlgorithmType algorithmType) {
        this.parameter = parameter;
        this.algorithmType = algorithmType;
    }
    /**
     * 收集客户端信息及参数信息
     * 根据参数，随机生成每棵树的样本采样信息和每个参与方的特征采样个数
     * 将请求发送给客户端
     *
     * @param clientInfos 客户端列表
     * @param idMap       id对齐信息，对于需要trainTestSplit的算法，包含测试集的id的index信息，非id原始信息
     * @param features    特征信息
     * @param other       其他信息
     * @return 给客户端的初始化请求
     */
    public List<CommonRequest> initControl(List<ClientInfo> clientInfos,
                                           MatchResult idMap,
                                           Map<ClientInfo, Features> features,
                                           Map<String, Object> other) {
        logger.info("Init control start{}", splitLine);

        Random rand = new Random(parameter.getRandomSeed());
        int maxSampledFeatures = parameter.getMaxSampledFeatures();
        int numTrees = parameter.getNumTrees();
        int maxTreeSamples = parameter.getMaxTreeSamples();


        Tuple2<List<Integer>, List<Integer>> trainTestSplit = TrainTestSplit.trainTestSplit(idMap.getLength(), 0, 666);
        assert trainTestSplit != null;

        int numSamples = trainTestSplit._1().size();
        Integer datasetSize = 400000;

        // 计算每棵树的样本采样信息
        Map<Integer, String> sampleIds = new HashMap<>();
        Map<Integer, List<Integer>> listSampleIds = new HashMap<>();
        for (int i = 0; i < numTrees; i++) {
            List<Integer> sampleId;
            if (maxTreeSamples == -1) {
                sampleId = IntStream.range(0, numSamples).boxed().collect(Collectors.toList());
            } else {
                sampleId = DataUtils.choice(maxTreeSamples, numSamples, rand);
            }
            Collections.sort(sampleId);
            String strSampleIds = DataUtils.sampleIdToString(DataUtils.asSortedList(sampleId), datasetSize);
            sampleIds.put(i, strSampleIds);
            listSampleIds.put(i, sampleId);
        }
        Map<Integer, Map<Integer, Integer>> sampleMap = new HashMap<>();
        // 获取每棵树采样样本的行和总样本对应行的映射关系
        for (int i = 0; i < numTrees; i++) {
            List<Integer> sampleIDs = listSampleIds.get(i);
            Map<Integer, Integer> temp = new HashMap<>();
            for (int j = 0; j < sampleIDs.size(); j++) {
                temp.put(sampleIDs.get(j), j);
            }
            sampleMap.put(i, temp);
        }

        // 计算每棵树每个参与方采样多少个特征
        List<Integer> selectedFeatureClient = new ArrayList<>();
        for (int i = 0; i < clientInfos.size(); i++) {
            Features localFeature = features.get(clientInfos.get(i));
            // get rid of uid
            int featureNum = localFeature.getFeatureList().size() - 1;
            if (localFeature.hasLabel()) {
                // get rid of label
                featureNum = featureNum - 1;
            }
            for (int k = 0; k < featureNum; k++) {
                selectedFeatureClient.add(i);
            }
        }
        // 最终的特征采样总数为min（特征采样比例，特征采样数）且至少为1。
        maxSampledFeatures = Math.min(maxSampledFeatures, (int) (selectedFeatureClient.size() * parameter.getMaxSampledRatio()));
        maxSampledFeatures = Math.max(1, maxSampledFeatures);
        Map<ClientInfo, String> featureAllocation = clientInfos.stream().collect(Collectors.toMap(clientInfo -> clientInfo, clientInfo -> ""));
        for (int id = 0; id < numTrees; id++) {
            Map<ClientInfo, Integer> featureAllocationTree = clientInfos.stream().collect(Collectors.toMap(clientInfo -> clientInfo, clientInfo -> 0));
            List<Integer> selectedFeatureClienti = DataUtils.choice(maxSampledFeatures, selectedFeatureClient, rand);
            for (int i : selectedFeatureClienti) {
                featureAllocationTree.put(clientInfos.get(i), featureAllocationTree.get(clientInfos.get(i)) + 1);
            }
            for (ClientInfo ci : clientInfos) {
                String tmp = featureAllocation.get(ci);
                int count = Math.max(1, featureAllocationTree.get(ci));
                tmp = tmp + count;
                if (id < numTrees - 1) {
                    tmp = tmp + ",";
                }
                featureAllocation.put(ci, tmp);
            }
        }

        // 生成请求
        List<CommonRequest> commonRequests = new ArrayList<>();
        for (ClientInfo clientInfo : clientInfos) {
            Features localFeature = features.get(clientInfo);
            Map<String, Object> others = new HashMap<>();
            others.put("featureAllocation", featureAllocation.get(clientInfo));
            others.put("sampleIds", sampleIds);
            others.put("sampleMap", sampleMap);
            others.put("listSampleIds", listSampleIds);
            TrainInit req = new TrainInit(parameter, localFeature, idMap.getMatchId(), others);
            CommonRequest request = CommonRequest.buildTrainInitial(clientInfo, req);
            commonRequests.add(request);
        }
        logger.info("init control end{}", splitLine);
        return commonRequests;
    }

    /**
     * 服务端的整体流程控制
     *
     * @param response 客户端返回结果
     * @return 给客户端的请求
     */
    @Override
    public List<CommonRequest> control(List<CommonResponse> response) {
        if (response.get(0).getBody() instanceof SingleElement) {
            return controlPhase1(response);
        }
        RandomForestTrainRes bodyOri;
        if (response.get(0).getBody() instanceof RandomForestTrainRes) {
            bodyOri = (RandomForestTrainRes) response.get(0).getBody();
        } else {
            throw new NotMatchException("Message to RandomForestTrainRes error in control");
        }
        for (CommonResponse res : response) {
            RandomForestTrainRes rfRes = (RandomForestTrainRes) res.getBody();
            if (rfRes.getMessageType() == RFDispatchPhaseType.SEND_FINAL_MODEL) {
                return sendForest(response);
            }
        }
        switch (bodyOri.getMessageType()) {
            case SEND_SAMPLE_ID:
                return controlPhase1(response);
            case CALCULATE_METRIC:
                return controlPhase2(response);
            case COMBINATION_MESSAGE:
                return controlPhase3(response);
            case SPLIT_NODE:
                return controlPhase4(response);
            case CREATE_CHILD_NODE:
                return controlPhase5(response);
            default:
                throw new NotMatchException("MessageType error in control");
        }
    }

    /**
     * 发起新的一轮训练
     *
     * @param responses 客户端返回结果
     * @return 给客户端的请求
     */
    public List<CommonRequest> controlPhase1(List<CommonResponse> responses) {
        logger.info("control phase 1 start{}", splitLine);
        // 构造请求
        List<CommonRequest> commonRequests = new ArrayList<>();
        for (CommonResponse response : responses) {
            RandomForestTrainReq req = new RandomForestTrainReq(response.getClient());
            commonRequests.add(new CommonRequest(response.getClient(), req, 1));
            if (response.getBody() instanceof RandomForestTrainRes) {
                RandomForestTrainRes rfRes = (RandomForestTrainRes) response.getBody();
                updateMetrics(rfRes);
            }
        }
        logger.info("control phase 1 end{}", splitLine);
        return commonRequests;
    }

    /**
     * 如果是初始化：收集主动方加密后的label，publicKey，featureMap，发送给各方。
     * 如果不是初始化：收集主动方回传的指标和特征重要性
     *
     * @param responses 客户端返回结果
     * @return 给客户端的请求
     */
    public List<CommonRequest> controlPhase2(List<CommonResponse> responses) {
        logger.info("control phase 2 start{}", splitLine);
        String[][] disEncryptY = null;
        String[] encryptY = null;
        String publicKey = null;
        Map<Integer, List<Integer>> treeIdToSampleId = new HashMap<>();
        Map<ClientInfo, List<Integer>[]> clientFeatureMap = new HashMap<>();
        boolean stop = false;
        boolean isDistributed = false;
        for (CommonResponse response : responses) {
            RandomForestTrainRes rfRes = (RandomForestTrainRes) response.getBody();
            if (rfRes.isInit()) {
                if (rfRes.isActive()) {
                    // 获取加密的label和publicKey
                    publicKey = rfRes.getPublicKey();
                    if (rfRes.getDisEncryptionLabel() != null){
                        disEncryptY = rfRes.getDisEncryptionLabel();
                        isDistributed = true;
                    } else {
                        encryptY = rfRes.getEncryptionLabel();
                    }
                    // 获取sample
                    treeIdToSampleId = rfRes.getTidToSampleId();
                }
                // 获取featureMap
                List<Integer>[] featureMap = rfRes.getFeatureIds();
                clientFeatureMap.put(response.getClient(), featureMap);
            } else {
                // 获取指标
                if (rfRes.isActive()) {
                    treeIdToSampleId = rfRes.getTidToSampleId();
                    if (treeIdToSampleId.size() == 0) {
                        stop = true;
                    }
                    updateMetrics(rfRes);
                }
            }
        }

        // 构造请求
        List<CommonRequest> commonRequests = new ArrayList<>();
        for (CommonResponse response : responses) {
            RandomForestTrainRes rfRes = (RandomForestTrainRes) response.getBody();
            RandomForestTrainReq req;
            if (stop) {
                req = new RandomForestTrainReq(response.getClient(), true);
            } else if (publicKey != null) {
                // 初始化
                if (rfRes.isActive()) {
                    req = new RandomForestTrainReq(response.getClient(), treeIdToSampleId, clientFeatureMap);
                } else {
                    if (isDistributed) {
                        req = new RandomForestTrainReq(response.getClient(), treeIdToSampleId, disEncryptY, publicKey, clientFeatureMap);
                    } else {
                        req = new RandomForestTrainReq(response.getClient(), treeIdToSampleId, encryptY, publicKey, clientFeatureMap);
                    }
                }
            } else {
                req = new RandomForestTrainReq(response.getClient(), treeIdToSampleId);
            }
            CommonRequest commonReq = new CommonRequest(response.getClient(), req, 2);
            commonRequests.add(commonReq);
        }
        logger.info("control phase 2 end{}", splitLine);
        return commonRequests;
    }

    /**
     * 收集Phase2传回的信息
     * 拼接信息发送给主动方
     *
     * @param responses 客户端返回结果
     * @return 给客户端的请求
     */
    public List<CommonRequest> controlPhase3(List<CommonResponse> responses) {
        logger.info("control phase 3 start{}", splitLine);
        RandomForestTrainRes[] res = new RandomForestTrainRes[responses.size()];
        List<ClientInfo> clientInfos = new ArrayList<>();
        String[] bodyAll = new String[responses.size()];
        for (int i = 0; i < responses.size(); i++) {
            CommonResponse response = responses.get(i);
            RandomForestTrainRes resi = (RandomForestTrainRes) response.getBody();
            res[i] = resi;
            bodyAll[i] = resi.getBody();
            clientInfos.add(response.getClient());
            updateMetrics(resi);
        }

        List<CommonRequest> commonRequests = new ArrayList<>();
        for (RandomForestTrainRes resi : res) {
            CommonRequest req;
            RandomForestTrainReq rfReq;
            if (resi.isActive()) {
                // 主动端
                rfReq = new RandomForestTrainReq(resi.getClient(), bodyAll, clientInfos);
                rfReq.setNumTrees(resi.getNumTrees());
                rfReq.setTreeIds(resi.getTreeIds());
            } else {
                rfReq = new RandomForestTrainReq(resi.getClient());
                rfReq.setNumTrees(resi.getNumTrees());
                rfReq.setTreeIds(resi.getTreeIds());
            }
            req = new CommonRequest(resi.getClient(), rfReq, 3);
            commonRequests.add(req);
        }
        logger.info("control phase 3 end{}", splitLine);
        return commonRequests;
    }

    /**
     * 收集Phase3回传信息
     * 将信息发送给每一方
     *
     * @param responses 客户端返回结果
     * @return 给客户端的请求
     */
    public List<CommonRequest> controlPhase4(List<CommonResponse> responses) {
        logger.info("control phase 4 start{}", splitLine);
        Map<String, String> splitMessage = new HashMap<>();
        Map<String, Map<Integer, List<Integer>>> tidToSampleIds = new HashMap<>();
        // 先解包 Phase 3 返回结果
        for (CommonResponse response : responses) {
            RandomForestTrainRes res = (RandomForestTrainRes) response.getBody();
            updateMetrics(res);
            boolean isActive = res.isActive();
            if (isActive) {
                splitMessage = res.getSplitMessageMap();
                tidToSampleIds = res.getTidToSampleIds();
            }
        }
        List<CommonRequest> commonRequests = new ArrayList<>();
        for (CommonResponse response : responses) {
            CommonRequest req;
            String clientStr = response.getClient().toString();
            RandomForestTrainReq rfReq = new RandomForestTrainReq(response.getClient(), splitMessage.get(clientStr), tidToSampleIds.get(clientStr));
            req = new CommonRequest(response.getClient(), rfReq, 4);
            commonRequests.add(req);
        }
        logger.info("control phase 4 end{}", splitLine);
        return commonRequests;
    }

    /**
     * 收集Phase4回传信息
     * 拼接信息发送给主动方
     *
     * @param responses 客户端返回结果
     * @return 给客户端的请求
     */
    public List<CommonRequest> controlPhase5(List<CommonResponse> responses) {
        logger.info("control phase 5 start{}", splitLine);
        List<String[]> allTreeIds = new ArrayList<>();
        List<ClientInfo> clientInfos = new ArrayList<>();
        List<Map<Integer, double[]>> maskLefts = new ArrayList<>();
        List<String[]> splitMessages = new ArrayList<>();
        // 收集 Phase 4 的信息
        for (CommonResponse response : responses) {
            RandomForestTrainRes res = (RandomForestTrainRes) response.getBody();
            updateMetrics(res);
            String[] treeIds = res.getTreeIds();
            clientInfos.add(res.getClient());
            allTreeIds.add(treeIds);
            maskLefts.add(res.getMaskLeft());
            splitMessages.add(res.getSplitMess());

        }
        List<CommonRequest> commonRequests = new ArrayList<>();
        for (CommonResponse response : responses) {
            RandomForestTrainRes res = (RandomForestTrainRes) response.getBody();
            RandomForestTrainReq req;
            if (res.isActive()) {
                req = new RandomForestTrainReq(response.getClient(), allTreeIds, clientInfos, maskLefts, splitMessages);
            } else {
                req = new RandomForestTrainReq(response.getClient());
            }
            commonRequests.add(new CommonRequest(response.getClient(), req, 5));

        }
        logger.info("control phase 5 end{}", splitLine);
        return commonRequests;
    }

    /**
     * 返回随机森林模型
     *
     * @param response 客户端回传信息
     * @return 给客户端返回随机森林模型信息
     */
    public List<CommonRequest> sendForest(List<CommonResponse> response) {
        logger.info("Train finished send forest...");
        List<CommonRequest> commonRequests = new ArrayList<>();
        if (response.get(0).getBody() != null && ((RandomForestTrainRes) response.get(0).getBody()).getBody().equals("finish")) {
            isForestSent = true;
            for (CommonResponse responsei : response) {
                RandomForestTrainRes res = (RandomForestTrainRes) responsei.getBody();
                if (res.isActive()) {
                    updateMetrics(res);
                }
            }
            return createNullRequest(response, 99);
        } else if (response.get(0).getBody() != null && ((RandomForestTrainRes) response.get(0).getBody()).getBody().equals("success")) {
            Map<String, String> jsonForest = new HashMap<>();
            for (CommonResponse responsei : response) {
                RandomForestTrainRes res = (RandomForestTrainRes) responsei.getBody();
                if (res.isActive()) {
                    updateMetrics(res);
                }
                if (res.isActive() && "success".equals(res.getBody())) {
                    jsonForest = res.getJsonForest();
                }
            }
            for (CommonResponse responsei : response) {
                commonRequests.add(new CommonRequest(responsei.getClient(), new RandomForestTrainReq(responsei.getClient(), jsonForest.get(responsei.getClient().toString())), 99));
            }
            return commonRequests;
        } else {
            for (CommonResponse responsei : response) {
                RandomForestTrainRes res = (RandomForestTrainRes) responsei.getBody();
                if (res.isActive()) {
                    updateMetrics(res);
                }
                commonRequests.add(new CommonRequest(responsei.getClient(), new RandomForestTrainReq(responsei.getClient(), "init"), 99));
            }
            return commonRequests;
        }
    }

    /**
     * 模型预测的流程控制
     *
     * @param responses 客户端回传信息
     * @return 根据返回结果生成的下一轮请求
     */
    @Override
    public List<CommonRequest> inferenceControl(List<CommonResponse> responses) {
        inferencePhase = getNextPhase(inferencePhase);
        if (inferencePhase == -1) {
            return inferencePhase1(responses);
        } else if (inferencePhase == -2) {
            return inferencePhase2(responses);
        } else if (inferencePhase == -3) {
            return inferencePhase3(responses);
        } else {
            throw new UnsupportedOperationException();
        }
    }


    /**
     * 推理初始化
     *
     * @param clientInfos 客户端列表，包含是否有label，
     * @param predictUid  需要推理的uid
     * @return 推理初始化请求
     */
    public List<CommonRequest> initInference(List<ClientInfo> clientInfos, String[] predictUid, Map<String, Object> others) {
        originIdArray = predictUid;
        clientInfoList = clientInfos;
        List<CommonRequest> initRequests = new ArrayList<>();
        for (ClientInfo clientInfo : clientInfoList) {
            InferenceInit init = new InferenceInit(predictUid);
            CommonRequest request = new CommonRequest(clientInfo, init, inferencePhase);
            initRequests.add(request);
        }
        return initRequests;
    }

    private List<CommonRequest> inferencePhase1(List<CommonResponse> responses) {
        Set<Integer> blacklist = new HashSet<>();
        for (CommonResponse response : responses) {
            InferenceInitRes res = (InferenceInitRes) (response.getBody());
            final List<Integer> result = Arrays.stream(res.getUid()).boxed().collect(Collectors.toList());
            blacklist.addAll(result);
        }

        final int existUidSize = originIdArray.length - blacklist.size();
        // 特殊情况，所有的ID都不需要预测
        if (existUidSize == 0) {
            finishInference = true;
        }

        // 过滤不需要预测的uid, filterSet返回的位置，所以根据位置过滤
        List<Integer> queryIdHasFiltered = new ArrayList<>();
        for (int i = 0; i < originIdArray.length; i++) {
            if (!blacklist.contains(i)) {
                queryIdHasFiltered.add(i);
            }
        }
        //过滤后的可推理的id 列表
        int[] idIndexArray = queryIdHasFiltered.stream().mapToInt(x -> x).toArray();
        String[] idArray = Arrays.stream(idIndexArray).mapToObj(x -> originIdArray[x]).toArray(String[]::new);
        List<CommonRequest> initRequests = new ArrayList<>();
        for (ClientInfo clientInfo : clientInfoList) {
            InferenceInit init = new InferenceInit(idArray);
            CommonRequest request = new CommonRequest(clientInfo, init, -1);
            initRequests.add(request);
        }
        return initRequests;
    }

    private List<CommonRequest> inferencePhase2(List<CommonResponse> responses) {
        int numInferenceSamples = 0;
        // inference initialization
        for (CommonResponse res : responses) {
            RandomForestInferMessage body = (RandomForestInferMessage) res.getBody();
            // parse inferenceType
            if (numInferenceSamples == 0) {
                // get inference data uid order
                inferenceDataUid = body.getInferenceUid();
                for (int idx = 0; idx < inferenceDataUid.length; idx++) {
                    mapInferenceOrder.put(inferenceDataUid[idx], idx);
                }
                numInferenceSamples = inferenceDataUid.length;
            }
        }

        Map<Integer, Map<Integer, List<String>>> treeInfo = new HashMap<>();
        for (CommonResponse res : responses) {
            RandomForestInferMessage body = (RandomForestInferMessage) res.getBody();
            if (!"active".equals(body.getType())) {
                Map<Integer, Map<Integer, List<String>>> treeInfoi = body.getTreeInfo();
                for (Map.Entry<Integer, Map<Integer, List<String>>> treeId : treeInfoi.entrySet()) {
                    if (treeInfo.containsKey(treeId.getKey())) {
                        Map<Integer, List<String>> subTreeInfo = treeInfo.get(treeId.getKey());
                        Map<Integer, List<String>> subClientMap = treeInfoi.get(treeId.getKey());
                        subTreeInfo.putAll(subClientMap);
                        treeInfo.put(treeId.getKey(), subTreeInfo);
                    } else {
                        treeInfo.put(treeId.getKey(), treeId.getValue());
                    }
                }
            }
        }

        List<CommonRequest> initRequests = new ArrayList<>();
        for (ClientInfo clientInfo : clientInfoList) {
            RandomForestInferMessage init = new RandomForestInferMessage(inferenceDataUid, treeInfo);
            CommonRequest request = new CommonRequest(clientInfo, init, -2);
            initRequests.add(request);
        }
        return initRequests;
    }

    private List<CommonRequest> inferencePhase3(List<CommonResponse> responses) {
        finishInference = true;
        for (CommonResponse resi : responses) {
            RandomForestInferMessage body = (RandomForestInferMessage) resi.getBody();
            if ("active".equals(body.getType())) {
                inferenceRes = body.getLocalPredict();
            }
        }
        return createNullRequest(responses, -3);
    }

    /**
     * 计算预测结果
     *
     * @param responses 各个返回结果
     * @return 预测结果
     */
    public PredictRes postInferenceControl(List<CommonResponse> responses) {
        //每个预测样本一个预测值
        logger.info("Post Inference Control...");
        double[] treePred;
        double[] res = new double[originIdArray.length];
        treePred = inferenceRes;
        for (int i = 0; i < res.length; i++) {
            // use idArray get correct order
            if (mapInferenceOrder.containsKey(originIdArray[i])) {
                int idx = mapInferenceOrder.get(originIdArray[i]);
                res[i] = treePred[idx];
            } else {
                res[i] = Double.NaN;
            }
        }
        return new PredictRes(new String[]{"label"}, res);
    }

    @Override
    public MetricValue readMetrics() {
        MetricValue metricValue;
        metricValue = new MetricValue(metricMap, metric2DimMap, validateMetricMap, validateMetric2DimMap, featureImportance, -1);
        return metricValue;
    }

    public boolean isContinue() {
        return !(isForestSent);
    }

    /**
     * 根据上一轮步骤及客户端返回结果，确定当前步骤
     *
     * @param old 上一轮步骤
     * @return 当前步骤
     */
    private int getNextPhase(int old) {
        if (old > 0) {
            return old;
        } else {
            if (old == -255) {
                return -1;
            } else if (old == -1) {
                return -2;
            } else if (old == -2) {
                return -3;
            } else {
                //  old == -4
                return -4;
            }
        }
    }

    public boolean isInferenceContinue() {
        //logger.info(String.format("isInitInference: %s", isInitInference));
        return !finishInference;
    }

    /**
     * @param responses 响应
     * @param phase     当前步骤
     * @return 请求
     */
    private List<CommonRequest> createNullRequest(List<CommonResponse> responses, int phase) {
        List<CommonRequest> req = new ArrayList<>();
        for (CommonResponse res : responses) {
            CommonRequest reqi = new CommonRequest(res.getClient(), null, phase);
            req.add(reqi);
        }
        return req;
    }

    private void updateMetrics(RandomForestTrainRes rfRes) {
        if (rfRes.isActive()) {
            metricMap = rfRes.getTrainMetric();
            metric2DimMap = rfRes.getTrainMetric2Dim();
            featureImportance = rfRes.getFeatureImportance();
        }
    }

    public AlgorithmType getAlgorithmType() {
        return algorithmType;
    }

}
