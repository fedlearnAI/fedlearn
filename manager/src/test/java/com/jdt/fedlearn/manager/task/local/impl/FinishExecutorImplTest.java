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
package com.jdt.fedlearn.manager.task.local.impl;

import com.jdt.fedlearn.common.constant.AppConstant;
import com.jdt.fedlearn.common.entity.*;
import com.jdt.fedlearn.common.enums.ResultTypeEnum;
import com.jdt.fedlearn.common.enums.RunStatusEnum;
import com.jdt.fedlearn.common.enums.TaskTypeEnum;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;


public class FinishExecutorImplTest {


    @Test
    public void testRunForEmptyPreTaskList() {
        FinishExecutorImpl finishExecutor = new FinishExecutorImpl();

        JobReq jobReq = new JobReq();
        jobReq.setJobId("test");
        Job job = new Job(jobReq, new JobResult());

        Task task = new Task(job, RunStatusEnum.RUNNING, TaskTypeEnum.FINISH);
        task.setTaskId("1");
        CommonResultStatus commonResultStatus = finishExecutor.run(task);
        Assert.assertEquals(commonResultStatus.getResultTypeEnum(), ResultTypeEnum.OTHER_FAIL);
        Assert.assertTrue(task.getJob().getJobResult().getData().get(AppConstant.MESSAGE).toString().contains("DAG"));
    }

    @Test
    public void testRunForErrorPreTaskList() {
        FinishExecutorImpl finishExecutor = new FinishExecutorImpl();
        JobReq jobReq = new JobReq();
        jobReq.setJobId("test");
        Job job = new Job(jobReq, new JobResult());
        Task preTask = new Task(job, RunStatusEnum.RUNNING, TaskTypeEnum.REDUCE);
        Task task = new Task(job, RunStatusEnum.RUNNING, TaskTypeEnum.FINISH);
        task.getPreTaskList().add(preTask);
        task.setTaskId("1");
        CommonResultStatus commonResultStatus = finishExecutor.run(task);
        Assert.assertEquals(commonResultStatus.getResultTypeEnum(), ResultTypeEnum.OTHER_FAIL);
        Assert.assertTrue(task.getJob().getJobResult().getData().get(AppConstant.MESSAGE).toString().contains("前置任务"));
    }

    @Test
    public void buildJobResult() {
        FinishExecutorImpl finishExecutor = new FinishExecutorImpl();
        TaskResultData taskResultData = new TaskResultData();
        taskResultData.setModelMap(new HashMap<>());
        Job job = new Job(new JobReq(),new JobResult());
        Task task = new Task();
        task.setJob(job);
        task.setRunStatusEnum(RunStatusEnum.SUCCESS);
        JobResult jobResult = finishExecutor.buildJobResult(task,taskResultData);
        Assert.assertEquals(jobResult.getResultTypeEnum(),ResultTypeEnum.SUCCESS);
    }
}
