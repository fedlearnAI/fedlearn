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
package com.jdt.fedlearn.worker.dao;

import ch.qos.logback.core.joran.spi.JoranException;
import com.jdt.fedlearn.client.dao.ModelDao;
import com.jdt.fedlearn.client.util.ConfigUtil;
import com.jdt.fedlearn.common.util.FileUtil;
import com.jdt.fedlearn.core.model.DistributedRandomForestModel;
import com.jdt.fedlearn.core.model.Model;

import com.jdt.fedlearn.core.model.common.CommonModel;
import com.jdt.fedlearn.core.type.AlgorithmType;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

@PrepareForTest({FileUtil.class})
public class ModelDaoTest extends PowerMockTestCase {

    @BeforeClass
    public void setUp() throws IOException, JoranException {
        ConfigUtil.init("src/test/resources/conf/worker.properties");
    }

    @Test
    public void saveModel() {
        PowerMockito.mockStatic(FileUtil.class);
        PowerMockito.when(FileUtil.writeFile(Mockito.any(),Mockito.any())).thenReturn(true);
        String modelToken = "testToken";
        String modelStr = "test";
        Model model = CommonModel.constructModel(AlgorithmType.DistributedRandomForest);
        model.deserialize(modelStr);
        Boolean aBoolean = ModelDao.saveModel(modelToken,model);
        Assert.assertTrue(aBoolean);
    }

    @Test
    public void testSaveModel() {
        PowerMockito.mockStatic(FileUtil.class);
        PowerMockito.when(FileUtil.writeFile(Mockito.any(),Mockito.any())).thenReturn(true);
        String modelToken = "testToken";
        Model model = new DistributedRandomForestModel();
        Boolean aBoolean = ModelDao.saveModel(modelToken,model);
        Assert.assertTrue(aBoolean);
    }

    @Test
    public void loadModel() {
    }
}
