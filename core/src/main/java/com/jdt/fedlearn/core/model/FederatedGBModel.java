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


import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;
import com.jdt.fedlearn.core.encryption.common.Ciphertext;
import com.jdt.fedlearn.core.encryption.common.EncryptionTool;
import com.jdt.fedlearn.core.encryption.common.PrivateKey;
import com.jdt.fedlearn.core.encryption.common.PublicKey;
import com.jdt.fedlearn.core.encryption.javallier.JavallierTool;
import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.core.entity.base.Int2dArray;
import com.jdt.fedlearn.core.entity.base.StringArray;
import com.jdt.fedlearn.core.entity.common.MetricValue;
import com.jdt.fedlearn.core.entity.distributed.SplitResult;
import com.jdt.fedlearn.core.loader.common.CommonInferenceData;
import com.jdt.fedlearn.core.loader.randomForest.RFTrainData;
import com.jdt.fedlearn.core.model.common.loss.Loss;
import com.jdt.fedlearn.core.model.common.loss.SquareLoss;
import com.jdt.fedlearn.core.model.common.loss.crossEntropy;
import com.jdt.fedlearn.core.model.common.tree.Tree;
import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.common.entity.core.feature.Features;
import com.jdt.fedlearn.core.loader.common.InferenceData;
import com.jdt.fedlearn.core.model.serialize.FgbModelSerializer;
import com.jdt.fedlearn.core.parameter.HyperParameter;
import com.jdt.fedlearn.core.preprocess.InferenceFilter;
import com.jdt.fedlearn.core.preprocess.Scaling;
import com.jdt.fedlearn.core.type.*;
import com.jdt.fedlearn.core.type.data.DoubleTuple2;
import com.jdt.fedlearn.core.type.data.Pair;
import com.jdt.fedlearn.core.type.data.StringTuple2;
import com.jdt.fedlearn.core.entity.boost.Bucket;
import com.jdt.fedlearn.core.loader.boost.BoostTrainData;
import com.jdt.fedlearn.core.math.MathExt;
import com.jdt.fedlearn.core.parameter.FgbParameter;
import com.jdt.fedlearn.core.type.data.Tuple2;
import com.jdt.fedlearn.core.util.Tool;
import com.jdt.fedlearn.core.model.common.loss.LogisticLoss;
import com.jdt.fedlearn.core.model.common.tree.TreeNode;
import com.jdt.fedlearn.core.exception.NotImplementedException;
import com.jdt.fedlearn.core.loader.common.TrainData;
import com.jdt.fedlearn.core.metrics.Metric;

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
    private PrivateKey privateKey;
    private final EncryptionTool encryptionTool = new JavallierTool();
    private PublicKey publicKey;
    private MetricValue metricValue;
    private int numClassRound = 0;
    public boolean hasLabel = false;
    public int datasetSize;
    public double[] label;

    public TreeNode[] correspondingTreeNode; // ???????????????datapoint?????????Treenode
    public double[][] pred;
    // new double[parameter.getNumClass()][datasetSize]
    public double[][] grad;
    public double[][] hess;
    private List<String> encryptedG;
    private List<String> encryptedH;
    private String[] testId;

    private List<Double> multiClassUniqueLabelList = new ArrayList<>();

    //phase2 ??????
    private Map<Integer, Tuple2<Ciphertext, Ciphertext>> ghMap2;

    //???????????????????????????
    public double maxg = 1;

    public int contributeFea = 0;

    // ??????label?????????????????????
    private Scaling scaling;
    // ??????????????????
    private int numFeature = 1;

    private boolean gotNumFeature = false;

    private List<String> expressions = new ArrayList<>();

    public FederatedGBModel() {
    }

    public FederatedGBModel(List<Tree> trees, Loss loss, double firstRoundPredict, double eta, List<QueryEntry> passiveQueryTable, List<Double> multiClassUniqueLabelList) {
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
     * ???????????????????????????trainId, label, datasetSize, pred, grad, hess, correspondingTreeNode, eta
     * ???????????????????????????????????????privateKey, loss, firstRoundPred
     */
    public BoostTrainData trainInit(String[][] rawData, String[] uids, int[] testIndex, HyperParameter hyperParameter, Features features, Map<String, Object> others) {
        Tuple2<String[], String[]> trainTestUId = Tool.splitUid(uids, testIndex);
        String[] trainUids = trainTestUId._1();
        testId = trainTestUId._2();
        BoostTrainData trainData = new BoostTrainData(rawData, trainUids, features, new ArrayList<>());
        this.expressions = trainData.getExpressions();
        newTreeNodes = new LinkedList<>();
        //TODO ????????????
        parameter = (FgbParameter) hyperParameter;
        //?????????????????????gradient hessian
        logger.info("actual received features:" + features.toString());
        logger.info("client data dim:" + trainData.getDatasetSize() + "," + trainData.getFeatureDim());
        // ????????? 1 . ???????????????????????????????????????
        // TODO????????????????????? parameter ?????????????????????
        if (!ObjectiveType.multiSoftmax.equals(parameter.getObjective()) && !ObjectiveType.multiSoftProb.equals(parameter.getObjective())) {
            parameter.setNumClass(1);
        }
        this.label = trainData.getLabel();
        this.datasetSize = trainData.getDatasetSize();
        this.pred = new double[parameter.getNumClass()][datasetSize];
        this.grad = new double[parameter.getNumClass()][datasetSize];
        this.hess = new double[parameter.getNumClass()][datasetSize];
        this.correspondingTreeNode = new TreeNode[datasetSize];
        //???label ??????????????????????????????
        if (trainData.hasLabel) {
            this.scaling = new Scaling();
            double initSumLoss = parameter.isMaximize() ? (-Double.MAX_VALUE) : Double.MAX_VALUE;
            List<Pair<Integer, Double>> tmpRoundMetric = new ArrayList<>();
            tmpRoundMetric.add(new Pair<>(0, initSumLoss));
            Map<MetricType, List<Pair<Integer, Double>>> metricMap = Arrays.stream(parameter.getEvalMetric())
                    .collect(Collectors.toMap(metric -> metric, metric -> new ArrayList<>(tmpRoundMetric)));
            metricValue = new MetricValue(metricMap);
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

    // TODO ??????trainPhase1, trainPhase2, ??????????????????

    /**
     * @param phase    ????????????
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

    @Override
    public SplitResult split(int phase, Message req) {
        switch (phase) {
            case 1:
                //????????????????????????????????????????????????
                SplitResult splitResult = new SplitResult();
                splitResult.setMessageBodys(Collections.singletonList(req));
                return splitResult;
            case 2:
//                EncryptedGradHess encryptedGradHess = (EncryptedGradHess)req;

                return null;
            case 3: {
                return null;
            }
            case 4: {
                return null;
            }
            case 5: {
                return new SplitResult();
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
                this.firstRoundPred = Math.log(MathExt.average(this.label));//TODO ?????????????????????
            } else if (FirstPredictType.RANDOM.equals(parameter.getFirstRoundPred())) {
                this.firstRoundPred = Math.log(Math.random());//TODO ????????????
            }
            return firstRoundPred;
        }
        if (FirstPredictType.ZERO.equals(parameter.getFirstRoundPred())) {
            this.firstRoundPred = 0.0;
        } else if (FirstPredictType.AVG.equals(parameter.getFirstRoundPred())) {
            this.firstRoundPred = MathExt.average(this.label);//TODO ?????????????????????
        } else if (FirstPredictType.RANDOM.equals(parameter.getFirstRoundPred())) {
            this.firstRoundPred = Math.random();//TODO ????????????
        }
        return firstRoundPred;
    }

    private void treeInit(BoostTrainData trainData) {
        //????????????????????????????????????????????????????????????
        newTreeNodes = new LinkedList<>();
        //?????????label?????????????????????????????????
        if (parameter.getNumClass() > 1) {
            logger.info("Train Round " + (trees.size() / parameter.getNumClass() + 1) + " at class " + (numClassRound + 1) + "/" + parameter.getNumClass());
        }
        //?????????????????????
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
     * ????????????server????????????????????????????????????label???client????????????????????????predict????????????predict??????g???h
     * ????????????predict??????????????????predict??????g???h
     * ????????????gradient ??? hessian???
     * ????????????g???h???????????????????????????????????????
     *
     * @param jsonData ??????
     * @param trainSet ?????????
     * @return json ?????????????????????
     */
    public EncryptedGradHess trainPhase1(Message jsonData, BoostTrainData trainSet) {
        BoostP1Req req = (BoostP1Req) (jsonData);
        //???label????????????????????????
        if (!trainSet.hasLabel) {
            return new EncryptedGradHess(null, null);
        }

        // ????????????????????????client?????????
        // ??????????????????????????????????????????????????????
        // ??????g???h
        if (req.isNewTree()) {
            treeInit(trainSet);
            encryptedG = Arrays.stream(grad[numClassRound]).parallel().mapToObj(g -> (encryptionTool.encrypt(g, privateKey.generatePublicKey()))).map(Ciphertext::serialize).collect(toList());
            encryptedH = Arrays.stream(hess[numClassRound]).parallel().mapToObj(h -> encryptionTool.encrypt(h, privateKey.generatePublicKey())).map(Ciphertext::serialize).collect(toList());
        }

        // ???label??????????????????g ??? h(?????????g???h??????????????????????????????????????????phase5??????)
        //TODO ?????????????????????????????????????????????????????????????????????
        //????????????tree????????????????????????????????????tree????????? alive nodes????????????????????????????????????????????????current node
        Tree currentTree = trees.get(trees.size() - 1);
        currentNode = currentTree.getAliveNodes().poll();
        assert currentNode != null;
        // ?????????????????????????????????????????????
        int[] instanceSpace = currentNode.instanceSpace;

        // ???g???h??????????????????????????????????????????
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
     * client?????????g???h??????????????????gl???hl
     * ?????????json data????????????encrypted gl ??? hl
     *
     * @param jsonData ????????????
     * @param trainSet ????????????
     * @return ????????????
     */
    // TODO ???ghMap2????????????
    public BoostP2Res trainPhase2(Message jsonData, BoostTrainData trainSet, EncryptionTool encryptionTool) {
        //???label????????????????????????
        if (trainSet.hasLabel) {
            BoostP2Res res = new BoostP2Res(null);
            res.setTrainMetric(metricValue);
            return res;
        }
        // jsonData is output from phase1
        EncryptedGradHess req = (EncryptedGradHess) (jsonData);
        // get instance information from the processing node
        int[] instanceSpace = req.getInstanceSpace();
        // initialize publicKey with null
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
        //TODO ????????????????????????
//        ColSampler colSampler = trainSet.getColSampler();
        //?????????????????????????????????????????????gl hl
        //??????column 0 ???col-0 ?????????id
        PublicKey finalPublicKey = publicKey;
        // feature???1??????????????????0??????
        // TODO ?????????????????????print???????????????is true??
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
                    return new FeatureLeftGH(req.getClient(), "" + col, full);
                })
                .toArray(FeatureLeftGH[]::new);
        return new BoostP2Res(bodyArray);
    }

    /**
     * ????????????feature??????????????????feature bucket
     */
    public List<Bucket> sortAndGroup(double[][] mat, int numbin) {
        //2????????????instanceId????????????????????????
        //TODO ???????????????????????????
        Arrays.sort(mat, Comparator.comparingDouble(a -> a[1]));
        //??????: get bucket info for "Left instance"
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
                            // ??????g h
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
     * ???????????????gl hl?????????label??????????????????gl hl?????? SplitFinding???????????????(i,k,v)?????????
     *
     * @param jsonData ????????????
     * @param trainSet BoostTrainData
     * @return BoostP3Res
     */
    public Tuple2<BoostP3Res, Double> trainPhase3(Message jsonData, BoostTrainData trainSet, TreeNode currentNode,
                                                  EncryptionTool encryptionTool, PrivateKey privateKey,
                                                  FgbParameter parameter, double[][] grad, double[][] hess,
                                                  int numClassRound) {
        //??????label????????????????????????
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
        // ??????????????????
        if(!gotNumFeature){
            numFeature = req.getDataList().parallelStream().map(List::size).reduce(0, Integer::sum);
            numFeature += trainSet.getFeatureDim() - 1;
            gotNumFeature = true;
        }
        List<GainOutput> allGain = req.getDataList()
                .parallelStream()
                .flatMap(Collection::stream)
                .map(x -> fetchGain(x, g, h, encryptionTool, privateKey, parameter))
                .collect(toList());;
        //????????????????????????????????????
        //??????????????????????????????????????????????????????????????????????????????????????????gain???????????????????????????gain????????????
        GainOutput gainOutput = allGain
                .parallelStream()
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
        //????????????????????????????????????
        //??????column 0 ???col 0 ?????????id
        int[] instanceSpace = currentNode.instanceSpace;
        Map<Integer, DoubleTuple2> ghMap = new HashMap<>();
        for (int ins : instanceSpace) {
            ghMap.put(ins, new DoubleTuple2(grad[numClassRound][ins], hess[numClassRound][ins]));
        }
        for (int col = 1; col <= trainSet.getFeatureDim(); col++) {
            double[][] rawFeature = trainSet.getFeature(instanceSpace, col);
            //TODO ??? sortAndGroup ????????????????????????????????????
            List<Bucket> buckets = sortAndGroup(rawFeature, parameter.getNumBin());
            //TODO ?????????????????????buckets????????????
            sortedFeatureMap.put(col, buckets);
            // body: BucketIndex -> (gL, hL)
            DoubleTuple2[] body = processEachNumericFeature2(buckets, ghMap); // ghMap: index -> (g,h)
            List<Tuple2<Double, Integer>> featureGain = computeGain(body, g, h, parameter);
            Optional<Tuple2<Double, Integer>> featureMaxGain = featureGain.parallelStream().max(Comparator.comparing(Tuple2::_1));
            // ??????????????????split gain?????????client????????????
            if (featureMaxGain.isPresent() && featureMaxGain.get()._1() > gain) {
                gain = featureMaxGain.get()._1();
                client = req.getClient();
                feature = "" + col;
                splitIndex = featureMaxGain.get()._2();
            }
        }
        Tuple2<BoostP3Res, Double> t = new Tuple2<BoostP3Res, Double>(new BoostP3Res(client, feature, splitIndex), gain);
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
        DoubleTuple2[] decryptedGH = decryptGH(input, encryptionTool, privateKey);
        // computeGain returns a list of gain of each split according to the bucket
        // returns the best gain and its index
        Tuple2<Double, Integer> maxGain = computeGain(decryptedGH, g, h, parameter)
                .parallelStream()
                .max(Comparator.comparing(Tuple2::_1))
                .orElse(new Tuple2<>(gain, splitIndex));
        return new GainOutput(input.getClient(), input.getFeature(), maxGain._2(), maxGain._1());
    }

    /**
     * Get all split index and gain from this split for one feature according to Xgboost algorithm
     *
     * @param input          FeatureLeftGH from phase2 output
     * @param g              G_Total at this node
     * @param h              H_Total at this node
     * @param encryptionTool
     * @param privateKey
     * @param parameter
     * @return
     */
    public List<GainOutput> fetchAllGain(FeatureLeftGH input, double g, double h,
                                         EncryptionTool encryptionTool, PrivateKey privateKey, FgbParameter parameter) {
        DoubleTuple2[] decryptedGH = decryptGH(input, encryptionTool, privateKey);
        // computeGain returns a list of gain of each split according to the bucket
        return computeGain(decryptedGH, g, h, parameter)
                .parallelStream()
                .map(x -> new GainOutput(input.getClient(), input.getFeature(), x._2(), x._1()))
                .collect(toList());
    }

    /**
     * decrypt the left g and h
     *
     * @param input          FeatureLeftGH from phase2 output
     * @param encryptionTool
     * @param privateKey
     * @return
     */
    private DoubleTuple2[] decryptGH(FeatureLeftGH input, EncryptionTool encryptionTool, PrivateKey privateKey) {
        // Left G and Left H for one feature at each bucket
        StringTuple2[] tmpGH = input.getGhLeft();
        // decrypt G and H
        return Arrays.asList(tmpGH)
                .parallelStream()
                .map(x -> new DoubleTuple2(encryptionTool.decrypt(x.getFirst(), privateKey), encryptionTool.decrypt(x.getSecond(), privateKey)))
                .toArray(DoubleTuple2[]::new);
    }

    //build sub query index based on <i,k,v>


    /**
     * ??????message?????????message????????????client????????????????????????????????????client???????????????????????????????????????
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
        //????????????????????????
        int featureIndex = req.getkOpt();
        //??????phase3 ??????????????? ?????????????????????????????????????????????????????????????????????????????????????????????i?????????
        int splitIndex = req.getvOpt();
        List<Bucket> sortedFeature2 = sortedFeatureMap.get(featureIndex); // ??????????????????
        double splitValue = sortedFeature2.get(splitIndex).getSplitValue(); // ???????????????????????????
        //?????????????????????????????????????????????????????????????????????????????????????????????
        //???????????????
        //???????????????????????????
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

        //??????????????????????????????????????????????????????splitValue?????????
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
            QueryEntry entry = new QueryEntry(recordId, featureIndex, splitValue); // ???????????????????????????????????????????????????
            passiveQueryTable = new ArrayList<>();
            passiveQueryTable.add(entry);
        } else {
            QueryEntry lastLine = passiveQueryTable.get(passiveQueryTable.size() - 1); // ??????????????????????????????
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
     * ???label???client???????????????client?????????????????????????????????????????????id list??????????????????????????????????????????
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

        // Update???pred, grad, hess????????????????????? ???updateGH???update
        // update???currentNode.recordId
        // update???numClassRound
        // update???correspondingTreeNode???postPrune
        // update depth
        // update metricMap
        // todo ???datasetSize???????????????
        //???label????????????????????????
        List res = new ArrayList();

        if (!this.hasLabel) {
            // List of <boostP5Res, numClassRound, correspondingTreeNode, p,g,h, depth>
            res = addElement(res, new BoostP5Res(false, 0), numClassRound,
                    correspondingTreeNode, pred, grad, hess, depth, null, currentNode);
            assert res.size() == 9;
            return res;
        }

        LeftTreeInfo req = (LeftTreeInfo) (jsonData);
        // ??????client??????????????????id???
        int[] instanceIds = req.getLeftInstances();
        // ??????????????????Gtotal???Htotal
        double lGrad = 0;
        double lHess = 0;
        for (int i : instanceIds) {
            lGrad += grad[numClassRound][i];
            lHess += hess[numClassRound][i];
        }
        //?????????????????????????????????????????????????????????????????????????????????????????????
        TreeNode node = currentNode;
        currentNode.recordId = req.getRecordId(); // ??????phase4?????????message?????????????????????recordID

//        currentNode.client = req.getClient();
//        if (node.gain < 0) {
//            //this node is leaf node
//            double leafScore = Tree.calculateLeafScore(node.Grad, node.Hess, parameter.lambda);
//            node.leafNodeSetter(leafScore, true);
//        } else {
        depth = node.depth; // ?????????????????????

        // ???????????????????????????????????????????????????
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
        // TODO ??????????????????NULL???????????????????????????????????????
        TreeNode nanChild = null;
        // ???????????????
        newTreeNodes.offer(leftChild);
        newTreeNodes.offer(rightChild);
//        if (nanChild != null) {
//            newTreeNodes.offer(nanChild);
//        }
        // ??????nan????????????????????????nan??????
        double bestNanGoTo = 1;
        node.internalNodeSetterSecure(bestNanGoTo, nanChild, leftChild, rightChild, false);
        //phase 5 ???????????????????????????????????????
//        }
        //???????????????????????????????????????
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
                TreeNode treenode = newTreeNodes.poll();
                if (treenode.depth >= parameter.getMaxDepth()
                        || treenode.Hess < parameter.getMinChildWeight()
                        || treenode.numSample <= parameter.getMinSampleSplit()
                        || treenode.instanceSpace.length < parameter.getMinSampleSplit()) {
                    double leafScore = Tree.calculateLeafScore(treenode.Grad, treenode.Hess, parameter.getLambda());
                    treenode.leafNodeSetter(leafScore, true);
                } else {
                    currentTree.getAliveNodes().offer(treenode);
                }
            }
            // ???????????????????????????
            if (currentTree.getAliveNodes().isEmpty()) {
                return emptyTreeUpdate(currentTree, parameter, correspondingTreeNode,
                        depth, datasetSize, label, numClassRound, pred, loss, metricMap, res);
            }
//            updateGH(trainSet, 1);
        }
        // ?????????????????????????????????
        if (currentTree.getAliveNodes().isEmpty()) {
            return emptyTreeUpdate(currentTree, parameter, correspondingTreeNode,
                    depth, datasetSize, label, numClassRound, pred, loss, metricMap, res);
        }

        // ?????????????????????return
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
            // TODO ??????trainMetric.forEach(...);
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

    // TODO ??????unit test ??????????????????private function??????????????????private function
    // recursively postPrune ???????????????root??? correspondingTreeNode???????????????
    public void postPrune(TreeNode root, FgbParameter parameter, TreeNode[] correspondingTreeNode) {
        if (root.isLeaf) {
            return;
        }
        // ??????root????????????leaf??????????????????????????????
        postPrune(root.leftChild, parameter, correspondingTreeNode);
        postPrune(root.rightChild, parameter, correspondingTreeNode);
        // ??????????????????????????????leaf??????root???gain<=0??????prune??????root?????????leaf
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
        // ??????pred, grad, hess????????????
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
        //?????????????????????debug???????????????????????????master????????????master??????????????????
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
                // TODO ???train phase5???pred return??????
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
            // ????????????datapoint?????????node????????????leaf???pass
            if (treenode.isLeaf) {
                continue;
            }
            // ????????????leaf??????????????????????????????????????????????????????????????????????????????????????????????????????????????????
            if (Tool.contain(treenode.leftChild.instanceSpace, i)) {
                correspondingTreeNode[i] = treenode.leftChild;
            } else {
                correspondingTreeNode[i] = treenode.rightChild;
            }
        }
    }

    /**
     * ???????????????uid?????????????????????????????????
     *
     * @param uidList            ???????????????????????????????????????id??????
     * @param inferenceCacheFile ?????????????????????id ?????????????????????
     * @param others             ???????????????
     * @return ????????????????????? ??????isAllowList ??????uids???????????????
     */
    public Message inferenceInit(String[] uidList, String[][] inferenceCacheFile, Map<String, Object> others) {
        return InferenceFilter.filter(uidList, inferenceCacheFile);
    }

    /**
     * @param inferenceData ????????????
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
     * @param data                      ???coordinator?????????????????????
     * @param samples                   ?????????????????????
     * @param trees                     ???????????????????????????
     * @param firstRoundPred            ??????????????????????????????????????????
     * @param multiClassUniqueLabelList ?????????????????????
     * @return ??????????????????
     */
    public Message inferencePhase1(StringArray data, InferenceData samples, List<Tree> trees, double firstRoundPred, List<Double> multiClassUniqueLabelList) {
        //???????????????label??????????????????????????????????????????
        //?????????index?????????initial ?????????uid

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
        return Tool.addExpressions(fgbModelSerializer.saveModel(), this.expressions);
    }

    public void deserialize(String content) {
        String[] contents = Tool.splitExpressionsAndModel(content);
        this.expressions = Tool.splitExpressions(contents[0]);
        FgbModelSerializer fgbModel = new FgbModelSerializer(contents[1]);
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

    public List<String> getExpressions() {
        return expressions;
    }

    public void setExpressions(List<String> expressions) {
        this.expressions = expressions;
    }
}
