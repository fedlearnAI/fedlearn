package com.jdt.fedlearn.coordinator.network;

import com.jdt.fedlearn.common.util.HttpClientUtil;
import com.jdt.fedlearn.common.util.JsonUtil;
import com.jdt.fedlearn.coordinator.entity.common.Response;
import com.jdt.fedlearn.coordinator.type.RunningType;
import com.jdt.fedlearn.coordinator.util.ConfigUtil;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.base.SingleElement;
import com.jdt.fedlearn.core.entity.boost.BoostN1Res;
import com.jdt.fedlearn.core.entity.boost.BoostP1Req;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.core.entity.common.CommonResponse;
import com.jdt.fedlearn.core.entity.serialize.JavaSerializer;
import com.jdt.fedlearn.core.type.AlgorithmType;
import mockit.Mock;
import mockit.MockUp;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SendAndRecvTest {
    ClientInfo C1 = new ClientInfo("1.0.0.2", 8050, "http");
    String modelToken = "1-FederatedGB-10000";
    String inferenceId = "1-FederatedGB-Inf001";
    JavaSerializer serializer = new JavaSerializer();

    @BeforeClass
    public void init() {
        MockQueryAndFetch();
        mockConfigInit();
        MockPost();
    }

    @Test
    public void testSend1() {
        MockPostData(0);
        Message data = new BoostP1Req(C1, true);
        String res = SendAndRecv.send(C1, modelToken, 0, AlgorithmType.FederatedGB,
                data, RunningType.COMPLETE, true, "1");
        Assert.assertEquals(res, "init_success");
        String res2 = SendAndRecv.send(C1, modelToken, 0, AlgorithmType.FederatedGB,
                data, RunningType.COMPLETE, false, "1");
        Assert.assertEquals(res2, "init_success");

    }

    @Test
    public void testSend2() {
        MockPostData(0);
        Message data = new BoostP1Req(C1, true);
        String res = SendAndRecv.send(C1, modelToken, 0, AlgorithmType.FederatedGB,
                data, RunningType.COMPLETE, true, "1", "dataset.csv");
        Assert.assertEquals(res, "init_success");
        String res2 = SendAndRecv.send(C1, modelToken, 0, AlgorithmType.FederatedGB,
                data, RunningType.COMPLETE, false, "1", "dataset.csv");
        Assert.assertEquals(res2, "init_success");

    }

    @Test
    public void testSend3() {
        String matchToken = "1-MD5";
        MockPostData(-1);
        Message data = new BoostP1Req(C1, true);
        String send = SendAndRecv.send(C1, matchToken, "dataset.csv", 0, "MD5", data);
        Assert.assertEquals(send, "init_success");


    }

    @Test
    public void testSend4() {
        //send(ClientInfo Client, String path, String httpType, Map<String, Object> context)
        // httpType: get, POST
        MockPostData(0);
        Map<String, Object> map = new HashMap<>();
        map.put("code", 0);
        String get1 = SendAndRecv.send(C1, "", "get", map);
        Assert.assertEquals(get1, "getData");
        String post1 = SendAndRecv.send(C1, "", "POST", map);
        System.out.println(post1);
        Assert.assertEquals(post1, "{\"code\":0,\"data\":\"init_success\"}");

    }

    @Test
    public void testReceivePacket() throws IOException {
        Map<String, Object> map = new HashMap<>();
        map.put("dataSize", 1);
        map.put("msgId", "1");
        String asynRet = JsonUtil.object2json(map);
        // 没有exception
        MockPostData(0);
        String res = SendAndRecv.receivePacket(asynRet, new ClientInfo("1.0.0.4", 8020, "http"));
        Assert.assertEquals(res, "init_success");
        // exception1
        MockPostData(1);
        String res1 = SendAndRecv.receivePacket(asynRet, new ClientInfo("1.0.0.4", 8020, "http"));
        Assert.assertEquals(res1, "");
        // exception2
        MockPostData(2);
        String res2 = SendAndRecv.receivePacket(asynRet, new ClientInfo("1.0.0.4", 8020, "http"));
        Assert.assertEquals(res2, "");

    }

    @Test
    public void TestBroadCastTrain() {
        // 无需传输数据
        MockPostData(0);
        Message data = new BoostP1Req(C1, true);
        CommonRequest request1 = new CommonRequest(C1, data);
        List<CommonRequest> requests = new ArrayList<>();
        requests.add(request1);
        List<CommonResponse> responses = SendAndRecv.broadcastTrain(requests, modelToken, AlgorithmType.FederatedGB,
                RunningType.RUNNING, "1");
        Assert.assertEquals(responses.size(), 1);
        Assert.assertEquals(((SingleElement)responses.get(0).getBody()).getElement(), "init_success");

    }

    @Test
    public void TestBroadCastTrain2() {
        // 有数据传输的broadcastTrain，但是本单元测试为了方便，并未模拟数据传输
        MockPostData(0);
        Message data = new BoostP1Req(C1, true);
        CommonRequest request1 = new CommonRequest(C1, data);
        List<CommonRequest> requests = new ArrayList<>();
        requests.add(request1);
        List<String> dataset = new ArrayList<>();
        dataset.add("dataset1.csv");
        List<CommonResponse> responses = SendAndRecv.broadcastTrain(requests, modelToken, AlgorithmType.FederatedGB,
                RunningType.RUNNING, "1", dataset);
        Assert.assertEquals(responses.size(), 1);
        Assert.assertEquals(((SingleElement)responses.get(0).getBody()).getElement(), "init_success");

    }

    @Test
    public void testSendInference() throws IOException {
        // 需要mock OkHttpUtil.post: result结果应为Response
        // mock传输phase1 - inference
        BoostN1Res data = new BoostN1Res();
        String s = SendAndRecv.sendInference(C1, modelToken, 0, AlgorithmType.FederatedGB, data, inferenceId);
        BoostN1Res res = (BoostN1Res)serializer.deserialize(s);
        Assert.assertNull(res.getTrees());
        Assert.assertNull(res.getMultiClassUniqueLabelList());
        Assert.assertEquals(res.getFirstRoundPred(), 0.0);
    }

    @Test
    public void testBroadCastInference() {
        List<CommonRequest> intiRequests = new ArrayList<>();
        BoostN1Res data = new BoostN1Res();
//        Message data = new InferenceInitRes(true, new int[]{0,1,2});
        CommonRequest commonRequest = new CommonRequest(C1, data);
        intiRequests.add(commonRequest);
        List<CommonResponse> responses = SendAndRecv.broadcastInference(intiRequests, modelToken, AlgorithmType.FederatedGB, inferenceId);
        Assert.assertEquals(responses.size(), 1);
        Assert.assertEquals(((BoostN1Res)responses.get(0).getBody()).getFirstRoundPred(), 0.0);

    }

    @Test
    public void testSendValidate() throws IOException {
        // why区分validate和inference？
        List<CommonRequest> intiRequests = new ArrayList<>();
        BoostN1Res data = new BoostN1Res();
//        Message data = new InferenceInitRes(true, new int[]{0,1,2});
        CommonRequest commonRequest = new CommonRequest(C1, data);
        intiRequests.add(commonRequest);
        String s = SendAndRecv.sendValidate(C1, modelToken, 0, AlgorithmType.FederatedGB, data, inferenceId, "label");
        BoostN1Res res = (BoostN1Res)serializer.deserialize(s);
        Assert.assertNull(res.getTrees());
        Assert.assertNull(res.getMultiClassUniqueLabelList());
        Assert.assertEquals(res.getFirstRoundPred(), 0.0);
    }

    @Test
    public void testBroadCastValidate() {
        List<CommonRequest> intiRequests = new ArrayList<>();
        BoostN1Res data = new BoostN1Res();
//        Message data = new InferenceInitRes(true, new int[]{0,1,2});
        CommonRequest commonRequest = new CommonRequest(C1, data);
        intiRequests.add(commonRequest);
        List<CommonResponse> responses = SendAndRecv.broadcastValidate(intiRequests, modelToken, AlgorithmType.FederatedGB, inferenceId, "label");
        Assert.assertEquals(responses.size(), 1);
        Assert.assertEquals(((BoostN1Res)responses.get(0).getBody()).getFirstRoundPred(), 0.0);

    }

    private void MockPost() {
        new MockUp<OkHttpUtil>() {
            @Mock
            public String post(String url, Map<String, Object> params) throws IOException {
                int code = 0;
                String status = "success";
                String data = serializer.serialize(new BoostN1Res());
//                String data = JsonUtil.object2json(new BoostN1Res());
                Map<String, Object> map = new HashMap<>();
                map.put("code", code);
                map.put("status", status);
                map.put("data", data);
                String s = JsonUtil.object2json(map);
                String compress = HttpClientUtil.compress(s);
                Response response = new Response(compress);
                String s1 = HttpClientUtil.compress(JsonUtil.object2json(response));
                return s1;
            }

        };
    }

    private void MockQueryAndFetch() {
        new MockUp<SendAndRecv>() {
            @Mock
            public String queryAndFetch(ClientInfo client, String stamp) throws InterruptedException, IOException {
                return "";
            }
        };
    }


    private void MockPostData(int exceptionType) {
        // 0: no exception; 1: exception1;2： exception2
        new MockUp<HttpClientUtil>() {
            @Mock
            public String doHttpPost(String uri, Object content) {
                Map<String, Object> query = new ConcurrentHashMap<>();
                query.put("data", "init_success");
                if (exceptionType==1) {
                    // 不compress会进入exception
                    query.put("dataIndex", 1);
                    query.put("msgid", "msgid");
                    query.put("dataSize", 1);
                    String s = JsonUtil.object2json(query);
                    return s;
                }
                else if (exceptionType==0) {
                    query.put("code", 0);
                    String s = JsonUtil.object2json(query);
                    String compress = HttpClientUtil.compress(s);
                    return compress;
                } else if (exceptionType==2){
                    // 没有code会有另一个exception
                    query.put("dataIndex", 1);
                    query.put("msgid", "msgid");
                    query.put("dataSize", 1);
                    String s = JsonUtil.object2json(query);
                    String compress = HttpClientUtil.compress(s);
                    return compress;
                } else {
                    // 最后一个send，预处理数据
                    query.put("code", 0);
                    query.put("status", "success");
                    String s = JsonUtil.object2json(query);
                    String compress = HttpClientUtil.compress(s);
                    return compress;
                }
            }
            @Mock
            public String doHttpGet(String uri) {
                return HttpClientUtil.compress("getData");
            }
        };
    }

    private void mockConfigInit() {
        new MockUp<ConfigUtil>() {
            @Mock
            public boolean getSplitTag() {
                return true;
            }

            @Mock
            public boolean getZipProperties() {
                return true;
            }

            @Mock
            public boolean getJdChainAvailable() {
                return false;
            }
        };
    }
}
