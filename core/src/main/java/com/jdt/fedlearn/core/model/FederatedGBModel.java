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
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.base.Int2dArray;
import com.jdt.fedlearn.core.entity.base.StringArray;
import com.jdt.fedlearn.core.model.common.loss.Loss;
import com.jdt.fedlearn.core.model.common.loss.SquareLoss;
import com.jdt.fedlearn.core.model.common.loss.crossEntropy;
import com.jdt.fedlearn.core.model.common.tree.Tree;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.loader.common.InferenceData;
import com.jdt.fedlearn.core.model.serialize.FgbModelSerializer;
import com.jdt.fedlearn.core.parameter.SuperParameter;
import com.jdt.fedlearn.core.preprocess.InferenceFilter;
import com.jdt.fedlearn.core.psi.MappingResult;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.core.type.FirstPredictType;
import com.jdt.fedlearn.core.type.ObjectiveType;
import com.jdt.fedlearn.core.type.data.DoubleTuple2;
import com.jdt.fedlearn.core.type.data.StringTuple2;
import com.jdt.fedlearn.core.entity.boost.Bucket;
import com.jdt.fedlearn.core.loader.boost.BoostTrainData;
import com.jdt.fedlearn.core.math.MathExt;
import com.jdt.fedlearn.core.parameter.FgbParameter;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.data.Tuple2;
import com.jdt.fedlearn.core.util.Tool;
import com.jdt.fedlearn.core.model.common.loss.LogisticLoss;
import com.jdt.fedlearn.core.model.common.tree.TreeNode;
import com.jdt.fedlearn.core.exception.NotImplementedException;
import com.jdt.fedlearn.core.loader.common.TrainData;
import com.jdt.fedlearn.core.loader.boost.BoostInferenceData;
import com.jdt.fedlearn.core.metrics.Metric;

import com.jdt.fedlearn.core.type.*;
import com.jdt.fedlearn.core.entity.boost.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

/**
 * TODO:items(sortedFeatureMap currentNode passiveQueryTable trees...)
 * should save as map(trainID, item) for train parallel
 */
public class FederatedGBModel implements Model {
    private static final Logger logger = LoggerFactory.getLogger(FederatedGBModel.class);

    private String trainId;
    private int depth = 0;
    //four col, id,val,grad,hess
    private Map<Integer, List<Bucket>> sortedFeatureMap = new ConcurrentHashMap<>();
    private TreeNode currentNode;
    private LinkedHashMap<Integer, QueryEntry> passiveQueryTable = new LinkedHashMap<>();
    private List<Tree> trees = new ArrayList<>();
    private double eta;
    private double firstRoundPred;
    private Loss loss;
    private Queue<TreeNode> newTreeNodes = new LinkedList<>();
    private FgbParameter parameter;
    //private KeyPair keyPair;
    private PrivateKey privateKey;
    private final EncryptionTool encryptionTool = new JavallierTool();
    private PublicKey publicKey;
    private Map<MetricType, List<Double>> metricMap;
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
    private Map<Integer, Tuple2<Ciphertext, Ciphertext>> ghMap2;

    //用于计算全局敏感度
    public double maxg = 1;

    public int contributeFea = 0;

    public FederatedGBModel() {
    }

    public FederatedGBModel(List<Tree> trees, Loss loss, double firstRoundPredict, double eta, LinkedHashMap<Integer, QueryEntry> passiveQueryTable, List<Double> multiClassUniqueLabelList) {
        this.trees = trees;
        this.loss = loss;
        this.firstRoundPred = firstRoundPredict;
        this.eta = eta;
        this.passiveQueryTable = passiveQueryTable;
        this.multiClassUniqueLabelList = multiClassUniqueLabelList;
//        this.contributeFea = contributeFea;
    }

    public FederatedGBModel(FgbModelSerializer serializer) {
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
    public BoostTrainData trainInit(String[][] rawData, String[] uids, int[] testIndex, SuperParameter superParameter, Features features, Map<String, Object> others) {
        Tuple2<String[],String[]> trainTestUId = Tool.splitUid(uids, testIndex);
        String[] trainUids = trainTestUId._1();
        testId = trainTestUId._2();
        BoostTrainData trainData = new BoostTrainData(rawData, trainUids, features, new ArrayList<>());
//        newTreeNodes = new LinkedList<>();
        //TODO 修改指针
        parameter = (FgbParameter) superParameter;
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
        //有label 的客户端生成公私钥对
        if (trainData.hasLabel) {
            double initSumLoss = parameter.isMaximize() ? (-Double.MAX_VALUE) : Double.MAX_VALUE;
//            double[] tmpRoundMetric = new double[parameter.getNumBoostRound() + 1];
//            Arrays.fill(tmpRoundMetric, initSumLoss);
            List<Double> tmpRoundMetric = new ArrayList<>();
            tmpRoundMetric.add(initSumLoss);
            metricMap = Arrays.stream(parameter.getEvalMetric()).collect(Collectors.toMap(metric -> metric, metric -> new ArrayList<>(tmpRoundMetric)));
            this.hasLabel = true;
            this.privateKey = encryptionTool.keyGenerate(parameter.getBitLength().getBitLengthType(), 64);
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
     *
     * @param phase         训练阶段
     * @param jsonData
     * @param train
     * @return
     */
    public Message train(int phase, Message jsonData, TrainData train) {
        BoostTrainData trainData = (BoostTrainData) train;
        switch (phase) {
            case 1:
                return trainPhase1(jsonData, trainData);
            case 2:
                return trainPhase2(jsonData, trainData, encryptionTool);
            case 3: {
                Tuple2<BoostP3Res, Double> req = trainPhase3(jsonData, trainData, currentNode, encryptionTool, privateKey,
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
                Tuple2<LeftTreeInfo, LinkedHashMap<Integer, QueryEntry>> req = trainPhase4(jsonData, sortedFeatureMap, passiveQueryTable);
                passiveQueryTable = req._2();
                return req._1();
            }
            case 5: {
                if (this.hasLabel) {
                    LeftTreeInfo lfi = (LeftTreeInfo) jsonData;
                    currentNode.recordId = lfi.getRecordId();
                }
                ArrayList res = trainPhase5(jsonData, grad, hess, numClassRound, currentNode, newTreeNodes,
                        parameter, correspondingTreeNode, metricMap, trees, loss, datasetSize, pred, label, depth);
                // List of <boostP5Res, numClassRound, correspondingTreeNode, p, g, h, depth>
                numClassRound = (int) res.get(1);
                correspondingTreeNode = (TreeNode[]) res.get(2);
                pred = (double[][]) res.get(3);
                grad = (double[][]) res.get(4);
                hess = (double[][]) res.get(5);
                depth = (int) res.get(6);
                metricMap = (Map<MetricType, List<Double>>) res.get(7);
                currentNode = (TreeNode) res.get(8);
                return (BoostP5Res) res.get(0);
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

    private void treeInit(BoostTrainData trainData) {
        //先检测是否第一次请求，需要初始化很多参数
        newTreeNodes = new LinkedList<>();
        //对于有label的一方，新一颗树的建立
        if (parameter.getNumClass() > 1) {
            logger.info("Train Round " + (trees.size() / parameter.getNumClass() + 1) + " at class " + (numClassRound + 1) + "/" + parameter.getNumClass());
        }
        //新建树和根节点
        TreeNode root = new TreeNode(1, 1, trainData.getFeatureDim(), false);
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
            return new EncryptedGradHess(null, null);
        }

        // 初始化的过程每个client都执行
        // 初始化分为全局初始化和每棵树的初始化
        // 加密g和h
        if (req.isNewTree()) {
            treeInit(trainSet);
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
        if (req.isNewTree()) {
            StringTuple2[] encryptedArray = Arrays.stream(instanceSpace).parallel().mapToObj(x -> new StringTuple2(encryptedG.get(x), encryptedH.get(x))).toArray(StringTuple2[]::new);
            String pk = privateKey.generatePublicKey().serialize();
            return new EncryptedGradHess(req.getClient(), instanceSpace, encryptedArray, pk, true);
        } else {
            return new EncryptedGradHess(req.getClient(), instanceSpace);
        }
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
            return new BoostP2Res(null);
        }
        // jsonData is output from phase1
        EncryptedGradHess req = (EncryptedGradHess) (jsonData);
        // get instance information from the processing node
        int[] instanceSpace = req.getInstanceSpace();
        // initialize publickKey with null
        if (req.getNewTree()) {
            // if new tree, get Grad Hess.
            StringTuple2[] gh = req.getGh();
            // if new tree, get publicKey.
            String pubKey = req.getPubKey();
            publicKey = encryptionTool.restorePublicKey(pubKey);
            // instance index i - (g_i, h_i)
            ghMap2 = new HashMap<>();
            for (int i = 0; i < instanceSpace.length; i++) {
                ghMap2.put(instanceSpace[i], new Tuple2<>(encryptionTool.restoreCiphertext(gh[i].getFirst()), encryptionTool.restoreCiphertext(gh[i].getSecond())));
            }
        }
        //TODO 根据样本空间查询
//        ColSampler colSampler = trainSet.getColSampler();
        //对每一列（特征），每个特征计算gl hl
        //忽略column 0 ，col-0 是用户id
        PublicKey finalPublicKey = publicKey;
        // feature从1开始，不是从0开始
        // TODO 为什么会从这里print出来很多的is true??
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
                    return new FeatureLeftGH(req.getClient(),"" + col, full);
                })
                .toArray(FeatureLeftGH[]::new);
        BoostP2Res boostP2Res = new BoostP2Res(bodyArray);
        return boostP2Res;
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
     * @param buckets feature buckets
     * @param ghMap2 instance index i,  (g_i, h_i)
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
     * @param buckets
     * @param ghMap: (index, (g, h))
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
     * @param jsonData 迭代数据
     * @param trainSet BoostTrainData
     * @return BoostP3Res
     */
    public Tuple2<BoostP3Res, Double> trainPhase3(Message jsonData, BoostTrainData trainSet, TreeNode currentNode,
                                   EncryptionTool encryptionTool, PrivateKey privateKey,
                                   FgbParameter parameter, double[][] grad, double[][] hess,
                                   int numClassRound) {
        //不含label的客户端无需处理
        if (!trainSet.hasLabel) {
            return new Tuple2<>(new BoostP3Res(), null);
        }
        BoostP3Req req = (BoostP3Req) (jsonData);
        double g = currentNode.Grad; //G_Total: the gradient sum of the samples fall into this tree node
        double h = currentNode.Hess; //H_Total: the hessian sum of the samples fall into this tree node
        ClientInfo client = null;
        String feature = "";
        int splitIndex = 0;
//        double gain = -Double.MAX_VALUE;

        //遍历属于其他方的加密特征
        //先将各个客户端的数据拆分成以特征为维度，然后对该特征计算最大gain，然后取所有特征得gain的最大值
        GainOutput gainOutput = req.getDataList()
                .parallelStream()
                .flatMap(Collection::stream)
                .map(x -> fetchGain(x, g, h, encryptionTool, privateKey, parameter))
                .max(Comparator.comparing(GainOutput::getGain))
                .get();
        // Corresponding client, feature, splitIndex for the best gain
        double gain = gainOutput.getGain();
        client = gainOutput.getClient();
        feature = gainOutput.getFeature();
        splitIndex = gainOutput.getSplitIndex();
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
                feature = "" + col;
                splitIndex = featureMaxGain.get()._2();
            }
        }
        //根据差分隐私指数机制随机选取一个分裂点
        Tuple2<BoostP3Res, Double> t = new Tuple2<BoostP3Res, Double>(new BoostP3Res(client, feature, splitIndex), gain);
        return t;
    }

    /**
     * Compute gain according XGBoost algorithm
     * @param decryptedGH: GL and HL at this node at each threshold
     * @param g: G_total at this node
     * @param h: H_total at this node
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
     * @param input FeatureLeftGH from phase2 output
     * @param g G_Total at this node
     * @param h H_Total at this node
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
     * @param jsonData
     * @param sortedFeatureMap ( feature id, feature buckets)
     * @param passiveQueryTable
     * @return
     */
    public Tuple2<LeftTreeInfo, LinkedHashMap<Integer, QueryEntry>> trainPhase4(Message jsonData, Map<Integer, List<Bucket>> sortedFeatureMap,
                                     LinkedHashMap<Integer, QueryEntry> passiveQueryTable) {
//        Map<Integer, List<Bucket>> sortedFeatureMap,
//        LinkedHashMap<Integer, QueryEntry> passiveQueryTable, int contributeFea
        BoostP4Req req = (BoostP4Req) (jsonData);
        if (!req.isAccept()) {
            Tuple2<LeftTreeInfo, LinkedHashMap<Integer, QueryEntry>> t = new Tuple2<>(new LeftTreeInfo(0, null), passiveQueryTable);
            return t;
        }
        //本次要分裂的特征
        int featureIndex = req.getkOpt();
        //根据phase3 的训练结果 生成的分裂点，并不是直接的数值，而是在原始给出的分裂选项中的第i个选项
        int splitIndex = req.getvOpt();
        List<Bucket> sortedFeature2 = sortedFeatureMap.get(featureIndex); // 最佳特征分桶
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
                leftIns.add(new Double(id).intValue());
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
                    leftIns.add(new Double(id).intValue());
                }
            }
        }
        int recordId = 1;
        if (passiveQueryTable == null || passiveQueryTable.size() == 0) {
            QueryEntry entry = new QueryEntry(recordId, featureIndex, splitValue); // 新建查询表条目记录分裂特征和分裂值
            passiveQueryTable = new LinkedHashMap<>();
            passiveQueryTable.put(recordId, entry);
        } else {
            Map.Entry<Integer, QueryEntry> lastLine = Tool.getTail(passiveQueryTable); // 当前查询表的最后一行
            assert lastLine != null;
            recordId = lastLine.getKey() + 1;
            QueryEntry line = new QueryEntry(recordId, featureIndex, splitValue);
            passiveQueryTable.put(recordId, line);
        }
        int[] left = Tool.list2IntArray(leftIns);
        Tuple2<LeftTreeInfo, LinkedHashMap<Integer, QueryEntry>> t = new Tuple2<>(new LeftTreeInfo(recordId, left), passiveQueryTable);
        return t;
    }

    //build full query index and update prediction and grad

    /**
     * 有label的client收到了各个client的本次树的分裂信息和左子树样本id list，更新查询树并进行下一轮迭代
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
    public ArrayList trainPhase5(Message jsonData, double[][] grad, double[][] hess,
                                  int numClassRound, TreeNode currentNode,
                                  Queue<TreeNode> newTreeNodes, FgbParameter parameter,
                                  TreeNode[] correspondingTreeNode, Map<MetricType, List<Double>> metricMap,
                                  List<Tree> trees, Loss loss, int datasetSize, double[][] pred, double[] label, int depth) {

        // Update了pred, grad, hess三个全局变量， 由updateGH来update
        // update了currentNode.recordId
        // update了numClassRound
        // update了correspondingTreeNode，postPrune
        // update depth
        // update metricMap
        // todo 将datasetSize也放入入参
        //无label的客户端直接返回
        ArrayList res = new ArrayList();

        if (!this.hasLabel) {
            // List of <boostP5Res, numClassRound, correspondingTreeNode, p,g,h, depth>
            res = addElement(res, new BoostP5Res(false, 0), numClassRound,
                    correspondingTreeNode, pred, grad, hess, depth, metricMap, currentNode);
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
    // method in the if(currentTree.getAliveNodes().isEmpty()) where isStop = true
    private ArrayList emptyTreeUpdate(Tree currentTree, FgbParameter parameter, TreeNode[] correspondingTreeNode,
                                      int depth, int datasetSize, double[] label, int numClassRound,
                                      double[][] pred, Loss loss, Map<MetricType, List<Double>> metricMap, ArrayList res) {
        postPrune(currentTree.getRoot(), parameter, correspondingTreeNode);
        BoostP5Res boostP5Res = new BoostP5Res(true, depth);
        ArrayList pgh = updateGH(datasetSize, correspondingTreeNode, label, numClassRound, pred, parameter, loss);
        if (numClassRound + 1 == parameter.getNumClass()) {
            assert metricMap != null;
            Map<MetricType, Double> trainMetric = updateMetric(metricMap, parameter, loss, pred, label);
            assert trainMetric != null;
            // TODO 优化trainMetric.forEach(...);
            trainMetric.forEach((key, value) -> metricMap.get(key).add(value));
            boostP5Res.setTrainMetric(metricMap);
            numClassRound = 0;
        } else {
            numClassRound++;
        }
        // List of <boostP5Res, numClassRound, correspondingTreeNode, pgh_tuple, depth, metricMap>
        res = addElement(res, boostP5Res, numClassRound, correspondingTreeNode,
                (double[][])pgh.get(0), (double[][])pgh.get(1), (double[][])pgh.get(2), depth, metricMap, currentNode);
        assert res.size() == 9;
        return res;
    }

    private ArrayList addElement (ArrayList a, Object ...arg) {
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

    private ArrayList updateGH(int datasetSize, TreeNode[] correspondingTreeNode, double[] label,
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

    private Map<MetricType, Double> updateMetric(Map<MetricType, List<Double>> metricMap,
                                                 FgbParameter parameter, Loss loss, double[][] pred, double[] label) {
        if (!this.hasLabel || metricMap == null) {
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
     * 判断并筛选uid数据是否存在（可预测）
     * @param uidList 用户输入的，需要推理的样本id列表
     * @param inferenceCacheFile 根据用户输入的id 加载的样本数据
     * @param others 自定义参数
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
     *
     * @param data 从coordinator传入的迭代数据
     * @param samples 需要推理的样本
     * @param trees 训练过程中建好的树
     * @param firstRoundPred 训练过程中生成的初始化预测值
     * @param multiClassUniqueLabelList 多分类类别标记
     * @return 推理中间结果
     */
    public Message inferencePhase1(StringArray data, InferenceData samples, List<Tree> trees, double firstRoundPred, List<Double> multiClassUniqueLabelList) {
        //判断是否有label的客户端，如果是，返回查询树
        //此处的index是基于initial 请求的uid

        String[] newUidIndex = data.getData();
        BoostInferenceData boostInferenceData = (BoostInferenceData)samples;
        boostInferenceData.filterOtherUid(newUidIndex);
        if (!trees.isEmpty()) {
            return new BoostN1Res(trees, firstRoundPred, multiClassUniqueLabelList);
        }
        return new BoostN1Res();
    }


    public Message inferencePhase2(Message jsonData, InferenceData inferenceData, LinkedHashMap<Integer, QueryEntry> queryTable) {
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
                    QueryEntry x = queryTable.get(recordId);
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

    public AlgorithmType getModelType(){
        return AlgorithmType.FederatedGB;
    }
}
