package com.jdt.fedlearn.core.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.jdt.fedlearn.core.encryption.common.Ciphertext;
import com.jdt.fedlearn.core.encryption.common.EncryptionTool;
import com.jdt.fedlearn.core.encryption.common.PrivateKey;
import com.jdt.fedlearn.core.encryption.common.PublicKey;
import com.jdt.fedlearn.core.encryption.fake.FakeTool;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.base.StringArray;
import com.jdt.fedlearn.core.entity.boost.*;
import com.jdt.fedlearn.core.entity.common.InferenceInitRes;
import com.jdt.fedlearn.core.entity.common.MetricValue;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.exception.NotImplementedException;
import com.jdt.fedlearn.core.fake.StructureGenerate;
import com.jdt.fedlearn.core.loader.boost.BoostTrainData;
import com.jdt.fedlearn.core.loader.common.CommonLoad;
import com.jdt.fedlearn.core.loader.common.InferenceData;
import com.jdt.fedlearn.core.model.common.loss.LogisticLoss;
import com.jdt.fedlearn.core.model.common.loss.Loss;
import com.jdt.fedlearn.core.model.common.tree.Tree;
import com.jdt.fedlearn.core.model.common.tree.TreeNode;
import com.jdt.fedlearn.core.model.serialize.FgbModelSerializer;
import com.jdt.fedlearn.core.parameter.FgbParameter;
import com.jdt.fedlearn.core.type.*;
import com.jdt.fedlearn.core.type.data.*;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;

public class TestFederatedGBModel {
    private static final FgbParameter fp = new FgbParameter.Builder(3, new MetricType[]{MetricType.ACC}, ObjectiveType.regLogistic).build();

    @Test
    public void testMultiLabelTransform() {
        FederatedGBModel model = new FederatedGBModel();
        // generate raw data
        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainClassInputStd();
        String[][] raw = compoundInput._1().get(); // raw data: each row is a data point
        String[] result = compoundInput._2().get(); // id match info
        Features features = compoundInput._3().get(); // feature and label column names
        // train init
        BoostTrainData trainData = model.trainInit(raw, result, new int[0], fp, features, new HashMap<>());
        model.multiLabelTransform();
        // expected value
        double[] target = {0.0, 1.0, 0.0, 1.0};
        for (int i = 0; i < model.label.length; i++) {
            Assert.assertEquals(model.label[i], target[i]);
        }
    }

    @Test
    public void trainInit() {
        /*
        trainInit test
        */
        FederatedGBModel model = new FederatedGBModel();
        List<Tuple3<String[][], String[], Features>> dataArray = new ArrayList<>();
        dataArray.add(StructureGenerate.trainInputStd());
        dataArray.add(StructureGenerate.trainClassInputStd());
        dataArray.add(StructureGenerate.trainInputStdNoLabel());
        // fp
        List<FgbParameter> fpList = new ArrayList<>();
        fpList.add(fp);
        MetricType[] metricTypes = new MetricType[]{MetricType.CROSS_ENTRO, MetricType.ACC, MetricType.AUC};
//        fpList.add(new FgbParameter.Builder(50, metricTypes, ObjectiveType.multiSoftmax).eta(0.1).maxDepth(5).build());
        fpList.add(new FgbParameter.Builder(50, metricTypes, ObjectiveType.regSquare).build());
        fpList.add(new FgbParameter.Builder(50, new MetricType[]{MetricType.CROSS_ENTRO, MetricType.ACC, MetricType.AUC}, ObjectiveType.countPoisson).build());
        fpList.add(new FgbParameter.Builder(50, new MetricType[]{MetricType.ACC}, ObjectiveType.multiSoftProb).numClass(2).eta(0.1).maxDepth(5).build());
        fpList.add(new FgbParameter.Builder(50, new MetricType[]{MetricType.CROSS_ENTRO, MetricType.ACC, MetricType.AUC}, ObjectiveType.regLogistic).build());
        fpList.add(new FgbParameter.Builder(50, new MetricType[]{MetricType.ACC}, ObjectiveType.multiSoftmax).numClass(2).build());
        fpList.add(new FgbParameter.Builder(50, new MetricType[]{MetricType.ACC}, ObjectiveType.binaryLogistic).numClass(1).build());
        for (Tuple3<String[][], String[], Features> compoundInput : dataArray) {
            for (FgbParameter fp : fpList) {
                String[][] raw = compoundInput._1().get();
                String[] result = compoundInput._2().get();
                Features features = compoundInput._3().get();
                try {
                    BoostTrainData trainData = model.trainInit(raw, result, new int[0], fp, features, new HashMap<>());
                    Assert.assertEquals(model.pred[0].length, 3);
                    if ((fp.getObjective() == ObjectiveType.multiSoftProb)||(fp.getObjective() == ObjectiveType.multiSoftmax)) {
                        Assert.assertEquals(model.pred.length, 2);
                    } else {
                        Assert.assertEquals(model.pred.length, 1);
                    }
                    //TODO add more Assert
                } catch (Exception e) {
                    System.out.println("unmatched dataset and objective");
                }
            }
        }
    }

    /**
     * countPoisson & negative label
     */
    @Test
    public void trainInit2() {
        FederatedGBModel model = new FederatedGBModel();
        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainClassInputStd();
        String[][] raw = compoundInput._1().get();
        String[] result = compoundInput._2().get();
        Features features = compoundInput._3().get();
        FgbParameter fp = new FgbParameter.Builder(50, new MetricType[]{MetricType.CROSS_ENTRO, MetricType.ACC, MetricType.AUC}, ObjectiveType.countPoisson).build();
        model.label = new double[]{-1.0, 0.0, 1.0};
        try {
            BoostTrainData trainData = model.trainInit(raw, result, new int[0], fp, features, new HashMap<>());
        } catch (UnsupportedOperationException e) {
            Assert.assertEquals(e.getMessage(), "There exist zero or negative labels for objective count:poisson!!!");
        }
    }

    /**
     * Other ObjectiveType
     */
    @Test
    public void trainInit3() {
        FederatedGBModel model = new FederatedGBModel();
        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainClassInputStd();
        String[][] raw = compoundInput._1().get();
        String[] result = compoundInput._2().get();
        Features features = compoundInput._3().get();
        try {
            FgbParameter fp = new FgbParameter.Builder(50, new MetricType[]{MetricType.ACC}, null).numClass(2).build();
            BoostTrainData trainData = model.trainInit(raw, result, new int[0], fp, features, new HashMap<>());
        } catch (NotImplementedException e) {
            Assert.assertNull(e.getMessage());
        }
    }

    /**
     * firstRoundPred
     * countPoisson && FirstPredictType.AVG
     */
    @Test
    public void firstRoundPred1() {
        FederatedGBModel model = new FederatedGBModel();
        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainInputStd();
        String[][] raw = compoundInput._1().get();
        String[] result = compoundInput._2().get();
        Features features = compoundInput._3().get();
        FgbParameter fp = new FgbParameter.Builder(50, new MetricType[]{MetricType.ACC}, ObjectiveType.countPoisson).build();
        BoostTrainData trainData = model.trainInit(raw, result, new int[0], fp, features, new HashMap<>());

        Assert.assertEquals(trainData.getDatasetSize(), 3);
        Assert.assertEquals(trainData.getFeatureDim(), 3);
        Assert.assertEquals(trainData.getLabel().length, 3);
        Assert.assertEquals(model.pred.length, 1);
        Assert.assertEquals(model.pred[0].length, 3);
        for (double d : model.pred[0]) {
            Assert.assertEquals(d, 0.99510191, 1e-6);
        }
    }

    /**
     * firstRoundPred
     * countPoisson && FirstPredictType.ZERO
     */
    @Test
    public void firstRoundPred2() {
        FederatedGBModel model = new FederatedGBModel();
        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainInputStd();
        String[][] raw = compoundInput._1().get();
        String[] result = compoundInput._2().get();
        Features features = compoundInput._3().get();
        FgbParameter fp = new FgbParameter.Builder(50, new MetricType[]{MetricType.ACC}, ObjectiveType.countPoisson)
                .firstRoundPred(FirstPredictType.ZERO).build();
        BoostTrainData trainData = model.trainInit(raw, result, new int[0], fp, features, new HashMap<>());

        Assert.assertEquals(trainData.getDatasetSize(), 3);
        Assert.assertEquals(trainData.getFeatureDim(), 3);
        Assert.assertEquals(trainData.getLabel().length, 3);
        Assert.assertEquals(model.pred.length, 1);
        Assert.assertEquals(model.pred[0].length, 3);
        for (double d : model.pred[0]) {
            Assert.assertEquals(d, 0.0, 1e-6);
        }
    }

    /**
     * firstRoundPred
     * countPoisson && FirstPredictType.RANDOM
     */
    @Test
    public void firstRoundPred3() {
        FederatedGBModel model = new FederatedGBModel();
        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainInputStd();
        String[][] raw = compoundInput._1().get();
        String[] result = compoundInput._2().get();
        Features features = compoundInput._3().get();
        FgbParameter fp = new FgbParameter.Builder(50, new MetricType[]{MetricType.ACC}, ObjectiveType.countPoisson).firstRoundPred(FirstPredictType.RANDOM).build();
        BoostTrainData trainData = model.trainInit(raw, result, new int[0], fp, features, new HashMap<>());

        Assert.assertEquals(trainData.getDatasetSize(), 3);
        Assert.assertEquals(trainData.getFeatureDim(), 3);
        Assert.assertEquals(trainData.getLabel().length, 3);
        Assert.assertEquals(model.pred.length, 1);
        Assert.assertEquals(model.pred[0].length, 3);
        // != 0.0 and != avg
        for (double d : model.pred[0]) {
            Assert.assertNotEquals(d, 0.0, 1e-6);
            Assert.assertNotEquals(d, 0.99510191, 1e-6);
        }
    }

    /**
     * firstRoundPred
     * binaryLogistic && FirstPredictType.AVG
     */
    @Test
    public void firstRoundPred4() {
        FederatedGBModel model = new FederatedGBModel();
        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainInputStd();
        String[][] raw = compoundInput._1().get();
        String[] result = compoundInput._2().get();
        Features features = compoundInput._3().get();
        FgbParameter fp = new FgbParameter.Builder(50, new MetricType[]{MetricType.ACC}, ObjectiveType.binaryLogistic).build();
        BoostTrainData trainData = model.trainInit(raw, result, new int[0], fp, features, new HashMap<>());

        Assert.assertEquals(trainData.getDatasetSize(), 3);
        Assert.assertEquals(trainData.getFeatureDim(), 3);
        Assert.assertEquals(trainData.getLabel().length, 3);
        Assert.assertEquals(model.pred.length, 1);
        Assert.assertEquals(model.pred[0].length, 3);
        for (double d : model.pred[0]) {
            Assert.assertEquals(d, 2.705, 1e-6);
        }
    }

    /**
     * firstRoundPred
     * binaryLogistic && FirstPredictType.ZERO
     */
    @Test
    public void firstRoundPred5() {
        FederatedGBModel model = new FederatedGBModel();
        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainInputStd();
        String[][] raw = compoundInput._1().get();
        String[] result = compoundInput._2().get();
        Features features = compoundInput._3().get();

        FgbParameter fp = new FgbParameter.Builder(50, new MetricType[]{MetricType.ACC}, ObjectiveType.binaryLogistic)
                .firstRoundPred(FirstPredictType.ZERO).build();
        BoostTrainData trainData = model.trainInit(raw, result, new int[0], fp, features, new HashMap<>());

        Assert.assertEquals(trainData.getDatasetSize(), 3);
        Assert.assertEquals(trainData.getFeatureDim(), 3);
        Assert.assertEquals(trainData.getLabel().length, 3);
        Assert.assertEquals(model.pred.length, 1);
        Assert.assertEquals(model.pred[0].length, 3);
        for (double d : model.pred[0]) {
            Assert.assertEquals(d, 0.0, 1e-6);
        }
    }

    /**
     * firstRoundPred
     * binaryLogistic && FirstPredictType.RANDOM
     */
    @Test
    public void firstRoundPred6() {
        FederatedGBModel model = new FederatedGBModel();
        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainInputStd();
        String[][] raw = compoundInput._1().get();
        String[] result = compoundInput._2().get();
        Features features = compoundInput._3().get();
        FgbParameter fp = new FgbParameter.Builder(50, new MetricType[]{MetricType.ACC}, ObjectiveType.binaryLogistic)
                .firstRoundPred(FirstPredictType.RANDOM).build();
        BoostTrainData trainData = model.trainInit(raw, result, new int[0], fp, features, new HashMap<>());

        Assert.assertEquals(trainData.getDatasetSize(), 3);
        Assert.assertEquals(trainData.getFeatureDim(), 3);
        Assert.assertEquals(trainData.getLabel().length, 3);
        Assert.assertEquals(model.pred.length, 1);
        Assert.assertEquals(model.pred[0].length, 3);
        // != 0.0 and != avg
        for (double d : model.pred[0]) {
            Assert.assertNotEquals(d, 0.0, 1e-6);
            Assert.assertNotEquals(d, 2.705, 1e-6);
        }
    }

    /**
     * method: train
     * condition: null trainID
     */
    @Test
    public void train0_1() {
        FederatedGBModel model = new FederatedGBModel();
        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainInputStd();
        String[][] raw = compoundInput._1().get();
        String[] result = compoundInput._2().get();
        Features features = compoundInput._3().get();
        FgbParameter fp = new FgbParameter.Builder(50, new MetricType[]{MetricType.ACC}, ObjectiveType.binaryLogistic).build();
        BoostTrainData trainData = model.trainInit(raw, result, new int[0], fp, features, new HashMap<>());

        try {
            model.train(1, null, trainData);
        } catch (NullPointerException e) {
            Assert.assertEquals(e.getMessage(), null);
        }
    }

    /**
     * method: updateGradHess
     * condition: scalePosWeight != 1; grad[i] < 0.0001
     */
    @Test
    public void updateGradHess() {
        FederatedGBModel model1 = new FederatedGBModel();
        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainClassInputStd();
        String[][] raw = compoundInput._1().get();
        String[] result = compoundInput._2().get();
        Features features = compoundInput._3().get();

        FgbParameter fp1 = new FgbParameter.Builder(50, new MetricType[]{MetricType.ACC}, ObjectiveType.binaryLogistic).gamma(0.6).minSampleSplit(30).scalePosWeight(5).maxDepth(5).build();
        model1.trainInit(raw, result, new int[0], fp1, features, new HashMap<>());

        Assert.assertEquals(model1.grad.length, 1);
        Assert.assertEquals(model1.grad[0].length, 3);
        Assert.assertEquals(model1.hess.length, 1);
        Assert.assertEquals(model1.hess[0].length, 3);
        double[] target_g = {-1.696218156, 0.6607563688, -1.696218156};
        double[] target_h = {1.1207869495, 0.2241573899, 1.1207869495};
        for (int i = 0; i < model1.grad[0].length; i++) {
            Assert.assertEquals(target_g[i], model1.grad[0][i], 1e-6);
            Assert.assertEquals(target_h[i], model1.hess[0][i], 1e-6);
        }

    }

    /**
     * method: train
     * condition: phase not in 1,2,3,4,5
     */
    @Test
    public void train0_2() {
        FederatedGBModel model = new FederatedGBModel();
        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainInputStd();
        String[][] raw = compoundInput._1().get();
        String[] result = compoundInput._2().get();
        Features features = compoundInput._3().get();
        FgbParameter fp = new FgbParameter.Builder(50, new MetricType[]{MetricType.ACC}, ObjectiveType.binaryLogistic).build();
        BoostTrainData trainData = model.trainInit(raw, result, new int[0], fp, features, new HashMap<>());

        try {
            model.train(6, null, trainData);
        } catch (UnsupportedOperationException e) {
            Assert.assertEquals(e.getMessage(), "unsupported phase in federated gradient boost model");
        }
    }

    /**
     * method: train
     * condition: phase1 without label
     */
    @Test
    public void train1_1() {
        FederatedGBModel model = new FederatedGBModel();
        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainInputStdNoLabel();
        String[][] raw = compoundInput._1().get();
        String[] result = compoundInput._2().get();
        Features features = compoundInput._3().get();
        FgbParameter fp = new FgbParameter.Builder(50, new MetricType[]{MetricType.ACC}, ObjectiveType.binaryLogistic).build();
        BoostTrainData trainData = model.trainInit(raw, result, new int[0], fp, features, new HashMap<>());

        EncryptedGradHess res = (EncryptedGradHess) model.train(1, null, trainData);
        Assert.assertEquals(res.getClient(), null);
        Assert.assertEquals(res.getInstanceSpace(), null);
    }

    /**
     * method: train, trainPhase1, treeInit
     * condition: phase1 with label, new tree, getNumClass > 1
     */
    @Test
    public void train1_2() {

        FederatedGBModel model = new FederatedGBModel();
        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainClassInputStd();
        String[][] raw = compoundInput._1().get();
        String[] result = compoundInput._2().get();
        Features features = compoundInput._3().get();
        FgbParameter fp = new FgbParameter.Builder(50, new MetricType[]{MetricType.ACC}, ObjectiveType.multiSoftmax).numClass(2).build();
        BoostTrainData trainData = model.trainInit(raw, result, new int[0], fp, features, new HashMap<>());

        ClientInfo client = new ClientInfo("10.0.0.4", 1092, "TCP");
        Message jsonData = new BoostP1Req(client, true);
        EncryptedGradHess res = (EncryptedGradHess) model.train(1, jsonData, trainData);
        Assert.assertEquals(res.getNewTree(), true);
        Assert.assertEquals(res.getInstanceSpace().length, 3);
        Assert.assertNotNull(res.getPubKey());
        Assert.assertNotNull(res.getGh());
        // TODO 补充解密后g,h相同
    }

    /**
     * method: train, trainPhase1, treeInit
     * condition: phase1 with label, existed tree, getNumClass > 1
     */
    @Test
    public void train1_3() {

        // TODO

    }

    /**
     * method: train
     * condition: phase2, client with label (Active)
     */
    @Test
    public void train2_1() {
        FederatedGBModel model = new FederatedGBModel();
        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainClassInputStd();
        String[][] raw = compoundInput._1().get();
        String[] result = compoundInput._2().get();
        Features features = compoundInput._3().get();
        FgbParameter fp = new FgbParameter.Builder(50, new MetricType[]{MetricType.ACC}, ObjectiveType.multiSoftmax).numClass(2).build();
        BoostTrainData trainData = model.trainInit(raw, result, new int[0], fp, features, new HashMap<>());

        ClientInfo client = new ClientInfo("10.0.0.4", 1092, "TCP");
        Message jsonData = new EncryptedGradHess(client, null);
        BoostP2Res bp2r = (BoostP2Res) model.train(2, jsonData, trainData);
        Assert.assertNull(bp2r.getFeatureGL());
    }

    /**
     * method: train
     * condition: phase2, clients without label (Passive), new tree
     */
    @Test
    public void train2_2() {
        FederatedGBModel model = new FederatedGBModel();
        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainClassInputStdNoLabel();
        String[][] raw = compoundInput._1().get();
        String[] result = compoundInput._2().get();
        Features features = compoundInput._3().get();

        BoostTrainData trainData = model.trainInit(raw, result, new int[0], fp, features, new HashMap<>());

        ClientInfo client = new ClientInfo("10.0.0.4", 1092, "TCP");
        EncryptionTool encryptionTool = new FakeTool();
        PrivateKey privateKey = encryptionTool.keyGenerate(1024, 64);
        // publicKey
        String publicKey = privateKey.generatePublicKey().serialize();
        StringTuple2[] gh = new StringTuple2[]{new StringTuple2("18", "5"), new StringTuple2("10", "6"),
                new StringTuple2("8", "5")};
        Message jsonData = new EncryptedGradHess(client, new int[]{0, 1, 2}, gh, publicKey, true);
        BoostP2Res bp2r = model.trainPhase2(jsonData, trainData, encryptionTool);
        FeatureLeftGH[] flgh = bp2r.getFeatureGL();
        Assert.assertEquals(flgh.length, 2);
        Assert.assertEquals(flgh[0].getClient(), client);
        Assert.assertEquals(flgh[1].getClient(), client);
        Assert.assertEquals(flgh[0].getFeature(), "1");
        Assert.assertEquals(flgh[1].getFeature(), "2");
        Assert.assertEquals(flgh[0].getGhLeft(), new StringTuple2[]{new StringTuple2("10.0", "6.0"),
                new StringTuple2("18.0", "5.0"), new StringTuple2("8.0", "5.0")});
        Assert.assertEquals(flgh[1].getGhLeft(), new StringTuple2[]{new StringTuple2("8.0", "5.0"),
                new StringTuple2("10.0", "6.0"), new StringTuple2("18.0", "5.0")});
    }

    @Test
    public void train2_3() {
        /**
         method: train
         condition: phase2, clients without label (Passive), existed tree
         */
    }

    @Test
    public void train3_1() {
        /**
         method: train
         condition: phase3, clients with label (Active), new tree
         */
        FederatedGBModel model = new FederatedGBModel();
        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainClassInputStd();
        String[][] raw = compoundInput._1().get();
        String[] result = compoundInput._2().get();
        Features features = compoundInput._3().get();
        FgbParameter fp = new FgbParameter.Builder(3, new MetricType[]{MetricType.ACC}, ObjectiveType.regLogistic).build();
        BoostTrainData trainData = model.trainInit(raw, result, new int[0], fp, features, new HashMap<>());

        ClientInfo client = new ClientInfo("10.0.0.4", 1092, "TCP");
        ClientInfo client2 = new ClientInfo("10.0.0.3", 1092, "TCP");
        EncryptionTool encryptionTool = new FakeTool();
        PrivateKey privateKey = encryptionTool.keyGenerate(1024, 64);
        // Phase3 Request
        FeatureLeftGH[] flgh = new FeatureLeftGH[]{new FeatureLeftGH(client, "1",
                new StringTuple2[]{new StringTuple2("10", "6"), new StringTuple2("10", "5"), new StringTuple2("10", "5"), new StringTuple2("20", "3")}),
                new FeatureLeftGH(client, "2",
                        new StringTuple2[]{new StringTuple2("30", "6"), new StringTuple2("10", "5"), new StringTuple2("10", "5"), new StringTuple2("20", "3")}),
                new FeatureLeftGH(client2, "2",
                        new StringTuple2[]{new StringTuple2("20", "6"), new StringTuple2("10", "5"), new StringTuple2("10", "5"), new StringTuple2("20", "3")}),
        };
        List<BoostP2Res> bp2rList = new ArrayList<BoostP2Res>();
        bp2rList.add(new BoostP2Res(flgh));
        BoostP3Req jsonData = new BoostP3Req(client, bp2rList);
        // global variables
        TreeNode currentNode = new TreeNode();
        currentNode.GradSetter(200.0);
        currentNode.HessSetter(200.0);
        currentNode.instanceSpace = new int[]{0, 1, 2};
        double[][] grad = new double[][]{{10.0, 8.0, 9.0, 2.0, 2.0, 5.0, 20.0, 2.0, 5.0, 20.0}};
        double[][] hess = new double[][]{{10.0, 8.0, 9.0, 2.0, 2.0, 5.0, 20.0, 2.0, 5.0, 20.0}}; // 假设所有client端共有10个样本
        int numClassRound = 0;
        Tuple2<BoostP3Res, Double> res = model.trainPhase3(jsonData, trainData, currentNode, encryptionTool, privateKey,
                fp, grad, hess, numClassRound);
        BoostP3Res bp3r = res._1();
        System.out.println(bp3r.getFeature());
        Assert.assertEquals(res._2(), 38.885790826089334, 1e-6);
        Assert.assertEquals(bp3r.getFeature(), "2");
        Assert.assertEquals(bp3r.getIndex(), 0);
        Assert.assertEquals(bp3r.getClient(), client);

    }

    @Test
    public void train3_2() {
        /**
         method: trainPhase3
         condition: phase3, clients without label (Passive), new tree
         */
        FederatedGBModel model = new FederatedGBModel();
        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainClassInputStdNoLabel();
        String[][] raw = compoundInput._1().get();
        String[] result = compoundInput._2().get();
        Features features = compoundInput._3().get();
        FgbParameter fp = new FgbParameter.Builder(3, new MetricType[]{MetricType.ACC}, ObjectiveType.regLogistic).build();
        BoostTrainData trainData = model.trainInit(raw, result, new int[0], fp, features, new HashMap<>());

        ClientInfo client = new ClientInfo("10.0.0.4", 1092, "TCP");
        EncryptionTool encryptionTool = new FakeTool();
        PrivateKey privateKey = encryptionTool.keyGenerate(1024, 64);
        // publicKey
        String publicKey = privateKey.generatePublicKey().serialize();
        // Phase3 Request
        BoostP3Req jsonData = new BoostP3Req(client, new ArrayList<BoostP2Res>());
        // global variables
        TreeNode currentNode = new TreeNode();
        double[][] grad = {{}};
        double[][] hess = {{}};
        int numClassRound = 0;
        Tuple2<BoostP3Res, Double> res = model.trainPhase3(jsonData, trainData, currentNode, encryptionTool, privateKey,
                fp, grad, hess, numClassRound);
        Assert.assertNull(res._1().getClient());
        Assert.assertNull(res._1().getFeature());
        Assert.assertEquals(res._1().getIndex(), 0);
    }

    @Test
    public void train4_1() {
        /**
         method: trainPhase4
         condition: client does not match
         */
        FederatedGBModel model = new FederatedGBModel();
        BoostP4Req jsonData = new BoostP4Req(false);
        //TODO Assert
        Map<Integer, List<Bucket>> sortedFeatureMap = new HashMap<>();
        List<QueryEntry> passiveQueryTable = new ArrayList<>();
        Tuple2<LeftTreeInfo, List<QueryEntry>> req = model.trainPhase4(jsonData, sortedFeatureMap, passiveQueryTable);
        Assert.assertEquals(req._1().getRecordId(), 0);
        Assert.assertNull(req._1().getLeftInstances());

    }

    @Test
    public void train4_2() {
        /**
         method: trainPhase4
         condition: client does match, null & notnull passiveQueryTable
         */
        FederatedGBModel model = new FederatedGBModel();
        ClientInfo client = new ClientInfo("10.0.0.4", 1092, "TCP");
        BoostP4Req jsonData = new BoostP4Req(client, 1, 1, true); // 第2个特征，第2个分裂点
        // 2 feats
        Map<Integer, List<Bucket>> sortedFeatureMap = new HashMap<>();
        // feat1's bucket
        List<Bucket> Buckets = new ArrayList<>();
        // 2个分桶， 4个uid
        Buckets.add(new Bucket(new double[]{1.0, 3.0}, new double[]{5.6, 8.6}));
        Buckets.add(new Bucket(new double[]{0.0, 2.0}, new double[]{9.5, 18.6}));
        // feat2's bucket
        List<Bucket> Buckets2 = new ArrayList<>();
        // 2个分桶， 4个uid
        Buckets2.add(new Bucket(new double[]{3.0}, new double[]{2.0}));
        Buckets2.add(new Bucket(new double[]{0.0, 2.0, 1.0}, new double[]{5.0, 9.0, 10.0}));
        sortedFeatureMap.put(0, Buckets);
        sortedFeatureMap.put(1, Buckets2);
        // null passiveQueryTable
        List<QueryEntry> passiveQueryTable = new ArrayList<>();
        Tuple2<LeftTreeInfo, List<QueryEntry>> req = model.trainPhase4(jsonData, sortedFeatureMap, passiveQueryTable);

        Assert.assertEquals(req._1().getRecordId(), 1);
        double[] target = new double[]{3.0, 0.0, 2.0, 1.0};
        for (int i = 0; i < req._1().getLeftInstances().length; i++) {
            Assert.assertEquals(req._1().getLeftInstances()[i], target[i]);
        }

        // not null passiveQueryTable
        List<QueryEntry> passiveQueryTable2 = new ArrayList<>();
        passiveQueryTable2.add(new QueryEntry(1, 1, 10.0));
        Tuple2<LeftTreeInfo, List<QueryEntry>> req2 = model.trainPhase4(jsonData, sortedFeatureMap, passiveQueryTable2);
        Assert.assertEquals(req2._1().getRecordId(), 2);
        double[] target2 = new double[]{3.0, 0.0, 2.0, 1.0};
        for (int i = 0; i < req2._1().getLeftInstances().length; i++) {
            Assert.assertEquals(req2._1().getLeftInstances()[i], target2[i]);
        }
    }

    @Test
    public void postPrune1() {
        /**
         * method: postPrune
         * condition: root is leaf
         */
        ClientInfo client = new ClientInfo("10.0.0.4", 1092, "TCP");
        FederatedGBModel model = new FederatedGBModel();
        // isLeaf
        TreeNode tn1 = new TreeNode(0, 3, 10, true);
        tn1.leafNodeSetter(10.0, true);
        TreeNode tn1_copy = new TreeNode(0, 3, 10, true);
        tn1_copy.leafNodeSetter(10.0, true);
//        TreeNode tn2 = new TreeNode(0, 0, client, 0, 0.0);
        TreeNode[] correspondingTreeNode = new TreeNode[]{};
        FgbParameter fgbParameter = new FgbParameter.Builder(3, new MetricType[]{MetricType.ACC}, ObjectiveType.regLogistic).build();
        ;
        model.postPrune(tn1, fgbParameter, correspondingTreeNode);
        Assert.assertEquals(tn1.client, tn1_copy.client);
        Assert.assertEquals(tn1.depth, tn1_copy.depth);
        Assert.assertEquals(tn1.featureDim, tn1_copy.featureDim);
        Assert.assertEquals(tn1.gain, tn1_copy.gain);

    }

    @Test
    public void postPrune2() {
        /**
         * method: postPrune
         * condition: root is not leaf
         */
        ClientInfo client = new ClientInfo("10.0.0.4", 1092, "TCP");
        FederatedGBModel model = new FederatedGBModel();
        // child
        TreeNode child1 = new TreeNode(2, 20);
        child1.instanceSpace = new int[]{0, 1};
        TreeNode child2 = new TreeNode(3, 10);
        child2.instanceSpace = new int[]{2};
        child1.gain = -10;
        child2.gain = -10;

        // not Leaf
        TreeNode tn1 = new TreeNode(1, 2, 10, false);
        tn1.instanceSpace = new int[]{0, 1, 2};
        tn1.leafNodeSetter(10.0, false);
        tn1.Grad = 100.0;
        tn1.Hess = 50.0;
        tn1.leftChild = child1;
        tn1.rightChild = child2;
        tn1.gain = -10;

        TreeNode tn1_copy = new TreeNode(1, 2, 10, false);
        tn1.instanceSpace = new int[]{0, 1, 2};
        tn1_copy.leafNodeSetter(10.0, false);
        tn1_copy.Grad = 100.0;
        tn1_copy.Hess = 50.0;
        tn1_copy.leftChild = child1;
        tn1_copy.rightChild = child2;
        tn1_copy.gain = -10;
        Assert.assertEquals(tn1.leftChild.leafScore, tn1_copy.leftChild.leafScore);
        Assert.assertEquals(tn1.leafScore, tn1_copy.leafScore);
        TreeNode[] correspondingTreeNode = new TreeNode[3];
        FgbParameter fp = new FgbParameter.Builder(3, new MetricType[]{MetricType.ACC}, ObjectiveType.regLogistic).build();
        ;
        model.postPrune(tn1, fp, correspondingTreeNode);
        Assert.assertNotEquals(tn1.leafScore, tn1_copy.leafScore);
    }


    @Test
    public void train5_1() {
        /**
         * method: trainPhase5
         * condition: Passive client
         */
        FederatedGBModel model = new FederatedGBModel();
        FgbParameter fp = new FgbParameter.Builder(3, new MetricType[]{MetricType.ACC}, ObjectiveType.regLogistic).build();
        ClientInfo client = new ClientInfo("10.0.0.4", 1092, "TCP");
        // jsonData: (int recordId, int[] leftIns)
        LeftTreeInfo jsonData = new LeftTreeInfo(1, new int[10]);
        // grad
        double[][] grad = new double[][]{{10.0, 8.0, 9.0, 2.0, 2.0, 5.0, 20.0, 2.0, 5.0, 20.0}};
        double[][] hess = new double[][]{{10.0, 8.0, 9.0, 2.0, 2.0, 5.0, 20.0, 2.0, 5.0, 20.0}}; // 假设所有client端共有10个样本
        int numClassRound = 10;
        TreeNode currentNode = new TreeNode();
        currentNode.GradSetter(200.0);
        currentNode.HessSetter(200.0);
        currentNode.instanceSpace = new int[]{0, 1, 2};
        Queue<TreeNode> newTreeNodes = new LinkedList<>();
        TreeNode[] correspondingTreeNode = new TreeNode[3];
        List<Tree> trees = new ArrayList<>();
        MetricValue metricMap = null;
        int datasetSize = 10;
        Loss loss = new LogisticLoss();
        double[][] pred = new double[][]{{}};
        double[] label = {};
        int depth = 2;
        //TODO Assert
        List res = model.trainPhase5(jsonData, grad, hess, numClassRound, currentNode,
                newTreeNodes, fp, correspondingTreeNode, metricMap, trees, loss,
                datasetSize, pred, label, depth);
        BoostP5Res bp5r = (BoostP5Res) res.get(0);
        Assert.assertEquals(bp5r.getDepth(), 0);
        Assert.assertFalse(bp5r.isStop());

    }

    @Test
    public void train5_2() {
        /**
         * method: trainPhase5
         * condition: Active client
         */
        FederatedGBModel model = new FederatedGBModel();
        model.hasLabel = true;
        FgbParameter fp = new FgbParameter.Builder(3, new MetricType[]{MetricType.RMSE}, ObjectiveType.regSquare).build();
        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainClassInputStd();
        String[][] raw = compoundInput._1().get();
        String[] result = compoundInput._2().get();
        Features features = compoundInput._3().get();
        BoostTrainData trainData = model.trainInit(raw, result, new int[0], fp, features, new HashMap<>());
        ClientInfo client = new ClientInfo("10.0.0.4", 1092, "TCP");
        // jsonData: (int recordId, int[] leftIns)
        LeftTreeInfo jsonData = new LeftTreeInfo(1, new int[]{0, 1, 2}); // 左子树有3个datapoint
        // grad
        double[][] grad = new double[][]{{10.0, 8.0, 9.0, 2.0, 2.0, 5.0, 20.0, 2.0, 5.0, 20.0}};
        double[][] hess = new double[][]{{10.0, 8.0, 9.0, 2.0, 2.0, 5.0, 20.0, 2.0, 5.0, 20.0}}; // 假设所有client端共有10个样本
        int numClassRound = 0;
        TreeNode currentNode = new TreeNode();
        currentNode.isLeaf = true;
        currentNode.GradSetter(200.0);
        currentNode.HessSetter(200.0);
        currentNode.instanceSpace = new int[]{0, 1, 2, 3};
        Queue<TreeNode> newTreeNodes = new LinkedList<>();
        TreeNode[] correspondingTreeNode = new TreeNode[]{currentNode, currentNode, currentNode,
                currentNode, new TreeNode(), new TreeNode(), new TreeNode(), new TreeNode(), new TreeNode(),
                new TreeNode()};
        TreeNode root = new TreeNode(1, 1, 10, true);
        Tree tree = new Tree(root);
        List<Tree> trees = new ArrayList<>();
        trees.add(tree);
        Map<MetricType, List<Pair<Integer, Double>>> metricMap = new HashMap<>();
        List<Pair<Integer, Double>> Metricvalue = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Metricvalue.add(new Pair<>(i, 0.1));
        }
        metricMap.put(MetricType.RMSE, Metricvalue);
        MetricValue metricValue = new MetricValue(metricMap);
        int datasetSize = 10;
        Loss loss = new LogisticLoss();
        double[][] pred = new double[1][10];
        double[] label = {1, 0, 0, 1, 1, 0, 0, 1, 0, 0};
        int depth = 2;
        //TODO Assert
        List res = model.trainPhase5(jsonData, grad, hess, numClassRound, currentNode,
                newTreeNodes, fp, correspondingTreeNode, metricValue, trees, loss,
                datasetSize, pred, label, depth);
        BoostP5Res bp5r = (BoostP5Res) res.get(0);
        Assert.assertTrue(bp5r.isStop());
        Assert.assertNull(bp5r.getTrainMetric());
        Assert.assertEquals(bp5r.getDepth(), 0);


    }

    @Test
    public void inferenceInit() {
        FederatedGBModel model = new FederatedGBModel();
        String[] uidList = new String[]{"aa", "1a", "c3"};
        String[][] data = new String[2][];
        data[0] = new String[]{"aa", "10", "12.1"};
        data[1] = new String[]{"1a", "10", "12.1"};
        Message msg = model.inferenceInit(uidList, data, new HashMap<>());
        InferenceInitRes res = (InferenceInitRes) msg;
        Assert.assertFalse(res.isAllowList());
        System.out.println(res.getUid()[0]);
        Assert.assertEquals(res.getUid(), new int[]{2}); // uidList中的第三个没有在data出现所以返回2这个index
    }


    @Test
    public void inference1() {
        FederatedGBModel model = new FederatedGBModel();
        String[] uidList = new String[]{"0", "1", "2"};
        String[][] data = new String[4][];
        data[0] = new String[]{"uid", "f1", "f2"};
        data[1] = new String[]{"0", "10", "12.1"};
        data[2] = new String[]{"1", "10", "12.1"};
        data[3] = new String[]{"2", "10", "12.1"};
        String[] originIdArray = new String[]{"0", "1"};
        ClientInfo client = new ClientInfo("10.0.0.4", 1092, "TCP");

        StringArray parameterData = new StringArray(originIdArray);
        InferenceData inferenceData = CommonLoad.constructInference(AlgorithmType.FederatedGB, data);
        // empty tree
        List<Tree> trees = new ArrayList<Tree>();
        List<Tree> trees2 = new ArrayList<Tree>();
        trees2.add(new Tree(new TreeNode(1, 1, client, 1, 0.0)));
        double firstRoundPred = 5.0;
        List<Double> multiClassUniqueLabelList = new ArrayList<Double>();
        multiClassUniqueLabelList.add(0.0);
        multiClassUniqueLabelList.add(1.0);
//        inferencePhase1(StringArray data, InferenceData samples, List<Tree> trees, double firstRoundPred, List<Double> multiClassUniqueLabelList) {
        BoostN1Res bn1r = (BoostN1Res) model.inferencePhase1(parameterData, inferenceData, trees, firstRoundPred, multiClassUniqueLabelList);
        Assert.assertEquals(bn1r.getFirstRoundPred(), 0.0);
        Assert.assertNull(bn1r.getTrees());
        Assert.assertNull(bn1r.getMultiClassUniqueLabelList());
        // not emtry tree
        BoostN1Res bn1r2 = (BoostN1Res) model.inferencePhase1(parameterData, inferenceData, trees2, firstRoundPred, multiClassUniqueLabelList);
        Assert.assertEquals(bn1r2.getFirstRoundPred(), 5.0);
        Assert.assertEquals(bn1r2.getTrees(), trees2);
        Assert.assertEquals(bn1r2.getMultiClassUniqueLabelList(), multiClassUniqueLabelList);
    }

    @Test
    public void inference2() {
//        FederatedGBModel model = new FederatedGBModel();
//        String[] uidList = new String[]{"aa", "1a", "c3"};
//        String[][] data = new String[2][];
//        data[0] = new String[]{"aa", "10", "12.1"};
//        data[1] = new String[]{"1a", "10", "12.1"};
//        //StringArray parameterData, InferenceData inferenceData, List<Tree> trees, double firstRoundPred, List<Double> multiClassUniqueLabelList
//        StringArray parameterData = new StringArray(new String[]{"aa", "bb"});
//        InferenceData inferenceData = new BoostInferenceData(data);
//        Message msg = model.inferencePhase2(parameterData, inferenceData, new ArrayList<>(), 10.0, new ArrayList<>());
//        InferenceInitRes res = (InferenceInitRes) msg;
//        Assert.assertFalse(res.isAllowList());
//        Assert.assertEquals(res.getUid(), new int[]{2});
    }

    @Test
    public void sortAndGroup() {
        double[][] mat = {{1.0, 10.0}, {2.0, 9.0}, {3.0, 1.0}, {4.0, 2.0}};
        FederatedGBModel model = new FederatedGBModel();
        // condition1: map size < max numbin
        List<Bucket> res1 = model.sortAndGroup(mat, 10);
        Assert.assertEquals(res1.size(), 4);
        Assert.assertEquals(res1.get(0).getIds(), new double[]{3.0});
        Assert.assertEquals(res1.get(0).getSplitValue(), 1.0);
        Assert.assertEquals(res1.get(0).getValues(), new double[]{1.0});
        Assert.assertEquals(res1.get(1).getIds(), new double[]{4.0});
        Assert.assertEquals(res1.get(1).getSplitValue(), 2.0);
        Assert.assertEquals(res1.get(1).getValues(), new double[]{2.0});

        List<Bucket> res2 = model.sortAndGroup(mat, 2);
        Assert.assertEquals(res2.size(), 2);
        Assert.assertEquals(res2.get(0).getIds(), new double[]{3.0, 4.0});
        Assert.assertEquals(res2.get(0).getSplitValue(), 2.0);
        Assert.assertEquals(res2.get(0).getValues(), new double[]{1.0, 2.0});
        Assert.assertEquals(res2.get(1).getIds(), new double[]{2.0, 1.0});
        Assert.assertEquals(res2.get(1).getSplitValue(), 10.0);
        Assert.assertEquals(res2.get(1).getValues(), new double[]{9.0, 10.0});
    }

    @Test
    public void ghSum() {
        // buckets for feature; 2 features in this case
        List<Bucket> buckets = new ArrayList<Bucket>();
        double[][] mat1 = {{1.0, 1.0}, {2.0, 9.0}, {3.0, 10.0}};
        double[][] mat2 = {{1.0, 8.0}, {2.0, 9.0}};
        buckets.add(new Bucket(mat1));
        buckets.add(new Bucket(mat2));
        // Gtotal and Htotal for each instance, 3 instance in this case
        StringTuple2[] gh = new StringTuple2[]{new StringTuple2("10", "5"), new StringTuple2("10", "2"), new StringTuple2("10", "6")};
        int[] instanceSpace = {1, 2, 3}; // match mat1, mat2 index
        // encrytionTool
        EncryptionTool encryptionTool = new FakeTool();
        PrivateKey privateKey = encryptionTool.keyGenerate(1024, 64);
        // publicKey
        PublicKey publicKey = privateKey.generatePublicKey();
        // ghMap2
        Map<Integer, Tuple2<Ciphertext, Ciphertext>> ghMap2 = new HashMap<Integer, Tuple2<Ciphertext, Ciphertext>>();
        for (int i = 0; i < instanceSpace.length; i++) {
            ghMap2.put(instanceSpace[i], new Tuple2<>(encryptionTool.restoreCiphertext(gh[i].getFirst()), encryptionTool.restoreCiphertext(gh[i].getSecond())));
        }
        FederatedGBModel model = new FederatedGBModel();
        StringTuple2[] res = model.ghSum(buckets, ghMap2, publicKey, encryptionTool);
        // Test
        Assert.assertEquals(res.length, 2); // 2 features
        Assert.assertEquals(res[0].getFirst(), "30.0");
        Assert.assertEquals(res[0].getSecond(), "13.0");
        Assert.assertEquals(res[1].getFirst(), "20.0");
        Assert.assertEquals(res[1].getSecond(), "7.0");

    }

    @Test
    public void processEachNumericFeature2() {
        // buckets
        List<Bucket> buckets = new ArrayList<>();
        double[][] mat1 = {{1.0, 1.0}, {2.0, 9.0}, {3.0, 10.0}};
        double[][] mat2 = {{1.0, 8.0}, {2.0, 9.0}};
        buckets.add(new Bucket(mat1));
        buckets.add(new Bucket(mat2));
        // ghMap
        Map<Integer, DoubleTuple2> ghMap = new HashMap<>();
        ghMap.put(1, new DoubleTuple2(10.0, 5.0));
        ghMap.put(2, new DoubleTuple2(10.0, 2.0));
        ghMap.put(3, new DoubleTuple2(10.0, 6.0));
        FederatedGBModel model = new FederatedGBModel();
        DoubleTuple2[] res = model.processEachNumericFeature2(buckets, ghMap);
        Assert.assertEquals(res.length, 2);
        Assert.assertEquals(res[0].getFirst(), 30.0);
        Assert.assertEquals(res[0].getSecond(), 13.0);
        Assert.assertEquals(res[1].getFirst(), 20.0);
        Assert.assertEquals(res[1].getSecond(), 7.0);

    }

    @Test
    public void computeGain() {
        // decryptedGH: 3 buckets for this feature
        DoubleTuple2[] decryptedGH = new DoubleTuple2[]{new DoubleTuple2(30.0, 13.0),
                new DoubleTuple2(50.0, 7.0), new DoubleTuple2(80.0, 15.0)};
        // gTotal, hTotal without split
        double g = 100.0;
        double h = 30.0;
        // parameter
        FgbParameter parameter = new FgbParameter.Builder(3, new MetricType[]{MetricType.ACC}, ObjectiveType.regLogistic).build();
        FederatedGBModel model = new FederatedGBModel();
        System.out.println(parameter.getGamma());
        System.out.println(parameter.getLambda());
        // Test: lambda = 1; gamma = 0;
        List<Tuple2<Double, Integer>> res = model.computeGain(decryptedGH, g, h, parameter);
        Assert.assertEquals(res.size(), 2);
        Assert.assertEquals(res.get(0)._1(), 6.96364567, 1e-6);
        Assert.assertEquals(res.get(1)._1(), 9.27244798, 1e-6);

    }

    /**
     * GainOutput fetchGain(FeatureLeftGH input, double g, double h,
     * EncryptionTool encryptionTool, PrivateKey privateKey)
     */
    @Test
    public void fetchGain() {
        ClientInfo client = new ClientInfo("10.0.0.4", 1092, "TCP");
        StringTuple2[] ghLeft = new StringTuple2[]{new StringTuple2("10", "5"), new StringTuple2("10", "2"), new StringTuple2("10", "6")};
        FeatureLeftGH input = new FeatureLeftGH(client, "feat", ghLeft);
        double g = 100.0;
        double h = 30.0;
        FgbParameter parameter = new FgbParameter.Builder(3, new MetricType[]{MetricType.ACC}, ObjectiveType.regLogistic).build();
        // encrytionTool
        EncryptionTool encryptionTool = new FakeTool();
        PrivateKey privateKey = encryptionTool.keyGenerate(1024, 64);
        // publicKey
        PublicKey publicKey = privateKey.generatePublicKey();
        FederatedGBModel model = new FederatedGBModel();
        GainOutput gop = model.fetchGain(input, g, h, encryptionTool, privateKey, parameter);
        Assert.assertEquals(gop.getClient(), client);
        Assert.assertEquals(gop.getFeature(), "feat");
        Assert.assertEquals(gop.getGain(), 2.8122415219, 1e-6);

    }

    @Test
    public void testFetchAllGain(){
        ClientInfo client = new ClientInfo("10.0.0.4", 1092, "TCP");
        StringTuple2[] ghLeft = new StringTuple2[]{new StringTuple2("10", "5"), new StringTuple2("10", "2"), new StringTuple2("10", "6")};
        FeatureLeftGH input = new FeatureLeftGH(client, "feat", ghLeft);
        double g = 100.0;
        double h = 30.0;
        FgbParameter parameter = new FgbParameter.Builder(3, new MetricType[]{MetricType.ACC}, ObjectiveType.regLogistic).build();
        // encrytionTool
        EncryptionTool encryptionTool = new FakeTool();
        PrivateKey privateKey = encryptionTool.keyGenerate(1024, 64);
        // publicKey
        PublicKey publicKey = privateKey.generatePublicKey();
        FederatedGBModel model = new FederatedGBModel();
        List<GainOutput> allGain = model.fetchAllGain(input, g, h, encryptionTool, privateKey, parameter);
        Assert.assertEquals(allGain.size(), 2);
        Assert.assertEquals(allGain.get(0).getGain(), 2.8122415219, 1e-6);
        Assert.assertEquals(allGain.get(0).getFeature(), "feat");
        Assert.assertEquals(allGain.get(0).getSplitIndex(), 0);
    }


    @Test
    public void serialize() {
        FederatedGBModel model = new FederatedGBModel();
        String s = model.serialize();
        Assert.assertEquals(s.split("\n").length, 7);
        Assert.assertEquals(s.split("\n")[6], "tree[end]");
        Assert.assertEquals(s.split("\n")[0], "first_round_predict=0.0");
        model.deserialize(s);
    }

    @Test
    public void FederatedGBModel() {
        List<Tree> trees = new ArrayList<>();
        Loss loss = new Loss();
        double firstRoundPredict = 0.0;
        double eta = 0.0;
        List<QueryEntry> passiveQueryTable = new ArrayList<>();
        List<Double> multiClassUniqueLabelList = new ArrayList<>();
        FederatedGBModel model1 = new FederatedGBModel(trees, loss, firstRoundPredict, eta, passiveQueryTable, multiClassUniqueLabelList);
        FgbModelSerializer fms = new FgbModelSerializer(trees, loss, firstRoundPredict, eta, passiveQueryTable, multiClassUniqueLabelList);
        FederatedGBModel model2 = new FederatedGBModel(fms);
        String s1 = model1.serialize();
        String s2 = model2.serialize();
        Assert.assertEquals(s1, s2);
    }
}
