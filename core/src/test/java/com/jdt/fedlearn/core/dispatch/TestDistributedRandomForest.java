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
import com.jdt.fedlearn.core.psi.MappingOutput;
import com.jdt.fedlearn.core.psi.MappingResult;
import com.jdt.fedlearn.core.psi.MatchResult;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestDistributedRandomForest {
    @Test
    public void getNextPhase(){
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        DistributedRandomForest randomForest = new DistributedRandomForest(new RandomForestParameter());
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
        MatchResult matchResult = new MatchResult();
        DistributedRandomForest randomForest = new DistributedRandomForest(new RandomForestParameter());
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
    public void control1FromInit(){
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        DistributedRandomForest randomForest = new DistributedRandomForest(new RandomForestParameter());
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
        DistributedRandomForest randomForest = new DistributedRandomForest(new RandomForestParameter());
        randomForest.setForTest(clientInfos,2,2,3,3,2,2,1);
        String trainId = "181_RandomForest_Binary";

        RandomforestMessage[] randomforestMessages = new RandomforestMessage[3];
        randomforestMessages[0] = new RandomforestMessage("IgwSCioIcGFpbGxpZXIy1w4K8AQS6AQ3MTA1MDk2NTcyNTgyMTc4MTk0MjQ3NzAzMTQzNjM0OTUyNDMwNTMwMjgxNjY3MTMyNTMwMTMzMDc5OTM0MzQ1Njk2OTc0NjgyMDM2NzcwOTQ3MDEyNDEzNTI4NTAxMTc4NTU3Mjk5OTMyODgzNTY5OTE3NTQ1NzY2OTA3MDU3OTUwNzYyNjI5MTA4Mzk1NDIwMjA1NTEwMDU3Njk3NTE2NzI5OTgwNTcxMzMzMjI4MjE5MjAwMzIxMTI1NzA5Mzk5NTI0NDc0MzczNzEyNTg3NTUzNjQyNTg5OTU1Mjk2Njc0MjU0NTcwNzA2MjkyNDk0NDY1NDE5OTEzMzA0ODQxMjgwNzA5Mzk3ODM1MTI3MTQzNjk4NTEyNDY0MTcwMjMyMzE5Mjg4MDUzMjI0MDAzNDgyNzcxNTA2OTEwMjc4NDk3OTIxNzU1MzI4NDc0NTc4MDg3NjEwNTY2ODE1MDY2NTI1NTgzMTEyMjY4NTE5NTI5MjYxNTk3NzE4MzAzMjg5NDAyMDk4MzI4MDg4MTA0MTYwMjEzODg1NjQ2MjU2NDkxOTI5OTc1NzE4MDcwNDUwMjAxNjkzNjAyNzYzMDI2Mzk5NTE1MTQxNjA1NzE0NDgxNzA3MDU3MDQwOTQ3NzAyOTU0MDc2NDU4MDI3MTAzMzgwMTgzNzIyNDczMDY4MTc4NzU5OTM3ODMwOTI4NzA3OTg0NzgyMDcyODExNDAzNjAwMjE0MTQ2OTYzMjUzNTE5NzEwMjk4Nzc5OTc4MzQzOTcwNTM0MjU0NTg4MTAyOTAzMTUxNTQ1MDA0MDYwOTcwNzk0NTE0GgMtMTMK7gQS6AQ3MzkyMzM1MzI3OTEyMjQwMTk3MTkyMDY3MDEyODAwMjcyOTI0NzE4MTcxODU5NzEzMDk1MzEzODE0MjYxNjExNjUyODEzOTk2NzMwNDQyMzkzNTI5Mzc1NDQyMTQ4NjA3OTE4NzA5MzU0MjY2NDU5NjIxODExNzU1MjY1NjU4NzAyODQzMjE2ODY5OTI4OTYxMDcxOTYwMzQyODE4MzY4NTUzNzU3ODYwNDgwNDgxNTEyNTAwNzM0MTIxMjAyMDM0NDgzNzE5NTMxNTkzOTc3OTg1Njk5MTY3NDU2MDYxOTA2NzYxNzYzNTEzNzE4NTEyNTE4ODgzNzE2NzM5MTMxMTg4MjMwMDU2NjM5NDY4ODExMTk1NTY5ODUwNzY0NTAzMjE4MjYwMDM2ODU0MjIyODI5MzY5NzY2NjEwOTM2MDA0NzE4OTQ4NDU2ODk0MTY5MDA2Njk2NjA1MTQzNjk5NzUwMjIxOTc4NzgzNjk5NTQ1MjAwMTcxMTU3MDEyMjA3ODEyNTA3MTI0ODYwMTc4NDUzNzk4OTk0MjExNjc3NTE3Mjg1ODAxNDcxOTI3MDcwNTIzMzUzNDQ5Nzc4NzA1NzY3OTEwNTkyNzk3MDc5MjE1MDk5NzU3NjIwODIyNTI4NTkxMTQ2NTg4MTE5NjIxNDA3ODQ0MzQ0MTAxNjA2NDM1NTIyMDkxNTExNDgxMjY4NTM3OTY5OTcyMTE5NjExODg3MDk4NzIxOTY5MjY3MDg5NjE3MDMzODU3MDcyOTQxNDExNzA2Nzk0MTQ3MTMyMjQ4ODE1ODIzNjMyMTAwMzc2MzQ0MzE3GgEwCvAEEugEMzk5MzY1ODQ2MDk2MjQ2Njg0MDAzODUyMDQ5MTQxMDkxMDY0NDYyMTcwNjU4OTIxMTIyNTAxNTExNjg1NzQ0MTExNjE2NjQ1NTE2ODIxNzQ4NjE4ODU3Mzc3MzY3Nzg0OTg0MzEzMzM0NzgyMzkyMzgzNzQ1ODIxMjE2MTIyNjczMjA5ODk5OTYzNTc0ODc2MTE0MjU4NTQ5MDE4ODQ0NDI2NDk1NDY5NTMwOTU3MzQwMjEzNjY2NjM1OTE2NzUxOTc3MDUxMjkxNjgwMDI1MzI0NTExNzEyMjUzOTY3MzI5NDI4OTE0ODMzMjQyMTUwMzQ5MzA4OTYyNTE4NzczNjgwMzg5NDA1OTI4MDgzMDAzNTI1MjYyMDg3NzEyOTg2NTE1MTY3OTM5NDMyMzU5MTEwMzkwODMwOTYwNDE0Mzg0OTI0NDQxNTMwMDQ1MDQ1NDE4MjI3ODgxMTA1OTI2NjUzMjk1NTE2OTk2Mzc4OTExMDQ2MjYxNDcwODg1NDI5NDg2MDQzOTQyMzU4NjQ0MjQyMzI3NDI4MTUxMDg0NDgwNTkxNTk5NTgzNjcwMDMyMTc4MDk2NTM2MDk5MTc5MTc0OTkwOTA4MDA5MDk0Njc0ODYyMDM5NjEzNTgwNTEyODQwNDU1NDk0NjA0NDc5NTQwNzA4ODc3MDU3NDM1NjUxMDc0OTk5NDU2NTQwMDY0OTgzMTc2MzEwODUyNjI0MTM5NTUzMjI2OTIxOTc1NTIzODU3NjIzMzMzMjMzNDMyMDYyODU2MDUwNTM0NDE0MTE3MjU1MDUwMjE1MDg4MzgwNzg1NjU4NxoDLTEzQtwJCrUCMTA5MzYzMjA3NTU3NjMwMDEwMjQzMTE4NTMyODg2MTcxMDkyNTE3MjMwOTEwNjMxODg2MTA3MjM4NTcyNTYxNTIwMDQ3OTI3MDMzNTk3ODcyNzQ1MjMzNDc3NTQ2MTM4MDMxNzA3NTkwNDgzODIyMzA5ODEzMDM2MzQ0NTgzNzMwMTg2ODc5ODc4OTE4NTc2MjQwODU2MzUxNTY3Mjc2ODQxOTcyNTg1Mjg0Mjk4NDU1Njg2NjUwODA1OTQwMDQ3OTE3OTAzNDU4NjIwODAzMjI3MzY0MjEwMTY4NzkzODYxODU4NzU0OTc4NDAyMTIyMTk1NTYzODA0ODU4ODgxODE3NDYxMDU3NDQxMzY1Mjk3OTI0MjIwODAyMTU3MzY4NzkzMDUzMTkxOTUyMzM3NDY0Nzc0ErUCMTA5MzYzMjA3NTU3NjMwMDEwMjQzMTE4NTMyODg2MTcxMDkyNTE3MjMwOTEwNjMxODg2MTA3MjM4NTcyNTYxNTIwMDQ3OTI3MDMzNTk3ODcyNzQ1MjMzNDc3NTQ2MTM4MDMxNzA3NTkwNDgzODIyMzA5ODEzMDM2MzQ0NTgzNzMwMTg2ODc5ODc4OTE4NTc2MjQwODU2MzUxNTY3Mjc2ODQxOTcyNTg1Mjg0Mjk4NDU1Njg2NjUwODA1OTQwMDQ3OTE3OTAzNDU4NjIwODAzMjI3MzY0MjEwMTY4NzkzODYxODU4NzU0OTc4NDAyMTIyMTk1NTYzODA0ODU4ODgxODE3NDYxMDU3NDQxMzY1Mjk3OTI0MjIwODAyMTU3MzY4NzkzMDUzMTkxOTUyMzM3NDY0NzczGukEMTE5NjAzMTExNjcyOTMyNjE3OTAyMTE4MTMxOTg4NzM3NTMxNjQ4NjQ2MDE2ODkzNzk5NzMwNDExOTc4NjQwNjU1NjQzNjM1MzU3MTg3Mjk4NDgwNjk2OTUxOTkyNTA3OTQ3OTQwOTAwMjQ5NjI2OTM5OTE4MzM5NjQwNzI2NDM5MTIwODMyNDY3NzYwMTMxODI0OTYzNDU3NTQzNTA3NDAyNzU2NzEwMDc2NTg5MDIxOTY3NzgwNDk5NDI3MjQyNDUzMjc2ODEwNjMyNzU2OTIyODc0NjQyNDg1ODU2MzUxNDU3NTY5MzcwODE2MzU4MjIwNjIxMTMwMDQ4Mzk3NDIxMjMyNzc0MDYyNDgzMzg4MTEyODc1ODY4NjY1NTU3OTgyMjYzMzI3MDgwOTMxODY2NjQ4ODMxNjU3OTg2MzE0NDU2NjE1NTcxMTk1NzM5MzUwNTU1NjkyMjQyODQ5NTc2OTI0NjA5OTM5NzUxMzg3Njg4NjM1MzkxNDcwMDQ5OTg1NDI3MTg2OTQxODkxNjk3Mzg5NjgwMjY3MDU2MjA4NjM0NTQ2MTY5MzgxMDU2MDU4NjEzOTExMDAxNDU1MDkwMTA1NDkwOTM2NDEyMzc4MDY1MDY4MjQ0MTQzMzYyNTE1NTgyNzU0MjcwMjg4NjU4MDc2MDAyMDk2MjcxNjUyMzkxNzU5MTUzNTA5ODQ2NTQxMDAzNDI0Njc0MTk0NzE2Nzc3NDY2ODk2MjAwNzA2MDgwMzIxMTEyMTYyNDk5Mzg0NTE5NzM3NjU5OTU4MDc1MjkxMDIxMjgwOTYyNjUwMTU5NDE1Mjk=||0;1;0;0;0;0;1;1;0;0||3");
        randomforestMessages[1] = new RandomforestMessage("||0;1;0;0;0;0;1;1;0;0||3");
        randomforestMessages[2] = new RandomforestMessage("||0;3;3;3;3;1;0;3;3;2||3");
        for (int i = 0; i < 3; i++) {
            responses.add(new CommonResponse(clientInfos.get(i),randomforestMessages[i]));
        }
        List<CommonRequest> res = randomForest.controlPhase2(responses);
        for (int i = 0; i < 3; i++) {
            Assert.assertEquals(res.get(i).getClient(),clientInfos.get(i));
            Assert.assertEquals(((DistributedRandomForestReq)res.get(i).getBody()).getExtraInfo(), "0|1||[0, 1]|[0, 1]");
        }
    }

    @Test
    public void control3(){
        List<ClientInfo> clientInfos = new ArrayList<>();
        clientInfos.add(new ClientInfo("127.0.0.1", 80, "HTTP", 0));
        clientInfos.add(new ClientInfo("127.0.0.1", 81, "HTTP",1));
        clientInfos.add(new ClientInfo("127.0.0.1", 82, "HTTP",2));
        DistributedRandomForest randomForest = new DistributedRandomForest(new RandomForestParameter());
        randomForest.setForTest(clientInfos,2,2,2,2,2,3,1);
        String trainId = "181_RandomForest_Binary";
        List<CommonResponse> responses = new ArrayList<>();

        DistributedRandomForestRes[] response = new DistributedRandomForestRes[3];
        response[0] = new DistributedRandomForestRes(clientInfos.get(0),
                "",
                true, null, -1,"0|1||[0, 1, 2]|[0, 1, 2]");
        response[1] = new DistributedRandomForestRes(clientInfos.get(1), "", false);
        response[2] = new DistributedRandomForestRes(clientInfos.get(2), "", false);
        for (int i = 0; i < 3; i++) {
            responses.add(new CommonResponse(clientInfos.get(i),response[i]));
        }
        List<CommonRequest> res = randomForest.controlPhase3(responses);

        Assert.assertEquals(((DistributedRandomForestReq)res.get(0).getBody()).getBody(),"{\"client\":{\"ip\":\"127.0.0.1\",\"port\":80,\"path\":null,\"protocol\":\"HTTP\",\"uniqueId\":0},\"isActive\":true,\"body\":\"\",\"sampleId\":null,\"treeId\":-1,\"extraInfo\":\"0|1||[0, 1, 2]|[0, 1, 2]\",\"active\":true}|||{\"client\":{\"ip\":\"127.0.0.1\",\"port\":81,\"path\":null,\"protocol\":\"HTTP\",\"uniqueId\":1},\"isActive\":false,\"body\":\"\",\"sampleId\":null,\"treeId\":0,\"extraInfo\":\"\",\"active\":false}|||{\"client\":{\"ip\":\"127.0.0.1\",\"port\":82,\"path\":null,\"protocol\":\"HTTP\",\"uniqueId\":2},\"isActive\":false,\"body\":\"\",\"sampleId\":null,\"treeId\":0,\"extraInfo\":\"\",\"active\":false}|||");

    }

    @Test
    public void control4(){
        List<ClientInfo> clientInfos = new ArrayList<>();
        clientInfos.add(new ClientInfo("127.0.0.1", 80, "HTTP", 0));
        clientInfos.add(new ClientInfo("127.0.0.1", 81, "HTTP",1));
        clientInfos.add(new ClientInfo("127.0.0.1", 82, "HTTP",2));
        DistributedRandomForest randomForest = new DistributedRandomForest(new RandomForestParameter());
        randomForest.setForTest(clientInfos,2,2,2,2,2,3,1);
        String trainId = "181_RandomForest_Binary";
        List<CommonResponse> responses = new ArrayList<>();


        DistributedRandomForestRes[] response = new DistributedRandomForestRes[3];

        response[0] = new DistributedRandomForestRes(clientInfos.get(0),"CjIaIAAAAAAAAPA/AAAAAAAAAAAAAAAAAAAAANPP9fn///M/Kg57ImlzX2xlYWYiOiAwfQoyGiAAAAAAAADwPwAAAAAAAAAAAAAAAAAAAADTz/X5///zPyoOeyJpc19sZWFmIjogMH0=", true,
                null, -1, "0|1||[0, 1, 2]|[0, 1, 2]");
        response[1] = new DistributedRandomForestRes(clientInfos.get(1),"", false,
                null, -1, "");
        response[2] = new DistributedRandomForestRes(clientInfos.get(2),"", false,
                null, -1, "");

        for (int i = 0; i < 3; i++) {
            responses.add(new CommonResponse(clientInfos.get(i),response[i]));
        }
        List<CommonRequest> res = randomForest.controlPhase4(responses);
        Assert.assertEquals(((DistributedRandomForestReq)res.get(0).getBody()).getBody(), "");
        Assert.assertEquals(((DistributedRandomForestReq)res.get(1).getBody()).getBody(), "{\"treeId\":0.0,\"percentile\":50.0,\"nodeId\":0.0,\"featureId\":0.0}||{\"treeId\":1.0,\"percentile\":50.0,\"nodeId\":0.0,\"featureId\":0.0}||");
        Assert.assertEquals(((DistributedRandomForestReq)res.get(2).getBody()).getBody(), "");
    }

    @Test
    public void control5(){

        List<ClientInfo> clientInfos = new ArrayList<>();
        clientInfos.add(new ClientInfo("127.0.0.1", 80, "HTTP", 0));
        clientInfos.add(new ClientInfo("127.0.0.1", 81, "HTTP",1));
        clientInfos.add(new ClientInfo("127.0.0.1", 82, "HTTP",2));
        DistributedRandomForest randomForest = new DistributedRandomForest(new RandomForestParameter());
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

        DistributedRandomForestRes[] response = new DistributedRandomForestRes[3];
        response[0] = new DistributedRandomForestRes(clientInfos.get(0),
                "",
                true, null, -1, "1||[0, 1, 2]");
        response[1] = new DistributedRandomForestRes(clientInfos.get(1),
                "Cp8BChQKEgoQR4hdhf7/PEDpMlsr1HVxPgooChIKEH3rzbL/f0FA6iAcoALbYj4KEgoQReYz0W9dp76XXfoT38eSvhIaChgAAAAAAAAAAAAAAAAAAPA/AAAAAAAAAAAqQXsiaXNfbGVhZiI6IDAsICJmZWF0dXJlX29wdCI6IDAsICJ2YWx1ZV9vcHQiOiAyOC45OTk5Nzc0MzE2MTY4MjR9Cp8BChQKEgoQ7/hdTv//PEAK+IsTOlivPgooChIKEKjtgdMBgEFAD/gWyNkerr4KEgoQbQzAsVyhtL6IGINfuMaavhIaChgAAAAAAAAAAAAAAAAAAPA/AAAAAAAAAAAqQXsiaXNfbGVhZiI6IDAsICJmZWF0dXJlX29wdCI6IDAsICJ2YWx1ZV9vcHQiOiAyOC45OTk5ODk0MTIyNTI4ODR9",
                false, null, -1, "1||[0, 1, 2]");
        response[2] = new DistributedRandomForestRes(clientInfos.get(2), "", false, null, -1, "1||[0, 1, 2]");
        for (int i = 0; i < 3; i++) {
            responses.add(new CommonResponse(clientInfos.get(i),response[i]));
        }

        List<CommonRequest> res = randomForest.controlPhase5(responses);
        Assert.assertEquals(res.size(), 3);
    }

    @Test
    public void isStop(){
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        DistributedRandomForest randomForest = new DistributedRandomForest(new RandomForestParameter());
        randomForest.setForTest(clientInfos,2,2,2,2,2,2,1);
        boolean res = randomForest.isStop();
        Assert.assertEquals(false, res);
    }

    @Test
    public void isContinue(){
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        DistributedRandomForest randomForest = new DistributedRandomForest(new RandomForestParameter());
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
        DistributedRandomForest randomForest = new DistributedRandomForest(new RandomForestParameter());
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
        DistributedRandomForest randomForest = new DistributedRandomForest(new RandomForestParameter());
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
        DistributedRandomForest randomForest = new DistributedRandomForest(new RandomForestParameter());
        randomForest.setForTest(clientInfos,2,2,2,2,2,2,1);
        String[] predictUid = new String[2];
        predictUid[0] = "1";
        predictUid[1] = "2";
        List<CommonRequest> res = randomForest.initInference(clientInfos, predictUid);
        CommonRequest[] commonRequest = new CommonRequest[3];
        commonRequest[0] = new CommonRequest(clientInfos.get(0) , new InferenceInit(predictUid),0);
        commonRequest[1] = new CommonRequest(clientInfos.get(1), new InferenceInit(predictUid),0);
        commonRequest[2] = new CommonRequest(clientInfos.get(2),  new InferenceInit(predictUid),0);
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
        DistributedRandomForest randomForest = new DistributedRandomForest(new RandomForestParameter());

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
        DistributedRandomForest randomForest = new DistributedRandomForest(new RandomForestParameter());
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
        DistributedRandomForest randomForest = new DistributedRandomForest(new RandomForestParameter());
        boolean res = randomForest.isInferenceContinue();
        Assert.assertEquals(res, true);
    }

    @Test
    public void testPostInferenceControl() {
        List<ClientInfo> clientInfos = new ArrayList<>();
        clientInfos.add(new ClientInfo("127.0.0.1", 80, "HTTP", 0));
        clientInfos.add(new ClientInfo("127.0.0.1", 81, "HTTP",1));
        clientInfos.add(new ClientInfo("127.0.0.1", 82, "HTTP",2));
        DistributedRandomForest randomForest = new DistributedRandomForest(new RandomForestParameter());
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
        DistributedRandomForest randomForest = new DistributedRandomForest(new RandomForestParameter());
        Assert.assertEquals(randomForest.getAlgorithmType().getAlgorithm(),"DistributedRandomForest");
    }

    @Test
    public void testmetric() {
        DistributedRandomForest randomForest = new DistributedRandomForest(new RandomForestParameter());
        Assert.assertEquals(randomForest.readMetrics().getMetrics(), new HashMap<>());
    }

    @Test
    public void testmetricArr() {
        DistributedRandomForest randomForest = new DistributedRandomForest(new RandomForestParameter());
        Assert.assertEquals(randomForest.metricArr(),new HashMap<>());
    }

}
