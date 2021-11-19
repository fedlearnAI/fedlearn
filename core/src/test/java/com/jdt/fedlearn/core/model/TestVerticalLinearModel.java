package com.jdt.fedlearn.core.model;


import com.jdt.fedlearn.core.encryption.common.PrivateKey;
import com.jdt.fedlearn.core.encryption.fake.FakeTool;
import com.jdt.fedlearn.core.encryption.paillier.PaillierTool;
import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.core.entity.base.DoubleArray;
import com.jdt.fedlearn.core.entity.base.StringArray;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.core.entity.common.InferenceInitRes;
import com.jdt.fedlearn.common.entity.core.feature.Features;
import com.jdt.fedlearn.core.entity.verticalLinearRegression.*;
import com.jdt.fedlearn.core.exception.NotMatchException;
import com.jdt.fedlearn.core.fake.StructureGenerate;
import com.jdt.fedlearn.core.loader.common.CommonInferenceData;
import com.jdt.fedlearn.core.loader.verticalLinearRegression.VerticalLinearTrainData;
import com.jdt.fedlearn.core.parameter.VerticalLinearParameter;
import com.jdt.fedlearn.core.preprocess.Scaling;
import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.data.Tuple3;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;
import java.util.stream.IntStream;


public class TestVerticalLinearModel {
    private static final String modelId = "123" + "_" + AlgorithmType.VerticalLinearRegression;

    private String[][] raw;  // raw data: each row is a data point
    private String[] result; // id match info
    private Features features;  // feature and label column names
    private int[] testIndex;

    @BeforeMethod
    public void setup() {
        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainClassInputStd();
        raw = compoundInput._1().get(); // raw data: each row is a data point
        result = compoundInput._2().get(); // id match info
        features = compoundInput._3().get(); // feature and label column names
        testIndex = new int[0];
    }


    @Test
    public void trainInit() {
        VerticalLinearModel model = new VerticalLinearModel();
        VerticalLinearTrainData trainData = model.trainInit(raw, result, testIndex, new VerticalLinearParameter(), features, new HashMap<>());
        double[] label = new double[]{1.0, 0.0, 1.0};
        Scaling scaling = trainData.getScaling();
        double[] scales = new double[]{0.14285714285714285, 0.01020408163265306}; // 1/(29-12),
        double[] X_min = new double[]{1.0, 85.0};
        double[] X_max = new double[]{8.0, 183.0};
        Assert.assertEquals(trainData.getDatasetSize(), 3);
        Assert.assertEquals(trainData.getFeatureDim(), 2);
        Assert.assertEquals(trainData.getUid(), new String[]{"1B", "2A", "3A"});
        Assert.assertEquals(trainData.getLabel(), label);
        Assert.assertEquals(scaling.getScales(), scales);
        Assert.assertEquals(scaling.getX_min(), X_min);
        Assert.assertEquals(scaling.getX_max(), X_max);
        double[] noises = model.getDifferentialPrivacy().getNoises();
        Assert.assertEquals(noises.length, 3);
    }

    @Test
    public void trainElseBranch() {
        // has label
        double[] weight = new double[]{0.0, 0.0, 0.0};
        FakeTool ft = new FakeTool();
        VerticalLinearModel model = new VerticalLinearModel(false, new VerticalLinearParameter(), ft, weight, new Scaling());
        VerticalLinearTrainData trainData = model.trainInit(raw, result, testIndex, new VerticalLinearParameter(), features, new HashMap<>());
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        PrivateKey privateKey = ft.keyGenerate(256, 64);
        String publicKey = privateKey.generatePublicKey().serialize();
        LinearP1Request req = new LinearP1Request(clientInfos.get(0), true, publicKey);
        try {
            LinearP1Response lp1r = (LinearP1Response) model.train(-2, req, trainData);
        } catch (NotMatchException e) {
            Assert.assertEquals(e.getMessage(), null);
        }

    }

    @Test
    public void trainPhase1_label() {
        // has label
        double[] weight = new double[]{0.0, 0.0, 0.0};
        FakeTool ft = new FakeTool();
        VerticalLinearModel model = new VerticalLinearModel(false, new VerticalLinearParameter(), ft, weight, new Scaling());
        VerticalLinearTrainData trainData = model.trainInit(raw, result, testIndex, new VerticalLinearParameter(), features, new HashMap<>());
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        PrivateKey privateKey = ft.keyGenerate(256, 64);
        String publicKey = privateKey.generatePublicKey().serialize();
        // message
        LinearP1Request req = new LinearP1Request(clientInfos.get(0), true, publicKey);
        //ClientInfo client, String[][] u, String loss
        // LinearP1Response(clientInfo, new String[0][], "")
        LinearP1Response lp1r = (LinearP1Response) model.train(1, req, trainData);
        Assert.assertEquals(lp1r.getClient(), clientInfos.get(0));
        Assert.assertEquals(lp1r.getLoss(), "");
        Assert.assertEquals(lp1r.getU(), new String[0][]);

    }

    @Test
    public void trainPhase1_noLabel() {
        // no label
        // 这里construct weight其实没有价值，会自动在trainInit的时候自动赋值weight
        double[] weight1 = new double[]{0.0, 0.0, 0.0, 0.0};
        FakeTool ft = new FakeTool();
        VerticalLinearModel model = new VerticalLinearModel(true, new VerticalLinearParameter(), ft, weight1, new Scaling());
        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainInputStdNoLabel();
        String[][] raw = compoundInput._1().get();
        String[] result = compoundInput._2().get();
        Features features = compoundInput._3().get();
        VerticalLinearTrainData trainData = model.trainInit(raw, result, new int[0], new VerticalLinearParameter(), features, new HashMap<>());
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        PrivateKey privateKey = ft.keyGenerate(256, 64);
        String publicKey = privateKey.generatePublicKey().serialize();
        // message
        LinearP1Request req = new LinearP1Request(clientInfos.get(0), true, publicKey);

        LinearP1Response lp1r = (LinearP1Response) model.train(1, req, trainData);
        Assert.assertEquals(lp1r.getClient(), clientInfos.get(0));
        Assert.assertEquals(lp1r.getLoss(), "0.0");
        Assert.assertEquals(lp1r.getU().length, 3);
        Assert.assertEquals(lp1r.getU()[0].length, 2);
        Assert.assertEquals(lp1r.getU()[0], new String[]{"0", "0.0"});
        Assert.assertEquals(lp1r.getU()[1], new String[]{"1", "0.0"});

    }

    @Test
    public void trainPhase2_noLabel() {
        // no label
        double[] weight1 = new double[]{0.0, 0.0, 0.0, 0.0};
        FakeTool ft = new FakeTool();
        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainInputStdNoLabel();
        String[][] raw = compoundInput._1().get();
        String[] result = compoundInput._2().get();
        Features features = compoundInput._3().get();
        VerticalLinearTrainData trainData = new VerticalLinearTrainData(raw, result, features, false);
        VerticalLinearModel model = new VerticalLinearModel(false, new VerticalLinearParameter(), ft, weight1, trainData.getScaling());
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        PrivateKey privateKey = ft.keyGenerate(256, 64);
        String publicKey = privateKey.generatePublicKey().serialize();
        // message
        List<LinearP1Response> responses = new ArrayList<LinearP1Response>();
        responses.add(new LinearP1Response(clientInfos.get(0), new String[][]{{}}, ""));
        responses.add(new LinearP1Response(clientInfos.get(1), new String[][]{{}}, ""));
        responses.add(new LinearP1Response(clientInfos.get(2), new String[][]{{}}, ""));
        LinearP2Request req = new LinearP2Request(clientInfos.get(0), responses);
        LossGradients lg = (LossGradients) model.train(2, req, trainData);
        Assert.assertEquals(lg.getGradient(), new String[0]);
        Assert.assertEquals(lg.getLoss(), new String[0]);
    }

    @Test
    public void trainPhase2_Label() {
        // has label
        double[] weight1 = new double[]{0.0, 0.0, 0.0, 0.0};
        FakeTool ft = new FakeTool();
        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainInputStd();
        String[][] raw = compoundInput._1().get();
        String[] result = compoundInput._2().get();
        Features features = compoundInput._3().get();
        VerticalLinearTrainData trainData = new VerticalLinearTrainData(raw, result, features, false);
        VerticalLinearModel model = new VerticalLinearModel(false, new VerticalLinearParameter(), ft, weight1, trainData.getScaling());
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        PrivateKey privateKey = ft.keyGenerate(256, 64);
        String publicKey = privateKey.generatePublicKey().serialize();
        // message
        List<LinearP1Response> responses = new ArrayList<LinearP1Response>();
        responses.add(new LinearP1Response(clientInfos.get(0), new String[][]{{"0", "0.0"}, {"1", "0.0"}, {"2", "0.0"}}, "0.0"));
        responses.add(new LinearP1Response(clientInfos.get(1), new String[][]{{"0", "0.0"}, {"1", "0.0"}, {"2", "0.0"}}, "0.0"));
        responses.add(new LinearP1Response(clientInfos.get(2), new String[][]{{"0", "0.0"}, {"1", "0.0"}, {"2", "0.0"}}, "0.0"));
        LinearP2Request req = new LinearP2Request(clientInfos.get(0), responses);
        LossGradients lg = (LossGradients) model.train(2, req, trainData);
        String[] s = {"-3.585", "-2.578", "-1.952"};
        for (int i = 0; i < s.length; i++) {
            Assert.assertEquals(s[i], lg.getGradient()[i]);
            Assert.assertEquals(s[i], lg.getLoss()[i]);

        }
    }

    @Test
    public void trainPhase3() {
        double[] weight1 = new double[]{0.0, 0.0, 0.0, 0.0};
        PaillierTool ft = new PaillierTool();
        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainInputStd();
        String[][] raw = compoundInput._1().get();
        String[] result = compoundInput._2().get();
        Features features = compoundInput._3().get();
        VerticalLinearTrainData trainData = new VerticalLinearTrainData(raw, result, features, false);
        PrivateKey privateKey = ft.keyGenerate(256, 64);
        VerticalLinearModel model = new VerticalLinearModel(false, new VerticalLinearParameter(), ft, weight1, trainData.getScaling(), privateKey.generatePublicKey());
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        String publicKey = privateKey.generatePublicKey().serialize();
        String[] beforeEncLoss = new String[]{"-3.585", "-2.578", "-1.952"};
        String[] beforeEncGrad = new String[]{"-3.585", "-2.578", "-1.952"};
        String[] afterEncLoss = IntStream.range(0, beforeEncLoss.length).mapToObj(i -> ft.encrypt(Double.valueOf(beforeEncLoss[i]), privateKey.generatePublicKey()).serialize()).toArray(String[]::new);
        String[] afterEncGrad = IntStream.range(0, beforeEncLoss.length).mapToObj(i -> ft.encrypt(Double.valueOf(beforeEncGrad[i]), privateKey.generatePublicKey()).serialize()).toArray(String[]::new);
        // message
        LossGradients request3 = new LossGradients(clientInfos.get(0), afterEncLoss, afterEncGrad);
        CommonRequest request = new CommonRequest(clientInfos.get(0), request3, 3);
        LossGradients lg = (LossGradients) model.train(3, request.getBody(), trainData);
        Assert.assertEquals(lg.getClient(), clientInfos.get(0));
        Assert.assertEquals(lg.getLoss().length, 3);
        Assert.assertEquals(lg.getGradient().length, 4);
        Double[] decrypted_L = IntStream.range(0, lg.getLoss().length).mapToObj(i -> ft.decrypt(lg.getLoss()[i], privateKey)).toArray(Double[]::new);
        Double[] decrypted_G = IntStream.range(0, lg.getGradient().length).mapToObj(i -> ft.decrypt(lg.getGradient()[i], privateKey)).toArray(Double[]::new);
        double[] target_L = new double[]{-3.585, -2.578, -1.952};
        double[] target_G = new double[]{-0.761977, 0.069951, -0.617763, -1.804997};
        for (int i = 0; i < target_L.length; i++) {
            Assert.assertEquals(target_L[i], decrypted_L[i], 1e-6);
        }

        for (int i = 0; i < decrypted_G.length; i++) {
            Assert.assertEquals(target_G[i], decrypted_G[i], 1e-6);
        }

    }

    @Test
    public void trainPhase4() {
        double[] weight1 = new double[]{0.0, 0.0, 0.0, 0.0};
        PaillierTool ft = new PaillierTool();
        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainInputStd();
        String[][] raw = compoundInput._1().get();
        String[] result = compoundInput._2().get();
        Features features = compoundInput._3().get();
        VerticalLinearTrainData trainData = new VerticalLinearTrainData(raw, result, features, false);
        PrivateKey privateKey = ft.keyGenerate(256, 64);
        double[] random = new double[]{0.5, 0.1, 0.2, 0.1};
        VerticalLinearModel model = new VerticalLinearModel(false, new VerticalLinearParameter(), ft, weight1,
                trainData.getScaling(), privateKey.generatePublicKey(), random);
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        double[] gradients = {0.78, 0.098, -0.03, 0.05};
        Map<MetricType, Double> metric = new HashMap<MetricType, Double>();
        metric.put(MetricType.RMSE, 2.787389);
        //
        GradientsMetric gm = new GradientsMetric(clientInfos.get(0), gradients, metric);
        GradientsMetric res = (GradientsMetric) model.train(4, gm, trainData);
        Assert.assertEquals(res.getMetric(), metric);

    }

    @Test
    public void inferenceInit() {
        VerticalLinearModel model = new VerticalLinearModel();
        String[] uidList = new String[]{"aa", "1a", "c3"};
        String[][] data = new String[2][];
        data[0] = new String[]{"aa", "10", "12.1"};
        data[1] = new String[]{"1a", "10", "12.1"};
        Message msg = model.inferenceInit(uidList, data, new HashMap<>());
        InferenceInitRes res = (InferenceInitRes) msg;
        Assert.assertFalse(res.isAllowList());
        Assert.assertEquals(res.getUid(), new int[]{2});
    }


    @Test
    public void inference1() {
        double[] weight = new double[]{0.1, 0.5, 1};
        Scaling scaling = new Scaling();
        double[] scales = new double[]{0.058823529411764705, 0.8196721311475418};
        double[] xMin = new double[]{8, 1.1};
        scaling.setScales(scales);
        scaling.setX_min(xMin);
        VerticalLinearModel model = new VerticalLinearModel(modelId, weight, scaling);
        String[][] data = new String[3][3];
        data[0] = new String[]{"uid", "age", "height"};
        data[1] = new String[]{"aa", "10", "1.2"};
        data[2] = new String[]{"1a", "8", "1.1"};
        StringArray stringArray = new StringArray(new String[]{"aa", "1a"});
        CommonInferenceData inferenceData = new CommonInferenceData(data, "uid", new String[0]);

        DoubleArray res = (DoubleArray) model.inference(-1, stringArray, inferenceData);
        double[] predict = new double[]{1.0527483124397299, 1.0};
        System.out.println("predict : " + Arrays.toString(res.getData()));
        Assert.assertEquals(res.getData(), predict);
    }

    @Test
    public void inference_elseBranch() {
        double[] weight = new double[]{0.1, 0.5, 1};
        Scaling scaling = new Scaling();
        double[] scales = new double[]{0.058823529411764705, 0.8196721311475418};
        double[] xMin = new double[]{8, 1.1};
        scaling.setScales(scales);
        scaling.setX_min(xMin);
        VerticalLinearModel model = new VerticalLinearModel(modelId, weight, scaling);
        String[][] data = new String[3][3];
        data[0] = new String[]{"uid", "age", "height"};
        data[1] = new String[]{"aa", "10", "1.2"};
        data[2] = new String[]{"1a", "8", "1.1"};

        CommonInferenceData inferenceData = new CommonInferenceData(data, "uid", new String[0]);
        StringArray stringArray = new StringArray(new String[]{"aa", "1a"});
        try {
            Message msg = model.inference(100, stringArray, inferenceData);
        } catch (NotMatchException e) {
            Assert.assertEquals(e.getMessage(), null);
        }
    }

    @Test
    public void inference() {
        double[] weight = new double[]{0.1, 0.5, 1};
        Scaling scaling = new Scaling();
        double[] scales = new double[]{0.058823529411764705, 0.8196721311475418};
        double[] xMin = new double[]{8, 1.1};
        scaling.setScales(scales);
        scaling.setX_min(xMin);
        VerticalLinearModel model = new VerticalLinearModel(modelId, weight, scaling);
        String[][] data = new String[3][3];
        data[0] = new String[]{"uid", "age", "height"};
        data[1] = new String[]{"aa", "10", "1.2"};
        data[2] = new String[]{"1a", "8", "1.1"};
        CommonInferenceData inferenceData = new CommonInferenceData(data, "uid", new String[0]);
        StringArray stringArray = new StringArray(new String[]{"aa", "1a"});
        Message msg = model.inference(-1, stringArray, inferenceData);
        DoubleArray linearN1Response = (DoubleArray) msg;
        double[] predict = new double[]{1.0527483124397299, 1.0};
        System.out.println("predict : " + Arrays.toString(linearN1Response.getData()));
        Assert.assertEquals(linearN1Response.getData(), predict);
    }

    @Test
    public void serializeAndDeserialize() {
        double[] weight = new double[]{0.1, 0.5, 1};
        Scaling scaling = new Scaling();
        double[] scales = new double[]{0.058823529411764705, 0.8196721311475418};
        double[] xMin = new double[]{8, 1.1};
        scaling.setScales(scales);
        scaling.setX_min(xMin);
        VerticalLinearModel model = new VerticalLinearModel(modelId, weight, scaling);
        String s = model.serialize();
        Assert.assertEquals(s.split("\n")[1], "weight=0.1,0.5,1.0");
        Assert.assertEquals(s.split("\n")[2], "scaling=0.058823529411764705,0.8196721311475418");
        Assert.assertEquals(s.split("\n")[3], "xMin=8.0,1.1");
        model.deserialize(s);
    }

    @Test
    public void getModelType() {
        double[] weight = new double[]{0.1, 0.5, 1};
        Scaling scaling = new Scaling();
        double[] scales = new double[]{0.058823529411764705, 0.8196721311475418};
        double[] xMin = new double[]{8, 1.1};
        scaling.setScales(scales);
        scaling.setX_min(xMin);
        VerticalLinearModel model = new VerticalLinearModel(modelId, weight, scaling);
        Assert.assertEquals(model.getModelType(), AlgorithmType.VerticalLinearRegression);

    }

}