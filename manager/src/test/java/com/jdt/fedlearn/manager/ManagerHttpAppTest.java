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
package com.jdt.fedlearn.manager;

import com.jdt.fedlearn.common.entity.JobResult;
import com.jdt.fedlearn.common.enums.ResultTypeEnum;
import com.jdt.fedlearn.common.util.GZIPCompressUtil;
import com.jdt.fedlearn.common.util.JsonUtil;
import com.jdt.fedlearn.common.network.INetWorkService;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


public class ManagerHttpAppTest {
    @BeforeClass
    public static void setUp(){
        String[] args = new String[]{"-c", "src/test/resources/conf/manager.properties"};
        new Thread(() -> ManagerHttpApp.main(args)).start();
    }

    @Test
    public void testMain() throws Exception {
        Thread.sleep(3000L);
        String s = INetWorkService.getNetWorkService().sendAndRecv("http://127.0.0.1:9090/demo", "");
        String result = GZIPCompressUtil.unCompress(s);
        JobResult jobResult = JsonUtil.json2Object(result, JobResult.class);
        Assert.assertEquals(jobResult.getResultTypeEnum(), ResultTypeEnum.SUCCESS);
    }

    @Test
    public void handle() {
    }
}
