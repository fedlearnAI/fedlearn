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

import com.jdt.fedlearn.client.util.ConfigUtil;
import com.jdt.fedlearn.common.entity.core.feature.Features;
import com.jdt.fedlearn.core.parameter.RandomForestParameter;
import com.jdt.fedlearn.tools.ManagerCommandUtil;
import com.jdt.fedlearn.worker.spring.SpringBean;
import com.jdt.fedlearn.common.entity.*;
import com.jdt.fedlearn.common.enums.*;
import com.jdt.fedlearn.core.entity.common.TrainInit;
import com.jdt.fedlearn.tools.serializer.JavaSerializer;
import com.jdt.fedlearn.tools.serializer.Serializer;
import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;

import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@PowerMockIgnore("javax.net.ssl.*")
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
        String json = "";
//        TrainInit trainInit = JsonUtil.json2Object(json,TrainInit.class);

        RandomForestParameter parameter = new RandomForestParameter();
        Features localFeature = new Features(new ArrayList<>());
        Map<String, Object> other = new HashMap<>();
        other.put("sampleId", "\"\\u0003\\u0000\\u0000\\u0000\\u00035UU@\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\\u0000\"");
        other.put("sampleIds", "{\"0\":[0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18],\"1\":[0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18]}");
        other.put("featureAllocation","4,3");
        String matchId = "2-MD5-210719144319";
        TrainInit trainInit = new TrainInit(parameter, localFeature, matchId, other);


        trainRequest.setData(serializer.serialize(trainInit));
        trainRequest.setModelToken("205-DistributedRandomForest-210329184017");
        trainRequest.setPhase(0);
        trainRequest.setDataNum(1);
        trainRequest.setAlgorithm(AlgorithmType.DistributedRandomForest);
        trainRequest.setStatus(RunningType.COMPLETE);
        jobReq.setSubRequest(trainRequest);
        Job job = new Job(jobReq, new JobResult());
        Task task = new Task(job, RunStatusEnum.RUNNING, TaskTypeEnum.INIT);
        task.setTaskId("1");
        trainRequest.setDataIndex(1);
        trainRequest.setSync(false);
        task.setTrainRequest(trainRequest);
        CommonResultStatus run = initRunnerImpl.run(task);
        Assert.assertEquals(run.getResultTypeEnum(), ResultTypeEnum.SUCCESS);
    }
}
