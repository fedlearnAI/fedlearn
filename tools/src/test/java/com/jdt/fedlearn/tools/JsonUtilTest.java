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

import com.jdt.fedlearn.common.entity.JobReq;
import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.tools.serializer.JsonUtil;
import org.testng.Assert;
import org.testng.annotations.Test;


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonUtilTest {
    @Test
    public void object2json() {
        Map map = new LinkedHashMap();
        for(int i = 0 ; i< 5; i++){
            map.put("key"+i,"value"+i);
        }
        String s = JsonUtil.object2json(map);
        Assert.assertEquals(s.contains("key0"),true);
    }

    @Test
    public void parseJson() {
        String s = "{\"key0\":\"value0\",\"key1\":\"value1\",\"key2\":\"value2\",\"key3\":\"value3\",\"key4\":\"value4\"}";
        Map map = JsonUtil.json2Object(s,Map.class);
        Assert.assertEquals(map.size(),5);
    }


    @Test
    public void toJSONString() {
        String jsonStr = "{\"username\":null,\"managerCommandEnum\":null,\"businessTypeEnum\":null,\"jobId\":\"1\",\"params\":null,\"subRequest\":null}";
        JobReq jobReq = new JobReq();
        jobReq.setJobId("1");
        String result = JsonUtil.object2json(jobReq);
        Assert.assertEquals(jsonStr,result);
    }

//    @Test
//    public void toPrettyString() {
//        String jsonStr = "{\n" +
//                "\t\"businessTypeEnum\":null,\n" +
//                "\t\"jobId\":\"1\",\n" +
//                "\t\"managerCommandEnum\":null,\n" +
//                "\t\"params\":null,\n" +
//                "\t\"subRequest\":null,\n" +
//                "\t\"username\":null\n" +
//                "}";
//        JobReq jobReq = new JobReq();
//        jobReq.setJobId("1");
//        String result = JsonUtil.object2json(jobReq);
//        Assert.assertEquals(jsonStr,result);
//    }

    @Test
    public void parseObject() {
        String jsonStr = "{\"jobId\":\"1\"}";
        JobReq jobReq = JsonUtil.json2Object(jsonStr, JobReq.class);
        Assert.assertEquals(jobReq.getJobId(),"1");
    }

    @Test
    public void parseArray() {
        String s = "[\"key1\",\"key2\",\"key3\"]";
        List list = JsonUtil.parseArray(s,String.class);
        Assert.assertEquals(list.size(),3);
    }

    @Test
    public void test(){
        ClientInfo clientInfo = new ClientInfo("127.0.0.1",8080,"http");
        String s = JsonUtil.object2json(clientInfo);
        ClientInfo clientInfo1 = JsonUtil.json2Object(s,ClientInfo.class);
        System.out.println(clientInfo1.toString());
    }
}
