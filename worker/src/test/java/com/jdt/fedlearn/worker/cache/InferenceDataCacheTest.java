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
package com.jdt.fedlearn.worker.cache;

import com.jdt.fedlearn.core.entity.common.InferenceInit;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.worker.util.ConfigUtil;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


import java.util.List;

public class InferenceDataCacheTest {

    @BeforeClass
    public void setUp(){
        ConfigUtil.init("src/test/resources/conf/worker.properties");
    }

    @Test
    public void checkAndCache() {
        String inferenceId = "";
        AlgorithmType algorithm = AlgorithmType.DistributedRandomForest;

        String[] uids = {"292"};
        InferenceInit init = new InferenceInit(uids);
        List<Integer> integers = InferenceDataCache.checkAndCache(inferenceId, algorithm, init);
        //uid存在
        Assert.assertEquals(integers.size(),0);
    }

}
