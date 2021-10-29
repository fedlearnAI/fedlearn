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
import com.jdt.fedlearn.core.encryption.common.PrivateKey;
import com.jdt.fedlearn.core.encryption.common.PublicKey;
import com.jdt.fedlearn.core.encryption.javallier.JavallierTool;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.base.EmptyMessage;
import com.jdt.fedlearn.core.entity.base.Int2dArray;
import com.jdt.fedlearn.core.entity.base.StringArray;
import com.jdt.fedlearn.core.entity.boost.*;
import com.jdt.fedlearn.core.entity.common.MetricValue;
import com.jdt.fedlearn.core.entity.common.TrainInit;
import com.jdt.fedlearn.core.entity.distributed.InitResult;
import com.jdt.fedlearn.core.entity.distributed.SplitResult;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.entity.feature.SingleFeature;
import com.jdt.fedlearn.core.exception.NotImplementedException;
import com.jdt.fedlearn.core.exception.NotMatchException;
import com.jdt.fedlearn.core.loader.boost.BoostTrainData;
import com.jdt.fedlearn.core.loader.common.CommonInferenceData;
import com.jdt.fedlearn.core.loader.common.InferenceData;
import com.jdt.fedlearn.core.loader.common.TrainData;
import com.jdt.fedlearn.core.math.MathExt;
import com.jdt.fedlearn.core.metrics.Metric;
import com.jdt.fedlearn.core.model.common.loss.LogisticLoss;
import com.jdt.fedlearn.core.model.common.loss.Loss;
import com.jdt.fedlearn.core.model.common.loss.SquareLoss;
import com.jdt.fedlearn.core.model.common.loss.crossEntropy;
import com.jdt.fedlearn.core.model.common.tree.Tree;
import com.jdt.fedlearn.core.model.common.tree.TreeNode;
import com.jdt.fedlearn.core.model.serialize.FgbModelSerializer;
import com.jdt.fedlearn.core.parameter.FgbParameter;
import com.jdt.fedlearn.core.parameter.HyperParameter;
import com.jdt.fedlearn.core.preprocess.InferenceFilter;
import com.jdt.fedlearn.core.type.*;
import com.jdt.fedlearn.core.type.data.DoubleTuple2;
import com.jdt.fedlearn.core.type.data.Pair;
import com.jdt.fedlearn.core.type.data.StringTuple2;
import com.jdt.fedlearn.core.type.data.Tuple2;
import com.jdt.fedlearn.core.util.Tool;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

/**
 * TODO:items(sortedFeatureMap currentNode passiveQueryTable trees...)
 * should save as map(trainID, item) for train parallel
 */
public class DistributedFederatedGBModel implements Model, Serializable {
    private static final Logger logger = LoggerFactory.getLogger(DistributedFederatedGBModel.class);

    private int depth = 0;
    //four col, id,val,grad,hess
    private Map<Integer, List<Bucket>> sortedFeatureMap = new ConcurrentHashMap<>();
    private TreeNode currentNode;
    private List<QueryEntry> passiveQueryTable = new ArrayList<>();
    private List<Tree> trees = new ArrayList<>();
    private double eta;
    private double firstRoundPred;
    private Loss loss;
    private Queue<TreeNode> newTreeNodes = new LinkedList<>();
    private FgbParameter parameter;
    //private KeyPair keyPair;
    private String privateKeyString;
    private String publicKeyString;
    private MetricValue metricValue;
    private int numClassRound = 0;
    public boolean hasLabel = false;
    public int datasetSize;
    public double[] label;

    public TreeNode[] correspondingTreeNode; // 记录每一个datapoint对应的Treenode
    public double[][] pred;
    // new double[parameter.getNumClass()][datasetSize]
    public double[][] grad;
    public double[][] hess;
    private List<String> encryptedG;
    private List<String> encryptedH;
    private String[] testId;

    private List<Double> multiClassUniqueLabelList = new ArrayList<>();

    //phase2 缓存
//    private Map<Integer, Tuple2<Ciphertext, Ciphertext>> ghMap2;
    private Map<Integer, Tuple2<String, String>> ghMap2String;

    //用于计算全局敏感度
    public double maxg = 1;

    public int contributeFea = 0;
    // 是否采用分布式
    private boolean isDistributed = false;
    // 特征id
//    private int featureId;
    private int modelId;
    private int workerNum;
    private List<Integer> featureindexs;

    public DistributedFederatedGBModel() {
    }

    public DistributedFederatedGBModel(List<Tree> trees, Loss loss, double firstRoundPredict, double eta, List<QueryEntry> passiveQueryTable, List<Double> multiClassUniqueLabelList) {
        this.trees = trees;
        this.loss = loss;
        this.firstRoundPred = firstRoundPredict;
        this.eta = eta;
        this.passiveQueryTable = passiveQueryTable;
        this.multiClassUniqueLabelList = multiClassUniqueLabelList;
//        this.contributeFea = contributeFea;
    }

    public DistributedFederatedGBModel(FgbModelSerializer serializer) {
        this.trees = serializer.getTrees();
        this.loss = serializer.getLoss();
        this.firstRoundPred = serializer.getFirstRoundPred();
        this.eta = serializer.getEta();
        this.passiveQueryTable = serializer.getPassiveQueryTable();
        this.multiClassUniqueLabelList = serializer.getMultiClassUniqueLabelList();
//        this.contributeFea = contributeFea;
    }

    public void multiLabelTransform() {
        this.multiClassUniqueLabelList = Arrays.stream(label).distinct().boxed().collect(Collectors.toList());
        this.label = Arrays.stream(label).map(l -> multiClassUniqueLabelList.indexOf(l)).toArray();
        int missingLabelNum = parameter.getNumClass() - multiClassUniqueLabelList.size();
        double startUniqueValue = multiClassUniqueLabelList.stream().max(Double::compareTo).get() + 1;
        for (int i = 0; i < missingLabelNum; i++) {
            multiClassUniqueLabelList.add(startUniqueValue + i);
        }
    }

    /**
     * 初始化模型，初始化trainId, label, datasetSize, pred, grad, hess, correspondingTreeNode, eta
     * 如果是主动方，则同时初始化privateKey, loss, firstRoundPred
     */
    public BoostTrainData trainInit(String[][] rawData, String[] uids, int[] testIndex, HyperParameter hyperParameter, Features features, Map<String, Object> others) {
        if ((others.containsKey("isDistributed")) && (others.get("isDistributed").equals("true"))) {
            this.isDistributed = true;
//            this.featureId = (int) (others.get("featureId"));
            this.workerNum = (int) (others.get("workerNum"));
            this.modelId = (int) (others.get("modelId"));
            this.featureindexs = (List<Integer>) others.get("featureindexs");
        }
        Tuple2<String[], String[]> trainTestUId = Tool.splitUid(uids, testIndex);
        String[] trainUids = trainTestUId._1();
        testId = trainTestUId._2();
        BoostTrainData trainData = new BoostTrainData(rawData, trainUids, features, new ArrayList<>());

//        newTreeNodes = new LinkedList<>();
        //TODO 修改指针
        parameter = (FgbParameter) hyperParameter;
        //初始化预测值和gradient hessian
        logger.info("actual received features:" + features.toString());
        logger.info("client data dim:" + trainData.getDatasetSize() + "," + trainData.getFeatureDim());
        // 强行置 1 . 防止用户在前端输入其他数据
        // TODO：后续可以改为 parameter 读入时进行判断
        if (!ObjectiveType.multiSoftmax.equals(parameter.getObjective()) && !ObjectiveType.multiSoftProb.equals(parameter.getObjective())) {
            parameter.setNumClass(1);
        }
        this.label = trainData.getLabel();
        this.datasetSize = trainData.getDatasetSize();
        this.pred = new double[parameter.getNumClass()][datasetSize];
        this.grad = new double[parameter.getNumClass()][datasetSize];
        this.hess = new double[parameter.getNumClass()][datasetSize];
        this.correspondingTreeNode = new TreeNode[datasetSize];
        EncryptionTool encryptionTool = getEncryptionTool();
        //有label 的客户端生成公私钥对
        if (trainData.hasLabel) {
            double initSumLoss = parameter.isMaximize() ? (-Double.MAX_VALUE) : Double.MAX_VALUE;
            List<Pair<Integer, Double>> tmpRoundMetric = new ArrayList<>();
            tmpRoundMetric.add(new Pair<>(0, initSumLoss));
            Map<MetricType, List<Pair<Integer, Double>>> metricMap = Arrays.stream(parameter.getEvalMetric())
                    .collect(Collectors.toMap(metric -> metric, metric -> new ArrayList<>(tmpRoundMetric)));
            metricValue = new MetricValue(metricMap);
            this.hasLabel = true;
            PrivateKey privateKey = encryptionTool.keyGenerate(parameter.getBitLength().getBitLengthType(), 64);
            this.privateKeyString = privateKey.serialize();
            if (ObjectiveType.regLogistic.equals(parameter.getObjective())) {
                this.loss = new LogisticLoss();
//                this.firstRoundPred = parameter.getFirstRoundPred();
                this.firstRoundPred = firstRoundPredict();
            } else if (ObjectiveType.regSquare.equals(parameter.getObjective())) {
                this.loss = new SquareLoss();
//                this.firstRoundPred = parameter.getFirstRoundPred();
//                this.firstRoundPred = MathExt.average(this.label);
                this.firstRoundPred = firstRoundPredict();
            } else if (ObjectiveType.countPoisson.equals(parameter.getObjective())) {
                if (Arrays.stream(this.label).filter(m -> m <= 0).findAny().isPresent()) {
                    throw new UnsupportedOperationException("There exist zero or negative labels for objective count:poisson!!!");
                } else {
//                    this.firstRoundPred = Math.log(MathExt.average(this.label));
                    this.firstRoundPred = firstRoundPredict();
                }
                this.loss = new SquareLoss();
                this.label = loss.logTransform(label);
            } else if (ObjectiveType.binaryLogistic.equals(parameter.getObjective())) {
                this.loss = new LogisticLoss();
//                this.firstRoundPred = parameter.getFirstRoundPred();
                this.firstRoundPred = firstRoundPredict();
            } else if (ObjectiveType.multiSoftmax.equals(parameter.getObjective())) {
                multiLabelTransform();
                this.loss = new crossEntropy(parameter.getNumClass());
//                this.firstRoundPred = parameter.getFirstRoundPred();
                this.firstRoundPred = firstRoundPredict();
            } else if (ObjectiveType.multiSoftProb.equals(parameter.getObjective())) {
                multiLabelTransform();
                this.loss = new crossEntropy(parameter.getNumClass());
//                this.firstRoundPred = parameter.getFirstRoundPred();
                this.firstRoundPred = firstRoundPredict();
            } else {
                throw new NotImplementedException();
            }
            initializePred(this.firstRoundPred);
            Tuple2 ghTuple = updateGradHess(loss, parameter.getScalePosWeight(), label, pred, datasetSize, parameter, numClassRound);
            grad = (double[][]) ghTuple._1();
            hess = (double[][]) ghTuple._2();

        }
        eta = parameter.getEta();

        return trainData;
    }

    // TODO 修改trainPhase1, trainPhase2, 将参数提出来

    /**
     * @param phase    训练阶段
     * @param jsonData
     * @param train
     * @return
     */
    public Message train(int phase, Message jsonData, TrainData train) {
        BoostTrainData trainData = (BoostTrainData) train;
        EncryptionTool encryptionTool = getEncryptionTool();
        switch (phase) {
            case 1:
                return trainPhase1(jsonData, trainData);
            case 2:
                return trainPhase2(jsonData, trainData, encryptionTool);
            case 3: {
                Tuple2<BoostP3Res, Double> req = trainPhase3(jsonData, trainData, currentNode, encryptionTool, privateKeyString,
                        parameter, grad, hess, numClassRound);
                if (trainData.hasLabel) {
                    currentNode.client = req._1().getClient();
                    currentNode.gain = req._2();
                    currentNode.splitFeature = Integer.parseInt(req._1().getFeature());
                }
                return req._1();
            }
            case 4: {
                contributeFea++;
                Tuple2<LeftTreeInfo, List<QueryEntry>> req = trainPhase4(jsonData, sortedFeatureMap, passiveQueryTable);
                passiveQueryTable = req._2();
                if (trainData.hasLabel) {
                    req._1().setTrainMetric(metricValue);
                }
                return req._1();
            }
            case 5: {
                List res = trainPhase5(jsonData, grad, hess, numClassRound, currentNode, newTreeNodes,
                        parameter, correspondingTreeNode, metricValue, trees, loss, datasetSize, pred, label, depth);
                // List of <boostP5Res, numClassRound, correspondingTreeNode, p, g, h, depth>
                BoostP5Res res0 = (BoostP5Res) res.get(0);
                numClassRound = (int) res.get(1);
                correspondingTreeNode = (TreeNode[]) res.get(2);
                pred = (double[][]) res.get(3);
                grad = (double[][]) res.get(4);
                hess = (double[][]) res.get(5);
                depth = (int) res.get(6);
                metricValue = (MetricValue) res.get(7);
                currentNode = (TreeNode) res.get(8);
                if (this.hasLabel) {
                    LeftTreeInfo lfi = (LeftTreeInfo) jsonData;
                    currentNode.recordId = lfi.getRecordId();
                    res0.setTrainMetric(metricValue);
                }
                return res0;
            }
            default:
                throw new UnsupportedOperationException("unsupported phase in federated gradient boost model");
        }
    }


    private double firstRoundPredict() {
        if (ObjectiveType.countPoisson.equals(parameter.getObjective())) {
            if (FirstPredictType.ZERO.equals(parameter.getFirstRoundPred())) {
                this.firstRoundPred = 0.0;
            } else if (FirstPredictType.AVG.equals(parameter.getFirstRoundPred())) {
                this.firstRoundPred = Math.log(MathExt.average(this.label));//TODO 需要加差分隐私
            } else if (FirstPredictType.RANDOM.equals(parameter.getFirstRoundPred())) {
                this.firstRoundPred = Math.log(Math.random());//TODO 随机种子
            }
            return firstRoundPred;
        }
        if (FirstPredictType.ZERO.equals(parameter.getFirstRoundPred())) {
            this.firstRoundPred = 0.0;
        } else if (FirstPredictType.AVG.equals(parameter.getFirstRoundPred())) {
            this.firstRoundPred = MathExt.average(this.label);//TODO 需要加差分隐私
        } else if (FirstPredictType.RANDOM.equals(parameter.getFirstRoundPred())) {
            this.firstRoundPred = Math.random();//TODO 随机种子
        }
        return firstRoundPred;
    }

    private void treeInit(int workerNum) {
        //先检测是否第一次请求，需要初始化很多参数
        newTreeNodes = new LinkedList<>();
        //对于有label的一方，新一颗树的建立
        if (parameter.getNumClass() > 1) {
            logger.info("Train Round " + (trees.size() / parameter.getNumClass() + 1) + " at class " + (numClassRound + 1) + "/" + parameter.getNumClass());
        }
        //新建树和根节点
        TreeNode root = new TreeNode(1, 1, workerNum, false);
        root.GradSetter(MathExt.sum(grad[numClassRound]));
        root.HessSetter(MathExt.sum(hess[numClassRound]));
        for (int i = 0; i < datasetSize; i++) {
            correspondingTreeNode[i] = root; // assign root to each datapoint
        }
        int[] tmp = new int[pred[numClassRound].length]; // variable to store result from each round
        for (int i = 0; i < pred[numClassRound].length; i++) {
            tmp[i] = i;
        }
        root.instanceSpace = tmp; // root node has all instance
        Tree tree = new Tree(root);
        //offer is similar to add
        tree.getAliveNodes().offer(root);
        trees.add(tree);
    }


    /**
     * 第一步，server端发送任务开始的请求，含label的client端收到后，初始化predict，并根据predict计算g和h
     * 如果已有predict，则直接根据predict计算g和h
     * 然后计算gradient 和 hessian，
     * 并对每个g和h同态加密，然后返回给服务端
     *
     * @param jsonData 数据
     * @param trainSet 训练集
     * @return json 格式的返回结果
     */
    public EncryptedGradHess trainPhase1(Message jsonData, BoostTrainData trainSet) {
        BoostP1Req req = (BoostP1Req) (jsonData);
        //无label的客户端直接返回
        if (!trainSet.hasLabel) {
            return new EncryptedGradHess(null, null, workerNum);
        }
        PrivateKey privateKey = getEncryptionTool().restorePrivateKey(privateKeyString);
        // 初始化的过程每个client都执行
        // 初始化分为全局初始化和每棵树的初始化
        // 加密g和h
        EncryptionTool encryptionTool = getEncryptionTool();
        if (req.isNewTree()) {
            treeInit(workerNum);
            encryptedG = Arrays.stream(grad[numClassRound]).parallel().mapToObj(g -> (encryptionTool.encrypt(g, privateKey.generatePublicKey()))).map(Ciphertext::serialize).collect(toList());
            encryptedH = Arrays.stream(hess[numClassRound]).parallel().mapToObj(h -> encryptionTool.encrypt(h, privateKey.generatePublicKey())).map(Ciphertext::serialize).collect(toList());
        }

        // 有label的客户端计算g 和 h(第一轮g、h已在初始化过程计算，其他轮在phase5计算)
        //TODO 第一轮的请求中带了表对齐等信息，随后会进行处理
        //读取当前tree（最后加进去的这棵），从tree中获取 alive nodes，然后取出队首元素并移除，赋值给current node
        Tree currentTree = trees.get(trees.size() - 1);
        currentNode = currentTree.getAliveNodes().poll();
        assert currentNode != null;
        // 当前节点的实例空间，即所有样本
        int[] instanceSpace = currentNode.instanceSpace;

        // 对g和h求和并复制给当前节点对应属性
        currentNode.Grad = Arrays.stream(instanceSpace).parallel().mapToDouble(x -> grad[numClassRound][x]).sum();
        currentNode.Hess = Arrays.stream(instanceSpace).parallel().mapToDouble(x -> hess[numClassRound][x]).sum();
        // generate publickey and encrytedArray storing (g, h) only at root(new tree)
        EncryptedGradHess res;
        if (req.isNewTree()) {
            StringTuple2[] encryptedArray = Arrays.stream(instanceSpace).parallel().mapToObj(x -> new StringTuple2(encryptedG.get(x), encryptedH.get(x))).toArray(StringTuple2[]::new);
            String pk = privateKey.generatePublicKey().serialize();
            res = new EncryptedGradHess(req.getClient(), instanceSpace, encryptedArray, pk, true);
        } else {
            res = new EncryptedGradHess(req.getClient(), instanceSpace);
        }
        res.setTrainMetric(metricValue);
        return res;
    }

    /**
     * client端根据g和h，计算所有的gl，hl
     * 输入的json data中包含了encrypted gl 和 hl
     *
     * @param jsonData 迭代参数
     * @param trainSet 训练数据
     * @return 训练结果
     */
    // TODO 把ghMap2加入入参
    public BoostP2Res trainPhase2(Message jsonData, BoostTrainData trainSet, EncryptionTool encryptionTool) {
        //含label的客户端无需处理
        if (trainSet.hasLabel) {
            BoostP2Res res = new BoostP2Res(null);
            res.setTrainMetric(metricValue);
            res.setWorkerNum(workerNum);
            return res;
        }
        // jsonData is output from phase1
        EncryptedGradHess req = (EncryptedGradHess) (jsonData);
        // get instance information from the processing node
        int[] instanceSpace = req.getInstanceSpace();
        // initialize publicKey with null
//        EncryptionTool encryptionTool = getEncryptionTool();
        if (req.getNewTree()) {
            // if new tree, get Grad Hess.
            StringTuple2[] gh = req.getGh();
            // if new tree, get publicKey.
            publicKeyString = req.getPubKey();
//            PublicKey publicKey = encryptionTool.restorePublicKey(pubKey);
            // instance index i - (g_i, h_i)
            ghMap2String = new HashMap<>();
            for (int i = 0; i < instanceSpace.length; i++) {
//                ghMap2.put(instanceSpace[i], new Tuple2<>(encryptionTool.restoreCiphertext(gh[i].getFirst()), encryptionTool.restoreCiphertext(gh[i].getSecond())));
                ghMap2String.put(instanceSpace[i], new Tuple2<>(gh[i].getFirst(), gh[i].getSecond()));
            }
        }
        //TODO 根据样本空间查询
//        ColSampler colSampler = trainSet.getColSampler();
        //对每一列（特征），每个特征计算gl hl
        //忽略column 0 ，col-0 是用户id
        PublicKey finalPublicKey = encryptionTool.restorePublicKey(publicKeyString);
        // feature从1开始，不是从0开始
        // TODO 为什么会从这里print出来很多的is true??
        Map<Integer, Tuple2<Ciphertext, Ciphertext>> ghMap2 = new HashMap<>();
        for (int i = 0; i < instanceSpace.length; i++) {
            ghMap2.put(instanceSpace[i], new Tuple2<>(encryptionTool.restoreCiphertext(ghMap2String.get(instanceSpace[i])._1()), encryptionTool.restoreCiphertext(ghMap2String.get(instanceSpace[i])._2())));
        }
        FeatureLeftGH[] bodyArray = IntStream.range(1, trainSet.getFeatureDim() + 1)
                .parallel()
                .mapToObj(col -> {
                    // current instance space corresponding feature value matrix
                    double[][] theFeature = trainSet.getFeature(instanceSpace, col);
                    // use feature value to sort and bucket the feature
                    List<Bucket> buckets = sortAndGroup(theFeature, parameter.getNumBin());
                    // feature id -> feature buckets
                    sortedFeatureMap.put(col, buckets);
                    StringTuple2[] full = ghSum(buckets, ghMap2, finalPublicKey, encryptionTool);
                    String feature;
                    // todo 是否可用isDistributed
                    if (isDistributed) {
                        feature = "" + featureindexs.get(col);
                    } else {
                        feature = "" + col;
                    }
                    return new FeatureLeftGH(req.getClient(), feature, full);
                })
                .toArray(FeatureLeftGH[]::new);
        return new BoostP2Res(bodyArray);
    }

    private EncryptionTool getEncryptionTool() {
        return new JavallierTool();
    }

    /**
     * 对于一个feature，计算并返回feature bucket
     */
    public List<Bucket> sortAndGroup(double[][] mat, int numbin) {
        //2列分别是instanceId，值，按照值排序
        //TODO 区分数值型与枚举型
        Arrays.sort(mat, Comparator.comparingDouble(a -> a[1]));
        //分桶: get bucket info for "Left instance"
        return Tool.split2bucket2(mat, numbin);
    }

    /**
     * encrypt sum of g and h for "left instances" for each bucket cumulatively
     *
     * @param buckets        feature buckets
     * @param ghMap2         instance index i,  (g_i, h_i)
     * @param publicKey
     * @param encryptionTool
     * @return StringTuple Array
     */
    public StringTuple2[] ghSum(List<Bucket> buckets, Map<Integer, Tuple2<Ciphertext,
            Ciphertext>> ghMap2, PublicKey publicKey, EncryptionTool encryptionTool) {
//        final String zero = PaillierTool.encryption(0, paillierPublicKey);
        StringTuple2[] full = buckets
                .parallelStream()
                .map(bucket -> {
                    StringTuple2 res = Arrays.stream(bucket.getIds())
                            .parallel()
                            .mapToObj(id -> ghMap2.get((int) id))
                            // 求和g h
                            .reduce((x, y) -> new Tuple2<>(encryptionTool.add(x._1(), y._1(), publicKey), encryptionTool.add(x._2(), y._2(), publicKey)))
                            .map(x -> new StringTuple2((x._1().serialize()), (x._2().serialize())))
                            .get();
                    return res;
                }).toArray(StringTuple2[]::new);
        return full;
    }

    /**
     * for each feature in the buckets(List), calculate totalG, totalH for each bucket; same function as ghSum
     * the only difference is that ghSum is encrypted but processEachNumericFeature2 is not.
     *
     * @param buckets
     * @param ghMap:  (index, (g, h))
     * @return
     */
    public DoubleTuple2[] processEachNumericFeature2(List<Bucket> buckets, Map<Integer, DoubleTuple2> ghMap) {
        List<DoubleTuple2> ghList = new ArrayList<>();
        for (Bucket bucket : buckets) {
            double tmpG = 0;
            double tmpH = 0;
            for (double id : bucket.getIds()) {
                int intId = (int) id;
                DoubleTuple2 singleGH = ghMap.get(intId);
                double singleG = singleGH.getFirst();
                double singleH = singleGH.getSecond();
                tmpG += singleG;
                tmpH += singleH;
            }
            ghList.add(new DoubleTuple2(tmpG, tmpH));
        }
        return ghList.toArray(new DoubleTuple2[0]);
    }

    /**
     * 服务端分发gl hl，含有label的客户端根据gl hl计算 SplitFinding算法，计算(i,k,v)并返回
     *
     * @param jsonData 迭代数据
     * @param trainSet BoostTrainData
     * @return BoostP3Res
     */
    public Tuple2<BoostP3Res, Double> trainPhase3(Message jsonData, BoostTrainData trainSet, TreeNode currentNode, EncryptionTool encryptionTool, String privateKeyString,
                                                  FgbParameter parameter, double[][] grad, double[][] hess,
                                                  int numClassRound) {
        //不含label的客户端无需处理
        if (!trainSet.hasLabel) {
            BoostP3Res boostP3Res = new BoostP3Res();
            boostP3Res.setWorkerNum(workerNum);
            return new Tuple2<>(boostP3Res, null);
        }
        BoostP3Req req = (BoostP3Req) (jsonData);
        double g = currentNode.Grad; //G_Total: the gradient sum of the samples fall into this tree node
        double h = currentNode.Hess; //H_Total: the hessian sum of the samples fall into this tree node
        ClientInfo client = null;
        String feature = "";
        int splitIndex = 0;
        double gain = 0;
        if (modelId == 0) {
//        double gain = -Double.MAX_VALUE;
//            EncryptionTool encryptionTool = getEncryptionTool();
            PrivateKey privateKey = encryptionTool.restorePrivateKey(privateKeyString);
            //遍历属于其他方的加密特征
            //先将各个客户端的数据拆分成以特征为维度，然后对该特征计算最大gain，然后取所有特征得gain的最大值
            GainOutput gainOutput = req.getDataList()
                    .parallelStream()
                    .flatMap(Collection::stream)
                    .map(x -> fetchGain(x, g, h, encryptionTool, privateKey, parameter))
                    .max(Comparator.comparing(GainOutput::getGain))
                    .get();
            // Corresponding client, feature, splitIndex for the best gain
            gain = gainOutput.getGain();
            client = gainOutput.getClient();
            feature = gainOutput.getFeature();
            splitIndex = gainOutput.getSplitIndex();
        }
//        if (gain <0) {
//           currentNode.gain =gain;
//        }
        //遍历属于自己的未加密特征
        //忽略column 0 ，col 0 是用户id
        int[] instanceSpace = currentNode.instanceSpace;
        Map<Integer, DoubleTuple2> ghMap = new HashMap<>();
        for (int ins : instanceSpace) {
            ghMap.put(ins, new DoubleTuple2(grad[numClassRound][ins], hess[numClassRound][ins]));
        }
        for (int col = 1; col <= trainSet.getFeatureDim(); col++) {
            double[][] rawFeature = trainSet.getFeature(instanceSpace, col);
            //TODO 在 sortAndGroup 函数中区分数值型与枚举型
            List<Bucket> buckets = sortAndGroup(rawFeature, parameter.getNumBin());
            //TODO 使用更轻量级的buckets作为缓存
            sortedFeatureMap.put(col, buckets);
            // body: BucketIndex -> (gL, hL)
            DoubleTuple2[] body = processEachNumericFeature2(buckets, ghMap); // ghMap: index -> (g,h)
            Optional<Tuple2<Double, Integer>> featureMaxGain = computeGain(body, g, h, parameter).parallelStream().max(Comparator.comparing(Tuple2::_1));
            // 本地特征最佳split gain和其他client进行对比
            if (featureMaxGain.isPresent() && featureMaxGain.get()._1() > gain) {
                gain = featureMaxGain.get()._1();
                client = req.getClient();
                if (isDistributed) {
                    feature = "" + featureindexs.get(col);
                } else {
                    feature = "" + col;
                }
                splitIndex = featureMaxGain.get()._2();
            }
        }
        //根据差分隐私指数机制随机选取一个分裂点
        BoostP3Res boostP3Res = new BoostP3Res(client, feature, splitIndex);
        if (isDistributed) {
            boostP3Res = new BoostP3Res(client, feature, splitIndex, gain);
            boostP3Res.setWorkerNum(workerNum);
        }
        Tuple2<BoostP3Res, Double> t = new Tuple2<>(boostP3Res, gain);
        t._1().setTrainMetric(metricValue);
        return t;
    }

    /**
     * Compute gain according XGBoost algorithm
     *
     * @param decryptedGH: GL and HL at this node at each threshold
     * @param g:           G_total at this node
     * @param h:           H_total at this node
     * @return
     */
    public List<Tuple2<Double, Integer>> computeGain(DoubleTuple2[] decryptedGH,
                                                     double g, double h, FgbParameter parameter) {
        List<Tuple2<Double, Integer>> allGain = new ArrayList<>();
        double gL = 0;
        double hL = 0;
        for (int i = 0; i < decryptedGH.length - 1; i++) {
            gL += decryptedGH[i].getFirst();
            hL += decryptedGH[i].getSecond();
            // for each bucket, calculate gain according XGBoost algorithm
            double tmpGain = Tree.calculateSplitGain(gL, hL, g, h, parameter);
            // i is split index
            allGain.add(new Tuple2<>(tmpGain, i));
        }
        return allGain;
    }

    /**
     * Get the best split index and gain from this split for one feature according to Xgboost algorithm
     *
     * @param input          FeatureLeftGH from phase2 output
     * @param g              G_Total at this node
     * @param h              H_Total at this node
     * @param encryptionTool
     * @param privateKey
     * @param parameter
     * @return GainOutput(ClientInfo client, String feature, int bestSplitIndex, double BestGain)
     */
    public GainOutput fetchGain(FeatureLeftGH input, double g, double h,
                                EncryptionTool encryptionTool, PrivateKey privateKey, FgbParameter parameter) {
        int splitIndex = 0;
        double gain = -Double.MAX_VALUE;
        // Left G and Left H for one feature at each bucket
        StringTuple2[] tmpGH = input.getGhLeft();
        // decrypt G and H
        DoubleTuple2[] decryptedGH = Arrays.asList(tmpGH)
                .parallelStream()
                .map(x -> new DoubleTuple2(encryptionTool.decrypt(x.getFirst(), privateKey), encryptionTool.decrypt(x.getSecond(), privateKey)))
                .toArray(DoubleTuple2[]::new);
        // computeGain returns a list of gain of each split according to the bucket
        // returns the best gain and its index
        Tuple2<Double, Integer> maxGain = computeGain(decryptedGH, g, h, parameter)
                .parallelStream()
                .max(Comparator.comparing(Tuple2::_1))
                .orElse(new Tuple2<>(gain, splitIndex));
        return new GainOutput(input.getClient(), input.getFeature(), maxGain._2(), maxGain._1());
    }

    //build sub query index based on <i,k,v>


    /**
     * 发送message和接受message若为不同client会直接返回空，若是相同的client会分裂并计算左子树样本集合
     *
     * @param jsonData
     * @param sortedFeatureMap  ( feature id, feature buckets)
     * @param passiveQueryTable
     * @return
     */
    public Tuple2<LeftTreeInfo, List<QueryEntry>> trainPhase4(Message jsonData, Map<Integer, List<Bucket>> sortedFeatureMap,
                                                              List<QueryEntry> passiveQueryTable) {
//        Map<Integer, List<Bucket>> sortedFeatureMap,
//        LinkedHashMap<Integer, QueryEntry> passiveQueryTable, int contributeFea
        BoostP4Req req = (BoostP4Req) (jsonData);
        if (!req.isAccept()) {
            Tuple2<LeftTreeInfo, List<QueryEntry>> t = new Tuple2<>(new LeftTreeInfo(0, null), passiveQueryTable);
            return t;
        }
        //本次要分裂的特征
        int featureIndex = req.getkOpt();
        int loaclIndex = featureIndex;
        //根据phase3 的训练结果 生成的分裂点，并不是直接的数值，而是在原始给出的分裂选项中的第i个选项
        int splitIndex = req.getvOpt();
        if (isDistributed) {
            if (!featureindexs.contains(featureIndex)) {
                Tuple2<LeftTreeInfo, List<QueryEntry>> t = new Tuple2<>(new LeftTreeInfo(0, null), passiveQueryTable);
                return t;
            } else {
                loaclIndex = featureindexs.indexOf(featureIndex);
            }
        }
        List<Bucket> sortedFeature2 = sortedFeatureMap.get(loaclIndex); // 最佳特征分桶
        double splitValue = sortedFeature2.get(splitIndex).getSplitValue(); // 最佳特征最优分裂值
        //分裂点处理，将与分裂点相同数值的样本放在同一侧，方便预测时使用
        //左子树样本
        //最佳分裂点左侧分桶
        List<Integer> leftIns = new ArrayList<>();
        for (int i = 0; i <= splitIndex; i++) {
            Bucket Bucket = sortedFeature2.get(i);
            double[] ids = Bucket.getIds();
            for (int j = 0; j < ids.length; j++) {
                double id = ids[j];
                leftIns.add((int) id);
                ;
            }
        }

        //最佳分裂点右侧分桶，右侧的分桶中小于splitValue的值？
        for (int i = splitIndex + 1; i < sortedFeature2.size(); i++) {
            Bucket bucket = sortedFeature2.get(i);
            double[] values = bucket.getValues();
            for (int j = 0; j < values.length; j++) {
                double v = values[j];
                if (v <= splitValue) {
                    double id = bucket.getIds()[j];
                    leftIns.add((int) id);
                }
            }
        }
        int recordId = 1;
        if (passiveQueryTable == null || passiveQueryTable.size() == 0) {
            QueryEntry entry = new QueryEntry(recordId, featureIndex, splitValue); // 新建查询表条目记录分裂特征和分裂值
            passiveQueryTable = new ArrayList<>();
            passiveQueryTable.add(entry);
        } else {
            QueryEntry lastLine = passiveQueryTable.get(passiveQueryTable.size() - 1); // 当前查询表的最后一行
            assert lastLine != null;
            recordId = lastLine.getRecordId() + 1;
            QueryEntry line = new QueryEntry(recordId, featureIndex, splitValue);
            passiveQueryTable.add(line);
        }
        int[] left = Tool.list2IntArray(leftIns);
        Tuple2<LeftTreeInfo, List<QueryEntry>> t = new Tuple2<>(new LeftTreeInfo(recordId, left), passiveQueryTable);
        return t;
    }

    //build full query index and update prediction and grad

    /**
     * 有label的client收到了各个client的本次树的分裂信息和左子树样本id list，更新查询树并进行下一轮迭代
     *
     * @param jsonData
     * @param grad
     * @param hess
     * @param numClassRound
     * @param currentNode
     * @param newTreeNodes
     * @param parameter
     * @param correspondingTreeNode
     * @param metricMap
     * @param trees
     * @return
     */
    public List trainPhase5(Message jsonData, double[][] grad, double[][] hess,
                            int numClassRound, TreeNode currentNode,
                            Queue<TreeNode> newTreeNodes, FgbParameter parameter,
                            TreeNode[] correspondingTreeNode, MetricValue metricMap,
                            List<Tree> trees, Loss loss, int datasetSize, double[][] pred, double[] label, int depth) {

        // Update了pred, grad, hess三个全局变量， 由updateGH来update
        // update了currentNode.recordId
        // update了numClassRound
        // update了correspondingTreeNode，postPrune
        // update depth
        // update metricMap
        // todo 将datasetSize也放入入参
        //无label的客户端直接返回
        List res = new ArrayList();

        if (!this.hasLabel) {
            // List of <boostP5Res, numClassRound, correspondingTreeNode, p,g,h, depth>
            res = addElement(res, new BoostP5Res(false, 0), numClassRound,
                    correspondingTreeNode, pred, grad, hess, depth, null, currentNode);
            assert res.size() == 9;
            return res;
        }

        LeftTreeInfo req = (LeftTreeInfo) (jsonData);
        // 获取client的左子树样本id集
        int[] instanceIds = req.getLeftInstances();
        // 计算左子树的Gtotal和Htotal
        double lGrad = 0;
        double lHess = 0;
        for (int i : instanceIds) {
            lGrad += grad[numClassRound][i];
            lHess += hess[numClassRound][i];
        }
        //需要根据阈值将当前节点的样本空间分为两个，分别赋值给左右子节点
        TreeNode node = currentNode;
        currentNode.recordId = req.getRecordId(); // 根据phase4传来的message更新当前节点的recordID

//        currentNode.client = req.getClient();
//        if (node.gain < 0) {
//            //this node is leaf node
//            double leafScore = Tree.calculateLeafScore(node.Grad, node.Hess, parameter.lambda);
//            node.leafNodeSetter(leafScore, true);
//        } else {
        depth = node.depth; // 当前树节点深度

        // 将当前节点进行分裂成左子树和右子树
        TreeNode leftChild = new TreeNode(3 * node.index - 1, node.depth + 1, node.featureDim, false); // not leaf
        leftChild.numSample = instanceIds.length;
        leftChild.instanceSpace = instanceIds;
        leftChild.Grad = lGrad;
        leftChild.Hess = lHess;

        TreeNode rightChild = new TreeNode(3 * node.index + 1, node.depth + 1, node.featureDim, false);
        int[] rightIns = MathExt.diffSet2(node.instanceSpace, instanceIds);

        rightChild.instanceSpace = rightIns;
        rightChild.numSample = rightIns.length;
        rightChild.Grad = node.Grad - lGrad;
        rightChild.Hess = node.Hess - lHess;
        // TODO 这里默认没有NULL的节点？和之前的处理不一致
        TreeNode nanChild = null;
        // 新树的节点
        newTreeNodes.offer(leftChild);
        newTreeNodes.offer(rightChild);
//        if (nanChild != null) {
//            newTreeNodes.offer(nanChild);
//        }
        // 处理nan的方式，默认没有nan子树
        double bestNanGoTo = 1;
        node.internalNodeSetterSecure(bestNanGoTo, nanChild, leftChild, rightChild, false);
        //phase 5 运行完成后，根据条件检测，
//        }
        //每层最后一个节点，执行更新
        Tree currentTree = trees.get(trees.size() - 1);

        if (currentTree.getAliveNodes().isEmpty()) {
//        if (Math.pow(3, node.depth) - 1 == 2 * node.index) {
            //update classList.correspondingTreeNode
            updateCorrespondingTreeNodeSecure(correspondingTreeNode);
            //update (Grad,Hess,numSample) for each new tree node
//            model.updateGradHessNumsampleForTreeNode();
            //update GradMissing, HessMissing for each new tree node
            //time consumption: 5ms
//            model.updateGradHessMissingForTreeNode(trainSet.missingIndex);
//            Tree currentTree = trees.get(trees.size() - 1);
            while (newTreeNodes.size() != 0) {
//                noise = DifferentialPrivacyUtil.laplaceMechanismNoise(deltaV, eNleaf);
                TreeNode treenode = newTreeNodes.poll();
                if (treenode.depth >= parameter.getMaxDepth()
                        || treenode.Hess < parameter.getMinChildWeight()
                        || treenode.numSample <= parameter.getMinSampleSplit()
                        || treenode.instanceSpace.length < parameter.getMinSampleSplit()) {
                    treenode.leafNodeSetter(Tree.calculateLeafScore(treenode.Grad, treenode.Hess, parameter.getLambda()), true);
                } else {
                    currentTree.getAliveNodes().offer(treenode);
                }
            }
            // 不再有待分裂的节点
            if (currentTree.getAliveNodes().isEmpty()) {
                return emptyTreeUpdate(currentTree, parameter, correspondingTreeNode,
                        depth, datasetSize, label, numClassRound, pred, loss, metricMap, res);
            }
//            updateGH(trainSet, 1);
        }
        // 如果不再有待分裂的节点
        if (currentTree.getAliveNodes().isEmpty()) {
            return emptyTreeUpdate(currentTree, parameter, correspondingTreeNode,
                    depth, datasetSize, label, numClassRound, pred, loss, metricMap, res);
        }

        // 如果之前都没有return
        // List of <boostP5Res, numClassRound, correspondingTreeNode, pgh_tuple, depth, metricMap>
        res = addElement(res, new BoostP5Res(false, depth), numClassRound,
                correspondingTreeNode, pred, grad, hess, depth, metricMap, currentNode);
        assert res.size() == 9;
        return res;
    }

    // method in the if(currentTree.getAliveNodes().byteArrayIsEmpty()) where isStop = true
    private List emptyTreeUpdate(Tree currentTree, FgbParameter parameter, TreeNode[] correspondingTreeNode,
                                 int depth, int datasetSize, double[] label, int numClassRound,
                                 double[][] pred, Loss loss, MetricValue metricMap, List res) {
        postPrune(currentTree.getRoot(), parameter, correspondingTreeNode);
        BoostP5Res boostP5Res = new BoostP5Res(true, depth);
        List pgh = updateGH(datasetSize, correspondingTreeNode, label, numClassRound, pred, parameter, loss);
        if (numClassRound + 1 == parameter.getNumClass()) {
            assert metricMap != null;
            Map<MetricType, Double> trainMetric = updateMetric(parameter, loss, pred, label);
            assert trainMetric != null;
            // TODO 优化trainMetric.forEach(...);
            trainMetric.forEach((key, value) -> {
                int size = metricMap.getMetrics().get(key).size();
                metricMap.getMetrics().get(key).add(new Pair<>(size, value));
            });
            numClassRound = 0;
        } else {
            numClassRound++;
        }
        // List of <boostP5Res, numClassRound, correspondingTreeNode, pgh_tuple, depth, metricMap>
        res = addElement(res, boostP5Res, numClassRound, correspondingTreeNode,
                (double[][]) pgh.get(0), (double[][]) pgh.get(1), (double[][]) pgh.get(2), depth, metricMap, currentNode);
        assert res.size() == 9;
        return res;
    }

    private List addElement(List a, Object... arg) {
        for (int i = 0; i < arg.length; i++) {
            a.add(arg[i]);
        }
        return a;
    }

    // TODO 修改unit test 把这些应该是private function的还是放成是private function
    // recursively postPrune 可能会更新root， correspondingTreeNode对应的信息
    public void postPrune(TreeNode root, FgbParameter parameter, TreeNode[] correspondingTreeNode) {
        if (root.isLeaf) {
            return;
        }
        // 如果root节点不是leaf，就继续左右分别剪枝
        postPrune(root.leftChild, parameter, correspondingTreeNode);
        postPrune(root.rightChild, parameter, correspondingTreeNode);
        // 如果左节点右节点都是leaf，且root的gain<=0那就prune，将root设置为leaf
        if (root.leftChild.isLeaf && root.rightChild.isLeaf && root.gain <= 0) {
            for (int sampleInstance : root.instanceSpace) {
                correspondingTreeNode[sampleInstance] = root;
            }
            // xgboost: leaf score = - G / (H + lambda)
            root.leafNodeSetter(Tree.calculateLeafScore(root.Grad, root.Hess, parameter.getLambda()), true);
        }
    }

    private List updateGH(int datasetSize, TreeNode[] correspondingTreeNode, double[] label,
                          int numClassRound, double[][] pred, FgbParameter parameter, Loss loss) {
//        if (this.hasLabel) {
        // eta = parameter.getEta()
        // 更新pred, grad, hess需要返回
        pred = updatePred(parameter.getEta(), datasetSize, correspondingTreeNode, numClassRound, pred);
        Tuple2 ghTuple = updateGradHess(loss, parameter.getScalePosWeight(), label, pred, datasetSize, parameter, numClassRound);
        double[][] grad = (double[][]) ghTuple._1();
        double[][] hess = (double[][]) ghTuple._2();
        ArrayList res = new ArrayList();
        res.add(pred);
        res.add(grad);
        res.add(hess);
        return res;
//        }
    }

    private Map<MetricType, Double> updateMetric(FgbParameter parameter, Loss loss, double[][] pred, double[] label) {
        if (!this.hasLabel) {
            return null;
        }
        Map<MetricType, Double> trainMetric;
        MetricType[] evalMetric = parameter.getEvalMetric();
        //此处统计指标除debug外不再开启，返回给master端，并在master端查看和展示
        if (ObjectiveType.countPoisson.equals(parameter.getObjective())) {
            trainMetric = Metric.calculateMetric(evalMetric, loss.expTransform(Arrays.stream(pred).flatMapToDouble(Arrays::stream).toArray()), loss.expTransform(label));
        } else {
            trainMetric = Metric.calculateMetric(evalMetric, loss.transform(Arrays.stream(pred).flatMapToDouble(Arrays::stream).toArray()), label);
        }
        return trainMetric;
    }

    public void initializePred(double firstRoundPred) {
        Arrays.fill(pred[numClassRound], firstRoundPred);
    }

    public double[][] updatePred(double eta, int datasetSize, TreeNode[] correspondingTreeNode,
                                 int numClassRound, double[][] pred) {
        for (int i = 0; i < datasetSize; i++) {
            TreeNode tmpNode = correspondingTreeNode[i];
            if (tmpNode == null) {
            } else {
                // TODO 在train phase5把pred return出去
                pred[numClassRound][i] += eta * tmpNode.leafScore;
            }
        }
        return pred;
    }

    public Tuple2 updateGradHess(Loss loss, double scalePosWeight, double[] label,
                                 double[][] pred, int datasetSize, FgbParameter parameter, int numClassRound) {
        double[][] grad = Tool.reshape(loss.grad(Arrays.stream(pred).flatMapToDouble(Arrays::stream).toArray(), label), parameter.getNumClass());
        double[][] hess = Tool.reshape(loss.hess(Arrays.stream(pred).flatMapToDouble(Arrays::stream).toArray(), label), parameter.getNumClass());
        // TODO: MuitlClass ScalePosWeight
        if (scalePosWeight != 1.0) {
            for (int i = 0; i < datasetSize; i++) {
                if (label[i] == 1) {
                    grad[numClassRound][i] *= scalePosWeight;
                    hess[numClassRound][i] *= scalePosWeight;
                }
            }
        }
        return new Tuple2(grad, hess);
    }

//    public void sampling(ArrayList<Double> rowMask) {
//        for (int i = 0; i < datasetSize; i++) {
//            grad[numClassRound][i] *= rowMask.get(i);
//            hess[numClassRound][i] *= rowMask.get(i);
//        }
//    }

    public void updateGradHessMissingForTreeNode(int[][] missingValueAttributeList) {
        for (int col = 0; col < missingValueAttributeList.length; col++) {
            for (int i : missingValueAttributeList[col]) {
                TreeNode treenode = correspondingTreeNode[i];
                if (!treenode.isLeaf) {
                    treenode.GradMissing[col] += grad[numClassRound][i];
                    treenode.HessMissing[col] += hess[numClassRound][i];
                }
            }
        }
    }

    private void updateCorrespondingTreeNodeSecure(TreeNode[] correspondingTreeNode) {
        for (int i = 0; i < datasetSize; i++) {
            TreeNode treenode = correspondingTreeNode[i];
            // 对于每个datapoint对应的node，如果是leaf就pass
            if (treenode.isLeaf) {
                continue;
            }
            // 如果不是leaf，判断是否当前节点在左子树里，如果是就更新为左子树，如果不是就更新到右子树里
            if (Tool.contain(treenode.leftChild.instanceSpace, i)) {
                correspondingTreeNode[i] = treenode.leftChild;
            } else {
                correspondingTreeNode[i] = treenode.rightChild;
            }
        }
    }

    /**
     * 分布式初始化map
     *
     * @param requestId   请求id
     * @param rawData     原始数据集
     * @param trainInit   初始化训练请求
     * @param matchResult id对齐结果
     * @return 初始化map
     */
    public InitResult initMap(String requestId, String[][] rawData, TrainInit trainInit, String[] matchResult) {
        Map<String, Object> others = trainInit.getOthers();
        others.put("isDistributed", "true");
        others.put("modelId", Integer.valueOf(requestId));
        int[] testIndex = trainInit.getTestIndex();
        Model localModel;
        localModel = new DistributedFederatedGBModel();
        Features features = trainInit.getFeatureList();
        int workerNum = (int) others.get("workerNum");
        HyperParameter hyperParameter = trainInit.getParameter();
        // TODO 要不要只传当前列的数据/特征
        TrainData trainData = localModel.trainInit(rawData, matchResult, testIndex, hyperParameter, features, others);
        List<String> modelIDs = new ArrayList<>();
        // todo 目前是仅切分了特征，竖着切，后续考虑是否横向切分【横向切分的单位？】
        for (int i = 0; i < workerNum; i++) {
            modelIDs.add(String.valueOf(i));
        }
        InitResult initResult = new InitResult();
        initResult.setModel(localModel);
        initResult.setTrainData(trainData);
        initResult.setModelIDs(modelIDs);
        return initResult;
    }

    /***
     * 返回需要读取的行
     * @param requestId 请求ID
     * @param trainInit 训练请求
     * @param sortedIndexList 需要读取的index列表
     * @return 完成排序的id列表
     */
    public ArrayList<Integer> dataIdList(String requestId, TrainInit trainInit, List<Integer> sortedIndexList) {
        // 防止乱序
        Collections.sort(sortedIndexList);
        // 添加第一行头
        ArrayList<Integer> sampleData = new ArrayList<>();
        sampleData.add(0);
        // 因为添加了一行头部，整体需要
        List<Integer> mapSampleRandomIndex = sortedIndexList.stream().map(integer -> integer + 1).collect(Collectors.toList());
        sampleData.addAll(mapSampleRandomIndex);
        return sampleData;
    }

    /***
     * 分布式任务拆分
     * @param phase 训练阶段
     * @param req   训练请求
     * @return 拆分的map结果
     */
    @Override
    public SplitResult split(int phase, Message req) {
        switch (phase) {
            case 0:
                return mapPhase0(req);
            case 1:
            case 5:
                return constructSplitRes(req);
            case 2:
                return mapPhase2(req);
            case 3:
                return mapPhase3(req);
            case 4:
                return mapPhase4(req);
            default:
                throw new UnsupportedOperationException("unsupported phase in federated gradient boost model");
        }
    }

    /***
     * 训练初始化任务拆分
     * 根据总特征数和每个任务的特征数，拆分训练任务，构造训练初始化请求。
     * @param req
     * @return
     */
    private SplitResult mapPhase0(Message req) {
        TrainInit request;
        if (req instanceof TrainInit) {
            request = (TrainInit) req;
        } else {
            throw new NotMatchException("Message to TrainInit error in map phase0");
        }
        SplitResult splitResult = new SplitResult();
        List<String> modelIDs = new ArrayList<>();
        List<Message> messageBodies = new ArrayList<>();
        Features features = request.getFeatureList();
        int featuresNum = features.getFeatureList().size() - 1;
        if (features.hasLabel()) {
            featuresNum = featuresNum - 1;
        }
        // todo 每个任务的特征数,需改成动态的
        int eachFeature = 3;
        int workerNum = featuresNum / eachFeature;
        if (featuresNum % eachFeature != 0) {
            workerNum = workerNum + 1;
        }
        List<Integer> allFeatureIndexs = IntStream.range(1, featuresNum + 1).boxed().collect(Collectors.toList());
        List<SingleFeature> featureList = features.getFeatureList();
        SingleFeature uid = featureList.get(0);
        SingleFeature label = featureList.get(featureList.size() - 1);
        int labelIndex = featureList.size() - 1;
        if (features.hasLabel()) {
            labelIndex = featureList.indexOf(featureList.stream().filter(x -> x.getName().equals(features.getLabel())).findFirst().get());
            if (labelIndex != featureList.size() - 1) {
                Collections.replaceAll(allFeatureIndexs, labelIndex, featureList.size() - 1);
            }
            label = featureList.get(labelIndex);
            featureList.remove(labelIndex);
        }
        List<List<Integer>> splitFeaturesIndexRes = splitList(allFeatureIndexs, workerNum);
        featureList.remove(0);
        List<List<SingleFeature>> splitFeaturesRes = splitList(featureList, workerNum);
        for (int i = 0; i < workerNum; i++) {
            List<SingleFeature> featureList1 = new ArrayList<>(splitFeaturesRes.get(i));
            List<Integer> featureindexs = new ArrayList<>(splitFeaturesIndexRes.get(i));
            featureList1.add(0, uid);
            featureindexs.add(0, 0);
            if (features.hasLabel()) {
                featureList1.add(label);
                featureindexs.add(labelIndex);
            }
            Features subFeatures = new Features(featureList1, features.getLabel());
            Map<String, Object> others = ((TrainInit) req).getOthers();
            Map<String, Object> objectMap = new HashMap<>(others);
            objectMap.put("featureindexs", featureindexs);
            objectMap.put("workerNum", workerNum);
            final TrainInit trainInit = new TrainInit(request.getParameter(), subFeatures, request.getTestIndex(), request.getMatchId(), objectMap);
//            //todo 各worker的请求是否保持一致 featureList？
            messageBodies.add(trainInit);
            modelIDs.add(String.valueOf(i));
        }
        splitResult.setReduceType(ReduceType.needMerge);
        splitResult.setMessageBodys(messageBodies);
        splitResult.setModelIDs(modelIDs);
        return splitResult;
    }

    /***
     * 列表拆分
     * 给定需要拆分的列表和拆分的数量，返回拆分结果
     *
     * @param list 需要拆分的列表
     * @param n 需要拆分的数量
     * @param <T> 列表的类型
     * @return 拆分结果
     */
    public <T> List<List<T>> splitList(List<T> list, int n) {
        List<List<T>> result = new ArrayList<List<T>>();
        int remaider = list.size() % n; //(先计算出余数)
        int number = list.size() / n; //然后是商
        int offset = 0;//偏移量
        for (int i = 0; i < n; i++) {
            List<T> value = null;
            if (remaider > 0) {
                value = list.subList(i * number + offset, (i + 1) * number + offset + 1);
                remaider--;
                offset++;
            } else {
                value = list.subList(i * number + offset, (i + 1) * number + offset);
            }
            result.add(value);
        }
        return result;
    }

    /**
     * 无需拆分的任务
     *
     * @param req 请求
     * @return 返回单个任务
     */
    private SplitResult constructSplitRes(Message req) {
        SplitResult splitResult = new SplitResult();
        List<String> modelIDs = new ArrayList<>();
        modelIDs.add(String.valueOf(0));
        splitResult.setMessageBodys(Collections.singletonList(req));
        splitResult.setModelIDs(modelIDs);
        splitResult.setReduceType(ReduceType.needMerge);
        return splitResult;
    }

    /**
     * 阶段2被动方训练任务拆分：
     * 按特征拆分，计算本地各特征分裂阈值并返回。
     *
     * @param req 请求
     * @return 完成拆分的任务列表
     */
    private SplitResult mapPhase2(Message req) {
        SplitResult splitResult = new SplitResult();
        List<String> modelIDs = new ArrayList<>();
        List<Message> messageBodys = new ArrayList<>();
        EncryptedGradHess gradHess;
        // TODO
        if (req instanceof EmptyMessage) {
            messageBodys.add(req);
            modelIDs.add(String.valueOf(0));
        } else if (req instanceof EncryptedGradHess) {
            gradHess = (EncryptedGradHess) req;
            for (int i = 0; i < gradHess.getWorkerNum(); i++) {
                messageBodys.add(req);
                modelIDs.add(String.valueOf(i));
            }
        }
        splitResult.setMessageBodys(messageBodys);
        splitResult.setModelIDs(modelIDs);
        splitResult.setReduceType(ReduceType.needMerge);
        return splitResult;
    }

    /**
     * 阶段3主动方训练任务拆分：
     * 按特征拆分，计算本地各特征的最优分裂点，其中一个与被动方最优分裂节点进行比较，将较优分裂节点返回。
     *
     * @param req 请求
     * @return 完成拆分的任务列表
     */
    private SplitResult mapPhase3(Message req) {
        SplitResult splitResult = new SplitResult();
        List<String> modelIDs = new ArrayList<>();
        List<Message> messageBodys = new ArrayList<>();
        BoostP3Req boostP3Req;
        if (req instanceof EmptyMessage) {
            messageBodys.add(req);
            modelIDs.add(String.valueOf(0));
        } else if (req instanceof BoostP3Req) {
            boostP3Req = (BoostP3Req) req;
            for (int i = 0; i < boostP3Req.getWorkerNum(); i++) {
                messageBodys.add(req);
                modelIDs.add(String.valueOf(i));
            }
        }
        splitResult.setMessageBodys(messageBodys);
        splitResult.setModelIDs(modelIDs);
        splitResult.setReduceType(ReduceType.needMerge);
        return splitResult;
    }

    /**
     * 阶段4更新查询表任务拆分
     * 根据最优分裂点是否是分布式模式，判断是否需要拆分，并构造相应请求。
     *
     * @param req 请求
     * @return 完成拆分的请求
     */
    private SplitResult mapPhase4(Message req) {
        SplitResult splitResult = new SplitResult();
        List<String> modelIDs = new ArrayList<>();
        List<Message> messageBodys = new ArrayList<>();
        BoostP4Req boostP4Req = (BoostP4Req) req;
        if (boostP4Req.isAccept()) {
            for (int i = 0; i < boostP4Req.getWorkerNum(); i++) {
                messageBodys.add(req);
                modelIDs.add(String.valueOf(i));
            }
        } else {
            messageBodys.add(req);
            modelIDs.add(String.valueOf(0));
        }
        splitResult.setMessageBodys(messageBodys);
        splitResult.setModelIDs(modelIDs);
        splitResult.setReduceType(ReduceType.needMerge);
        return splitResult;
    }


    /***
     * 对各任务运行结果进行合并，以支撑后续训练任务
     *
     * @param phase 训练阶段
     * @param result  响应列表
     * @return 合并结果
     */
    public Message merge(int phase, List<Message> result) {
        try {
            if (phase == 1) {
                return reducePhase1(result);
            } else if (phase == 2) {
                return reducePhase2(result);
            } else if (phase == 3) {
                return reducePhase3(result);
            } else if (phase == 4) {
                return reducePhase4(result);
            } else if (phase == 5) {
                return reducePhase5(result);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            throw new NotMatchException(e);
        }
        return result.get(0);
    }

    /***
     * 阶段1未进行拆分，仅一个map，此处直接返回即可。
     *
     * @param result map运行结果列表
     * @return 合并结果
     */
    private static Message reducePhase1(List<Message> result) {
        EncryptedGradHess res0 = (EncryptedGradHess) result.get(0);
        return res0;
    }

    /***
     * 阶段2被动方map任务计算结果合并：
     * 将各任务的结果FeatureLeftGH[]进行合并，返回被动方全部特征各特征及其对应分裂阈值列表
     * @param result 各map任务计算结果
     * @return 全部特征的分裂阈值
     */
    private static Message reducePhase2(List<Message> result) {
        BoostP2Res res0 = (BoostP2Res) result.get(0);
        if (res0.getFeatureGL() == null) {
            return res0;
        }
        FeatureLeftGH[] featureLeftGHS = res0.getFeatureGL();
        for (int i = 1; i < result.size(); i++) {
            BoostP2Res boostP2Res = (BoostP2Res) result.get(i);
            FeatureLeftGH[] featureLeftGHS1 = boostP2Res.getFeatureGL();
            featureLeftGHS = ArrayUtils.addAll(featureLeftGHS, featureLeftGHS1);
        }
        return new BoostP2Res(featureLeftGHS);
    }


    /***
     * 阶段3主动方map任务计算结果合并
     * 比较各任务返回的本地最优分裂节点，返回最优分裂节点。
     * @param results 各map任务计算结果
     * @return 最优分裂节点
     */
    private static Message reducePhase3(List<Message> results) {
        BoostP3Res res0 = (BoostP3Res) results.get(0);
        if (res0.getClient() == null && res0.getFeature() == null) {
            return res0;
        }
        double maxGain = res0.getGain();
        for (Message result : results) {
            BoostP3Res boostP3Res = (BoostP3Res) result;
            if (boostP3Res.getGain() > maxGain) {
                res0 = boostP3Res;
            }
        }
        return res0;
    }

    /***
     * 阶段4map任务结果合并
     *
     * @param result 各map任务结果
     * @return 完成查询表更新的左子树信息
     */
    private static Message reducePhase4(List<Message> result) {
        LeftTreeInfo res0 = (LeftTreeInfo) result.get(0);
        for (Message message : result) {
            LeftTreeInfo leftTreeInfo = (LeftTreeInfo) message;
            if (leftTreeInfo.getLeftInstances() != null) {
                res0 = leftTreeInfo;
            }
        }
        return res0;
    }


    /***
     * 阶段5map任务结果合并：
     * 由于阶段5无需拆分，故直接返回
     * @param result 各map结果
     * @return 直接返回
     */
    private static Message reducePhase5(List<Message> result) {
        BoostP5Res res0 = (BoostP5Res) result.get(0);
        return res0;
    }


    /***
     * 各任务model中间数据合并
     * 主动方分布式：需要同步的中间结果包括当前分裂节点信息、g和h、树结构、查询表；
     * 被动方分布式：需要同步的中间结果包括最新树结构、查询表；
     * @param models 模型列表
     * @return 完成合并的模型列表
     */
    public List<Model> mergeModel(List<Model> models) {
        if (models.size() == 0) {
            return models;
        }
        DistributedFederatedGBModel gbModel = ((DistributedFederatedGBModel) models.get(0));
        TreeNode currentNode = null;
        ClientInfo clientInfo = null;
        int splitFeature = 0;
        double gain = 0;
        double[][] grad = gbModel.grad;
        double[][] hess = gbModel.hess;
        String privateKey = null;
        String keyPublic = null;
        List<QueryEntry> passiveQueryTable = gbModel.passiveQueryTable;
        for (Model value : models) {
            DistributedFederatedGBModel model = (DistributedFederatedGBModel) value;
            if (privateKey == null) {
                privateKey = model.privateKeyString;
                keyPublic = model.publicKeyString;
            }
            if (model.encryptedG != null) {
                currentNode = model.currentNode;
                grad = model.grad;
                hess = model.hess;
            }
            if (model.currentNode != null && model.currentNode.gain > gain) {
                gain = model.currentNode.gain;
                clientInfo = model.currentNode.client;
                splitFeature = model.currentNode.splitFeature;
            }
            if ((passiveQueryTable == null || model.passiveQueryTable != null) && (passiveQueryTable != null && model.passiveQueryTable.size() > passiveQueryTable.size())) {
                passiveQueryTable = model.passiveQueryTable;
            }
        }
        for (Model model : models) {
            DistributedFederatedGBModel model1 = (DistributedFederatedGBModel) model;
            model1.privateKeyString = privateKey;
            model1.publicKeyString = keyPublic;
            model1.passiveQueryTable = passiveQueryTable;
            model1.currentNode = currentNode;
            // 只有主动方分布式需要更新这些参数
            if (currentNode != null && clientInfo != null) {
                model1.currentNode.client = clientInfo;
                model1.currentNode.splitFeature = splitFeature;
                model1.currentNode.gain = gain;
                model1.grad = grad;
                model1.hess = hess;
            }
        }
        return models;
    }

    /**
     * 判断并筛选uid数据是否存在（可预测）
     *
     * @param uidList            用户输入的，需要推理的样本id列表
     * @param inferenceCacheFile 根据用户输入的id 加载的样本数据
     * @param others             自定义参数
     * @return 初始化中间信息 包含isAllowList 还有uids两部分信息
     */
    public Message inferenceInit(String[] uidList, String[][] inferenceCacheFile, Map<String, Object> others) {
        return InferenceFilter.filter(uidList, inferenceCacheFile);
    }

    /**
     * @param inferenceData 特征列表
     * @return map
     */
    public Message inference(int phase, Message jsonData, InferenceData inferenceData) {
        if (phase == -1) {
            StringArray parameterData = (StringArray) jsonData;
            return inferencePhase1(parameterData, inferenceData, this.trees, this.firstRoundPred, this.multiClassUniqueLabelList);
        } else if (phase == -2) {
            return inferencePhase2(jsonData, inferenceData, this.passiveQueryTable);
        } else {
            throw new UnsupportedOperationException("unsupported phase:" + phase);
        }
    }

    /**
     * @param data                      从coordinator传入的迭代数据
     * @param samples                   需要推理的样本
     * @param trees                     训练过程中建好的树
     * @param firstRoundPred            训练过程中生成的初始化预测值
     * @param multiClassUniqueLabelList 多分类类别标记
     * @return 推理中间结果
     */
    public Message inferencePhase1(StringArray data, InferenceData samples, List<Tree> trees, double firstRoundPred, List<Double> multiClassUniqueLabelList) {
        //判断是否有label的客户端，如果是，返回查询树
        //此处的index是基于initial 请求的uid

        String[] newUidIndex = data.getData();
        CommonInferenceData boostInferenceData = (CommonInferenceData) samples;
        boostInferenceData.filterOtherUid(newUidIndex);
        if (!trees.isEmpty()) {
            return new BoostN1Res(trees, firstRoundPred, multiClassUniqueLabelList);
        }
        return new BoostN1Res();
    }


    public Message inferencePhase2(Message jsonData, InferenceData inferenceData, List<QueryEntry> queryTable) {
        if (jsonData == null) {
            return null;
        }
        //three element each line, is uid,treeIndex,recordId
        int[][] reqBody = ((Int2dArray) (jsonData)).getData();

        if (reqBody == null || reqBody.length == 0) {
            return new Int2dArray();
        }

        double[][] featuresList = inferenceData.getSample();
//        int[] fakeIdIndex = inferenceData.getFakeIdIndex();

        List<int[]> bodies = Arrays.stream(reqBody)
                .map(l -> {
                    int uid = l[0];
                    int treeIndex = l[1];
                    int recordId = l[2];
                    QueryEntry x = queryTable.get(recordId - 1);
                    int row = uid;
                    double[] line = featuresList[row];
                    double featureValue = line[x.getFeatureIndex() - 1];
                    // 1 is left, 2 is right
                    if (featureValue > x.getSplitValue()) {
                        return new int[]{uid, treeIndex, 2};
                    } else {
                        return new int[]{uid, treeIndex, 1};
                    }
                }).collect(toList());
        return new Int2dArray(bodies);
    }

    public String serialize() {
        FgbModelSerializer fgbModelSerializer = new FgbModelSerializer(trees, loss, firstRoundPred, eta, passiveQueryTable, multiClassUniqueLabelList);
        return fgbModelSerializer.saveModel();
    }

    public void deserialize(String content) {
        FgbModelSerializer fgbModel = new FgbModelSerializer(content);
        this.trees = fgbModel.getTrees();
        this.loss = fgbModel.getLoss();
        this.firstRoundPred = fgbModel.getFirstRoundPred();
        this.eta = fgbModel.getEta();
        this.passiveQueryTable = fgbModel.getPassiveQueryTable();
        this.multiClassUniqueLabelList = fgbModel.getMultiClassUniqueLabelList();
    }

    public AlgorithmType getModelType() {
        return AlgorithmType.FederatedGB;
    }

}
