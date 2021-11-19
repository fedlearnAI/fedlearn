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
package com.jdt.fedlearn.worker.service;

import ch.qos.logback.core.joran.spi.JoranException;
import com.jdt.fedlearn.client.entity.prepare.MatchRequest;
import com.jdt.fedlearn.client.service.PrepareService;
import com.jdt.fedlearn.client.util.ConfigUtil;
import com.jdt.fedlearn.common.constant.ResponseConstant;
import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.tools.PacketUtil;
import com.jdt.fedlearn.tools.serializer.JavaSerializer;
import com.jdt.fedlearn.tools.serializer.Serializer;
import com.jdt.fedlearn.core.psi.CommonPrepare;
import com.jdt.fedlearn.core.psi.Prepare;
import com.jdt.fedlearn.core.type.MappingType;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PrepareServiceTest {

    @BeforeClass
    public void setUp() throws IOException, JoranException {
        ConfigUtil.init("src/test/resources/conf/worker.properties");
    }

    @Test
    public void match() {
        Serializer serializer = new JavaSerializer();
        MappingType mappingType = MappingType.valueOf("MD5");
        Prepare prepare = CommonPrepare.construct(mappingType);
        List<ClientInfo> clientInfos = new ArrayList<>();
        ClientInfo clientInfo = new ClientInfo("127.0.0.1",8094,"http");
        clientInfos.add(clientInfo);
        List<CommonRequest> matchRequests = prepare.masterInit(clientInfos);
        CommonRequest commonRequest = matchRequests.get(0);
        System.out.println(matchRequests.get(0).getBody());

        PrepareService prepareService = new PrepareService();
        MatchRequest request = new MatchRequest();
        request.setDataset("mo17k.csv");
        request.setMatchType("MD5");
        request.setMatchToken("test");
        String body = serializer.serialize(commonRequest.getBody());
        request.setBody(body);
        Map<String, Object> match = prepareService.match(request);
        Assert.assertEquals(match.get(ResponseConstant.STATUS),"success");
    }

    @Test
    public void getSplitData() {
        String key = "key";
        List<String> list = new ArrayList<>();
        for(int i=0;i<1000;i++){
            list.add(i+"");
        }
        Map<String,Object> map = new HashMap<>();
        map.put("msgid",key);
        map.put("dataSize",2);
        map.put("dataIndex",1);
        PacketUtil.msgMap.put(key,list);
        PrepareService prepareService = new PrepareService();
        Map<String, Object> splitData = prepareService.getSplitData(map);
        Assert.assertEquals(splitData.get(ResponseConstant.STATUS),"fail");
    }
}
