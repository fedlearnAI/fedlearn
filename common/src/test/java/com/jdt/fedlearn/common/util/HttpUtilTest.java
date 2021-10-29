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

import com.jdt.fedlearn.common.entity.MockRequest;
import com.jdt.fedlearn.common.network.INetWorkService;
import org.mockserver.client.MockServerClient;
import org.mockserver.mockserver.MockServer;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class HttpUtilTest {
    private static MockServer mockServer;

    @BeforeClass
    public static void setup(){
        mockServer = new MockServer(1090);
    }

    @Test
    public void postData() {

        MockServerClient mockClient = new MockServerClient("127.0.0.1", 1090);
        String expected = "postData";
        mockClient.when(
                request()
                        .withPath("/postData")
                        .withMethod("POST")
        ).respond(
                response()
                        .withStatusCode(200)
                        .withBody(expected)
        );
        String result = INetWorkService.getNetWorkService().sendAndRecv("http://127.0.0.1:1090/postData", "test");
        Assert.assertEquals(expected,result);
    }

    @Test
    public void compress() throws IOException {
        String s = "{\"code\":0,\"data\":{\"predict\":[{\"uid\":\"107601\",\"score\":\"1.0\"},{\"uid\":\"107601\",\"score\":\"1.0\"},{\"uid\":\"107601\",\"score\":\"NaN\"},{\"uid\":\"107601\",\"score\":\"NaN\"},{\"uid\":\"107601\",\"score\":\"NaN\"}]},\"status\":\"success\"}";
        String compress = GZIPCompressUtil.compress(s);
        System.out.println(compress);
        String result = GZIPCompressUtil.unCompress(compress);
        Assert.assertEquals(s,result);
    }

    @Test
    public void unCompress() throws IOException {
        String s = "{\"code\":0,\"data\":{\"predict\":[{\"uid\":\"107601\",\"score\":\"1.0\"},{\"uid\":\"107601\",\"score\":\"1.0\"},{\"uid\":\"107601\",\"score\":\"NaN\"},{\"uid\":\"107601\",\"score\":\"NaN\"},{\"uid\":\"107601\",\"score\":\"NaN\"}]},\"status\":\"success\"}";
        String compress = GZIPCompressUtil.compress(s);
        System.out.println(compress);
        String result = GZIPCompressUtil.unCompress(compress);
        Assert.assertEquals(s,result);
    }

    @Test
    public void getRemoteIP() {
        String ip = "127.0.0.1";
        MockRequest mockRequest = new MockRequest(ip);
        String remoteIP = IpAddressUtil.getRemoteIP(mockRequest);
        Assert.assertEquals(ip,remoteIP);
    }

    @AfterClass
    public static void tearDown(){
        mockServer.stop();
    }

}
