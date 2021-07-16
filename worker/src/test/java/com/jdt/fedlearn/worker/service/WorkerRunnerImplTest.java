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
package com.jdt.fedlearn.worker.service;

import com.google.common.collect.Table;
import com.jdt.fedlearn.worker.runner.Runner;
import com.jdt.fedlearn.worker.runner.impl.fedLearning.InitRunnerImpl;
import com.jdt.fedlearn.worker.spring.SpringBean;
import com.jdt.fedlearn.common.constant.AppConstant;
import com.jdt.fedlearn.common.entity.*;
import com.jdt.fedlearn.common.enums.BusinessTypeEnum;
import com.jdt.fedlearn.common.enums.RunStatusEnum;
import com.jdt.fedlearn.common.enums.TaskTypeEnum;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.worker.util.ConfigUtil;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

public class WorkerRunnerImplTest {
    WorkerRunnerImpl workerRunner;

    @BeforeClass
    public void setUp() throws Exception {
        ConfigUtil.init("src/test/resources/conf/worker.properties");
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(SpringBean.class);
        workerRunner = (WorkerRunnerImpl) applicationContext.getBean("workerRunner");
    }

    @Test
    public void getRunnerTable() {
        Table<BusinessTypeEnum, TaskTypeEnum, Runner> runnerTable = workerRunner.getRunnerTable();
        Runner runner = runnerTable.get(BusinessTypeEnum.FED_LEARNING, TaskTypeEnum.INIT);
        Assert.assertEquals(runner instanceof InitRunnerImpl,true);
    }

    @Test
    public void isReady() {
        boolean ready = workerRunner.isReady(8094);
        Assert.assertTrue(ready);
    }

    @Test
    public void addRunner() {
        Runner runner = workerRunner.addRunner(BusinessTypeEnum.FED_LEARNING, TaskTypeEnum.INIT, new InitRunnerImpl());
        Assert.assertTrue(runner instanceof InitRunnerImpl);
    }

    @Test
    public void run() {
        isReady();
        JobReq jobReq = new JobReq();
        jobReq.setJobId("test");
        TrainRequest request = new TrainRequest();
        request.setAlgorithm(AlgorithmType.DistributedRandomForest);
        jobReq.setSubRequest(request);
        Job job = new Job(jobReq, new JobResult());


        WorkerUnit workerUnit = new WorkerUnit();
        workerUnit.setName("worker1");
        workerUnit.setPort(8094);
        workerUnit.setIp("127.0.0.1");

        Task task = new Task(job, RunStatusEnum.READY, TaskTypeEnum.INIT);
        task.setWorkerUnit(workerUnit);
        CommonResultStatus run = workerRunner.run(task);
        Assert.assertEquals(run.getResultTypeEnum().getCode(),-2);
        List<Task> data = (List) run.getData().get("data");
//Assert.assertEquals(data != null,true);
    }
}
