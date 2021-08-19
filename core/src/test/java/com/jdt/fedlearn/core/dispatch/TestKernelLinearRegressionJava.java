package com.jdt.fedlearn.core.dispatch;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.base.SingleElement;
import com.jdt.fedlearn.core.entity.common.*;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.entity.kernelLinearRegression.InferenceReqAndRes;
import com.jdt.fedlearn.core.entity.kernelLinearRegression.TrainReq;
import com.jdt.fedlearn.core.entity.kernelLinearRegression.TrainRes;
import com.jdt.fedlearn.core.fake.StructureGenerate;
import com.jdt.fedlearn.core.parameter.KernelLinearRegressionParameter;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.core.type.KernelDispatchJavaPhaseType;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.data.Pair;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;

public class TestKernelLinearRegressionJava {
    private final String taskId = "181";
    private final String token = taskId + "_" + "KernelBinaryClassification";

    @Test
    public void getNextPhase() {
        KernelLinearRegressionJava kernelLinearRegressionJava = new KernelLinearRegressionJava(new KernelLinearRegressionParameter());
        int p = kernelLinearRegressionJava.getNextPhase(1,new ArrayList<>());
        Assert.assertEquals(p, 2);
    }

    @Test
    public void initControl() {
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        Map<Long, String> value = new HashMap<>();
        value.put(0L, "1a");
        value.put(1L, "2b");
        MatchResult matchResult = new MatchResult(10);
        Map<ClientInfo, Features> features = StructureGenerate.fgbFeatures(clientInfos);
        KernelLinearRegressionJava kernelLinearRegressionJava = new KernelLinearRegressionJava(new KernelLinearRegressionParameter());
        Map<String, Object> other = new HashMap<>();
        other.put("splitRatio", 0.7);
        List<CommonRequest> requests = kernelLinearRegressionJava.initControl(clientInfos, matchResult, features, other);
        Assert.assertEquals(clientInfos.size(), requests.size());
        CommonRequest first = requests.get(0);
        Assert.assertEquals(first.getPhase(), 0);
        Assert.assertFalse(first.isSync());
        Message message = first.getBody();
        TrainInit body = (TrainInit) message;
        Assert.assertEquals(body.getFeatureList(), features.get(clientInfos.get(0)));
    }

    @Test
    public void control1FromInit() {
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        KernelLinearRegressionJava kernelLinearRegressionJava = new KernelLinearRegressionJava(new KernelLinearRegressionParameter());
        List<CommonResponse> responses = new ArrayList<>();
        responses.add(new CommonResponse(clientInfos.get(0), new SingleElement("init_success")));
        responses.add(new CommonResponse(clientInfos.get(1), new SingleElement("init_success")));
        responses.add(new CommonResponse(clientInfos.get(2), new SingleElement("init_success")));
        List<CommonRequest> requests = kernelLinearRegressionJava.control(responses);
        Assert.assertEquals(requests.size(), 3);
    }

    @Test
    public void control() {
        KernelLinearRegressionJava kernelLinearRegressionJava = new KernelLinearRegressionJava(new KernelLinearRegressionParameter());
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        List<CommonResponse> responses = new ArrayList<>();
        boolean isActive = false;
        double[][] vector = new double[][]{{0d, 0d},{1d,2d}};
        double paraNorm = 0d;
        Map<MetricType, List<Double>> metricTypeListMap = new HashMap<>();
        List<Double> list = new ArrayList<>();
        list.add(-Double.MAX_VALUE);
        metricTypeListMap.put(MetricType.TRAINLOSS, list);
        metricTypeListMap.put(MetricType.AUC, list);
        int numClassRound = 0;
        int clientInd = 1;
        KernelDispatchJavaPhaseType kernelDispatchJavaPhaseType = KernelDispatchJavaPhaseType.UPDATE_METRIC;
        clientInfos.forEach(client -> responses.add(new CommonResponse(client, new TrainRes(client, vector, paraNorm,isActive,clientInd,numClassRound, clientInfos,kernelDispatchJavaPhaseType))));
        List<CommonRequest> requests = kernelLinearRegressionJava.control(responses);
        List<CommonRequest> target = new ArrayList<>();
        TrainReq req = new TrainReq(clientInfos.get(0), null, false);
        clientInfos.forEach(client -> target.add(new CommonRequest(client, req, 1)));
        Assert.assertEquals(requests.size(), target.size());
        Assert.assertEquals(requests.get(0).getPhase(), target.get(0).getPhase());
        Assert.assertEquals(requests.get(0).getClient(), target.get(0).getClient());
        Assert.assertEquals(((TrainReq) requests.get(0).getBody()).getClient(), ((TrainReq) target.get(0).getBody()).getClient());
        Assert.assertNull(((TrainReq) requests.get(0).getBody()).getValueList());
        Assert.assertEquals(((TrainReq) requests.get(0).getBody()).isUpdate(), ((TrainReq) target.get(0).getBody()).isUpdate());
    }

    @Test
    public void testControlPhase1() {
        KernelLinearRegressionJava kernelLinearRegressionJava = new KernelLinearRegressionJava(new KernelLinearRegressionParameter());
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        kernelLinearRegressionJava.setForUnitTest(2);
        List<CommonResponse> responses = new ArrayList<>();
        boolean isActive = false;
        int numClassRound = 1;
        clientInfos.forEach(client -> responses.add(new CommonResponse(client, new TrainRes(client, numClassRound, isActive))));
        List<CommonRequest> controlPhase2Res = kernelLinearRegressionJava.controlPhase1(responses);
        List<CommonRequest> target = new ArrayList<>();
        TrainReq req = new TrainReq(clientInfos.get(0), new ArrayList<>());
        clientInfos.forEach(client -> target.add(new CommonRequest(client, req, 1)));
        Assert.assertEquals(controlPhase2Res.size(), target.size());
        Assert.assertEquals(controlPhase2Res.get(0).getPhase(), target.get(0).getPhase());
        Assert.assertEquals(controlPhase2Res.get(0).getClient(), target.get(0).getClient());
        Assert.assertEquals(((TrainReq) controlPhase2Res.get(0).getBody()).getClient(), ((TrainReq) target.get(0).getBody()).getClient());
    }


    @Test
    public void testControlPhase2() {
        KernelLinearRegressionJava kernelLinearRegressionJava = new KernelLinearRegressionJava(new KernelLinearRegressionParameter());
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        kernelLinearRegressionJava.setForUnitTest(2);
        List<CommonResponse> responses = new ArrayList<>();
        boolean isActive = true;
        double[][] vector = new double[][]{{0d, 0d}, {1d, 2d}};
        double paraNorm = 0d;
        Map<MetricType, List<Double>> metricTypeListMap = new HashMap<>();
        List<Double> list = new ArrayList<>();
        list.add(-Double.MAX_VALUE);
        metricTypeListMap.put(MetricType.TRAINLOSS, list);
        metricTypeListMap.put(MetricType.AUC, list);
        int numClassRound = 0;
        clientInfos.forEach(client -> responses.add(new CommonResponse(client, new TrainRes(client, vector, paraNorm, isActive, 0, numClassRound,clientInfos, KernelDispatchJavaPhaseType.UPDATE_METRIC))));
        List<CommonRequest> controlPhase1Res = kernelLinearRegressionJava.controlPhase2(responses);
        List<CommonRequest> target = new ArrayList<>();
        List<Integer> sampleIndex = new ArrayList<>();
        sampleIndex.add(0);
        sampleIndex.add(1);
        double[] valuelist = new double[]{0, 0};
        boolean isUpdate = true;
        TrainReq req = new TrainReq(clientInfos.get(0), valuelist, sampleIndex, isUpdate);
        clientInfos.forEach(client -> target.add(new CommonRequest(client, req, 2)));
        Assert.assertEquals(controlPhase1Res.size(), target.size());
        Assert.assertEquals(controlPhase1Res.get(0).getPhase(), target.get(0).getPhase());
        Assert.assertEquals(controlPhase1Res.get(0).getClient(), target.get(0).getClient());
        Assert.assertEquals(((TrainReq) controlPhase1Res.get(0).getBody()).getClient(), ((TrainReq) target.get(0).getBody()).getClient());
        Assert.assertEquals(((TrainReq) controlPhase1Res.get(0).getBody()).isUpdate(), ((TrainReq) target.get(0).getBody()).isUpdate());
    }


    @Test
    public void testIsContinue() {
        KernelLinearRegressionJava kernelLinearRegressionJava = new KernelLinearRegressionJava(new KernelLinearRegressionParameter());
        boolean isContinue = kernelLinearRegressionJava.isContinue();
        Assert.assertTrue(isContinue);
    }

    @Test
    public void testIsInferenceContinue() {
        KernelLinearRegressionJava kernelLinearRegressionJava = new KernelLinearRegressionJava(new KernelLinearRegressionParameter());
        boolean isInferenceContinue = kernelLinearRegressionJava.isInferenceContinue();
        Assert.assertTrue(isInferenceContinue);
    }

    @Test
    public void testMetric() {
        KernelLinearRegressionJava kernelLinearRegressionJava = new KernelLinearRegressionJava(new KernelLinearRegressionParameter());
        MetricValue metricTypeListMap = kernelLinearRegressionJava.readMetrics();
        Map<MetricType, List<Pair<Integer, Double>>> metricMap = new HashMap<>();
        List<Pair<Integer, Double>> metric = new ArrayList<>();
        metric.add(0, new Pair<>(0, -Double.MAX_VALUE));
        metricMap.put(MetricType.TRAINLOSS, metric);
        Assert.assertEquals(new HashMap<>(), metricTypeListMap.getMetrics());
    }

    @Test
    public void testInitInference() {
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        KernelLinearRegressionJava kernelLinearRegressionJava = new KernelLinearRegressionJava(new KernelLinearRegressionParameter());
        String[] predUid = new String[]{"0A", "1B", "2C"};
        List<CommonRequest> requests = kernelLinearRegressionJava.initInference(clientInfos, predUid, new HashMap<>());
        Assert.assertEquals(clientInfos.size(), requests.size());
        CommonRequest commonRequest = requests.get(0);
        Message message = commonRequest.getBody();
        String[] predOriginId = ((com.jdt.fedlearn.core.entity.common.InferenceInit) message).getUid();
        Assert.assertEquals(Arrays.toString(predOriginId), Arrays.toString(predUid));
    }

    @Test
    public void testinferenceControlPhase0() {
        KernelLinearRegressionJava kernelLinearRegressionJava = new KernelLinearRegressionJava(new KernelLinearRegressionParameter());
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        String[] originIdArray = new String[]{"0", "1", "2"};
        int[] idIndexArray = new int[]{0, 1, 2};
        kernelLinearRegressionJava.setForUnitTestInfer(originIdArray, idIndexArray, -1);
        List<CommonResponse> commonResponses = new ArrayList<>();
        InferenceInitRes inferenceInitRes = new InferenceInitRes(false, new int[0]);
        clientInfos.forEach(client -> commonResponses.add(new CommonResponse(client, inferenceInitRes)));
        List<CommonRequest> res = kernelLinearRegressionJava.inferenceFilter(commonResponses);
        List<CommonRequest> commonRequests = new ArrayList<>();
        Message init = new InferenceInit(originIdArray);
        clientInfos.forEach(client -> commonRequests.add(new CommonRequest(client, init, -1)));
        Assert.assertEquals(res.get(0).getClient(), commonRequests.get(0).getClient());
        Assert.assertEquals(res.get(0).getPhase(), commonRequests.get(0).getPhase());
    }

    @Test
    public void testInferenceControlPhase1() {
        KernelLinearRegressionJava kernelLinearRegressionJava = new KernelLinearRegressionJava(new KernelLinearRegressionParameter());
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        String[] originIdArray = new String[]{"0", "1", "2"};
        int[] idIndexArray = new int[]{0, 1, 2};
        kernelLinearRegressionJava.setForUnitTestInfer(originIdArray, idIndexArray, -2);
        List<CommonResponse> commonResponses = new ArrayList<>();
        InferenceReqAndRes inferenceInitRes = new InferenceReqAndRes(new ArrayList<>(), true, 1,KernelDispatchJavaPhaseType.INFERENCE_EMPTY_REQUEST);
        clientInfos.forEach(client -> commonResponses.add(new CommonResponse(client, inferenceInitRes)));
        List<CommonRequest> res = kernelLinearRegressionJava.constructHeaders(commonResponses);
        List<CommonRequest> commonRequests = new ArrayList<>();
        Map<String, Double> predict = new HashMap<>();
        Message reqAndRes = new InferenceReqAndRes(clientInfos.get(0), predict);
        clientInfos.forEach(client -> commonRequests.add(new CommonRequest(client, reqAndRes, -2)));
        Assert.assertEquals(res.get(0).getClient(), commonRequests.get(0).getClient());
        Assert.assertEquals(res.get(0).getPhase(), commonRequests.get(0).getPhase());
        Assert.assertEquals(((InferenceReqAndRes) res.get(0).getBody()).getPredict(), ((InferenceReqAndRes) commonRequests.get(0).getBody()).getPredict());
        Assert.assertEquals(((InferenceReqAndRes) res.get(0).getBody()).getClient(), ((InferenceReqAndRes) commonRequests.get(0).getBody()).getClient());
    }

    @Test
    public void testgetAlgorithmType() {
        KernelLinearRegressionJava kernelLinearRegressionJava = new KernelLinearRegressionJava(new KernelLinearRegressionParameter());
        AlgorithmType type = kernelLinearRegressionJava.getAlgorithmType();
        Assert.assertEquals(type, AlgorithmType.KernelBinaryClassificationJava);
    }

}