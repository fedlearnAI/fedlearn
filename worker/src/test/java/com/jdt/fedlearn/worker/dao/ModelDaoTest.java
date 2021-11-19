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
import com.jdt.fedlearn.core.model.DistributedRandomForestModel;
import com.jdt.fedlearn.core.model.Model;

import com.jdt.fedlearn.core.model.common.CommonModel;
import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;
import com.jdt.fedlearn.tools.FileUtil;
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
        Model model = CommonModel.constructModel(AlgorithmType.DistributedRandomForest);
        String input = "{Tree3={\"0\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":151.0001514171516}\",\"isLeaf\":\"0\",\"nodeId\":\"0\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"1\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":99.99998002234719}\",\"isLeaf\":\"0\",\"nodeId\":\"1\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"7\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":43.999921054931626}\",\"isLeaf\":\"0\",\"nodeId\":\"7\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"9\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":124.00012914274178}\",\"isLeaf\":\"0\",\"nodeId\":\"9\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"30\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":155.00012467329694}\",\"isLeaf\":\"0\",\"nodeId\":\"30\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"}}, numTrees=5, Tree2={\"0\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":151.00027697217268}\",\"isLeaf\":\"0\",\"nodeId\":\"0\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"1\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":99.9999472645977}\",\"isLeaf\":\"0\",\"nodeId\":\"1\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"16\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":-8.048749544327601E-7}\",\"isLeaf\":\"0\",\"nodeId\":\"16\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"7\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":-9.680471521253467E-7}\",\"isLeaf\":\"0\",\"nodeId\":\"7\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"8\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":43.99999614858515}\",\"isLeaf\":\"0\",\"nodeId\":\"8\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"19\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":124.99982322738359}\",\"isLeaf\":\"0\",\"nodeId\":\"19\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"9\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":124.99984795553796}\",\"isLeaf\":\"0\",\"nodeId\":\"9\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"30\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":155.00027031865756}\",\"isLeaf\":\"0\",\"nodeId\":\"30\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"21\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":126.9999433362098}\",\"isLeaf\":\"0\",\"nodeId\":\"21\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"}}, Tree4={\"22\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":3.0000075248494786}\",\"isLeaf\":\"0\",\"nodeId\":\"22\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"0\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":151.0001365676909}\",\"isLeaf\":\"0\",\"nodeId\":\"0\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"1\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":99.9999366657892}\",\"isLeaf\":\"0\",\"nodeId\":\"1\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"29\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":155.0001897353887}\",\"isLeaf\":\"0\",\"nodeId\":\"29\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"20\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":127.99992401629248}\",\"isLeaf\":\"0\",\"nodeId\":\"20\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"}}, Tree1={\"11\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":145.00003989208258}\",\"isLeaf\":\"0\",\"nodeId\":\"11\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"0\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":127.99985305695758}\",\"isLeaf\":\"0\",\"nodeId\":\"0\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"5\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":160.9997883838923}\",\"isLeaf\":\"0\",\"nodeId\":\"5\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"28\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":153.99981309357239}\",\"isLeaf\":\"0\",\"nodeId\":\"28\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"6\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":154.99983717684108}\",\"isLeaf\":\"0\",\"nodeId\":\"6\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"10\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":99.00010749733747}\",\"isLeaf\":\"0\",\"nodeId\":\"10\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"}}, Tree0={\"22\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":7.999994188589154}\",\"isLeaf\":\"0\",\"nodeId\":\"22\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"0\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":151.00017897553693}\",\"isLeaf\":\"0\",\"nodeId\":\"0\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"1\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":99.9999725963405}\",\"isLeaf\":\"0\",\"nodeId\":\"1\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"3\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":10.000011611323714}\",\"isLeaf\":\"0\",\"nodeId\":\"3\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"15\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":-1.6991660774055133E-6}\",\"isLeaf\":\"0\",\"nodeId\":\"15\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"26\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":7.000009888437513}\",\"isLeaf\":\"0\",\"nodeId\":\"26\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"16\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":44.00001916744659}\",\"isLeaf\":\"0\",\"nodeId\":\"16\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"5\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":155.00001821716612}\",\"isLeaf\":\"0\",\"nodeId\":\"5\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"21\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":123.99996211692273}\",\"isLeaf\":\"0\",\"nodeId\":\"21\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"}}, localModelType=Null, localModel={alpha\u00030.0\u0002beta\u00030.0\u00040.0}}";
        model.deserialize(input);
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
