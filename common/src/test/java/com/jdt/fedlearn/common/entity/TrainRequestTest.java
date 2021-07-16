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
package com.jdt.fedlearn.common.entity;


import org.testng.Assert;
import org.testng.annotations.Test;

public class TrainRequestTest {

    @Test
    public void testEquals() {
        TrainRequest t1 = new TrainRequest();
        TrainRequest t2 = new TrainRequest();
        t1.setPhase(1);
        t1.setModelToken("modelToken");
        t1.setDataNum(10);
        t2.setPhase(1);
        t2.setModelToken("modelToken");
        t2.setDataNum(10);
        Assert.assertEquals(t1, t2);
    }

    @Test
    public void parseJson() {
        String jsonStr = "{\"modelToken\":\"modelToken\",\"algorithm\":\"DistributedRandomForest\",\"phase\":1,\"data\":null,\"dataNum\":10,\"dataIndex\":0,\"isGzip\":false,\"status\":null,\"requestId\":\"1\",\"reduceType\":\"2\",\"isSync\":false}";
        TrainRequest t = new TrainRequest(jsonStr);
        Assert.assertEquals(t.getModelToken(),"modelToken");
    }

    @Test
    public void testToString() {
        String jsonStr = "{\"modelToken\":\"modelToken\",\"algorithm\":\"DistributedRandomForest\",\"phase\":1,\"data\":null,\"dataNum\":10,\"dataIndex\":0,\"isGzip\":false,\"status\":null,\"requestId\":\"1\",\"reduceType\":\"2\",\"isSync\":false}";
        TrainRequest t = new TrainRequest(jsonStr);
        String s = t.toString();
        Assert.assertTrue(s.startsWith("SubRequest{"));
    }
}
