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

import com.jdt.fedlearn.common.entity.TrainRequest;
import com.jdt.fedlearn.tools.PacketUtil;
import org.testng.Assert;
import org.testng.annotations.Test;


import java.io.IOException;
public class PacketUtilTest {

    @Test
    public void preHandel() throws IOException {
        TrainRequest trainRequest = new TrainRequest();
        trainRequest.setDataNum(1);
        trainRequest.setDataIndex(1);
        trainRequest.setGzip(false);
        trainRequest.setModelToken("test");
        boolean b = PacketUtil.preHandel(trainRequest);
        Assert.assertEquals(b,true);
    }
}
