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
package com.jdt.fedlearn.worker.runner.impl.fedLearning;

import com.jdt.fedlearn.worker.spring.SpringBean;
import com.jdt.fedlearn.common.entity.*;
import com.jdt.fedlearn.common.enums.*;
import com.jdt.fedlearn.common.util.JsonUtil;
import com.jdt.fedlearn.common.util.ManagerCommandUtil;
import com.jdt.fedlearn.core.entity.common.TrainInit;
import com.jdt.fedlearn.core.entity.serialize.JavaSerializer;
import com.jdt.fedlearn.core.entity.serialize.Serializer;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.worker.util.ConfigUtil;

import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


@PrepareForTest({SpringBean.class, ManagerCommandUtil.class})
public class InitRunnerImplTest extends PowerMockTestCase {
    private InitRunnerImpl initRunnerImpl;

    @BeforeClass
    public void setUp() throws Exception {
        mockAddTask();
        ConfigUtil.init("src/test/resources/conf/worker.properties");
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(SpringBean.class);
        initRunnerImpl = (InitRunnerImpl) applicationContext.getBean("initRunnerImpl");
    }

    private void mockAddTask() {
        PowerMockito.mockStatic(ManagerCommandUtil.class);
        PowerMockito.when(ManagerCommandUtil.addTask(Mockito.any(),Mockito.any())).thenReturn(new JobResult());
    }


    @Test
    public void run() {
        JobReq jobReq = new JobReq();
        jobReq.setJobId("test");
        jobReq.setUsername("user");
        jobReq.setBusinessTypeEnum(BusinessTypeEnum.DEMO);
        jobReq.setManagerCommandEnum(ManagerCommandEnum.START);
        TrainRequest trainRequest = new TrainRequest();
        Serializer serializer = new JavaSerializer();
        String json = "{\"parameter\":{\"@clazz\":\"com.jdt.fedlearn.core.parameter.RandomForestParameter\",\"numTrees\":2,\"maxDepth\":3,\"maxTreeSamples\":300,\"maxSampledFeatures\":25,\"maxSampledRatio\":0.6,\"numPercentiles\":30,\"boostRatio\":0.0,\"nJobs\":10,\"minSamplesSplit\":10,\"localModel\":\"Null\",\"eval_metric\":[\"RMSE\"],\"loss\":\"Regression:MSE\",\"cat_features\":\"null\",\"encryptionType\":\"Paillier\",\"encryptionKeyPath\":\"/export/Data/paillier/\",\"encryptionCertainty\":1024},\"featureList\":{\"featureList\":[{\"name\":\"uid\",\"type\":\"float\",\"frequency\":1,\"id\":0},{\"name\":\"job\",\"type\":\"float\",\"frequency\":1,\"id\":0},{\"name\":\"previous\",\"type\":\"float\",\"frequency\":1,\"id\":0},{\"name\":\"balance\",\"type\":\"float\",\"frequency\":1,\"id\":0},{\"name\":\"education\",\"type\":\"float\",\"frequency\":1,\"id\":0},{\"name\":\"campaign\",\"type\":\"float\",\"frequency\":1,\"id\":0},{\"name\":\"poutcome\",\"type\":\"float\",\"frequency\":1,\"id\":0},{\"name\":\"y\",\"type\":\"float\",\"frequency\":1,\"id\":0}],\"label\":\"y\",\"index\":\"uid\"},\"idMap\":{\"content\":{\"0\":\"12739pQ\",\"1\":\"1331vB\",\"2\":\"1514kq\",\"3\":\"16393tv\",\"4\":\"19393tA\",\"5\":\"20062pI\",\"6\":\"25356Ux\",\"7\":\"2651gN\",\"8\":\"27004TS\",\"9\":\"32852Du\",\"10\":\"34879uN\",\"11\":\"36435go\",\"12\":\"38474dp\",\"13\":\"41891lx\",\"14\":\"4526LH\",\"15\":\"4879ZM\",\"16\":\"6762Yo\",\"17\":\"7203Cp\",\"18\":\"7656wQ\"},\"size\":19},\"others\":{\"sampleId\":\"\\u0003\\u0000\\u0000\\u0000\\u00035UU@\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\",\"sampleIds\":{\"0\":[0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18],\"1\":[0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18]},\"featureAllocation\":\"4,3\"}}";
        TrainInit trainInit = JsonUtil.json2Object(json,TrainInit.class);
        trainRequest.setData(serializer.serialize(trainInit));
        trainRequest.setModelToken("205-DistributedRandomForest-210329184017");
        trainRequest.setPhase(0);
        trainRequest.setDataNum(1);
        trainRequest.setAlgorithm(AlgorithmType.DistributedRandomForest);
        jobReq.setSubRequest(trainRequest);
        Job job = new Job(jobReq, new JobResult());
        Task task = new Task(job, RunStatusEnum.RUNNING, TaskTypeEnum.INIT);
        task.setTaskId("1");
        trainRequest.setDataIndex(1);
        trainRequest.setSync(false);
        CommonResultStatus run = initRunnerImpl.run(task);
        Assert.assertEquals(run.getResultTypeEnum(), ResultTypeEnum.SUCCESS);
    }
}
