package com.jdt.fedlearn.core.dispatch;

import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.core.entity.base.SingleElement;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.core.entity.common.CommonResponse;
import com.jdt.fedlearn.core.entity.common.TrainInit;
import com.jdt.fedlearn.common.entity.core.feature.Features;
import com.jdt.fedlearn.core.entity.verticalLinearRegression.LinearP1Response;
import com.jdt.fedlearn.core.entity.verticalLinearRegression.LinearP2Request;
import com.jdt.fedlearn.core.fake.StructureGenerate;
import com.jdt.fedlearn.core.parameter.VerticalLRParameter;
import com.jdt.fedlearn.core.psi.MatchResult;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestVerticalLogisticRegression {
    List<ClientInfo> clientInfos = StructureGenerate.threeClients();

    @Test
    public void getNextPhase() {
        VerticalLogisticRegression verticalLR = new VerticalLogisticRegression(new VerticalLRParameter());
        int p = verticalLR.getNextPhase(1);
        Assert.assertEquals(p, 2);
    }

    @Test
    public void initControl() {
        Map<ClientInfo, Features> features = StructureGenerate.fgbFeatures(clientInfos);
        MatchResult matchResult = new MatchResult(10);
        VerticalLogisticRegression verticalLR = new VerticalLogisticRegression(new VerticalLRParameter());
        Map<String, Object> other = new HashMap<>();
        other.put("pubKey", "mockPubKey");

        List<CommonRequest> requests = verticalLR.initControl(clientInfos, matchResult, features, other);
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
        VerticalLogisticRegression verticalLR = new VerticalLogisticRegression(new VerticalLRParameter());
        List<CommonResponse> responses = new ArrayList<>();
        responses.add(new CommonResponse(clientInfos.get(0), new SingleElement("init_success")));
        responses.add(new CommonResponse(clientInfos.get(1), new SingleElement("init_success")));
        responses.add(new CommonResponse(clientInfos.get(2), new SingleElement("init_success")));
        List<CommonRequest> requests = verticalLR.control(responses);
        //TODO
        Assert.assertEquals(requests.size(), 3);
    }

    @Test
    public void controlPhase2() {
        int phase = 2;
        String[][] mockU = new String[2][];
        mockU[0] = new String[]{"1"};
        mockU[1] = new String[]{"2"};
        List<Message> msg = new ArrayList<>();
        ClientInfo clientInfo = new ClientInfo();
        msg.add(new LinearP1Response(clientInfo, mockU, "3"));
        msg.add(new LinearP1Response(clientInfo, mockU, "4"));
        msg.add(new LinearP1Response(clientInfo, mockU, "5"));
        List<CommonResponse> response = packageThreeMsgsIntoCommonResponses(msg);

        VerticalLogisticRegression verticalLR = new VerticalLogisticRegression(new VerticalLRParameter());
        verticalLR.setPhase(phase - 1);
        List<CommonRequest> req = verticalLR.control(response);
        List<LinearP1Response> oneReq = ((LinearP2Request) req.get(0).getBody()).getBodies();
        Assert.assertEquals(oneReq.get(0).getU()[0][0], "1");
        Assert.assertEquals(oneReq.get(0).getLoss(), "3");
        Assert.assertEquals(oneReq.size(), 3);
    }

    private List<CommonResponse> packageThreeMsgsIntoCommonResponses(List<Message> in) {
        List<CommonResponse> out = new ArrayList<>();
        out.add(new CommonResponse(clientInfos.get(0), in.get(0)));
        out.add(new CommonResponse(clientInfos.get(1), in.get(1)));
        out.add(new CommonResponse(clientInfos.get(2), in.get(2)));
        return out;
    }
}
