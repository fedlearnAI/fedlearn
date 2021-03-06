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
import com.jdt.fedlearn.common.entity.core.type.ReduceType;
import com.jdt.fedlearn.core.encryption.common.Ciphertext;
import com.jdt.fedlearn.core.encryption.common.EncryptionTool;
import com.jdt.fedlearn.core.encryption.common.PrivateKey;
import com.jdt.fedlearn.core.encryption.common.PublicKey;
import com.jdt.fedlearn.core.encryption.javallier.JavallierTool;
import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.core.entity.base.EmptyMessage;
import com.jdt.fedlearn.core.entity.base.Int2dArray;
import com.jdt.fedlearn.core.entity.base.StringArray;
import com.jdt.fedlearn.core.entity.boost.*;
import com.jdt.fedlearn.core.entity.common.MetricValue;
import com.jdt.fedlearn.core.entity.common.TrainInit;
import com.jdt.fedlearn.core.entity.distributed.InitResult;
import com.jdt.fedlearn.core.entity.distributed.SplitResult;
import com.jdt.fedlearn.common.entity.core.feature.Features;
import com.jdt.fedlearn.common.entity.core.feature.SingleFeature;
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
 * ?????????XGB model??????????????????????????????????????????????????????????????????????????????????????????
 *  @author fanmingjie
 */
public class DistributedFederatedGBModel implements Model, Serializable {
    private static final Logger logger = LoggerFactory.getLogger(DistributedFederatedGBModel.class);

    private int depth = 0;
    //four col, id,val,grad,hess
    private Map<Integer, List<Bucket>> sortedFeatureMap = new ConcurrentHashMap<>();
    public TreeNode currentNode;
    private List<QueryEntry> passiveQueryTable = new ArrayList<>();
    public List<Tree> trees = new ArrayList<>();
    private double eta;
    private double firstRoundPredict;
    private Loss loss;
    private Queue<TreeNode> newTreeNodes = new LinkedList<>();
    private FgbParameter parameter;
    private String privateKeyString;
    private String publicKeyString;
    private MetricValue metricValue;
    private int numClassRound = 0;
    public boolean hasLabel = false;
    public int datasetSize;
    public double[] label;

    public TreeNode[] correspondingTreeNode; // ???????????????datapoint?????????TreeNode
    public double[][] pred;
    // new double[parameter.getNumClass()][datasetSize]
    public double[][] grad;
    public double[][] hess;

    private List<Double> multiClassUniqueLabelList = new ArrayList<>();

    //phase2 ??????
    private Map<Integer, Tuple2<Ciphertext, Ciphertext>> ghMap2 = new HashMap<>();

    public int contributeFea = 0;
    // ?????????????????????
    private boolean isDistributed = false;
    // ??????id
//    private int featureId;
    private int modelId;
    private int workerNum;
    private List<Integer> featureIndexs;

    public DistributedFederatedGBModel() {
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
        if ((others.containsKey("isDistributed")) && (others.get("isDistributed").equals("true"))) {
            this.isDistributed = true;
//            this.featureId = (int) (others.get("featureId"));
            this.workerNum = (int) (others.get("workerNum"));
            this.modelId = (int) (others.get("modelId"));
            this.featureIndexs = (List<Integer>) others.get("featureindexs");
        }
        Tuple2<String[], String[]> trainTestUId = Tool.splitUid(uids, testIndex);
        String[] trainUids = trainTestUId._1();
        BoostTrainData trainData = new BoostTrainData(rawData, trainUids, features, new ArrayList<>());

//        newTreeNodes = new LinkedList<>();
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
        this.correspondingTreeNode = new TreeNode[datasetSize];
        EncryptionTool encryptionTool = getEncryptionTool();
        //???label ??????????????????????????????
        if (trainData.hasLabel) {
            this.pred = new double[parameter.getNumClass()][datasetSize];
            this.grad = new double[parameter.getNumClass()][datasetSize];
            this.hess = new double[parameter.getNumClass()][datasetSize];
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
                this.firstRoundPredict = firstRoundPredict();
            } else if (ObjectiveType.regSquare.equals(parameter.getObjective())) {
                this.loss = new SquareLoss();
//                this.firstRoundPred = parameter.getFirstRoundPred();
//                this.firstRoundPred = MathExt.average(this.label);
                this.firstRoundPredict = firstRoundPredict();
            } else if (ObjectiveType.countPoisson.equals(parameter.getObjective())) {
                if (Arrays.stream(this.label).filter(m -> m <= 0).findAny().isPresent()) {
                    throw new UnsupportedOperationException("There exist zero or negative labels for objective count:poisson!!!");
                } else {
//                    this.firstRoundPred = Math.log(MathExt.average(this.label));
                    this.firstRoundPredict = firstRoundPredict();
                }
                this.loss = new SquareLoss();
                this.label = loss.logTransform(label);
            } else if (ObjectiveType.binaryLogistic.equals(parameter.getObjective())) {
                this.loss = new LogisticLoss();
//                this.firstRoundPred = parameter.getFirstRoundPred();
                this.firstRoundPredict = firstRoundPredict();
            } else if (ObjectiveType.multiSoftmax.equals(parameter.getObjective())) {
                multiLabelTransform();
                this.loss = new crossEntropy(parameter.getNumClass());
//                this.firstRoundPred = parameter.getFirstRoundPred();
                this.firstRoundPredict = firstRoundPredict();
            } else if (ObjectiveType.multiSoftProb.equals(parameter.getObjective())) {
                multiLabelTransform();
                this.loss = new crossEntropy(parameter.getNumClass());
//                this.firstRoundPred = parameter.getFirstRoundPred();
                this.firstRoundPredict = firstRoundPredict();
            } else {
                throw new NotImplementedException();
            }
            initializePred(this.firstRoundPredict);
            Tuple2 ghTuple = updateGradHess(loss, parameter.getScalePosWeight(), label, pred, datasetSize, parameter, numClassRound);
            grad = (double[][]) ghTuple._1();
            hess = (double[][]) ghTuple._2();

        }
        eta = parameter.getEta();

        return trainData;
    }

    // TODO ??????trainPhase1, trainPhase2, ??????????????????

    /**
     * ??????????????????????????????????????????
     *
     * @param phase    ????????????
     * @param jsonData ????????????
     * @param train    ????????????
     * @return ????????????
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
                    if (isDistributed) {
                        req._1().setSubModel(new SubModel(currentNode.client, currentNode.splitFeature, currentNode.gain));
                    }
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
                if (isDistributed) {
                    req._1().setSubModel(new SubModel(passiveQueryTable));
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
                    if (isDistributed) {
                        res0.setSubModel(new SubModel(grad, hess, currentNode.recordId, trees));
                    }
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
                this.firstRoundPredict = 0.0;
            } else if (FirstPredictType.AVG.equals(parameter.getFirstRoundPred())) {
                this.firstRoundPredict = Math.log(MathExt.average(this.label));//TODO ?????????????????????
            } else if (FirstPredictType.RANDOM.equals(parameter.getFirstRoundPred())) {
                this.firstRoundPredict = Math.log(Math.random());//TODO ????????????
            }
            return firstRoundPredict;
        }
        if (FirstPredictType.ZERO.equals(parameter.getFirstRoundPred())) {
            this.firstRoundPredict = 0.0;
        } else if (FirstPredictType.AVG.equals(parameter.getFirstRoundPred())) {
            this.firstRoundPredict = MathExt.average(this.label);//TODO ?????????????????????
        } else if (FirstPredictType.RANDOM.equals(parameter.getFirstRoundPred())) {
            this.firstRoundPredict = Math.random();//TODO ????????????
        }
        return firstRoundPredict;
    }

    private void treeInit(int workerNum) {
        //????????????????????????????????????????????????????????????
        newTreeNodes = new LinkedList<>();
        //?????????label?????????????????????????????????
        if (parameter.getNumClass() > 1) {
            logger.info("Train Round " + (trees.size() / parameter.getNumClass() + 1) + " at class " + (numClassRound + 1) + "/" + parameter.getNumClass());
        }
        //?????????????????????
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
            return new EncryptedGradHess(null, null, workerNum);
        }
        PrivateKey privateKey = getEncryptionTool().restorePrivateKey(privateKeyString);
        // ????????????????????????client?????????
        // ??????????????????????????????????????????????????????
        // ??????g???h
        EncryptionTool encryptionTool = getEncryptionTool();
        List<Ciphertext> encryptedG = null;
        List<Ciphertext> encryptedH = null;
        if (req.isNewTree()) {
            treeInit(workerNum);
            encryptedG = Arrays.stream(grad[numClassRound]).parallel().mapToObj(g -> (encryptionTool.encrypt(g, privateKey.generatePublicKey()))).collect(toList());
            encryptedH = Arrays.stream(hess[numClassRound]).parallel().mapToObj(h -> encryptionTool.encrypt(h, privateKey.generatePublicKey())).collect(toList());
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
            List<Ciphertext> finalEncryptedG = encryptedG;
            List<Ciphertext> finalEncryptedH = encryptedH;
            StringTuple2[] encryptedArray = Arrays.stream(instanceSpace).parallel().mapToObj(x -> new StringTuple2(finalEncryptedG.get(x).serialize(), finalEncryptedH.get(x).serialize())).toArray(StringTuple2[]::new);
            String pk = privateKey.generatePublicKey().serialize();
            res = new EncryptedGradHess(req.getClient(), instanceSpace, encryptedArray, pk, true);
            if (isDistributed) {
                res.setSubModel(new SubModel(privateKeyString, pk, currentNode));
            }
        } else {
            res = new EncryptedGradHess(req.getClient(), instanceSpace);
            if (isDistributed) {
                res.setSubModel(new SubModel(currentNode));
            }
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
            res.setWorkerNum(workerNum);
            return res;
        }
        long s = System.currentTimeMillis();
        // jsonData is output from phase1
        EncryptedGradHess req = (EncryptedGradHess) (jsonData);
        // get instance information from the processing node
        int[] instanceSpace = req.getInstanceSpace();
        // initialize publicKey with null
//        EncryptionTool encryptionTool = getEncryptionTool();
        if (req.getNewTree()) {
            publicKeyString = req.getPubKey();
            if (!isDistributed) {
                // if new tree, get Grad Hess.
                StringTuple2[] gh = req.getGh();
                // if new tree, get publicKey.
                // instance index i - (g_i, h_i)
                for (int i = 0; i < instanceSpace.length; i++) {
                    ghMap2.put(instanceSpace[i], new Tuple2<>(encryptionTool.restoreCiphertext(gh[i].getFirst()), encryptionTool.restoreCiphertext(gh[i].getSecond())));
                }
            }
        }
        //TODO ????????????????????????
//        ColSampler colSampler = trainSet.getColSampler();
        //?????????????????????????????????????????????gl hl
        //??????column 0 ???col-0 ?????????id
        PublicKey finalPublicKey = encryptionTool.restorePublicKey(publicKeyString);
        // feature???1??????????????????0??????
        // TODO ?????????????????????print???????????????is true??
        FeatureLeftGH[] bodyArray = IntStream.range(1, trainSet.getFeatureDim() + 1)
                .parallel()
                .mapToObj(col -> {
                    // current instance space corresponding feature value matrix
                    double[][] theFeature = trainSet.getFeature(instanceSpace, col);
                    // use feature value to sort and bucket the feature
                    List<Bucket> buckets = sortAndGroup(theFeature, parameter.getNumBin());
                    // todo ????????????bucket??????????????????????????????list ???????????????ghSum?????????????????????????????????train response???map<fea_inx,list<list<integer>>?????????????????????worker???
                    // todo ??????worker????????????????????????????????????phase?????????????????????phase??????????????????????????????????????????????????????????????????gh?????????????????????????????????????????????????????????????????????phase2?????????????????????phase??????
                    // todo ??????????????????????????????gh ?????????????????????????????????gh???????????????????????????????????????????????????????????????????????????????????????????????????
                    // feature id -> feature buckets
                    sortedFeatureMap.put(col, buckets);
                    StringTuple2[] full = ghSum(buckets, ghMap2, finalPublicKey, encryptionTool);
                    String feature;
                    // todo ????????????isDistributed
                    if (isDistributed) {
                        feature = "" + featureIndexs.get(col);
                        List<int[]> instanceList = filterInstances(buckets, ghMap2);
                        return new FeatureLeftGH(req.getClient(), feature, full, instanceList);
                    } else {
                        feature = "" + col;
                        return new FeatureLeftGH(req.getClient(), feature, full);
                    }
                })
                .toArray(FeatureLeftGH[]::new);
        long e = System.currentTimeMillis();
        logger.info("core phase2???????????????{} ms", (e - s));
        return new BoostP2Res(bodyArray);
    }


    /**
     * ???phase2???????????????message?????????????????????instance??????
     *
     * @param message BoostP2Res
     * @return ????????????????????????id??????
     */
    public Map<String, List<int[]>> getInstanceLists(Message message) {
        Map<String, List<int[]>> listMap = new HashMap<>();
        if (message instanceof BoostP2Res) {
            BoostP2Res boostP2Res = (BoostP2Res) message;
            FeatureLeftGH[] featureLeftGHS = boostP2Res.getFeatureGL();
            if (featureLeftGHS == null || featureLeftGHS[0].getInstanceList().size() == 0 || featureLeftGHS[0].getInstanceList() == null) {
                return listMap;
            }
            for (FeatureLeftGH leftGH : featureLeftGHS) {
                listMap.put(leftGH.getFeature(), leftGH.getInstanceList());
            }
        }
        return listMap;
    }


    /**
     * ????????????????????????id???sum(gh)
     *
     * @param listMap ????????????????????????id??????
     * @return ?????????sum(GH)
     */
    // todo ??????????????????uid???????????????????????????  gh???????????????????????????????????????????????????worker?????????gh????????????cipertext ????????????????????????restore???????????????
    public Message subCalculation(Map<String, List<int[]>> listMap) {
        long s1 = System.currentTimeMillis();
        EncryptionTool encryptionTool = getEncryptionTool();
        PublicKey finalPublicKey = encryptionTool.restorePublicKey(publicKeyString);
        FeatureLeftGH[] featureLeftGHS = new FeatureLeftGH[listMap.entrySet().size()];
        int m = 0;
        for (Map.Entry<String, List<int[]>> e : listMap.entrySet()) {
            List<int[]> instances = e.getValue();
            StringTuple2[] full = instances
                    .parallelStream()
                    .map(d -> {
                        logger.info("fea" + e.getKey() + " subCalculation send uid length:" + d.length);
                        if (d.length == 0) {
                            logger.info("send uids  is: null");
                            return new StringTuple2("", "");
                        }
                        logger.info("fea" + e.getKey() + " subCalculation send uid examples :" + d[0] + " , gh map uid " + ghMap2.keySet().toArray()[0]);
                        int[] existsIds = Arrays.stream(d).filter(x -> ghMap2.containsKey(x)).toArray();
                        logger.info("fea" + e.getKey() + " subCalculation existsIds length:" + existsIds.length);
                        if (existsIds.length == 0) {
                            logger.info("subCalculation r is: null");
                            return new StringTuple2("", "");
                        }
                        return Arrays.stream(Arrays.stream(d).filter(ghMap2::containsKey).toArray())
                                .parallel()
                                .mapToObj(id -> ghMap2.get(id))
                                // ??????g h
                                .reduce((x, y) -> new Tuple2<>(encryptionTool.add(x._1(), y._1(), finalPublicKey), encryptionTool.add(x._2(), y._2(), finalPublicKey)))
                                .map(x -> new StringTuple2((x._1().serialize()), (x._2().serialize())))
                                .get();
                    }).toArray(StringTuple2[]::new);
            featureLeftGHS[m] = new FeatureLeftGH(e.getKey(), full);
            m++;
        }
        long e1 = System.currentTimeMillis();
        logger.info("core?????????????????????{} ms", (e1 - s1));
        return new BoostP2Res(featureLeftGHS);
    }

    /**
     * ??????worker??????GH????????????
     *
     * @param message ??????map???response
     * @param subGHs  ??????worker???????????????
     * @return ??????map?????????response
     */
    public Message mergeSubResult(Message message, List<Message> subGHs) {
        long s1 = System.currentTimeMillis();
        EncryptionTool encryptionTool = getEncryptionTool();
        PublicKey finalPublicKey = encryptionTool.restorePublicKey(publicKeyString);
        Message result = message;
        if (message instanceof BoostP2Res) {
            BoostP2Res boostP2Res = (BoostP2Res) message;
            FeatureLeftGH[] leftGHS = boostP2Res.getFeatureGL();
            FeatureLeftGH[] res = Arrays.stream(leftGHS).parallel().map(x ->
                    {
                        StringTuple2[] ghLeft = x.getGhLeft();
                        int num = x.getInstanceList().size();
                        StringTuple2[] stringTuple2s = new StringTuple2[num];
                        logger.info("feature " + x.getFeature() + ", num is " + num);
                        for (Message sub : subGHs) {
                            if (sub instanceof BoostP2Res) {
                                BoostP2Res boostP2Res1 = (BoostP2Res) sub;
                                FeatureLeftGH[] leftGHS1 = boostP2Res1.getFeatureGL();
                                StringTuple2[] othersGH = Arrays.stream(leftGHS1).filter(f -> f.getFeature().equals(x.getFeature())).findFirst().get().getGhLeft();
                                if (othersGH.length == 0) {
                                    return new FeatureLeftGH(x.getClient(), x.getFeature(), ghLeft);
                                }
                                IntStream.range(0, stringTuple2s.length).forEach(ss -> {
                                    String ciphertext;
                                    String ciphertext1;
                                    if (othersGH[ss] != null && othersGH[ss].getFirst() != null && !"".equals(othersGH[ss].getFirst())) {
                                        if (ghLeft[ss] != null && ghLeft[ss].getFirst() != null && !"".equals(ghLeft[ss].getFirst())) {
                                            ciphertext = encryptionTool.add(encryptionTool.restoreCiphertext(ghLeft[ss].getFirst()), encryptionTool.restoreCiphertext(othersGH[ss].getFirst()), finalPublicKey).serialize();
                                            ciphertext1 = encryptionTool.add(encryptionTool.restoreCiphertext(ghLeft[ss].getSecond()), encryptionTool.restoreCiphertext(othersGH[ss].getSecond()), finalPublicKey).serialize();
                                        } else {
                                            ciphertext = othersGH[ss].getFirst();
                                            ciphertext1 = othersGH[ss].getSecond();
                                        }
                                    } else {
                                        if (ghLeft[ss] != null && ghLeft[ss].getFirst() != null && !"".equals(ghLeft[ss].getFirst())) {
                                            ciphertext = ghLeft[ss].getFirst();
                                            ciphertext1 = ghLeft[ss].getSecond();
                                        } else {
                                            logger.error("othersGH " + othersGH[ss] + ", ghLeftCipher " + ghLeft[ss].getFirst());
                                            throw new UnsupportedOperationException("??????????????????????????????????????????null");
                                        }
                                    }
                                    stringTuple2s[ss] = new StringTuple2(ciphertext, ciphertext1);
                                });
                            }
                        }
                        return new FeatureLeftGH(x.getClient(), x.getFeature(), stringTuple2s);
                    }
            ).toArray(FeatureLeftGH[]::new);
            result = new BoostP2Res(res);
        }
        long e1 = System.currentTimeMillis();
        logger.info("core????????????:{} ms ", (e1 - s1));
        return result;
    }


    private EncryptionTool getEncryptionTool() {
        return new JavallierTool();
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
     * @param buckets ???????????????????????????
     * @param ghMap2  ??????worker?????????gh
     * @return ????????????id??????
     */
    public List<int[]> filterInstances(List<Bucket> buckets, Map<Integer, Tuple2<Ciphertext, Ciphertext>> ghMap2) {
        List<int[]> instanceList = new ArrayList<>();
        for (Bucket bucket : buckets) {
            double[] instance = Arrays.stream(bucket.getIds()).filter(x -> !ghMap2.containsKey((int) x)).toArray();
            int[] instance1 = Arrays.stream(instance).mapToInt(d -> (int) d).toArray();
            instanceList.add(instance1);
        }
        return instanceList;
    }

    /**
     * encrypt sum of g and h for "left instances" for each bucket cumulatively
     *
     * @param buckets        feature buckets
     * @param ghMap2         instance index i,  (g_i, h_i)
     * @param publicKey      publicKey
     * @param encryptionTool encryptionTool
     * @return StringTuple Array
     */
    public StringTuple2[] ghSum(List<Bucket> buckets, Map<Integer, Tuple2<Ciphertext,
            Ciphertext>> ghMap2, PublicKey publicKey, EncryptionTool encryptionTool) {
//        final String zero = PaillierTool.encryption(0, paillierPublicKey);
//        double[] ids = Arrays.stream(buckets.get(0).getIds()).filter(ghMap2::containsKey).toArray();
        return buckets
                .parallelStream()
                .map(bucket -> {
                    double[] existsIds = Arrays.stream(bucket.getIds()).filter(x -> ghMap2.containsKey((int) x)).toArray();
                    logger.info("existsIds length:" + existsIds.length);
                    if (existsIds.length == 0) {
                        logger.info("r is: null");
                        return new StringTuple2("", "");
                    }
                    return Arrays.stream(existsIds)
                            .parallel()
                            .mapToObj(id -> ghMap2.get((int) id))
                            // ??????g h
                            .reduce((x, y) -> new Tuple2<>(encryptionTool.add(x._1(), y._1(), publicKey), encryptionTool.add(x._2(), y._2(), publicKey)))
                            .map(x -> new StringTuple2((x._1().serialize()), (x._2().serialize())))
                            .get();
                }).toArray(StringTuple2[]::new);
    }


    /**
     * for each feature in the buckets(List), calculate totalG, totalH for each bucket; same function as ghSum
     * the only difference is that ghSum is encrypted but processEachNumericFeature2 is not.
     *
     * @param buckets feature buckets
     * @param ghMap:  (index, (g, h))
     * @return ghSum array
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
    public Tuple2<BoostP3Res, Double> trainPhase3(Message jsonData, BoostTrainData trainSet, TreeNode currentNode, EncryptionTool encryptionTool, String privateKeyString,
                                                  FgbParameter parameter, double[][] grad, double[][] hess,
                                                  int numClassRound) {
        //??????label????????????????????????
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
            //????????????????????????????????????
            //??????????????????????????????????????????????????????????????????????????????????????????gain???????????????????????????gain????????????
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
            Optional<Tuple2<Double, Integer>> featureMaxGain = computeGain(body, g, h, parameter).parallelStream().max(Comparator.comparing(Tuple2::_1));
            // ??????????????????split gain?????????client????????????
            if (featureMaxGain.isPresent() && featureMaxGain.get()._1() > gain) {
                gain = featureMaxGain.get()._1();
                client = req.getClient();
                if (isDistributed) {
                    feature = "" + featureIndexs.get(col);
                } else {
                    feature = "" + col;
                }
                splitIndex = featureMaxGain.get()._2();
            }
        }
        //?????????????????????????????????????????????????????????
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
     * @return (gain, index)
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
     * @param encryptionTool encryptionTool
     * @param privateKey     privateKey
     * @param parameter      FgbParameter
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
     * ??????message?????????message????????????client????????????????????????????????????client???????????????????????????????????????
     *
     * @param jsonData          request from coordinator
     * @param sortedFeatureMap  ( feature id, feature buckets)
     * @param passiveQueryTable passive Query Table
     * @return LeftTreeInfo and query table
     */
    public Tuple2<LeftTreeInfo, List<QueryEntry>> trainPhase4(Message jsonData, Map<Integer, List<Bucket>> sortedFeatureMap,
                                                              List<QueryEntry> passiveQueryTable) {
//        Map<Integer, List<Bucket>> sortedFeatureMap,
//        LinkedHashMap<Integer, QueryEntry> passiveQueryTable, int contributeFea
        BoostP4Req req = (BoostP4Req) (jsonData);
        if (!req.isAccept()) {
            return new Tuple2<>(new LeftTreeInfo(0, null), passiveQueryTable);
        }
        //????????????????????????
        int featureIndex = req.getkOpt();
        int loaclIndex = featureIndex;
        //??????phase3 ??????????????? ?????????????????????????????????????????????????????????????????????????????????????????????i?????????
        int splitIndex = req.getvOpt();
        if (isDistributed) {
            if (!featureIndexs.contains(featureIndex)) {
                return new Tuple2<>(new LeftTreeInfo(0, null), passiveQueryTable);
            } else {
                loaclIndex = featureIndexs.indexOf(featureIndex);
            }
        }
        List<Bucket> sortedFeature2 = sortedFeatureMap.get(loaclIndex); // ??????????????????
        double splitValue = sortedFeature2.get(splitIndex).getSplitValue(); // ???????????????????????????
        //?????????????????????????????????????????????????????????????????????????????????????????????
        //???????????????
        //???????????????????????????
        List<Integer> leftIns = new ArrayList<>();
        for (int i = 0; i <= splitIndex; i++) {
            Bucket Bucket = sortedFeature2.get(i);
            double[] ids = Bucket.getIds();
            for (double id : ids) {
                leftIns.add((int) id);
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
        return new Tuple2<>(new LeftTreeInfo(recordId, left), passiveQueryTable);
    }

    //build full query index and update prediction and grad

    /**
     * ???label???client???????????????client?????????????????????????????????????????????id list??????????????????????????????????????????
     *
     * @param jsonData              ????????????????????????
     * @param grad                  g
     * @param hess                  h
     * @param numClassRound         ??????
     * @param currentNode           ????????????
     * @param newTreeNodes          ????????????
     * @param parameter             ????????????
     * @param correspondingTreeNode ?????????
     * @param metricMap             ????????????
     * @param trees                 ???
     * @return ????????????
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
                pgh.get(0), pgh.get(1), pgh.get(2), depth, metricMap, currentNode);
        assert res.size() == 9;
        return res;
    }

    private List addElement(List a, Object... arg) {
        Collections.addAll(a, arg);
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
     * ??????????????????map
     *
     * @param requestId   ??????id
     * @param rawData     ???????????????
     * @param trainInit   ?????????????????????
     * @param matchResult id????????????
     * @return ?????????map
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
        // TODO ?????????????????????????????????/??????
        TrainData trainData = localModel.trainInit(rawData, matchResult, testIndex, hyperParameter, features, others);
        List<String> modelIDs = new ArrayList<>();
        // todo ??????????????????????????????????????????????????????????????????????????????????????????????????????
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
     * ????????????????????????
     * @param requestId ??????ID
     * @param trainInit ????????????
     * @param sortedIndexList ???????????????index??????
     * @return ???????????????id??????
     */
    public ArrayList<Integer> dataIdList(String requestId, TrainInit trainInit, List<Integer> sortedIndexList) {
        // ????????????
        Collections.sort(sortedIndexList);
        // ??????????????????
        ArrayList<Integer> sampleData = new ArrayList<>();
        sampleData.add(0);
        // ??????????????????????????????????????????
        List<Integer> mapSampleRandomIndex = sortedIndexList.stream().map(integer -> integer + 1).collect(Collectors.toList());
        sampleData.addAll(mapSampleRandomIndex);
        return sampleData;
    }

    private static final String TRAIN_REQUEST = "trainRequest";
    private static final String MESSAGE_LIST = "messageList";

    /**
     * ??????????????????????????????????????????????????????
     *
     * @param message ????????????????????????????????????
     * @return ????????????
     */
    public Map<String, Object> messageSplit(Message message) {
        Map<String, Object> messageSplit = new HashMap<>();
        if (!(message instanceof EncryptedGradHess)) {
            messageSplit.put(TRAIN_REQUEST, message);
            return messageSplit;
        }
        EncryptedGradHess encryptedGradHess = (EncryptedGradHess) message;
        StringTuple2[] gh = encryptedGradHess.getGh();
        int workerNum = encryptedGradHess.getWorkerNum();
        if (!encryptedGradHess.getNewTree() || gh == null || gh.length == 0 || workerNum == 0) {
            messageSplit.put(TRAIN_REQUEST, message);
            return messageSplit;
        }
        int instanceMin = 0;
        int instanceMax;
        List<StringTuple2> allGh = Arrays.asList(gh);
        List<List<StringTuple2>> ghList = splitList(allGh, workerNum);
        ((EncryptedGradHess) message).setGh(null);
        messageSplit.put(TRAIN_REQUEST, message);
        List<Message> messageList = new ArrayList<>();
        for (int i = 0; i < workerNum; i++) {
            instanceMax = instanceMin + ghList.get(i).size() - 1;
            EncryptedGradHess encryptedGradHess1 = new EncryptedGradHess();
            encryptedGradHess1.setModelId(i);
            encryptedGradHess1.setGh(ghList.get(i).toArray(new StringTuple2[0]));
            encryptedGradHess1.setInstanceMin(instanceMin);
            encryptedGradHess1.setInstanceMax(instanceMax);
            messageList.add(encryptedGradHess1);
            instanceMin = instanceMax + 1;
        }
        messageSplit.put(MESSAGE_LIST, messageList);
        return messageSplit;
    }

    /**
     * ????????????????????????
     *
     * @param subMessage ????????????
     */
    public void updateSubMessage(List<Message> subMessage) {
        EncryptionTool encryptionTool = getEncryptionTool();
        for (Message message : subMessage) {
            if (message instanceof EncryptedGradHess) {
                EncryptedGradHess encryptedGradHess = (EncryptedGradHess) message;
                if (encryptedGradHess.getGh() != null && encryptedGradHess.getGh().length > 0) {
                    StringTuple2[] subGh = encryptedGradHess.getGh();
                    int instanceMin = encryptedGradHess.getInstanceMin();
                    IntStream.range(0, subGh.length).parallel().forEach(i -> ghMap2.put(instanceMin + i, new Tuple2<>(encryptionTool.restoreCiphertext(subGh[i].getFirst()), encryptionTool.restoreCiphertext(subGh[i].getSecond()))));
                }
            }
        }
    }


    /***
     * ?????????????????????
     * @param phase ????????????
     * @param req   ????????????
     * @return ?????????map??????
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
     * ???????????????????????????
     * ???????????????????????????????????????????????????????????????????????????????????????????????????
     * @param req ??????
     * @return ????????????
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
        int matchSize = (int) request.getOthers().get("matchSize");
        int workerNum = dynamicMapNum(matchSize, featuresNum);
        // todo ????????????????????????,??????????????????
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
//            //todo ???worker??????????????????????????? featureList???
            messageBodies.add(trainInit);
            modelIDs.add(String.valueOf(i));
        }
        splitResult.setReduceType(ReduceType.needMerge);
        splitResult.setMessageBodys(messageBodies);
        splitResult.setModelIDs(modelIDs);
        return splitResult;
    }

    /**
     * ???????????????????????????????????????map??????
     *
     * @param matchSize  id????????????????????????
     * @param featureNum ????????????
     * @return ?????????map??????
     */
    private int dynamicMapNum(int matchSize, int featureNum) {
        int eachFeature = 2;
        if (matchSize <= 100000) {
            if (featureNum > 10 && featureNum <= 20) {
                eachFeature = 5;
            } else if (featureNum > 20 && featureNum <= 30) {
                eachFeature = 8;
            } else if (featureNum > 30 && featureNum <= 40) {
                eachFeature = 10;
            } else if (featureNum > 40) {
                eachFeature = 20;
            }
        } else if (matchSize <= 2000000) {
            if (featureNum <= eachFeature) {
                eachFeature = 1;
            } else if (featureNum <= 60) {
                eachFeature = featureNum;
            } else {
                eachFeature = 58;
            }
        } else {
            eachFeature = 10;
        }
        int mapNum = featureNum / eachFeature;
        if (featureNum % eachFeature != 0) {
            mapNum = mapNum + 1;
        }
        return mapNum;
    }

    /***
     * ????????????
     * ??????????????????????????????????????????????????????????????????
     *
     * @param list ?????????????????????
     * @param n ?????????????????????
     * @param <T> ???????????????
     * @return ????????????
     */
    public <T> List<List<T>> splitList(List<T> list, int n) {
        List<List<T>> result = new ArrayList<List<T>>();
        int remaider = list.size() % n; //(??????????????????)
        int number = list.size() / n; //????????????
        int offset = 0;//?????????
        for (int i = 0; i < n; i++) {
            List<T> value;
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
     * ?????????????????????
     *
     * @param req ??????
     * @return ??????????????????
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
     * ??????2??????????????????????????????
     * ???????????????????????????????????????????????????????????????
     *
     * @param req ??????
     * @return ???????????????????????????
     */
    private SplitResult mapPhase2(Message req) {
        SplitResult splitResult = new SplitResult();
        List<String> modelIDs = new ArrayList<>();
        List<Message> messageBodies = new ArrayList<>();
        EncryptedGradHess gradHess;
        // TODO
        if (req instanceof EmptyMessage) {
            messageBodies.add(req);
            modelIDs.add(String.valueOf(0));
        } else if (req instanceof EncryptedGradHess) {
            gradHess = (EncryptedGradHess) req;
            for (int i = 0; i < gradHess.getWorkerNum(); i++) {
                modelIDs.add(String.valueOf(i));
            }
        }
        splitResult.setMessageBodys(messageBodies);
        splitResult.setModelIDs(modelIDs);
        splitResult.setReduceType(ReduceType.needMerge);
        return splitResult;
    }

    /**
     * ??????3??????????????????????????????
     * ???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     *
     * @param req ??????
     * @return ???????????????????????????
     */
    private SplitResult mapPhase3(Message req) {
        SplitResult splitResult = new SplitResult();
        List<String> modelIDs = new ArrayList<>();
        List<Message> messageBodies = new ArrayList<>();
        BoostP3Req boostP3Req;
        if (req instanceof EmptyMessage) {
            messageBodies.add(req);
            modelIDs.add(String.valueOf(0));
        } else if (req instanceof BoostP3Req) {
            boostP3Req = (BoostP3Req) req;
            for (int i = 0; i < boostP3Req.getWorkerNum(); i++) {
                modelIDs.add(String.valueOf(i));
            }
        }
        splitResult.setMessageBodys(messageBodies);
        splitResult.setModelIDs(modelIDs);
        splitResult.setReduceType(ReduceType.needMerge);
        return splitResult;
    }

    /**
     * ??????4???????????????????????????
     * ???????????????????????????????????????????????????????????????????????????????????????????????????
     *
     * @param req ??????
     * @return ?????????????????????
     */
    private SplitResult mapPhase4(Message req) {
        SplitResult splitResult = new SplitResult();
        List<String> modelIDs = new ArrayList<>();
        List<Message> messageBodies = new ArrayList<>();
        BoostP4Req boostP4Req = (BoostP4Req) req;
        if (boostP4Req.isAccept()) {
            for (int i = 0; i < boostP4Req.getWorkerNum(); i++) {
                modelIDs.add(String.valueOf(i));
            }
        } else {
            messageBodies.add(req);
            modelIDs.add(String.valueOf(0));
        }
        splitResult.setMessageBodys(messageBodies);
        splitResult.setModelIDs(modelIDs);
        splitResult.setReduceType(ReduceType.needMerge);
        return splitResult;
    }


    /***
     * ??????????????????????????????????????????????????????????????????
     *
     * @param phase ????????????
     * @param result  ????????????
     * @return ????????????
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
     * ??????1???????????????????????????map??????????????????????????????
     *
     * @param result map??????????????????
     * @return ????????????
     */
    private static Message reducePhase1(List<Message> result) {
        return result.get(0);
    }

    /***
     * ??????2?????????map???????????????????????????
     * ?????????????????????FeatureLeftGH[]?????????????????????????????????????????????????????????????????????????????????
     * @param result ???map??????????????????
     * @return ???????????????????????????
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
     * ??????3?????????map????????????????????????
     * ??????????????????????????????????????????????????????????????????????????????
     * @param results ???map??????????????????
     * @return ??????????????????
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
     * ??????4map??????????????????
     *
     * @param result ???map????????????
     * @return ???????????????????????????????????????
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
     * ??????5map?????????????????????
     * ????????????5??????????????????????????????
     * @param result ???map??????
     * @return ????????????
     */
    private static Message reducePhase5(List<Message> result) {
        return result.get(0);
    }


    /**
     * ???????????????????????????????????????
     *
     * @param message ???????????????model??????
     * @return ???????????????model
     */
    public Message updateSubModel(Message message) {
        SubModel subModel;
        if (message instanceof EncryptedGradHess) {
            EncryptedGradHess encryptedGradHess = ((EncryptedGradHess) message);
            subModel = encryptedGradHess.getSubModel();
            if (subModel == null) {
                return message;
            }
            if (modelId != 0) {
                currentNode = subModel.getCurrentNode();
                trees = subModel.getTrees();
            }
            if (subModel.getPrivateKey() != null || subModel.getKeyPublic() != null) {
                privateKeyString = subModel.getPrivateKey();
                publicKeyString = subModel.getKeyPublic();
            }
//            encryptedGradHess.setSubModel(null);
            return encryptedGradHess;
        } else if (message instanceof BoostP3Res) {
            BoostP3Res boostP3Res = ((BoostP3Res) message);
            subModel = boostP3Res.getSubModel();
            if (subModel == null) {
                return message;
            }
            currentNode.client = subModel.getClientInfo();
            currentNode.splitFeature = subModel.getSplitFeature();
            currentNode.gain = subModel.getGain();
//            boostP3Res.setSubModel(null);
            return boostP3Res;
        } else if (message instanceof LeftTreeInfo) {
            LeftTreeInfo leftTreeInfo = ((LeftTreeInfo) message);
            subModel = leftTreeInfo.getSubModel();
            if (subModel == null) {
                return message;
            }

            passiveQueryTable = subModel.getPassiveQueryTable();

//            leftTreeInfo.setSubModel(null);
            return leftTreeInfo;
        } else if (message instanceof BoostP5Res) {
            BoostP5Res boostP5Res = ((BoostP5Res) message);
            subModel = boostP5Res.getSubModel();
            if (subModel == null) {
                return message;
            }
            grad = subModel.getGrad();
            hess = subModel.getHess();
            currentNode.recordId = subModel.getRecordId();
            if (modelId != 0) {
                trees = subModel.getTrees();
            }
//            boostP5Res.setSubModel(null);
            return boostP5Res;
        } else if (message instanceof BoostP2Res) {
            ghMap2.clear();
            return message;
        } else {
            throw new UnsupportedOperationException("unsupported message type in federated gradient boost model");
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
            return inferencePhase1(parameterData, inferenceData, this.trees, this.firstRoundPredict, this.multiClassUniqueLabelList);
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
        FgbModelSerializer fgbModelSerializer = new FgbModelSerializer(trees, loss, firstRoundPredict, eta, passiveQueryTable, multiClassUniqueLabelList);
        return fgbModelSerializer.saveModel();
    }

    public void deserialize(String content) {
        FgbModelSerializer fgbModel = new FgbModelSerializer(content);
        this.trees = fgbModel.getTrees();
        this.loss = fgbModel.getLoss();
        this.firstRoundPredict = fgbModel.getFirstRoundPred();
        this.eta = fgbModel.getEta();
        this.passiveQueryTable = fgbModel.getPassiveQueryTable();
        this.multiClassUniqueLabelList = fgbModel.getMultiClassUniqueLabelList();
    }

    public AlgorithmType getModelType() {
        return AlgorithmType.FederatedGB;
    }

    public String getPrivateKeyString() {
        return privateKeyString;
    }

    public void setModelId(int modelId) {
        this.modelId = modelId;
    }

    public void setPublicKeyString(String publicKeyString) {
        this.publicKeyString = publicKeyString;
    }
}
