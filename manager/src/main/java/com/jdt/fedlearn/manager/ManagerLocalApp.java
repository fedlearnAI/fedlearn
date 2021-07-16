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
package com.jdt.fedlearn.manager;

import com.google.common.collect.Lists;
import com.jdt.fedlearn.common.entity.*;
import com.jdt.fedlearn.common.enums.ExceptionEnum;
import com.jdt.fedlearn.common.enums.RunStatusEnum;
import com.jdt.fedlearn.common.enums.TaskTypeEnum;
import com.jdt.fedlearn.common.exception.BusinessException;
import com.jdt.fedlearn.manager.service.JobManager;
import com.jdt.fedlearn.manager.service.WorkerManager;
import com.jdt.fedlearn.manager.service.TaskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;


/**
 * @Description: 本地主入口
 */
@Component
public class ManagerLocalApp {

    private static Logger logger = LoggerFactory.getLogger(ManagerLocalApp.class);

    @Resource
    private TaskManager taskManager;
    @Resource
    private WorkerManager workerManager;
    @Resource
    private JobManager jobManager;
    /**
     * 关闭停止循环
     */
    public void close() {
        taskManager.stop();
    }

    public JobResult process(JobReq jobReq) throws InterruptedException {

        Job job = jobManager.addJob(jobReq);
        synchronized (job) {
            logger.info("等待job执行完成， {}", job.getJobReq().getJobId());
            // 初始化task任务
            initTaskForJob(job);
            //必须锁一下, 暂时是同步的，不支持异步，后续启用
            while (job.getJobResult().getResultTypeEnum() == null) {
                job.wait();
            }
            logger.info("job执行完成, {}", job.getJobReq().getJobId());
        }
        switch (job.getJobResult().getResultTypeEnum()) {
            case BUS_FAIL:
                logger.info("业务处理结果出错, {}", job);
                break;
            case OTHER_FAIL:
                logger.info("框架处理结果出错, {}", job);
                break;
            case SUCCESS:
                logger.info("处理结果, {}", job.getJobResult());
                break;
            default:
                Exception exception = new BusinessException("位置状态异常", ExceptionEnum.UNIMPLEMENT);
                logger.error("未知job运行状态： {}" + job.getJobReq().getJobId(), exception);
        }
        return job.getJobResult();

    }

    private void initTaskForJob(Job job) {
        TaskTypeEnum taskTypeEnum=TaskTypeEnum.INIT;
        Task task = new Task(job, RunStatusEnum.INIT, taskTypeEnum);
        List<Task> tasks = Lists.newArrayList(task);
        taskManager.addTasks(tasks);
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public WorkerManager getWorkerManager() {
        return workerManager;
    }

    public JobManager getJobManager() {
        return jobManager;
    }

}
