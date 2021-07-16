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
package com.jdt.fedlearn.common.util;

import com.jdt.fedlearn.common.constant.AppConstant;
import com.jdt.fedlearn.common.entity.JobResult;
import com.jdt.fedlearn.common.enums.ManagerCommandEnum;
import com.jdt.fedlearn.common.enums.ResultTypeEnum;

import org.mockserver.client.MockServerClient;
import org.mockserver.mockserver.MockServer;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class ManagerCommandUtilTest {
    private static final String ip = "127.0.0.1";
    private static final int port = 1070;
    private static final String baseUrl = "http://"+ip+":"+port;
    private MockServer mockServer;

    @BeforeClass
    public void setup(){
        mockServer = new MockServer(port);
    }

    private void mockServer() throws IOException {
        JobResult jobResult = new JobResult();
        jobResult.setResultTypeEnum(ResultTypeEnum.SUCCESS);
        String jsonStr = JsonUtil.object2json(jobResult);

        MockServerClient mockClient = new MockServerClient(ip, port);
        String expected = HttpClientUtil.compress(jsonStr);
        /* 现在返回的编码不是utf-8的 后期建议统一*/
        String body = new String(expected.getBytes("UTF-8"),"ISO-8859-1");
        mockClient.when(
                request()
                        .withPath("/"+ ManagerCommandEnum.ADD_TASKS.getCode())
                        .withMethod("POST")
        ).respond(
                response()
                        .withStatusCode(200)
                        .withBody(body)
        );
    }

    private void mockServerFail() throws IOException {
        JobResult jobResult = new JobResult();
        jobResult.setResultTypeEnum(ResultTypeEnum.BUS_FAIL);
        Map<String,Object> map = new HashMap<>();
        map.put(AppConstant.MESSAGE,"error");
        jobResult.setData(map);
        String jsonStr = JsonUtil.object2json(jobResult);

        MockServerClient mockClient = new MockServerClient(ip, port);
        String expected = HttpClientUtil.compress(jsonStr);
        /* 现在返回的编码不是utf-8的 后期建议统一*/
        String body = new String(expected.getBytes("UTF-8"),"ISO-8859-1");
        mockClient.when(
                request()
                        .withPath("/"+ ManagerCommandEnum.ADD_TASKS.getCode())
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
        JobResult jobResult = ManagerCommandUtil.request(baseUrl, ManagerCommandEnum.ADD_TASKS, "");
        Assert.assertEquals(jobResult.getResultTypeEnum(),ResultTypeEnum.SUCCESS);
    }

    @Test
    public void testRequestFail() throws IOException {
        mockServerFail();
        try {
            JobResult jobResult = ManagerCommandUtil.request(baseUrl, ManagerCommandEnum.ADD_TASKS, "");
        } catch (Exception e) {
            e.printStackTrace();
            Assert.assertEquals(e.getMessage(),"java.lang.RuntimeException: 调用manager command执行异常:ADD_TASKS");
        }

    }

    @Test
    public void addTask() throws IOException {
        mockServer();
        JobResult jobResult = ManagerCommandUtil.addTask(baseUrl, new ArrayList<>());
        Assert.assertEquals(jobResult.getResultTypeEnum(),ResultTypeEnum.SUCCESS);
    }

    @AfterClass
    public void tearDown(){
        mockServer.stop();
    }
}
