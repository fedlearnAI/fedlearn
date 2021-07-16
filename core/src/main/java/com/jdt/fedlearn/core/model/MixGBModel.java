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

package com.jdt.fedlearn.core.model;

import com.jdt.fedlearn.core.encryption.common.Ciphertext;
import com.jdt.fedlearn.core.encryption.common.EncryptionTool;
import com.jdt.fedlearn.core.encryption.common.PublicKey;
import com.jdt.fedlearn.core.encryption.fake.FakeTool;
import com.jdt.fedlearn.core.encryption.javallier.JavallierTool;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.common.InferenceInitRes;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.entity.mixGB.*;
import com.jdt.fedlearn.core.exception.NotImplementedException;
import com.jdt.fedlearn.core.loader.common.InferenceData;
import com.jdt.fedlearn.core.loader.common.TrainData;
import com.jdt.fedlearn.core.loader.mixGBoost.MixGBInferenceData;
import com.jdt.fedlearn.core.loader.mixGBoost.MixGBTrainData;
import com.jdt.fedlearn.core.metrics.Metric;
import com.jdt.fedlearn.core.model.common.loss.LogisticLoss;
import com.jdt.fedlearn.core.model.common.loss.Loss;
import com.jdt.fedlearn.core.model.common.loss.SquareLoss;
import com.jdt.fedlearn.core.model.common.loss.crossEntropy;
import com.jdt.fedlearn.core.model.common.tree.MixTreeNode;
import com.jdt.fedlearn.core.model.serialize.MixGBSerializer;
import com.jdt.fedlearn.core.parameter.MixGBParameter;
import com.jdt.fedlearn.core.parameter.SuperParameter;
import com.jdt.fedlearn.core.preprocess.InferenceFilter;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.core.type.MessageType;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.ObjectiveType;
import com.jdt.fedlearn.core.type.data.DoubleTuple2;
import com.jdt.fedlearn.core.type.data.StringTuple2;
import com.jdt.fedlearn.core.type.data.Tuple2;
import com.jdt.fedlearn.core.type.data.Tuple3;
import com.jdt.fedlearn.core.util.Tool;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


// TODO client: trainEpochInit add commonIDSet

/**
 * MixGBoost Model side codes
 * @author zhangwenxi
 */

public class MixGBModel implements Model {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MixGBModel.class);
    private EncryptionTool encryptionTool = new JavallierTool();
    private PublicKey pubKey;
    /**
     * 存储每一颗树的根节点
     */
    private List<MixTreeNode> curRootNodeList = new ArrayList<>();
    /**
     * 用于记录查找节点信息 包括叶节点(为了保存和恢复模型)
     */
    private Map<Integer, MixTreeNode> curRecordIdTreeNodeMap = new HashMap<>();
    /**
     * 用于记录最终的查找节点信息 包括叶节点(为了保存和恢复模型)
     */
    private Map<Integer, MixTreeNode> finalRecordIdTreeNodeMap;
    /**
     * 当前正在处理的节点
     */
    private MixTreeNode curTreeNode;
    /**
     * 混合 XGB 使用的参数信息
     */
    private MixGBParameter mixParams;
    /**
     * 混合 XGB G H info
     */
    private Map<Integer, Tuple3<Integer, Ciphertext, Ciphertext>> ghList;
    /**
     * local 共同ID集合
     */
    private Set<Integer> commonIdSet;
    /**
     * 数据预测的结果列表
     */
    private double[] predList;
    /**
     * loss类型
     */
    private Loss loss;
    /**
     * 保存和恢复模型
     */
    private String modelContent;

    public MixGBModel() {
    }

    public MixGBModel(String content, Loss loss, Map<Integer, MixTreeNode> finalRecordIdTreeNodeMap) {
        this.modelContent = content;
        this.loss = loss;
        this.finalRecordIdTreeNodeMap = finalRecordIdTreeNodeMap;
    }

    public MixGBModel(List<MixTreeNode> curRootNodeList, Map<Integer, Tuple3<Integer, Ciphertext, Ciphertext>> ghList, MixTreeNode node, double[] predList, Map<Integer, MixTreeNode> curRecordIdTreeNodeMap) {
        this.encryptionTool = new FakeTool();
        if (curRootNodeList != null) {
            this.curRootNodeList = curRootNodeList;
        }
        this.curTreeNode = node;
        this.ghList = ghList;
        this.predList = predList;
        if (curRecordIdTreeNodeMap != null) {
            this.curRecordIdTreeNodeMap = curRecordIdTreeNodeMap;
        }
    }

    /**
     * @param rawData   原始数据
     * @param parameter 从 master 传入的超参数
     * @param uids      用户 id 对照表
     * @param features  特征
     * @param others    其他参数
     * @return MixGBTrainData
     */
    @Override
    public MixGBTrainData trainInit(String[][] rawData, String[] uids, int[] testIndex, SuperParameter parameter, Features features, Map<String, Object> others) {
        logger.info("come in init process");
        pubKey = encryptionTool.restorePublicKey((String) others.get("pubkey"));
        mixParams = (MixGBParameter) parameter;
        Set<String> uidSet = new HashSet<>();
        if (uids == null || uids.length == 0) {
            commonIdSet = new HashSet<>();
        } else {
            uidSet = Arrays.stream(uids).filter(uid -> !"uid".equals(uid)).collect(Collectors.toSet());
            commonIdSet =  IntStream.range(0, uidSet.size()).boxed().collect(Collectors.toSet());
        }

        MixGBTrainData trainData = new MixGBTrainData(rawData, uidSet.toArray(new String[0]), features, new ArrayList<>());
        this.loss = getLoss(mixParams);
        return trainData;
    }

    /**
     * 进行模型训练，客户端整个控制流程
     * 消息驱动，对接收到的 master 消息类型进行反馈
     *
     * @param phase         阶段
     * @param parameterData 训练迭代参数
     * @param trainData     本地训练数据
     * @return 训练中间 Message
     */
    @Override
    public Message train(int phase, Message parameterData, TrainData trainData) {
        if (!(parameterData instanceof BoostBodyReq)) {
            logger.error("wrong message type!");
            throw new IllegalStateException("Unexpected message: " + parameterData.getClass().getName());
        }
        BoostBodyReq req = (BoostBodyReq) parameterData;
        MessageType msg = req.getMsgType();
        switch (msg) {
            case GlobalInit:
                return trainIdMatch();
            case EpochInit:
                return trainEpochInit(req, (MixGBTrainData) trainData);
            case TreeInit:
                return trainTreeInit((MixGBTrainData) trainData);
            case GiHi:
                return trainGiHi((MixGBTrainData) trainData);
            case UpdateGiHi:
                return trainUpdateGiHi(req, (MixGBTrainData) trainData);
            case FeaturesSet:
                return trainHorizontalFeatureValue(req, (MixGBTrainData) trainData);
            case H_IL:
                return trainHorizontalIL(req, (MixGBTrainData) trainData);
            case GkvHkv:
                return trainVerticalGkvHkv((MixGBTrainData) trainData);
            case KVGain:
                return trainVerticalGVGain(req, (MixGBTrainData) trainData);
            case Wj:
                return trainWj(req, (MixGBTrainData) trainData);
            case HorizontalSplit:
            case VerticalSplit:
                return trainSplit(req, (MixGBTrainData) trainData);
            case EpochFinish:
                return trainFinalModel(req, (MixGBTrainData) trainData);
            default:
                throw new IllegalStateException("Unexpected messageType: " + msg);
        }
    }

    /**
     * 初始化树
     */
    private void initTreeInfo(MixGBTrainData trainData) {
        curTreeNode = new MixTreeNode(1);
        curRootNodeList.add(curTreeNode);
        Set<Integer> idSet = new HashSet<>(trainData.getInstanceIdToIndexMap().keySet());
        curTreeNode.setInstanceIdSpaceSet(idSet);
    }

    /**
     * 进行每个 epoch 训练的初始化
     *
     * @return 将要发送给服务端的 GH 信息
     */
    private Message trainEpochInit(BoostBodyReq req, MixGBTrainData trainData) {
//        int[] commonId = req.getInstId();
//        if (commonId != null) {
//            commonIdSet = Arrays.stream(commonId).boxed().collect(Collectors.toSet());
//        }
        if (trainData.hasLabel()) {
            this.predList = new double[trainData.getLabel().length];
            Arrays.fill(predList, trainData.getFirstPredictValue());
        }
        curRecordIdTreeNodeMap.clear();
        curRootNodeList.clear();
        initTreeInfo(trainData);
        return trainGiHi(trainData);
    }

    /**
     * 进行每棵树训练的初始化
     *
     * @param trainData trainData
     * @return 将要发送给服务端的信息
     */
    private Message trainTreeInit(MixGBTrainData trainData) {
        initTreeInfo(trainData);
        return trainGiHi(trainData);
    }

    /**
     * 进行IdMatch
     *
     * @return 将要发送给服务端的 local Id
     */
    private Message trainIdMatch() {
        BoostBodyRes res = new BoostBodyRes(MessageType.GlobalInit);
        res.setInstId(commonIdSet.stream().mapToInt(Integer::intValue).toArray());
        return res;
    }


    /**
     * 计算并更新本地的 gi 和 hi
     */
    private StringTuple2[] updateGradHess(MixGBTrainData trainData) {
        double[] tempGi = loss.grad(predList, trainData.getLabel());
        double[] tempHi = loss.hess(predList, trainData.getLabel());
        StringTuple2[] res = new StringTuple2[tempHi.length];
        IntStream.range(0, tempGi.length).parallel().forEachOrdered(index -> {
            String encG = encryptionTool.encrypt(tempGi[index], pubKey).serialize();
            String encH = encryptionTool.encrypt(tempHi[index], pubKey).serialize();
            res[index] = new StringTuple2(encG, encH);
        });
        return res;
    }

    /**
     * 每棵树训练之初：
     * 客户端计算 gi 和 hi，并发送给服务端
     * 更新 metric 数据
     *
     * @return 将要发送给服务端的信息
     */
    private Message trainGiHi(MixGBTrainData trainData) {
        BoostBodyRes res = new BoostBodyRes(MessageType.GiHi);
        // no label, no need to submit local new gh
        if (!trainData.hasLabel()) {
            return res;
        }
        // 计算并更新本地的gi和hi信息, 获得gi和hi
        StringTuple2[] ghStr = updateGradHess(trainData);
        res.setGh(ghStr);
        int[] instID = trainData.getLocalLabeledId().keySet().stream().mapToInt(Integer::intValue).toArray();
        res.setInstId(instID);

        // 当已经建过一棵树之后，在每棵树训练之初更新 metric
        if (curRootNodeList.size() > 1) {
            res.setTrainMetric(updateMetric(trainData));
        }
        return res;
    }

    /**
     * 客户端接收服务端汇总后的 gi 和 hi 信息，更新本地的 gi 和 hi。
     *
     * @param req 服务端发送的json格式信息f
     * @return 将要发送给服务端的信息
     */
    private Message trainUpdateGiHi(BoostBodyReq req, MixGBTrainData trainData) {
        BoostBodyRes res = new BoostBodyRes(MessageType.UpdateGiHi);
        int[] instId = req.getInstId();
        if (instId != null) {
            StringTuple2[] enGH = req.getGh();
            final double[] cntList = req.getCntList();
            ghList = IntStream.range(0, instId.length).boxed().parallel()
                    .filter(i -> trainData.getInstanceIdToIndexMap().containsKey(instId[i]))
                    .collect(Collectors.toMap(i -> trainData.getInstanceIdToIndexMap().get(instId[i]),
                            i -> new Tuple3<>((int) cntList[i], encryptionTool.restoreCiphertext(enGH[i].getFirst()),
                                    encryptionTool.restoreCiphertext(enGH[i].getSecond()))));
        }
        return res;
    }

    /**
     * 返回对应特征的特征值
     *
     * @param req 服务端发送的特征查询信息
     * @return 对应特征的特征值
     */
    private Message trainHorizontalFeatureValue(BoostBodyReq req, MixGBTrainData trainData) {
        BoostBodyRes res = new BoostBodyRes(MessageType.FeatureValue);
        if (trainData.getFeatureDim() == 0) {
            return res;
        }
        String[] targetFeatureNames = req.getFeaturesSet();
        Map<String, Double> featureThresholds = Arrays.stream(targetFeatureNames).parallel()
                .map(feaName -> trainData.getFeatureRandValue(feaName, curTreeNode.getInstanceIdSpaceSet()))
                .filter(Objects::nonNull).collect(Collectors.toMap(Tuple2::_1, Tuple2::_2));
        res.setFvMap(featureThresholds);
        return res;
    }

    /**
     * 计算横向分裂方式的左节点空间
     *
     * @param req 服务端发送的json格式信息
     * @return 将要发送给服务端的信息
     */
    private Message trainHorizontalIL(BoostBodyReq req, MixGBTrainData trainData) {
        // 根据服务端发送的消息，对节点进行分裂，得到IL，发送给服务端。如果不存在对应的特征，返回空值
        Map<String, Double> featureThresholds = req.getfVMap();
        Map<String, Integer> feaIndexMap = trainData.getAllFeatureNamesToIndex();
        Map<String, Integer[]> tuple2IL = featureThresholds.entrySet().parallelStream()
                .filter(tuple -> feaIndexMap.containsKey(tuple.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> trainData.getLeftInstance(curTreeNode.getInstanceIdSpaceSet(), entry.getKey(), entry.getValue())));
        BoostBodyRes res = new BoostBodyRes(MessageType.H_IL);
        res.setFeaturesIL(tuple2IL);
        return res;
    }

    /**
     * 根据服务端发送的叶子节点权重，更新对应叶子结点实例的预测值
     * 根据服务端发送节点完成分裂消息，记录相关信息，回溯其他非叶子节点。
     *
     * @param req 服务端发送的json格式信息
     * @return 将要发送给服务端的信息
     */
    private Message trainWj(BoostBodyReq req, MixGBTrainData trainData) {
        // 使用 wj 更新前一轮的 label 信息
        double wj = req.getWj();
        if (trainData.hasLabel()) {
            Map<Integer, Integer> localLabeledId = trainData.getLocalLabeledId();
            curTreeNode.getInstanceIdSpaceSet().parallelStream()
                    .filter(localLabeledId::containsKey)
                    .forEach(instId -> {
                        int index = localLabeledId.get(instId);
                        predList[index] += wj;
                    });
        }
        curTreeNode.setAsLeaf(wj);
        curTreeNode.setSplitFeatureName("");
        curTreeNode.setSplitThreshold(0);
        // temp save all leaves
        curTreeNode.setRecordId(req.getRecordId());
        curRecordIdTreeNodeMap.put(req.getRecordId(), curTreeNode);
        curTreeNode = curTreeNode.backTrackingTreeNode();
        return new BoostBodyRes(MessageType.Wj);
    }

    /**
     * 获取特征值排序信息
     *
     * @param featureIndex 特征编号
     * @param trainData    数据
     * @return 特征值排序信息
     */
    private Tuple3<Integer, List<Map.Entry<Double, List<Integer>>>, Set<Integer>> getFeaSplitCandidate(int featureIndex, MixGBTrainData trainData) {
        Set<Integer> toUseInstIdList = new HashSet<>(curTreeNode.getVerticalInstanceIdSpaceSet());
        // missing-value IDs in toUseInstIdList
        Set<Integer> missingValueInstIdSet = new HashSet<>(trainData.getFeatureMissValueInstIdMap()[featureIndex]);
        missingValueInstIdSet.retainAll(toUseInstIdList);
        // split toUseInstIdList to two sets: toUseInstIdList and missingValueInstIdSet
        toUseInstIdList.removeAll(missingValueInstIdSet);
        if (toUseInstIdList.isEmpty()) {
            return null;
        }
        // map missing value IDs to indexes
        missingValueInstIdSet = missingValueInstIdSet.parallelStream()
                .map(id -> trainData.getInstanceIdToIndexMap().get(id))
                .collect(Collectors.toSet());
        Map<Integer, Double> idFeatureValueMap = trainData.getUsageIdFeatureValueByIndex(toUseInstIdList, featureIndex);
        // merge IDs that have same values, group by values
        Map<Double, List<Integer>> idGroupByValueMap = idFeatureValueMap.entrySet().parallelStream()
                .collect(Collectors.groupingBy(Map.Entry::getValue, Collectors.mapping(Map.Entry::getKey, Collectors.toList())));
        // sort values
        List<Map.Entry<Double, List<Integer>>> sortedIdGroupByValueMap = new ArrayList<>(idGroupByValueMap.entrySet());
        sortedIdGroupByValueMap.sort(Comparator.comparingDouble(Map.Entry::getKey));
        return new Tuple3<>(featureIndex, sortedIdGroupByValueMap, missingValueInstIdSet);
    }

    /**
     * 获取特征的 GL和 HL信息
     *
     * @param binSize                    桶大小限制 若为零 则目前分桶个数已经符合参数要求
     * @param sortedIndexGroupByValueMap 将要使用的实例 feature sorted 列表
     * @param missingValueInstIdSet      缺失该特征值的样本
     * @return 将要发送给服务端的Gkv和Hkv信息
     */
    private Tuple2<StringTuple2[], double[]> getGlHlNew(int binSize, List<Map.Entry<Double, List<Integer>>> sortedIndexGroupByValueMap, Set<Integer> missingValueInstIdSet) {
        final Ciphertext zero = encryptionTool.encrypt(0, pubKey);
        /* 计算相同取值的样本的梯度值和 */
        List<Tuple3<Double, Ciphertext, Ciphertext>> sortedSameValueSumGHList = sortedIndexGroupByValueMap.parallelStream()
                .map(e -> {
                    Ciphertext tempGlValue = zero;
                    Ciphertext tempHlValue = zero;
                    for (int index : e.getValue()) {
                        Tuple3<Integer, Ciphertext, Ciphertext> tuple3 = ghList.get(index);
                        tempGlValue = encryptionTool.add(tempGlValue, tuple3._2().orElse(zero), pubKey);
                        tempHlValue = encryptionTool.add(tempHlValue, tuple3._3().orElse(zero), pubKey);
                    }
                    return new Tuple3<>(e.getKey(), tempGlValue, tempHlValue);
                }).collect(Collectors.toList());

        /* 获取特征的符合参数桶个数要求的分桶候选*/
        if (sortedIndexGroupByValueMap.size() > mixParams.getMaxBinNum()) {
            sortedSameValueSumGHList = getBinGHSplit(binSize, sortedIndexGroupByValueMap, sortedSameValueSumGHList);
        }
        /* 处理缺失值情况 */
        if (!missingValueInstIdSet.isEmpty()) {
            sortedSameValueSumGHList = getMissingGHSplit(missingValueInstIdSet, sortedSameValueSumGHList);
        }

        StringTuple2[] ghStr = sortedSameValueSumGHList.parallelStream().map(gh ->
                new StringTuple2(gh._2().orElse(zero).serialize(), gh._3().orElse(zero).serialize())).toArray(StringTuple2[]::new);
        return new Tuple2<>(ghStr, sortedSameValueSumGHList.parallelStream()
                .mapToDouble(gh -> gh._1().orElse(0.0)).toArray());
    }

    /**
     * 获取特征的符合参数桶个数要求的分桶候选
     *
     * @param binSize                  桶大小限制 若为零 则目前分桶个数已经符合参数要求
     * @param sortedSameValueSumGHList 目前特征值的有序取值和相应的梯度分桶累加结果
     * @return 特征的符合参数桶个数要求的分桶候选
     */
    private List<Tuple3<Double, Ciphertext, Ciphertext>> getBinGHSplit(int binSize, List<Map.Entry<Double, List<Integer>>> sortedIndexGroupByValueMap, List<Tuple3<Double, Ciphertext, Ciphertext>> sortedSameValueSumGHList) {
        final Ciphertext zero = encryptionTool.encrypt(0, pubKey);

        int uniqueSize = sortedIndexGroupByValueMap.size();
        List<Tuple3<Double, Ciphertext, Ciphertext>> newSortedSameValueSumGHList = new ArrayList<>();
        int index = 0;
        Tuple3<Double, Ciphertext, Ciphertext> ghTriple;
        while (index < uniqueSize) {
            int tmpBucketSize = sortedIndexGroupByValueMap.get(index).getValue().size();
            ghTriple = sortedSameValueSumGHList.get(index);
            Ciphertext tempGlValue = ghTriple._2().orElse(zero);
            Ciphertext tempHlValue = ghTriple._3().orElse(zero);
            index++;
            while (tmpBucketSize < binSize && index < uniqueSize) {
                tmpBucketSize += sortedIndexGroupByValueMap.get(index).getValue().size();
                ghTriple = sortedSameValueSumGHList.get(index);
                tempGlValue = encryptionTool.add(tempGlValue, ghTriple._2().orElse(zero), pubKey);
                tempHlValue = encryptionTool.add(tempHlValue, ghTriple._3().orElse(zero), pubKey);
                index++;
            }
            newSortedSameValueSumGHList.add(new Tuple3<>(sortedSameValueSumGHList.get(index - 1)._1().orElse(-Double.MAX_VALUE), tempGlValue, tempHlValue));
        }
        return newSortedSameValueSumGHList;
    }

    /**
     * 获取特征的缺失值在左子树和右子树两种情况的分桶候选
     *
     * @param missingValueInstIdSet    缺失该特征值的样本
     * @param sortedSameValueSumGHList 目前特征值的有序取值和相应的梯度分桶累加结果
     * @return 缺失值在左子树和右子树两种情况的分桶候选
     */
    private List<Tuple3<Double, Ciphertext, Ciphertext>> getMissingGHSplit(Set<Integer> missingValueInstIdSet, List<Tuple3<Double, Ciphertext, Ciphertext>> sortedSameValueSumGHList) {
        final Ciphertext zero = encryptionTool.encrypt(0, pubKey);
        Ciphertext missingValueG = zero;
        Ciphertext missingValueH = zero;
        for (int index : missingValueInstIdSet) {
            Tuple3<Integer, Ciphertext, Ciphertext> ghPair = ghList.get(index);
            missingValueG = encryptionTool.add(missingValueG, ghPair._2().orElse(zero), pubKey);
            missingValueH = encryptionTool.add(missingValueH, ghPair._3().orElse(zero), pubKey);
        }

        Ciphertext finalMissingValueG = missingValueG;
        Ciphertext finalMissingValueH = missingValueH;

        List<Tuple3<Double, Ciphertext, Ciphertext>> doubleSortedSameValueSumGHList = sortedSameValueSumGHList.parallelStream()
                .map(gh -> new Tuple3<>(gh._1().orElse(-Double.MAX_VALUE), encryptionTool.add(gh._2().orElse(zero), finalMissingValueG, pubKey),
                        encryptionTool.add(gh._3().orElse(zero), finalMissingValueH, pubKey)))
                .collect(Collectors.toList());
        sortedSameValueSumGHList = Tool.listAlternateMerge(sortedSameValueSumGHList, doubleSortedSameValueSumGHList);
        return sortedSameValueSumGHList;
    }

    /**
     * 获取随机特征集合
     *
     * @param featureDim 所有的特征维度
     * @param resultNum  目标特征维度
     * @return 随机产生将要使用的特征维度的集合
     */
    private List<Integer> getRandomFeatureSet(int featureDim, int resultNum) {
        List<Integer> resultSet = new ArrayList<>();
        List<Integer> tempList = IntStream.range(0, featureDim).boxed().collect(Collectors.toCollection(ArrayList::new));
        while (resultSet.size() < resultNum) {
            //显示数字并将其从列表中删除,从而实现不重复.
            int j = new Random().nextInt(tempList.size());
            resultSet.add(tempList.get(j));
            tempList.remove(j);
        }
        return resultSet;
    }

    /**
     * 计算客户端中的 Gkv和 Hkv 的值，需要先对没有使用的特征进行随机刷选，然后计算。
     *
     * @return 计算所得到的的Gkv和Hkv信息
     */
    private StringTuple2[][] getGkvHkv(MixGBTrainData trainData, int binSize) {
        // 随机特征集合
        int useFeatureNum = trainData.getFeatureDim();
        if (useFeatureNum >= 3) {
            useFeatureNum = Integer.max(1, (int) (useFeatureNum * mixParams.getVerticalFeatureSampling()));
        }
        List<Integer> usefulFeatureSet = getRandomFeatureSet(trainData.getFeatureDim(), useFeatureNum);

        List<Tuple3<Integer, List<Map.Entry<Double, List<Integer>>>, Set<Integer>>> feaSplitCandidate = usefulFeatureSet.parallelStream().map(index ->
                getFeaSplitCandidate(index, trainData)).filter(Objects::nonNull).collect(Collectors.toList());
        List<Tuple2<StringTuple2[], double[]>> boostGkvHkvBodies = feaSplitCandidate.parallelStream().map(candidate ->
                getGlHlNew(binSize, candidate._2().get(), candidate._3().get())).collect(Collectors.toList());
        trainData.setFeatureIndexList(feaSplitCandidate.parallelStream().mapToInt(candidate -> candidate._1().get()).toArray());
        trainData.setFeatureThresholdList(boostGkvHkvBodies.parallelStream().map(Tuple2::_2).toArray(double[][]::new));
        return boostGkvHkvBodies.parallelStream().map(Tuple2::_1).toArray(StringTuple2[][]::new);
    }

    /**
     * 客户端计算Gkv和Hkv的值，返回给服务端；如果服务端发送的信息中，包含了[[gi]]和[[hi]]，则更新客户端的这些信息
     *
     * @return 将要发送给服务端的信息
     */
    private Message trainVerticalGkvHkv(MixGBTrainData trainData) {
        BoostBodyRes res = new BoostBodyRes(MessageType.GkvHkv);
        if (trainData.getFeatureDim() == 0) {
            return res;
        }
        // 仅仅对共同ID的那部分数据进行处理
        Set<Integer> tempToUseIdSet = new HashSet<>(curTreeNode.getInstanceIdSpaceSet());
        tempToUseIdSet.retainAll(commonIdSet);
        if (tempToUseIdSet.size() <= mixParams.getMinSampleSplit()) {
            return res;
        }
        curTreeNode.setVerticalInstanceIdSpaceSet(tempToUseIdSet);
        int binSize = Math.max(1, curTreeNode.getVerticalInstanceIdSpaceSet().size() / mixParams.getMaxBinNum());
        StringTuple2[][] gkvHkvList = getGkvHkv(trainData, binSize);
        res.setFeatureGlHl(gkvHkvList);
        return res;
    }

    /**
     * 客户端根据接收到的(k, v, gain)值，计算得到当前节点的IL，返回给服务端；
     *
     * @param req 服务端发送的json格式信息
     * @return 将要发送给服务端的信息
     */
    private Message trainVerticalGVGain(BoostBodyReq req, MixGBTrainData trainData) {
        // 需要传递一个特征的一个真实的 value值，而不是序号
        int featureIndex = trainData.getFeatureIndexList()[req.getK()];
        double splitThreshold = trainData.getFeatureThresholdList()[req.getK()][req.getV()];
        String featureName = trainData.getFeatureName()[featureIndex];
        trainData.setFeatureThresholdList(null);
        trainData.setFeatureIndexList(null);
        // from common ids
        int[] tempIl = trainData.getLeftInstanceForFeaSplit(curTreeNode.getInstanceIdSpaceSet(), featureIndex, splitThreshold, Math.abs(req.getV()) % 2 == 1);
        if (tempIl.length == 0) {
            logger.error("vertical split, IdLeftSet is null! ");
        }
        curTreeNode.setSplitFeatureName(featureName);
        curTreeNode.setSplitFeatureType(1);
        curTreeNode.setSplitThreshold(splitThreshold);
        curTreeNode.setRecordId(req.getRecordId());
        // reduce memory cost
        curTreeNode.setVerticalInstanceIdSpaceSet(null);
        curRecordIdTreeNodeMap.put(req.getRecordId(), curTreeNode);
        MixTreeNode leftNode = new MixTreeNode(curTreeNode.getDepth() + 1);
        leftNode.setParent(curTreeNode);
        Set<Integer> leftIds = Arrays.stream(tempIl).boxed().collect(Collectors.toCollection(HashSet::new));
        leftNode.setInstanceIdSpaceSet(leftIds);
        curTreeNode.setLeftChild(leftNode);
        curTreeNode = leftNode;
        Map<String, Double> feaValue = new HashMap<>();
        feaValue.put(featureName, splitThreshold);
        // 需要一个真实的features value阈值
        BoostBodyRes res = new BoostBodyRes(MessageType.KVGain);
        res.setFvMap(feaValue);
        res.setInstId(tempIl);
        return res;
    }

    /**
     * 进行横向纵向分裂的处理
     *
     * @param req 服务端发送的json格式信息
     * @return 将要发送给服务端的信息
     */
    private Message trainSplit(BoostBodyReq req, MixGBTrainData trainData) {
        BoostBodyRes res = new BoostBodyRes(req.getMsgType());
        // 根据服务端发送IL，记录相关信息，进入下一个节点。
        curTreeNode.setRecordId(req.getRecordId());
        if (req.getMsgType() == MessageType.HorizontalSplit) {
            curTreeNode.setSplitFeatureType(0);
        } else {
            curTreeNode.setSplitFeatureType(1);
        }
        if (curTreeNode.getRecordId() > 0) {
            if (req.getMsgType() == MessageType.HorizontalSplit) {
                curTreeNode.setSplitFeatureName(req.getFeatureName());
                curTreeNode.setSplitThreshold(req.getFeatureThreshold());
            }
            curRecordIdTreeNodeMap.put(req.getRecordId(), curTreeNode);
        }
        // reduce memory cost
        curTreeNode.setVerticalInstanceIdSpaceSet(null);
        trainData.setFeatureThresholdList(null);
        trainData.setFeatureIndexList(null);
        MixTreeNode leftNode = new MixTreeNode(curTreeNode.getDepth() + 1);
        leftNode.setParent(curTreeNode);
        Set<Integer> leftSet = new HashSet<>(curTreeNode.getInstanceIdSpaceSet());
        leftSet.retainAll(Arrays.stream(req.getInstId()).boxed().collect(Collectors.toSet()));
        leftNode.setInstanceIdSpaceSet(leftSet);
        curTreeNode.setLeftChild(leftNode);
        curTreeNode = leftNode;
        return res;
    }

    /**
     * 训练完毕后的，训练模型的处理和保存
     *
     * @param req 服务端发送的json格式信息
     * @return 将要发送给服务端的信息
     */
    private Message trainFinalModel(BoostBodyReq req, MixGBTrainData trainData) {
        // 最终的模型
        finalRecordIdTreeNodeMap = curRecordIdTreeNodeMap;
        curTreeNode = null;
        BoostBodyRes res = new BoostBodyRes(MessageType.MetricValue);
        int[] saveNodes = req.getSaveNodes() == null ? new int[0] : req.getSaveNodes();
        List<Integer> saveNodesList = Arrays.stream(saveNodes).boxed().collect(Collectors.toList());

        int[] deleteNodes = req.getDeleteNodes();
        Arrays.stream(deleteNodes).filter(x -> !saveNodesList.contains(x)).forEach(x -> finalRecordIdTreeNodeMap.remove(x));

        List<Integer> myLeaves = finalRecordIdTreeNodeMap.entrySet().stream()
                .filter(x -> x.getValue().isLeaf() && x.getValue().getParent() != null && x.getValue().getParent().getRecordId() <= 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        myLeaves.stream().filter(x -> !saveNodesList.contains(x)).forEach(x -> finalRecordIdTreeNodeMap.remove(x));
        res.setTrainMetric(updateMetric(trainData));
        return res;
    }

    /**
     * @param uidList            infer uid
     * @param inferenceCacheFile infer data
     * @param others             自定义参数
     * @return init Message containing existing IDs
     */
    @Override
    public Message inferenceInit(String[] uidList, String[][] inferenceCacheFile, Map<String, Object> others) {
        InferenceInitRes res = (InferenceInitRes) InferenceFilter.filter(uidList, inferenceCacheFile);
        return new BoostInferenceInitRes(res.isAllowList(), res.getUid(), modelContent);
    }

    /**
     * 按层级推理，每次尽量推理到本平台能遍历的最深层。以此提高推理速度。
     *
     * @param queryNode 当前进入的根节点
     * @param queryIds  需要推理的 id
     * @param data      推理数据
     * @return 推理中间结果
     * 本平台能到达叶节点: 返回叶节点权重
     * 本平台不能到达叶节点: 返回无法继续的节点编号、向左或向右的指示，由 master 向其他平台 query 下一层节点走向
     */
    private BoostInferQueryResBody[] inferLevelTravel(MixTreeNode queryNode, Set<String> queryIds, InferenceData data) {
        Queue<Tuple2<MixTreeNode, Set<String>>> levelNodes = new LinkedList<>();
        levelNodes.offer(new Tuple2<>(queryNode, queryIds));

        List<BoostInferQueryResBody> resBodies = new ArrayList<>();

        while (!levelNodes.isEmpty()) {
            Tuple2<MixTreeNode, Set<String>> tuple2 = levelNodes.poll();
            assert tuple2 != null;
            MixTreeNode node = tuple2._1();
            Set<String> leftSet = ((MixGBInferenceData) data).getLeftInstance(tuple2._2(), node.getSplitFeatureName(), node.getSplitThreshold());

            if (!leftSet.isEmpty()) {
                // left set: if the left node is on other clients ,send back to master
                BoostInferQueryResBody leftPath = inferNodePath(leftSet, node.getRecordId(), node.getLeftChild(), 0);
                if (leftPath != null) {
                    resBodies.add(leftPath);
                } else {
                    // else: continue process the left node
                    levelNodes.offer(new Tuple2<>(node.getLeftChild(), leftSet));
                }
            }
            // if left set size < root set size, then process the right set
            if (leftSet.size() < tuple2._2().size()) {
                tuple2._2().removeAll(leftSet);
                BoostInferQueryResBody rightPath = inferNodePath(tuple2._2(), node.getRecordId(), node.getRightChild(), 1);
                // right set: if the right node is on other clients ,send back to master
                if (rightPath != null) {
                    resBodies.add(rightPath);
                } else {
                    // else: continue process the right node
                    levelNodes.offer(new Tuple2<>(node.getRightChild(), tuple2._2()));
                }
            }
        }
        return resBodies.toArray(new BoostInferQueryResBody[0]);
    }

    /**
     * 进行推理的总体控制
     *
     * @param phase    阶段
     * @param jsonData 中间数据
     * @param data     推理的数据集
     * @return 将要发送给服务端的信息
     */
    @Override
    public Message inference(int phase, Message jsonData, InferenceData data) {
        BoostInferQueryReqBody req = (BoostInferQueryReqBody) jsonData;
        if (!finalRecordIdTreeNodeMap.containsKey(req.getRecordId())) {
            return new BoostInferQueryRes(null);
        }
        MixTreeNode queryNode = finalRecordIdTreeNodeMap.get(req.getRecordId());
        Set<String> queryIds = Arrays.stream(req.getInstanceId()).collect(Collectors.toSet());
        BoostInferQueryResBody[] bodies = inferLevelTravel(queryNode, queryIds, data);
        return new BoostInferQueryRes(bodies);
    }

    /**
     * @param ids      id 集合
     * @param parentId 当前节点的父节点（父节点一定在本地）
     * @param node     当前节点
     * @param left     结果是否向左
     * @return 本平台能到达叶节点: 返回叶节点权重
     * 当前节点为空或本地不存在，本平台不能到达叶节点: 返回无法继续的节点编号、向左或向右的指示，由 master 向其他平台 query 下一层节点走向
     */
    private BoostInferQueryResBody inferNodePath(Set<String> ids, int parentId, MixTreeNode node, int left) {
        if (node == null || node.getRecordId() < 0) {
            return new BoostInferQueryResBody(ids.toArray(new String[0]), parentId, left);
        }
        if (node.isLeaf()) {
            return new BoostInferQueryResBody(ids.toArray(new String[0]), -1, node.getLeafScore());
        }
        // else: continue process the left node
        return null;
    }

    /**
     * @return 模型序列化
     */
    @Override
    public String serialize() {
        MixGBSerializer mixGBSerializer = new MixGBSerializer(loss, mixParams, finalRecordIdTreeNodeMap);
        return mixGBSerializer.saveMixGBModel();
    }

    /**
     * @param content 模型反序列化
     */
    @Override
    public void deserialize(String content) {
        MixGBSerializer mixGBSerializer = new MixGBSerializer(null, null, new HashMap<>());
        MixGBModel model = mixGBSerializer.loadMixGBModel(content);
        this.modelContent = model.modelContent;
        this.loss = model.loss;
        this.finalRecordIdTreeNodeMap = model.finalRecordIdTreeNodeMap;
    }

    /**
     * @return modeltype
     */
    @Override
    public AlgorithmType getModelType() {
        return AlgorithmType.MixGBoost;
    }

    /**
     * @param parameter mixgb param
     * @return Loss
     */
    private Loss getLoss(MixGBParameter parameter) {
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

    /**
     * 更新当前的 metric
     *
     * @param trainData 训练数据
     * @return 训练当前的 metric
     */
    private Map<MetricType, DoubleTuple2> updateMetric(MixGBTrainData trainData) {
        if (!trainData.hasLabel() || trainData.getLabel() == null || trainData.getLocalLabeledId() == null || trainData.getLocalLabeledId().isEmpty()) {
            return new EnumMap<>(MetricType.class);
        }
        MetricType[] evalMetric = mixParams.getEvalMetric();
        Map<MetricType, Double> allMetric;
        double[] allWeight = trainData.getLocalLabeledId().keySet().parallelStream().mapToDouble(id -> 1.0 / ghList.get(trainData.getInstanceIdToIndexMap().get(id))._1().get()).toArray();
        if (ObjectiveType.countPoisson == mixParams.getObjective()) {
            allMetric = Metric.calculateLocalMetricSumPart(evalMetric, loss.expTransform(predList), loss.expTransform(trainData.getLabel()), allWeight);
        } else {
            allMetric = Metric.calculateLocalMetricSumPart(evalMetric, loss.transform(predList), trainData.getLabel(), allWeight);
        }
        int[] commonIdIndex = commonIdSet.stream().filter(id -> trainData.getLocalLabeledId().containsKey(id)).mapToInt(id -> trainData.getLocalLabeledId().get(id)).toArray();
        if (commonIdIndex == null) {
            return allMetric.entrySet().parallelStream().collect(Collectors.toMap(Map.Entry::getKey, entry -> new DoubleTuple2(entry.getValue(), 0)));
        }
        double[] commonWeight = Arrays.stream(commonIdIndex).parallel().mapToDouble(id -> 1.0 / ghList.get(id)._1().get()).toArray();
        double[] commonLabel = Arrays.stream(commonIdIndex).parallel().mapToDouble(index -> trainData.getLabel()[index]).toArray();
        double[] commonPred = Arrays.stream(commonIdIndex).parallel().mapToDouble(index -> predList[index]).toArray();
//        //此处统计指标除debug外不再开启，返回给master端，并在master端查看和展示
        if (ObjectiveType.countPoisson == mixParams.getObjective()) {
            Map<MetricType, Double> commonMetric = Metric.calculateLocalMetricSumPart(evalMetric, loss.expTransform(commonPred), loss.expTransform(commonLabel), commonWeight);
            return allMetric.entrySet().parallelStream().collect(Collectors.toMap(Map.Entry::getKey, entry -> new DoubleTuple2(entry.getValue(), commonMetric.get(entry.getKey()))));
        }
        Map<MetricType, Double> commonMetric = Metric.calculateLocalMetricSumPart(evalMetric, loss.transform(commonPred), commonLabel, commonWeight);
        return allMetric.entrySet().parallelStream().collect(Collectors.toMap(Map.Entry::getKey, entry -> new DoubleTuple2(entry.getValue(), commonMetric.get(entry.getKey()))));
    }
}
