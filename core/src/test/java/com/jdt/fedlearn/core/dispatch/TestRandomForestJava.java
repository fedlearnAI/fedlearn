package com.jdt.fedlearn.core.dispatch;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.common.*;
import com.jdt.fedlearn.core.entity.randomForest.*;
import com.jdt.fedlearn.core.entity.base.SingleElement;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.fake.StructureGenerate;
import com.jdt.fedlearn.core.math.MathExt;
import com.jdt.fedlearn.core.parameter.RandomForestParameter;
import com.jdt.fedlearn.core.psi.MatchResult;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;

public class TestRandomForestJava {
    @Test
    public void getNextPhase(){
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        RandomForestJava randomForest = new RandomForestJava(new RandomForestParameter());
        randomForest.setForTest(clientInfos,2,2,2,2,2,2,2);
        int p = randomForest.getNextPhase(-4);
        Assert.assertEquals(p, -3);
    }

    @Test
    public void initControl(){
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        Map<ClientInfo, Features> features = StructureGenerate.fgbFeatures(clientInfos);
        Map<Long, String> value = new HashMap<>();
        value.put(0L,"1a");
        value.put(1L,"2b");
        MatchResult matchResult = new MatchResult(10);
        RandomForestJava randomForest = new RandomForestJava(new RandomForestParameter());
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
    }

    @Test
    public void control1(){
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        RandomForestJava randomForest = new RandomForestJava(new RandomForestParameter());
        randomForest.setForTest(clientInfos,2,2,2,2,2,2,2);
        List<CommonResponse> responses = new ArrayList<>();
        responses.add(new CommonResponse(clientInfos.get(0), new SingleElement("init_success")));
        responses.add(new CommonResponse(clientInfos.get(1), new SingleElement("init_success")));
        responses.add(new CommonResponse(clientInfos.get(2), new SingleElement("init_success")));
        List<CommonRequest> requests = randomForest.control(responses);
        //TODO
        Assert.assertEquals(requests.size(), 3);
    }


    @Test
    public void control2(){
        List<CommonResponse> responses = new ArrayList<>();
        List<ClientInfo> clientInfos = new ArrayList<>();
        clientInfos.add(new ClientInfo("127.0.0.1", 80, "HTTP", 0));
        clientInfos.add(new ClientInfo("127.0.0.1", 81, "HTTP",1));
        clientInfos.add(new ClientInfo("127.0.0.1", 82, "HTTP",2));
        RandomForestJava randomForest = new RandomForestJava(new RandomForestParameter());
        randomForest.setForTest(clientInfos,2,2,3,3,2,2,1);
        String trainId = "181_RandomForest_Binary";

        RandomforestMessage[] randomforestMessages = new RandomforestMessage[3];
        randomforestMessages[0] = new RandomforestMessage("116538899140454451178489398576396543066286245116155706856513364826318316316018889335698655981482985158094654023717216264360943875385650660527980704942996936881032631996283651289130321284586024054794878424310493723956573336331182315248720134121148160920287813169513653948703528960788425139228235242779639033829:true:-:524844542743118482913876378195939410572376783400236201743470157119832864758782315059026479285927100111357139961040341258825413987635423057960999552967452800458121697959966383356020491759669406297241225601440370544090005324450272818240490131325782645143693081151190333322840420894769311748801971667634141040455809925260509185:-13:116538899140454451178489398576396543066286245116155706856513364826318316316018889335698655981482985158094654023717216264360943875385650660527980704942996936881032631996283651289130321284586024054794878424310493723956573336331182315248720134121148160920287813169513653948703528960788425139228235242779639033829,1:0:116538899140454451178489398576396543066286245116155706856513364826318316316018889335698655981482985158094654023717216264360943875385650660527980704942996936881032631996283651289130321284586024054794878424310493723956573336331182315248720134121148160920287813169513653948703528960788425139228235242779639033829,524844542743118482913876378195939410572376783400236201743470157119832864758782315059026479285927100111357139961040341258825413987635423057960999552967452800458121697959966383356020491759669406297241225601440370544090005324450272818240490131325782645143693081151190333322840420894769311748801971667634141040455809925260509185:-13:116538899140454451178489398576396543066286245116155706856513364826318316316018889335698655981482985158094654023717216264360943875385650660527980704942996936881032631996283651289130321284586024054794878424310493723956573336331182315248720134121148160920287813169513653948703528960788425139228235242779639033829||0,1;0,1;0,1;1;0;1;1;;;1||3");
        randomforestMessages[1] = new RandomforestMessage("||0;1;;1;0;1;0,1;0,1;1;1||3");
        randomforestMessages[2] = new RandomforestMessage("||0,1;2,3;1,2,3;0,2,3;0,1,2;0,1,3;0,3;1,2,3;0,1,2,3;0,1,2||3");
        for (int i = 0; i < 3; i++) {
            responses.add(new CommonResponse(clientInfos.get(i),randomforestMessages[i]));
        }
        List<CommonRequest> res = randomForest.controlPhase2(responses);
        for (int i = 0; i < 3; i++) {
            Assert.assertEquals(res.get(i).getClient(),clientInfos.get(i));
            Assert.assertEquals(((RandomForestReq)res.get(i).getBody()).getExtraInfo(), "0|1||[0, 1]|[0, 1]");
        }
    }

    @Test
    public void control3(){
        List<ClientInfo> clientInfos = new ArrayList<>();
        clientInfos.add(new ClientInfo("127.0.0.1", 80, "HTTP", 0));
        clientInfos.add(new ClientInfo("127.0.0.1", 81, "HTTP",1));
        clientInfos.add(new ClientInfo("127.0.0.1", 82, "HTTP",2));
        RandomForestJava randomForest = new RandomForestJava(new RandomForestParameter());
        randomForest.setForTest(clientInfos,2,2,2,2,2,3,1);
        String trainId = "181_RandomForest_Binary";
        List<CommonResponse> responses = new ArrayList<>();
        HashMap<Integer, ArrayList<Integer>> treeIdToSampleId = new HashMap<>();
        ArrayList<Integer> sample = new ArrayList<>();
        sample.add(0);
        sample.add(1);
        sample.add(2);
        for (int i = 0; i < 2; i++) {
            treeIdToSampleId.put(i, sample);
        }

        RandomForestRes[] response = new RandomForestRes[3];
        response[0] = new RandomForestRes(clientInfos.get(0), "", true, null, -1, null, treeIdToSampleId, 1);
        response[1] = new RandomForestRes(clientInfos.get(1), "", false, null, -1, null, treeIdToSampleId, 1);
        response[2] = new RandomForestRes(clientInfos.get(2), "", false, null, -1, null, treeIdToSampleId, 1);
        for (int i = 0; i < 3; i++) {
            responses.add(new CommonResponse(clientInfos.get(i),response[i]));
        }
        List<CommonRequest> res = randomForest.controlPhase3(responses);

        Assert.assertEquals(((RandomForestReq)res.get(0).getBody()).getBody(),"{\"client\":{\"ip\":\"127.0.0.1\",\"port\":80,\"path\":null,\"protocol\":\"HTTP\",\"uniqueId\":0},\"isActive\":true,\"body\":\"\",\"tidToXsampleId\":{\"0\":\"\\u0003\\u0000\\u0000\\u0000\\u00015@\\u0000\\u0000\\u0000\",\"1\":\"\\u0003\\u0000\\u0000\\u0000\\u00015@\\u0000\\u0000\\u0000\"},\"numTree\":1}|||{\"client\":{\"ip\":\"127.0.0.1\",\"port\":81,\"path\":null,\"protocol\":\"HTTP\",\"uniqueId\":1},\"isActive\":false,\"body\":\"\",\"tidToXsampleId\":{\"0\":\"\\u0003\\u0000\\u0000\\u0000\\u00015@\\u0000\\u0000\\u0000\",\"1\":\"\\u0003\\u0000\\u0000\\u0000\\u00015@\\u0000\\u0000\\u0000\"},\"numTree\":1}|||{\"client\":{\"ip\":\"127.0.0.1\",\"port\":82,\"path\":null,\"protocol\":\"HTTP\",\"uniqueId\":2},\"isActive\":false,\"body\":\"\",\"tidToXsampleId\":{\"0\":\"\\u0003\\u0000\\u0000\\u0000\\u00015@\\u0000\\u0000\\u0000\",\"1\":\"\\u0003\\u0000\\u0000\\u0000\\u00015@\\u0000\\u0000\\u0000\"},\"numTree\":1}|||");

    }

    @Test
    public void control4(){
        List<ClientInfo> clientInfos = new ArrayList<>();
        clientInfos.add(new ClientInfo("127.0.0.1", 80, "HTTP", 0));
        clientInfos.add(new ClientInfo("127.0.0.1", 81, "HTTP",1));
        clientInfos.add(new ClientInfo("127.0.0.1", 82, "HTTP",2));
        RandomForestJava randomForest = new RandomForestJava(new RandomForestParameter());
        randomForest.setForTest(clientInfos,2,2,2,2,2,3,1);
        String trainId = "181_RandomForest_Binary";
        List<CommonResponse> responses = new ArrayList<>();
        HashMap<Integer, ArrayList<Integer>> treeIdToSampleId = new HashMap<>();
        ArrayList<Integer> sample = new ArrayList<>();
        sample.add(0);
        sample.add(1);
        sample.add(2);
        for (int i = 0; i < 2; i++) {
            treeIdToSampleId.put(i, sample);
        }

        RandomForestRes[] response = new RandomForestRes[3];
        response[0] = new RandomForestRes(clientInfos.get(0), "CjIaIAAAAAAAAPA/AAAAAAAAAAAAAAAAAAAAAJM4Q/X///8/Kg57ImlzX2xlYWYiOiAwfQoyGiAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAADwPyoOeyJpc19sZWFmIjogMH0=", true, null, -1, "0|1||[0, 2]|[1, 2]", treeIdToSampleId);
        response[1] = new RandomForestRes(clientInfos.get(1), "", false, null, -1, "");
        response[2] = new RandomForestRes(clientInfos.get(2), "", false, null, -1, "");
        for (int i = 0; i < 3; i++) {
            responses.add(new CommonResponse(clientInfos.get(i),response[i]));
        }
        List<CommonRequest> res = randomForest.controlPhase4(responses);
        Assert.assertEquals(((RandomForestReq)res.get(0).getBody()).getBody(), "{\"treeId\":1.0,\"percentile\":50.0,\"nodeId\":0.0,\"featureId\":0.0}||");
        Assert.assertEquals(((RandomForestReq)res.get(1).getBody()).getBody(), "{\"treeId\":0.0,\"percentile\":50.0,\"nodeId\":0.0,\"featureId\":0.0}||");
        Assert.assertEquals(((RandomForestReq)res.get(2).getBody()).getBody(), "");
    }

    @Test
    public void control5(){

        List<ClientInfo> clientInfos = new ArrayList<>();
        clientInfos.add(new ClientInfo("127.0.0.1", 80, "HTTP", 0));
        clientInfos.add(new ClientInfo("127.0.0.1", 81, "HTTP",1));
        clientInfos.add(new ClientInfo("127.0.0.1", 82, "HTTP",2));
        RandomForestJava randomForest = new RandomForestJava(new RandomForestParameter());
        randomForest.setForTest(clientInfos,2,2,3,3,2,3,1);
        String trainId = "181_RandomForest_Binary";
        List<CommonResponse> responses = new ArrayList<>();
        HashMap<Integer, ArrayList<Integer>> treeIdToSampleId = new HashMap<>();
        ArrayList<Integer> sample = new ArrayList<>();
        sample.add(0);
        sample.add(1);
        sample.add(2);
        for (int i = 0; i < 2; i++) {
            treeIdToSampleId.put(i, sample);
        }

        RandomForestRes[] response = new RandomForestRes[3];
        response[0] = new RandomForestRes(clientInfos.get(0),
                "CocBCgwKCgoI5vlJnAGAYkAKGAoKCgigg99iAUBVQAoKCggWJHy+AOBmQBIaChgAAAAAAADwPwAAAAAAAAAAAAAAAAAAAAAqQXsiaXNfbGVhZiI6IDAsICJmZWF0dXJlX29wdCI6IDAsICJ2YWx1ZV9vcHQiOiAxNDguMDAwMTk2NTk0NzAwNTN9",
                true, null, -1, "1||[0, 1, 2]", treeIdToSampleId);
        response[1] = new RandomForestRes(clientInfos.get(1),
                "Cp8BChQKEgoQptIq2AAAPUCDWVjqC2rAvgooChIKECJ3kYABgEFAGg1jLJ/6sD4KEgoQKFW/wi9WtT5eWlZaJJCNvhIaChgAAAAAAAAAAAAAAAAAAPA/AAAAAAAAAAAqQXsiaXNfbGVhZiI6IDAsICJmZWF0dXJlX29wdCI6IDAsICJ2YWx1ZV9vcHQiOiAyOS4wMDAwMTI4ODQ1NzM3NDJ9",
                false, null, -1, "1||[0, 1, 2]", treeIdToSampleId);
        response[2] = new RandomForestRes(clientInfos.get(2), "", false, null, -1, "1||[0, 1, 2]", treeIdToSampleId);
        for (int i = 0; i < 3; i++) {
            responses.add(new CommonResponse(clientInfos.get(i),response[i]));
        }

        List<CommonRequest> res = randomForest.controlPhase5(responses);
        Assert.assertEquals(res.size(), 3);
    }


    @Test
    public void isStop(){
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        RandomForestJava randomForest = new RandomForestJava(new RandomForestParameter());
        randomForest.setForTest(clientInfos,2,2,2,2,2,2,1);
        boolean res = randomForest.isStop();
        Assert.assertEquals(false, res);
    }

    @Test
    public void isContinue(){
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        RandomForestJava randomForest = new RandomForestJava(new RandomForestParameter());
        randomForest.setForTest(clientInfos, 2,2,2,2,2,2,1);
        boolean res = randomForest.isContinue();
        Assert.assertEquals(true, res);
    }

    @Test
    public void sendForest(){

        List<ClientInfo> clientInfos = new ArrayList<>();
        clientInfos.add(new ClientInfo("127.0.0.1", 80, "HTTP", 0));
        clientInfos.add(new ClientInfo("127.0.0.1", 81, "HTTP",1));
        clientInfos.add(new ClientInfo("127.0.0.1", 82, "HTTP",2));
        RandomForestJava randomForest = new RandomForestJava(new RandomForestParameter());
        randomForest.setForTest(clientInfos,2,2,2,2,2,2,1);
        String trainId = "181_RandomForest_Binary";

        List<CommonResponse> response = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            CommonResponse commonResponse = new CommonResponse(clientInfos.get(i), null);
            response.add(commonResponse);
        }
        List<CommonRequest>  res = randomForest.sendForest(response);
        CommonRequest[] commonRequest = new CommonRequest[3];
        commonRequest[0] = new CommonRequest(clientInfos.get(0), new RandomforestMessage("{\"numTrees\":\"2\",\"Tree1\":\"{}\",\"Tree0\":\"{\\\"0\\\":{\\\"referenceJson\\\":\\\"{}\\\",\\\"isLeaf\\\":\\\"0\\\",\\\"nodeId\\\":\\\"0\\\",\\\"party\\\":\\\"{\\\\\\\"ip\\\\\\\":\\\\\\\"127.0.0.1\\\\\\\",\\\\\\\"port\\\\\\\":80,\\\\\\\"path\\\\\\\":null,\\\\\\\"protocol\\\\\\\":\\\\\\\"HTTP\\\\\\\",\\\\\\\"uniqueId\\\\\\\":0}\\\"}}\"}"), 0);
        commonRequest[1] = new CommonRequest(clientInfos.get(1), new RandomforestMessage("{\"numTrees\":\"2\",\"Tree1\":\"{\\\"0\\\":{\\\"referenceJson\\\":\\\"{}\\\",\\\"isLeaf\\\":\\\"0\\\",\\\"nodeId\\\":\\\"0\\\",\\\"party\\\":\\\"{\\\\\\\"ip\\\\\\\":\\\\\\\"127.0.0.1\\\\\\\",\\\\\\\"port\\\\\\\":81,\\\\\\\"path\\\\\\\":null,\\\\\\\"protocol\\\\\\\":\\\\\\\"HTTP\\\\\\\",\\\\\\\"uniqueId\\\\\\\":1}\\\"}}\",\"Tree0\":\"{}\"}"), 0);
        commonRequest[2] = new CommonRequest(clientInfos.get(2), new RandomforestMessage("{\"numTrees\":\"2\",\"Tree1\":\"{}\",\"Tree0\":\"{}\"}"), 0);
        for (int i = 0; i < 3; i++) {
            Assert.assertEquals(((RandomforestMessage)commonRequest[i].getBody()).getResponseStr(), ((RandomforestMessage)res.get(i).getBody()).getResponseStr());
        }
    }
    @Test
    public void printMetricMap(){
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        RandomForestJava randomForest = new RandomForestJava(new RandomForestParameter());
        randomForest.setForTest(clientInfos,2,2,2,2,2,2,1);
        String res = randomForest.printMetricMap();
        Assert.assertEquals("{RMSE=[1=-1.0]}", res);
    }

    @Test
    public void initInference(){
        List<ClientInfo> clientInfos = new ArrayList<>();
        clientInfos.add(new ClientInfo("127.0.0.1", 80, "HTTP", 0));
        clientInfos.add(new ClientInfo("127.0.0.1", 81, "HTTP",1));
        clientInfos.add(new ClientInfo("127.0.0.1", 82, "HTTP",2));
        RandomForestJava randomForest = new RandomForestJava(new RandomForestParameter());
        randomForest.setForTest(clientInfos,2,2,2,2,2,2,1);
        String[] predictUid = new String[2];
        predictUid[0] = "1";
        predictUid[1] = "2";
        List<CommonRequest> res = randomForest.initInference(clientInfos, predictUid);
        CommonRequest[] commonRequest = new CommonRequest[3];
        commonRequest[0] = new CommonRequest(clientInfos.get(0), new InferenceInit(predictUid),0);
        commonRequest[1] = new CommonRequest(clientInfos.get(1), new InferenceInit(predictUid),0);
        commonRequest[2] = new CommonRequest(clientInfos.get(2), new InferenceInit(predictUid),0);
        for (int i = 0; i < 3; i++) {
            Assert.assertEquals(((InferenceInit)commonRequest[i].getBody()).getUid(), ((InferenceInit)res.get(i).getBody()).getUid());

        }
    }

    @Test
    public void Inference1() {
        List<ClientInfo> clientInfos = new ArrayList<>();
        clientInfos.add(new ClientInfo("127.0.0.1", 80, "HTTP", 0));
        clientInfos.add(new ClientInfo("127.0.0.1", 81, "HTTP",1));
        clientInfos.add(new ClientInfo("127.0.0.1", 82, "HTTP",2));
        RandomForestJava randomForest = new RandomForestJava(new RandomForestParameter());

        List<CommonResponse> responses = new ArrayList<>();
        int[] predictUid = new int[0];
        String[] UidList = new String[2];
        UidList[0] = "1";
        UidList[1] = "2";
        InferenceInitRes inferenceInitRes = new InferenceInitRes(false, predictUid);
        for (int i = 0; i < 3; i++) {
            responses.add(new CommonResponse(clientInfos.get(i),inferenceInitRes));
        }

        randomForest.setForTest(clientInfos, UidList, null, null, null,null);
        List<CommonRequest> res = randomForest.inferenceControl(responses);

        List<CommonRequest> target = new ArrayList<>();
        for (ClientInfo clientInfo : clientInfos) {
            InferenceInit init = new InferenceInit(UidList);
            CommonRequest request = new CommonRequest(clientInfo, init, -1);
            target.add(request);
        }
        for (int i = 0; i < 3; i++) {
            Assert.assertEquals(res.get(i).getClient(), target.get(i).getClient());
            Assert.assertEquals(((InferenceInit)res.get(i).getBody()).getUid(), ((InferenceInit)target.get(i).getBody()).getUid());
        }
    }


    @Test
    public void Inference2() {
        List<ClientInfo> clientInfos = new ArrayList<>();
        clientInfos.add(new ClientInfo("127.0.0.1", 80, "HTTP", 0));
        clientInfos.add(new ClientInfo("127.0.0.1", 81, "HTTP",1));
        clientInfos.add(new ClientInfo("127.0.0.1", 82, "HTTP",2));
        RandomForestJava randomForest = new RandomForestJava(new RandomForestParameter());
        List<CommonResponse> responses = new ArrayList<>();
        String trainId = "181_RandomForest_Binary";

        String jsonResult1 = "{\n" +
                "  \"0\" : {\n" +
                "    \"0\" : [ \"L\" ],\n" +
                "    \"1\" : [ \"R\" ],\n" +
                "    \"19\" : [ \"R\" ],\n" +
                "    \"6\" : [ \"L\" ]\n" +
                "  },\n" +
                "  \"1\" : {\n" +
                "    \"0\" : [ \"L\" ],\n" +
                "    \"1\" : [ \"R\" ],\n" +
                "    \"20\" : [ \"L\" ],\n" +
                "    \"6\" : [ \"L\" ],\n" +
                "    \"7\" : [ \"R\" ],\n" +
                "    \"8\" : [ \"R\" ],\n" +
                "    \"9\" : [ \"R\" ]\n" +
                "  }\n" +
                "}";
        String jsonResult2 = "{\n" +
                "  \"0\" : {\n" +
                "    \"22\" : [ \"L\" ],\n" +
                "    \"8\" : [ \"L\" ]\n" +
                "  },\n" +
                "  \"1\" : {\n" +
                "    \"21\" : [ \"R\" ],\n" +
                "    \"14\" : [ \"L\" ]\n" +
                "  }\n" +
                "}";
        String jsonResult3 = "{\n" +
                "  \"0\" : {\n" +
                "    \"2\" : [ \"R\" ],\n" +
                "    \"3\" : [ \"R\" ],\n" +
                "    \"4\" : [ \"R\" ],\n" +
                "    \"5\" : [ \"1.0\" ],\n" +
                "    \"7\" : [ \"0.4\" ],\n" +
                "    \"9\" : [ \"R\" ],\n" +
                "    \"10\" : [ \"R\" ],\n" +
                "    \"13\" : [ \"0.3333333333333333\" ],\n" +
                "    \"14\" : [ \"L\" ],\n" +
                "    \"17\" : [ \"R\" ],\n" +
                "    \"18\" : [ \"0.19047619047619047\" ],\n" +
                "    \"20\" : [ \"0.6666666666666666\" ],\n" +
                "    \"21\" : [ \"R\" ],\n" +
                "    \"29\" : [ \"R\" ],\n" +
                "    \"30\" : [ \"0.3333333333333333\" ],\n" +
                "    \"35\" : [ \"0.02631578947368421\" ],\n" +
                "    \"36\" : [ \"0.1111111111111111\" ],\n" +
                "    \"39\" : [ \"0.027777777777777776\" ],\n" +
                "    \"40\" : [ \"0.08108108108108109\" ],\n" +
                "    \"43\" : [ \"0.30973451327433627\" ],\n" +
                "    \"44\" : [ \"0.75\" ],\n" +
                "    \"45\" : [ \"0.4854368932038835\" ],\n" +
                "    \"46\" : [ \"0.6875\" ],\n" +
                "    \"59\" : [ \"0.6666666666666666\" ],\n" +
                "    \"60\" : [ \"0.8484848484848485\" ]\n" +
                "  },\n" +
                "  \"1\" : {\n" +
                "    \"2\" : [ \"L\" ],\n" +
                "    \"3\" : [ \"R\" ],\n" +
                "    \"4\" : [ \"R\" ],\n" +
                "    \"5\" : [ \"0.631578947368421\" ],\n" +
                "    \"10\" : [ \"R\" ],\n" +
                "    \"13\" : [ \"0.5\" ],\n" +
                "    \"15\" : [ \"0.5\" ],\n" +
                "    \"16\" : [ \"R\" ],\n" +
                "    \"17\" : [ \"1.0\" ],\n" +
                "    \"18\" : [ \"R\" ],\n" +
                "    \"19\" : [ \"0.0\" ],\n" +
                "    \"22\" : [ \"R\" ],\n" +
                "    \"29\" : [ \"R\" ],\n" +
                "    \"30\" : [ \"0.6\" ],\n" +
                "    \"33\" : [ \"0.0\" ],\n" +
                "    \"34\" : [ \"0.3333333333333333\" ],\n" +
                "    \"37\" : [ \"0.42857142857142855\" ],\n" +
                "    \"38\" : [ \"0.10294117647058823\" ],\n" +
                "    \"41\" : [ \"0.07692307692307693\" ],\n" +
                "    \"42\" : [ \"0.25\" ],\n" +
                "    \"43\" : [ \"0.5142857142857142\" ],\n" +
                "    \"44\" : [ \"0.3103448275862069\" ],\n" +
                "    \"45\" : [ \"0.37209302325581395\" ],\n" +
                "    \"46\" : [ \"0.6896551724137931\" ],\n" +
                "    \"59\" : [ \"0.8793103448275862\" ],\n" +
                "    \"60\" : [ \"0.5\" ]\n" +
                "  }\n" +
                "}";
        String[] inferenceUid = {"591B"};
        double[] localPredict = {0.0};
        Randomforestinfer2Message[] randomforestinfer2Message = new Randomforestinfer2Message[3];
        randomforestinfer2Message[0] = new Randomforestinfer2Message(jsonResult1, inferenceUid, localPredict, "one-shot");
        randomforestinfer2Message[1] = new Randomforestinfer2Message(jsonResult2, inferenceUid, null, "");
        randomforestinfer2Message[2] = new Randomforestinfer2Message(jsonResult3, inferenceUid, null, "");
        for (int i = 0; i < 3; i++) {
            responses.add(new CommonResponse(clientInfos.get(i),randomforestinfer2Message[i]));
        }

        List<CommonRequest> res = randomForest.inferencePhase2(responses);
        List<CommonRequest> target = randomForest.createNullRequest(responses, -255);

        for (int i = 0; i < 3; i++) {
            Assert.assertEquals(res.get(i).getClient(), target.get(i).getClient());
            Assert.assertEquals((res.get(i).getBody()), (target.get(i).getBody()));
            Assert.assertEquals((res.get(i).getPhase()), (target.get(i).getPhase()));
        }
    }

    @Test
    public void isInferenceContinue() {
        RandomForestJava randomForest = new RandomForestJava(new RandomForestParameter());
        boolean res = randomForest.isInferenceContinue();
        Assert.assertEquals(res, true);
    }

    @Test
    public void testPostInferenceControl() {
        List<ClientInfo> clientInfos = new ArrayList<>();
        clientInfos.add(new ClientInfo("127.0.0.1", 80, "HTTP", 0));
        clientInfos.add(new ClientInfo("127.0.0.1", 81, "HTTP",1));
        clientInfos.add(new ClientInfo("127.0.0.1", 82, "HTTP",2));
        RandomForestJava randomForest = new RandomForestJava(new RandomForestParameter());
        String[] inferenceUid = {"1","2"};
        double[] pred = {0.6,0.1};
        Map<String, Integer> mapInferenceOrder = new HashMap<>();
        mapInferenceOrder.put("1", 0);
        mapInferenceOrder.put("2", 1);
        double[] localPredict = {0.0,0.0};
        randomForest.setForTest(clientInfos, inferenceUid, inferenceUid, pred, mapInferenceOrder,localPredict);
        double[][] res = randomForest.postInferenceControl(null).getPredicts();
        Assert.assertEquals(MathExt.transpose(res)[0], pred);
    }


    @Test
    public void testGetAlgorithmType() {
        RandomForestJava randomForest = new RandomForestJava(new RandomForestParameter());
        Assert.assertEquals(randomForest.getAlgorithmType().getAlgorithm(),"RandomForestJava");
    }

    @Test
    public void testmetric() {
        RandomForestJava randomForest = new RandomForestJava(new RandomForestParameter());
        Assert.assertEquals(randomForest.readMetrics().getMetrics(),new HashMap<>());
    }

    @Test
    public void testmetricArr() {
        RandomForestJava randomForest = new RandomForestJava(new RandomForestParameter());
        Assert.assertEquals(randomForest.metricArr(),new HashMap<>());
    }

}
