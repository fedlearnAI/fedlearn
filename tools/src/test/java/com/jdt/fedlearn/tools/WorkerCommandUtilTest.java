/* Copyright 2020 The FedLearn Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.jdt.fedlearn.tools;

import com.jdt.fedlearn.common.constant.ResponseConstant;
import com.jdt.fedlearn.common.entity.CommonResultStatus;
import com.jdt.fedlearn.common.entity.JobReq;
import com.jdt.fedlearn.common.entity.WorkerUnit;
import com.jdt.fedlearn.common.entity.Task;
import com.jdt.fedlearn.common.enums.ResultTypeEnum;
import com.jdt.fedlearn.common.enums.WorkerCommandEnum;


import com.jdt.fedlearn.tools.serializer.JsonUtil;
import org.mockserver.client.MockServerClient;
import org.mockserver.mockserver.MockServer;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class WorkerCommandUtilTest {

    private static final String ip = "127.0.0.1";
    private static final int port = 1080;
    private MockServer mockServer;

    @BeforeClass
    public void setup(){
        mockServer = new MockServer(port);
    }


    private void mockServer() throws IOException {
        CommonResultStatus commonResultStatus = new CommonResultStatus();
        commonResultStatus.setResultTypeEnum(ResultTypeEnum.SUCCESS);
        JobReq jobReq = new JobReq();
        jobReq.setJobId("1");
        jobReq.setUsername("test");
        Map map = new HashMap();
        map.put(ResponseConstant.DATA,jobReq);
        commonResultStatus.setData(map);
        String jsonStr = JsonUtil.object2json(commonResultStatus);

        MockServerClient mockClient = new MockServerClient(ip, port);
        String expected = GZIPCompressUtil.compress(jsonStr);
        /* 现在返回的编码不是utf-8的 后期建议统一*/
        String body = new String(expected.getBytes("UTF-8"),"ISO-8859-1");
        mockClient.when(
                request()
                        .withPath("/"+ WorkerCommandEnum.RUN_TASK.getCode())
                        .withMethod("POST")
        ).respond(
                response()
                        .withStatusCode(200)
                        .withBody(body)
        );
    }

    private void mockServerFail() throws IOException {
        CommonResultStatus commonResultStatus = new CommonResultStatus();
        commonResultStatus.setResultTypeEnum(ResultTypeEnum.BUS_FAIL);
        JobReq jobReq = new JobReq();
        jobReq.setJobId("1");
        jobReq.setUsername("test");
        Map map = new HashMap();
        map.put(ResponseConstant.DATA,jobReq);
        map.put(ResponseConstant.MESSAGE,"error");
        commonResultStatus.setData(map);
        String jsonStr = JsonUtil.object2json(commonResultStatus);

        MockServerClient mockClient = new MockServerClient(ip, port);
        String expected = GZIPCompressUtil.compress(jsonStr);
        /* 现在返回的编码不是utf-8的 后期建议统一*/
        String body = new String(expected.getBytes("UTF-8"),"ISO-8859-1");
        mockClient.when(
                request()
                        .withPath("/"+ WorkerCommandEnum.RUN_TASK.getCode())
                        .withMethod("POST")
        ).respond(
                response()
                        .withStatusCode(200)
                        .withBody(body)
        );
    }
    @Test
    public void testRequest() throws IOException {
        mockServer();
        CommonResultStatus request = WorkerCommandUtil.request("http://127.0.0.1:1080", WorkerCommandEnum.RUN_TASK, "");
        Assert.assertEquals(request.getResultTypeEnum(),ResultTypeEnum.SUCCESS);
    }

    @Test
    public void testRequestFail() throws IOException {
        mockServerFail();
        try {
            CommonResultStatus request = WorkerCommandUtil.request("http://127.0.0.1:1080", WorkerCommandEnum.RUN_TASK, "");
        }catch (Exception e){
            Assert.assertEquals(e.getMessage(),"调用worker 执行异常");
        }
    }


    @Test
    public void processTaskRequestData() throws IOException {
        mockServer();
        Task task = new Task();
        WorkerUnit workerUnit = new WorkerUnit();
        workerUnit.setIp(ip);
        workerUnit.setPort(port);
        task.setWorkerUnit(workerUnit);
        JobReq jobReq = WorkerCommandUtil.processTaskRequestData(task, WorkerCommandEnum.RUN_TASK, JobReq.class);
        Assert.assertEquals(jobReq.getJobId(),"1");
    }

    @Test
    public void buildUrl() {
        WorkerUnit workerUnit = new WorkerUnit();
        workerUnit.setIp(ip);
        workerUnit.setPort(port);
        String result = WorkerCommandUtil.buildUrl(workerUnit);
        Assert.assertEquals(result,"http://127.0.0.1:1080/");
    }

    @AfterClass
    public void afterClass(){
        mockServer.stop();
    }
}
