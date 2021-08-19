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
import com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillier;
import com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillierNative;
import com.jdt.fedlearn.core.encryption.distributedPaillier.HomoEncryptionUtil;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.common.InferenceInitRes;
import com.jdt.fedlearn.core.entity.common.MetricValue;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.entity.mixGBoost.*;
import com.jdt.fedlearn.core.entity.mixedLinearRegression.CypherMessage;
import com.jdt.fedlearn.core.entity.mixedLinearRegression.CypherMessageList;
import com.jdt.fedlearn.core.entity.serialize.JavaSerializer;
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
import com.jdt.fedlearn.core.type.data.Pair;
import com.jdt.fedlearn.core.type.data.Tuple2;
import com.jdt.fedlearn.core.type.data.Tuple3;
import com.jdt.fedlearn.core.util.Tool;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * MixGBoost Model side codes
 * @author zhangwenxi
 */

public class MixGBModel implements Model {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MixGBModel.class);
    /**
     * 存储每一颗树的根结点
     */
    private List<MixTreeNode> curRootNodeList;
    /**
     * 用于记录查找结点信息 包括叶结点(为了保存和恢复模型)
     */
    private Map<Integer, MixTreeNode> curRecordIdTreeNodeMap;
    /**
     * 当前正在处理的结点
     */
    private MixTreeNode curTreeNode;
    /**
     * 混合 XGB 使用的参数信息
     */
    private MixGBParameter mixParams;
    private MetricValue metricValue;
    /**
     * common ID G H info
     */
    private Map<Integer, DistributedPaillierNative.signedByteArray[]> encGH;
    private Map<Integer, DoubleTuple2> localGH;
    /**
     * 训练中的当前叶结点权重是否保存(根据本地 label 情况和叶结点上样本情况决定)
     */
    private boolean saveWj;
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
    private int globalRecordId = 0;
    /**
     * 所有推理实例
     */
    private List<String> inferUid;
    /**
     * 存储推理实例的结果的 Map
     */
    private Map<String, Double> inferScoreMap;
    private int[] startNodes;
    private boolean secureMode = true;
    private HomoEncryptionUtil pheKeys;
    private int thisPartyID;
    private DistributedPaillierNative.signedByteArray[] decPartialSum;
    private DistributedPaillierNative.signedByteArray[] partialSum;
    private DistributedPaillierNative.signedByteArray[][] gkvHkvList;
    private String[] featureNames;
    private double[] featureThresholds;

    public MixGBModel() {
    }

    public MixGBModel(MixGBSerializer mixGBSerializer) {
        this.loss = mixGBSerializer.getLoss();
        int[] recordId = mixGBSerializer.getRecordId();
        List<MixTreeNode> treeNodes = mixGBSerializer.getNodeList();
        this.curRecordIdTreeNodeMap = IntStream.range(0, recordId.length).boxed().collect(Collectors.toMap(i -> recordId[i], treeNodes::get));
    }

    public MixGBModel(List<MixTreeNode> curRootNodeList, Map<Integer, Tuple3<Integer, Ciphertext, Ciphertext>> ghList, MixTreeNode node, double[] predList, Map<Integer, MixTreeNode> curRecordIdTreeNodeMap) {
        if (curRootNodeList != null) {
            this.curRootNodeList = curRootNodeList;
        }
        this.curTreeNode = node;
        this.predList = predList;
        if (curRecordIdTreeNodeMap != null) {
            this.curRecordIdTreeNodeMap = curRecordIdTreeNodeMap;
        }
    }

    private static DoubleTuple2 doubleTupleSum(DoubleTuple2 a, DoubleTuple2 b) {
        return new DoubleTuple2(a.getFirst() + b.getFirst(), a.getSecond() + b.getSecond());
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
        int numP = (int) others.get("numP");
        int encBits = (int) others.get("ENC_BITS");
        secureMode = (boolean) others.get("secureMode");
        boolean useFakeEnc = !secureMode;
        pheKeys = new HomoEncryptionUtil(numP, encBits, useFakeEnc);
        this.thisPartyID =  (Integer) others.get("thisPartyID") ;

        if (secureMode) {
            DistributedPaillier.DistPaillierPubkey pubkey = new DistributedPaillier.DistPaillierPubkey();
            DistributedPaillier.DistPaillierPrivkey privkey = new DistributedPaillier.DistPaillierPrivkey();
            pubkey.parseJson((String)others.get( "pubKeyStr" ));
            privkey.parseJson((String)others.get("privKeyStr"));
            this.pheKeys.setPk(pubkey);
            this.pheKeys.setSk(privkey);
            this.pheKeys.getSk().setRank(thisPartyID);
        }
        mixParams = (MixGBParameter) parameter;
        this.loss = getLoss(mixParams.getObjective(), mixParams.getNumClass());
        Set<String> uidSet = new HashSet<>();

        if (uids == null || uids.length == 0) {
            commonIdSet = new HashSet<>();
        } else {
            uidSet = Arrays.stream(uids).filter(uid -> !"uid".equals(uid)).collect(Collectors.toSet());
            commonIdSet =  IntStream.range(0, uidSet.size()).boxed().collect(Collectors.toSet());
        }
        Set<String> commonFeas = (Set<String>) others.get("commonFea");
        return new MixGBTrainData(rawData, uidSet.toArray(new String[0]), features, commonFeas);
    }

    /**
     * 进行模型训练，客户端整个控制流程
     * 消息驱动，对接收到的 master 消息类型进行反馈
     *
     * @param phase         阶段
     * @param parameterData 训练迭代参数
     * @param trainData     本地训练数据
     * @return 训练中间 BoostBodyRes
     */
    @Override
    public Message train(int phase, Message parameterData, TrainData trainData) {
        if (!(parameterData instanceof BoostBodyReq)) {
            logger.error("wrong BoostBodyRes type!");
            throw new IllegalStateException("Unexpected BoostBodyRes: " + parameterData.getClass().getName());
        }
        BoostBodyReq req = (BoostBodyReq) parameterData;
        MessageType msg = req.getMsgType();
        BoostBodyRes res;
        switch (msg) {
            case EpochInit:
                res = trainEpochInit((MixGBTrainData) trainData); break;
            case UpdateGiHi:
                res = trainUpdateGiHi(req, (MixGBTrainData) trainData); break;
            case FeaturesSet:
                res = trainHorizontalFeatureSet((MixGBTrainData) trainData); break;
            case FeatureValue:
                res = trainHorizontalFeatureValue(req, (MixGBTrainData) trainData); break;
            case HorizontalEnc:
                res = trainHorizontalEncGH(req, (MixGBTrainData) trainData); break;
            case HorizontalDec:
                res = horizontalGHDecryption(req); break;
            case HorizontalGain:
                res = trainHorizontalGain(req, (MixGBTrainData) trainData); break;
            case GkvHkv:
                res = trainVerticalGkvHkv(req, (MixGBTrainData) trainData); break;
            case VerticalDec:
                res = verticalPartialDecryption(req); break;
            case KVGain:
                res = trainVerticalGVGain(req, (MixGBTrainData) trainData); break;
            case V_IL:
                res = setVerticalSplit(req, (MixGBTrainData) trainData); break;
            case HorizontalSplit:
                res = horizontalSplit((MixGBTrainData) trainData); break;
            case VerticalSplit:
                res = verticalSplit(req, (MixGBTrainData) trainData); break;
            case WjEnc:
                res = decPartial(req); break;
            case WjDec:
                res = finalDecWjSum(req, (MixGBTrainData) trainData); break;
            case Wj:
                res = afterLeaf((MixGBTrainData) trainData); break;
            case EpochFinish:
                res = trainFinalModel(req, (MixGBTrainData) trainData); break;
            default:
                throw new IllegalStateException("Unexpected MessageType: " + msg);
        }
        res.setMetricValue(metricValue);
        return res;
    }

    /**
     * 初始化树
     */
    private void initTreeInfo(MixGBTrainData trainData) {
        updateMetric(trainData);
        curTreeNode = new MixTreeNode(1);
        curRootNodeList.add(curTreeNode);
        Set<Integer> idSet = IntStream.range(0, trainData.getDatasetSize()).boxed().collect(Collectors.toSet());
        curTreeNode.setInstanceIdSpaceSet(idSet);
    }

    /**
     * 进行每个 epoch 训练的初始化
     *
     * @return 将要发送给服务端的 GH 信息
     */
    private BoostBodyRes trainEpochInit(MixGBTrainData trainData) {
        if (trainData.hasLabel()) {
            predList = new double[trainData.getLabel().length];
            Arrays.fill(predList, trainData.getFirstPredictValue());
        }
        curRecordIdTreeNodeMap = new HashMap<>();
        curRootNodeList = new ArrayList<>();
        initTreeInfo(trainData);
        return trainGiHi(trainData);
    }

    /**
     * 进行每棵树训练的初始化
     *
     * @param trainData trainData
     * @return 将要发送给服务端的信息
     */
    private BoostBodyRes trainTreeInit(MixGBTrainData trainData) {
        initTreeInfo(trainData);
        return trainGiHi(trainData);
    }

    /**
     * 计算并更新本地的 gi 和 hi
     */
    private DistributedPaillierNative.signedByteArray[][] updateGradHess(MixGBTrainData trainData) {
        double[] tempGi = loss.grad(predList, trainData.getLabel());
        double[] tempHi = loss.hess(predList, trainData.getLabel());

        List<Integer> instId = trainData.getLocalLabeledId();

        localGH = IntStream.range(0, tempGi.length).boxed().parallel().collect(Collectors.toMap(instId::get,
            index -> new DoubleTuple2(tempGi[index], tempHi[index])));
        /* compute common id gi hi, if there exists. */
        return commonIdSet.parallelStream().map(id -> {
            if (localGH.containsKey(id)) {
                DoubleTuple2 gh = localGH.get(id);
                DistributedPaillierNative.signedByteArray[] ghPair = new DistributedPaillierNative.signedByteArray[2];
                ghPair[0] = pheKeys.encryption(gh.getFirst(), pheKeys.getPk());
                ghPair[1] = pheKeys.encryption(gh.getSecond(), pheKeys.getPk());
                return ghPair;
            }
            return null;
        }).filter(Objects::nonNull).toArray(DistributedPaillierNative.signedByteArray[][]::new);
    }

    /**
     * 每棵树训练之初：
     * 客户端计算 gi 和 hi，并发送给服务端
     * 更新 metric 数据
     *
     * @return 将要发送给服务端的信息
     */
    private BoostBodyRes trainGiHi(MixGBTrainData trainData) {
        BoostBodyRes res = new BoostBodyRes(MessageType.GiHi);
        localGH = new HashMap<>();
        /* no label, no need to submit local new gh */
        if (!trainData.hasLabel()) {
            return res;
        }
        /* 计算并更新本地的gi和hi信息, 获得 common id gi 和 hi */
        DistributedPaillierNative.signedByteArray[][] ghEnc = updateGradHess(trainData);
        res.setGh(ghEnc);
        res.setBoolFlag(trainData.hasAllCommonLabel);
        Set<Integer> labeledCommonId = new HashSet<>(commonIdSet);
        labeledCommonId.retainAll(trainData.getLocalLabeledId());
        int[] instId = labeledCommonId.stream().mapToInt(Integer::intValue).toArray();
        res.setInstId(instId);
        return res;
    }

    /**
     * 客户端接收服务端汇总后的 gi 和 hi 信息，更新本地的 gi 和 hi。
     *
     * @param req 服务端发送的json格式信息f
     * @return 将要发送给服务端的信息
     */
    private BoostBodyRes trainUpdateGiHi(BoostBodyReq req, MixGBTrainData trainData) {
        int[] instId = req.getInstId();
        if (instId != null) {
            DistributedPaillierNative.signedByteArray[][] commonIdGH = req.getGh();
            encGH = IntStream.range(0, instId.length).boxed().parallel()
                    .collect(Collectors.toMap(i -> instId[i], i -> commonIdGH[i]));
            Arrays.stream(instId).forEach(id -> localGH.remove(id));
        }
        /* 获取 master 计算的全局 metric 值 */
        Message metric = new JavaSerializer().deserialize(req.getMetric());
        assert metric instanceof MetricValue;
        metricValue = (MetricValue) metric;
        return startNode(trainData);
    }

    /**
     * 返回特征集合
     *
     * @return 对应特征的特征值
     */
    private BoostBodyRes trainHorizontalFeatureSet(MixGBTrainData trainData) {
        BoostBodyRes res = new BoostBodyRes(MessageType.FeaturesSet);
        if (trainData.getFeatureDim() == 0) {
            return res;
        }
        res.setFeaturesSet(trainData.getFeatureName());
        return res;
    }

    /**
     * 返回对应特征的特征值
     *
     * @param req 服务端发送的特征查询信息
     * @return 对应特征的特征值
     */
    private BoostBodyRes trainHorizontalFeatureValue(BoostBodyReq req, MixGBTrainData trainData) {
        BoostBodyRes res = new BoostBodyRes(MessageType.FeatureValue);
        if (trainData.getFeatureDim() == 0) {
            return res;
        }
        String[] targetFeatureNames = req.getFeaturesSet();
        double[] thresholds = Arrays.stream(targetFeatureNames).parallel()
                .mapToDouble(feaName -> trainData.getFeatureRandValue(feaName, curTreeNode.getInstanceIdSpaceSet()))
                .toArray();
        res.setFeaturesSet(targetFeatureNames);
        res.setFeatureValue(thresholds);
        if (encGH == null) {
            curTreeNode.setTmpInstanceIdSpaceSet(curTreeNode.getInstanceIdSpaceSet());
        } else {
            /* for common features, only use local ids in horizontal split phase, remove all common ids */
            Set<Integer> tempToUseIdSet = new HashSet<>(curTreeNode.getInstanceIdSpaceSet());
            tempToUseIdSet.removeAll(commonIdSet);
            curTreeNode.setTmpInstanceIdSpaceSet(tempToUseIdSet);
        }
        return res;
    }

    /**
     * 计算横向分裂方式的 best gain 和所对应的左结点空间
     *
     * @param req 服务端发送的json格式信息
     * @return 将要发送给服务端的信息
     */
    private BoostBodyRes trainHorizontalGain(BoostBodyReq req, MixGBTrainData trainData) {
        List<DistributedPaillierNative.signedByteArray[][]> featureSplitGH = req.getMyPartialDec();
        double[] featureGains = IntStream.range(0, featureSplitGH.size()).parallel().mapToDouble(i -> {
            double[] ghValues = pheKeys.decryptFinal(featureSplitGH.get(i), gkvHkvList[i], pheKeys.getSk());
            return computeFeatureGain(ghValues, mixParams.getLambda());
        }).toArray();
        int index = 0;
        double gain = -Double.MAX_VALUE;
        for (int i = 0; i < featureGains.length; i++) {
            if (featureGains[i] > gain) {
                gain = featureGains[i];
                index = i;
            }
        }

        String bestSplitFeature = featureNames[index];
        double bestSplitValue = featureThresholds[index];

        Set<Integer> leftIds = trainData.getLeftInstanceSet(curTreeNode.getTmpInstanceIdSpaceSet(), bestSplitFeature, bestSplitValue);

        BoostBodyRes res = new BoostBodyRes(MessageType.HorizontalGain);
        res.setInstId(leftIds.stream().mapToInt(Integer::intValue).toArray());
        res.setGain(gain);
        if (gain > curTreeNode.getTempHorizontalGain() && gain > mixParams.getGamma()) {
            curTreeNode.setTempHorizontalGain(gain);
            curTreeNode.setSplitFeatureName(bestSplitFeature);
            curTreeNode.setSplitThreshold(bestSplitValue);
        }
        return res;
    }

    private static double computeFeatureGain(double[] gh, double lambda) {
        if (gh == null || gh.length != 4) {
            logger.error("wrong node g h input!");
            return -Double.MAX_VALUE;
        }
        double gAll = gh[0] + gh[2];
        double hAll = gh[1] + gh[3];
        return 0.5 * (gh[0] * gh[0] / (gh[1] + lambda)
                + gh[2] * gh[2] / (gh[3] + lambda)
                - gAll * gAll / (hAll + lambda));
    }

    private static double computeFeatureGain(double gLeft, double hLeft, DoubleTuple2 nodeGH, double lambda) {
        double gRight = nodeGH.getFirst() - gLeft;
        double hRight = nodeGH.getSecond() - hLeft;
        return 0.5 * (gLeft * gLeft / (hLeft + lambda)
                + gRight * gRight / (hRight + lambda)
                - nodeGH.getFirst() * nodeGH.getFirst() / (nodeGH.getSecond() + lambda));
    }

    /**根据服务端发送的消息，对结点进行分裂，得到gi hi sum，发送给服务端。
     * 如果不存在对应的特征，返回空值
     * 计算横向分裂方式的 encG encH
     *
     * @param req 服务端发送的json格式信息
     * @return 将要发送给服务端的信息
     */
    private BoostBodyRes trainHorizontalEncGH(BoostBodyReq req, MixGBTrainData trainData) {
        /* 根据服务端发送的消息，对结点进行分裂，得到gi hi sum，发送给服务端。如果不存在对应的特征，返回空值 */
        featureThresholds = req.getValues();
        featureNames = req.getFeaturesSet();
        DistributedPaillierNative.signedByteArray[][] featureSplitGH = IntStream.range(0, featureNames.length).boxed().parallel()
                .map(index -> {
                    Set<Integer> leftIds = trainData.getLeftInstanceSet(curTreeNode.getTmpInstanceIdSpaceSet(), featureNames[index], featureThresholds[index]);
                    return computeMixGHSum(leftIds);
                }).toArray(DistributedPaillierNative.signedByteArray[][]::new);
        BoostBodyRes res = new BoostBodyRes(MessageType.HorizontalEnc);
        res.setGh(featureSplitGH);
        res.setStrToUse(pheKeys.getPk().toJson());
        res.setFeaturesSet(featureNames);
        return res;
    }

    private DistributedPaillierNative.signedByteArray[] computeMixGHSum(Set<Integer> leftIds) {
        Set<Integer> rightIds = new HashSet<>(curTreeNode.getTmpInstanceIdSpaceSet());
        rightIds.removeAll(leftIds);
        if (encGH == null || encGH.isEmpty()) {
            /* which means this client is the active party in this turn */
            return computeEncLocalGHSum(leftIds, rightIds);
        }
        /* otherwise, use encrypted g h from the active party for common IDs */

        /* local IDs in leftIdSet */
        Set<Integer> commonLeftIds = splitIdSetByCommon(leftIds, commonIdSet);
        /* local IDs in rightIdSet */
        Set<Integer> commonRightIds = splitIdSetByCommon(rightIds, commonIdSet);

        DistributedPaillierNative.signedByteArray[] partialGH = computeEncLocalGHSum(leftIds, rightIds);
        for (Integer leftId: commonLeftIds) {
            DistributedPaillierNative.signedByteArray[] ghi = encGH.get(leftId);
            partialGH[0] = pheKeys.add(ghi[0], partialGH[0], pheKeys.getPk());
            partialGH[1] = pheKeys.add(ghi[1], partialGH[1], pheKeys.getPk());
        }
        for (Integer rightId: commonRightIds) {
            DistributedPaillierNative.signedByteArray[] ghi = encGH.get(rightId);
            partialGH[2] = pheKeys.add(ghi[0], partialGH[2], pheKeys.getPk());
            partialGH[3] = pheKeys.add(ghi[1], partialGH[3], pheKeys.getPk());
        }
        return partialGH;
    }

    /** split idSet into local idSet and common idSet
     * common IDs in origin idSet would be removed during the process
     * @param idSet origin idSet
     * @param commonIdSet global common set
     * @return common IDs in origin idSet
     */
    private static Set<Integer> splitIdSetByCommon(Set<Integer> idSet, Set<Integer> commonIdSet) {
        Set<Integer> commonIds = new HashSet<>(idSet);
        commonIds.retainAll(commonIdSet);
        /* local IDs in idSet */
        idSet.removeAll(commonIds);
        return commonIds;
    }

    /**
     * @param leftIds
     * @param rightIds
     * @return
     */
    private DistributedPaillierNative.signedByteArray[] computeEncLocalGHSum(Set<Integer> leftIds, Set<Integer> rightIds) {
        double[] ghValues = computeLocalGHSum(leftIds, rightIds);
        return pheKeys.encryption(ghValues, pheKeys.getPk());
    }

    /**
     * @param leftIds
     * @param rightIds
     * @return
     */
    private double[] computeLocalGHSum(Set<Integer> leftIds, Set<Integer> rightIds) {
        double[] partialGH = new double[4];
        /* leftG */
        partialGH[0] = leftIds.parallelStream().map(id -> localGH.get(id).getFirst()).mapToDouble(Double::doubleValue).sum();
        /* leftH */
        partialGH[1] = leftIds.parallelStream().map(id -> localGH.get(id).getSecond()).mapToDouble(Double::doubleValue).sum();
        /* rightG */
        partialGH[2] = rightIds.parallelStream().map(id -> localGH.get(id).getFirst()).mapToDouble(Double::doubleValue).sum();
        /* rightH */
        partialGH[3] = rightIds.parallelStream().map(id -> localGH.get(id).getSecond()).mapToDouble(Double::doubleValue).sum();
        return partialGH;
    }

    /**根据服务端发送的消息，对 G H sum partial decrypt，发送给服务端
     *
     * @param req 服务端发送的json格式信息
     * @return 将要发送给服务端的信息
     */
    private BoostBodyRes horizontalGHDecryption(BoostBodyReq req) {
        BoostBodyRes res = new BoostBodyRes(MessageType.HorizontalDec);
        /* 存储 partialSum 用于后面计算 gain */
        gkvHkvList = req.getGh();
        DistributedPaillierNative.signedByteArray[][] decFeatureSplitGH = Arrays.stream(gkvHkvList).parallel()
                .map(gh -> pheKeys.decryptPartial(gh, pheKeys.getSk()))
                .toArray(DistributedPaillierNative.signedByteArray[][]::new);
        res.setGh(decFeatureSplitGH);
        if (encGH == null) {
            /* 标记 active client */
            res.setBoolFlag(true);
        }
        return res;
    }

    /**
     * 计算叶子结点的权重值所需 G H 的一部分
     * 本地没有 label，或不需要用本地 label, 或没有当前叶子上的样本的 label, 标记 saveWj = false
     * @param trainData 训练数据
     */
    private DistributedPaillierNative.signedByteArray[] calculateLocalGH(MixGBTrainData trainData) {
        DistributedPaillierNative.signedByteArray[] encWjGH = new DistributedPaillierNative.signedByteArray[2];
        final DistributedPaillierNative.signedByteArray zero = pheKeys.encryption(0.0, pheKeys.getPk());
        if (!trainData.hasLabel() || localGH.isEmpty()) {
            saveWj = false;
            Arrays.fill(encWjGH, zero);
            return encWjGH;
        }
        DoubleTuple2 ghSum = curTreeNode.getInstanceIdSpaceSet().parallelStream()
                .filter(id -> localGH.containsKey(id))
                .map(localGH::get).reduce(MixGBModel::doubleTupleSum).orElse(new DoubleTuple2());
        encWjGH[0] = pheKeys.encryption(ghSum.getFirst(), pheKeys.getPk());
        encWjGH[1] = pheKeys.encryption(ghSum.getSecond(), pheKeys.getPk());
        return encWjGH;
    }

    /**
     * 判断是否停止分裂
     *
     * @return 是否停止结点的分裂
     */
    private boolean stopSplit() {
        if (curTreeNode.getInstanceIdSpaceSet().size() < mixParams.getMinSampleSplit()) {
            return true;
        }
        return curTreeNode.getDepth() >= mixParams.getMaxDepth();
    }

    /**
     * 计算叶子结点的权重值
     */
    private static double calculateWj(double nodeG, double nodeH, double lambda, double eta) {
        double wj = -nodeG / (nodeH + lambda);
        return wj * eta;
    }

    /** 开始叶子结点权重计算流程
     * 将本地有 label 样本对该叶子的权重贡献部分 (G H 一部分) 发给服务端
     * @param trainData 训练数据
     * @return 该叶子上本地样本的 G H 贡献部分
     */
    private BoostBodyRes trainWjEnc(MixGBTrainData trainData) {
        /* 当前 node 上本地 id 的 score contribution */
        BoostBodyRes res = new BoostBodyRes(MessageType.WjEnc);
        DistributedPaillierNative.signedByteArray[][] encWjGH = new DistributedPaillierNative.signedByteArray[1][2];
        saveWj = true;
        encWjGH[0] = calculateLocalGH(trainData);
        res.setGh(encWjGH);
        res.setStrToUse(pheKeys.getPk().toJson());
        return res;
    }

    /** 加密数据的 partial 解密. 记录 partialSum 和 decPartialSum 用于 final decrypt
     *  本地没有 label，或不需要用本地 label, 或没有当前叶子上的样本的 label. 不记录且不进行 final decrypt
     * @param req 服务端发来的需要 partial 解密的内容
     * @return partial 解密结果
     */
    private BoostBodyRes decPartial(BoostBodyReq req) {
        BoostBodyRes res = new BoostBodyRes(MessageType.WjDec);
        DistributedPaillierNative.signedByteArray[][] devWjGH = new DistributedPaillierNative.signedByteArray[1][2];
        devWjGH[0] = pheKeys.decryptPartial(req.getGh()[0], pheKeys.getSk());
        res.setGh(devWjGH);
        if (saveWj) {
            partialSum = req.getGh()[0];
            decPartialSum = devWjGH[0];
            res.setBoolFlag(true);
        }
        return res;
    }

    /** 解密当前 node 上所有平台本地 id 的 score contribution
     * @param req 服务端转发的来自其他平台的 partial 解密结果
     * @param trainData 训练数据
     * @return 处理解密的 wj 结果
     */
    private BoostBodyRes finalDecWjSum(BoostBodyReq req, MixGBTrainData trainData) {
        DistributedPaillierNative.signedByteArray[][] othersDec = req.getGh();
        double[] finalScores = new double[2];
        if (saveWj) {
            finalScores = finalDecrypt(pheKeys, othersDec, decPartialSum, partialSum);
        }
        return controlActuallyDealWithWj(trainData, finalScores[0], finalScores[1], saveWj);
    }

    /** final Decrypt 工具
     * @param pheKeys 密钥
     * @param othersDec 其他平台的部分解密结果
     * @param decPartialSum 本平台的部分解密结果
     * @param partialSum 加密内容
     * @return final decrypt 结果
     */
    private static double[] finalDecrypt(HomoEncryptionUtil pheKeys,
                                         DistributedPaillierNative.signedByteArray[][] othersDec,
                                         DistributedPaillierNative.signedByteArray[] decPartialSum,
                                         DistributedPaillierNative.signedByteArray[] partialSum) {
        DistributedPaillierNative.signedByteArray[][] decodeList = new DistributedPaillierNative.signedByteArray[othersDec.length + 1][othersDec[0].length];
        decodeList[0] = decPartialSum;
        int cnt = 1;
        for (DistributedPaillierNative.signedByteArray[] imres : othersDec) {
            assert (decPartialSum.length == imres.length);
            decodeList[cnt++] = imres;
        }
        return pheKeys.decryptFinal(decodeList, partialSum, pheKeys.getSk());
    }

    /**
     * 计算叶子结点的权重，并将当前结点设置为叶子
     *
     * @param nodeG 叶子结点 G
     * @param nodeG 叶子结点 H
     * @return 将要发送给客户端的消息队列
     */
    private BoostBodyRes controlActuallyDealWithWj(MixGBTrainData trainData, double nodeG, double nodeH, boolean saveWj) {
        ++globalRecordId;
        curTreeNode.setAsLeaf(-Double.MAX_VALUE);
        curTreeNode.setSplitFeatureName("");
        curTreeNode.setSplitThreshold(0);
        curTreeNode.setRecordId(globalRecordId);
        if (saveWj) {
            /* update leave score */
            double wj = calculateWj(nodeG, nodeH, mixParams.getLambda(), mixParams.getEta());
            curTreeNode.setAsLeaf(wj);
            curRecordIdTreeNodeMap.put(globalRecordId, curTreeNode);
            if (trainData.hasLabel()) {
                /* update predictions */
                List<Integer> localLabeledId = trainData.getLocalLabeledId();
                curTreeNode.getInstanceIdSpaceSet().parallelStream()
                        .filter(localLabeledId::contains)
                        .forEach(instId -> predList[instId] += wj);
            }
        }
        /* 回溯结点, 找到下一个待处理的右结点 */
        curTreeNode = curTreeNode.backTrackingTreeNode();
        return afterLeaf(trainData);
    }

    /**
     * 判断是否停止整个森林的训练
     *
     * @return 是否停止训练
     */
    private boolean continueForest() {
        return curRootNodeList.size() < mixParams.getMaxTreeNum() || curTreeNode != null;
    }

    private BoostBodyRes afterLeaf(MixGBTrainData trainData) {
        /* 当前树构建未完成, 进入下一个结点的构建 */
        if (curTreeNode != null) {
            return startNode(trainData);
        }
        /* 当前树构建完成，进入下一棵树的构建 */
        if (continueForest()) {
            return trainTreeInit(trainData);
        }
        return trainFinalModel(trainData);
    }

    private BoostBodyRes trainFinalModel(MixGBTrainData trainData) {
        updateMetric(trainData);
        curTreeNode = null;
        // TODO: remove some node infos for security reasons
        return new BoostBodyRes(MessageType.EpochFinish);
    }

    /**
     * 获取特征值排序信息
     *
     * @param featureIndex 特征编号
     * @param trainData    数据
     * @return 特征值排序信息
     */
    private Tuple3<Integer, List<Map.Entry<Double, List<Integer>>>, Set<Integer>> getFeaSplitCandidate(int featureIndex, MixGBTrainData trainData) {
        Set<Integer> toUseInstIdList = new HashSet<>(curTreeNode.getTmpInstanceIdSpaceSet());
        /* missing-value IDs in toUseInstIdList */
        Set<Integer> missingValueInstIdSet = new HashSet<>(trainData.getFeatureMissValueInstIdMap()[featureIndex]);
        missingValueInstIdSet.retainAll(toUseInstIdList);
        /* split toUseInstIdList to two sets: toUseInstIdList and missingValueInstIdSet */
        toUseInstIdList.removeAll(missingValueInstIdSet);
        if (toUseInstIdList.isEmpty()) {
            return null;
        }
        Map<Integer, Double> idFeatureValueMap = trainData.getUsageIdFeatureValueByIndex(toUseInstIdList, featureIndex);
        /* merge IDs that have same values, group by values */
        Map<Double, List<Integer>> idGroupByValueMap = idFeatureValueMap.entrySet().parallelStream()
                .collect(Collectors.groupingBy(Map.Entry::getValue, Collectors.mapping(Map.Entry::getKey, Collectors.toList())));
        /* sort values */
        List<Map.Entry<Double, List<Integer>>> sortedIdGroupByValueMap = new ArrayList<>(idGroupByValueMap.entrySet());
        sortedIdGroupByValueMap.sort(Comparator.comparingDouble(Map.Entry::getKey));
        return new Tuple3<>(featureIndex, sortedIdGroupByValueMap, missingValueInstIdSet);
    }

    /**
     * 获取特征的 G 和 H 信息
     *
     * @param binSize                    桶大小限制 若为零 则目前分桶个数已经符合参数要求
     * @param sortedIndexGroupByValueMap 将要使用的实例 feature sorted 列表
     * @param missingValueInstIdSet      缺失该特征值的样本
     * @return 某特征的各个分桶阈值所对应的分桶 G H 列表。最后一项是缺失该特征值的样本的 G H
     */
    private Tuple2<double[], double[][]> getBinGH(
            int binSize,
            List<Map.Entry<Double, List<Integer>>> sortedIndexGroupByValueMap,
            Set<Integer> missingValueInstIdSet) {
        /* 计算相同取值的样本的梯度值和 */
        double[][] sortedSameValueSumGHList = sortedIndexGroupByValueMap.parallelStream()
                .map(e -> {
                    DoubleTuple2 ghSum = e.getValue().stream().map(id -> localGH.get(id)).reduce(MixGBModel::doubleTupleSum).orElse(new DoubleTuple2());
                    return new double[]{ghSum.getFirst(), ghSum.getSecond()};
                }).toArray(double[][]::new);

        double[] splitValue = sortedIndexGroupByValueMap.parallelStream().mapToDouble(Map.Entry::getKey).toArray();
        /* 获取特征的符合参数桶个数要求的分桶候选*/
        if (sortedIndexGroupByValueMap.size() > mixParams.getMaxBinNum()) {
            Tuple2<double[], double[][]> binGHSplit = getBinGHSplit(binSize, splitValue, sortedIndexGroupByValueMap, sortedSameValueSumGHList);
            sortedSameValueSumGHList = binGHSplit._2();
            splitValue = binGHSplit._1();
        }
        /* 处理缺失值情况 */
        /* TODO: local IDS */
        sortedSameValueSumGHList = addMissingGHSplit(missingValueInstIdSet, sortedSameValueSumGHList);
        return new Tuple2<>(splitValue, sortedSameValueSumGHList);
    }


    /** 计算特征分桶的 Gkv 和 Hkv. k v 分别标记特征序号及它的分裂阈值
     * 最后一项是缺失值的 G H
     * @param binSize 分桶大小
     * @param sortedIndexGroupByValueMap 已排好顺序的当前结点的某特征取值
     * @param missingValueInstIdSet 当前结点缺失某特征的样本集合
     * @return 序号为 k 的特征，各个分桶的 [Gkv, Hkv] 对的列表及所对应的分裂阈值 v. 最后一项是缺失值的 G H
     */
    private Tuple2<DistributedPaillierNative.signedByteArray[], double[]> getEncBinGH(
            int binSize,
            List<Map.Entry<Double, List<Integer>>> sortedIndexGroupByValueMap,
            Set<Integer> missingValueInstIdSet) {

        final DistributedPaillierNative.signedByteArray zero = pheKeys.encryption(0.0, pheKeys.getPk());
        /* 计算相同取值的样本的梯度值和 */
        List<Tuple2<Double, DistributedPaillierNative.signedByteArray[]>> sortedSameValueSumGHList =
                sortedIndexGroupByValueMap.parallelStream()
                .map(e -> {
                    DistributedPaillierNative.signedByteArray[] tempGHlValue = new DistributedPaillierNative.signedByteArray[2];
                    Arrays.fill(tempGHlValue, zero);
                    for (int index : e.getValue()) {
                        DistributedPaillierNative.signedByteArray[] idGH = encGH.get(index);
                        tempGHlValue[0] = pheKeys.add(idGH[0], tempGHlValue[0], pheKeys.getPk());
                        tempGHlValue[1] = pheKeys.add(idGH[1], tempGHlValue[1], pheKeys.getPk());
                    }
                    return new Tuple2<>(e.getKey(), tempGHlValue);
                }).collect(Collectors.toList());

        /* 获取特征的符合参数桶个数要求的分桶候选*/
        if (sortedIndexGroupByValueMap.size() > mixParams.getMaxBinNum()) {
            sortedSameValueSumGHList = getEncBinGHSplit(binSize, sortedIndexGroupByValueMap, sortedSameValueSumGHList);
        }
        /* 处理缺失值情况 */
        addEncMissingGHSplit(missingValueInstIdSet, sortedSameValueSumGHList);
        /* 拉平 g h pair 为一列, [gk0, hk0, gk1, hk1, gk2, hk2, gk3, hk3, ...] */
        /* 最后两项为缺失值的 G H */
        DistributedPaillierNative.signedByteArray[] sortedSameValueSumGHArray = sortedSameValueSumGHList.parallelStream()
                .flatMap(splitValueGH -> Arrays.stream(splitValueGH._2()))
                .toArray(DistributedPaillierNative.signedByteArray[]::new);
        /* 最后一项为0，缺失值 */
        double[] sortedSameValueSumGHSplitValue = sortedSameValueSumGHList.parallelStream().mapToDouble(Tuple2::_1).toArray();
        assert sortedSameValueSumGHArray.length == 2 * sortedSameValueSumGHSplitValue.length;
        return new Tuple2<>(sortedSameValueSumGHArray, sortedSameValueSumGHSplitValue);
    }


    /**
     * 获取特征的符合参数桶个数要求的分桶候选
     *
     * @param binSize                  桶大小限制 若为零 则目前分桶个数已经符合参数要求
     * @param sortedSameValueSumGHList 目前特征值的有序取值和相应的梯度分桶累加结果
     * @return 特征的符合参数桶个数要求的分桶候选
     */
    private List<Tuple2<Double, DistributedPaillierNative.signedByteArray[]>> getEncBinGHSplit(
            int binSize,
            List<Map.Entry<Double, List<Integer>>> sortedIndexGroupByValueMap,
            List<Tuple2<Double, DistributedPaillierNative.signedByteArray[]>> sortedSameValueSumGHList) {

        int uniqueSize = sortedIndexGroupByValueMap.size();
        List<Tuple2<Double, DistributedPaillierNative.signedByteArray[]>> newSortedSameValueSumGHList = new ArrayList<>();
        int index = 0;
        Tuple2<Double, DistributedPaillierNative.signedByteArray[]> ghTriple;
        while (index < uniqueSize) {
            int tmpBucketSize = sortedIndexGroupByValueMap.get(index).getValue().size();
            ghTriple = sortedSameValueSumGHList.get(index);
            DistributedPaillierNative.signedByteArray[] tempGHlValue = new DistributedPaillierNative.signedByteArray[2];
            tempGHlValue[0] = ghTriple._2()[0];
            tempGHlValue[1] = ghTriple._2()[1];
            index++;
            while (tmpBucketSize < binSize && index < uniqueSize) {
                tmpBucketSize += sortedIndexGroupByValueMap.get(index).getValue().size();
                ghTriple = sortedSameValueSumGHList.get(index);
                tempGHlValue[0] = pheKeys.add(ghTriple._2()[0], tempGHlValue[0], pheKeys.getPk());
                tempGHlValue[1] = pheKeys.add(ghTriple._2()[1], tempGHlValue[1], pheKeys.getPk());
                index++;
            }
            newSortedSameValueSumGHList.add(new Tuple2<>(sortedSameValueSumGHList.get(index - 1)._1(), tempGHlValue));
        }
        return newSortedSameValueSumGHList;
    }

    /**
     * 获取特征的符合参数桶个数要求的分桶候选
     *
     * @param binSize                  桶大小限制 若为零 则目前分桶个数已经符合参数要求
     * @param splitValue               目前特征值的有序取值
     * @param sortedSameValueSumGHList 目前特征值的有序取值相应的梯度分桶累加结果
     * @return 特征的符合参数桶个数要求的分桶候选
     */
    private Tuple2<double[], double[][]> getBinGHSplit(int binSize,
                                     double[] splitValue,
                                     List<Map.Entry<Double, List<Integer>>> sortedIndexGroupByValueMap,
                                     double[][] sortedSameValueSumGHList) {
        int uniqueSize = sortedIndexGroupByValueMap.size();
        List<double[]> newSortedSameValueSumGHList = new ArrayList<>();
        List<Double> newSplitValueList = new ArrayList<>();
        int index = 0;
        double[] ghTriple;
        while (index < uniqueSize) {
            int tmpBucketSize = sortedIndexGroupByValueMap.get(index).getValue().size();
            ghTriple = sortedSameValueSumGHList[index];
            double tempGlValue = ghTriple[0];
            double tempHlValue = ghTriple[1];
            index++;
            while (tmpBucketSize < binSize && index < uniqueSize) {
                tmpBucketSize += sortedIndexGroupByValueMap.get(index).getValue().size();
                ghTriple = sortedSameValueSumGHList[index];
                tempGlValue += ghTriple[0];
                tempHlValue += ghTriple[1];
                index++;
            }
            newSplitValueList.add(splitValue[index - 1]);
            newSortedSameValueSumGHList.add(new double[]{tempGlValue, tempHlValue});
        }
        double[] newSplitValue = newSplitValueList.stream().mapToDouble(Double::doubleValue).toArray();
        double[][] newSortedSameValueSumGH = newSortedSameValueSumGHList.toArray(new double[0][]);
        return new Tuple2<>(newSplitValue, newSortedSameValueSumGH);
    }

    /** 在分桶 G H 列表的最后，加上缺少特征值的样本的 G H
     * @param missingValueInstIdSet 缺少特征值的样本集合
     * @param sortedSameValueSumGHList 按特征排序的分桶 G H 列表
     * @return 加上缺少特征值的样本的 G H 的分桶候选 G H 列表
     */
    private void addEncMissingGHSplit(
            Set<Integer> missingValueInstIdSet,
            List<Tuple2<Double, DistributedPaillierNative.signedByteArray[]>> sortedSameValueSumGHList) {

        DistributedPaillierNative.signedByteArray[] missingValueGH = pheKeys.getAllZero(2);

        for (int index : missingValueInstIdSet) {
            DistributedPaillierNative.signedByteArray[] ghPair = encGH.get(index);
            missingValueGH[0] = pheKeys.add(ghPair[0], missingValueGH[0], pheKeys.getPk());
            missingValueGH[1] = pheKeys.add(ghPair[1], missingValueGH[1], pheKeys.getPk());
        }
        sortedSameValueSumGHList.add(new Tuple2<>(0.0, missingValueGH));
    }

    /** 在分桶 G H 列表的最后，加上缺少特征值的样本的 G H
     * @param missingValueInstIdSet 缺少特征值的样本集合
     * @param sortedSameValueSumGHList 按特征排序的分桶 G H 列表
     * @return 加上缺少特征值的样本的 G H 的分桶候选 G H 列表
     */
    private double[][] addMissingGHSplit(Set<Integer> missingValueInstIdSet, double[][] sortedSameValueSumGHList) {
        double[][] addMissingGHSplit = new double[sortedSameValueSumGHList.length + 1][2];
        System.arraycopy(sortedSameValueSumGHList, 0, addMissingGHSplit, 0, sortedSameValueSumGHList.length);
        if (missingValueInstIdSet.isEmpty()) {
            return addMissingGHSplit;
        }
        DoubleTuple2 missingValueGH = missingValueInstIdSet.parallelStream()
                .map(id -> localGH.get(id)).reduce(MixGBModel::doubleTupleSum).orElse(new DoubleTuple2(0.0, 0.0));
        addMissingGHSplit[sortedSameValueSumGHList.length][0] = missingValueGH.getFirst();
        addMissingGHSplit[sortedSameValueSumGHList.length][1] = missingValueGH.getSecond();
        return addMissingGHSplit;
    }

    /**
     * 获取随机特征集合
     *
     * @param trainData 训练数据
     * @return 随机产生将要使用的特征维度的集合
     */
    private List<Integer> getVerticalRandomFeatureSet(MixGBTrainData trainData) {
        int useFeatureNum = trainData.getFeatureDim();
        List<Integer> tempList = IntStream.range(0, useFeatureNum).boxed().collect(Collectors.toCollection(ArrayList::new));
        /* 纵向被动方的平台不计算共同特征，由主动方计算 */
        if (encGH != null) {
            tempList.removeAll(trainData.getCommonFea());
        }
        if (tempList.isEmpty()) {
            return new ArrayList<>();
        }
        useFeatureNum = Integer.min(tempList.size(), useFeatureNum);
        /* feature sampling number */
        if (useFeatureNum >= 3) {
            useFeatureNum = Integer.max(1, (int) (useFeatureNum * mixParams.getVerticalFeatureSampling()));
        }
        List<Integer> resultSet = new ArrayList<>();
        while (resultSet.size() < useFeatureNum) {
            /* 将其从列表中删除,从而实现不重复. */
            int j = new Random().nextInt(tempList.size());
            resultSet.add(tempList.get(j));
            tempList.remove(j);
        }
        return resultSet;
    }

    /**
     * 计算客户端中的 Gkv和 Hkv 的值
     *
     * @return 计算所得到的的 Gkv和 Hkv 信息
     */
    private DistributedPaillierNative.signedByteArray[][] getGkvHkvCandidate(
            MixGBTrainData trainData,
            int binSize,
            List<Tuple3<Integer, List<Map.Entry<Double, List<Integer>>>, Set<Integer>>> feaSplitCandidate) {
        /* split into bins. And compute bin G H for bin feature value*/
        List<Tuple2<DistributedPaillierNative.signedByteArray[], double[]>> boostGkvHkvBodies = feaSplitCandidate.parallelStream()
                .map(candidate -> getEncBinGH(binSize,
                        candidate._2().get(),
                        candidate._3().get()))
                .collect(Collectors.toList());
        /* 和 GKv Hkv 相对应的 feature 的序号列表，用于下一步骤 best gain 对应的 feature 查找 */
        int[] featureIndexList = feaSplitCandidate.parallelStream()
                .mapToInt(candidate -> candidate._1().get())
                .toArray();
        trainData.setFeatureIndexList(featureIndexList);
        /* 和 GKv Hkv 相对应的 feature 的各个 split value threshold 列表，用于下一步骤 best gain 对应的 feature threshold 查找 */
        double[][] featureThresholdList = boostGkvHkvBodies.parallelStream()
                .map(Tuple2::_2)
                .toArray(double[][]::new);
        trainData.setFeatureThresholdList(featureThresholdList);
        return boostGkvHkvBodies.parallelStream()
                .map(Tuple2::_1)
                .toArray(DistributedPaillierNative.signedByteArray[][]::new);
    }

    private void computeVerticalNodeGH(MixTreeNode node) {
        if (node.getTmpInstanceIdSpaceSet().isEmpty()) {
            return;
        }
        DoubleTuple2 sumGH = node.getTmpInstanceIdSpaceSet().parallelStream()
                .filter(id -> localGH.containsKey(id))
                .map(id -> localGH.get(id))
                .reduce(MixGBModel::doubleTupleSum).orElse(new DoubleTuple2());
        node.setNodeGH(sumGH);
    }

    /** 计算本地最佳 vertical split feature 并暂时记录在 curTreeNode 中
     * @param featureBinGHList 每个特征及其分桶 G H 列表。列表最后一项是缺失特征值的样本 G H
     */
    private void verticalLocalBestFeature(List<Tuple3<Integer, double[], double[][]>>  featureBinGHList) {
        Tuple2<Integer, Tuple2<double[], Boolean>> bestLocalFea = featureBinGHList.stream().map(featureIndexBinGH -> {
            double[] splitVale = featureIndexBinGH._2().orElse(new double[0]);
            double[][] featureBinGH = featureIndexBinGH._3().orElse(new double[0][]);
            Tuple2<double[], Boolean> featureBestGain = splitValueGains(splitVale, featureBinGH);
            return new Tuple2<>(featureIndexBinGH._1().orElse(0), featureBestGain);
        }).max(Comparator.comparingDouble(tuple -> tuple._2()._1()[0]))
                .orElse(new Tuple2<>(0, new Tuple2<>(new double[]{-Double.MAX_VALUE, -Double.MAX_VALUE}, false)));
        curTreeNode.setTempVerticalGain(bestLocalFea._2()._1()[0]);
        curTreeNode.setVerticalFeatureIndex(bestLocalFea._1());
        curTreeNode.setVerticalSplitThreshold(bestLocalFea._2()._1()[1]);
        curTreeNode.setMissingGoLeft(bestLocalFea._2()._2());
    }

    /** 计算某特征的各个分桶特征取值下，最佳分裂增益所对应的特征阈值等信息
     * @param splitValueArray splitVale
     * @param featureBinGH  某特征的各个分桶阈值所对应的分桶 G H 列表。最后一项是缺失该特征值的样本的 G H
     * @return 该特征的 (best gain, best split value, missingValueGoLeft)
     */
    private Tuple2<double[], Boolean> splitValueGains(double[] splitValueArray, double[][] featureBinGH) {
        assert featureBinGH.length > 2;
        /* 从最后一项取出缺失该特征值的样本的 G H */
        double missingValueG = featureBinGH[featureBinGH.length - 1][0];
        double missingValueH = featureBinGH[featureBinGH.length - 1][1];
        double accumulatedG = 0;
        double accumulatedH = 0;
        Tuple2<double[], Boolean> featureBestGain = new Tuple2<>(new double[]{-Double.MAX_VALUE, -Double.MAX_VALUE}, false);
        /* 最后一个 bin 的 threshold 不纳入计算，否则如选中，所有样本向左分，分裂无效 */
        for (int i = 0; i < featureBinGH.length - 2; i++) {
            double splitValue = splitValueArray[i];
            accumulatedG += featureBinGH[i][0];
            accumulatedH += featureBinGH[i][1];
            double missingGoRightGain = computeFeatureGain(accumulatedG, accumulatedH, curTreeNode.getNodeGH(), mixParams.getLambda());
            double missingGoLeftGain = computeFeatureGain(accumulatedG + missingValueG, accumulatedH + missingValueH, curTreeNode.getNodeGH(), mixParams.getLambda());
            if (missingGoLeftGain < featureBestGain._1()[0] && missingGoRightGain < featureBestGain._1()[0]) {
                continue;
            }
            if (missingGoLeftGain > missingGoRightGain) {
                featureBestGain = new Tuple2<>(new double[]{missingGoLeftGain, splitValue}, true);
            } else {
                featureBestGain = new Tuple2<>(new double[]{missingGoRightGain, splitValue}, false);
            }
        }
        return featureBestGain;
    }


    /**
     * 客户端计算Gkv和Hkv的值，返回给服务端；如果服务端发送的信息中，包含了[[gi]]和[[hi]]，则更新客户端的这些信息
     *
     * @return 将要发送给服务端的信息
     */
    private BoostBodyRes trainVerticalGkvHkv(BoostBodyReq req, MixGBTrainData trainData) {
        curTreeNode.setTempHorizontalGain(req.getGain());
        curTreeNode.setHoriInstanceIdSpaceSet(Arrays.stream(req.getInstId()).boxed().collect(Collectors.toSet()));
        BoostBodyRes res = new BoostBodyRes(MessageType.GkvHkv);
        /* 仅仅对共同ID的那部分数据进行处理 */
        Set<Integer> tempToUseIdSet = new HashSet<>(curTreeNode.getInstanceIdSpaceSet());
        tempToUseIdSet.retainAll(commonIdSet);
        if (tempToUseIdSet.size() <= mixParams.getMinSampleSplit()) {
            return res;
        }
        curTreeNode.setTmpInstanceIdSpaceSet(tempToUseIdSet);
        computeVerticalNodeGH(curTreeNode);

        /* 随机特征集合 */
        List<Integer> usefulFeatureSet = getVerticalRandomFeatureSet(trainData);
        /* sorted by feature values */
        List<Tuple3<Integer, List<Map.Entry<Double, List<Integer>>>, Set<Integer>>> feaSplitCandidate = usefulFeatureSet.parallelStream().map(index ->
                getFeaSplitCandidate(index, trainData)).filter(Objects::nonNull).collect(Collectors.toList());
        int binSize = Math.max(1, curTreeNode.getTmpInstanceIdSpaceSet().size() / mixParams.getMaxBinNum());
        if (encGH == null) {
            /* active party compute LocalBestFeatureGain and record it temporarily */
            List<Tuple3<Integer, double[], double[][]>> featureBinGHList = featureBinGHList(binSize, feaSplitCandidate);
            verticalLocalBestFeature(featureBinGHList);
            return res;
        }
        // TODO: only compute local features, except active party who computes all features including common features
        gkvHkvList = getGkvHkvCandidate(trainData, binSize, feaSplitCandidate);
        res.setGh(gkvHkvList);
        return res;
    }

    /** 为每个特征计算分桶 G H 列表。列表最后一项是缺失特征值的样本 G H
     * @param binSize 分桶大小
     * @param feaSplitCandidate 按特征排序后的样本
     */
    private List<Tuple3<Integer, double[], double[][]>> featureBinGHList(
            int binSize,
            List<Tuple3<Integer, List<Map.Entry<Double, List<Integer>>>, Set<Integer>>> feaSplitCandidate) {
        return feaSplitCandidate.parallelStream().map(feature -> {
            Tuple2<double[], double[][]> binGH = getBinGH(binSize, feature._2()
                    .orElse(new ArrayList<>()), feature._3().orElse(new HashSet<>()));
            if (binGH._2().length <= 2){
                /* 最后一个是缺失值 G H。那么没有或仅有一个分桶，则不参与计算 */
                return null;
            }
            return new Tuple3<>(feature._1().orElse(0), binGH._1(), binGH._2());
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * 将别的 client 的加密内容进行 partial decrypt
     *
     * @param req 服务端发送的json格式信息
     * @return 将要发送给服务端的信息
     */
    private BoostBodyRes verticalPartialDecryption(BoostBodyReq req) {
        BoostBodyRes res = new BoostBodyRes(MessageType.VerticalDec);
        /* 根据服务端发送的消息，对 G H sum partial decrypt，发送给服务端 */
        Map<String, DistributedPaillierNative.signedByteArray[][]> featureSplitGH = req.getClientPartialDec();
        Map<String, DistributedPaillierNative.signedByteArray[][]> decFeatureSplitGH = featureSplitGH.entrySet().parallelStream()
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> Arrays.stream(entry.getValue())
                                .parallel()
                                .map(gh -> pheKeys.decryptPartial(gh, pheKeys.getSk())).toArray(DistributedPaillierNative.signedByteArray[][]::new)
                ));
        res.setClientPartialDec(decFeatureSplitGH);
        return res;
    }

    /** 接收其他 client 对本平台 Gkv Hkv 进行 partial decrypt 的结果
     * 据此计算本平台各个特征的 locally vertical vest gain
     * @param req 其他 client 对本平台 Gkv Hkv 进行 partial decrypt 的结果
     * @return  locally vertical vest gain
     */
    private BoostBodyRes trainVerticalGVGain(BoostBodyReq req, MixGBTrainData trainData) {
        /* 获取别的 client 为本方 partial decrypt 的结果 */
        List<DistributedPaillierNative.signedByteArray[][]> myGHList = req.getMyPartialDec();
        if (myGHList != null) {
            int[] feaIndex = trainData.getFeatureIndexList();
            List<Tuple3<Integer, double[], double[][]>> feaDecGkvHkvList = IntStream.range(0, gkvHkvList.length).boxed().parallel().map(j -> {
                /* local dec for this feature */
                DistributedPaillierNative.signedByteArray[] decPartialGHkv = pheKeys.decryptPartial(gkvHkvList[j], pheKeys.getSk());
                DistributedPaillierNative.signedByteArray[][] otherDecPartialGHkv = myGHList.stream().map(decAllFea -> decAllFea[j]).toArray(DistributedPaillierNative.signedByteArray[][]::new);
                double[] gHkvLine = finalDecrypt(pheKeys, otherDecPartialGHkv, decPartialGHkv, gkvHkvList[j]);
                if (gHkvLine.length <= 4){
                    /* 最后一对是缺失值 G H。那么没有或仅有一个分桶，则不参与计算 */
                    return null;
                }
                double[][] featureBinGH = Tool.reshape(gHkvLine, gHkvLine.length / 2);
                double[] splitValue = trainData.getFeatureThresholdList()[j];
                return new Tuple3<>(feaIndex[j], splitValue, featureBinGH);
            }).filter(Objects::nonNull).collect(Collectors.toList());

            if (!feaDecGkvHkvList.isEmpty()) {
                double[][] gHkv = feaDecGkvHkvList.get(0)._3().orElse(new double[0][]);
                double nodeG = Arrays.stream(gHkv).mapToDouble(doubles -> doubles[0]).sum();
                double nodeH = Arrays.stream(gHkv).mapToDouble(doubles -> doubles[1]).sum();
                curTreeNode.setNodeGH(new DoubleTuple2(nodeG, nodeH));
            }
            verticalLocalBestFeature(feaDecGkvHkvList);
        }
        BoostBodyRes res = new BoostBodyRes(MessageType.KVGain);
        if (curTreeNode.getTempVerticalGain() > curTreeNode.getTempHorizontalGain() && curTreeNode.getTempVerticalGain() > mixParams.getGamma()) {
            res.setGain(curTreeNode.getTempVerticalGain());
        } else {
            res.setGain(-Double.MAX_VALUE);
        }
        return res;
    }

    /**
     * 客户端根据接收到的使用本平台特征纵向分裂的指令，计算得到当前结点若纵向分裂的左子树样本空间 IL，返回给服务端
     *
     * @param req 服务端发送的json格式信息
     * @return 将要发送给服务端的信息
     */
    private BoostBodyRes setVerticalSplit(BoostBodyReq req, MixGBTrainData trainData){
        double verticalGain = req.getGain();
        /* if vertical gain is less than horizontal gain, horizontal split */
        if (verticalGain <= mixParams.getGamma() || verticalGain <= curTreeNode.getTempHorizontalGain()) {
            return horizontalSplit(trainData);
        }
        /* 本平台的纵向候选未被选中，返回空值，等待服务端转发左子树样本空间 */
        if (!req.isBoolFlag()) {
            return new BoostBodyRes(MessageType.V_IL);
        }
        /* 计算左子树样本空间 */
        int featureIndex = curTreeNode.getVerticalFeatureIndex();
        String featureName = trainData.getFeatureName()[featureIndex];
        double splitValue = curTreeNode.getVerticalSplitThreshold();
        boolean missingGoLeft = curTreeNode.isMissingGoLeft();
        /* from common ids */

        int[] tempIl = trainData.getLeftInstanceForFeaSplit(curTreeNode.getTmpInstanceIdSpaceSet(),
                featureIndex, splitValue, missingGoLeft);
        if (tempIl.length == 0) {
            logger.error("vertical split, IdLeftSet is null! ");
        }
        curTreeNode.setSplitFeatureName(featureName);
        curTreeNode.setSplitFeatureType(1);
        curTreeNode.setSplitThreshold(splitValue);
        curTreeNode.setTmpInstanceIdSpaceSet(null);
        BoostBodyRes res = new BoostBodyRes(MessageType.V_IL);
        res.setInstId(tempIl);
        res.setBoolFlag(missingGoLeft);
        return res;
    }

    /** 更新 Gi和 Hi之后，或结点分裂后，处理下一个结点
     * @param trainData 本地数据
     * @return 将要发送给服务端的消息
     */
    private BoostBodyRes startNode(MixGBTrainData trainData) {
        if (curTreeNode == null) {
            logger.error("curNode is null, restart training process...");
            return trainEpochInit(trainData);
        }
        if (stopSplit()) {
            /* 计算叶子结点信息，不进行分裂，告诉客户端已经完成结点处理操作 */
            return trainWjEnc(trainData);
        }
        /* 判断是否进行横向处理 */
        /* 如果纵向样本占绝大多数，对本地来说横纵向分裂结果是非常接近的。由 master 汇总决定是否进行横向 */
        return needHorizontalControl();
    }

    /**
     * 判断是否需要进行横向的分裂处理
     *
     * @return 是否进行横向处理
     */
    private BoostBodyRes needHorizontalControl() {
        BoostBodyRes res = new BoostBodyRes(MessageType.SkipHorizontal);
        if (curTreeNode.getSplitFeatureType() == 0) {
            res.setBoolFlag(true);
        }
        if (curTreeNode.getSplitFeatureType() == 1) {
            res.setBoolFlag(false);
        }
        /*
         小于 0，表示未决定横向或纵向分裂
         当前结点上的共同 id 数量
        */
        Set<Integer> commonId = new HashSet<>(curTreeNode.getInstanceIdSpaceSet());
        commonId.retainAll(commonIdSet);
        if (curTreeNode.getInstanceIdSpaceSet().size() * mixParams.getNeedVerticalSplitRatio() <= commonId.size()) {
            curTreeNode.setSplitFeatureType(1);
            res.setBoolFlag(false);
        } else {
            curTreeNode.setSplitFeatureType(0);
            res.setBoolFlag(true);
        }
        return res;
    }

    private BoostBodyRes horizontalSplit(MixGBTrainData trainData) {
        if (curTreeNode.getTempHorizontalGain() <= mixParams.getGamma()) {
            return trainWjEnc(trainData);
        }
        curTreeNode.setSplitFeatureType(0);
        /* horizontal split featureName and splitValue has been updated in trainHorizontalGain */
        return trainSplit(trainData, curTreeNode.getHoriInstanceIdSpaceSet());
    }

    /**
     * 进行横向纵向分裂的处理
     *
     * @param leftSet 分裂左子樹樣本
     * @return 将要发送给服务端的信息
     */
    private BoostBodyRes trainSplit(MixGBTrainData trainData, Set<Integer> leftSet) {
        /* 根据 IL，记录相关信息，进入下一个结点。 */
        ++globalRecordId;
        String featureName = curTreeNode.getSplitFeatureName();
        curTreeNode.setRecordId(globalRecordId);
        if (featureName != null && !featureName.isEmpty()
                && trainData.getAllFeatureNamesToIndex().containsKey(featureName)) {
            curRecordIdTreeNodeMap.put(globalRecordId, curTreeNode);
        }
        /* reduce memory cost */
        curTreeNode.setTmpInstanceIdSpaceSet(null);
        curTreeNode.setHoriInstanceIdSpaceSet(null);
        trainData.setFeatureThresholdList(null);
        trainData.setFeatureIndexList(null);
        MixTreeNode leftNode = new MixTreeNode(curTreeNode.getDepth() + 1);
        leftNode.setParent(curTreeNode);
        leftNode.setInstanceIdSpaceSet(leftSet);
        curTreeNode.setLeftChild(leftNode);
        curTreeNode = leftNode;
        return startNode(trainData);
    }

    /**
     * 进行纵向分裂的处理
     *
     * @param req 服务端发送的json格式信息
     * @return 将要发送给服务端的信息
     */
    private BoostBodyRes verticalSplit(BoostBodyReq req, MixGBTrainData trainData) {
        /* 根据服务端发送IL，记录相关信息，进入下一个结点。 */
        Set<Integer> leftSet = Arrays.stream(req.getInstId()).boxed().collect(Collectors.toSet());
        Set<Integer> myLeftSet = new HashSet<>(curTreeNode.getInstanceIdSpaceSet());
        myLeftSet.retainAll(leftSet);
        curTreeNode.setSplitFeatureType(1);
        return trainSplit(trainData, myLeftSet);
    }

    /**
     * 训练完毕后的，训练模型的处理和保存
     *
     * @param req 服务端发送的json格式信息
     * @return 将要发送给服务端的信息
     */
    private BoostBodyRes trainFinalModel(BoostBodyReq req, MixGBTrainData trainData) {
        /* 最终的模型 */
        curTreeNode = null;
//        int[] saveNodes = req.getSaveNodes() == null ? new int[0] : req.getSaveNodes();
//        List<Integer> saveNodesList = Arrays.stream(saveNodes).boxed().collect(Collectors.toList());
//
//        int[] deleteNodes = req.getDeleteNodes();
//        Arrays.stream(deleteNodes).filter(x -> !saveNodesList.contains(x)).forEach(x -> curRecordIdTreeNodeMap.remove(x));
//
//        List<Integer> myLeaves = curRecordIdTreeNodeMap.entrySet().stream()
//                .filter(x -> x.getValue().isLeaf() && x.getValue().getParent() != null && x.getValue().getParent().getRecordId() <= 0)
//                .map(Map.Entry::getKey)
//                .collect(Collectors.toList());
//        myLeaves.stream().filter(x -> !saveNodesList.contains(x)).forEach(x -> curRecordIdTreeNodeMap.remove(x));
//
        return new BoostBodyRes(MessageType.MetricValue);
    }

    /**
     * @param uidList            infer uid
     * @param inferenceCacheFile infer data
     * @param others             自定义参数
     * @return init BoostBodyRes containing existing IDs
     */
    @Override
    public Message inferenceInit(String[] uidList, String[][] inferenceCacheFile, Map<String, Object> others) {
        if (others == null) {
            throw new AssertionError("inferenceInit get null others map");
        }
        InferenceInitRes res = (InferenceInitRes) InferenceFilter.filter(uidList, inferenceCacheFile);
        inferUid = Arrays.asList(uidList.clone());
        MixGBSerializer mixGBSerializer = new MixGBSerializer();
        startNodes = mixGBSerializer.shareStartNodes(curRecordIdTreeNodeMap);
        secureMode = (boolean) others.get("secureMode");
        if (secureMode) {
            int numP = (int) others.get("numP");
            int encBits = (int) others.get("ENC_BITS");
            pheKeys = new HomoEncryptionUtil(numP, encBits, false);
            thisPartyID =  (Integer) others.get("thisPartyID") ;
            DistributedPaillier.DistPaillierPubkey pubkey = new DistributedPaillier.DistPaillierPubkey();
            DistributedPaillier.DistPaillierPrivkey privkey = new DistributedPaillier.DistPaillierPrivkey();
            pubkey.parseJson((String)others.get( "pubKeyStr" ));
            privkey.parseJson((String)others.get("privKeyStr"));
            this.pheKeys.setPk(pubkey);
            this.pheKeys.setSk(privkey);
            this.pheKeys.getSk().setRank(thisPartyID);
        }
        return res;
    }

    private Message inferenceFilterOutUids(InferenceInitRes message, MixGBInferenceData data) {
        inferScoreMap = new HashMap<>();
        double rootLeafSum = curRecordIdTreeNodeMap.values().parallelStream()
                .filter(mixTreeNode -> mixTreeNode.getDepth() == 1)
                .filter(MixTreeNode::isLeaf).mapToDouble(MixTreeNode::getLeafScore).sum();
        Set<Integer> removeIds = Arrays.stream(message.getUid()).boxed().collect(Collectors.toSet());
        IntStream.range(0, inferUid.size())
                .filter(i -> !removeIds.contains(i))
                .forEach(i -> inferScoreMap.put(inferUid.get(i), rootLeafSum));

        List<BoostInferQueryResBody> bodies = curRecordIdTreeNodeMap.values().parallelStream()
                .filter(mixTreeNode -> mixTreeNode.getDepth() == 1)
                .filter(mixTreeNode -> !mixTreeNode.isLeaf())
                .flatMap(mixTreeNode -> inferLevelTravel(mixTreeNode, new HashSet<>(inferScoreMap.keySet()), data).stream())
                .collect(Collectors.toList());
        BoostInferQueryResBody[] bodiesArray = updateInferScoreMap(bodies);
        return new BoostInferQueryRes(bodiesArray, startNodes);
    }

    private BoostInferQueryResBody[] updateInferScoreMap(List<BoostInferQueryResBody> bodies) {
        List<BoostInferQueryResBody> leafBodies = bodies.parallelStream()
                .filter(body -> body.getRecordId() == -1)
                .collect(Collectors.toList());
        bodies.removeAll(leafBodies);
        leafBodies.forEach(body -> {
            double score = body.getValue();
            Arrays.stream(body.getInstanceId())
                    .forEach(id -> inferScoreMap.replace(id, inferScoreMap.get(id) + score));
        });

        return bodies.toArray(new BoostInferQueryResBody[0]);
    }

    /**
     * 按层级推理，每次尽量推理到本平台能遍历的最深层。以此提高推理速度。
     *
     * @param queryNode 当前进入的根结点
     * @param queryIds  需要推理的 id
     * @param data      推理数据
     * @return 推理中间结果
     * 本平台能到达叶结点: 返回叶结点权重
     * 本平台不能到达叶结点: 返回无法继续的结点编号、向左或向右的指示，由 master 向其他平台 query 下一层结点走向
     */
    private List<BoostInferQueryResBody> inferLevelTravel(MixTreeNode queryNode, Set<String> queryIds, MixGBInferenceData data) {
        List<BoostInferQueryResBody> resBodies = new ArrayList<>();
        if (queryNode.isLeaf()) {
            resBodies.add(new BoostInferQueryResBody(queryIds.toArray(new String[0]), -1,
                    queryNode.getLeafScore()));
            return resBodies;
        }
        Queue<Tuple2<MixTreeNode, Set<String>>> levelNodes = new LinkedList<>();
        levelNodes.offer(new Tuple2<>(queryNode, queryIds));
        while (!levelNodes.isEmpty()) {
            Tuple2<MixTreeNode, Set<String>> tuple2 = levelNodes.poll();
            assert tuple2 != null;
            MixTreeNode node = tuple2._1();
            Set<String> leftSet = data.getLeftInstance(tuple2._2(), node.getSplitFeatureName(), node.getSplitThreshold());

            if (!leftSet.isEmpty()) {
                /* left set: if the left node is on other clients ,send back to master */
                BoostInferQueryResBody leftPath = inferNodePath(leftSet, node, 0);
                if (leftPath != null) {
                    resBodies.add(leftPath);
                } else {
                    /* else: continue process the left node */
                    levelNodes.offer(new Tuple2<>(node.getLeftChild(), leftSet));
                }
            }
            /* if left set size < root set size, then process the right set */
            if (leftSet.size() < tuple2._2().size()) {
                tuple2._2().removeAll(leftSet);
                BoostInferQueryResBody rightPath = inferNodePath(tuple2._2(), node, 1);
                /* right set: if the right node is on other clients ,send back to master */
                if (rightPath != null) {
                    resBodies.add(rightPath);
                } else {
                    /* else: continue process the right node */
                    levelNodes.offer(new Tuple2<>(node.getRightChild(), tuple2._2()));
                }
            }
        }
        return resBodies;
    }

    /**
     * 进行推理的总体控制
     *
     * @param phase    阶段
     * @param jsonData 中间数据
     * @param inferenceData     推理的数据集
     * @return 将要发送给服务端的信息
     */
    @Override
    public Message inference(int phase, Message jsonData, InferenceData inferenceData) {
        if (!(inferenceData instanceof  MixGBInferenceData)) {
            throw new AssertionError("wrong type inferenceData");
        }
        MixGBInferenceData data = (MixGBInferenceData) inferenceData;
        /* filter out wrong uids, start inference */
        if (jsonData instanceof InferenceInitRes) {
            return inferenceFilterOutUids((InferenceInitRes) jsonData, data);
        }
        /* node inference processes*/
        if (jsonData instanceof BoostInferQueryReq) {
            BoostInferQueryReq req = (BoostInferQueryReq) jsonData;
            BoostInferQueryReqBody[] reqBodies = req.getBodies();
            List<BoostInferQueryResBody>  bodies = Arrays.stream(reqBodies).parallel()
                    .filter(reqBody -> curRecordIdTreeNodeMap.containsKey(reqBody.getRecordId()))
                    .flatMap(reqBody -> {
                        MixTreeNode queryNode = curRecordIdTreeNodeMap.get(reqBody.getRecordId());
                        Set<String> queryIds = Arrays.stream(reqBody.getInstanceId()).collect(Collectors.toSet());
                        return inferLevelTravel(queryNode, queryIds, data).stream();
                    }).collect(Collectors.toList());
            BoostInferQueryResBody[] bodiesArray = updateInferScoreMap(bodies);
            return new BoostInferQueryRes(bodiesArray, startNodes);
        }
        if (jsonData instanceof BoostInferScoreReq) {
            return inferScores();
        }
        if (jsonData instanceof CypherMessage) {
            return decPaillierPartialScores((CypherMessage) jsonData);
        }
        if (jsonData instanceof CypherMessageList) {
            return decPaillierFinalScores((CypherMessageList) jsonData);
        }
        return null;
    }

    private Message decPaillierPartialScores(CypherMessage boostBodyRes) {
        partialSum = boostBodyRes.getBody();
        decPartialSum = pheKeys.decryptPartial(partialSum, pheKeys.getSk());
        List<DistributedPaillierNative.signedByteArray[]> diffResultType = new ArrayList<>();
        diffResultType.add(decPartialSum);
        return new BoostInferDecRes(diffResultType);
    }

    private Message decPaillierFinalScores(CypherMessageList boostBodyRes) {
        DistributedPaillierNative.signedByteArray[][] othersDec = boostBodyRes.getBody().toArray(new DistributedPaillierNative.signedByteArray[0][]);
        double[] finalScores = finalDecrypt(pheKeys, othersDec, decPartialSum, partialSum);
        return new BoostInferScoreRes(finalScores, true);
    }

    private Message inferScores() {
        double[] pred = inferUid.parallelStream()
                .map(id -> inferScoreMap.getOrDefault(id, Double.NaN))
                .mapToDouble(Double::doubleValue).toArray();
        if (secureMode) {
            DistributedPaillierNative.signedByteArray[] predEnc = Arrays.stream(pred).parallel()
                    .mapToObj(x -> pheKeys.encryption(x, pheKeys.getPk()))
                    .toArray(DistributedPaillierNative.signedByteArray[]::new);
            String pkStr = pheKeys.getPk().toJson();
            return new BoostInferEncRes(predEnc, pkStr);
        }

        for (int i = 0; i < pred.length; i++) {
            if (!inferScoreMap.containsKey(inferUid.get(i))) {
                pred[i] = Double.NaN;
            }
        }
        return new BoostInferScoreRes(pred, false);
    }

    /**
     * @param ids      id 集合
     * @param node  当前结点（一定在本地）
     * @param left     结果是否向左
     * @return 本平台能到达叶结点: 返回叶结点权重
     * 当前结点为空或本地不存在，本平台不能到达叶结点: 返回无法继续的结点编号、向左或向右的指示，由 master 向其他平台 query 下一层结点走向
     */
    private BoostInferQueryResBody inferNodePath(Set<String> ids, MixTreeNode node, int left) {
        MixTreeNode childNode;
        if (left == 0) {
            assert node.getLeftChild() != null;
            childNode = node.getLeftChild();
        } else {
            assert node.getRightChild() != null;
            childNode = node.getRightChild();
        }
        if (!curRecordIdTreeNodeMap.containsKey(childNode.getRecordId())) {
            return new BoostInferQueryResBody(ids.toArray(new String[0]), childNode.getRecordId());
        }
        if (childNode.isLeaf()) {
            return new BoostInferQueryResBody(ids.toArray(new String[0]), -1, childNode.getLeafScore());
        }
        /* else: continue process the left node */
        return null;
    }

    /**
     * @return 模型序列化
     */
    @Override
    public String serialize() {
        /*
         must not be empty, because in fedlearn-client   ModelDao.java
         slim() won't deal with empty String
        */
        MixGBSerializer mixGBSerializer = new MixGBSerializer(loss, mixParams);
        return mixGBSerializer.saveMixGBModel(curRecordIdTreeNodeMap);
    }

    /**
     * @param content 模型反序列化
     */
    @Override
    public void deserialize(String content) {
        MixGBSerializer mixGBSerializer = new MixGBSerializer(content);
        this.loss = mixGBSerializer.getLoss();
        int[] recordId = mixGBSerializer.getRecordId();
        List<MixTreeNode> treeNodes = mixGBSerializer.getNodeList();
        this.curRecordIdTreeNodeMap = IntStream.range(0, recordId.length).boxed().collect(Collectors.toMap(i -> recordId[i], treeNodes::get));
    }

    /**
     * @return model type
     */
    @Override
    public AlgorithmType getModelType() {
        return AlgorithmType.MixGBoost;
    }

    /**
     * @param objective mix gb parameter objective
     * @return Loss
     */
    private static Loss getLoss(ObjectiveType objective, int numClass) {
        switch (objective) {
            case regLogistic:
            case binaryLogistic:
                return new LogisticLoss();
            case regSquare:
                return new SquareLoss();
            case multiSoftmax:
            case multiSoftProb:
                return new crossEntropy(numClass);
            default:
                throw new NotImplementedException();
        }
    }

    /**
     * 在新建一棵树之前，更新当前的 metric
     *
     * @param trainData 训练数据
     */
    private void updateMetric(MixGBTrainData trainData) {
        if (trainData.getLabel() == null || trainData.getLabel().length == 0 ||
                localGH == null || localGH.isEmpty() || predList == null) {
            metricValue = null;
            return;
        }
        /* 当处在第一棵树训练过程中 */
        if (metricValue == null) {
            return;
        }
        /* 当已经建过一棵树之后，在每棵树训练之初更新 partial metric */
        MetricType[] evalMetric = mixParams.fetchMetric();
        double[] allWeight = new double[localGH.size()];
        double[] partOfPred = localGH.keySet().stream().parallel()
                .mapToDouble(id -> predList[id]).toArray();
        double[] partOfLabels = localGH.keySet().stream().parallel()
                .mapToDouble(id -> trainData.getLabel()[id]).toArray();
        Arrays.fill(allWeight, 1.0);
        Map<MetricType, Double> currentMetric;
        if (ObjectiveType.countPoisson == mixParams.getObjective()) {
            currentMetric = Metric.calculateLocalMetricSumPart(evalMetric, loss.expTransform(partOfPred), loss.expTransform(partOfLabels), allWeight);
        } else {
            currentMetric = Metric.calculateLocalMetricSumPart(evalMetric, loss.transform(partOfPred), partOfLabels, allWeight);
        }
        Map<MetricType, List<Pair<Integer, Double>>> beforeMetrics = metricValue.getMetrics();
        int computeBaseSize = localGH.size();
        currentMetric.forEach((key, value) -> {
            List<Pair<Integer, Double>> previousRound = beforeMetrics.get(key);
            previousRound.add(new Pair<>(computeBaseSize, value));
        });
    }
}
