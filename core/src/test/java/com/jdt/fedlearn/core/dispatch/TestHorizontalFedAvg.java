package com.jdt.fedlearn.core.dispatch;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.base.SingleElement;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.core.entity.common.CommonResponse;
import com.jdt.fedlearn.core.entity.common.InferenceInit;
import com.jdt.fedlearn.core.entity.common.TrainInit;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.fake.StructureGenerate;
import com.jdt.fedlearn.core.parameter.HorizontalFedAvgPara;
import com.jdt.fedlearn.core.psi.MatchResult;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestHorizontalFedAvg {
    @Test
    public void getNextPhase(){
        Map<Integer,Integer> inOutPair = new HashMap<>();
        inOutPair.put(0,1);
        inOutPair.put(1,2);
        inOutPair.put(2,3);
        inOutPair.put(3,4);
        inOutPair.put(4,5);
//        inOutPair.put(5,1);
        HorizontalFedAvg fedAvg = new HorizontalFedAvg(new HorizontalFedAvgPara());
        for (Map.Entry<Integer, Integer> entry:inOutPair.entrySet()){
            int p = fedAvg.getNextPhase(entry.getKey(), new ArrayList<>());
            Assert.assertEquals(p, entry.getValue().intValue());
        }
    }

    @Test
    public void initControl(){
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        MatchResult matchResult = new MatchResult(10);
        Map<ClientInfo, Features> features = StructureGenerate.fgbFeatures(clientInfos);

        HorizontalFedAvg fedAvg = new HorizontalFedAvg(new HorizontalFedAvgPara());

        List<CommonRequest> requests = fedAvg.initControl(clientInfos, matchResult, features, new HashMap<>());
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
        HorizontalFedAvg fedAvg = new HorizontalFedAvg(new HorizontalFedAvgPara());
        List<CommonResponse> responses = new ArrayList<>();
        responses.add(new CommonResponse(clientInfos.get(0), new SingleElement("init_success")));
        responses.add(new CommonResponse(clientInfos.get(1), new SingleElement("init_success")));
        responses.add(new CommonResponse(clientInfos.get(2), new SingleElement("init_success")));
        List<CommonRequest> requests = fedAvg.control(responses);

        Assert.assertEquals(requests.size(), 3);
        //TODO add more assert
    }

    @Test
    public void inferenceInit(){
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        HorizontalFedAvg fedAvg = new HorizontalFedAvg(new HorizontalFedAvgPara());
        String[] predictUid = new String[]{"1", "2"};
        List<CommonRequest> requests = fedAvg.initInference(clientInfos, predictUid,new HashMap<>());

        Assert.assertEquals(requests.size(), 3);
        CommonRequest request = requests.stream().findAny().get();
        Assert.assertEquals(((InferenceInit)request.getBody()).getUid(), predictUid);
    }

}
