package com.jdt.fedlearn.core.dispatch;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.common.*;
import com.jdt.fedlearn.core.entity.feature.SingleFeature;
import com.jdt.fedlearn.core.entity.randomForest.*;
import com.jdt.fedlearn.core.entity.base.SingleElement;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.fake.StructureGenerate;
import com.jdt.fedlearn.core.math.MathExt;
import com.jdt.fedlearn.core.parameter.RandomForestParameter;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.core.type.EncryptionType;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.RFDispatchPhaseType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;

public class TestDistributedRandomForest {

    @Test
    public void initControl(){
        List<ClientInfo> clientInfos = StructureGenerate.twoClients();

        Map<ClientInfo, Features> features = new HashMap<>();
        List<SingleFeature> features0 = new ArrayList<>();
        features0.add(new SingleFeature("uid", "String"));
        features0.add(new SingleFeature("x1", "String"));
        features0.add(new SingleFeature("x2", "String"));
        features0.add(new SingleFeature("y", "String"));
        features.put(clientInfos.get(0), new Features(features0, "y"));

        List<SingleFeature> features1 = new ArrayList<>();
        features1.add(new SingleFeature("uid", "String"));
        features1.add(new SingleFeature("x3", "String"));
        features1.add(new SingleFeature("x4", "String"));
        features.put(clientInfos.get(1), new Features(features1));

        MatchResult matchResult = new MatchResult(3);
        RandomForestParameter parameter1 = new RandomForestParameter();
        MetricType[] metrics = new MetricType[]{MetricType.AUC, MetricType.ACC};
        String loss = "Regression:MSE";
        RandomForestParameter parameter2 = new RandomForestParameter(
                2,
                3,
                3,
                50,
                0.8,
                30,
                30,
                "Null",
                10,
                EncryptionType.IterativeAffine,
                metrics,
                loss,
                666);
        DistributedRandomForest randomForest = new DistributedRandomForest(parameter1);
        Map<String, Object> other = new HashMap<>();
        other.put("splitRatio", 1.0);
        List<CommonRequest> requests = randomForest.initControl(clientInfos, matchResult, features, other);
        Assert.assertEquals(clientInfos.size(), requests.size());
        CommonRequest first = requests.get(0);
        Assert.assertEquals(first.getPhase(),0);
        Assert.assertFalse(first.isSync());
        Message message = first.getBody();
        TrainInit body = (TrainInit) message;
        Assert.assertEquals(body.getFeatureList(), features.get(clientInfos.get(0)));

        randomForest = new DistributedRandomForest(parameter2);
        requests =  randomForest.initControl(clientInfos, matchResult, features, other);

        Assert.assertEquals(clientInfos.size(), requests.size());
        first = requests.get(0);
        Assert.assertEquals(first.getPhase(),0);
        Assert.assertFalse(first.isSync());
        message = first.getBody();
        body = (TrainInit) message;
        Assert.assertEquals(body.getFeatureList(), features.get(clientInfos.get(0)));
    }

    @Test
    public void control1(){
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        DistributedRandomForest randomForest = new DistributedRandomForest(new RandomForestParameter());
        List<CommonResponse> responses = new ArrayList<>();
        responses.add(new CommonResponse(clientInfos.get(0), new SingleElement("init_success")));
        responses.add(new CommonResponse(clientInfos.get(1), new SingleElement("init_success")));
        responses.add(new CommonResponse(clientInfos.get(2), new SingleElement("init_success")));
        List<CommonRequest> requests = randomForest.control(responses);
        Assert.assertEquals(requests.size(), 3);

        responses = new ArrayList<>();
        RandomForestTrainRes randomForestTrainRes = new RandomForestTrainRes();
        randomForestTrainRes.setMessageType(RFDispatchPhaseType.SEND_SAMPLE_ID);
        responses.add(new CommonResponse(clientInfos.get(0), randomForestTrainRes));
        requests = randomForest.control(responses);
        Assert.assertEquals(requests.size(), 1);

        responses = new ArrayList<>();
        responses.add(new CommonResponse(clientInfos.get(0), new RandomForestTrainReq()));
        try {
            randomForest.control(responses);
        } catch (Exception e) {

            Assert.assertEquals(e.getMessage(), "Message to RandomForestTrainRes error in control");
        }
    }


    @Test
    public void control2(){
        List<ClientInfo> clientInfos = new ArrayList<>();
        clientInfos.add(new ClientInfo("127.0.0.1", 80, "HTTP", "", "0"));
        clientInfos.add(new ClientInfo("127.0.0.1", 81, "HTTP", "", "1"));
        DistributedRandomForest randomForest = new DistributedRandomForest(new RandomForestParameter());
        RandomForestTrainRes randomForestTrainRes0 = new RandomForestTrainRes();
        randomForestTrainRes0.setActive(true);
        randomForestTrainRes0.setInit(true);
        List<Integer>[] featureMap = new ArrayList[2];
        featureMap[0] = new ArrayList<>();
        featureMap[0].add(0);
        featureMap[0].add(1);
        featureMap[1] = new ArrayList<>();
        featureMap[1].add(0);
        randomForestTrainRes0.setFeatureIds(featureMap);
        String[] encryptY = new String[1];
        encryptY[0] = "1452450455066536397192189168910917121579549144756146445241800560517265122975814:7925076045163469841733139553607511865674018635971698276961155640189909943865181402705484892998433339017402822464575486591100575274529127518527417451862964526026789815637447716968254811376947726284624485932035306505696755223429356379498910315955624940545349193434714908208300479428820018625580867181051542652:45840743033931265986258670189660689032704006639914906848370858655311479305415087079065255697218278582564077026401578755105373669802994380221612393808729649008681261553753047522975786914501114252703970199534874485322578903502253150713015867169628869900751564181385036546734022029805573107563649955394576471929:1125899906842624:1125899906842624:0:1125899906842624";
        String publicKey = "{a1\u000362167541134542531277506478271729877649886587652514460543291694622506627668015071077610684479955582025661318251655924369877832644205823121959146\u0002n0\u00035539842827535787516618404082067798905071453264901183666896955348885173405012814865431017623071589081507310302711795865203100047673226168067394990436841830557164019307269844218748154892574308291652522390219436313170946319838458385139139866274612910809\u0002encodedPrecision\u00039223372036854775808\u0002n1\u000345840743033931265986258670189660689032704006639914906848370858655311479305415087079065255697218278582564077026401578755105373669802994380221612393808729649008681261553753047522975786914501114252703970199534874485322578903502253150713015867169628869900751564181385036546734022029805573107563649955394576471929\u0002key_round\u00032\u0002g\u00034592029892691387625252146534218\u0002x\u0003437600556051393509146698409980270395562113464987\u0002ainv0\u00031810197526710069479482518947965088161011719555967122459903542337888877186941032719357358683604386044008043007253416035307467448659929259842424811563764267984311521121719063381389662812256439799932502685589064109858216368615458691894879129894745997401\u0002ainv1\u000311952851045498210859043371918527854604827785568792282918335085779662479880029909878401756893438437824111119624947971916962566571526681414368326112097280751087967508029726773117384643371842924957789757586385647951266091985145303584296723725704714286088692573022027570245146160571461469672890107457438527244780\u0002a0\u0003149009573519831976929691450349130639441905563027166682864142113671147793614212883251801988880225219628168603438000775983285762781903436431352561427706889272381141432622713304557521321988528658251499986522475986878554075778}";
        Map<Integer, List<Integer>> treeIdToSampleId = new HashMap<>();
        List<Integer> sampleList = new ArrayList<>();
        sampleList.add(0);
        treeIdToSampleId.put(0, sampleList);
        randomForestTrainRes0.setTidToSampleId(treeIdToSampleId);
        randomForestTrainRes0.setPublicKey(publicKey);
        randomForestTrainRes0.setEncryptionLabel(encryptY);

        RandomForestTrainRes randomForestTrainRes1 = new RandomForestTrainRes();
        randomForestTrainRes1.setActive(false);
        randomForestTrainRes1.setActive(true);
        randomForestTrainRes1.setFeatureIds(featureMap);
        List<CommonResponse> responses = new ArrayList<>();
        responses.add(new CommonResponse(clientInfos.get(0),randomForestTrainRes0));
        responses.add(new CommonResponse(clientInfos.get(1),randomForestTrainRes1));
        List<CommonRequest> res = randomForest.controlPhase2(responses);
        for (int i = 0; i < 2; i++) {
            Assert.assertEquals(res.get(i).getClient(),clientInfos.get(i));
        }

        randomForestTrainRes1.setTidToSampleId(treeIdToSampleId);
        responses = new ArrayList<>();
        responses.add(new CommonResponse(clientInfos.get(0),randomForestTrainRes0));
        responses.add(new CommonResponse(clientInfos.get(1),randomForestTrainRes1));
        randomForest.controlPhase2(responses);

        randomForestTrainRes0.setInit(false);
        randomForestTrainRes1.setInit(false);
        randomForestTrainRes1.setTidToSampleId(treeIdToSampleId);
        responses = new ArrayList<>();
        responses.add(new CommonResponse(clientInfos.get(0),randomForestTrainRes0));
        responses.add(new CommonResponse(clientInfos.get(1),randomForestTrainRes1));
        randomForest.controlPhase2(responses);


    }

    @Test
    public void control3(){
        List<ClientInfo> clientInfos = new ArrayList<>();
        clientInfos.add(new ClientInfo("127.0.0.1", 80, "HTTP", "", "0"));
        clientInfos.add(new ClientInfo("127.0.0.1", 81, "HTTP", "", "1"));
        DistributedRandomForest randomForest = new DistributedRandomForest(new RandomForestParameter());
        RandomForestTrainRes randomForestTrainRes0 = new RandomForestTrainRes();
        randomForestTrainRes0.setClient(clientInfos.get(0));
        randomForestTrainRes0.setBody("test1");
        randomForestTrainRes0.setActive(true);
        RandomForestTrainRes randomForestTrainRes1 = new RandomForestTrainRes();
        randomForestTrainRes1.setClient(clientInfos.get(1));
        randomForestTrainRes1.setBody("test2");

        List<CommonResponse> responses = new ArrayList<>();
        responses.add(new CommonResponse(clientInfos.get(0),randomForestTrainRes0));
        responses.add(new CommonResponse(clientInfos.get(1),randomForestTrainRes1));

        List<CommonRequest> res = randomForest.controlPhase3(responses);
        for (int i = 0; i < 2; i++) {
            Assert.assertEquals(res.get(i).getClient(),clientInfos.get(i));
        }
        Assert.assertEquals(((RandomForestTrainReq)res.get(0).getBody()).getBodyAll(), new String[]{"test1", "test2"});
    }

    @Test
    public void control4(){
        List<ClientInfo> clientInfos = new ArrayList<>();
        clientInfos.add(new ClientInfo("127.0.0.1", 80, "HTTP", "", "0"));
        clientInfos.add(new ClientInfo("127.0.0.1", 81, "HTTP", "", "1"));
        DistributedRandomForest randomForest = new DistributedRandomForest(new RandomForestParameter());

        List<CommonResponse> responses = new ArrayList<>();


        Map<String, String> splitMessage = new HashMap<>();


        Map<Integer, List<Integer>> treeIdToSampleId = new HashMap<>();
        List<Integer> sampleList = new ArrayList<>();
        sampleList.add(0);
        treeIdToSampleId.put(0, sampleList);
        RandomForestTrainRes randomForestTrainRes0 = new RandomForestTrainRes();
        randomForestTrainRes0.setClient(clientInfos.get(0));
        randomForestTrainRes0.setActive(true);
        Map<String, Map<Integer, List<Integer>>> tidToSampleIds = new HashMap<>();
        tidToSampleIds.put(clientInfos.get(0).toString(), treeIdToSampleId);
        splitMessage.put(clientInfos.get(0).toString(), "1111");
        randomForestTrainRes0.setSplitMessageMap(splitMessage);
        randomForestTrainRes0.setTidToSampleIds(tidToSampleIds);
        RandomForestTrainRes randomForestTrainRes1 = new RandomForestTrainRes();
        randomForestTrainRes1.setClient(clientInfos.get(1));



        responses.add(new CommonResponse(clientInfos.get(0),randomForestTrainRes0));
        responses.add(new CommonResponse(clientInfos.get(1),randomForestTrainRes1));

        List<CommonRequest> res = randomForest.controlPhase4(responses);
        for (int i = 0; i < 2; i++) {
            Assert.assertEquals(res.get(i).getClient(),clientInfos.get(i));
        }
        Assert.assertEquals(((RandomForestTrainReq)res.get(0).getBody()).getBody(), "1111");
        Assert.assertEquals(((RandomForestTrainReq)res.get(0).getBody()).getTidToSampleID(), treeIdToSampleId);
    }

    @Test
    public void control5(){

        List<ClientInfo> clientInfos = new ArrayList<>();
        clientInfos.add(new ClientInfo("127.0.0.1", 80, "HTTP", "", "0"));
        clientInfos.add(new ClientInfo("127.0.0.1", 81, "HTTP", "", "1"));

        DistributedRandomForest randomForest = new DistributedRandomForest(new RandomForestParameter());

        Map<Integer, List<Integer>> treeIdToSampleId = new HashMap<>();
        List<Integer> sampleList = new ArrayList<>();
        sampleList.add(0);
        treeIdToSampleId.put(0, sampleList);
        RandomForestTrainRes randomForestTrainRes0 = new RandomForestTrainRes();
        randomForestTrainRes0.setClient(clientInfos.get(0));
        randomForestTrainRes0.setActive(true);
        randomForestTrainRes0.setTreeIds(new String[]{"0"});
        Map<Integer, double[]> maskLeft = new HashMap<>();
        maskLeft.put(0, new double[]{0,1});
        randomForestTrainRes0.setMaskLeft(maskLeft);
        randomForestTrainRes0.setSplitMess(new String[]{"0"});

        Map<String, Map<Integer, List<Integer>>> tidToSampleIds = new HashMap<>();
        tidToSampleIds.put(clientInfos.get(0).toString(), treeIdToSampleId);

        randomForestTrainRes0.setTidToSampleIds(tidToSampleIds);
        RandomForestTrainRes randomForestTrainRes1 = new RandomForestTrainRes();
        randomForestTrainRes1.setClient(clientInfos.get(1));



        List<CommonResponse> responses = new ArrayList<>();
        responses.add(new CommonResponse(clientInfos.get(0),randomForestTrainRes0));
        responses.add(new CommonResponse(clientInfos.get(1),randomForestTrainRes1));

        List<CommonRequest> res = randomForest.controlPhase5(responses);
        for (int i = 0; i < 2; i++) {
            Assert.assertEquals(res.get(i).getClient(),clientInfos.get(i));
        }
        Assert.assertEquals(((RandomForestTrainReq)res.get(0).getBody()).getAllTreeIds().get(0), new String[]{"0"});
        Assert.assertEquals(((RandomForestTrainReq)res.get(0).getBody()).getSplitMessages().get(0), new String[]{"0"});
        Assert.assertEquals(((RandomForestTrainReq)res.get(0).getBody()).getMaskLefts().get(0), maskLeft);
    }


    @Test
    public void isContinue(){
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        DistributedRandomForest randomForest = new DistributedRandomForest(new RandomForestParameter());
//        randomForest.setForTest(clientInfos, 2,2,2,2,2,2,1);
        boolean res = randomForest.isContinue();
        Assert.assertEquals(true, res);
    }

    @Test
    public void sendForest(){

        List<ClientInfo> clientInfos = new ArrayList<>();
        clientInfos.add(new ClientInfo("127.0.0.1", 80, "HTTP", "", "0"));
        clientInfos.add(new ClientInfo("127.0.0.1", 81, "HTTP", "", "1"));
        DistributedRandomForest randomForest = new DistributedRandomForest(new RandomForestParameter());
        RandomForestTrainRes[] randomForestTrainRes = new RandomForestTrainRes[2];
        randomForestTrainRes[0] = new RandomForestTrainRes();
        randomForestTrainRes[0].setClient(clientInfos.get(0));
        randomForestTrainRes[0].setBody("");
//        randomForestTrainRes[0].setBody("{\"numTrees\":\"2\",\"Tree1\":\"{}\",\"Tree0\":\"{\\\"0\\\":{\\\"referenceJson\\\":\\\"{}\\\",\\\"isLeaf\\\":\\\"0\\\",\\\"nodeId\\\":\\\"0\\\",\\\"party\\\":\\\"{\\\\\\\"ip\\\\\\\":\\\\\\\"127.0.0.1\\\\\\\",\\\\\\\"port\\\\\\\":80,\\\\\\\"path\\\\\\\":null,\\\\\\\"protocol\\\\\\\":\\\\\\\"HTTP\\\\\\\",\\\\\\\"uniqueId\\\\\\\":0}\\\"}}\"}");
        randomForestTrainRes[0].setActive(true);
        randomForestTrainRes[1] = new RandomForestTrainRes();
        randomForestTrainRes[1].setClient(clientInfos.get(1));
//        randomForestTrainRes[0].setBody("{\"numTrees\":\"2\",\"Tree1\":\"{\\\"0\\\":{\\\"referenceJson\\\":\\\"{}\\\",\\\"isLeaf\\\":\\\"0\\\",\\\"nodeId\\\":\\\"0\\\",\\\"party\\\":\\\"{\\\\\\\"ip\\\\\\\":\\\\\\\"127.0.0.1\\\\\\\",\\\\\\\"port\\\\\\\":81,\\\\\\\"path\\\\\\\":null,\\\\\\\"protocol\\\\\\\":\\\\\\\"HTTP\\\\\\\",\\\\\\\"uniqueId\\\\\\\":1}\\\"}}\",\"Tree0\":\"{}\"}");
        randomForestTrainRes[1].setActive(false);
        randomForestTrainRes[0].setBody("");
        List<CommonResponse> response = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            CommonResponse commonResponse = new CommonResponse(clientInfos.get(i), randomForestTrainRes[i]);
            response.add(commonResponse);
        }
        List<CommonRequest>  res = randomForest.sendForest(response);
        Assert.assertEquals(((RandomForestTrainReq)res.get(0).getBody()).getBody(), "init");
        Assert.assertEquals(((RandomForestTrainReq)res.get(0).getBody()).getBody(), "init");
    }


    @Test
    public void Inference(){
        List<ClientInfo> clientInfos = new ArrayList<>();
        clientInfos.add(new ClientInfo("127.0.0.1", 80, "HTTP", "", "0"));
        clientInfos.add(new ClientInfo("127.0.0.1", 81, "HTTP", "", "1"));
        clientInfos.add(new ClientInfo("127.0.0.1", 82, "HTTP", "", "2"));
        DistributedRandomForest randomForest = new DistributedRandomForest(new RandomForestParameter());
        String[] predictUid = new String[2];
        predictUid[0] = "1";
        predictUid[1] = "2";
        List<CommonRequest> res = randomForest.initInference(clientInfos, predictUid,new HashMap<>());
        CommonRequest[] commonRequest = new CommonRequest[3];
        commonRequest[0] = new CommonRequest(clientInfos.get(0), new InferenceInit(predictUid),0);
        commonRequest[1] = new CommonRequest(clientInfos.get(1), new InferenceInit(predictUid),0);
        commonRequest[2] = new CommonRequest(clientInfos.get(2), new InferenceInit(predictUid),0);
        for (int i = 0; i < 3; i++) {
            Assert.assertEquals(((InferenceInit)commonRequest[i].getBody()).getUid(), ((InferenceInit)res.get(i).getBody()).getUid());
        }


        List<CommonResponse> responses = new ArrayList<>();
        int[] predictUid1 = new int[0];
        String[] UidList = new String[2];
        UidList[0] = "1";
        UidList[1] = "2";
        InferenceInitRes inferenceInitRes = new InferenceInitRes(false, predictUid1);
        for (int i = 0; i < 3; i++) {
            responses.add(new CommonResponse(clientInfos.get(i),inferenceInitRes));
        }

        res = randomForest.inferenceControl(responses);
        for (int i = 0; i < 3; i++) {
            Assert.assertEquals(((InferenceInit)commonRequest[i].getBody()).getUid(), ((InferenceInit)res.get(i).getBody()).getUid());
        }


        responses = new ArrayList<>();

        Map<Integer, Map<Integer, List<String>>> treeInfo1 = new HashMap<>();
        Map<Integer, Map<Integer, List<String>>> treeInfo2 = new HashMap<>();
        Map<Integer, List<String>> tree1 = new HashMap<>();
        Map<Integer, List<String>> tree2 = new HashMap<>();
        List<String> list = Arrays.asList("L","R");
        tree1.put(0, list);
        treeInfo1.put(0,tree1);

        String[] inferenceUid = {"1"};
        double[] localPredict = {0.0};
        RandomforestInferMessage[] randomforestInferMessage = new RandomforestInferMessage[3];
        randomforestInferMessage[0] = new RandomforestInferMessage(inferenceUid, localPredict, "active", null);
        randomforestInferMessage[1] = new RandomforestInferMessage(inferenceUid, null, "", treeInfo1);
        randomforestInferMessage[2] = new RandomforestInferMessage(inferenceUid, null, "", treeInfo1);
        for (int i = 0; i < 3; i++) {
            responses.add(new CommonResponse(clientInfos.get(i), randomforestInferMessage[i]));
        }

        res = randomForest.inferenceControl(responses);
        for (int i = 0; i < 3; i++) {
            Assert.assertEquals(((RandomforestInferMessage)res.get(i).getBody()).getInferenceUid()[0], "1");
        }

        responses = new ArrayList<>();
        randomforestInferMessage = new RandomforestInferMessage[3];
        randomforestInferMessage[0] = new RandomforestInferMessage(null, localPredict, "active", null);
        randomforestInferMessage[1] = new RandomforestInferMessage(null, null, "", null);
        randomforestInferMessage[2] = new RandomforestInferMessage(null, null, "", null);
        for (int i = 0; i < 3; i++) {
            responses.add(new CommonResponse(clientInfos.get(i), randomforestInferMessage[i]));
        }

        res = randomForest.inferenceControl(responses);
        for (int i = 0; i < 3; i++) {
            Assert.assertEquals(res.get(i).getBody(), null);
        }

        double[][] result = randomForest.postInferenceControl(null).getPredicts();
        Assert.assertEquals(result[0][0], 0.0);
    }

    @Test
    public void isInferenceContinue() {
        DistributedRandomForest randomForest = new DistributedRandomForest(new RandomForestParameter());
        boolean res = randomForest.isInferenceContinue();
        Assert.assertEquals(res, true);
    }


    @Test
    public void testGetAlgorithmType() {
        DistributedRandomForest randomForest = new DistributedRandomForest(new RandomForestParameter());
        Assert.assertEquals(randomForest.getAlgorithmType().getAlgorithm(),"DistributedRandomForest");
    }

    @Test
    public void testmetric() {
        DistributedRandomForest randomForest = new DistributedRandomForest(new RandomForestParameter());
        Assert.assertEquals(randomForest.readMetrics().getMetrics(),new HashMap<>());
    }



}
