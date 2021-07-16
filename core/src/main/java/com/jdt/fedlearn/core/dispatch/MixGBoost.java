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
import com.jdt.fedlearn.core.encryption.common.PublicKey;
import com.jdt.fedlearn.core.encryption.fake.FakeTool;
import com.jdt.fedlearn.core.encryption.javallier.JavallierTool;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.base.SingleElement;
import com.jdt.fedlearn.core.entity.boost.FeatureLeftGH;
import com.jdt.fedlearn.core.entity.boost.GainOutput;
import com.jdt.fedlearn.core.entity.common.*;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.entity.feature.SingleFeature;
import com.jdt.fedlearn.core.entity.mixGB.*;
import com.jdt.fedlearn.core.entity.psi.MatchInitRes;
import com.jdt.fedlearn.core.exception.NotImplementedException;
import com.jdt.fedlearn.core.metrics.Metric;
import com.jdt.fedlearn.core.model.common.loss.LogisticLoss;
import com.jdt.fedlearn.core.model.common.loss.Loss;
import com.jdt.fedlearn.core.model.common.loss.SquareLoss;
import com.jdt.fedlearn.core.model.common.loss.crossEntropy;
import com.jdt.fedlearn.core.model.common.tree.MixTreeNode;
import com.jdt.fedlearn.core.parameter.MixGBParameter;
import com.jdt.fedlearn.core.psi.MappingResult;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.core.psi.mixMd5.MixMd5Match;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.core.type.MessageType;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.ObjectiveType;
import com.jdt.fedlearn.core.type.data.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author zhangwenxi
 */
public class MixGBoost implements Control {
    private static final Logger logger = LoggerFactory.getLogger(MixGBoost.class);
    private static final AlgorithmType algorithmType = AlgorithmType.MixGBoost;
    private final PrivateKey privateKey;
    private final PublicKey pubKey;
    private final EncryptionTool encryptionTool;
    private final MixGBParameter parameter;
    private boolean isStop = false;
    /**
     * 存储每一颗树的根节点
     */
    private List<MixTreeNode> curRootTreeNodeList = new ArrayList<>();
    /**
     * 最终的模型根节点
     */
    private List<MixTreeNode> finalRootNodeList = new ArrayList<>();
    /**
     * 当前正在处理的节点
     */
    private MixTreeNode curTreeNode = null;
    private int curEpochNum = 0;
    /**
     * 所有客户端共同的ID集合
     */
    private Set<Integer> commonIdSet;
    private List<ClientInfo> clientInfoList;
    /**
     * client useful fea size
     */
    private Map<ClientInfo, Integer> clientVerticalSize = new ConcurrentHashMap<>();
    private ClientInfo clientSave;
    /**
     * 所有的特征集合
     */
    private Map<String, Set<ClientInfo>> allFeatures;
    /**
     * 每个客户端所具有的非共同 ID 集合
     */
    private Map<ClientInfo, Set<Integer>> clientNoCommonIdSetMap;
    /**
     * 横向 XGB 临时分裂节点信息
     */
    private Set<Integer> tempHorizontalIL;
    /**
     * 横向 XGB 临时分裂 feature 数
     */
    private int randomFeatureSetSize;
    private int globalRecordId = 0;
    private Map<MetricType, List<Pair<Integer, Double>>> metricMap;
    private Loss loss;
    /**
     * 重复ID的数量 和对应的gi hi 映射表
     */
    private Map<Integer, IntDoubleTuple3> dupIdGHMap;
    /**
     * 是否完成推理
     */
    private boolean isStopInference = false;
    /**
     * 进行推理的树根节点的索引
     */
    private Map<Integer, MixTreeNode> recordIdNodeMap;
    private List<String> inferUid;
    /**
     * 存储所有推理实例的结果的 Map
     */
    private Map<String, Double> inferScoreMap;
    private List<MixTreeNode> inferRootNodeList;
    private Map<ClientInfo, List<String>> clientNotExistId;
    private Map<String, Double> featureThresholds;
    private Random random;

    public MixGBoost(MixGBParameter parameter) {
        this.encryptionTool = new JavallierTool();
        this.parameter = parameter;
        this.privateKey = encryptionTool.keyGenerate(1024, 64);
        this.pubKey = privateKey.generatePublicKey();
    }

    public MixGBoost(MixGBParameter parameter, Map<Integer, IntDoubleTuple3> dupIdGHMap, Map<MetricType, List<Pair<Integer, Double>>> metricMap, List<ClientInfo> clientInfos, Map<String, Set<ClientInfo>> allFeatures, MixTreeNode node, List<MixTreeNode> treeNodeList, Set<Integer> commonIdSet) {
        this.parameter = parameter == null ? new MixGBParameter() : parameter;
        this.metricMap = metricMap;
        assert clientInfos != null;
        if (clientInfos != null) {
            this.clientInfoList = clientInfos;
            this.clientSave = clientInfos.get(0);
        }
        this.dupIdGHMap = dupIdGHMap;
        this.clientNoCommonIdSetMap = new HashMap<>();
        this.commonIdSet = commonIdSet == null ? new HashSet<>() : commonIdSet;
        this.allFeatures = allFeatures;
        if (allFeatures != null) {
            this.featureThresholds = allFeatures.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> 1.0));
        }
        this.random = new Random();
        this.curTreeNode = node;
        if (treeNodeList != null) {
            this.curRootTreeNodeList = treeNodeList;
        }
        if (allFeatures != null) {
            this.randomFeatureSetSize = allFeatures.size();
        }
        for (ClientInfo client : clientInfos) {
            clientVerticalSize.put(client, 0);
        }
        this.tempHorizontalIL = new HashSet<>();
        this.encryptionTool = new FakeTool();
        this.privateKey = encryptionTool.keyGenerate(1024, 64);
        this.pubKey = privateKey.generatePublicKey();
    }

    public MixGBoost(String[] inferUid, Map<String, Double> inferScoreMap, boolean setSquare, List<MixTreeNode> inferRootNodeList, Map<Integer, MixTreeNode> recordIdNodeMap, List<ClientInfo> clientInfos) {
        this.inferUid = Arrays.asList(inferUid.clone());
        this.inferScoreMap = inferScoreMap;
        if (setSquare) {
            this.loss = new SquareLoss();
        }
        this.inferRootNodeList = inferRootNodeList;
        this.recordIdNodeMap = recordIdNodeMap;
        this.clientNotExistId = clientInfos.stream().collect(Collectors.toMap(clientInfo -> clientInfo, clientInfo -> new ArrayList<>()));
        this.encryptionTool = new FakeTool();
        this.parameter = new MixGBParameter();
        this.privateKey = encryptionTool.keyGenerate(1024, 64);
        this.pubKey = privateKey.generatePublicKey();
    }

    public Loss getLoss(MixGBParameter parameter) {
        switch (parameter.getObjective()) {
            case regLogistic:
            case binaryLogistic:
                return new LogisticLoss();
            case regSquare:
                return new SquareLoss();
            case multiSoftmax:
            case multiSoftProb:
                return new crossEntropy(parameter.getNumClass());
            default:
                throw new NotImplementedException();
        }
    }

    @Override
    public List<CommonRequest> initControl(List<ClientInfo> clientInfos,
                                           MatchResult idMap,
                                           Map<ClientInfo, Features> featureList,
                                           Map<String, Object> other) {
        // loss
        this.loss = getLoss(parameter);
        // clientList
        clientInfoList = clientInfos;
        dupIdGHMap = new HashMap<>();
        clientNoCommonIdSetMap = new HashMap<>();
        allFeatures = new HashMap<>();
        random = new Random();
        // features, IDs
        for (ClientInfo client : clientInfos) {
            clientVerticalSize.put(client, 0);
            // 初始化
            initServerInfoOnce(client, featureList.get(client));
        }
        // horizontal param
        this.randomFeatureSetSize = Integer.max((int) (allFeatures.size() * parameter.getHorizontalFeaturesRatio()), 1);

        // 设置和初始化验证数据
        // 设置共同ID列表
        // init the first epoch and its first tree
        other.put("pubkey", pubKey.serialize());
//        other.put("commonId", new ArrayList<>(commonIdSet));
        List<CommonRequest> requestList = new ArrayList<>();
        // TODO 确认是否可以这么处理
        clientInfos.forEach(client -> {
            TrainInit init = new TrainInit(parameter, featureList.get(client), null, other);
            CommonRequest request = CommonRequest.buildTrainInitial(client, init);
            requestList.add(request);
        });
        return requestList;
    }

    /**
     * 1、初始化：（1）初始化第一个epoch的信息；（2）初始化每个epoch训练的信息；
     * 2、每一个epoch进行训练；
     * 2.1 进行横向XGB训练（基于特有ID的那部分数据）
     * 2.2 进行纵向XGB训练（基于公用ID的那部分数据）
     * 2.3 确定采用横向还是纵向的方式，分别进行不同的处理；
     * 3、重复步骤2，直到当前树构建完毕，然后进行下一棵树的构建；
     * 4、如果当前epoch完毕，存在构建完毕的XGB模型，开启新一轮的epoch；
     *
     * @param responses 客户端返回结果
     * @return 将要发送给客户端的消息列表
     */
    @Override
    public List<CommonRequest> control(List<CommonResponse> responses) {
        Message message = responses.get(0).getBody();
        String trainId = 1 + getAlgorithmType().toString();
        if (message instanceof SingleElement && "init_success".equals(((SingleElement) message).getElement())) {
            return responses.parallelStream()
                    .map(response -> new CommonRequest(response.getClient(),
                            new BoostBodyReq(MessageType.GlobalInit)))
                    .collect(Collectors.toList());
        }
        MessageType messageType = responses.parallelStream().filter(response -> response.getBody() != null).map(response -> (BoostBodyRes) (response.getBody()))
                .map(BoostBodyRes::getMsgType).filter(Objects::nonNull).findAny().orElse(MessageType.GlobalInit);
        switch (messageType) {
            case GlobalInit:
                return controlIdMatch(trainId, responses);
            case UpdateGiHi:
            case HorizontalSplit:
            case VerticalSplit:
                return startNode(trainId, responses);
            case H_IL:
                return horizontalFeatureSplit(trainId, responses);
            case GkvHkv:
                return controlVerticalKVGain(trainId, responses);
            case KVGain:
                return controlVerticalSplit(trainId, responses);
            case Wj:
                return afterLeaf(trainId, responses);
            case FeatureValue:
                return controlHorizontalIL(trainId, responses);
            case GiHi:
                return controlUpdateGiHi(trainId, responses);
            case MetricValue:
                return resultControl(trainId, responses);
            default:
                throw new IllegalStateException("Unexpected messageType: " + messageType);
        }
    }

    /**
     * 判断是否停止分裂
     *
     * @return 是否停止节点的分裂
     */
    private boolean stopSplit() {
        if (curTreeNode.getInstanceIdSpaceSet().size() < parameter.getMinSampleSplit()) {
            return true;
        }
        return curTreeNode.getDepth() >= parameter.getMaxDepth();
    }

    /**
     * 判断是否停止整个森林的训练
     *
     * @return 是否停止训练
     */
    private boolean continueForest() {
        return curRootTreeNodeList.size() < parameter.getMaxTreeNum() || curTreeNode != null;
    }

    /**
     * 判断是否需要进行纵向的分裂处理
     *
     * @return 是否进行纵向处理
     */
    private boolean needVerticalControl() {
        if (curTreeNode.getSplitFeatureType() == 0) {
            return false;
        }
        if (curTreeNode.getSplitFeatureType() == 1) {
            return true;
        }
        // 小于 0，表示未决定横向或纵向分裂
        // 当前节点上的共同 id 数量
        Set<Integer> commonId = new HashSet<>(curTreeNode.getInstanceIdSpaceSet());
        commonId.retainAll(commonIdSet);
        if (curTreeNode.getInstanceIdSpaceSet().size() * parameter.getNeedVerticalSplitRatio() <= commonId.size()) {
            curTreeNode.setSplitFeatureType(1);
            return true;
        }
        curTreeNode.setSplitFeatureType(0);
        return false;
    }

    /**
     * 计算叶子结点的权重
     *
     * @param trainId 任务ID
     * @return 将要发送给客户端的消息队列
     */
    private List<CommonRequest> controlActuallyDealWithWj(String trainId) {
        ++globalRecordId;
        curTreeNode.setAsLeaf(calculateWj());
        curTreeNode.setSplitFeatureName("");
        curTreeNode.setSplitThreshold(0);
        curTreeNode.setRecordId(globalRecordId);
        BoostBodyReq request = new BoostBodyReq(MessageType.Wj);
        request.setWj(curTreeNode.getLeafScore());
        request.setRecordId(globalRecordId);
        // 回溯节点, 找到下一个待处理的右节点
        curTreeNode = curTreeNode.backTrackingTreeNode();
        // 更新该节点的 Gain
        if (curTreeNode != null) {
            computeNodeGH(curTreeNode);
        }
        return clientInfoList.parallelStream()
                .map(client -> new CommonRequest(client, request)).collect(Collectors.toList());
    }

    private List<CommonRequest> afterLeaf(String trainId, List<CommonResponse> responses) {
        // 当前树构建未完成, 进入下一个节点的构建
        if (curTreeNode != null) {
            return startNode(trainId, responses);
        }
        // 当前树构建完成，进入下一棵树的构建
        if (continueForest()) {
            return controlTreeInitInfo(trainId, responses);
        }
        return controlFinalModel(trainId);
    }

    /**
     * 训练中验证中，流程控制结束
     *
     * @param trainId   任务ID
     * @param responses 接收客户端的消息
     * @return 将要发送给客户端的消息队列
     */
    private List<CommonRequest> resultControl(String trainId, List<CommonResponse> responses) {
        isStop = true;
        EnumMap<MetricType, DoubleTuple2> gatherMetric = new EnumMap<>(MetricType.class);
        for (CommonResponse response : responses) {
            BoostBodyRes res = (BoostBodyRes) (response.getBody());
            for (Map.Entry<MetricType, DoubleTuple2> item : res.getTrainMetric().entrySet()) {
                if (gatherMetric.containsKey(item.getKey())) {
                    gatherMetric.get(item.getKey()).add(item.getValue());
                    continue;
                }
                gatherMetric.put(item.getKey(), item.getValue());
            }
        }
        computeMetric(gatherMetric, finalRootNodeList.size());
        logger.info("Successfully make MixGBoost!!!");
        BoostBodyReq request = new BoostBodyReq(MessageType.EpochFinish);
        return clientInfoList.parallelStream()
                .map(clientInfo -> new CommonRequest(clientInfo, request))
                .collect(Collectors.toList());
    }

    /**
     * 初始化服务端的信息，只进行一次性的初始化操作
     *
     * @param clientInfo 客户端信息
     * @param features   客户端的特征
     */
    private void initServerInfoOnce(ClientInfo clientInfo, Features features) {
        // features
        // tmp: deal with fixed feature 'uid'; Once Master code modified, remove this part.
        String labelName = features.getLabel();
        if (labelName == null) {
            labelName = "";
        }
        for (SingleFeature singleFeature : features.getFeatureList()) {
            String fname = singleFeature.getName();
            if (features.getIndex().equals(fname) || labelName.equals(fname)) {
                continue;
            }
            if (allFeatures.containsKey(fname)) {
                allFeatures.get(fname).add(clientInfo);
            } else {
                Set<ClientInfo> feaClients = new HashSet<>();
                feaClients.add(clientInfo);
                allFeatures.put(fname, feaClients);
            }
        }
    }

    /**
     * 开始构建每一颗树时的初始化操作
     */
    private void initTreeServerInfo() {
        curTreeNode = new MixTreeNode(1);
        curRootTreeNodeList.add(curTreeNode);
        dupIdGHMap = new HashMap<>();
        logger.info("start the {} epoch, {} trees!!!!!!!!!!!", curEpochNum, curRootTreeNodeList.size());
    }

    /**
     * 开始构建每一轮时的初始化操作
     */
    private void initEpochServerInfo() {
        // 每一个epoch信息初始化
        curRootTreeNodeList.clear();
        curEpochNum++;
        logger.info("start the {} epoch!!!!!!!!!!!", curEpochNum);
        // 每一个 epoch 的阶段 metric 会重置
        metricMap = new EnumMap<>(MetricType.class);
        for (MetricType metricType : parameter.getEvalMetric()) { //遍历预测指标
            List<Pair<Integer, Double>> tmpRoundMetric = new ArrayList<>();
            tmpRoundMetric.add(new Pair<>(curRootTreeNodeList.size(), -Double.MAX_VALUE));
            tmpRoundMetric.add(new Pair<>(curRootTreeNodeList.size(), -Double.MAX_VALUE));
            metricMap.put(metricType, tmpRoundMetric);
        }
        initTreeServerInfo();
    }

    /**
     * 进行每一颗树的初始化操作
     *
     * @param trainId   任务ID
     * @param responses 接收的客户端消息
     * @return 将要发送给客户端的消息队列
     */
    private List<CommonRequest> controlTreeInitInfo(String trainId, List<CommonResponse> responses) {
        initTreeServerInfo();
        // 向客户端申请gi和hi信息
        BoostBodyReq request = new BoostBodyReq(MessageType.TreeInit);
        // TODO: problems may occur since request is directly used, instead of request.toJson()
        return clientInfoList.parallelStream().map(clientInfo -> new CommonRequest(clientInfo, request)).collect(Collectors.toList());
    }

    /**
     * 进行每一轮的初始化操作
     */
    private List<CommonRequest> controlEpochInitInfo(String trainId, List<CommonResponse> responses) {
        initEpochServerInfo();
        BoostBodyReq request = new BoostBodyReq(MessageType.EpochInit);
        return responses.parallelStream().map(response -> new CommonRequest(response.getClient(), request)).collect(Collectors.toList());
    }

    /**
     * Id match
     */
    private List<CommonRequest> controlIdMatch(String trainId, List<CommonResponse> responses) {
        // 公共id
        int[] commonId = responses.stream()
                .map(response -> ((BoostBodyRes) (response.getBody())).getInstId())
                .filter(Objects::nonNull).findAny().orElse(new int[0]);
        commonIdSet = Arrays.stream(commonId).boxed().collect(Collectors.toSet());
        return controlEpochInitInfo(trainId, responses);
    }

    /**
     * 处理客户端发送过来的gi和hi信息
     *
     * @param instIdList 客户端发送的实例ID列表
     * @param ghStrList  客户端发送的gi hi列表
     */
    private void dealWithGiHiInfo(int[] instIdList, StringTuple2[] ghStrList) {
        DoubleTuple2[] ghList = Arrays.stream(ghStrList).parallel()
                .map(gh -> new DoubleTuple2(
                        encryptionTool.decrypt(encryptionTool.restoreCiphertext(gh.getFirst()), privateKey),
                        encryptionTool.decrypt(encryptionTool.restoreCiphertext(gh.getSecond()), privateKey)))
                .toArray(DoubleTuple2[]::new);
        IntStream.range(0, instIdList.length).forEach(i -> {
            int id = instIdList[i];
            if (dupIdGHMap.containsKey(id)) {
                IntDoubleTuple3 old = dupIdGHMap.get(id);
                dupIdGHMap.get(id).setValues(old.getFirst() + 1, old.getSecond() + ghList[i].getFirst(), old.getThird() + ghList[i].getSecond());
            } else {
                dupIdGHMap.put(id, new IntDoubleTuple3(1, ghList[i].getFirst(), ghList[i].getSecond()));
            }
        });
    }

    /**
     * 处理客户端发送过来的 id 信息
     *
     * @param clientInfo 客户端
     * @param instIdList 客户端发送的实例ID列表
     */
    private void dealWithNoCommonId(ClientInfo clientInfo, int[] instIdList) {
        // has been recorded
        if (clientNoCommonIdSetMap.containsKey(clientInfo)) {
            return;
        }
        if (instIdList == null) {
            clientNoCommonIdSetMap.put(clientInfo, new HashSet<>());
            return;
        }
        Set<Integer> noCommentId = Arrays.stream(instIdList).boxed().collect(Collectors.toSet());
        noCommentId.removeAll(commonIdSet);
        clientNoCommonIdSetMap.put(clientInfo, noCommentId);
    }

    /**
     * 处理客户端发送过来的 metric 信息
     *
     * @param trainMetric  客户trainMetric
     * @param gatherMetric gthered metric
     */
    private EnumMap<MetricType, DoubleTuple2> dealWithMetric(Map<MetricType, DoubleTuple2> trainMetric, EnumMap<MetricType, DoubleTuple2> gatherMetric) {
        if (trainMetric.isEmpty()) {
            return gatherMetric;
        }
        for (Map.Entry<MetricType, DoubleTuple2> item : trainMetric.entrySet()) {
            if (gatherMetric.containsKey(item.getKey())) {
                gatherMetric.get(item.getKey()).add(item.getValue());
                continue;
            }
            gatherMetric.put(item.getKey(), item.getValue());
        }
        return gatherMetric;
    }

    /**
     * 处理客户端发送过来的 metric 信息
     *
     * @param gatherMetric gathered metric
     */
    private void computeMetric(Map<MetricType, DoubleTuple2> gatherMetric, int treeSize) {
        double metric0;
        for (Map.Entry<MetricType, DoubleTuple2> entry : gatherMetric.entrySet()) {
            metric0 = Metric.calculateGlobalMetric(entry.getKey(), entry.getValue().getFirst(), dupIdGHMap.size());
            metricMap.get(entry.getKey()).add(new Pair<>(treeSize, metric0));
        }
        printMetric(treeSize);
    }

    private void printMetric(int treeSize) {
        StringBuilder metricOutput = new StringBuilder(String.format("MixGBoost round %d,%n", treeSize));
        for (Map.Entry<MetricType, List<Pair<Integer, Double>>> entry : metricMap.entrySet()) {
            int size = entry.getValue().size();
            metricOutput.append(String.format("all IDs from the clients: %n"));
            metricOutput.append(String.format("train-%s:%.15f",
                    entry.getKey(), entry.getValue().get(size - 1).getValue()));
        }
        logger.info("{}", metricOutput);
    }

    /**
     * 接收客户端发送的 gi 和 hi 信息，进行统计和汇总，将处理后的信息发送给客户端
     *
     * @param trainId   任务ID
     * @param responses 接收的客户端信息
     * @return 将要发送给客户端的消息队列
     */
    private List<CommonRequest> controlUpdateGiHi(String trainId, List<CommonResponse> responses) {
        BoostBodyReq request = new BoostBodyReq(MessageType.UpdateGiHi);
        dupIdGHMap.clear();
        // 接收客户端发送的gi和hi信息（如果有的话），发送随机的得到的特征集合）
        boolean noDealWithGiHi = true;
        int idSize = 0;
        EnumMap<MetricType, DoubleTuple2> gatherMetric = new EnumMap<>(MetricType.class);
        for (CommonResponse response : responses) {
            BoostBodyRes res = (BoostBodyRes) (response.getBody());
            int[] instId = res.getInstId();
            if (instId != null) {
                if (instId.length > idSize) {
                    idSize = instId.length;
                    clientSave = response.getClient();
                }
//                dealWithGiHi 标记统计样本出现次数及累计 gi hi 值
                noDealWithGiHi = false;
                dealWithGiHiInfo(instId, res.getGh());
            }
            if (curRootTreeNodeList.size() > 1) {
                dealWithMetric(res.getTrainMetric(), gatherMetric);
            }
            dealWithNoCommonId(response.getClient(), instId);
        }
        if (noDealWithGiHi) {
            logger.error("no GH values received from clients.");
        } else {
            dupIdGHMap.entrySet().parallelStream().forEach(e -> {
                double idNum = e.getValue().getFirst();
                double secondG = e.getValue().getSecond() / idNum;
                double thirdH = e.getValue().getThird() / idNum;
                e.getValue().setDoubles(secondG, thirdH);
            });
            // update curNode GH values
            curTreeNode.setInstanceIdSpaceSet(new HashSet<>(dupIdGHMap.keySet()));
            computeNodeGH(curTreeNode);
            computeMetric(gatherMetric, curRootTreeNodeList.size() - 1);

            int[] allIdList = dupIdGHMap.keySet().stream().mapToInt(Integer::intValue).toArray();
            request.setInstId(allIdList);
            double[] cntList = dupIdGHMap.values().parallelStream().mapToDouble(IntDoubleTuple3::getFirst).toArray();
            request.setCntList(cntList);
            StringTuple2[] encGHList = dupIdGHMap.values().parallelStream().map(gh ->
                    new StringTuple2(encryptionTool.encrypt(gh.getSecond(), pubKey).serialize()
                            , encryptionTool.encrypt(gh.getThird(), pubKey).serialize())).toArray(StringTuple2[]::new);
            request.setGh(encGHList);
        }
        return clientInfoList.parallelStream().map(clientInfo -> new CommonRequest(clientInfo, request)).collect(Collectors.toList());
    }

    /**
     * @param trainId   任务ID
     * @param responses 客户端返回的消息
     * @return 将要发送给客户端的消息队列
     */
    private List<CommonRequest> startNode(String trainId, List<CommonResponse> responses) {
        if (curTreeNode == null) {
            logger.error("curNode is null, restart training process...");
            return controlEpochInitInfo(trainId, responses);
        }
        if (stopSplit()) {
//            计算叶子节点信息，不进行分裂，告诉客户端已经完成节点处理操作
            return controlActuallyDealWithWj(trainId);
        }
        // 判断是否进行横向处理
        if (needVerticalControl()) {
            // 当前节点上的训练样本，大部分都是共同 id，该节点使用纵向分裂
            return controlHorizontalNodeFinish(trainId, responses);
        }
        // 发送特征集合
        // Gi和Hi之后，进行特征集合获取，或者进行叶子节点处理
        // 如果节点需要分裂，特征集合获取；否则，进行叶子节点处理
        return controlHorizontalFeatureSet(trainId, responses);
    }

    /**
     * 随机产生进行横向分裂的特征集合
     *
     * @return 所产生的的特征集合
     */
    private String[] randomFeaturesSet() {
        List<String> tempFeaturesList = new ArrayList<>(allFeatures.keySet());
        List<String> res = new ArrayList<>();
        //随机取出n条不重复的数据
        while (res.size() < randomFeatureSetSize) {
            //在数组大小之间产生一个随机数 j
            int j = randomIndex(tempFeaturesList.size() + 1);
            //取得list 中下标为j 的数据存储到 listRandom 中
            res.add(tempFeaturesList.get(j));
            //把已取到的数据移除,避免下次再次取到出现重复
            tempFeaturesList.remove(j);
        }
        return res.toArray(new String[0]);
    }

    /**
     * 横向训练的处理控制：产生特征集合, 向客户端请求对应特征的阈值。
     *
     * @param trainId   任务ID
     * @param responses 客户端返回的消息
     * @return 将要发送给客户端的消息队列
     */
    private List<CommonRequest> controlHorizontalFeatureSet(String trainId, List<CommonResponse> responses) {
        // 接收客户端发送的 gi 和 hi 信息（如果有的话），发送随机的得到的特征集合）
        // 随机产生特征集合，发送给客户端
        String[] horizontalFeaturesSet = randomFeaturesSet();
        BoostBodyReq request = new BoostBodyReq(MessageType.FeaturesSet);
        request.setFeaturesSet(horizontalFeaturesSet);
        return clientInfoList.parallelStream().map(res -> new CommonRequest(res, request)).collect(Collectors.toList());
    }

    /**
     * 从特征阈值列表中，随机获取一个阈值下标
     *
     * @param size 特征阈值列表长度
     * @return 随机得到的阈值
     */
    private int randomIndex(int size) {
        //在数组大小之间产生一个随机数 j
        return random.nextInt(size - 1);
    }

    /**
     * 接收各个客户端发送的特征阈值，向客户端发送随机选中的特征阈值，并请求对应特征的IL
     *
     * @param trainId   任务ID
     * @param responses 接收的客户端消息
     * @return 将要发送到客户端的消息列表
     */
    private List<CommonRequest> controlHorizontalIL(String trainId, List<CommonResponse> responses) {
        //取出特征的所有随机阈值
        Map<String, List<Double>> featureValuesMap = responses.parallelStream().map(response -> (BoostBodyRes) (response.getBody()))
                .filter(boostBodyRes -> boostBodyRes.getFvMap() != null)
                .flatMap(boostBodyRes -> boostBodyRes.getFvMap().entrySet().stream())
                .collect(Collectors.groupingBy(Map.Entry::getKey, HashMap::new, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
        // compute a random value for each feature
        featureThresholds = featureValuesMap.entrySet().parallelStream().collect(Collectors.toMap(Map.Entry::getKey,
                featureValues -> {
                    List<Double> values = featureValues.getValue();
                    double randomThreshold = values.get(0);
                    if (featureValues.getValue().size() > 1) {
                        randomThreshold = values.get(randomIndex(values.size()));
                    }
                    return randomThreshold;
                }));
        // send back to clients to split the node
        BoostBodyReq request = new BoostBodyReq(MessageType.H_IL);
        request.setfVMap(featureThresholds);
        return responses.parallelStream()
                .map(response -> new CommonRequest(response.getClient(), request)).collect(Collectors.toList());
    }

    /**
     * 计算横向分裂方式的gain信息
     *
     * @param iLSet            左节点的实例ID集合
     * @param featureName      特征名称
     * @param featureThreshold 特征阈值
     */
    private Tuple3<String, Double, Double> calculateHorizontalGain(MixTreeNode node, Set<Integer> iLSet, String featureName, double featureThreshold) {
        DoubleTuple2 sumGHLeft = iLSet.parallelStream().filter(id -> dupIdGHMap.containsKey(id)).map(id -> dupIdGHMap.get(id))
                .collect(Collectors.collectingAndThen(Collectors.toList(),
                        ghList -> new DoubleTuple2(ghList.parallelStream().mapToDouble(IntDoubleTuple3::getSecond).sum(),
                                ghList.parallelStream().mapToDouble(IntDoubleTuple3::getThird).sum())));
        // 剔除共同ID的数据
        DoubleTuple2 sumGHnode = node.getNodeGH();
        double sumGR = sumGHnode.getFirst() - sumGHLeft.getFirst();
        double sumHR = sumGHnode.getSecond() - sumGHLeft.getSecond();
        double gain = 0.5 * ((sumGHLeft.getFirst() * sumGHLeft.getSecond()) / (sumGHLeft.getSecond() + parameter.getLambda()) +
                (sumGR * sumGR) / (sumHR + parameter.getLambda()) - sumGHnode.getFirst() * sumGHnode.getSecond() / (sumGHnode.getSecond() + parameter.getLambda()));

        return new Tuple3<>(featureName, featureThreshold, gain);
    }

    /**
     * 接收客户端返回的IL信息，计算出当前特征的最佳分裂信息
     *
     * @param trainId   任务ID
     * @param responses 接收的客户端消息
     */
    private List<CommonRequest> horizontalFeatureSplit(String trainId, List<CommonResponse> responses) {
        // group all Horizontal IL by feaName
        Map<String, List<Integer[]>> featureILMap = responses.parallelStream()
                .flatMap(response -> ((BoostBodyRes) (response.getBody())).getFeaturesIL().entrySet().stream())
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.<Integer[]>toList())));

        // merge ID lists
        Map<String, Set<Integer>> featureILSet = featureILMap.entrySet().parallelStream().collect(Collectors.toMap(Map.Entry::getKey, x -> x.getValue().stream().flatMap(Arrays::stream).collect(Collectors.toSet())));
        // compute gains
        Tuple3<String, Double, Double> bestGain = featureILSet.entrySet().parallelStream()
                .map(entry -> calculateHorizontalGain(curTreeNode, entry.getValue(), entry.getKey(), featureThresholds.get(entry.getKey())))
                .max(Comparator.comparingDouble(stringDoubleDoubleTuple3 -> stringDoubleDoubleTuple3._3().orElse(-Double.MAX_VALUE)))
                .orElse(new Tuple3<>("", 0.0, -Double.MAX_VALUE));
        featureThresholds = null;

        double maxGain = bestGain._3().orElse(-Double.MAX_VALUE);
        if (maxGain > curTreeNode.getTempHorizontalGain() && maxGain > parameter.getGamma()) {
            curTreeNode.setTempHorizontalGain(maxGain);
            curTreeNode.setSplitFeatureName(bestGain._1().orElse(""));
            curTreeNode.setSplitThreshold(bestGain._2().orElse(0.0));
            tempHorizontalIL = featureILSet.get(bestGain._1().orElse(""));
        }
        return controlHorizontalNodeFinish(trainId, responses);
    }

    /**
     * 进行横向方式的节点分裂处理
     */
    private void splitHorizontalTreeNode() {
        // 节点分裂，并进入左子树节点
        curTreeNode.setSplitFeatureType(0);
        curTreeNode.setRecordId(globalRecordId);
        MixTreeNode leftNode = new MixTreeNode(curTreeNode.getDepth() + 1);
        leftNode.setParent(curTreeNode);
        leftNode.setInstanceIdSpaceSet(new HashSet<>(tempHorizontalIL));
        computeNodeGH(leftNode);
        curTreeNode.setLeftChild(leftNode);
        curTreeNode = leftNode;
    }

    /**
     * 完成节点的横向处理，进行纵向XBG的处理控制
     *
     * @param trainId   任务ID
     * @param responses 接收客户端的消息
     * @return 将要发送给客户端的消息队列
     */
    private List<CommonRequest> controlHorizontalNodeFinish(String trainId, List<CommonResponse> responses) {
        // 根据当前处理的特征名称，告知客户端横向分裂结束
        computeVerticalCurrentNodeGH(curTreeNode);
        return controlVerticalGkvHkv(trainId, responses);
    }

    /**
     * 确定应该选择特征集合哪个特征进行分裂，向客户端发送左节点空间信息
     *
     * @param trainId   任务ID
     * @param responses 接收客户端的消息
     * @return 将要发送给客户端的消息队列
     */
    private List<CommonRequest> controlHorizontalSplit(String trainId, List<CommonResponse> responses) {
        if (curTreeNode.getTempHorizontalGain() < curTreeNode.getTempVerticalGain() || curTreeNode.getTempHorizontalGain() < parameter.getGamma()
                || curTreeNode.getInstanceIdSpaceSet().size() == tempHorizontalIL.size()) {
            return controlActuallyDealWithWj(trainId);
        }
        ++globalRecordId;
        String feaName = curTreeNode.getSplitFeatureName();
        allFeatures.get(feaName).forEach(client -> clientVerticalSize.replace(client, clientVerticalSize.get(client) + 1));

        int[] instId = tempHorizontalIL.stream().mapToInt(Integer::intValue).toArray();

        List<CommonRequest> res = clientInfoList.parallelStream().map(item -> {
            BoostBodyReq request = new BoostBodyReq(MessageType.HorizontalSplit);
            // IL集合需要根据每个客户端的实例ID进行特殊处理
            request.setInstId(instId);
            request.setRecordId(-1);
            if (allFeatures.get(feaName).contains(item)) {
                request.setRecordId(globalRecordId);
                request.setFeatureName(feaName);
                request.setFeatureThreshold(curTreeNode.getSplitThreshold());
            }
            return new CommonRequest(item, request);
        }).collect(Collectors.toList());
        // 节点分裂，并进入左子树节点
        splitHorizontalTreeNode();
        tempHorizontalIL = null;
        return res;
    }

    /**
     * 计算叶子节点的权重值
     */
    private double calculateWj() {
        double wj = -curTreeNode.getNodeGH().getFirst() / (curTreeNode.getNodeGH().getSecond() + parameter.getLambda());
        return wj * this.parameter.getEta();
    }

    /**
     * 服务端向所有客户端请求Gkv和Hkv的信息
     *
     * @param trainId   任务ID
     * @param responses 接收客户端的消息
     * @return 将要发送给客户端的消息队列
     */
    private List<CommonRequest> controlVerticalGkvHkv(String trainId, List<CommonResponse> responses) {
        BoostBodyReq request = new BoostBodyReq(MessageType.GkvHkv);
        return clientInfoList.parallelStream().map(client -> new CommonRequest(client, request)).collect(Collectors.toList());
    }

    /**
     * 更新纵向分裂的当前 G 和 H 信息
     */
    private void computeVerticalCurrentNodeGH(MixTreeNode node) {
        if (commonIdSet.containsAll(curTreeNode.getInstanceIdSpaceSet())) {
            return;
        }
        Set<Integer> commonId = new HashSet<>(curTreeNode.getInstanceIdSpaceSet());
        commonId.retainAll(commonIdSet);
        if (commonId.size() > node.getInstanceIdSpaceSet().size() / 2) {
            DoubleTuple2 nodeGH = node.getNodeGH();
            DoubleTuple2 otherGH = commonId.parallelStream().filter(id -> dupIdGHMap.containsKey(id)).map(id -> dupIdGHMap.get(id))
                    .collect(Collectors.collectingAndThen(Collectors.toList(),
                            ghList -> new DoubleTuple2(ghList.parallelStream().mapToDouble(IntDoubleTuple3::getSecond).sum(),
                                    ghList.parallelStream().mapToDouble(IntDoubleTuple3::getThird).sum())));
            node.setNodeGH(new DoubleTuple2(nodeGH.getFirst() - otherGH.getFirst(), nodeGH.getSecond() - otherGH.getSecond()));
            return;
        }
        DoubleTuple2 commonGH = commonId.parallelStream().filter(id -> dupIdGHMap.containsKey(id)).map(id -> dupIdGHMap.get(id))
                .collect(Collectors.collectingAndThen(Collectors.toList(),
                        ghList -> new DoubleTuple2(ghList.parallelStream().mapToDouble(IntDoubleTuple3::getSecond).sum(),
                                ghList.parallelStream().mapToDouble(IntDoubleTuple3::getThird).sum())));
        node.setNodeGH(commonGH);
    }

    /**
     * 计算分裂的gain
     *
     * @param nodeVerticalGH 节点的gi总和 hi总和
     * @param leftGH         左子树节点的 gi总和 hi总和
     * @return 计算得到的gain值
     */
    private double calculateSplitGain(DoubleTuple2 nodeVerticalGH, DoubleTuple2 leftGH) {
        double gr = nodeVerticalGH.getFirst() - leftGH.getFirst();
        double hr = nodeVerticalGH.getSecond() - leftGH.getSecond();
        return 0.5 * (leftGH.getFirst() * leftGH.getFirst() / (leftGH.getSecond() + parameter.getLambda())
                + gr * gr / (hr + parameter.getLambda())
                - nodeVerticalGH.getFirst() * nodeVerticalGH.getFirst() / (nodeVerticalGH.getSecond() + parameter.getLambda()));
    }

    /**
     * 服务端根据各个客户端的(k, v, gain)，计算得到纵向最优分割点。
     * 进行横向和纵向的混合：判断是否需要进行节点分裂，如果不需要，计算叶子节点权重；
     * 需要分裂，选择横向或者纵向分裂方式。
     *
     * @param trainId   任务ID
     * @param responses 接收客户端的消息
     * @return 将要发送给客户端的消息队列
     */
    private List<CommonRequest> controlVerticalKVGain(String trainId, List<CommonResponse> responses) {
        // 仅仅对共同 ID 的那部分数据进行处理，客户端已经处理过
        DoubleTuple2 nodeVerticalGH = curTreeNode.getNodeGH();

        GainOutput gainOutput = responses.parallelStream().flatMap(response -> {
            StringTuple2[][] boostGkvHkv = ((BoostBodyRes) (response.getBody())).getFeatureGlHl();
            if (boostGkvHkv == null) {
                return null;
            }
            return IntStream.range(0, boostGkvHkv.length).parallel().filter(i -> boostGkvHkv[i] != null)
                    .mapToObj(i -> new FeatureLeftGH(response.getClient(), i + "", boostGkvHkv[i]));
        }).filter(Objects::nonNull)
                .map(x -> fetchGain(x, nodeVerticalGH))
                .max(Comparator.comparing(GainOutput::getGain)).orElse(null);
        if (gainOutput == null || gainOutput.getClient() == null || gainOutput.getGain() < curTreeNode.getTempHorizontalGain()) {
            curTreeNode.setTempVerticalGain(-Double.MAX_VALUE);
            return controlHorizontalSplit(trainId, responses);
        }
        if (gainOutput.getGain() <= parameter.getGamma()) {
            return controlActuallyDealWithWj(trainId);
        }
        return verticalSplitFetchIL(trainId, gainOutput);
    }

    private GainOutput fetchGain(FeatureLeftGH input, DoubleTuple2 nodeVerticalGH) {
        StringTuple2[] tmpGH = input.getGhLeft();
        Tuple2<Double, Integer> fakeGain = new Tuple2<>(0.0, -1);
        DoubleTuple2[] decryptedGH = Arrays.asList(tmpGH).parallelStream().map(x ->
                new DoubleTuple2(encryptionTool.decrypt(encryptionTool.restoreCiphertext(x.getFirst()), privateKey),
                        encryptionTool.decrypt(encryptionTool.restoreCiphertext(x.getSecond()), privateKey)))
                .toArray(DoubleTuple2[]::new);
        Tuple2<Double, Integer> maxGain = computeGain(decryptedGH, nodeVerticalGH).parallelStream().max(Comparator.comparing(Tuple2::_1)).orElse(fakeGain);
        return new GainOutput(input.getClient(), input.getFeature(), maxGain._2(), maxGain._1());
    }

    /**
     * computeGains for a feature
     *
     * @param ghValues gh values
     * @param ghNode   that node
     * @return list of (gain, split index) pairs for that feature
     */
    private List<Tuple2<Double, Integer>> computeGain(DoubleTuple2[] ghValues, DoubleTuple2 ghNode) {
        List<Tuple2<Double, Integer>> allGain = new ArrayList<>();
        DoubleTuple2 ghLeft = new DoubleTuple2(0, 0);
        for (int i = 0; i < ghValues.length - 1; i++) {
            ghLeft.add(ghValues[i]);
            double curGain = calculateSplitGain(ghNode, ghLeft);
            // i is split index
            allGain.add(new Tuple2<>(curGain, i));
        }
        return allGain;
    }

    /**
     * 进行纵向节点分裂的处理
     *
     * @param tempVerticalIL 左子树节点的实例空间
     */
    private void splitVerticalTreeNode(Set<Integer> tempVerticalIL) {
        // 节点分裂，并进入左子树节点
        curTreeNode.setSplitFeatureName(curTreeNode.getVerticalSplitFeatureName());
        curTreeNode.setSplitThreshold(curTreeNode.getVerticalSplitThreshold());
        MixTreeNode leftNode = new MixTreeNode(curTreeNode.getDepth() + 1);
        leftNode.setParent(curTreeNode);
        leftNode.setInstanceIdSpaceSet(tempVerticalIL);
        // pre-compute GH for left node
        computeNodeGH(leftNode);
        curTreeNode.setLeftChild(leftNode);
        curTreeNode = leftNode;
    }

    /**
     * 确定应该选择特征集合哪个特征进行分裂，向客户端发送左节点空间信息
     *
     * @param trainId   任务ID
     * @param responses 接收客户端的消息
     * @return 将要发送给客户端的消息队列
     */
    private List<CommonRequest> controlVerticalSplit(String trainId, List<CommonResponse> responses) {
        ClientInfo clientInfo = curTreeNode.getClient();
        BoostBodyRes res = (BoostBodyRes) (responses.get(0).getBody());
        Map<String, Double> feaValue = res.getFvMap();
        // 判断是否需要分裂节点
        Iterator<Map.Entry<String, Double>> iterator = feaValue.entrySet().iterator();
        if (iterator.hasNext()) {
            Map.Entry<String, Double> entry = iterator.next();
            curTreeNode.setVerticalSplitFeatureName(entry.getKey());
            curTreeNode.setVerticalSplitThreshold(entry.getValue());
        }

        // only common ids (has been processed by client)
        Set<Integer> tempVerticalIL = Arrays.stream(res.getInstId()).boxed().collect(Collectors.toSet());
        // 节点分裂，并进入左子树节点
        splitVerticalTreeNode(tempVerticalIL);
        List<ClientInfo> resClients = clientInfoList.parallelStream().filter(x -> x != clientInfo).collect(Collectors.toList());
        if (resClients.isEmpty()) {
            return startNode(trainId, responses);
        }
        BoostBodyReq request = new BoostBodyReq(MessageType.VerticalSplit);
        request.setRecordId(-1);
        request.setInstId(res.getInstId());
        return resClients.stream().map(item -> new CommonRequest(item, request)).collect(Collectors.toList());
    }

    /**
     * 确定应该选择特征集合哪个特征进行分裂，向客户端发送左节点空间信息
     *
     * @param trainId    任务ID
     * @param gainOutput feature vertical gain info
     * @return 将要发送给客户端的消息队列
     */
    private List<CommonRequest> verticalSplitFetchIL(String trainId, GainOutput gainOutput) {
        ++globalRecordId;
        curTreeNode.setSplitFeatureType(1);
        curTreeNode.setClient(gainOutput.getClient());
        curTreeNode.setTempVerticalGain(gainOutput.getGain());
        curTreeNode.setRecordId(globalRecordId);
        clientVerticalSize.replace(gainOutput.getClient(), clientVerticalSize.get(gainOutput.getClient()) + 1);

        BoostBodyReq req = new BoostBodyReq(MessageType.KVGain);
        req.setRecordId(globalRecordId);
        req.setK(Integer.parseInt(gainOutput.getFeature()));
        req.setV(gainOutput.getSplitIndex());
        req.setGain(gainOutput.getGain());
        CommonRequest commonRequest = new CommonRequest(gainOutput.getClient(), req);
        List<CommonRequest> res = new ArrayList<>();
        res.add(commonRequest);
        return res;
    }

    private List<Tuple2<MixTreeNode, ClientInfo>> traverseBeforeSerialize(List<MixTreeNode> roots) {
        List<Tuple2<MixTreeNode, ClientInfo>> scatterNodes = new ArrayList<>();
        roots.forEach(root -> traverseTree(null, root, scatterNodes));
        return scatterNodes;
    }

    private boolean traverseTree(ClientInfo parent, MixTreeNode node, List<Tuple2<MixTreeNode, ClientInfo>> res) {
        if (node.isLeaf()) {
            Tuple2<MixTreeNode, ClientInfo> nodeInfo = new Tuple2<>(node, parent);
            res.add(nodeInfo);
            return true;
        }
        ClientInfo myClient = node.getClient();
        // vertical split, and at different clients
        if (node.getSplitFeatureType() == 1 && parent != null && myClient != parent) {
            return false;
        }
        if (node.getSplitFeatureType() == 0) {
            myClient = parent;
        }
        boolean left = traverseTree(myClient, node.getLeftChild(), res);
        boolean right = traverseTree(myClient, node.getRightChild(), res);
        return left || right;
    }

    /**
     * 所有epoch的模型都训练完毕后的处理操作
     *
     * @param trainId 任务ID
     * @return 将要发送给客户端的消息队列
     */
    private List<CommonRequest> controlFinalModel(String trainId) {
        // 清理信息
        finalRootNodeList.clear();
        finalRootNodeList.addAll(curRootTreeNodeList);
        curRootTreeNodeList.clear();
        curTreeNode = null;
        // traverse trees, find paths that depend on one single client
        List<Tuple2<MixTreeNode, ClientInfo>> scatterNodes = traverseBeforeSerialize(finalRootNodeList);
        assert clientSave != null;
        // delete list
        // first, single-client-owned nodes
        Map<ClientInfo, List<Integer>> deleteFromClients = scatterNodes.parallelStream().filter(tuple2 -> tuple2._2() != null)
                .map(tuple2 -> new Tuple2<>(tuple2._2(), tuple2._1().getRecordId()))
                .collect(Collectors.groupingBy(Tuple2::_1, HashMap::new, Collectors.mapping(Tuple2::_2, Collectors.toList())));
        clientInfoList.forEach(client -> {
            if (!deleteFromClients.containsKey(client)) {
                deleteFromClients.put(client, new ArrayList<>());
            }
        });
        // second, common nodes
        List<Integer> commonLeaf = scatterNodes.parallelStream().filter(tuple2 -> tuple2._2() == null)
                .map(tuple2 -> tuple2._1().getRecordId())
                .collect(Collectors.toList());
        deleteFromClients.entrySet().stream().filter(item -> item.getKey() != clientSave)
                .forEach(item -> item.getValue().addAll(commonLeaf));
        // scatter list
        Map<ClientInfo, List<Integer>> scatterOnClients = scatterNodes.parallelStream()
                .map(tuple2 -> new Tuple2<>(randomSaveClient(tuple2._2(), clientSave, clientInfoList), tuple2._1().getRecordId()))
                .collect(Collectors.groupingBy(Tuple2::_1, HashMap::new, Collectors.mapping(Tuple2::_2, Collectors.toList())));
        return clientInfoList.parallelStream()
                .map(clientInfo -> {
                    BoostBodyReq request = new BoostBodyReq(MessageType.EpochFinish);
                    if (deleteFromClients.containsKey(clientInfo)) {
                        request.setDeleteNodes(deleteFromClients.get(clientInfo).stream().mapToInt(Integer::intValue).toArray());
                    }
                    if (scatterOnClients.containsKey(clientInfo)) {
                        request.setSaveNodes(scatterOnClients.get(clientInfo).stream().mapToInt(Integer::intValue).toArray());
                    }
                    return new CommonRequest(clientInfo, request);
                }).collect(Collectors.toList());
    }

    private ClientInfo randomSaveClient(ClientInfo origin, ClientInfo defaultClient, List<ClientInfo> clientList) {
        if (origin == null) {
            return defaultClient;
        }
        if (clientList.size() == 1) {
            return clientList.get(0);
        }
        ClientInfo res;
        do {
            // 在数组大小之间产生一个随机数 j
            int j = randomIndex(clientList.size() + 1);
            // 取得list 中下标为 j 的数据
            res = clientList.get(j);
        } while (res == origin);
        return res;
    }

    /**
     * 进行推理的整体流程控制
     *
     * @param responses 接收客户端的消息
     * @return 将要发送给客户端的消息队列
     */
    @Override
    public List<CommonRequest> inferenceControl(List<CommonResponse> responses) {
        return inferRootNodeList == null ? inferenceTreesInit(responses) : inferenceQueryprocess(responses);
    }

    @Override
    public PredictRes postInferenceControl(List<CommonResponse> responses) {
        double[] pred = inferUid.parallelStream().map(id -> {
            if (inferScoreMap.containsKey(id)) {
                return inferScoreMap.get(id);
            }
            return Double.NaN;
        }).mapToDouble(Double::doubleValue).toArray();
        inferenceFinish();
        return new PredictRes(new String[]{"label"}, loss.transform(pred));
    }

    @Override
    public List<CommonRequest> initInference(List<ClientInfo> clientInfos, String[] predictUid) {
        InferenceInit initString = new InferenceInit(predictUid);
        inferUid = Arrays.asList(predictUid.clone());
        inferScoreMap = new ConcurrentHashMap<>();
        clientNotExistId = clientInfos.stream().collect(Collectors.toMap(clientInfo -> clientInfo, clientInfo -> new ArrayList<>()));
        recordIdNodeMap = null;
        isStopInference = false;
        inferRootNodeList = null;
        return clientInfos.parallelStream().map(clientInfo -> CommonRequest.buildInferenceInitial(clientInfo, initString)).collect(Collectors.toList());
    }

    private List<String> getNoExistId(List<String> noExistIds, String[] instanceId) {
        List<String> toProcessId = Arrays.stream(instanceId).collect(Collectors.toList());
        toProcessId.retainAll(noExistIds);
        return toProcessId;
    }

    private String[] getExistId(String[] instanceId, List<String> noExistIds, Map<String, Double> inferScoreMap) {
        List<String> toProcessId = Arrays.stream(instanceId).filter(inferScoreMap::containsKey).collect(Collectors.toList());
        toProcessId.removeAll(noExistIds);
        return toProcessId.toArray(new String[0]);
    }

    /**
     * 推理过程中每棵树开始的初始化设置
     *
     * @return 将要发送给客户端的消息队列
     */
    private List<CommonRequest> inferenceTreesInit(List<CommonResponse> responses) {
        recordIdNodeMap = new HashMap<>();
        for (CommonResponse response : responses) {
            BoostInferenceInitRes res = (BoostInferenceInitRes) (response.getBody());
            String content = res.getContent();
            int[] getUid = res.getUid();
            // filter out IDs that do not exist on any client
            if (getUid != null) {
                List<String> deleteId = Arrays.stream(getUid).boxed().map(index -> inferUid.get(index)).collect(Collectors.toList());
                clientNotExistId.put(response.getClient(), deleteId);
            }
            if (content == null || content.isEmpty()) {
                continue;
            }
            deserializeNodes2(content, response.getClient());
        }
        deserializeTrees2();
        if (inferRootNodeList.isEmpty()) {
            logger.error("There is no training results, A.K.A., finalRootNodeList is empty.");
            isStopInference = true;
            return new ArrayList<>();
        }
        double rootLeafSum = inferRootNodeList.parallelStream().filter(MixTreeNode::isLeaf).mapToDouble(MixTreeNode::getLeafScore).sum();
        List<String> delete = inferRootNodeList.parallelStream().filter(node -> !node.isLeaf())
                .flatMap(node -> clientNotExistId.get(node.getClient()).stream()).collect(Collectors.toList());
        inferUid.stream().filter(id -> !delete.contains(id)).forEach(id -> inferScoreMap.put(id, rootLeafSum));
        String[] toInferIds = inferScoreMap.keySet().toArray(new String[0]);

        List<CommonRequest> requests = inferRootNodeList.parallelStream().filter(node -> !node.isLeaf())
                .map(node -> {
                    BoostInferQueryReqBody reqBody = new BoostInferQueryReqBody(toInferIds, node.getRecordId());
                    return new CommonRequest(node.getClient(), reqBody, -1);
                }).collect(Collectors.toList());
        // all roots are leaves
        if (requests.isEmpty()) {
            isStopInference = true;
        }
        return requests;
    }

    private List<CommonRequest> inferenceQueryprocess(List<CommonResponse> responses) {
        List<CommonRequest> requests = inferenceNodeUpdate(responses);
        if (requests.isEmpty()) {
            isStopInference = true;
        }
        return requests;
    }

    private List<CommonRequest> inferenceNodeUpdate(List<CommonResponse> responses) {
        List<BoostInferQueryResBody> resBodies = responses.parallelStream().flatMap(response ->
                Arrays.stream(((BoostInferQueryRes) (response.getBody())).getBodies()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        // a resBody for a node
        return resBodies.stream().map(resBody -> {
            // -1, arrives at leaf node
            if (resBody.getRecordId() == -1 || recordIdNodeMap.get(resBody.getRecordId()) == null) {
                Arrays.stream(resBody.getInstanceId()).forEach(id -> inferScoreMap.replace(id, inferScoreMap.get(id) + resBody.getValue()));
                return null;
            }
            MixTreeNode node = recordIdNodeMap.get(resBody.getRecordId());
            // proceed to tbe next nodes
            if (resBody.getValue() == 0) {
                // 0: goes to left
                node = node.getLeftChild();
            } else {
                // 1: goes to right
                node = node.getRightChild();
            }
            if (node.isLeaf()) {
                double leafV = node.getLeafScore();
                Arrays.stream(resBody.getInstanceId()).filter(inferScoreMap::containsKey).forEach(id -> inferScoreMap.replace(id, inferScoreMap.get(id) + leafV));
                return null;
            }
            List<String> noExistId = getNoExistId(clientNotExistId.get(node.getClient()), resBody.getInstanceId());
            noExistId.forEach(id -> inferScoreMap.remove(id));
            String[] nextIds = getExistId(resBody.getInstanceId(), noExistId, inferScoreMap);
            BoostInferQueryReqBody reqBody = new BoostInferQueryReqBody(nextIds, node.getRecordId());
            return new CommonRequest(node.getClient(), reqBody, -1);
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * 推理完成之后的处理
     */
    private void inferenceFinish() {
        inferScoreMap.clear();
        isStopInference = true;
    }

    @Override
    public boolean isContinue() {
        return !isStop;
    }

    @Override
    public boolean isInferenceContinue() {
        return !isStopInference;
    }

    @Override
    public MetricValue readMetrics() {
        return  new MetricValue(metricMap);
    }

    private MixTreeNode getRecordIdNodeMapNode(int depth, int recordId) {
        if (recordIdNodeMap.containsKey(recordId)) {
            return recordIdNodeMap.get(recordId);
        }
        MixTreeNode node = new MixTreeNode(depth, recordId);
        recordIdNodeMap.put(recordId, node);
        return node;
    }

    private void computeNodeGH(MixTreeNode node) {
        if (node.getInstanceIdSpaceSet().isEmpty()) {
            return;
        }
        DoubleTuple2 sumGH = node.getInstanceIdSpaceSet().parallelStream()
                .filter(id -> dupIdGHMap.containsKey(id))
                .map(id -> dupIdGHMap.get(id))
                .collect(Collectors.collectingAndThen(Collectors.toList(),
                        ghList -> new DoubleTuple2(ghList.parallelStream().mapToDouble(IntDoubleTuple3::getSecond).sum(),
                                ghList.parallelStream().mapToDouble(IntDoubleTuple3::getThird).sum())));
        node.setNodeGH(sumGH);
    }

    private void deserializeNodes2(String content, ClientInfo client) {
        String[] lines = content.split("\n");
        int treeStarts = 0;
        if (lines[0].startsWith("first_round_predict")) {
            treeStarts = 2;
            if (this.loss == null) {
                if ("logloss".equals(lines[1])) {
                    this.loss = new LogisticLoss();
                } else if ("squareloss".equals(lines[1])) {
                    this.loss = new SquareLoss();
                } else if (lines[1].startsWith("crossEntropy")) {
                    String[] elements = lines[1].split(",");
                    this.loss = new crossEntropy(Integer.parseInt(elements[1]));
                }
            }
        }
        // Notice: repeated leaf node / horizontal split node
        for (int i = treeStarts + 1; i < lines.length; i++) {
            if (lines[i].isEmpty()) {
                continue;
            }
            String[] elements = lines[i].split(",");
            // elements[0] recordId
            int recordId = Integer.parseInt(elements[0].split("=")[1]);
            // elements[1] depth
            int depth = Integer.parseInt(elements[1].split("=")[1]);
            // find node
            MixTreeNode node = getRecordIdNodeMapNode(depth, recordId);
            node.setClient(client);
            // leaf: setAsLeaf, process parentNode. default set as left child
            if (elements[2].startsWith("leaf")) {
                node.setAsLeaf(Double.parseDouble(elements[2].split("=")[1]));
            } else {
                // internal node: set feature infos, process parent/leftchild/rightchild
                node.setSplitFeatureType(Integer.parseInt(elements[2].split("=")[1]));
                node.setSplitFeatureName(elements[3].split("=")[1]);
                node.setSplitThreshold(Double.parseDouble(elements[4].split("=")[1]));
            }
        }
    }

    // reconstruct trees
    private void deserializeTrees2() {
        // depth == 1 means root nodes(whether as a leaf or a internal node)
        inferRootNodeList = recordIdNodeMap.values().parallelStream().filter(mixTreeNode -> mixTreeNode.getDepth() == 1).collect(Collectors.toList());
        // tree recordId ranges
        List<Integer> rootRecordId = inferRootNodeList.parallelStream().mapToInt(MixTreeNode::getRecordId).boxed().collect(Collectors.toList());
        // add a fake node whose recordId is bigger than all existing recordIds
        rootRecordId.add(recordIdNodeMap.size() + 1);
        for (int i = 0; i < rootRecordId.size() - 1; i++) {
            constructTree2(rootRecordId.get(i), rootRecordId.get(i + 1));
        }
    }

    private int constructTree2(int curRecord, int endRecord) {
        assert recordIdNodeMap.containsKey(curRecord);
        // return this tree last recordId + 1
        if (curRecord + 1 == endRecord || recordIdNodeMap.get(curRecord).isLeaf()) {
            return ++curRecord;
        }
        MixTreeNode curNode = recordIdNodeMap.get(curRecord);
        MixTreeNode left = recordIdNodeMap.get(curRecord + 1);
        assert curNode != null;
        assert left != null;
        curNode.setLeftChild(left);
        left.setParent(curNode);
        // construct left tree, thus get the recordId of the root of right tree
        int rightRecord = constructTree2(curRecord + 1, endRecord);
        MixTreeNode right = recordIdNodeMap.get(rightRecord);
        assert right != null;
        curNode.setRightChild(right);
        right.setParent(curNode);
        return constructTree2(rightRecord, endRecord);
    }

    @Override
    public AlgorithmType getAlgorithmType() {
        return algorithmType;
    }
}
