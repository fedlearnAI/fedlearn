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

import ch.qos.logback.core.joran.spi.JoranException;
import com.jdt.fedlearn.client.cache.ModelCache;
import com.jdt.fedlearn.client.util.ConfigUtil;
import com.jdt.fedlearn.core.model.DistributedRandomForestModel;
import com.jdt.fedlearn.core.model.Model;
import com.jdt.fedlearn.core.type.AlgorithmType;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

public class ModelCacheTest {
    @BeforeClass
    public void setUp() throws IOException, JoranException {
        ConfigUtil.init("src/test/resources/conf/worker.properties");

    }
    @Test
    public void put() {
        String modelToken = "124-DistributedRandomForest-210427162559";
        Model model = new DistributedRandomForestModel();
        ModelCache modelCache = ModelCache.getInstance();
        boolean put = modelCache.put(modelToken, model);
        Assert.assertEquals(put,true);
    }

    @Test
    public void get() {
        String modelToken = "124-DistributedRandomForest-210427162559";
        ModelCache modelCache = ModelCache.getInstance();
        Model model = modelCache.get(modelToken);
        Assert.assertEquals(model.getModelType(), AlgorithmType.DistributedRandomForest);
    }
}
