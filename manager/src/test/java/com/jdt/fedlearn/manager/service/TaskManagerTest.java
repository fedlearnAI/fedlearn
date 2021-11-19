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
package com.jdt.fedlearn.manager.service;


import com.jdt.fedlearn.common.constant.ResponseConstant;
import com.jdt.fedlearn.common.entity.*;
import com.jdt.fedlearn.common.enums.ResultTypeEnum;
import com.jdt.fedlearn.common.enums.RunStatusEnum;
import com.jdt.fedlearn.common.enums.TaskTypeEnum;
import com.jdt.fedlearn.common.enums.WorkerCommandEnum;
import com.jdt.fedlearn.tools.GZIPCompressUtil;
import com.jdt.fedlearn.tools.WorkerCommandUtil;
import com.jdt.fedlearn.tools.network.impl.HttpClientImpl;
import com.jdt.fedlearn.tools.serializer.JsonUtil;
import com.jdt.fedlearn.tools.serializer.JavaSerializer;
import com.jdt.fedlearn.manager.util.ConfigUtil;
import com.jdt.fedlearn.manager.spring.SpringBean;

import mockit.Mock;
import mockit.MockUp;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;

@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({SpringBean.class, WorkerCommandUtil.class})
public class TaskManagerTest {
    private TaskManager taskManager;
    private static List<Task> taskList;
    private static final int total = 5;
    private static List<String> taskIdList;
    JavaSerializer serializer = new JavaSerializer();

    @BeforeClass
    public void setUp() {
//        Config config = new Config("src/test/resources/conf/manager.properties", "");
//        PowerMockito.whenNew(Config.class).withArguments(Mockito.anyString(), Mockito.anyString()).thenReturn(config);
        ConfigUtil.init("src/test/resources/conf/manager.properties");
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(SpringBean.class);
        taskManager = (TaskManager) applicationContext.getBean("taskManager");

        taskList = new ArrayList<>();
        taskIdList = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            JobReq jobReq = new JobReq();
            jobReq.setJobId("test" + i);
            Job job = new Job(jobReq, new JobResult());
            Task task;
            if (i == total - 1) {
                task = new Task(job, RunStatusEnum.READY, TaskTypeEnum.FINISH);
            } else {
                task = new Task(job, RunStatusEnum.READY, TaskTypeEnum.INIT);
            }
            taskIdList.add(task.getTaskId());
            System.out.printf("jobId = %s,taskId = %s%n", task.getJob().getJobReq().getJobId(), task.getTaskId());
            taskList.add(task);
        }
        taskManager.addTasks(taskList);
    }

    @Test
    public void addTasks() {
        List<Task> tasks = taskManager.addTasks(taskList);
        Assert.assertEquals(tasks.size(), total);
        Assert.assertEquals(tasks, taskList);
    }

    @Test
    public void updateTaskRunStatus() {
        Random r = new Random();
        int index = r.nextInt(total);
        System.out.println("随机下标 = " + index);
        JobReq jobReq = new JobReq();
        jobReq.setJobId("test" + index);
        Job job = new Job(jobReq, new JobResult());
        Task task = new Task(job, RunStatusEnum.RUNNING, TaskTypeEnum.INIT);
        task.setTaskId(taskIdList.get(index));
        Task res = taskManager.updateTaskRunStatus(task);
        Assert.assertEquals(res.getRunStatusEnum(), RunStatusEnum.RUNNING);
    }


    @Test
    public void getFinishTaskForJob() {
        Random r = new Random();
        int i = r.nextInt(total);
        JobReq jobReq = new JobReq();
        jobReq.setJobId("test" + i);
        Job job = new Job(jobReq, new JobResult());
        Task finishTaskForJob = taskManager.getFinishTaskForJob(job);
        if (i == total - 1) {
            Assert.assertEquals(finishTaskForJob.getTaskId(), taskIdList.get(i));
        } else {
            Assert.assertNull(finishTaskForJob);
        }
    }

    @Test
    public void getTasksForJob() {
        Random r = new Random();
        int i = r.nextInt(total);
        JobReq jobReq = new JobReq();
        jobReq.setJobId("test" + i);
        Job job = new Job(jobReq, new JobResult());
        List<Task> tasks = taskManager.getTasksForJob(job);
        Assert.assertEquals(tasks.get(0).getTaskId(), taskIdList.get(i));
    }

    @Test
    public void removeTasksForJob() {
        Random r = new Random();
        int i = r.nextInt(total);
        JobReq jobReq = new JobReq();
        jobReq.setJobId("test" + i);
        Job job = new Job(jobReq, new JobResult());
        taskManager.removeTasksForJob(job);
        List<Task> tasksForJob = taskManager.getTasksForJob(job);
        Assert.assertEquals(tasksForJob.size(), 0);
    }

    @Test
    public void scheduler() throws InterruptedException {
        MockPostData();
        taskManager.scheduler();
        Thread.sleep(3000L);
    }

    @Test
    public void stop() {
        System.out.println(Runtime.getRuntime().availableProcessors());
    }

    private void MockPostData() {
        new MockUp<HttpClientImpl>() {
            @Mock
            public String sendAndRecv(String uri, Object content) {
                if(uri.contains(WorkerCommandEnum.IS_READY.getCode())){
                    CommonResultStatus commonResultStatus = new CommonResultStatus();
                    Map<String,Object> data = new HashMap<>();
                    data.put(ResponseConstant.DATA,"true");
                    commonResultStatus.setData(data);
                    commonResultStatus.setResultTypeEnum(ResultTypeEnum.SUCCESS);
                    String s = JsonUtil.object2json(commonResultStatus);
                    return GZIPCompressUtil.compress(s);
                }else if(uri.contains(WorkerCommandEnum.RUN_TASK.getCode())){
                    CommonResultStatus commonResultStatus = new CommonResultStatus();
                    Map<String,Object> data = new HashMap<>();
                    data.put(ResponseConstant.DATA,"true");
                    commonResultStatus.setData(data);
                    commonResultStatus.setResultTypeEnum(ResultTypeEnum.SUCCESS);
                    String s = JsonUtil.object2json(commonResultStatus);
                    return GZIPCompressUtil.compress(s);
                }
                return null;
            }
        };
    }
}
