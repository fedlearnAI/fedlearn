package com.jdt.fedlearn.core.dispatch;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.base.SingleElement;
import com.jdt.fedlearn.core.entity.common.*;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.fake.StructureGenerate;
import com.jdt.fedlearn.core.parameter.KernelLinearRegressionParameter;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.data.Pair;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;

public class TestKernelLinearRegression {
    @Test
    public void getNextPhase() {
        KernelLinearRegression kernelLinearRegression = new KernelLinearRegression(new KernelLinearRegressionParameter());
        int p = kernelLinearRegression.getNextPhase(1, new ArrayList<>());
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
        KernelLinearRegression kernelLinearRegression = new KernelLinearRegression(new KernelLinearRegressionParameter());
        List<CommonRequest> requests = kernelLinearRegression.initControl(clientInfos, matchResult, features, new HashMap<>());
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
        KernelLinearRegression kernelLinearRegression = new KernelLinearRegression(new KernelLinearRegressionParameter());
        List<CommonResponse> responses = new ArrayList<>();
        responses.add(new CommonResponse(clientInfos.get(0), new SingleElement("init_success")));
        responses.add(new CommonResponse(clientInfos.get(1), new SingleElement("init_success")));
        responses.add(new CommonResponse(clientInfos.get(2), new SingleElement("init_success")));
        List<CommonRequest> requests = kernelLinearRegression.control(responses);
        //TODO
        Assert.assertEquals(requests.size(), 3);
    }

    @Test
    public void control1FromLastPhase() {
        KernelLinearRegression kernelLinearRegression = new KernelLinearRegression(new KernelLinearRegressionParameter());
        List<CommonResponse> responses = new ArrayList<>();

    }

    @Test
    public void control2() {
        KernelLinearRegression kernelLinearRegression = new KernelLinearRegression(new KernelLinearRegressionParameter());
        List<CommonResponse> responses = new ArrayList<>();

    }

    @Test
    public void control3() {
        KernelLinearRegression kernelLinearRegression = new KernelLinearRegression(new KernelLinearRegressionParameter());
        List<CommonResponse> responses = new ArrayList<>();

    }

    @Test
    public void control4() {
        KernelLinearRegression kernelLinearRegression = new KernelLinearRegression(new KernelLinearRegressionParameter());
        List<CommonResponse> responses = new ArrayList<>();

    }

    @Test
    public void control5() {
        KernelLinearRegression kernelLinearRegression = new KernelLinearRegression(new KernelLinearRegressionParameter());
        List<CommonResponse> responses = new ArrayList<>();

    }

    @Test
    public void testIsContinue() {
        KernelLinearRegression kernelLinearRegression = new KernelLinearRegression(new KernelLinearRegressionParameter());
        boolean isContinue = kernelLinearRegression.isContinue();
        Assert.assertTrue(isContinue);
    }

    @Test
    public void testIsInferenceContinue() {
        KernelLinearRegression kernelLinearRegression = new KernelLinearRegression(new KernelLinearRegressionParameter());
        boolean isInferenceContinue = kernelLinearRegression.isInferenceContinue();
        Assert.assertTrue(isInferenceContinue);
    }

    @Test
    public void testMetric() {
        KernelLinearRegression kernelLinearRegression = new KernelLinearRegression(new KernelLinearRegressionParameter());
        MetricValue metricTypeListMap = kernelLinearRegression.readMetrics();
        Map<MetricType, List<Pair<Integer, Double>>> metricMap = new HashMap<>();
        MetricValue realMetricValue = new MetricValue(metricMap);
        List<Pair<Integer, Double>> metric = new ArrayList<>();
        metric.add(0, new Pair<>(0, 0.0));
        metricMap.put(MetricType.TRAINLOSS, metric);
        Assert.assertEquals(realMetricValue, metricTypeListMap);


    }

    @Test
    public void testInitInference() {
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        KernelLinearRegression kernelLinearRegression = new KernelLinearRegression(new KernelLinearRegressionParameter());
        String[] predUid = new String[]{"0A", "1B", "2C"};
        List<CommonRequest> requests = kernelLinearRegression.initInference(clientInfos, predUid,new HashMap<>());
        Assert.assertEquals(clientInfos.size(), requests.size());
        CommonRequest commonRequest = requests.get(0);
        Message message = commonRequest.getBody();
        String[] predOriginId = ((InferenceInit) message).getUid();
        Assert.assertEquals(Arrays.toString(predOriginId), Arrays.toString(predUid));
    }


}