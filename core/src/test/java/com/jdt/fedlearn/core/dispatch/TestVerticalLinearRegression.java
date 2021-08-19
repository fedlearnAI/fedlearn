package com.jdt.fedlearn.core.dispatch;

import com.jdt.fedlearn.core.encryption.common.PrivateKey;
import com.jdt.fedlearn.core.encryption.common.PublicKey;
import com.jdt.fedlearn.core.encryption.paillier.PaillierTool;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.base.DoubleArray;
import com.jdt.fedlearn.core.entity.base.SingleElement;
import com.jdt.fedlearn.core.entity.base.StringArray;
import com.jdt.fedlearn.core.entity.common.*;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.entity.verticalLinearRegression.*;
import com.jdt.fedlearn.core.fake.StructureGenerate;
import com.jdt.fedlearn.core.parameter.VerticalLinearParameter;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.OptimizerType;
import com.jdt.fedlearn.core.type.data.Pair;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;
import java.util.stream.IntStream;

public class TestVerticalLinearRegression {

    @Test
    public void getNextPhase(){
        VerticalLinearParameter vlrp = new VerticalLinearParameter(0.0, 0.0, new MetricType[]{MetricType.RMSE, MetricType.MAPE}, OptimizerType.NEWTON, 10, 10, "l2", 0.0001, 0.0);
        VerticalLinearParameter vlrp2 = new VerticalLinearParameter(0.0, 0.0, new MetricType[]{MetricType.RMSE, MetricType.MAPE}, OptimizerType.BFGS, 10, 10, "l2", 0.0001, 0.0);

        VerticalLinearRegression verticalLinearRegression = new VerticalLinearRegression(vlrp);
        VerticalLinearRegression verticalLinearRegression2 = new VerticalLinearRegression(vlrp2);

        int p1 = verticalLinearRegression.getNextPhase(1, new ArrayList<>());
        Assert.assertEquals(p1, 2);
        int p2 = verticalLinearRegression.getNextPhase(2, new ArrayList<>());
        Assert.assertEquals(p2, 3);
        int p3 = verticalLinearRegression.getNextPhase(3, new ArrayList<>());
        Assert.assertEquals(p3, 4);
        int p4 = verticalLinearRegression.getNextPhase(4, new ArrayList<>());
        Assert.assertEquals(p4, 1);
        int p5 = verticalLinearRegression.getNextPhase(-255, new ArrayList<>());
        Assert.assertEquals(p5, -1);
        int p6 = verticalLinearRegression.getNextPhase(-1, new ArrayList<>());
        Assert.assertEquals(p6, -2);
        int p7 = verticalLinearRegression.getNextPhase(-2, new ArrayList<>());
        Assert.assertEquals(p7, 1);
    }

    @Test
    public void initControl(){
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        MatchResult matchResult = new MatchResult(10);

        Map<ClientInfo, Features> features = StructureGenerate.fgbFeatures(clientInfos); // featureMap

        VerticalLinearRegression verticalLinearRegression = new VerticalLinearRegression(new VerticalLinearParameter());

        List<CommonRequest> requests = verticalLinearRegression.initControl(clientInfos, matchResult, features, new HashMap<>());
        Assert.assertEquals(clientInfos.size(), requests.size());
        CommonRequest first = requests.get(0);
        Assert.assertEquals(first.getPhase(),0);
        Assert.assertFalse(first.isSync());
        Message message = first.getBody();
        TrainInit body = (TrainInit) message;
        Assert.assertEquals(body.getFeatureList(), features.get(clientInfos.get(0)));

    }

    @Test
    public void initControl2(){
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        MatchResult matchResult = new MatchResult(10);

        Map<ClientInfo, Features> features = StructureGenerate.fgbFeatures(clientInfos); // featureMap
        //double minLoss, double eta, MetricType[] loss, OptimizerType optimizer, int batchSize, int maxEpoch, String regularization, double lambda, double differentialPrivacyParameter
        VerticalLinearRegression verticalLinearRegression = new VerticalLinearRegression(new VerticalLinearParameter(0.0, 0.0, new MetricType[]{}, OptimizerType.StochasticGD, 10, 10, "l1", 0.001, 0.0));
        List<CommonRequest> requests = verticalLinearRegression.initControl(clientInfos, matchResult, features, new HashMap<>());
        Assert.assertEquals(clientInfos.size(), requests.size());
        CommonRequest first = requests.get(0);
        Assert.assertEquals(first.getPhase(),0);
        Assert.assertFalse(first.isSync());
        Message message = first.getBody();
        TrainInit body = (TrainInit) message;
        Assert.assertEquals(body.getFeatureList(), features.get(clientInfos.get(0)));

    }
    @Test
    public void controlElseBranch() {
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        VerticalLinearRegression verticalLinearRegression = new VerticalLinearRegression(new VerticalLinearParameter());
        List<CommonResponse> responses = new ArrayList<>();
        responses.add(new CommonResponse(clientInfos.get(0), new SingleElement("init_success")));
        responses.add(new CommonResponse(clientInfos.get(1), new SingleElement("init_success")));
        responses.add(new CommonResponse(clientInfos.get(2), new SingleElement("init_success")));
        verticalLinearRegression.setPhase(-1);
        try {
            List<CommonRequest> requests = verticalLinearRegression.control(responses);
        } catch (UnsupportedOperationException e) {
            Assert.assertEquals(e.getMessage(), null);
        }

    }

    @Test
    public void control1FromInit(){
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        VerticalLinearRegression verticalLinearRegression = new VerticalLinearRegression(new VerticalLinearParameter());
        List<CommonResponse> responses = new ArrayList<>();
        responses.add(new CommonResponse(clientInfos.get(0), new SingleElement("init_success")));
        responses.add(new CommonResponse(clientInfos.get(1), new SingleElement("init_success")));
        responses.add(new CommonResponse(clientInfos.get(2), new SingleElement("init_success")));
        verticalLinearRegression.setPhase(0);
        List<CommonRequest> requests = verticalLinearRegression.control(responses);
        Assert.assertEquals(requests.size(), 3);
        //CommonRequest(response.getClient(), trainId, req, phase)
        Assert.assertEquals(requests.get(0).getClient(), clientInfos.get(0));
        LinearP1Request lp1r = (LinearP1Request) requests.get(0).getBody();
        Assert.assertEquals(lp1r.isNewIter(), true);

    }

    @Test
    public void control1FromLastPhase(){
        Map<MetricType, List<Pair<Integer, Double>>> metricMap = new HashMap<>();
        List<Pair<Integer, Double>> l1 = new ArrayList<>();
//        List<Pair<Integer, Double>> l2 = new ArrayList<>();

        l1.add(new Pair<>(0, 3.66666));
//        l2.add(new Pair<>(0, 2.66666));

        metricMap.put(MetricType.RMSE, l1);
//        metricMap.put(MetricType.MAPE, l2);
        VerticalLinearRegression verticalLinearRegression = new VerticalLinearRegression(1, new VerticalLinearParameter(0.0, 0.0, new MetricType[]{MetricType.RMSE, MetricType.MAPE}, OptimizerType.BatchGD, 10, 10, "l2", 0.0001, 0.0), metricMap);
        List<CommonResponse> responses = new ArrayList<>();
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        Map<MetricType, Double> metric = new HashMap<MetricType, Double>();
        metric.put(MetricType.RMSE, 2.787389); // existing metric
        metric.put(MetricType.MAPE, 1.333333); // "new" metric

        GradientsMetric gm = new GradientsMetric(null, null, metric);
        responses.add(new CommonResponse(clientInfos.get(0), gm));
        verticalLinearRegression.setPhase(4);
        List<CommonRequest> requests = verticalLinearRegression.control(responses);
        Assert.assertEquals(requests.size(), 1);
        LinearP1Request lp1r = (LinearP1Request) requests.get(0).getBody();
        Assert.assertEquals(lp1r.isNewIter(), false);
        Assert.assertEquals(requests.get(0).getClient(), clientInfos.get(0));
        Assert.assertEquals(requests.get(0).getPhase(), 1);


    }

    @Test
    public void control2(){
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        VerticalLinearRegression verticalLinearRegression = new VerticalLinearRegression(new VerticalLinearParameter());
        List<CommonResponse> responses = new ArrayList<>();
        responses.add(new CommonResponse(clientInfos.get(0), new LinearP1Response(clientInfos.get(0), new String[][]{{}}, "")));
        responses.add(new CommonResponse(clientInfos.get(1), new LinearP1Response(clientInfos.get(1), new String[][]{{}}, "")));
        responses.add(new CommonResponse(clientInfos.get(2), new LinearP1Response(clientInfos.get(2), new String[][]{{}}, "")));
        verticalLinearRegression.setPhase(1);
        List<CommonRequest> requests = verticalLinearRegression.control(responses);
        Assert.assertEquals(requests.get(0).getClient(), clientInfos.get(0));
        Assert.assertEquals(requests.get(1).getClient(), clientInfos.get(1));
        Assert.assertEquals(requests.get(2).getClient(), clientInfos.get(2));
        Assert.assertEquals(requests.get(0).getPhase(), 2);
        LinearP2Request lp2r0 = (LinearP2Request) requests.get(0).getBody();
        LinearP2Request lp2r1 = (LinearP2Request) requests.get(1).getBody();
        Assert.assertEquals(lp2r0.getBodies().size(), 3);
        List<LinearP1Response> list = lp2r0.getBodies();
        Assert.assertEquals(list.get(0), responses.get(0).getBody());
        Assert.assertEquals(list.get(1), responses.get(1).getBody());
        Assert.assertEquals(list.get(2), responses.get(2).getBody());
    }

    @Test
    public void control3(){
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        VerticalLinearRegression verticalLinearRegression = new VerticalLinearRegression(new VerticalLinearParameter());
        List<CommonResponse> responses = new ArrayList<>();
        responses.add(new CommonResponse(clientInfos.get(0), new LossGradients(clientInfos.get(0), new String[]{}, new String[]{})));
        responses.add(new CommonResponse(clientInfos.get(1), new LossGradients(new ClientInfo(), new String[0], new String[0])));
        responses.add(new CommonResponse(clientInfos.get(2), new LossGradients(new ClientInfo(), new String[0], new String[0])));
        verticalLinearRegression.setPhase(2);
        List<CommonRequest> lcr = verticalLinearRegression.control(responses);
        Assert.assertEquals(lcr.size(), 3);
        Assert.assertEquals(lcr.get(0).getPhase(), 3);
        LossGradients lg = (LossGradients) lcr.get(0).getBody();
        Assert.assertEquals(lg.getClient(), clientInfos.get(0));
        Assert.assertEquals(lg.getLoss(), new String[]{});
        Assert.assertEquals(lg.getGradient(), new String[]{});
    }

    @Test
    public void control4(){
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        List<CommonResponse> responses = new ArrayList<>();
        double[] target_L = new double[] {-3.585, -2.578, -1.952};
        double[] target_G = new double[] {-0.761977, 0.069951, -0.617763, -1.804997};
        PaillierTool encryptionTool = new PaillierTool();
        PrivateKey privateKey = encryptionTool.keyGenerate(256, 64);
        PublicKey publicKey = privateKey.generatePublicKey();
        VerticalLinearRegression verticalLinearRegression = new VerticalLinearRegression(encryptionTool, privateKey, new VerticalLinearParameter());

        String[] enc_L = IntStream.range(0, target_L.length).mapToObj(i -> encryptionTool.encrypt(target_L[i], publicKey).serialize()).toArray(String[]::new);
        String[] enc_G = IntStream.range(0, target_G.length).mapToObj(i -> encryptionTool.encrypt(target_G[i], publicKey).serialize()).toArray(String[]::new);
        LossGradients lg1 = new LossGradients(clientInfos.get(0), enc_L, enc_G);
        LossGradients lg2 = new LossGradients(clientInfos.get(1), enc_L, enc_G);
        responses.add(new CommonResponse(clientInfos.get(0), lg1));
//        responses.add(new CommonResponse(clientInfos.get(1), lg2));
        verticalLinearRegression.setPhase(3);
        List<CommonRequest> lcr = verticalLinearRegression.control(responses);
        Assert.assertEquals(lcr.get(0).getClient(),clientInfos.get(0));
//        Assert.assertEquals(lcr.get(1).getClient(),clientInfos.get(1));
        GradientsMetric req1 = (GradientsMetric) lcr.get(0).getBody();
        Map<MetricType, Double> mp = new HashMap<MetricType, Double>();
        mp.put(MetricType.RMSE, 2.787389);
        Map<MetricType, Double> res = req1.getMetric();
        Assert.assertEquals(res.get(MetricType.RMSE), mp.get(MetricType.RMSE), 1e-6); // TODO try more metrics
        for (int i = 0; i < target_G.length; i++) {
            Assert.assertEquals(req1.getGradients()[i], -target_G[i]*0.1, 1e-6); // learning_rate = eta = 0.01, method = BatchGD
        }

    }

    @Test
    public void testIsContinue() {
        VerticalLinearRegression verticalLinearRegression = new VerticalLinearRegression(new VerticalLinearParameter());
        boolean isContinue = verticalLinearRegression.isContinue();
        Assert.assertTrue(isContinue);
    }

    @Test
    public void testIsInferenceContinue() {
        VerticalLinearRegression verticalLinearRegression = new VerticalLinearRegression(new VerticalLinearParameter());
        boolean isInferenceContinue = verticalLinearRegression.isInferenceContinue();
        Assert.assertTrue(isInferenceContinue);
    }
    @Test
    public void initInference(){
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        VerticalLinearRegression verticalLinearRegression = new VerticalLinearRegression(new VerticalLinearParameter());
        String[] predUid = new String[]{"0A", "1B", "2C"};
        List<CommonRequest> requests = verticalLinearRegression.initInference(clientInfos, predUid,new HashMap<>());
        Assert.assertEquals(clientInfos.size(), requests.size());
        CommonRequest commonRequest = requests.get(0);
        Message message = commonRequest.getBody();
        String[] predOriginId = ((InferenceInit) message).getUid();
        Assert.assertEquals(Arrays.toString(predOriginId), Arrays.toString(predUid));
    }

    @Test
    public void inferenceControl(){
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        String[] uidList = new String[]{"aa", "1a", "c3"};
        String[][] data = new String[2][];
        data[0] = new String[]{"aa", "10", "12.1"};
        data[1] = new String[]{"1a", "10", "12.1"};
        VerticalLinearRegression verticalLinearRegression = new VerticalLinearRegression(new VerticalLinearParameter());
        InferenceInitRes response = new InferenceInitRes(false, new int[]{2});
        List<CommonResponse> responses = new ArrayList<>();
        responses.add(new CommonResponse(clientInfos.get(0), response));
        // test unsupported
        verticalLinearRegression.setPhase(3);
        try {
            verticalLinearRegression.inferenceControl(responses);
        } catch (UnsupportedOperationException e) {
            Assert.assertEquals(e.getMessage(), null);
        }
    }

    @Test
    public void inferenceControlPhase1() {
        // 特殊情况，所有的ID都不需要预测
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        String[] predUid = new String[]{"aa", "1a", "c3"};
//        String[][] data = new String[2][];
//        data[0] = new String[]{"aa", "10", "12.1"};
//        data[1] = new String[]{"1a", "10", "12.1"};
        VerticalLinearRegression verticalLinearRegression = new VerticalLinearRegression(new VerticalLinearParameter());
        List<CommonRequest> requests = verticalLinearRegression.initInference(clientInfos, predUid,new HashMap<>());
        InferenceInitRes response1 = new InferenceInitRes(false, new int[]{0});
        InferenceInitRes response2 = new InferenceInitRes(false, new int[]{1});
        InferenceInitRes response3 = new InferenceInitRes(false, new int[]{2});
        List<CommonResponse> responses = new ArrayList<>();
        responses.add(new CommonResponse(clientInfos.get(0), response1));
        responses.add(new CommonResponse(clientInfos.get(1), response2));
        responses.add(new CommonResponse(clientInfos.get(2), response3));
        verticalLinearRegression.setPhase(-255);
        List<CommonRequest> res = verticalLinearRegression.inferenceControl(responses);
        StringArray sa1 = (StringArray) res.get(0).getBody();
        StringArray sa2 = (StringArray) res.get(1).getBody();
        StringArray sa3 = (StringArray) res.get(2).getBody();
        Assert.assertEquals(sa1.getData().length, 0);
        Assert.assertEquals(sa2.getData().length, 0);
        Assert.assertEquals(sa3.getData().length, 0);

    }

    @Test
    public void inferenceControlPhase1_2() {
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        String[] predUid = new String[]{"aa", "1a", "c3"};
        VerticalLinearRegression verticalLinearRegression = new VerticalLinearRegression(new VerticalLinearParameter());
        List<CommonRequest> requests = verticalLinearRegression.initInference(clientInfos, predUid,new HashMap<>());
        InferenceInitRes response1 = new InferenceInitRes(false, new int[]{0});
        InferenceInitRes response2 = new InferenceInitRes(false, new int[]{1});
        InferenceInitRes response3 = new InferenceInitRes(false, new int[]{});
        List<CommonResponse> responses = new ArrayList<>();
        responses.add(new CommonResponse(clientInfos.get(0), response1));
        responses.add(new CommonResponse(clientInfos.get(1), response2));
        responses.add(new CommonResponse(clientInfos.get(2), response3));
        verticalLinearRegression.setPhase(-255);
        List<CommonRequest> res = verticalLinearRegression.inferenceControl(responses);
        StringArray sa1 = (StringArray) res.get(0).getBody();
        StringArray sa2 = (StringArray) res.get(1).getBody();
        StringArray sa3 = (StringArray) res.get(2).getBody();
        Assert.assertEquals(sa1.getData().length, 1);
        Assert.assertEquals(sa2.getData().length, 1);
        Assert.assertEquals(sa3.getData().length, 1);

    }

    @Test
    public void inferenceControlPhase2() {
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        DoubleArray response1 = new DoubleArray(new double[]{1.0, 0.8, 2.0});
        DoubleArray response2 = new DoubleArray(new double[]{1.0, 0.7});
        VerticalLinearRegression verticalLinearRegression = new VerticalLinearRegression(new VerticalLinearParameter());
        List<CommonResponse> responses = new ArrayList<>();
        responses.add(new CommonResponse(clientInfos.get(0), response1));
        responses.add(new CommonResponse(clientInfos.get(1), response2));
        verticalLinearRegression.setPhase(-1);
        List<CommonRequest> list = verticalLinearRegression.inferenceControl(responses);
        Assert.assertEquals(list.get(0).getClient(), clientInfos.get(0));
        Assert.assertEquals(list.get(1).getClient(), clientInfos.get(1));
        DoubleArray da1 = (DoubleArray) list.get(0).getBody();
        DoubleArray da2 = (DoubleArray) list.get(1).getBody();
        Assert.assertEquals(list.size(), 2);
        Assert.assertEquals(da1, response1);
        Assert.assertEquals(da2, response2);
    }

    @Test
    public void postInferenceControl() {
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        DoubleArray response1 = new DoubleArray(new double[]{1.0, 0.8, 2.0});
        DoubleArray response2 = new DoubleArray(new double[]{1.0, 0.7, 1.0});
        int[] idIndexArray = new int[] {0, 1, 2};
        String[] originIdArray = new String[] {"a", "b", "c", "d"};
        VerticalLinearRegression verticalLinearRegression = new VerticalLinearRegression(new VerticalLinearParameter(), idIndexArray, -2, originIdArray, new double[originIdArray.length]);
        List<CommonResponse> responses = new ArrayList<>();
        responses.add(new CommonResponse(clientInfos.get(0), response1));
        responses.add(new CommonResponse(clientInfos.get(1), response2));
        double[][] res = verticalLinearRegression.postInferenceControl(responses).getPredicts();
        double[] target = new double[] {2.0, 1.5, 3.0, Double.NaN};
        Assert.assertEquals(res.length, 4);
        for (int i = 0; i < target.length; i++) {
            Assert.assertEquals(res[i][0], target[i]);
        }

    }

    @Test
    public void testMetric() {
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        VerticalLinearRegression verticalLinearRegression = new VerticalLinearRegression(new VerticalLinearParameter());
        MetricValue nullMetrics = verticalLinearRegression.readMetrics();
        Assert.assertEquals(nullMetrics.getMetrics().size(),0);
        List<Pair<Integer, Double>> tmpRoundMetric = new ArrayList<>();
        Map<MetricType, List<Pair<Integer, Double>>> metricMap = new HashMap<>();
        metricMap.put(MetricType.ACC, tmpRoundMetric);
        verticalLinearRegression.setForTest( metricMap, clientInfos);
        MetricValue emptyMetrics = verticalLinearRegression.readMetrics();
        Assert.assertEquals(emptyMetrics.getMetrics().size(), 1);
        Assert.assertEquals(verticalLinearRegression.getAlgorithmType(), AlgorithmType.VerticalLinearRegression);
    }


}
