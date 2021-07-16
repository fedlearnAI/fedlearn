package com.jdt.fedlearn.core.dispatch;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.common.*;
import com.jdt.fedlearn.core.entity.randomForest.RandomForestReq;
import com.jdt.fedlearn.core.entity.randomForest.RandomForestRes;
import com.jdt.fedlearn.core.entity.randomForest.RandomforestMessage;
import com.jdt.fedlearn.core.entity.base.SingleElement;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.entity.randomForest.Randomforestinfer2Message;
import com.jdt.fedlearn.core.fake.StructureGenerate;
import com.jdt.fedlearn.core.math.MathExt;
import com.jdt.fedlearn.core.parameter.RandomForestParameter;
import com.jdt.fedlearn.core.psi.MatchResult;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestRandomForest {

    @Test
    public void getNextPhase(){
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        RandomForest randomForest = new RandomForest(new RandomForestParameter());
        randomForest.setForTest(clientInfos,2,2,2,2,2,2,2);
        int p = randomForest.getNextPhase(-4, new ArrayList<>());
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

        RandomForest randomForest = new RandomForest(new RandomForestParameter());
        List<CommonRequest> requests = randomForest.initControl(clientInfos, matchResult, features, new HashMap<>());
        Assert.assertEquals(clientInfos.size(), requests.size());
        CommonRequest first = requests.get(0);
        Assert.assertEquals(first.getPhase(),0);
        Assert.assertFalse(first.isSync());
        Message message = first.getBody();
        TrainInit body = (TrainInit) message;
        Assert.assertEquals(body.getFeatureList(), features.get(clientInfos.get(0)));

    }

    @Test
    public void control1FromInit(){
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        RandomForest randomForest = new RandomForest(new RandomForestParameter());
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
    public void control1FromInitSingle(){
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        RandomForest randomForest = new RandomForest(new RandomForestParameter());
        randomForest.setForTest(clientInfos,2,2,2,2,2,2,1);
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
        RandomForest randomForest = new RandomForest(new RandomForestParameter());
        randomForest.setForTest(clientInfos,2,2,3,3,2,2,1);
        String trainId = "181_RandomForest_Binary";

        RandomforestMessage[] randomforestMessages = new RandomforestMessage[3];
        randomforestMessages[0] = new RandomforestMessage("IgwSCioIcGFpbGxpZXIy2Q4K8AQS6AQzMTg4ODExODg1NDY1NDM5NzE0NTYwMjkwOTEyMTcwMjM0MjUyMzUyNTcxNjg2MjE1Nzc0NjgxNjM1NDI4MDkxMjczNTI0NjIwNjgwMDMyMTkwMzc0MjY4NjM4NTc3NDM0MDEzMDk4NjMzOTg0NzYyNzk1MjY5MTQ0NjI5NTQxNTI1NjE3MzM5MDcxNjQ3Mzg4MjkzODI5NDI1NzYwMTEyMDAzMDY2ODIyNTI3Mzc2NzQzOTE4MzMyMDI4NTcwNTkxMzU2MTQ3OTMyODgxNjc4MzcxNTQ3NDY1NzA0OTI0Nzk3NTg3NDg2MDc4NjgyMTYwMjAyNTcwMjY0ODM2MzIwOTYwMzU4Mjk2NjkzODk1OTgzMDI3NTE1ODg0MzEyMjUxNTMzMjk2ODIxNTUwNzE4MTMxODMwODE0NTI5NTcwNjIzNTE1NTA5MDgyNTk4OTAxNDE2MDQ1MjAwMDkxNzc5MzcyODgxODY2NzY3NjAyNjcwMDEyNTk5MzM0Mjk2OTU3MDkwNjAyNzAzNjA1OTk1Nzc5MzEwNTkxNjgzNjU3MzEyMjUzNzM2NzU4Mjc1ODEzNjU0MTYxMTY2NTAwNjkzNDY0NTc2MDE3NzcwODAzNDE2NDE3OTkyOTgyNDM0ODAzMTMyMDUyNDI5NTIyMjY5OTY5NDQ3ODcyMTU2NDc2NTEwMTY5NDY5ODQ3NTgwMDYwMjMyMDQ4ODg0OTI4MDA1ODI1MDIyOTE3OTcyNTA3ODIxMzYyMDM0MTU1MzUyNjc5MjQ4MzQ0MzA0MzI3MDgzNDQ5OTg2OTg0MzQzNzczODI3NzA4NjY2GgMtMTMK7wQS6QQxMjExMjg5OTM4NTkyMzIyOTYwMjE0MDYyMjE2Mzg0NTYzOTk3NzE5Mzk5ODExNzExNTI4NTUyNjY3OTIyODg5OTA4MzAxMDg4NDEzNTQ1Njk4NDkwNzc2NDAxMzkwMDE5MjUyNTczMTA1NzYyMTU3NTg4OTMzMTMyMTYxODQyNDkxOTQ2OTEzMDgxMjQ0MTkzNTEyNDc4NTc0ODY3MDQ2ODQxMDc3NDI3MjQxMTU1NjM1ODgyMDkwODI2MDY2NDI1NjQxMjU2OTI2MzM1ODc1OTg2NzA2NDMyNjM1MzYyMTMyOTk1NjYxNjI3NzI3OTI4MDA2ODE2NTY0OTg4MzU4MDQ3NzAyNzY1NDI3NjA3MTYwODgzNzk3MjcyNzUwNjg2MTQyNDE1MzQyNDc4Mjc4ODc3OTAxMDYxMDE3NTI5ODI5MzU1MDYwMzk1OTgwMTMyNjM4Mzg5NTgyNTcyMDYyMDAwOTk5NTY2Njg4NDc3MTg3NzMwNDk3Njk3ODgzNTQyMjQxNDk1NjM5MDA2NzA4MjQzMTI0OTA0MjkyODkyOTUzODMwMTIzMDA2NDgyMzQwMjAyOTUxNTkzMDI0MjIzODIyOTQ0NjYwMzM4MDk0Nzk2ODgzMDU0MjAzODE3MTcwMzgxMDk3MjQxODQxNzM1OTgzOTIyOTU4NjcyMTY1MDg1MzIxMDEwMzAzNjc4MjEyMzkwNzQ2NDA5MDk4MTQzNzQwMzkzNjcyMjQ5ODQ3MTEwMzY5MTY3ODg3NzQ2NjQ2ODEyMDQwMTczMjcwOTY2MTUwNzQ2NDUxODEwODczMDI5Njk4NjIxOBoBMArxBBLpBDEyODM5MDU0MTc4MDc2NzYzMTMzNzA5ODYxODk3MjU0MzE2MTI0MjYyMTE3MTE4MzM2MTc0ODY4NTQzNzk2NTY0Mjc5NTMwNzIzNzE3OTc2Mjk1NTM2NDQ2OTI2MTg0MjM4NzIwODE4MzQ2MjUyNzYyNDUyODMzNTY3MzQ5ODk2NzcwMTYxNzg4Njk2MTMzMDYzNTU0NjU0NjE2ODI0ODI2NDE3MzkzODQzNjQyNDI3NDA3OTMxMjYwMDQzMDA2NTE3ODA4NzQ2MzU0Mzk0MjA1NjE5NDUzMDk4MzA4NTQwMDk4MTM1MTkwNzAwNjc1ODYzMzY3MzM1MTY4MTQwNDA4OTc3MjIyMDc1NzgwMDA1NDU3NjkxOTM2ODM0MjQwOTc4MDMwMjUwOTg3MTU3NDc1Nzg3NjQyNzI1NTgzNjM5OTc4MzkzMzcxMzEwMTY0NDUyOTI3NTI2MzY5NjE4NDg4NTk3NzQ4NDg1Nzg5MDQ4Njc2Mzg0MjAxMjg5NDc1OTM0ODU3NzgwNDM0Nzg2OTM1NTMzMjkyNjcwMjM3MDQ1MTYwNTE0NzgwNjQ1OTQ1ODg0Njg2OTUzMDk4MjI4NTE0NTI1OTE5NzU5NzUyMjI5MDg3NTMxNDI4ODE0NTgyNjA2OTc3NzIzMzI2MTk5Mzg1MzA1OTY4MTAxNTU3MzU2MTgzMDk4NDU3ODI3MzUxNTMyOTIxNDMxOTcyODU5MTYyMjQ1Mzg5NDI2MDc1MTU1ODk3NjM5MjE5OTU1OTk3NjM0NjIyODQ5MTExNDQ0Njk3MjkxNDE4Mzk2MjE0MTI1NzMzODYyNTU2GgMtMTNC3AkKtQIxMjU4MzI0NTExMDczODM1MzQ0MjI0Nzg0OTAyMjM1NjIzNjYwNzY4MzMwNTQzNTQ1OTI4MjgwOTQ0NDczODE3Nzc3NTY2MTE5ODQ5NDUzMzI0MjY0NTg4NzMxMTQzNDE0ODg3NTI1MjM1Mzc0OTU3MDk5NjU3OTQxMDk0OTY4Mjg3MDUwOTg4NzY1MjY2MDE1MjY3NDYxODM0NDQwNTE2MzY2NTYwNDYwOTIwNzc2NjExODUwMjUzODMzNzc1NjY0ODk0MjU5MjI0ODYwMDI4MTYxODc0NTEzOTAwMzc3MzMwNjE4OTQ3MTY5NDEzMDA0ODYyNTMzMTM3NjI4ODU1NTU4MDEyMDczODk2NDYzMTcyMjg5NjE3MDY3Mjc4NTQ4Mzg3NDg3NzUwOTk0NTY3MTQ0NDQStQIxMjU4MzI0NTExMDczODM1MzQ0MjI0Nzg0OTAyMjM1NjIzNjYwNzY4MzMwNTQzNTQ1OTI4MjgwOTQ0NDczODE3Nzc3NTY2MTE5ODQ5NDUzMzI0MjY0NTg4NzMxMTQzNDE0ODg3NTI1MjM1Mzc0OTU3MDk5NjU3OTQxMDk0OTY4Mjg3MDUwOTg4NzY1MjY2MDE1MjY3NDYxODM0NDQwNTE2MzY2NTYwNDYwOTIwNzc2NjExODUwMjUzODMzNzc1NjY0ODk0MjU5MjI0ODYwMDI4MTYxODc0NTEzOTAwMzc3MzMwNjE4OTQ3MTY5NDEzMDA0ODYyNTMzMTM3NjI4ODU1NTU4MDEyMDczODk2NDYzMTcyMjg5NjE3MDY3Mjc4NTQ4Mzg3NDg3NzUwOTk0NTY3MTQ0NDMa6QQxNTgzMzgwNTc1MTY5MjA2NzY3ODM3Nzg5OTMwNDI4NDQwMzc2NjQ4MzU0MzY3MzU1NDA1Njg3MzQyNzkxODIwMDQzMjcxNDc5OTAxNDE5MTY4MjIzMDQ5Mzk0MTkwMjI2NDkxNDUzODgxMTg3MDg2NzkyNDkxNjU0MjE3MDQzOTE2MjQ3MTM2NDQyMTc0MTE3MTgxOTk2MDE1MTMzNTEwMjM2MTQyODM1MzQ4MTY3MDM0MjA1OTI4MTgxMjM3NDQzNTA1Mjg0OTcxMTE4OTM4MzI4MDA0Mjg2MjcxMzQ4NDcwODc4OTQxMzQ2MzY5ODMxMjg2MjQ0MDcxNjQyNTE1ODc2MTg4NDczNDkyNTQzNjY5MDg1MjUyMzkxOTk2MDc3NDEyNDkwMzg0NTMxMDY2MDcwMzg4MzA1OTcwODExNTQwOTg4NTQ5NDE2MDQwMTkwMjY5OTAwMzM5NjgzODUxOTM5NTUxOTI4ODc0MDIzNzEyMDkxNDk1NzcxNzM2NDg4NzEyNDY0MzE1ODcwMzE0MjIwMTYyNjAzMDcwNzQ2NTk4MDQzMzc3NzA5NjM3MTc1MTg4NzQyNDQ0OTc1MzY5OTUwMTIxMzk3ODMyODY4ODc2MDc1Mjc3ODcxMDQxNDkxODYwMTk0MjY2NDE3NDQ0MDE0MzkwMjk5ODE5MjYxMzMxMDQ3MTA0MDU1MTgxNDU5MjEwMTEwMzIzMDI0NTcxNDU0NzczMzU5MjIxODc4MDM5MjYwNzAyMTkzOTkyNTE3NjI4ODAzNTkwNTIyMzQxMDQ2MzA3Mzc1MjY5Nzc5NjQ0NDgwMDI0OQ==||0;1;0,1;;0;1;1;;;||3");
        randomforestMessages[1] = new RandomforestMessage("||0;1;;1;;;1;0;1;1||3");
        randomforestMessages[2] = new RandomforestMessage("||0;2;1;0,3;1,2;0,1;0;1,3;0,3;0,1||3");
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
        RandomForest randomForest = new RandomForest(new RandomForestParameter());
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
        RandomForest randomForest = new RandomForest(new RandomForestParameter());
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
        Assert.assertEquals(((RandomForestReq)res.get(0).getBody()).getBody(), "{\"treeId\":1.0,\"percentile\":50.0,\"featureId\":0.0}||");
        Assert.assertEquals(((RandomForestReq)res.get(1).getBody()).getBody(), "{\"treeId\":0.0,\"percentile\":50.0,\"featureId\":0.0}||");
        Assert.assertEquals(((RandomForestReq)res.get(2).getBody()).getBody(), "");
    }

    @Test
    public void control5(){

        List<ClientInfo> clientInfos = new ArrayList<>();
        clientInfos.add(new ClientInfo("127.0.0.1", 80, "HTTP", 0));
        clientInfos.add(new ClientInfo("127.0.0.1", 81, "HTTP",1));
        clientInfos.add(new ClientInfo("127.0.0.1", 82, "HTTP",2));
        RandomForest randomForest = new RandomForest(new RandomForestParameter());
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
        RandomForest randomForest = new RandomForest(new RandomForestParameter());
        randomForest.setForTest(clientInfos,2,2,2,2,2,2,1);
        boolean res = randomForest.isStop();
        Assert.assertEquals(false, res);
    }

    @Test
    public void isContinue(){
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        RandomForest randomForest = new RandomForest(new RandomForestParameter());
        randomForest.setForTest(clientInfos,2,2,2,2,2,2,1);
        boolean res = randomForest.isContinue();
        Assert.assertEquals(true, res);
    }

    @Test
    public void sendForest(){
        List<ClientInfo> clientInfos = new ArrayList<>();
        clientInfos.add(new ClientInfo("127.0.0.1", 80, "HTTP", 0));
        clientInfos.add(new ClientInfo("127.0.0.1", 81, "HTTP",1));
        clientInfos.add(new ClientInfo("127.0.0.1", 82, "HTTP",2));
        RandomForest randomForest = new RandomForest(new RandomForestParameter());
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
        commonRequest[1] = new CommonRequest(clientInfos.get(0), new RandomforestMessage("{\"numTrees\":\"2\",\"Tree1\":\"{\\\"0\\\":{\\\"referenceJson\\\":\\\"{}\\\",\\\"isLeaf\\\":\\\"0\\\",\\\"nodeId\\\":\\\"0\\\",\\\"party\\\":\\\"{\\\\\\\"ip\\\\\\\":\\\\\\\"127.0.0.1\\\\\\\",\\\\\\\"port\\\\\\\":81,\\\\\\\"path\\\\\\\":null,\\\\\\\"protocol\\\\\\\":\\\\\\\"HTTP\\\\\\\",\\\\\\\"uniqueId\\\\\\\":1}\\\"}}\",\"Tree0\":\"{}\"}"), 0);
        commonRequest[2] = new CommonRequest(clientInfos.get(0), new RandomforestMessage("{\"numTrees\":\"2\",\"Tree1\":\"{}\",\"Tree0\":\"{}\"}"), 0);
        for (int i = 0; i < 3; i++) {
            Assert.assertEquals(((RandomforestMessage)commonRequest[i].getBody()).getResponseStr(), ((RandomforestMessage)res.get(i).getBody()).getResponseStr());
        }
    }
    @Test
    public void printMetricMap(){
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        RandomForest randomForest = new RandomForest(new RandomForestParameter());
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
        RandomForest randomForest = new RandomForest(new RandomForestParameter());
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
        RandomForest randomForest = new RandomForest(new RandomForestParameter());

        List<CommonResponse> responses = new ArrayList<>();
        int[] predictUid = new int[0];
        String[] UidList = new String[2];
        UidList[0] = "1";
        UidList[1] = "2";
        InferenceInitRes inferenceInitRes = new InferenceInitRes(false, predictUid);
        for (int i = 0; i < 3; i++) {
            responses.add(new CommonResponse(clientInfos.get(0),inferenceInitRes));
        }
        String trainId = "181_RandomForest_Binary";
        randomForest.setForTest(clientInfos, UidList, null, null, null, null);
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
        RandomForest randomForest = new RandomForest(new RandomForestParameter());
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

        List<CommonRequest> res = randomForest.inferencePhase2(trainId, responses);
        List<CommonRequest> target = randomForest.createNullRequest(responses, -255);

        for (int i = 0; i < 3; i++) {
            Assert.assertEquals(res.get(i).getClient(), target.get(i).getClient());
            Assert.assertEquals((res.get(i).getBody()), (target.get(i).getBody()));
            Assert.assertEquals((res.get(i).getPhase()), (target.get(i).getPhase()));
        }
    }

    @Test
    public void isInferenceContinue() {
        RandomForest randomForest = new RandomForest(new RandomForestParameter());
        boolean res = randomForest.isInferenceContinue();
        Assert.assertEquals(res, true);
    }

    @Test
    public void testPostInferenceControl() {
        List<ClientInfo> clientInfos = new ArrayList<>();
        clientInfos.add(new ClientInfo("127.0.0.1", 80, "HTTP", 0));
        clientInfos.add(new ClientInfo("127.0.0.1", 81, "HTTP",1));
        clientInfos.add(new ClientInfo("127.0.0.1", 82, "HTTP",2));
        RandomForest randomForest = new RandomForest(new RandomForestParameter());
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
        RandomForest randomForest = new RandomForest(new RandomForestParameter());
        Assert.assertEquals(randomForest.getAlgorithmType().getAlgorithm(),"RandomForest");
    }

    @Test
    public void testmetric() {
        RandomForest randomForest = new RandomForest(new RandomForestParameter());
        Assert.assertEquals(randomForest.readMetrics().getMetrics(),new HashMap<>());
    }

    @Test
    public void testmetricArr() {
        RandomForest randomForest = new RandomForest(new RandomForestParameter());
        Assert.assertEquals(randomForest.metricArr(),new HashMap<>());
    }

    @Test
    public void testgetFeatureImportance() {
        RandomForest randomForest = new RandomForest(new RandomForestParameter());
        Assert.assertEquals(randomForest.getFeatureImportance(),new ArrayList<>());
    }

}
