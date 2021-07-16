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

import com.google.common.collect.Lists;
import com.jdt.fedlearn.common.entity.Job;
import com.jdt.fedlearn.common.entity.JobReq;
import com.jdt.fedlearn.common.entity.JobResult;
import com.jdt.fedlearn.common.entity.Task;
import com.jdt.fedlearn.common.enums.RunStatusEnum;
import com.jdt.fedlearn.common.enums.TaskTypeEnum;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


import java.util.ArrayList;
import java.util.List;

public class DAGUtilTest {

    private Job job;
    private Task initTask;

    @BeforeClass
    public void setUp() {
        JobReq jobReq = new JobReq();
        jobReq.setJobId("test");
        JobResult jobResult = new JobResult();
        job = new Job(jobReq, jobResult);

        initTask = new Task(job, RunStatusEnum.RUNNING, TaskTypeEnum.INIT);

    }

    @Test
    public void testCorrectCheckTasksIsDAG() {

        Task mapTask1 = new Task(job, RunStatusEnum.INIT, TaskTypeEnum.MAP);
        Task mapTask2 = new Task(job, RunStatusEnum.INIT, TaskTypeEnum.MAP);
        Task mapTask3 = new Task(job, RunStatusEnum.INIT, TaskTypeEnum.MAP);
        Task mapTask4 = new Task(job, RunStatusEnum.INIT, TaskTypeEnum.MAP);
        Task mapTask5 = new Task(job, RunStatusEnum.INIT, TaskTypeEnum.MAP);
        Task mapTask6 = new Task(job, RunStatusEnum.INIT, TaskTypeEnum.MAP);
        Task mapTask7 = new Task(job, RunStatusEnum.INIT, TaskTypeEnum.MAP);

        Task reduceTask = new Task(job, RunStatusEnum.INIT, TaskTypeEnum.REDUCE);
//Task reduceTask2 = new Task(job, RunStatusEnum.INIT, TaskTypeEnum.REDUCE);
//reduceTask2.setPreTaskList(Lists.newArrayList(mapTask3));
        Task finishTask = new Task(job, RunStatusEnum.INIT, TaskTypeEnum.FINISH);

        mapTask1.setPreTaskList(Lists.newArrayList(initTask));
        mapTask2.setPreTaskList(Lists.newArrayList(initTask));
        mapTask3.setPreTaskList(Lists.newArrayList(initTask));
        mapTask4.setPreTaskList(Lists.newArrayList(initTask));
        mapTask5.setPreTaskList(Lists.newArrayList(initTask));
        mapTask6.setPreTaskList(Lists.newArrayList(initTask));
        mapTask7.setPreTaskList(Lists.newArrayList(initTask));
        reduceTask.setPreTaskList(Lists.newArrayList(mapTask1, mapTask2,
                mapTask3, mapTask4, mapTask5, mapTask6, mapTask7));
        finishTask.setPreTaskList(Lists.newArrayList(reduceTask));
        List<Task> taskList = new ArrayList<>();

        taskList.add(initTask);
        taskList.add(mapTask1);
        taskList.add(mapTask2);
        taskList.add(mapTask3);
        taskList.add(mapTask4);
        taskList.add(mapTask5);
        taskList.add(mapTask6);
        taskList.add(mapTask7);
        taskList.add(reduceTask);
        taskList.add(finishTask);

        boolean isDag = DAGUtil.checkTasksIsDAG(taskList);
        Assert.assertTrue(isDag);
    }


    @Test
    public void testCircleErrorCheckTasksIsDAG() {

        Task mapTask1 = new Task(job, RunStatusEnum.INIT, TaskTypeEnum.MAP);
        Task mapTask2 = new Task(job, RunStatusEnum.INIT, TaskTypeEnum.MAP);
        Task mapTask3 = new Task(job, RunStatusEnum.INIT, TaskTypeEnum.MAP);
        Task mapTask4 = new Task(job, RunStatusEnum.INIT, TaskTypeEnum.MAP);
        Task mapTask5 = new Task(job, RunStatusEnum.INIT, TaskTypeEnum.MAP);
        Task mapTask6 = new Task(job, RunStatusEnum.INIT, TaskTypeEnum.MAP);
        Task mapTask7 = new Task(job, RunStatusEnum.INIT, TaskTypeEnum.MAP);

        Task reduceTask = new Task(job, RunStatusEnum.INIT, TaskTypeEnum.REDUCE);
        Task finishTask = new Task(job, RunStatusEnum.INIT, TaskTypeEnum.FINISH);

        mapTask1.setPreTaskList(Lists.newArrayList(initTask));
        mapTask2.setPreTaskList(Lists.newArrayList(initTask));
        mapTask3.setPreTaskList(Lists.newArrayList(initTask));
        mapTask4.setPreTaskList(Lists.newArrayList(initTask));
        mapTask5.setPreTaskList(Lists.newArrayList(initTask));
        mapTask6.setPreTaskList(Lists.newArrayList(initTask));
        reduceTask.setPreTaskList(Lists.newArrayList(mapTask1, mapTask2,
                mapTask3, mapTask4, mapTask5, mapTask6, mapTask7));
//make a circle
        mapTask7.setPreTaskList(Lists.newArrayList(initTask, reduceTask));

        finishTask.setPreTaskList(Lists.newArrayList(reduceTask));
        List<Task> taskList = new ArrayList<>();

        taskList.add(initTask);
        taskList.add(mapTask1);
        taskList.add(mapTask2);
        taskList.add(mapTask3);
        taskList.add(mapTask4);
        taskList.add(mapTask5);
        taskList.add(mapTask6);
        taskList.add(mapTask7);
        taskList.add(reduceTask);
        taskList.add(finishTask);

        boolean isDag = DAGUtil.checkTasksIsDAG(taskList);
        Assert.assertFalse(isDag);
    }


    @Test
    public void testSelfCircleErrorCheckTasksIsDAG() {

        Task mapTask1 = new Task(job, RunStatusEnum.INIT, TaskTypeEnum.MAP);
        Task mapTask2 = new Task(job, RunStatusEnum.INIT, TaskTypeEnum.MAP);
        Task mapTask3 = new Task(job, RunStatusEnum.INIT, TaskTypeEnum.MAP);

        Task reduceTask = new Task(job, RunStatusEnum.INIT, TaskTypeEnum.REDUCE);
        Task finishTask = new Task(job, RunStatusEnum.INIT, TaskTypeEnum.FINISH);

        mapTask1.setPreTaskList(Lists.newArrayList(initTask));
        mapTask2.setPreTaskList(Lists.newArrayList(initTask, mapTask2));
        reduceTask.setPreTaskList(Lists.newArrayList(mapTask1, mapTask2,
                mapTask3));
//make a circle
        mapTask3.setPreTaskList(Lists.newArrayList(initTask));

        finishTask.setPreTaskList(Lists.newArrayList(reduceTask));
        List<Task> taskList = new ArrayList<>();

        taskList.add(initTask);
        taskList.add(mapTask1);
        taskList.add(mapTask2);
        taskList.add(mapTask3);
        taskList.add(reduceTask);
        taskList.add(finishTask);

        boolean isDag = DAGUtil.checkTasksIsDAG(taskList);
        Assert.assertFalse(isDag);
    }

    @Test
    public void testFinishErrorIsDAG() {

        Task mapTask1 = new Task(job, RunStatusEnum.INIT, TaskTypeEnum.MAP);

        Task reduceTask = new Task(job, RunStatusEnum.INIT, TaskTypeEnum.REDUCE);
        Task finishTask = new Task(job, RunStatusEnum.INIT, TaskTypeEnum.FINISH);

        mapTask1.setPreTaskList(Lists.newArrayList(initTask));
        reduceTask.setPreTaskList(Lists.newArrayList(mapTask1));

        finishTask.setPreTaskList(Lists.newArrayList(reduceTask));
        List<Task> taskList = new ArrayList<>();

        taskList.add(initTask);
        taskList.add(mapTask1);
        taskList.add(reduceTask);
//taskList.add(finishTask);

        boolean isDag = DAGUtil.checkTasksIsDAG(taskList);
        Assert.assertFalse(isDag);
    }


    @Test
    public void testTwoCircleErrorIsDAG() {

        Task mapTask1 = new Task(job, RunStatusEnum.INIT, TaskTypeEnum.MAP);
        Task mapTask2 = new Task(job, RunStatusEnum.INIT, TaskTypeEnum.MAP);
        Task mapTask3 = new Task(job, RunStatusEnum.INIT, TaskTypeEnum.MAP);

        Task reduceTask = new Task(job, RunStatusEnum.INIT, TaskTypeEnum.REDUCE);
        Task finishTask = new Task(job, RunStatusEnum.INIT, TaskTypeEnum.FINISH);

        mapTask1.setPreTaskList(Lists.newArrayList(initTask));
        mapTask2.setPreTaskList(Lists.newArrayList(initTask, reduceTask));
        mapTask3.setPreTaskList(Lists.newArrayList(initTask, reduceTask));
        reduceTask.setPreTaskList(Lists.newArrayList(mapTask1, mapTask2,
                mapTask3));
        finishTask.setPreTaskList(Lists.newArrayList(reduceTask));
        List<Task> taskList = new ArrayList<>();

        taskList.add(initTask);
        taskList.add(mapTask1);
        taskList.add(mapTask2);
        taskList.add(mapTask3);
        taskList.add(reduceTask);
        taskList.add(finishTask);

        boolean isDag = DAGUtil.checkTasksIsDAG(taskList);
        Assert.assertFalse(isDag);
    }
}
