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
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jdt.fedlearn.core.dispatch.common.Control;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.base.EmptyMessage;
import com.jdt.fedlearn.core.entity.base.SingleElement;
import com.jdt.fedlearn.core.entity.common.*;
import com.jdt.fedlearn.core.entity.randomForest.RandomforestMessage;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.entity.randomForest.*;
import com.jdt.fedlearn.core.preprocess.TrainTestSplit;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.core.type.RFDispatchPhaseType;
import com.jdt.fedlearn.core.type.RFModelPhaseType;
import com.jdt.fedlearn.core.type.data.Tuple2;
import com.jdt.fedlearn.core.util.Tool;
import com.jdt.fedlearn.grpc.federatedlearning.Vector;
import com.jdt.fedlearn.grpc.federatedlearning.*;
import com.jdt.fedlearn.core.parameter.RandomForestParameter;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.data.Pair;
import org.ejml.simple.SimpleMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;


public class DistributedRandomForest implements Control {

    private static final AlgorithmType algorithmType = AlgorithmType.DistributedRandomForest;
    // log
    private static final Logger logger = LoggerFactory.getLogger(DistributedRandomForest.class);
    // 客户端维护
    public List<ClientInfo> clientInfoList;
    public ClientInfo activeClient;
    // 随机森林类
    public TypeRandomForest forest;
    Random rand = new Random(666);
    // new param
    int minSampleSplit;
    private final RandomForestParameter parameter;
    // 加这个 splitLine 完全是因为log太多不加看不清……
    private final String splitLine = "========================================================";
    private int numSamples = -1;
    private int nJobs = 1;
    private Map<ClientInfo, ArrayList<Integer>[]> clientFeatureMap = new HashMap<>();
    private int round = 0;
    private final Map<MetricType, List<Pair<Integer, Double>>> metricMap = new HashMap<>();
    private Map<MetricType, List<Pair<Integer, String>>> metricArrMap = new HashMap<>();
    private final Map<MetricType, List<Pair<Integer, Double>>> validateMetricMap = new HashMap<>();
    private Map<MetricType, List<Pair<Integer, String>>> validateMetricArrMap = new HashMap<>();
    private boolean isInitTrain = false;
    private boolean isInitInference = false;
    private boolean isForestSent = false;
    private Map<Integer, Integer>[] sampleMap;
    private boolean managerHasLabel = true;
    private PaillierKeyPublic keyPublic;
    private PaillierKeyPublic[] keyPublics;
    private PaillierVector[] yPvec1;
    private PaillierVector yPvec;
    //接口输入的需要推理的id列表
    private String[] originIdArray;
    //过滤后的可推理的id 列表
    private int[] idIndexArray;
    private ArrayList<Integer> fromManager = new ArrayList<>();
    private int trainPhase = 0;
    private int inferencePhase = -255;
    //inference
    private String[] idArray;
    private String[] inferenceDataUid;
    private String inferenceType;
    private double[] localPred;
    private double[] inferenceRes;
    private List<Integer> testId; // TODO 是否需要改回String待确定
    private Map<String, Integer> mapInferenceOrder = new HashMap<>();
    private Map<String, Double> featureImportance = new HashMap<>();
    private int bestRound = 0;

    public DistributedRandomForest(RandomForestParameter parameter) {
        this.parameter = parameter;
    }

    // 训练开始前初始化调用的初始化方法

    /**
     * 收集客户端信息及参数信息
     * 根据信息初始化随机森林
     * 计算出样本采样,特征采样,数据集等信息发送给客户端
     *
     *
     * @param clientInfos   客户端列表
     * @param idMap         id对齐信息
     * @param features      特征信息
     * @param other         其他信息
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
        // TODO Random seed 设为参数
        double splitRatio = (double)other.get("splitRatio");
        // splitRation等于0时，训练集取全部数据集，验证集为空
        if (splitRatio >= 1) {
            splitRatio = 0;
        }
        // 按交叉验证参数划分idMap
        Tuple2<List<Integer>, List<Integer>> trainTestSplit = TrainTestSplit.trainTestSplit(idMap.getLength(), splitRatio, 666);
        assert trainTestSplit != null;
        // test index id
        // TODO 这里修改之前储存的是加密后的validation的ID list；后面匹配需要加密还是非加密状态
        testId = trainTestSplit._2();

        // 第0轮指标赋值
        String[] arr = MetricType.getArrayMetrics();
        for (MetricType metricType : parameter.getEval_metric()) {
            if (Arrays.asList(arr).contains(metricType.getMetric())) {
                List<Pair<Integer, String>> tmpRoundMetricArr = new ArrayList<>();
                tmpRoundMetricArr.add(new Pair<>(round, "-1."));
                metricArrMap.put(metricType, tmpRoundMetricArr);
                List<Pair<Integer, String>> tmpRoundMetricArr1 = new ArrayList<>();
                tmpRoundMetricArr1.add(new Pair<>(round, "-1."));
                validateMetricArrMap.put(metricType, tmpRoundMetricArr1);
            } else {
                List<Pair<Integer, Double>> tmpRoundMetric = new ArrayList<>();
                tmpRoundMetric.add(new Pair<>(round, -1.));
                metricMap.put(metricType, tmpRoundMetric);
                List<Pair<Integer, Double>> tmpRoundMetric1 = new ArrayList<>();
                tmpRoundMetric1.add(new Pair<>(round, -1.));
                validateMetricMap.put(metricType, tmpRoundMetric1);
            }
        }

        // get num samples
        if (numSamples == -1) {
            // 切 feature 的联邦学习可以用这种方法或缺样本数
            numSamples = trainTestSplit._1().size();
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
        Map<Long, ArrayList<Integer>> sampleIdsMap = Maps.newHashMap();
        sampleMap = new Map[forest.getRoots().size()];
        // 获取每棵树采样样本的行和总样本对应行的映射关系
        int treeIdi = 0;
        for(TreeNodeRF treeNodeRF : forest.getRoots()){
            long treeId = treeNodeRF.treeId;
            ArrayList<Integer> sampleIds = treeNodeRF.sampleIds;
            sampleIdsMap.put(treeId, sampleIds);
            sampleMap[treeIdi] = new HashMap<>();
            for (int j=0;j<sampleIds.size();j++) {
                sampleMap[treeIdi].put(sampleIds.get(j), new Integer(j));
            }
            treeIdi++;
        }
        Integer datasetSize = 400000;
        String strSampleIds = DataUtils.sampleIdToString(DataUtils.asSortedList(setSampleIds), datasetSize);
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
        logger.info(selectedFeatureClient.toString());
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
                int count = Math.max(1, featureAllocationTree.get(ci));
                tmp = tmp + count;
                if (id < numTrees - 1) {
                    tmp = tmp + ",";
                }
                featureAllocation.put(ci, tmp);
            }
        }
        logger.info(featureAllocation.toString());
        int[] testIndex = testId.stream().mapToInt(Integer::valueOf).toArray();
        for (ClientInfo clientInfo : clientInfos) {
            Features localFeature = features.get(clientInfo);
            Map<String, Object> others = new HashMap<>();
            others.put("sampleId", strSampleIds);
            others.put("featureAllocation", featureAllocation.get(clientInfo));
            others.put("sampleIds", sampleIdsMap);
            TrainInit nestedReq = new TrainInit(parameter, localFeature, idMap.getMatchId(), others, testIndex);
            CommonRequest request = CommonRequest.buildTrainInitial(clientInfo, nestedReq);
            res.add(request);
        }
        logger.info("Finish init control!" + splitLine);
        logger.info(String.valueOf(forest.getRoots().get(0)));

        return res;
    }

    /**
     * 服务端的整体流程控制
     *
     * @param response 客户端返回结果
     * @return 给客户端的请求
     */
    @Override
    public List<CommonRequest> control(List<CommonResponse> response) {
        trainPhase = getNextPhase(trainPhase);
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
            case SEND_CURRENT_MODEL:
                return controlPhase6(response);
            case INIT_VALIDATION:
                return controlPhase7();
            case DO_VALIDATION:
                return controlPhase8(response);
            case GET_VALIDATION_METRIC:
                return controlPhase9(response);
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
     * @param response   客户端返回结果
     * @return 给客户端的请求
     */
    public List<CommonRequest> controlPhase1(List<CommonResponse> response) {
        logger.info("Algo phase 1 start" + splitLine);
        Message message = response.get(0).getBody();
        List<CommonRequest> commonRequests = new ArrayList<>();
        Map<Integer, TreeNodeRF> currentNodeMap = forest.getTrainNodeAllTrees();
        if (!(message instanceof SingleElement)) {
            if (currentNodeMap.isEmpty() || ((message != null) && ((DistributedRandomForestRes) message).getBody().equals("early stopping success"))) {
                // 结束流程
                forest.trainStop = true;
                logger.info("Finish! Stop!");
                return createNullRequest(response, trainPhase);
            }
        }
        // 发送样本id获取预测均值
        StringBuilder treeIdsBuilder = new StringBuilder();
        StringBuilder sampleIdsBuilder = new StringBuilder();

        for (Map.Entry<Integer, TreeNodeRF> keyi : currentNodeMap.entrySet()) {
            treeIdsBuilder.append(keyi.getValue().treeId + "|");
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
        for (CommonResponse responsei : response) {
            DistributedRandomForestReq req = new DistributedRandomForestReq(responsei.getClient(),
                    "", -1, null, extraMessage);
            commonRequests.add(new CommonRequest(responsei.getClient(), req, trainPhase));
        }
        logger.info("Algo phase 1 end" + splitLine);
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
     * 根据客户端返回信息判断是否为分布式方，执行对应操作
     *
     * @param response   客户端返回结果
     * @return 给客户端的请求
     */
    public List<CommonRequest> controlPhase2(List<CommonResponse> response) {
        List<CommonRequest> commonRequests = new ArrayList<>();
        logger.info("Algo phase 2 start" + splitLine);
        // 尝试获取待生长节点
        Map<Integer, TreeNodeRF> currentNodeMap;

        // 先处理 active 端回传的加密的 Y
        //Double loss = -1.;
        Map<String, Double> loss = new HashMap<>();
        Map<String, String> lossArr = new HashMap<>();
        if (isInitTrain) {
            currentNodeMap = forest.getTrainNodeAllTrees();
            for (CommonResponse responsei : response) {
                RandomforestMessage bodyOri = (RandomforestMessage)responsei.getBody();
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
                RandomforestMessage bodyOri = (RandomforestMessage)responsei.getBody();
                String[] body = bodyOri.getResponseStr().split("\\|\\|");
                // 解析加密的Y
                if (!"".equals(body[0])) {
                    activeClient = responsei.getClient();
                    String[] encryptYs = body[0].split("\\|");
                    // 根据encryptYs的长度判断是否为分布式端回传的信息
                    if (encryptYs.length > 1) {
                        logger.info("managerHasLabel: true");
                        InputMessage[] encryptY = new InputMessage[encryptYs.length];
                        yPvec1 = new PaillierVector[encryptYs.length];
                        for (int i = 0; i < encryptYs.length; i++){
                            encryptY[i] = DataUtils.json2inputMessage(encryptYs[i]);
                            yPvec1[i] = encryptY[i].getPailliervectors(0);
                        }
                        keyPublic = encryptY[0].getPaillierkeypublic();
                    } else {
                        logger.info("managerHasLabel: false");
                        managerHasLabel = false;
                        InputMessage encryptY = DataUtils.json2inputMessage(body[0]);
                        yPvec = encryptY.getPailliervectors(0);
                        keyPublic = encryptY.getPaillierkeypublic();
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

        if (round != 0) {
            for (MetricType metricType : parameter.getEval_metric()) {
                if (metricMap.containsKey(metricType)) {
                    metricMap.get(metricType).add(new Pair<>(round, loss.get(metricType.getMetric())));
                }
                if (metricArrMap.containsKey(metricType)) {
                    metricArrMap.get(metricType).add(new Pair<>(round, lossArr.get(metricType.getMetric())));
                }
            }
        }
        printMetricMap();
        round = round + 1;
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
        PaillierVector[][] yPhase2 = new PaillierVector[currentNodeMap.keySet().size()][1];
//        PaillierKeyPublic[] distributedKeyPublics = new PaillierKeyPublic[currentNodeMap.keySet().size()];
        for (Map.Entry<Integer, TreeNodeRF> i : currentNodeMap.entrySet()) {
            TreeNodeRF nodei = i.getValue();
            String sampleIdi = nodei.sampleIds.toString();
            PaillierVector.Builder subYBuilder = PaillierVector.newBuilder();
            // 判断分布式方是否有label，采取不同的策略读取加密的label
            if (managerHasLabel) {
                logger.info("use managerHasLabel: true");
                for (int si: nodei.sampleIds) {
                    subYBuilder.addValues(yPvec1[i.getKey()].getValues(sampleMap[i.getKey()].get(si)));
                }
            } else {
                for (int si : nodei.sampleIds) {
                    subYBuilder.addValues(yPvec.getValues(si));
                }
            }
            yPhase2[idx][0] = subYBuilder.build();
            treeIdsBuilder.append(nodei.treeId + "|");
            sampleIdsBuilder.append(sampleIdi + "|");
            idx = idx + 1;
        }
        String treeIds = treeIdsBuilder.toString();
        String sampleIds = sampleIdsBuilder.toString();
        treeIds = treeIds.substring(0, treeIds.length() - 1);
        sampleIds = sampleIds.substring(0, sampleIds.length() - 1);
        keyPublics = new PaillierKeyPublic[yPhase2.length];
        Arrays.fill(keyPublics, keyPublic);
        MultiInputMessage subYs = DataUtils.prepareMultiInputMessage(
                new Matrix[][]{},
                new Vector[][]{},
                new Double[][]{},
                new PaillierMatrix[][]{},
                yPhase2,
                new PaillierValue[][]{},
                keyPublics,
                currentNodeMap.keySet().size());
        for (CommonResponse responsei: response) {
            DistributedRandomForestReq reqI = new DistributedRandomForestReq(responsei.getClient(), DataUtils.inputMessage2json(subYs),
                    -1, fromManager, treeIds + "||" + sampleIds);
            CommonRequest reqi = new CommonRequest(responsei.getClient(),
                    reqI,
                    trainPhase);
            commonRequests.add(reqi);
        }
        logger.info("Algo phase 2 end" + splitLine);
        return commonRequests;
    }

    /**
     * 收集客户端的Phase2传回的信息
     * 拼接信息发送给主动方。
     *
     * @param response   客户端返回结果
     * @return 给客户端的请求
     */
    public List<CommonRequest> controlPhase3(List<CommonResponse> response) {
        List<CommonRequest> commonRequests = new ArrayList<>();
        logger.info("Algo phase 3 start" + splitLine);
        DistributedRandomForestRes[] res1 = new DistributedRandomForestRes[response.size()];
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
            DistributedRandomForestRes resi = (DistributedRandomForestRes)responsei.getBody();
            res1[i] = resi;
            Y1sOwners.add(responsei.getClient());
            jsonStrBuilder.append(resi.toJson()).append("|||");
        }
        String jsonStr = jsonStrBuilder.toString();
        for (TreeNodeRF nodei : currentNodeMap.values()) {
            nodei.Y1ClientMapping = Y1sOwners;
        }
        for (DistributedRandomForestRes resi : res1) {
            CommonRequest reqI;
            DistributedRandomForestReq req;
            if (resi.getIsActive()) {
                // 主动端
                req = new DistributedRandomForestReq(resi.getClient(),
                        jsonStr,
                        -1,
                        null,
                        resi.getExtraInfo());
            } else {
                req = new DistributedRandomForestReq(resi.getClient(),
                        "",
                        resi.getTreeId(),
                        resi.getSampleId(),
                        "");
            }
            reqI = new CommonRequest(resi.getClient(), req, trainPhase);
            commonRequests.add(reqI);
        }
        logger.info("Algo phase 3 end" + splitLine);
        return commonRequests;
    }

    /**
     * 收集Phase3回传信息
     * 判断分裂情况：若分裂失败，则将当前节点置为叶节点；若分裂成功，写入分裂信息，并生成请求发送给分裂方
     *
     * @param response   客户端返回结果
     * @return 给客户端的请求
     */
    public List<CommonRequest> controlPhase4(List<CommonResponse> response) {
        List<CommonRequest> commonRequests = new ArrayList<>();
        logger.info("Algo phase 4 start" + splitLine);
        Map<Integer, TreeNodeRF> currentNodeMap = forest.getTrainNodeAllTrees();
//        List<Boolean> is_active_list = new ArrayList<>();
        ArrayList<Integer> releaseNode = new ArrayList<>();
        String[] exInfo = null;

        // 先解包 Phase 3 返回结果
        for (CommonResponse responsei : response) {
            DistributedRandomForestRes res = (DistributedRandomForestRes)responsei.getBody();
            boolean isActive = res.getIsActive();
//            is_active_list.add(isActive);
            if (isActive) {
                // active 端
                exInfo = res.getExtraInfo().split("\\|\\|");
                String[] treeIds = exInfo[0].split("\\|");
                MultiOutputMessage responsePhase3 = DataUtils.json2MultiOutputMessage(res.getBody());
                for (int i = 0; i < responsePhase3.getMessagesCount(); i++) {
                    OutputMessage messagei = responsePhase3.getMessages(i);
                    int ownerId = (int) messagei.getValues(0);
                    int treeId = Integer.parseInt(treeIds[i]);
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
                assert exInfo != null;
                DistributedRandomForestReq reqI = new DistributedRandomForestReq(resi.getClient(),
                        "", -1, null, String.join("||", exInfo));
                reqI.setSkip(true);
                reqi = new CommonRequest(resi.getClient(), reqI, trainPhase);
            } else {

                StringBuilder jsonStrBuilder = new StringBuilder();
                ArrayList<String> treeIds = new ArrayList<>();
                ArrayList<String> sampleIds = new ArrayList<>();

                for (Map.Entry<Integer, TreeNodeRF> treeId : currentNodeMap.entrySet()) {
                    TreeNodeRF nodei = currentNodeMap.get(treeId.getKey());
                    if (Objects.equals(resi.getClient(), nodei.party)) {
                        // fill in request
                        Map<String, Double> tmp = new HashMap<String, Double>();
                        tmp.put("treeId", Double.valueOf(treeId.getKey()));
                        tmp.put("featureId", Double.valueOf(nodei.featureId));
                        tmp.put("percentile", nodei.percentile);
                        tmp.put("nodeId", Double.valueOf(nodei.nodeId));
                        treeIds.add(String.valueOf(treeId.getKey()));
                        sampleIds.add(nodei.sampleIds.toString());
                        try {
                            jsonStrBuilder.append(objectMapper.writeValueAsString(tmp) + "||");
                        } catch (JsonProcessingException e) {
                            logger.error("JsonProcessingException: ",e);
                        }
                    }
                }
                String jsonStr = jsonStrBuilder.toString();
                DistributedRandomForestReq reqI = null;
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

                    reqI = new DistributedRandomForestReq(resi.getClient(),
                            jsonStr, -1, null, String.join("||", exInfo));
                } else {
                    reqI = new DistributedRandomForestReq(resi.getClient(),
                            jsonStr, -1, null, "");
                }
                reqi = new CommonRequest(resi.getClient(), reqI, trainPhase);
            }
            commonRequests.add(reqi);
        }
        logger.info("Algo phase 4 end" + splitLine);
        return commonRequests;
    }

    /**
     * 收集Phase4回传信息
     * 写入分裂阈值信息
     * 生成左右子节点
     * 根据样本id的左右子节点从属关系，分配样本id到左右子节点中
     * 子节点设置为待分裂节点
     *
     * @param response   客户端返回结果
     * @return 给客户端的请求
     */
    public List<CommonRequest> controlPhase5(List<CommonResponse> response) {
        List<CommonRequest> commonRequests = new ArrayList<>();
        logger.info("Algo phase 5 start" + splitLine);
        MultiOutputMessage responsePhase4 = null;
        Map<Integer, TreeNodeRF> currentNodeMap = forest.getTrainNodeAllTrees();
        if (currentNodeMap.isEmpty()) {
            // 完成全部的节点计算，退出
            forest.trainStop = true;
        }

        // 收集 Phase 4 的信息，对节点进行分割
        for (CommonResponse responsei : response) {
            DistributedRandomForestRes res = (DistributedRandomForestRes)responsei.getBody();
            if ("".equals(res.getBody())) {
                continue;
            } else {
                String[] exInfo = res.getExtraInfo().split("\\|\\|");
                String[] treeIds = exInfo[0].split("\\|");
                responsePhase4 = DataUtils.json2MultiOutputMessage(res.getBody());
                for (int i = 0; i < treeIds.length; i++) {
                    int treeId = Integer.parseInt(treeIds[i]);
                    TreeNodeRF nodei = currentNodeMap.get(treeId);
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
                    logger.info(String.format("Split side: %s:%s, split score %s,%s  ,split feature: %s",
                            nodei.party.getIp(),
                            nodei.party.getPort(),
                            nodei.getNumSamples(),
                            nodei.getScore(),
                            nodei.referenceJson.get("feature_opt")));

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
            DistributedRandomForestReq reqI = new DistributedRandomForestReq(resi.getClient(),
                    "", -1, null, "");
            commonRequests.add(new CommonRequest(resi.getClient(), reqI, trainPhase));
        }
        logger.info("Algo phase 5 end" + splitLine);

        // 完成一次分裂，round + 1
//        round = round + 1;
        return commonRequests;
    }


    public List<CommonRequest> controlPhase6(List<CommonResponse> response) {
        logger.info("Train finished send forest...");
        List<Map.Entry<String, Double>> featureImportanceList = featureImportance.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed()).collect(Collectors.toList());
        logger.info("featureImportanceList" + featureImportanceList);
        List<CommonRequest> commonRequests = new ArrayList<>();
        Map<ClientInfo, String> jsonForest = serialize();
        assert jsonForest != null;
        for (CommonResponse responsei : response) {
            commonRequests.add(new CommonRequest(responsei.getClient(), new RandomforestMessage(jsonForest.get(responsei.getClient())), 6));
        }
        return commonRequests;
    }


    public List<CommonRequest> controlPhase7() {
        List<CommonRequest> initRequests = new ArrayList<>();
        for (ClientInfo clientInfo : clientInfoList) {
            CommonRequest request = new CommonRequest(clientInfo, new EmptyMessage(), 7);
            initRequests.add(request);
        }
        return initRequests;
    }

    public List<CommonRequest> controlPhase8(List<CommonResponse> responses) {
        doInference(responses);
        //每个预测样本一个预测值
        double[] treePred = new double[inferenceDataUid.length];
        double[] res = new double[inferenceDataUid.length];
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
            if (mapInferenceOrder.containsKey(inferenceDataUid[i])) {
                int idx = mapInferenceOrder.get(inferenceDataUid[i]);
                res[i] = treePred[idx] + localPred[idx];
            } else {
                res[i] = Double.NaN;
            }
        }
        List<CommonRequest> validateRequests = new ArrayList<>();
        for (CommonResponse resi : responses) {
            CommonRequest request;
            Randomforestinfer2Message body = (Randomforestinfer2Message) resi.getBody();
            if (body.getLocalPredict() != null) {
                RandomforestValidateReq req = new RandomforestValidateReq(res, inferenceDataUid);
                request = new CommonRequest(resi.getClient(), req, 8);
            } else {
                request = new CommonRequest(resi.getClient(), null, 8);
            }
            validateRequests.add(request);
        }
        return validateRequests;
    }

    private void doInference(List<CommonResponse> responses) {
        int numInferenceSamples = 0;
        // inference initialization
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
        // one shot inference
        Map<Integer, Map<Integer, List<String>>> treeInfo = new HashMap<>();
        for (CommonResponse resi : responses) {
            Randomforestinfer2Message body = (Randomforestinfer2Message) resi.getBody();
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
                        if (singleTreeInfo.get(nodeIds[i]) == null) {
                            inferenceRes[i] = inferenceRes[i] + 1;
                            nodeIds[i] = -1;
                            countNode = countNode + 1;
                        } else {
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
        }
        // divide by numTrees
        int numTrees = treeInfo.keySet().size();
        for (int i = 0; i < inferenceRes.length; i++) {
            inferenceRes[i] = inferenceRes[i] / numTrees;
        }
    }

    public List<CommonRequest> controlPhase9(List<CommonResponse> responses) {
        Map<String, Double> loss = new HashMap<>();
        Map<String, String> lossArr = new HashMap<>();
        for (CommonResponse responsei : responses) {
            RandomforestMessage bodyOri = (RandomforestMessage) responsei.getBody();

            if (bodyOri != null) {
                // 传入预测均值
                String body = bodyOri.getResponseStr();
                //logger.info("Receive body: " + body);
                String[] s = body.split("\\|\\|");
                //loss = Double.valueOf(s[1]);
                loss = Arrays.stream(s[0].substring(1, s[0].length() - 1).split(", "))
                        .map(entry -> entry.split("="))
                        .collect(Collectors.toMap(entry -> entry[0], entry -> Double.parseDouble(entry[1])));
                if (s.length == 2) {
                    lossArr = Arrays.stream(s[1].split(";;")).map(entry -> entry.split(": "))
                            .collect(Collectors.toMap(entry -> entry[0], entry -> entry[1]));
                }
            }
        }
        if (round != 0) {
            for (MetricType metricType : parameter.getEval_metric()) {
                if (validateMetricMap.containsKey(metricType)) {
                    validateMetricMap.get(metricType).add(new Pair<>(round-1, loss.get(metricType.getMetric())));
//                    validateMetricMap.get(metricType).set(validateMetricMap.get(metricType).size() - 1, new Pair<>(round, loss.get(metricType.getMetric())));
                }
                if (validateMetricArrMap.containsKey(metricType)) {
                    validateMetricArrMap.get(metricType).add(new Pair<>(round-1, lossArr.get(metricType.getMetric())));
//                    validateMetricArrMap.get(metricType).set(validateMetricArrMap.get(metricType).size() - 1, new Pair<>(round, lossArr.get(metricType.getMetric())));
                }
            }
        }
//        int lastMetric = validateMetricMap.get(MetricType.AUC).size() - 1;
        printValidateMetricMap();

        //TODO erlystopping round and metrictype
        for (Map.Entry<MetricType, List<Pair<Integer, Double>>> entry : validateMetricMap.entrySet()) {
            int tmpround = entry.getValue().size();
            if (tmpround > 1000) {
                if (entry.getKey().equals(MetricType.RMSE)) {
                    List<Pair<Integer, Double>> lossMetric = entry.getValue();
                    List<Double> doubleList = lossMetric.stream().map(Pair::getValue).collect(Collectors.toList());
                    bestRound = Tool.earlyStopping(doubleList, 1000);
                    logger.info("bestRound " + bestRound);
                }
            }
        }
        List<CommonRequest> validateRequests = new ArrayList<>();
        for (CommonResponse resi : responses) {
            CommonRequest request;
//            RandomforestValidateReq req = new RandomforestValidateReq(bestRound);
            DistributedRandomForestReq req = new DistributedRandomForestReq(resi.getClient(), bestRound);
            request = new CommonRequest(resi.getClient(), req, 9);
            validateRequests.add(request);
        }
        return validateRequests;
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
        if (response.get(0).getBody() != null && ((RandomforestMessage)response.get(0).getBody()).getResponseStr().equals("finish")) {
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
        inferencePhase = getNextPhase(inferencePhase);
        if (inferencePhase == -1) {
            return inferencePhase1(responses);
        } else if (inferencePhase == -2) {
            return inferencePhase2(responses);
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
    public List<CommonRequest> initInference(List<ClientInfo> clientInfos, String[] predictUid) {
        originIdArray = predictUid;
        idArray = predictUid;
        clientInfoList = clientInfos;
        List<CommonRequest> initRequests = new ArrayList<>();
        for (ClientInfo clientInfo : clientInfoList) {
            InferenceInit init = new InferenceInit(predictUid);
            CommonRequest request = new CommonRequest(clientInfo, init, inferencePhase);
            initRequests.add(request);
        }
        return initRequests;
    }

    public List<CommonRequest> inferencePhase1(List<CommonResponse> responses) {

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


    public List<CommonRequest> inferencePhase2(List<CommonResponse> responses) {
        isInitInference = true;
        doInference(responses);
        return createNullRequest(responses, inferencePhase);

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
        double[] treePred;
        double[] res = new double[originIdArray.length];
        treePred = inferenceRes;
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
        MetricValue metricValue;
        if (bestRound != 0) {
            metricValue = new MetricValue(metricMap, metricArrMap, validateMetricMap, validateMetricArrMap,featureImportance,bestRound);
        }else{
            // -1即代表当前轮效果最优
            metricValue = new MetricValue(metricMap, metricArrMap, validateMetricMap, validateMetricArrMap,featureImportance,-1);
        }
        return metricValue;
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
     * @return 当前步骤
     */
    public int getNextPhase(int old) {

        // 传树到client = 99
        if (old > 0) {
            // old > 0 训练流程
            if (forest.trainStop) {
                if (!isForestSent) {
                    return 99;
                }
            }
            if (old == 9) {
                return 1;
            } else if (old < 5 && (forest.currentNode == null) && (forest.treeNodeMap.isEmpty()) && testId.size() != 0) {
                return 6;
            } else if (old < 5 && (forest.currentNode == null) && (forest.treeNodeMap.isEmpty()) && testId.size() == 0) {
                return 1;
            } else if (old == 5 && testId.size() == 0) {
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
    public String printValidateMetricMap() {
        String mapAsString = validateMetricMap.keySet().stream()
                .map(key -> key + "=" + validateMetricMap.get(key))
                .collect(Collectors.joining(", ", "{", "}"));
        logger.info("validateMetricMap: " + mapAsString);
        return mapAsString;
    }


    public AlgorithmType getAlgorithmType() {
        return algorithmType;
    }

    public void setForTest(List<ClientInfo> clientInfos, int numTrees, int maxDepth, int maxSampledFeatures, int maxTreeSamples,  int numPercentiles, int numSamples, int nJobs) {
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
