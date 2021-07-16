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
package com.jdt.fedlearn.common.entity;

import com.jdt.fedlearn.common.enums.RunStatusEnum;
import com.jdt.fedlearn.common.enums.TaskTypeEnum;
import org.testng.Assert;
import org.testng.annotations.Test;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;




public class TaskTest {
    @Test
    public void testName() {
        JobReq jobReq = new JobReq();
        jobReq.setJobId("test1");
        Job job = new Job(jobReq, new JobResult());
        Task task = new Task(job, RunStatusEnum.READY, TaskTypeEnum.INIT);

        System.out.println(task.getTaskId());
        Assert.assertTrue(task.getTaskId().contains("test1@INIT@10@"));
    }

    @Test
    public void testCompare() {
        JobReq jobReq = new JobReq();
        jobReq.setJobId("test1");

        Job job = new Job(jobReq, new JobResult());

        JobReq jobReq2 = new JobReq();
        jobReq2.setJobId("test2");
        Job job2 = new Job(jobReq2, new JobResult());

        Task task = new Task(job, RunStatusEnum.READY, TaskTypeEnum.INIT);
        Task task1 = new Task(job, RunStatusEnum.READY, TaskTypeEnum.FINISH);
        Task task2 = new Task(job2, RunStatusEnum.READY, TaskTypeEnum.INIT);
        Task task3 = new Task(job2, RunStatusEnum.READY, TaskTypeEnum.FINISH);

        List<Task> taskList = new ArrayList<>();
        taskList.add(task);
        taskList.add(task1);
        taskList.add(task2);
        taskList.add(task3);

        Collections.sort(taskList);
        Assert.assertEquals(taskList.get(0), task1);
        Assert.assertEquals(taskList.get(1), task3);
        Assert.assertEquals(taskList.get(2), task);
        Assert.assertEquals(taskList.get(3), task2);

    }
}
