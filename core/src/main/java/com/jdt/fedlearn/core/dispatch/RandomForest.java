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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jdt.fedlearn.core.dispatch.common.Control;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.common.*;
import com.jdt.fedlearn.core.entity.randomForest.RandomforestMessage;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.entity.randomForest.*;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.core.type.RFDispatchPhaseType;
import com.jdt.fedlearn.core.type.RFModelPhaseType;
import com.jdt.fedlearn.grpc.federatedlearning.InputMessage;
import com.jdt.fedlearn.grpc.federatedlearning.MultiOutputMessage;
import com.jdt.fedlearn.grpc.federatedlearning.OutputMessage;
import com.jdt.fedlearn.grpc.federatedlearning.Vector;
import com.jdt.fedlearn.core.parameter.RandomForestParameter;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.data.Pair;
import org.ejml.simple.SimpleMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;


public class RandomForest implements Control {

    private static final AlgorithmType algorithmType = AlgorithmType.RandomForest;
    // log
    private static final Logger logger = LoggerFactory.getLogger(RandomForest.class);
    // 客户端维护
    public List<ClientInfo> clientInfoList;
    public ClientInfo activeClient;
    // 随机森林类
    public TypeRandomForest forest;
    Random rand = new Random(666);
    RandomForestEncryptData encryptionData;
    // new param
    int minSampleSplit;
    private final RandomForestParameter parameter;
    // 加这个 splitLine 完全是因为log太多不加看不清……
    private final String splitLine = "========================================================";
    private int numSamples = -1;
    private int nJobs = 1;
    private Map<ClientInfo, ArrayList<Integer>[]> clientFeatureMap = new HashMap<>();
    private int round = 0;
    private int nextLogRound = 1;
    private final Map<MetricType, List<Pair<Integer, Double>>> metricMap = new HashMap<>();
    private Map<MetricType, List<Pair<Integer, String>>> metricArrMap = new HashMap<>();
    private boolean isInitTrain = false;
    private boolean hasSentY = false;
    private boolean isInitInference = false;
    private boolean isForestSent = false;
    private int trainPhase = 0;
    private int inferencePhase = -255;
    //inference
    private String[] idArray;
    private String[] inferenceDataUid;
    private String inferenceType;
    //接口输入的需要推理的id列表
    private String[] originIdArray;
    //过滤后的可推理的id 列表
    private int[] idIndexArray;
    private double[] localPred;
    private double[] inferenceRes;
    private Map<String, Integer> mapInferenceOrder = new HashMap<>();
    private Map<String, Double> featureImportance = new HashMap<>();

    public RandomForest(RandomForestParameter parameter) {
        this.parameter = parameter;
    }

    // 训练开始前初始化调用的初始化方法

    /**
     * 收集客户端信息及参数信息
     * 根据信息初始化随机森林
     * 计算出样本采样,特征采样,数据集等信息发送给客户端
     *
     * @param clientInfos 客户端列表
     * @param idMap       id对齐信息
     * @param features    特征信息
     * @param other       其他信息
     * @return 给客户端的初始化请求
     */
    public List<CommonRequest> initControl(List<ClientInfo> clientInfos,
                                           MatchResult idMap,
                                           Map<ClientInfo, Features> features,
                                           Map<String, Object> other) {
        logger.info("Init control");
        this.clientInfoList = clientInfos;

        // 初始化参数
        int maxDepth = parameter.getMaxDepth();
        int maxSampledFeatures = parameter.getMaxSampledFeatures();
        int numPercentiles = parameter.getNumPercentiles();
        int numTrees = parameter.getNumTrees();
        int maxTreeSamples = parameter.getMaxTreeSamples();
        minSampleSplit = parameter.getMinSamplesSplit();
        nJobs = parameter.getnJobs();
        rand = new Random(parameter.getRandomSeed());
        // 设置 metricMap
         String[] arr = MetricType.getArrayMetrics();
        for (MetricType metricType : parameter.getEval_metric()) {
            if (Arrays.asList(arr).contains(metricType.getMetric())) {
                List<Pair<Integer, String>> tmpRoundMetricArr = new ArrayList<>();
                tmpRoundMetricArr.add(new Pair<>(round, "-1."));
                metricArrMap.put(metricType, tmpRoundMetricArr);
            } else {
                List<Pair<Integer, Double>> tmpRoundMetric = new ArrayList<>();
                tmpRoundMetric.add(new Pair<>(round, -1.));
                metricMap.put(metricType, tmpRoundMetric);
            }
        }

        // get num samples
        if (numSamples == -1) {
            // 切 feature 的联邦学习可以用这种方法或缺样本数
            numSamples = idMap.getLength();
        }

        // 新建随机森林instance
        // TODO 目前thisIdMap还是不work
        forest = new TypeRandomForest(
                numTrees,
                maxDepth,
                maxSampledFeatures,
                maxTreeSamples,
                numPercentiles,
                numSamples,
                nJobs,
                rand);
        // multi processing change
        if (nJobs > 1) {
            forest.getTrainNodeAllTrees();
        } else {
            forest.getTrainNode();
        }

        // get total sample ids
        Set<Integer> setSampleIds = new HashSet<>();
        for (TreeNodeRF treei : forest.getRoots()) {
            List<Integer> sampleIdi = treei.sampleIds;
            setSampleIds.addAll(sampleIdi);
        }
        // test efficiency, for debug only
        logger.info("Start sample id encoding, length = " + setSampleIds.size());
        Integer datasetSize = 400000;
        String strSampleIds = DataUtils.sampleIdToString(DataUtils.asSortedList(setSampleIds), datasetSize);
        logger.info("Sample id encoding end...");
        // 生成 Traininit
        List<CommonRequest> res = new ArrayList<>();
        // feature allocation
        List<Integer> selectedFeatureClient = new ArrayList<>();
        for (int i = 0; i < clientInfos.size(); i++) {
            Features localFeature = features.get(clientInfos.get(i));
            int featureNum = localFeature.getFeatureList().size() - 1; // get rid of uid
            if (localFeature.hasLabel()) {
                featureNum = featureNum - 1; // get rid of label
            }
            for (int k = 0; k < featureNum; k++) {
                selectedFeatureClient.add(i);
            }
        }
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
                tmp = tmp + featureAllocationTree.get(ci);
                if (id < numTrees - 1) {
                    tmp = tmp + ",";
                }
                featureAllocation.put(ci, tmp);
            }
        }
        logger.info(String.format("Feature allocation: %s", featureAllocation.toString()));
        for (ClientInfo clientInfo : clientInfos) {
            Features localFeature = features.get(clientInfo);
            Map<String, Object> others = new HashMap<>();
            others.put("sampleId", strSampleIds);
            others.put("featureAllocation", featureAllocation.get(clientInfo));
//            other.put("dataset", clientInfo.getDataset());
            TrainInit nestedReq = new TrainInit(parameter, localFeature, idMap.getMatchId(), others);
            CommonRequest request = CommonRequest.buildTrainInitial(clientInfo, nestedReq);
            res.add(request);
        }

        logger.info("Finish init control!" + splitLine);
        logger.info(String.valueOf(forest.getRoots().get(0)));

        return res;
    }

    // TODO refactor RandomForestReq as a subclass of CommonRequest
//    @Override
//    public List<RandomForestReq> control(String trainId, int phase, List<RandomForestRes> response) {
//        List<RandomForestRes> RF_Req;
//        return RF_Req;
//    }

    /**
     * 服务端的整体流程控制
     *
     * @param response 客户端返回结果
     * @return 给客户端的请求
     */
    @Override
    public List<CommonRequest> control(List<CommonResponse> response) {
        trainPhase = getNextPhase(trainPhase, response);
        switch (RFDispatchPhaseType.valueOf(trainPhase)) {
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
            case SEND_FINAL_MODEL:
                return sendForest(response);
            default:
                throw new UnsupportedOperationException();
        }
    }

    /**
     * 尝试获取当前待分裂节点
     * 发送样本id给主动方计算预测值
     *
     * @param response 客户端返回结果
     * @return 给客户端的请求
     */
    public List<CommonRequest> controlPhase1(List<CommonResponse> response) {
        logger.info("Algo multi phase 1 start" + splitLine);
        List<CommonRequest> commonRequests = new ArrayList<>();
        Map<Integer, TreeNodeRF> currentNodeMap = forest.getTrainNodeAllTrees();
        if (currentNodeMap.isEmpty()) {
            // 结束流程
            forest.trainStop = true;
            logger.info("Finish! Stop!");
            return createNullRequest(response, trainPhase);
        }
        // 发送样本id获取预测均值
        StringBuilder treeIdsBuilder = new StringBuilder();
        StringBuilder sampleIdsBuilder = new StringBuilder();
        HashMap<Integer, ArrayList<Integer>> treeToSampleID = new HashMap<>();

        for (Map.Entry<Integer, TreeNodeRF> keyi : currentNodeMap.entrySet()) {
            treeIdsBuilder.append(keyi.getValue().treeId + "|");
            Long tid = keyi.getValue().treeId;
            treeToSampleID.put(tid.intValue(), keyi.getValue().sampleIds);
            logger.info("Tid: " + tid);
            logger.info("sampleIds: " + keyi.getValue().sampleIds);
            String sampleIdi = keyi.getValue().sampleIds.stream().map(Object::toString)
                    .collect(Collectors.joining(","));
            sampleIdsBuilder.append(sampleIdi + "|");
//            logger.info(String.format("Current tree info: Tree %s, num samples: %s",
//                    currentNodeMap.get(keyi).treeId, currentNodeMap.get(keyi).sampleIds.size()));

        }
        String treeIds = treeIdsBuilder.toString();
        String sampleIds = sampleIdsBuilder.toString();
        // clip last "|"
        treeIds = treeIds.substring(0, treeIds.length() - 1);
        sampleIds = sampleIds.substring(0, sampleIds.length() - 1);

        String extraMessage = treeIds + "||" + sampleIds;
        logger.info("extraMessage: " + extraMessage);
        for (CommonResponse responsei : response) {
            RandomForestReq req = new RandomForestReq(responsei.getClient(),
                    "", -1, null, extraMessage, treeToSampleID); // testing version, remove extraMessage
            req.tidToXsample();
            commonRequests.add(new CommonRequest(responsei.getClient(), req, trainPhase));
        }
        logger.info("Algo multi phase 1 end" + splitLine);
        return commonRequests;
    }

    /**
     * 如果是初始化：
     * 收集同态加密后的label，设置特征采样
     * 如果非初始化：
     * 收集该节点预测均值计算性能指标
     * 尝试获取未分裂节点，如果失败则算法结束
     * 如果未分裂节点的样本数过少或分裂层数过深，则将该节点设置为叶节点
     * 根据未分裂节点的样本id选取对应加密后的label发送到客户端进行计算
     *
     * @param response 客户端返回结果
     * @return 给客户端的请求
     */
    public List<CommonRequest> controlPhase2(List<CommonResponse> response) {
        List<CommonRequest> commonRequests = new ArrayList<>();
        logger.info("Algo multi phase 2 start" + splitLine);
        // 尝试获取待生长节点
        Map<Integer, TreeNodeRF> currentNodeMap;

        // 先处理 active 端回传的加密的 Y
        //Double loss = -1.;
        Map<String, Double> loss = new HashMap<>();
        Map<String, String> lossArr = new HashMap<>();
        if (isInitTrain) {
            currentNodeMap = forest.getTrainNodeAllTrees();
            for (CommonResponse responsei : response) {
                RandomforestMessage bodyOri = (RandomforestMessage) responsei.getBody();
                String body = bodyOri.getResponseStr();
//                logger.info("body: " + body);
                if (!("".equals(body))) {
                    // 传入预测均值
                    //logger.info("Receive body: " + body);
                    String[] s = body.split("\\|\\|");
                    String[] predictions = s[0].split(",");
                    int idx = 0;
                    for (Map.Entry<Integer, TreeNodeRF> i : currentNodeMap.entrySet()) {
                        i.getValue().prediction = Double.valueOf(predictions[idx]);
                        idx = idx + 1;
                    }
                    //loss = Double.valueOf(s[1]);
                    loss = Arrays.stream(s[1].substring(1, s[1].length() - 1).split(", "))
                            .map(entry -> entry.split("="))
                            .collect(Collectors.toMap(entry -> entry[0], entry -> Double.parseDouble(entry[1])));
                    if (s.length == 3) {
                        lossArr = Arrays.stream(s[2].split(";;")).map(entry -> entry.split(": "))
                                .collect(Collectors.toMap(entry -> entry[0], entry -> entry[1]));
                    }
                }
            }
        } else {
            // body contructure: encrypted Y || all feature id map
            for (CommonResponse responsei : response) {
                RandomforestMessage bodyOri = (RandomforestMessage) responsei.getBody();
                String[] body = bodyOri.getResponseStr().split("\\|\\|");
                // 解析加密的Y
                if (!"".equals(body[0])) {
                    activeClient = responsei.getClient();
                    InputMessage encryptY = DataUtils.json2inputMessage(body[0]);
                    // 初始化 encryptionData
                    switch (parameter.getEncryptionType()) {
                        case Paillier:
                            encryptionData = new PaillierEncryptData(encryptY);
                            break;
                        case IterativeAffine:
                            encryptionData = new IterativeAffineEncryptData(encryptY);
                            break;
                        default:
                            throw new IllegalArgumentException("Unsupported encryption type!");
                    }
                }
                // feature map
                //logger.info(body[1]);
                if ("".equals(body[1])) {
                    // no sampled features, pass
                } else {
                    String[] featureMapStr = body[1].split(";", -1);
                    ArrayList<Integer>[] featureMap = new ArrayList[parameter.getNumTrees()];
                    for (int i = 0; i < featureMap.length; i++) {
                        featureMap[i] = new ArrayList<>();
                        if (!("".equals(featureMapStr[i]))) {
                            for (String si : featureMapStr[i].split(",")) {
                                featureMap[i].add(Integer.valueOf(si));
                            }
                        }
                    }
                    clientFeatureMap.put(responsei.getClient(), featureMap);
                }
            }
            isInitTrain = true;
            currentNodeMap = forest.getTrainNodeAllTrees();
        }

        //logger.info(String.format("RMSE: %s", loss.get("RMSE")) + splitLine);
        // update metricMap
        if (round >= nextLogRound) {
            for (Map.Entry<MetricType, List<Pair<Integer, Double>>> metric : metricMap.entrySet()) {
                metric.getValue().add(new Pair<>(round, -1.));
            }
            for (Map.Entry<MetricType, List<Pair<Integer, String>>> metric : metricArrMap.entrySet()) {
                metric.getValue().add(new Pair<>(round, "-1."));
            }
            nextLogRound = nextLogRound * 2;
        }
        if (round != 0) {
            for (MetricType metricType : parameter.getEval_metric()) {
                if (metricMap.containsKey(metricType)) {
                    metricMap.get(metricType).set(metricMap.get(metricType).size() - 1, new Pair<>(round, loss.get(metricType.getMetric())));
                }
                if (metricArrMap.containsKey(metricType)) {
                    metricArrMap.get(metricType).set(metricArrMap.get(metricType).size() - 1, new Pair<>(round, lossArr.get(metricType.getMetric())));
                }
            }
        }
        printMetricMap();

        // 过滤出需要生长的节点
        ArrayList<Integer> removeList = new ArrayList<>();
        for (Map.Entry<Integer, TreeNodeRF> i : currentNodeMap.entrySet()) {
            TreeNodeRF nodei = i.getValue();
            if ((nodei.numSamples <= minSampleSplit) || (nodei.level() + 1 > forest.getMaxDepth())) {
                logger.info("Too few samples, making a leaf.");
                nodei.makeLeaf(activeClient);
                removeList.add(i.getKey());
            }
        }
        for (int i : removeList) {
            currentNodeMap.remove(i);
        }
        // 如果没有节点，则结束算法
        if (currentNodeMap.isEmpty()) {
            // 结束流程
            //forest.trainStop = true;
            //logger.info("Finish! Stop!");
            return createNullRequest(response, trainPhase);
        }

        int idx = 0;
        StringBuilder treeIdsBuilder = new StringBuilder();
        StringBuilder sampleIdsBuilder = new StringBuilder();

        HashMap<Integer, ArrayList<Integer>> treeIdToSampleId = new HashMap<>();
        for (Map.Entry<Integer, TreeNodeRF> i : currentNodeMap.entrySet()) {
            TreeNodeRF nodei = i.getValue();
            String sampleIdi = nodei.sampleIds.toString();
            treeIdsBuilder.append(nodei.treeId + "|");
            sampleIdsBuilder.append(sampleIdi + "|");
            idx = idx + 1;

            Long tid = nodei.treeId;
            treeIdToSampleId.put(tid.intValue(), nodei.sampleIds);
        }
        String treeIds = treeIdsBuilder.toString();
        String sampleIds = sampleIdsBuilder.toString();
        treeIds = treeIds.substring(0, treeIds.length() - 1);
        sampleIds = sampleIds.substring(0, sampleIds.length() - 1);
        if (!hasSentY) {
            InputMessage Y = encryptionData.getEncryptedY();
            hasSentY = true;

            for (CommonResponse responsei : response) {
//                RandomForestReq reqi = new RandomForestReq(responsei.getClient(), DataUtils.inputMessage2json(Y),
//                        -1, null, treeIds + "||" + sampleIds); // previous version
                RandomForestReq nestedReqi = new RandomForestReq(responsei.getClient(), DataUtils.inputMessage2json(Y),
                        -1, null, treeIds + "||" + sampleIds, treeIdToSampleId); // testing version
                nestedReqi.tidToXsample();
                CommonRequest reqi = new CommonRequest(responsei.getClient(),
                        nestedReqi,
                        trainPhase);
                commonRequests.add(reqi);
            }
        } else {
            for (CommonResponse responsei : response) {
//                RandomForestReq reqi = new RandomForestReq(responsei.getClient(), "",
//                        -1, null, treeIds + "||" + sampleIds);
                RandomForestReq nestedReqi = new RandomForestReq(responsei.getClient(), "",
                        -1, null, treeIds + "||" + sampleIds, treeIdToSampleId);
                nestedReqi.tidToXsample();
                CommonRequest reqi = new CommonRequest(responsei.getClient(),
                        nestedReqi,
                        trainPhase);
                commonRequests.add(reqi);
            }
        }

        logger.info("Algo multi phase 2 end" + splitLine);
        return commonRequests;
    }


    /**
     * 收集客户端的Phase2传回的信息
     * 拼接信息发送给主动方。
     *
     * @param response 客户端返回结果
     * @return 给客户端的请求
     */
    public List<CommonRequest> controlPhase3(List<CommonResponse> response) {
        List<CommonRequest> commonRequests = new ArrayList<>();
        logger.info("Algo multi phase 3 start" + splitLine);
        RandomForestRes[] res1 = new RandomForestRes[response.size()];
        ArrayList<ClientInfo> Y1sOwners = new ArrayList<>();
        Map<Integer, TreeNodeRF> currentNodeMap = forest.getTrainNodeAllTrees();
        if (currentNodeMap.isEmpty()) {
            // 结束流程
            forest.trainStop = true;
            logger.info("Finish! Stop!");
            return createNullRequest(response, trainPhase);
        }
        StringBuilder jsonStrBuilder = new StringBuilder();
        for (int i = 0; i < response.size(); i++) {
            CommonResponse responsei = response.get(i);
            RandomForestRes resi = (RandomForestRes) responsei.getBody();
            res1[i] = resi;
            Y1sOwners.add(responsei.getClient());
            jsonStrBuilder.append(resi.toJson() + "|||");
        }
        String jsonStr = jsonStrBuilder.toString();
        for (TreeNodeRF nodei : currentNodeMap.values()) {
            nodei.Y1ClientMapping = Y1sOwners;
        }
        for (RandomForestRes resi : res1) {
            CommonRequest reqi;
            RandomForestReq req;
            if (resi.getIsActive()) {
                // 主动端
                req = new RandomForestReq(resi.getClient(),
                        jsonStr,
                        -1,
                        null,
                        resi.getExtraInfo(),
                        resi.getTidToSampleId());
            } else {
                req = new RandomForestReq(resi.getClient(),
                        "",
                        resi.getTreeId(),
                        resi.getSampleId(),
                        "",
                        resi.getTidToSampleId());
            }
            req.tidToXsample();
            reqi = new CommonRequest(resi.getClient(), req, trainPhase);
            commonRequests.add(reqi);
        }
        logger.info("Algo multi phase 3 end" + splitLine);
        return commonRequests;
    }

    /**
     * 收集Phase3回传信息
     * 判断分裂情况：若分裂失败，则将当前节点置为叶节点；若分裂成功，写入分裂信息，并生成请求发送给分裂方
     *
     * @param response 客户端返回结果
     * @return 给客户端的请求
     */
    public List<CommonRequest> controlPhase4(List<CommonResponse> response) {
        List<CommonRequest> commonRequests = new ArrayList<>();
        logger.info("Algo multi phase 4 start" + splitLine);
        Map<Integer, TreeNodeRF> currentNodeMap = forest.getTrainNodeAllTrees();
//        List<Boolean> is_active_list = new ArrayList<>();
        ArrayList<Integer> releaseNode = new ArrayList<>();
        String[] exInfo = new String[2];

        // 先解包 Phase 3 返回结果
        for (CommonResponse responsei : response) {
            RandomForestRes res = (RandomForestRes) responsei.getBody();
            boolean isActive = res.getIsActive();
//            is_active_list.add(isActive);
            if (isActive) {
                // active 端
//                exInfo = res.getExtraInfo().split("\\|\\|");
                exInfo[0] = res.getTidToSampleId().keySet().stream().map(Object::toString)
                        .collect(Collectors.joining("|"));
                exInfo[1] = res.getTidToSampleId().values().stream().map(Object::toString).collect(Collectors.joining("|"));

                //                String[] treeIds = exInfo[0].split("\\|");
                MultiOutputMessage responsePhase3 = DataUtils.json2MultiOutputMessage(res.getBody());
                for (int i = 0; i < responsePhase3.getMessagesCount(); i++) {
                    OutputMessage messagei = responsePhase3.getMessages(i);
                    int ownerId = (int) messagei.getValues(0);
//                    int treeId = Integer.parseInt(treeIds[i]);
                    int treeId = (int) (res.getTidToSampleId().keySet().toArray()[i]);
                    TreeNodeRF nodei = currentNodeMap.get(treeId);
                    nodei.featureId = (int) messagei.getValues(1);
                    double optPercentile = messagei.getValues(2);
                    nodei.score = messagei.getValues(3);
                    String nodeStrMessage = messagei.getMessage();
                    JsonObject nodeJsonMessage = JsonParser.parseString(nodeStrMessage).getAsJsonObject();
                    if (nodeJsonMessage.get("is_leaf").getAsInt() == 1) {
                        logger.info(String.format("Node id: %s does not split, make a node...", nodei.nodeId));
                        nodei.makeLeaf(activeClient);
                        releaseNode.add(treeId);
                    } else {
                        nodei.party = nodei.Y1ClientMapping.get(ownerId);
                        nodei.percentile = ((optPercentile + 1) / forest.getNumPercentiles()) * 100.;
//                        logger.info(String.format("Tree id: %s split on party %s", treeId, nodei.party.toString()));
                    }
                }
            }
        }
        // release leaf node
        for (int treeId : releaseNode) {
            currentNodeMap.remove(treeId);
        }
        // Phase 4 - Split the instances into two groups (left vs. right) -------------------------------
        ObjectMapper objectMapper = new ObjectMapper();
        for (CommonResponse resi : response) {
            CommonRequest reqi = null;
            if (currentNodeMap.isEmpty()) {
                // is leaf, skip
                RandomForestReq nestedReqi = new RandomForestReq(resi.getClient(),
                        "", -1, null, String.join("||", exInfo));
                nestedReqi.setSkip(true);
                nestedReqi.tidToXsample();
                reqi = new CommonRequest(resi.getClient(), nestedReqi, trainPhase);
            } else {

                StringBuilder jsonStrBuilder = new StringBuilder();
                ArrayList<String> treeIds = new ArrayList<>();
                ArrayList<String> sampleIds = new ArrayList<>();
                HashMap<Integer, ArrayList<Integer>> tidToSampleId = new HashMap<>();
                for (Map.Entry<Integer, TreeNodeRF> treeId : currentNodeMap.entrySet()) {
                    TreeNodeRF nodei = currentNodeMap.get(treeId.getKey());
                    if (Objects.equals(resi.getClient(), nodei.party)) {
                        // fill in request
                        tidToSampleId.put(treeId.getKey(), nodei.sampleIds);
                        Map<String, Double> tmp = new HashMap<String, Double>();
                        tmp.put("treeId", Double.valueOf(treeId.getKey()));
                        tmp.put("featureId", Double.valueOf(nodei.featureId));
                        tmp.put("percentile", nodei.percentile);
                        treeIds.add(String.valueOf(treeId.getKey()));
                        sampleIds.add(nodei.sampleIds.toString());
                        try {
                            jsonStrBuilder.append(objectMapper.writeValueAsString(tmp) + "||");
                        } catch (JsonProcessingException e) {
                            logger.error("JsonProcessingException: ", e);
                        }
                    }
                }
                String jsonStr = jsonStrBuilder.toString();
                RandomForestReq nestedReqi = null;
                if (treeIds.size() > 0) {
                    // 从该 client 分割样本
                    exInfo = new String[2];

                    StringBuilder exInfoBuilder0 = new StringBuilder();
                    StringBuilder exInfoBuilder1 = new StringBuilder();

                    for (String treeId : treeIds) {
                        exInfoBuilder0.append(treeId + "|");
                    }
                    for (String sampleId : sampleIds) {
                        exInfoBuilder1.append(sampleId + "|");
                    }
                    exInfo[0] = exInfoBuilder0.toString();
                    exInfo[1] = exInfoBuilder1.toString();

                    exInfo[0] = exInfo[0].substring(0, exInfo[0].length() - 1);
                    exInfo[1] = exInfo[1].substring(0, exInfo[1].length() - 1);

                    nestedReqi = new RandomForestReq(resi.getClient(),
                            jsonStr, -1, null, String.join("||", exInfo), tidToSampleId);
                } else {
                    nestedReqi = new RandomForestReq(resi.getClient(),
                            jsonStr, -1, null, "", tidToSampleId);
                }
                nestedReqi.tidToXsample();
                reqi = new CommonRequest(resi.getClient(), nestedReqi, trainPhase);
            }
            commonRequests.add(reqi);
        }
        logger.info("Algo multi phase 4 end" + splitLine);
        return commonRequests;
    }

    public List<CommonRequest> controlPhase5(List<CommonResponse> response) {
        List<CommonRequest> commonRequests = new ArrayList<>();
        logger.info("Algo multi phase 5 start" + splitLine);
        MultiOutputMessage responsePhase4 = null;
        Map<Integer, TreeNodeRF> currentNodeMap = forest.getTrainNodeAllTrees();
        if (currentNodeMap.isEmpty()) {
            // 完成全部的节点计算，退出
            forest.trainStop = true;
        }

        // 收集 Phase 4 的信息，对节点进行分割
        for (CommonResponse responsei : response) {
            RandomForestRes res = (RandomForestRes) responsei.getBody();
            if ("".equals(res.getBody())) {
                continue;
            } else {
                String[] exInfo = res.getExtraInfo().split("\\|\\|");
                String[] treeIds = exInfo[0].split("\\|");
                responsePhase4 = DataUtils.json2MultiOutputMessage(res.getBody());
                for (int i = 0; i < treeIds.length; i++) {
                    int treeId = Integer.parseInt(treeIds[i]);
                    TreeNodeRF nodei = currentNodeMap.get(treeId);
                    // get outputMessage
                    OutputMessage message = responsePhase4.getMessages(i);
                    ClientInfo targetClient = responsei.getClient();
                    // A mask indicating how the instance should be grouped
                    Vector maskLeft = message.getVectors(0);
                    assert maskLeft.getValuesCount() == nodei.sampleIds.size() :
                            "Unexpected length of mask at Phase 4";
                    String nodeStrMessage = message.getMessage();

                    JsonObject nodeJsonMessage = JsonParser.parseString(nodeStrMessage).getAsJsonObject();
                    if (nodeJsonMessage.has("feature_opt")) {
                        int featureOpt = nodeJsonMessage.get("feature_opt").getAsInt();
                        int realFeatureOpt = clientFeatureMap.get(targetClient)[treeId].get(featureOpt);
                        nodeJsonMessage.addProperty("feature_opt", realFeatureOpt);
                    }
                    nodei.referenceJson = nodeJsonMessage;
                    nodei.thres = nodeJsonMessage.get("value_opt").getAsDouble();

                    ArrayList<Integer> leftSampleIds = new ArrayList<>();
                    ArrayList<Integer> rightSampleIds = new ArrayList<>();

                    // gather two groups of y for left and right branches
                    for (int idx = 0; idx < maskLeft.getValuesCount(); idx++) {
                        if (maskLeft.getValues(idx) == 1) {
                            leftSampleIds.add(nodei.sampleIds.get(idx));
                        } else {
                            rightSampleIds.add(nodei.sampleIds.get(idx));
                        }
                    }

                    logger.info(String.format("Tree %s node %s (%s samples) split to %s (%s sample) and %s (%s samples)",
                            nodei.treeId, nodei.nodeId, nodei.sampleIds.size(), nodei.nodeId * 2 + 1,
                            leftSampleIds.size(), nodei.nodeId * 2 + 2, rightSampleIds.size()));
                    logger.info(String.format("Split side: %s:%s, split score %s,%s  ,split feature: %s, split value: %s",
                            nodei.party.getIp(),
                            nodei.party.getPort(),
                            nodei.getNumSamples(),
                            nodei.getScore(),
                            nodei.referenceJson.get("feature_opt"),
                            nodei.referenceJson.get("value_opt")));

                    // feature importance
                    String key = nodei.party.getIp() + ":" + nodei.party.getPort() + "=" + nodei.referenceJson.get("feature_opt");
                    if (featureImportance.containsKey(key)) {
                        featureImportance.put(key, featureImportance.get(key) + nodei.getScore() * nodei.getNumSamples() / 100.0);
                    } else {
                        featureImportance.put(key, nodei.getScore() * nodei.getNumSamples());
                    }

                    nodei.left = new TreeNodeRF(leftSampleIds, nodei.nodeId * 2 + 1, nodei.treeId);
                    nodei.right = new TreeNodeRF(rightSampleIds, nodei.nodeId * 2 + 2, nodei.treeId);

                }
            }
        }

        // 释放 current node，完成这一次的split
        forest.releaseTreeNodeAllTrees();

        for (CommonResponse resi : response) {
            RandomForestReq reqi = new RandomForestReq(resi.getClient(),
                    "", -1, null, "");
            reqi.tidToXsample();
            commonRequests.add(new CommonRequest(resi.getClient(), reqi, trainPhase));
        }
        logger.info("Algo multi phase 5 end" + splitLine);

        // 完成一次分裂，round + 1
        round = round + 1;

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
        List<Map.Entry<String, Double>> featureImportanceList = featureImportance.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed()).collect(Collectors.toList());
        logger.info("featureImportanceList" + featureImportanceList);
        List<CommonRequest> commonRequests = new ArrayList<>();
        if (response.get(0).getBody() != null && ((RandomforestMessage) response.get(0).getBody()).getResponseStr().equals("finish")) {
            isForestSent = true;
            return createNullRequest(response, trainPhase);
        } else {
            //String jsonForest = serialize() + "\003";
            Map<ClientInfo, String> jsonForest = serialize();
            assert jsonForest != null;
            for (CommonResponse responsei : response) {
                commonRequests.add(new CommonRequest(responsei.getClient(), new RandomforestMessage(jsonForest.get(responsei.getClient())), trainPhase));
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
        inferencePhase = getNextPhase(inferencePhase, responses);
        String trainId = 1 + "_" + getAlgorithmType().getAlgorithm(); //TODO
        if (inferencePhase == -1) {
            return inferencePhase1(trainId, responses);
        } else if (inferencePhase == -2) {
            return inferencePhase2(trainId, responses);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public List<CommonRequest> inferencePhase1(String trainId, List<CommonResponse> responses) {

        Set<Integer> blacklist = new HashSet<>();
        for (CommonResponse response : responses) {
            InferenceInitRes boostN1Req = (InferenceInitRes) (response.getBody());
            //TODO 根据 isAllowList判断
            final List<Integer> result = Arrays.stream(boostN1Req.getUid()).boxed().collect(Collectors.toList());
            blacklist.addAll(result);
        }

        final int existUidSize = originIdArray.length - blacklist.size();
        // 特殊情况，所有的ID都不需要预测
        if (existUidSize == 0) {
            isInitInference = true;
        }

        // 过滤不需要预测的uid, filterSet返回的位置，所以根据位置过滤
        List<Integer> queryIdHasFiltered = new ArrayList<>();
        for (int i = 0; i < originIdArray.length; i++) {
            if (!blacklist.contains(i)) {
                queryIdHasFiltered.add(i);
            }
        }
        idIndexArray = queryIdHasFiltered.stream().mapToInt(x -> x).toArray();
        idArray = Arrays.stream(idIndexArray).mapToObj(x -> originIdArray[x]).toArray(String[]::new);

        List<CommonRequest> initRequests = new ArrayList<>();
        for (ClientInfo clientInfo : clientInfoList) {
            InferenceInit init = new InferenceInit(idArray);
            CommonRequest request = new CommonRequest(clientInfo, init, -1);
            initRequests.add(request);
        }
        return initRequests;
    }


    public List<CommonRequest> inferencePhase2(String trainId, List<CommonResponse> responses) {
        int numInferenceSamples = 0;
        for (int i = 0; i < responses.size(); i++) {
            CommonResponse resi = responses.get(i);
            // TODO: check if active client is always zero
            Randomforestinfer2Message body = (Randomforestinfer2Message) resi.getBody();
            // parse inferenceType
            if (!"".equals(body.getType())) {
                inferenceType = body.getType();
                // get local model predict
                localPred = body.getLocalPredict();
            }
            if (numInferenceSamples == 0) {
                // get inference data uid order
                inferenceDataUid = body.getInferenceUid();
                for (int idx = 0; idx < inferenceDataUid.length; idx++) {
                    mapInferenceOrder.put(inferenceDataUid[idx], idx);
                }
                numInferenceSamples = inferenceDataUid.length;
//                numInferenceSamples = Integer.parseInt(bodys[1]);
            }
        }
        if ("one-shot".equals(inferenceType)) {
            isInitInference = true;
            // one shot inference
            Map<Integer, Map<Integer, List<String>>> treeInfo = new HashMap<>();
            for (CommonResponse resi : responses) {
                Randomforestinfer2Message body = (Randomforestinfer2Message) resi.getBody();
                ;
                ObjectMapper mapper = new ObjectMapper();
                TypeReference<HashMap<Integer, Map<Integer, List<String>>>> typeRef
                        = new TypeReference<HashMap<Integer, Map<Integer, List<String>>>>() {
                };
                try {
                    Map<Integer, Map<Integer, List<String>>> singleClientMap =
                            mapper.readValue(body.getModelString(), typeRef);
                    for (Map.Entry<Integer, Map<Integer, List<String>>> treeId : singleClientMap.entrySet()) {
                        if (treeInfo.containsKey(treeId.getKey())) {
                            Map<Integer, List<String>> subTreeInfo = treeInfo.get(treeId.getKey());
                            Map<Integer, List<String>> subClientMap = singleClientMap.get(treeId.getKey());
                            subTreeInfo.putAll(subClientMap);
                            treeInfo.put(treeId.getKey(), subTreeInfo);
                        } else {
                            treeInfo.put(treeId.getKey(), treeId.getValue());
                        }
                    }
                } catch (JsonProcessingException e) {
                    logger.error("JsonProcessingException: ", e);
                }
            }
            // do inference
            inferenceRes = new double[inferenceDataUid.length];
            Arrays.fill(inferenceRes, 0.);
            for (Map.Entry<Integer, Map<Integer, List<String>>> treeId : treeInfo.entrySet()) {
                int[] nodeIds = new int[inferenceDataUid.length];
                int countNode = 0;
                Arrays.fill(nodeIds, 0);
                Map<Integer, List<String>> singleTreeInfo = treeInfo.get(treeId.getKey());
                while (countNode < nodeIds.length) {
                    for (int i = 0; i < nodeIds.length; i++) {
                        if (nodeIds[i] != -1) {
                            String val = singleTreeInfo.get(nodeIds[i]).get(i);
                            if ("L".equals(val)) {
                                nodeIds[i] = nodeIds[i] * 2 + 1;
                            } else if ("R".equals(val)) {
                                nodeIds[i] = nodeIds[i] * 2 + 2;
                            } else {
                                inferenceRes[i] = inferenceRes[i] + Double.parseDouble(val);
                                nodeIds[i] = -1;
                                countNode = countNode + 1;
                            }
                        }
                    }
                }
            }
            // divide by numTrees
            int numTrees = treeInfo.keySet().size();
            for (int i = 0; i < inferenceRes.length; i++) {
                inferenceRes[i] = inferenceRes[i] / numTrees;
            }
            return createNullRequest(responses, inferencePhase);
        } else {
            throw new IllegalArgumentException("Unsupported inference type!");
        }
    }

    /**
     * 计算预测结果
     *
     * @param responses1 各个返回结果
     * @return 预测结果
     */
    public PredictRes postInferenceControl(List<CommonResponse> responses1) {
        //每个预测样本一个预测值
        logger.info("Post Inference Control...");
        double[] treePred = new double[idArray.length];
        double[] res = new double[originIdArray.length];
        if ("query".equals(inferenceType)) {
            SimpleMatrix pred = forest.getPrediction();
            for (int i = 0; i < treePred.length; i++) {
                treePred[i] = pred.get(i);
            }
            //double[] res = new double[pred.numCols()];
        } else {
            treePred = inferenceRes;
        }

        for (int i = 0; i < res.length; i++) {
            // use idArray get correct order
            if (mapInferenceOrder.containsKey(originIdArray[i])) {
                int idx = mapInferenceOrder.get(originIdArray[i]);
                res[i] = treePred[idx] + localPred[idx];
            } else {
                res[i] = Double.NaN;
            }
        }
        return new PredictRes(new String[]{"label"}, res);
    }

    @Override
    public MetricValue readMetrics() {
        return  new MetricValue(metricMap);
    }

    public List<Map.Entry<String, Double>> getFeatureImportance() {
        List<Map.Entry<String, Double>> featureImportanceList = featureImportance.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed()).collect(Collectors.toList());
        logger.info("featureImportanceList" + featureImportanceList);
        return featureImportanceList;
    }

    public Map<MetricType, List<Pair<Integer, String>>> metricArr() {
        return metricArrMap;
    }

    //满足终止条件，返回true
    public boolean isStop() {
        return (forest.trainStop && isForestSent);
    }

    public boolean isContinue() {
        return !(forest.trainStop && isForestSent);
    }

    /**
     * 根据上一轮步骤及客户端返回结果，确定当前步骤
     *
     * @param old       上一轮步骤
     * @param responses 上一轮客户端返回结果
     * @return 当前步骤
     */
    public int getNextPhase(int old, List<CommonResponse> responses) {

        // 传树到client = 99
        if (old > 0) {
            // old > 0 训练流程
            if (forest.trainStop) {
                if (!isForestSent) {
                    return 99;
                }
            }
            if (old == 5) {
                return 1;
            } else if ((forest.currentNode == null) && (forest.treeNodeMap.isEmpty())) {
                return 1;
            } else {
                return old + 1;
            }
        } else if (old == 0) {
            return 1;
        } else {
            // old < 0 inference流程
//            if (!isInitInference) {
//                return -1;
//            }
            if (old == -255) {
                return -1;
            } else if (old == -1) {
                return -2;
            } else {
                //  old == -4
                return -3;
            }
        }
    }

    public boolean isInferenceContinue() {
        //logger.info(String.format("isInitInference: %s", isInitInference));
        if (!isInitInference) {
            return true;
        } else {
            if ("query".equals(inferenceType)) {
                return !forest.inferStop;
            } else {
                return false;
            }
        }
    }

    /**
     * 推理初始化
     *
     * @param clientInfos 客户端列表，包含是否有label，
     * @param predictUid  需要推理的uid
     * @return 推理初始化请求
     */
    public List<CommonRequest> initInference(List<ClientInfo> clientInfos, String[] predictUid) {
        idArray = predictUid;
        clientInfoList = clientInfos;
        originIdArray = predictUid;
        List<CommonRequest> initRequests = new ArrayList<>();
        for (ClientInfo clientInfo : clientInfoList) {
            InferenceInit init = new InferenceInit(predictUid);
            CommonRequest request = new CommonRequest(clientInfo, init, inferencePhase);
            initRequests.add(request);
        }
        return initRequests;
    }


    public List<CommonRequest> createNullRequest(List<CommonResponse> response, int p) {
        List<CommonRequest> req = new ArrayList<>();
        for (CommonResponse resi : response) {
            CommonRequest reqi = new CommonRequest(resi.getClient(), null, p);
            req.add(reqi);
        }
        return req;
    }


    public Map<String, String> getModel(ClientInfo client) {
        Map<String, String> strTrees = new HashMap<>();
        List<TreeNodeRF> roots = forest.getRoots();
        int numTrees = 0;

        for (int i = 0; i < roots.size(); i++) {
            logger.info("Tree to json...");

            TreeNodeRF aTree = roots.get(i);
            if (null != aTree) { /* check is null tree */
                if (aTree.isLeaf == false) {
                    /* is non-null tree */
                    strTrees.put(String.format("Tree%s", numTrees), forest.tree2json(aTree, client));
                    numTrees += 1;
                }
            }
        }
        strTrees.put("numTrees", Integer.toString(numTrees));
        logger.info("strTrees: " + strTrees.toString());
        return strTrees;
    }


    public Map<ClientInfo, String> serialize() {
        Map<ClientInfo, String> map = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            for (ClientInfo client : clientInfoList) {
                Map<String, String> trees = getModel(client);
                map.put(client, objectMapper.writeValueAsString(trees));
            }
            return map;
        } catch (JsonProcessingException e) {
            logger.error("serialize error", e);
            return null;
        }
    }


    public String printMetricMap() {
        String mapAsString = metricMap.keySet().stream()
                .map(key -> key + "=" + metricMap.get(key))
                .collect(Collectors.joining(", ", "{", "}"));
        logger.info("metricMap: " + mapAsString);
        return mapAsString;
    }

    public AlgorithmType getAlgorithmType() {
        return algorithmType;
    }

    public void setForTest(List<ClientInfo> clientInfos, int numTrees, int maxDepth, int maxSampledFeatures, int maxTreeSamples, int numPercentiles, int numSamples, int nJobs) {
        this.nJobs = nJobs;
        this.numSamples = numSamples;
        this.clientInfoList = clientInfos;
        this.forest = new TypeRandomForest(
                numTrees,
                maxDepth,
                maxSampledFeatures,
                maxTreeSamples,
                numPercentiles,
                numSamples,
                nJobs,
                rand);
        for (int i = 0; i < numTrees; i++) {
            Map<Integer, TreeNodeRF> currentNodeMap = forest.getTrainNodeAllTrees();
            TreeNodeRF nodei = currentNodeMap.get(i);
            nodei.featureId = 1;
            nodei.score = 3.00;
            nodei.Y1ClientMapping = clientInfos;
//            nodei.makeLeaf(activeClient);
            nodei.party = nodei.Y1ClientMapping.get(i);
            nodei.percentile = 3.00;
        }
        List<Pair<Integer, Double>> tmpRoundMetric = new ArrayList<>();
        tmpRoundMetric.add(new Pair<>(1, -1.));
        metricMap.put(MetricType.RMSE, tmpRoundMetric);
        Map<ClientInfo, ArrayList<Integer>[]> clientFeatureMap = new HashMap<>();
        for (int i = 0; i < 3; i++) {
            ArrayList<Integer>[] featureMap = new ArrayList[2];
            featureMap[0] = new ArrayList<>();
            featureMap[1] = new ArrayList<>();
            if (i == 0) {
                featureMap[0].add(0);
                featureMap[1].add(1);
            } else if (i == 1) {
                featureMap[0].add(0);
                featureMap[0].add(1);
                featureMap[1].add(1);
            } else {
                featureMap[0].add(0);
                featureMap[1].add(2);
                featureMap[1].add(3);
            }
            clientFeatureMap.put(clientInfos.get(i), featureMap);
        }
        this.clientFeatureMap = clientFeatureMap;
    }

    // 推理单元测试需要
    public void setForTest(List<ClientInfo> clientInfos, String[] predictUid, String[] idArray, double[] inferenceRes, Map<String, Integer> mapInferenceOrder, double[] localPred) {
        originIdArray = predictUid;
        clientInfoList = clientInfos;
        this.idArray = idArray;
        this.inferenceRes = inferenceRes;
        this.mapInferenceOrder = mapInferenceOrder;
        this.localPred = localPred;
    }
}
