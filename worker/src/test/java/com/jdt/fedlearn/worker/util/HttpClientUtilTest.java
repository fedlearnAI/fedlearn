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
package com.jdt.fedlearn.worker.util;

import com.jdt.fedlearn.common.network.INetWorkService;
import org.mockserver.client.MockServerClient;
import org.mockserver.mockserver.MockServer;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class HttpClientUtilTest {

    private static final String ip = "127.0.0.1";
    private static final int port = 1070;
    private static final String baseUrl = "http://"+ip+":"+port;
    MockServer server;

    @BeforeClass
    public void setup(){
        server = new MockServer(port);
    }

    @Test
    public void doHttpPost() {
        String path = "/getData";
        MockServerClient mockClient = new MockServerClient(ip, port);
        String expected = "data";
        mockClient.when(
                request()
                        .withPath(path)
                        .withMethod("POST")
        ).respond(
                response()
                        .withStatusCode(200)
                        .withBody(expected)
        );
        String result = INetWorkService.getNetWorkService().sendAndRecv(baseUrl + path , null);
        Assert.assertEquals(expected,result);
    }

    @AfterClass
    public void tearDown(){
        server.stop();
    }
}
