package com.jdt.fedlearn.core.model;

import com.jdt.fedlearn.core.dispatch.mixLinear.LinearRegression;
import com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillierNative;
import com.jdt.fedlearn.core.encryption.distributedPaillier.HomoEncryptionDebugUtil;
import com.jdt.fedlearn.core.encryption.distributedPaillier.HomoEncryptionUtil;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.base.Double2dArray;
import com.jdt.fedlearn.core.entity.base.StringArray;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.core.entity.common.CommonResponse;
import com.jdt.fedlearn.core.entity.common.InferenceInitRes;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.entity.feature.SingleFeature;
import com.jdt.fedlearn.core.entity.mixedLinearRegression.*;
import com.jdt.fedlearn.core.entity.psi.MatchResourceLinReg;
import com.jdt.fedlearn.core.fake.StructureGenerate;
import com.jdt.fedlearn.core.loader.linearRegression.LinearInferenceData;
import com.jdt.fedlearn.core.loader.linearRegression.LinearTrainData;
import com.jdt.fedlearn.core.model.mixLinear.LinearRegressionModel;
import com.jdt.fedlearn.core.optimizer.bfgs.WeightedLinRegLossNonprivClient;
import com.jdt.fedlearn.core.optimizer.bfgs.WeightedLinRegLossPriv;
import com.jdt.fedlearn.core.parameter.LinearParameter;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.core.type.data.Tuple3;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

import static com.jdt.fedlearn.core.util.TypeConvUtils.parse1dDouble;


public class TestLinearRegressionModel {
    // encryption and decryption settings
    private static final int n = 3;
    private static final int l = 1024;
    private static final long maxNegAbs = Long.MAX_VALUE;
    private static final boolean usingFakeEnc = false;
    private static final HomoEncryptionUtil key = new HomoEncryptionUtil(n, l, usingFakeEnc);
    private static HomoEncryptionDebugUtil decHelper = null;
    private static LinearTrainData clientTrainData;
    private static LinearInferenceData clientInferData;
    private static Features features;

    // common mock settings for LinReg
    private static final List<ClientInfo> clientInfos = StructureGenerate.threeClients();
    private static final Map<ClientInfo, double[]> clientFeatMap = new HashMap<>();
    private static final String[][] rawTable = new String[4][];
    private static final String[][] rawTableInfer = new String[4][];
    private static final double[][] mockPhi = new double[3][];
    private static double[] mockY = new double[3];
    private static final String inferModel = "modelToken=123-MixLinReg-2103012220\n" +
            "weight=[1, 1, 1, 1, 1, 1, 1, 1]\n" +
            "M=8\n" +
            "numP=3\n" +
            "M_priv=4\n" +
            "weight_priv=[1, 1, 1, 1]\n";

    private LinearRegressionTrainInitOthers mockTrainMatchingRes() {
        int phase = 2;
        LinearRegression linearRegression = new LinearRegression(new LinearParameter());
        Map<String, Object> others = new HashMap<>();
        others.put("pubKeyStr" , key.getPk().toJson());
        Map<ClientInfo, Features> features = StructureGenerate.linRegFeatures(clientInfos);
        linearRegression.initControl(clientInfos, null, features, others);

        // mock client response
        List<Message> msgs = new ArrayList<Message>() {
        };
        msgs.add(new MatchResourceLinReg(new String[]{"f1", "f2", "f3"}, new String[]{"u1", "u2", "u3"}, new double[]{1, 2, 3}));
        msgs.add(new MatchResourceLinReg(new String[]{"f3", "f4", "f5"}, new String[]{"u3", "u4", "u5"}, new double[]{1, 2, 3}));
        msgs.add(new MatchResourceLinReg(new String[]{"f5", "f6", "f7"}, new String[]{"u5", "u6", "u7"}, new double[]{1, 2, 3}));
        List<CommonResponse> response = packageThreeMsgsIntoCommonResponses(msgs);

        linearRegression.setPhase(phase - 1);
        List<CommonRequest> requests = linearRegression.control(response);
        CommonRequest first = requests.get(0);
        return (LinearRegressionTrainInitOthers) first.getBody();
    }

    private LinearRegressionInferInitOthers mockInferMatchingRes() {
        int phase = -1;
        LinearRegression linearRegression = new LinearRegression(new LinearParameter());

        // mock client response
        List<Message> msgs = new ArrayList<Message>() {
        };
        msgs.add(new MatchResourceLinReg(new String[]{"f1", "f2", "f3"}, new String[]{"u1", "u2", "u3"}, new double[]{1, 2, 3}));
        msgs.add(new MatchResourceLinReg(new String[]{"f3", "f4", "f5"}, new String[]{"u3", "u4", "u5"}, new double[]{1, 2, 3}));
        msgs.add(new MatchResourceLinReg(new String[]{"f5", "f6", "f7"}, new String[]{"u5", "u6", "u7"}, new double[]{1, 2, 3}));
        List<CommonResponse> response = packageThreeMsgsIntoCommonResponses(msgs);

        linearRegression.setPhase(phase - 1);
        List<CommonRequest> requests = linearRegression.inferenceControl(response);
        CommonRequest first = requests.get(0);
        return (LinearRegressionInferInitOthers) first.getBody();
    }

    @BeforeMethod
    public void setUp() {
        key.generateKeyStandalone();
        key.getSkAll()[0].setRank(1);
        key.getSkAll()[1].setRank(2);
        key.getSkAll()[2].setRank(3);

        if (usingFakeEnc) {
            decHelper = new HomoEncryptionDebugUtil(true);
        } else {
            decHelper = new HomoEncryptionDebugUtil(key.getPk(), key.getSkAll(), key.getN(), maxNegAbs);
        }
        clientFeatMap.put(clientInfos.get(0), new double[]{1, 1, 1, 0, 0, 0, 0});
        clientFeatMap.put(clientInfos.get(1), new double[]{0, 0, 1, 1, 1, 0, 0});
        clientFeatMap.put(clientInfos.get(2), new double[]{0, 0, 0, 0, 1, 1, 1});

        rawTable[0] = new String[]{"uid", "f1", "f2", "f3", "label"};
        rawTable[1] = new String[]{"u1", "1.1", "2.2", "3.3", "1"};
        rawTable[2] = new String[]{"u2", "2.1", "3.2", "4.3", "2"};
        rawTable[3] = new String[]{"u3", "3.1", "4.2", "5.3", "3"};

        rawTableInfer[0] = new String[]{"uid", "f1", "f2", "f3",};
        rawTableInfer[1] = new String[]{"u1", "1.1", "2.2", "3.3"};
        rawTableInfer[2] = new String[]{"u2", "2.1", "3.2", "4.3"};
        rawTableInfer[3] = new String[]{"u3", "3.1", "4.2", "5.3"};

        List<SingleFeature> features0 = new ArrayList<>();
        features0.add(new SingleFeature("uid", "String"));
        features0.add(new SingleFeature("f1", "String"));
        features0.add(new SingleFeature("f2", "String"));
        features0.add(new SingleFeature("f3", "String"));
        features0.add(new SingleFeature("label", "String"));
        features = new Features(features0, "label");

        mockPhi[0] = new double[]{1, 1, 1, 0, 0, 0, 0, 1};
        mockPhi[1] = new double[]{0, 0, 1, 1, 1, 0, 0, 1};
        mockPhi[2] = new double[]{0, 0, 0, 0, 1, 1, 1, 1};
        mockY = new double[]{1, 2, 3};

        clientTrainData = new LinearTrainData(rawTable, features);
        clientInferData = new LinearInferenceData(rawTableInfer, null);
    }

    @Test
    public void testTrainInit() {
        LinearRegressionModel model = new LinearRegressionModel();
        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainInputStd();
        String[][] raw = compoundInput._1().get();
        String[] result = compoundInput._2().get();
        Features features = compoundInput._3().get();
        Map<String, Object> others = new HashMap<>();
        others.put("clientList", clientInfos.stream().map(x -> x.getIp() + x.getPort()).toArray(String[]::new));
        others.put("selfPort", clientInfos.get(0).getIp() + clientInfos.get(0).getPort());
        others.put("pubKeyStr" , key.getPk().toJson());
        others.put("privKeyStr", key.getSkAll()[0].toJson());
        others.put("thisPartyID", 1);

        LinearTrainData data = (LinearTrainData) model.trainInit(raw, result, new int[0], new LinearParameter(), features, others);

        Assert.assertEquals(data.getDatasetSize(), 4);
        Assert.assertEquals(data.getFeatureDim(), 3);
    }

    @Test
    public void testTrainPhase1() {
        LinearRegressionModel model = new LinearRegressionModel();
        MatchResourceLinReg ret = (MatchResourceLinReg) model.train(1, null, clientTrainData);
        Assert.assertEquals(ret.getFeatNameList(), new String[]{"f1", "f2", "f3"});
        Assert.assertEquals(ret.getIdNameList(), new String[]{"u1", "u2", "u3"});
        Assert.assertEquals(ret.getLabelList(), new double[]{1, 1, 1});
    }

    private LinearRegressionModel mockTrainInit(int skStrIdx, String selfPort) {
        LinearRegressionModel model = new LinearRegressionModel();

        Map<String, Object> others = new HashMap<>();
        others.put("pubKeyStr" , key.getPk().toJson());
        others.put("privKeyStr", key.getSkAll()[skStrIdx].toJson());
        others.put("clientList", clientInfos.stream().map(x -> x.getIp() + x.getPort()).toArray(String[]::new));
        others.put("selfPort", selfPort);
        others.put("thisPartyID", skStrIdx+1);
        model.trainInit(rawTable, null, null, new LinearParameter(), features, others);
        return model;
    }

    private LinearRegressionModel mockInferInit() {
        LinearRegressionModel model = new LinearRegressionModel();

        Map<String, Object> others = new HashMap<>();
        others.put("encBits", key.getLength());
        others.put("numP", 3);
        others.put("pubKeyStr" , key.getPk().toJson());
        others.put("privKeyStr", key.getSkAll()[0].toJson());
        others.put("clientList", clientInfos.stream().map(x -> x.getIp() + x.getPort()).toArray(String[]::new));
        others.put("selfPort", clientInfos.get(0).getIp() + clientInfos.get(0).getPort());
        others.put("thisPartyID", 1);
        model.inferenceInit(new String[]{"u1", "u2", "u3"}, rawTableInfer, others);
        return model;
    }

    private LinearRegressionModel mockInferInit(int skStrIdx, String selfPort) {
        LinearRegressionModel model = new LinearRegressionModel();

        Map<String, Object> others = new HashMap<>();
        others.put("encBits", key.getLength());
        others.put("numP", 3);
        others.put("pubKeyStr" , key.getPk().toJson());
        others.put("privKeyStr", key.getSkAll()[skStrIdx].toJson());
        others.put("clientList", clientInfos.stream().map(x -> x.getIp() + x.getPort()).toArray(String[]::new));
        others.put("selfPort", selfPort);
        others.put("thisPartyID", skStrIdx+1);
        model.inferenceInit(new String[]{"u1", "u2", "u3"}, rawTableInfer, others);
        return model;
    }

    private LinearRegressionModel mockTrainParamInit(int skStrIdx, String selfPort) {
        LinearRegressionModel model = mockTrainInit(skStrIdx, selfPort);
        model.train(2, mockTrainMatchingRes(), clientTrainData);
        return model;
    }

    private LinearRegressionModel mockInferParamInit() {
        LinearRegressionModel model = mockInferInit();
        model.deserialize(inferModel);
        model.inference(-2, mockInferMatchingRes(), clientInferData);
        model.inference(-3, mockInferMatchingRes(), clientInferData);
        return model;
    }

    private LinearRegressionModel mockInferParamInit(int skStrIdx, String selfPort) {
        LinearRegressionModel model = mockInferInit(skStrIdx, selfPort);
        model.deserialize(inferModel);
        model.inference(-2, mockInferMatchingRes(), clientInferData);
        model.inference(-3, mockInferMatchingRes(), clientInferData);
        return model;
    }

    @Test
    public void testTrainPhase2() {

        int phase = 2;
        LinearRegressionModel model = mockTrainInit(0, clientInfos.get(0).getIp() + clientInfos.get(0).getPort());

        // calling target func
        model.train(phase, mockTrainMatchingRes(), clientTrainData);
        // assertions
    }

    @Test
    public void testTrainPhase3() {
        int phase = 2;
        // model init
        LinearRegressionModel model = mockTrainInit(0, clientInfos.get(0).getIp() + clientInfos.get(0).getPort());

        // calling target func
        model.train(phase, mockTrainMatchingRes(), clientTrainData);
        Message ret = model.train(phase+1, mockTrainMatchingRes(), clientTrainData);

        // assertions
        Assert.assertEquals(
                decHelper.decDouble(((CypherMessage) ret).getBody()),
                new double[]{0.0, 0.0});
    }

    @Test
    public void testTrainPhase4() {
        int phase = 2;
        // model init
        LinearRegressionModel model = mockTrainInit(0, clientInfos.get(0).getIp() + clientInfos.get(0).getPort());
        CypherMessage masterMsg = new CypherMessage(key.encryption(new double[]{1, 1}, key.getPk()));
        model.privLoss = new WeightedLinRegLossPriv(true);
        model.nonPrivLoss = null;

        // calling target func
        model.train(phase, mockTrainMatchingRes(), clientTrainData);
        model.train(phase+1, mockTrainMatchingRes(), clientTrainData);
        Message ret = model.train(phase+2, masterMsg, clientTrainData);

        // assertions
        Assert.assertEquals(
                decHelper.decDouble(((TwoCypherMessage) ret).getFirst().getBody()),
                new double[]{0.5, 0.5, 0.5, 0.0, 0.0, 0.0, 0.0, 0.333},
                0.1);
    }

    @Test
    public void testTrainPhase5() {
        int phase = 5;
        // model init
        LinearRegressionModel model = mockTrainInit(0, clientInfos.get(0).getIp() + clientInfos.get(0).getPort());
        CypherMessage masterMsg = new CypherMessage(key.encryption(new double[]{1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0}, key.getPk()));
        model.privLoss = new WeightedLinRegLossPriv(true);
        model.nonPrivLoss = null;

        // calling target func
        model.train(phase, masterMsg, clientTrainData);

        // assertions
        // encrypted random values, could not assert. Will compare with Plaintext when decrypted.
        // As long as this test finish without exception, we consider it correct.
    }

    @Test
    public void testTrainPhase5And6() {
        int phase = 6;
        // model init
        LinearRegressionModel model0 = mockTrainParamInit(0, clientInfos.get(0).getIp() + clientInfos.get(0).getPort());
        LinearRegressionModel model1 = mockTrainParamInit(1, clientInfos.get(0).getIp() + clientInfos.get(1).getPort());
        LinearRegressionModel model2 = mockTrainParamInit(2, clientInfos.get(0).getIp() + clientInfos.get(2).getPort());
        CypherMessage masterMsg = new CypherMessage(key.encryption(new double[]{1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0}, key.getPk()));
        model0.privLoss = new WeightedLinRegLossPriv(true);
        model0.nonPrivLoss = null;
        model1.privLoss = new WeightedLinRegLossPriv(true);
        model1.nonPrivLoss = null;
        model2.privLoss = new WeightedLinRegLossPriv(true);
        model2.nonPrivLoss = null;

        // calling target func
        model0.train(5, masterMsg, clientTrainData);
        Message ret1 = model1.train(5, masterMsg, clientTrainData);
        Message ret2 = model2.train(5, masterMsg, clientTrainData);

        List<DistributedPaillierNative.signedByteArray[]> fromMaster = new ArrayList<>();
        fromMaster.add(((PartialDecMessage) ret1).getBody().get(clientInfos.get(0).getIp() + clientInfos.get(0).getPort()));
        fromMaster.add(((PartialDecMessage) ret2).getBody().get(clientInfos.get(0).getIp() + clientInfos.get(0).getPort()));

        CypherMessageList imDecResToOneParty = new CypherMessageList(fromMaster);

        model0.setMaskedGDec(model0.pheKeys.decryptPartial(
                key.encryption(new double[]{1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0}, key.getPk()),
                model0.pheKeys.getSk()));
        model0.setWeightWithMask(new double[]{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0});
        Message ret = model0.train(phase, imDecResToOneParty, clientTrainData);
        // assertions
        // encrypted random values, could not assert. Should compare with Plaintext when decrypted
        String res = "[\"[0.14285714285714285,0.2857142857142857,0.42857142857142855,0.5714285714285714,0.7142857142857142,0.8571428571428571,1.0,1.1428571428571428]\",\"[[0.14285714285714285,0.2857142857142857,0.42857142857142855,0.5714285714285714,0.7142857142857142,0.8571428571428571,1.0,1.1428571428571428],[0.14285714285714285,0.2857142857142857,0.42857142857142855,0.5714285714285714,0.7142857142857142,0.8571428571428571,1.0,1.1428571428571428],[0.14285714285714285,0.2857142857142857,0.42857142857142855,0.5714285714285714,0.7142857142857142,0.8571428571428571,1.0,1.1428571428571428],[0.14285714285714285,0.2857142857142857,0.42857142857142855,0.5714285714285714,0.7142857142857142,0.8571428571428571,1.0,1.1428571428571428],[0.14285714285714285,0.2857142857142857,0.42857142857142855,0.5714285714285714,0.7142857142857142,0.8571428571428571,1.0,1.1428571428571428],[0.14285714285714285,0.2857142857142857,0.42857142857142855,0.5714285714285714,0.7142857142857142,0.8571428571428571,1.0,1.1428571428571428],[0.14285714285714285,0.2857142857142857,0.42857142857142855,0.5714285714285714,0.7142857142857142,0.8571428571428571,1.0,1.1428571428571428],[0.14285714285714285,0.2857142857142857,0.42857142857142855,0.5714285714285714,0.7142857142857142,0.8571428571428571,1.0,1.1428571428571428]]\",\"[[0.14285714285714285,0.14285714285714285,0.14285714285714285,0.14285714285714285,0.14285714285714285,0.14285714285714285,0.14285714285714285,0.14285714285714285],[0.2857142857142857,0.2857142857142857,0.2857142857142857,0.2857142857142857,0.2857142857142857,0.2857142857142857,0.2857142857142857,0.2857142857142857],[0.42857142857142855,0.42857142857142855,0.42857142857142855,0.42857142857142855,0.42857142857142855,0.42857142857142855,0.42857142857142855,0.42857142857142855],[0.5714285714285714,0.5714285714285714,0.5714285714285714,0.5714285714285714,0.5714285714285714,0.5714285714285714,0.5714285714285714,0.5714285714285714],[0.7142857142857142,0.7142857142857142,0.7142857142857142,0.7142857142857142,0.7142857142857142,0.7142857142857142,0.7142857142857142,0.7142857142857142],[0.8571428571428571,0.8571428571428571,0.8571428571428571,0.8571428571428571,0.8571428571428571,0.8571428571428571,0.8571428571428571,0.8571428571428571],[1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0],[1.1428571428571428,1.1428571428571428,1.1428571428571428,1.1428571428571428,1.1428571428571428,1.1428571428571428,1.1428571428571428,1.1428571428571428]]\",\"[[1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0],[1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0],[1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0],[1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0],[1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0],[1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0],[1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0],[1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0]]\",\"[1,1,1,0,0,0,0,1]\",\"[[1,1,1,0,0,0,0,1],[1,1,1,0,0,0,0,1],[1,1,1,0,0,0,0,1],[0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0],[1,1,1,0,0,0,0,1]]\"]";

        Assert.assertEquals(((StringArray) ret).getData()[0], res);
    }

    @Test
    public void testTrainPhase7() {
        int phase = 7;
        // model init
        LinearRegressionModel model = mockTrainInit(0, clientInfos.get(0).getIp() + clientInfos.get(0).getPort());
        CypherMessage masterMsg = new CypherMessage(key.encryption(new double[]{1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0}, key.getPk()));
        model.privLoss = new WeightedLinRegLossPriv(true);
        model.nonPrivLoss = null;

        // calling target func
        model.train(phase, masterMsg, clientTrainData);

        // assertions
        // encrypted random values, could not being assertEqual to a specific value. Will compare with Plaintext when decrypted.
        // As long as this test finish without exception, we consider it correct.
    }

    @Test
    public void testTrainPhase7And8() {
        int phase = 8;
        // model init
        LinearRegressionModel model0 = mockTrainParamInit(0, clientInfos.get(0).getIp() + clientInfos.get(0).getPort());
        LinearRegressionModel model1 = mockTrainParamInit(1, clientInfos.get(0).getIp() + clientInfos.get(1).getPort());
        LinearRegressionModel model2 = mockTrainParamInit(2, clientInfos.get(0).getIp() + clientInfos.get(2).getPort());
        CypherMessage masterMsg = new CypherMessage(key.encryption(new double[]{1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0}, key.getPk()));
        model0.privLoss = new WeightedLinRegLossPriv(true);
        model0.nonPrivLoss = new WeightedLinRegLossNonprivClient(mockPhi,
                new double[]{1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0}, mockY,
                0.1, 0.1);

        model1.privLoss = new WeightedLinRegLossPriv(true);
        model1.nonPrivLoss = new WeightedLinRegLossNonprivClient(mockPhi,
                new double[]{1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0}, mockY,
                0.1, 0.1);
        model2.privLoss = new WeightedLinRegLossPriv(true);
        model2.nonPrivLoss = new WeightedLinRegLossNonprivClient(mockPhi,
                new double[]{1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0}, mockY,
                0.1, 0.1);

        // calling target func
        model0.train(7, masterMsg, clientTrainData);
        Message ret1 = model1.train(7, masterMsg, clientTrainData);
        Message ret2 = model2.train(7, masterMsg, clientTrainData);

        List<DistributedPaillierNative.signedByteArray[]> fromMaster = new ArrayList<>();
        fromMaster.add(((PartialDecMessage) ret1).getBody().get(clientInfos.get(0).getIp() + clientInfos.get(0).getPort()));
        fromMaster.add(((PartialDecMessage) ret2).getBody().get(clientInfos.get(0).getIp() + clientInfos.get(0).getPort()));

        CypherMessageList imDecResToOneParty = new CypherMessageList(fromMaster);

        model0.setMaskedGDec(model0.pheKeys.decryptPartial(
                key.encryption(new double[]{1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0}, key.getPk()),
                model0.pheKeys.getSk()));
        model0.setWeightWithMask(new double[]{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0});
        Message ret = model0.train(phase, imDecResToOneParty, clientTrainData);
        // assertions
        // encrypted random values, could not assert. Should compare with Plaintext when decrypted
        if (usingFakeEnc) {
            String res = "[1.0,2.0,3.0,0.0,0.0,0.0,0.0,8.0]";
            Assert.assertEquals(((StringArray) ret).getData()[0], res);
        } else {
            String res = "[0.1,0.2,0.3,0.0,0.0,0.0,0.0,0.8]";
            Assert.assertEquals(parse1dDouble(((StringArray) ret).getData()[0]), parse1dDouble(res), 0.01);
        }
    }


    @Test
    public void testInferenceInit() {
        LinearRegressionModel model = new LinearRegressionModel();

        Map<String, Object> others = new HashMap<>();
        others.put("pubKeyStr" , key.getPk().toJson());
        others.put("privKeyStr", key.getSkAll()[0].toJson());
        others.put("clientList", clientInfos.stream().map(x -> x.getIp() + x.getPort()).toArray(String[]::new));
        others.put("selfPort", clientInfos.get(0).getIp() + clientInfos.get(0).getPort());
        others.put("encBits", 1024);
        others.put("numP", 3);
        others.put("thisPartyID", 1);

        Message msg = model.inferenceInit(new String[]{"u1", "u2"}, rawTableInfer, others);
        InferenceInitRes res = (InferenceInitRes) msg;
        Assert.assertFalse(res.isAllowList());
        Assert.assertEquals(res.getUid(), new int[]{});
    }

    @Test
    public void testInferIdMatchingPhase1() {
        int phase = -2;
        LinearRegressionModel model = new LinearRegressionModel();

        model.deserialize(inferModel);

        MatchResourceLinReg ret = (MatchResourceLinReg) model.inference(phase, null, clientInferData);
        Assert.assertEquals(ret.getFeatNameList(), new String[]{"f1", "f2", "f3"});
        Assert.assertEquals(ret.getIdNameList(), new String[]{"u1", "u2", "u3"});
    }

    @Test
    public void testInferIdMatchingPhase2() {
        int phase = -3;
        LinearRegressionModel model = new LinearRegressionModel();

        model.deserialize(inferModel);
        model.inference(-2, mockInferMatchingRes(), clientInferData);

        model.inference(phase, mockInferMatchingRes(), clientInferData);
        Assert.assertEquals(model.getDataCategory(), new int[]{1, 1, 2, 0, 2, 0, 0});
    }

    @Test
    public void testInferencePhase1() {
        int phase = -4;
        LinearRegressionModel model = mockInferInit();

        // 1+2+3+8; 3+4+5+8; 5+6+7+8
        model.nonPrivLoss = new WeightedLinRegLossNonprivClient(mockPhi,
                new double[]{1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0}, mockY,
                0.1, 0.1);

        Message ret = model.inference(phase, mockInferMatchingRes(), clientInferData);
        Assert.assertEquals(
                decHelper.decDouble(((CypherMessage) ret).getBody()),
                new double[]{14, 20, 26});
    }

    @Test
    public void testInferencePhase2() {
        int phase = -5;
        LinearRegressionModel model = mockInferParamInit();

        CypherMessage masterMsg = new CypherMessage(key.encryption(new double[]{1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0}, key.getPk()));
        model.privLoss = new WeightedLinRegLossPriv(true);
        model.nonPrivLoss = null;

        // calling target func
        model.inference(phase, masterMsg, clientInferData);
    }

    @Test
    public void testInferencePhase2And3() {
        int phase = -6;
        // model init
        LinearRegressionModel model0 = mockInferParamInit(0, clientInfos.get(0).getIp() + clientInfos.get(0).getPort());
        LinearRegressionModel model1 = mockInferParamInit(1, clientInfos.get(0).getIp() + clientInfos.get(1).getPort());
        LinearRegressionModel model2 = mockInferParamInit(2, clientInfos.get(0).getIp() + clientInfos.get(2).getPort());
        CypherMessage masterMsg = new CypherMessage(key.encryption(new double[]{1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0}, key.getPk()));
        model0.privLoss = new WeightedLinRegLossPriv(mockPhi, new double[]{1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0});
        model0.nonPrivLoss = new WeightedLinRegLossNonprivClient(mockPhi,
                new double[]{1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0}, mockY,
                0.1, 0.1);

        model1.privLoss = new WeightedLinRegLossPriv(mockPhi, new double[]{1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0});
        model1.nonPrivLoss = new WeightedLinRegLossNonprivClient(mockPhi,
                new double[]{1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0}, mockY,
                0.1, 0.1);
        model2.privLoss = new WeightedLinRegLossPriv(true);
        model2.nonPrivLoss = new WeightedLinRegLossNonprivClient(mockPhi,
                new double[]{1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0}, mockY,
                0.1, 0.1);

        // calling target func
        model0.inference(-5, masterMsg, clientInferData);
        Message ret1 = model1.inference(-5, masterMsg, clientInferData);
        Message ret2 = model2.inference(-5, masterMsg, clientInferData);

        List<DistributedPaillierNative.signedByteArray[]> fromMaster = new ArrayList<>();
        fromMaster.add(((PartialDecMessage) ret1).getBody().get(clientInfos.get(0).getIp() + clientInfos.get(0).getPort()));
        fromMaster.add(((PartialDecMessage) ret2).getBody().get(clientInfos.get(0).getIp() + clientInfos.get(0).getPort()));

        CypherMessageList imDecResToOneParty = new CypherMessageList(fromMaster);

        model0.setMaskedGDec(model0.pheKeys.decryptPartial(
                key.encryption(new double[]{1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0}, key.getPk()),
                model0.pheKeys.getSk()));
        model0.setWeightWithMask(new double[]{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0});

        // call test function
        Message ret = model0.inference(phase, imDecResToOneParty, clientInferData);

        // assertions
        // encrypted random values, could not assert. Should compare with Plaintext when decrypted
        // 1+2+3+8, 3+4+5+8, 1 (input nonPrivData is 1,2,3... but only take the first one which is 1)
        Assert.assertEquals(((Double2dArray) ret).getData()[1], new double[]{14, 20, 1});
    }

    @Test
    public void testInferencePhase2And3NoNonPrivLoss() {
        int phase = -6;
        // model init
        LinearRegressionModel model0 = mockInferParamInit(0, clientInfos.get(0).getIp() + clientInfos.get(0).getPort());
        LinearRegressionModel model1 = mockInferParamInit(1, clientInfos.get(0).getIp() + clientInfos.get(1).getPort());
        LinearRegressionModel model2 = mockInferParamInit(2, clientInfos.get(0).getIp() + clientInfos.get(2).getPort());
        CypherMessage masterMsg = new CypherMessage(key.encryption(new double[]{1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0}, key.getPk()));
        model0.privLoss = new WeightedLinRegLossPriv(mockPhi, new double[]{1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0});
        model0.nonPrivLoss = null;
        model0.setDataCategory(Arrays.stream(model0.getDataCategory()).map(x -> x == 2 ? 0 : x).toArray());

        model1.privLoss = new WeightedLinRegLossPriv(mockPhi, new double[]{1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0});
        model1.nonPrivLoss = null;
        model2.privLoss = new WeightedLinRegLossPriv(true);
        model2.nonPrivLoss = null;

        // calling target func
        model0.inference(-5, masterMsg, clientInferData);
        Message ret1 = model1.inference(-5, masterMsg, clientInferData);
        Message ret2 = model2.inference(-5, masterMsg, clientInferData);

        List<DistributedPaillierNative.signedByteArray[]> fromMaster = new ArrayList<>();
        fromMaster.add(((PartialDecMessage) ret1).getBody().get(clientInfos.get(0).getIp() + clientInfos.get(0).getPort()));
        fromMaster.add(((PartialDecMessage) ret2).getBody().get(clientInfos.get(0).getIp() + clientInfos.get(0).getPort()));

        CypherMessageList imDecResToOneParty = new CypherMessageList(fromMaster);

        model0.setMaskedGDec(model0.pheKeys.decryptPartial(
                key.encryption(new double[]{1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0}, key.getPk()),
                model0.pheKeys.getSk()));
        model0.setWeightWithMask(new double[]{1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0});

        // call test function
        Message ret = model0.inference(phase, imDecResToOneParty, clientInferData);

        // assertions
        // encrypted random values, could not assert. Should compare with Plaintext when decrypted
        // 1+2+3+8, 3+4+5+8, Double.NaN (this is Double.NaN because dataCategory is set to [1,1,0,0,0,0,0,0])
        Assert.assertEquals(((Double2dArray) ret).getData()[1], new double[]{14, 20, Double.NaN});
    }

    @Test
    public void testDeserialize() {
        String content = "modelToken=call-911\n" +
                "weight=[25.37,31.10,58.09,23.40,62.35,25.48,35.10,84.21]\n" +
                "M=8\n" +
                "numP=3\n" +
                "M_priv=4\n" +
                "weight_priv=[25.37,31.10,58.09,84.21]\n";

        LinearRegressionModel model = new LinearRegressionModel();

        model.deserialize(content);
        Assert.assertEquals(model.weight, new double[]{25.37, 31.10, 58.09, 23.40, 62.35, 25.48, 35.10, 84.21});
        Assert.assertEquals(model.numP, 3);
        Assert.assertEquals(model.weightPriv, new double[]{25.37, 31.10, 58.09, 84.21});
    }

    @Test
    public void serialize() {
        String content = "modelToken=call-911\n" +
                "weight=[25.37,31.1,58.09,23.4,62.35,25.48,35.1,84.21]\n" +
                "M=8\n" +
                "numP=3\n" +
                "M_priv=4\n" +
                "weight_priv=[25.37,31.10,58.09,84.21]\n";

        String afterContent = "modelToken=call-911\n" +
                "weight=[25.37,31.1,58.09,23.4,62.35,25.48,35.1,84.21]\n" +
                "M=0\n" +
                "numP=3\n" +
                "M_priv=0\n" +
                "weight_priv=[25.37,31.1,58.09,84.21]\n";

        LinearRegressionModel model = new LinearRegressionModel();

        model.deserialize(content);
        String after = model.serialize();
        Assert.assertEquals(after, afterContent);
    }

    @Test
    public void testGetModelType() {
        LinearRegressionModel model = new LinearRegressionModel();

        Assert.assertEquals(model.getModelType(), AlgorithmType.LinearRegression);
    }


    private List<CommonResponse> packageThreeMsgsIntoCommonResponses(List<Message> in) {
        List<CommonResponse> out = new ArrayList<>();
        out.add(new CommonResponse(clientInfos.get(0), in.get(0)));
        out.add(new CommonResponse(clientInfos.get(1), in.get(1)));
        out.add(new CommonResponse(clientInfos.get(2), in.get(2)));
        return out;
    }
}