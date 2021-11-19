package com.jdt.fedlearn.core.dispatch;

import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.core.entity.base.SingleElement;
import com.jdt.fedlearn.core.entity.common.*;
import com.jdt.fedlearn.common.entity.core.feature.Features;
import com.jdt.fedlearn.core.entity.mixGBoost.BoostBodyReq;
import com.jdt.fedlearn.core.entity.mixGBoost.BoostBodyRes;
import com.jdt.fedlearn.core.entity.mixGBoost.BoostInferScoreRes;
import com.jdt.fedlearn.core.fake.StructureGenerate;
import com.jdt.fedlearn.core.parameter.MixGBParameter;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;
import com.jdt.fedlearn.core.type.MessageType;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author zhangwenxi
 */
public class MixGBoostTest {
    private List<ClientInfo> clientInfos;
    private Map<ClientInfo, Features> features;
    private MixGBParameter parameter;
    private MixGBoost mixGBoost;

    @BeforeMethod
    public void setUp() {
        clientInfos = StructureGenerate.threeClients();
        features = StructureGenerate.mixGbFeatures(clientInfos);
        parameter = new MixGBParameter();
        mixGBoost = new MixGBoost(parameter);
    }

    @Test
    public void testInitControl() {
        MatchResult matchResult = new MatchResult("matchTest", 10, "report");

        List<CommonRequest> requests = mixGBoost.initControl(clientInfos, matchResult, features, new HashMap<>());
        Assert.assertEquals(requests.size(), clientInfos.size());
        IntStream.range(0, requests.size()).parallel().forEachOrdered(i -> {
            CommonRequest first = requests.get(i);
            Assert.assertEquals(first.getPhase(), 0);
            Assert.assertFalse(first.isSync());
            Message message = first.getBody();
            Assert.assertTrue(message instanceof TrainInit);
            TrainInit body = (TrainInit) message;
            Assert.assertEquals(body.getFeatureList(), features.get(clientInfos.get(i)));
            Assert.assertEquals(body.getMatchId(), matchResult.getMatchId());
            Assert.assertEquals(body.getParameter(), parameter);
            Map<String, Object> other = body.getOthers();
            Assert.assertNotNull(other.get("commonFea"));
            Assert.assertNotNull(other.get("ENC_BITS"));
            Assert.assertEquals((int) other.get("numP"), clientInfos.size());
            Assert.assertEquals((int) other.get("thisPartyID"), i + 1);
        });
    }

    @Test
    public void testControl() {
        List<CommonResponse> responses = clientInfos.parallelStream()
                .map(clientInfo -> new CommonResponse(clientInfo, new SingleElement("init_success")))
                .collect(Collectors.toList());
        List<CommonRequest> requests = mixGBoost.control(responses);
        Assert.assertEquals(requests.size(), clientInfos.size());
        IntStream.range(0, responses.size()).parallel().mapToObj(requests::get)
                .map(CommonRequest::getBody).forEachOrdered(message -> {
            Assert.assertTrue(message instanceof BoostBodyReq);
            BoostBodyReq req = (BoostBodyReq) message;
            Assert.assertEquals(req.getMsgType(), MessageType.EpochInit);
        });
    }

    @Test(expectedExceptions = AssertionError.class)
    public void testInferenceControl() {
        List<CommonResponse> responses = clientInfos.parallelStream()
                .map(clientInfo -> new CommonResponse(clientInfo, null))
                .collect(Collectors.toList());
        mixGBoost.inferenceControl(responses);
    }

    @Test
    public void testPostInferenceControl() {
        double[] inferScores = new double[]{1.0, 2.0, Double.NaN, 4.0};
        double[] inferScoresSum = new double[]{3.0, 6.0, Double.NaN, 12.0};

        List<CommonResponse> responses = clientInfos.parallelStream()
                .map(clientInfo -> new CommonResponse(clientInfo, new BoostInferScoreRes(inferScores, false)))
                .collect(Collectors.toList());
        PredictRes predictRes = mixGBoost.postInferenceControl(responses);
        double[][] pred = predictRes.getPredicts();
        Assert.assertNotNull(pred);
        Assert.assertEquals(pred.length, inferScoresSum.length);
        IntStream.range(0, pred.length).forEachOrdered(i -> Assert.assertEquals(pred[i][0], inferScoresSum[i]));

        List<CommonResponse> responses2 = clientInfos.parallelStream()
                .map(clientInfo -> new CommonResponse(clientInfo, new BoostInferScoreRes(inferScores, true)))
                .collect(Collectors.toList());
        PredictRes predictRes2 = mixGBoost.postInferenceControl(responses2);
        double[][] pred2 = predictRes2.getPredicts();

        Assert.assertNotNull(pred2);
        Assert.assertEquals(pred2.length, inferScores.length);
        IntStream.range(0, pred2.length).forEachOrdered(i -> Assert.assertEquals(pred2[i][0], inferScores[i]));
    }

//    @Test
//    public void testInitInference() {
//        String[] predUid = new String[]{"0A", "1B", "2C"};
//        Map<String, Object> others = new HashMap<>();
//        boolean[] secureMode = new boolean[]{true, false};
//        for (boolean bool: secureMode) {
//            others.put("secureMode", bool);
//            List<CommonRequest> requests1 = mixGBoost.initInference(clientInfos, predUid, others);
//            Assert.assertEquals(requests1.size(), clientInfos.size());
//            IntStream.range(0, requests1.size()).forEachOrdered(i -> {
//                Assert.assertTrue(requests1.get(i).getBody() instanceof InferenceInit);
//                InferenceInit inferenceInit = (InferenceInit) requests1.get(i).getBody();
//                String[] predOriginId = inferenceInit.getUid();
//                Assert.assertEquals(Arrays.toString(predOriginId), Arrays.toString(predUid));
//                Map<String, Object> extraParams = inferenceInit.getOthers();
//                Assert.assertEquals(extraParams.get("useDistributedPaillier"), bool);
//                if (bool) {
//                    Assert.assertEquals(extraParams.get("numP"), clientInfos.size());
//                    Assert.assertEquals(extraParams.get("thisPartyID"), i + 1);
//                }
//            });
//        }
//    }

    @Test
    public void testIsContinue() {
        boolean isContinue = mixGBoost.isContinue();
        Assert.assertTrue(isContinue);
    }

    @Test
    public void testIsInferenceContinue() {
        boolean isInferenceContinue = mixGBoost.isInferenceContinue();
        Assert.assertTrue(isInferenceContinue);
    }

    @Test
    public void testReadMetrics() {
        MetricValue metricValue = mixGBoost.readMetrics();
        metricValue.getMetrics().values().parallelStream().forEach(value -> {
            Assert.assertNotNull(value);
            Assert.assertEquals(value.size(), 1);
            Assert.assertEquals(value.get(0).getKey().intValue(), 0);
            Assert.assertEquals(value.get(0).getValue().doubleValue(), -Double.MAX_VALUE);
        });
        BoostBodyRes boostBodyRes = new BoostBodyRes(MessageType.GiHi);
        List<CommonResponse> responses = clientInfos.parallelStream()
                .map(clientInfo -> new CommonResponse(clientInfo, boostBodyRes))
                .collect(Collectors.toList());
        mixGBoost.control(responses);
        MetricValue emptyMetrics = mixGBoost.readMetrics();
        emptyMetrics.getMetrics().values().parallelStream().forEach(value -> {
            Assert.assertNotNull(value);
            Assert.assertEquals(value.size(), 1);
            Assert.assertEquals(value.get(0).getKey().intValue(), 0);
            Assert.assertEquals(value.get(0).getValue().doubleValue(), -Double.MAX_VALUE);
        });
    }

    @Test
    public void testGetAlgorithmType() {
        AlgorithmType type = mixGBoost.getAlgorithmType();
        Assert.assertEquals(type, AlgorithmType.MixGBoost);
    }

    @Test
    public void testInferenceFinish() {
        List<CommonResponse> responses = clientInfos.parallelStream()
                .map(clientInfo -> new CommonResponse(clientInfo, new BoostInferScoreRes(new double[0], false)))
                .collect(Collectors.toList());
        List<CommonRequest> commonRequests = mixGBoost.inferenceControl(responses);
        Assert.assertNull(commonRequests);

        boolean isInferenceContinue = mixGBoost.isInferenceContinue();
        Assert.assertFalse(isInferenceContinue);
    }

    @Test
    public void testInferenceUids () {
        Map<ClientInfo, int[]> uid = new HashMap<>();
        uid.put(clientInfos.get(0), new int[]{1, 2, 3});
        uid.put(clientInfos.get(1), new int[]{3, 4, 5});
        uid.put(clientInfos.get(2), new int[]{2, 3, 4});

        List<CommonResponse> responses = clientInfos.parallelStream()
                .map(clientInfo -> new CommonResponse(clientInfo,
                        new InferenceInitRes(false, uid.get(clientInfo))))
                .collect(Collectors.toList());
        List<CommonRequest> commonRequests = mixGBoost.inferenceControl(responses);
        Assert.assertEquals(commonRequests.size(), clientInfos.size());
        commonRequests.parallelStream().map(CommonRequest::getBody).forEachOrdered(message -> {
            Assert.assertTrue(message instanceof InferenceInitRes);
            InferenceInitRes res = (InferenceInitRes) message;
            Assert.assertFalse(res.isAllowList());
            Assert.assertEquals(res.getUid(), new int[]{3});
        });
    }
}