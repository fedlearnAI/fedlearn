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
import com.jdt.fedlearn.common.constant.ResponseConstant;
import com.jdt.fedlearn.common.entity.*;
import com.jdt.fedlearn.common.enums.*;
import com.jdt.fedlearn.common.exception.BusinessException;
import com.jdt.fedlearn.common.util.TimeUtil;
import com.jdt.fedlearn.manager.ManagerLocalApp;
import com.jdt.fedlearn.manager.service.TaskManager;
import com.jdt.fedlearn.manager.task.Executor;
import com.jdt.fedlearn.common.util.WorkerCommandUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Description: 1. 确认上次执行task，
 * 2. 获取执行结果
 * 3. 设置jobresult
 * 4. 清除缓存
 * 5. notify job ，返回job结果。
 */
@Component("finishExecutor")
public class FinishExecutorImpl implements Executor {

    private static Logger logger = LoggerFactory.getLogger(FinishExecutorImpl.class);

    @Resource
    private TaskManager taskManager;

    @Resource
    private ManagerLocalApp managerLocalApp;

    /**
     * 服务器执行下一个任务
     *
     * @param task 任务
     */
    @Override
    public CommonResultStatus run(Task task) {

        CommonResultStatus commonResultStatus = new CommonResultStatus();
        logger.info("执行finish 计算， {}", task.getTaskId());
        Task finishTask;
        Job job = task.getJob();

        if (task.getTaskTypeEnum() != TaskTypeEnum.FINISH) {
            logger.warn("执行非finish任务的finish工作, {}", task.getTaskId());
            finishTask = taskManager.getFinishTaskForJob(task.getJob());
        } else {
            finishTask = task;
        }

        if (finishTask != null) {

            try {
                if (CollectionUtils.isEmpty(finishTask.getPreTaskList())) {
                    BusinessException exception =
                            new BusinessException("无效的DAG，需要依赖一个上游节点 ， 当前任务uid为 " + finishTask.getTaskId(), ExceptionEnum.DAG_ERROR);
                    logger.error("无效的任务", exception);
                    throw exception;
                }

                logger.info("检查前置任务是否执行完毕，判断是否为异常任务终端");
                for (Task preTask : finishTask.getPreTaskList()) {
                    if (preTask.getRunStatusEnum() != RunStatusEnum.SUCCESS) {
                        BusinessException exception =
                                new BusinessException(" 前置任务" + preTask.getTaskId() + " 没有执行完毕 for " + finishTask.getTaskId(), ExceptionEnum.DAG_ERROR);
                        logger.error("无效的任务", exception);
                        throw exception;
                    }
                }
                logger.info("获取计算结果: ");
                //获取最后一个计算结果
                Task lastTask = finishTask.getPreTaskList().get(0);
                TaskResultData preData = WorkerCommandUtil.processTaskRequestData(
                        lastTask, WorkerCommandEnum.GET_TASK_RESULT, TaskResultData.class);

                if(preData == null){
                    throw new RuntimeException("WorkerCommandUtil.processTaskRequestData error");
                }
                commonResultStatus.setResultTypeEnum(ResultTypeEnum.SUCCESS);
                finishTask.setRunStatusEnum(RunStatusEnum.SUCCESS);

                buildJobResult(finishTask, preData);

            } catch (Exception e) {
                logger.error("execute finish job fail, ", e);
                commonResultStatus.setResultTypeEnum(ResultTypeEnum.OTHER_FAIL);
                job.getJobResult().setResultTypeEnum(ResultTypeEnum.BUS_FAIL);
                job.getJobResult().getData().put(ResponseConstant.MESSAGE, e.getMessage());
            }
        }
        logger.info("通知job执行完成，返回job执行结果 {}", job.getJobReq().getJobId());
        synchronized (job) {
            job.notifyAll();
        }
        logger.info("清除缓存和任务列表");
        //清除缓存数据
        try {
            clear(job);
        } catch (Exception e) {
            logger.warn("清除任务失败", e);
        }
        return commonResultStatus;

    }


    /**
     * 任务结束 清除相关信息
     *
     * @param job 任务
     */
    public void clear(Job job) {

        List<Task> taskList = managerLocalApp.getTaskManager().getTasksForJob(job);
        //清除worker中间运算结果
        try {
            for (Task task : taskList) {
                WorkerCommandUtil.processTaskRequestData(task, WorkerCommandEnum.CLEAR_TASK_CACHE, TaskResultData.class);
            }
        } catch (Exception e) {
            logger.error("清除task cache 有错", e);
        }

        //清除taskManager中的任务
        managerLocalApp.getTaskManager().removeTasksForJob(job);

        //清除jobManager任务
        managerLocalApp.getJobManager().removeJob(job);
    }


    /**
     * 构建jobResult
     *
     * @param task    任务
     * @param preData 任务结果数据
     * @return 任务结果
     */
    public JobResult buildJobResult(Task task, TaskResultData preData) {
        JobResult jobResult = task.getJob().getJobResult();
        if (task.getRunStatusEnum() == RunStatusEnum.SUCCESS) {
            logger.info("封装返回data from ", preData.getTaskId());
            jobResult.getData().putAll(preData.getModelMap());

            jobResult.setResultTypeEnum(ResultTypeEnum.SUCCESS);
        } else {
            jobResult.setResultTypeEnum(ResultTypeEnum.BUS_FAIL);
        }
        jobResult.setEndTime(TimeUtil.getNowTime());
        return jobResult;

    }

}
