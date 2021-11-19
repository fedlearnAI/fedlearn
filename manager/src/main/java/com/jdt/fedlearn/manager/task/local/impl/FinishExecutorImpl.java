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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.common.constant.AppConstant;
import com.jdt.fedlearn.common.constant.CacheConstant;
import com.jdt.fedlearn.common.constant.ResponseConstant;
import com.jdt.fedlearn.common.entity.*;
import com.jdt.fedlearn.common.enums.*;
import com.jdt.fedlearn.common.exception.BusinessException;
import com.jdt.fedlearn.common.intf.IAlgorithm;
import com.jdt.fedlearn.tools.TimeUtil;
import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;
import com.jdt.fedlearn.manager.ManagerLocalApp;
import com.jdt.fedlearn.manager.service.TaskManager;
import com.jdt.fedlearn.manager.task.Executor;
import com.jdt.fedlearn.tools.WorkerCommandUtil;
import com.jdt.fedlearn.manager.util.ConfigUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                TrainRequest trainRequest = lastTask.getSubRequest();
                String workerProperties = ConfigUtil.getWorkerProperties();
                String[] workers = StringUtils.split(workerProperties, AppConstant.SPLIT);
                queryUpdateTrainResult(lastTask, preData, trainRequest, workers);
                deleteMessage(trainRequest, workers);
                if (preData == null) {
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
     * 处理reduce结果的模型部分，更新模型和重新保存response
     *
     * 查询reduce的结果为本次训练最终结果response
     * 根据查询到的response更新各model
     * 更新缓存中的trainResult
     *
     * @param lastTask 最后一个任务
     * @param preData 任务结果
     * @param trainRequest 任务请求
     * @param workers workers信息
     * @throws JsonProcessingException
     */
    private void queryUpdateTrainResult(Task lastTask, TaskResultData preData, TrainRequest trainRequest, String[] workers) throws JsonProcessingException {
        if (trainRequest != null && trainRequest.getPhase() != 0 && !RunningType.COMPLETE.equals(trainRequest.getStatus())) {
            logger.info("phase :{}", trainRequest.getPhase());
            logger.info("enter update subModel");
            String stempData = ((Map<String, String>) preData.getModelMap().get(IAlgorithm.DATA)).get(IAlgorithm.DATA);
            String url = WorkerCommandUtil.buildUrl(lastTask.getWorkerUnit());
            long s1 = System.currentTimeMillis();
            CommonResultStatus commonResultStatus1 = WorkerCommandUtil.request(url + AppConstant.SLASH, WorkerCommandEnum.API_TRAIN_RESULT_QUERY, stempData);
            long e1 = System.currentTimeMillis();
            logger.info("finish API_TRAIN_RESULT_QUERY cost：{}", (e1 - s1));
            String modelResult = (String) commonResultStatus1.getData().get(ResponseConstant.DATA);
            // 取出最后一个reduce的结果，发送给全部worker更新model
            CommonResultStatus ResponseStatus = null;
            if (modelResult != null && !"".equals(modelResult)) {
                for (String worker : workers) {
                    logger.info("正在更新的worker：{}", worker);
                    Map<String, Object> objectMap = new HashMap<>();
                    objectMap.put("modelToken", trainRequest.getModelToken());
                    objectMap.put("subModel", modelResult);
                    long s2 = System.currentTimeMillis();
                    ResponseStatus = WorkerCommandUtil.request(AppConstant.HTTP_PREFIX + worker + AppConstant.SLASH, WorkerCommandEnum.API_SUB_MODEL_UPDATE, objectMap);
                    long e2 = System.currentTimeMillis();
                    logger.info("finish API_SUB_MODEL_UPDATE cost：{}", (e2 - s2));
                }
                String lastRes = (String) ResponseStatus.getData().get(ResponseConstant.DATA);
                ObjectMapper mapper = new ObjectMapper();
                Map json = mapper.readValue(stempData, Map.class);
                String stamp = (String) json.get("stamp");
                Map<String, Object> objectMap = new HashMap<>();
                objectMap.put("stamp", stamp);
                objectMap.put("strMessage", lastRes);
                long s3 = System.currentTimeMillis();
                WorkerCommandUtil.request(url + AppConstant.SLASH, WorkerCommandEnum.API_TRAIN_RESULT_UPDATE, objectMap);
                long e3 = System.currentTimeMillis();
                logger.info("finish API_TRAIN_RESULT_UPDATE cost：{}", (e3 - s3));
            }
        }
    }

    /**
     * 删除缓存的message
     *
     * @param trainRequest 训练请求
     * @param workers worker信息
     */
    private void deleteMessage(TrainRequest trainRequest, String[] workers) {
        if (trainRequest != null && trainRequest.getAlgorithm().equals(AlgorithmType.DistributedFederatedGB) && trainRequest.getModelToken() != null && trainRequest.getPhase() != 0) {
            String modelMessageDataKey = CacheConstant.getModelAddressKey(trainRequest.getModelToken(), String.valueOf(trainRequest.getPhase()));
            for (String worker : workers) {
                long s4 = System.currentTimeMillis();
                WorkerCommandUtil.request(AppConstant.HTTP_PREFIX + worker + AppConstant.SLASH, WorkerCommandEnum.API_MESSAGE_DATA_DELETE, modelMessageDataKey);
                long e4 = System.currentTimeMillis();
                logger.info("finish API_MESSAGE_DATA_DELETE cost：{}", (e4 - s4));
            }
        }
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
            logger.info("封装返回data from {}", preData.getTaskId());
            jobResult.getData().putAll(preData.getModelMap());

            jobResult.setResultTypeEnum(ResultTypeEnum.SUCCESS);
        } else {
            jobResult.setResultTypeEnum(ResultTypeEnum.BUS_FAIL);
        }
        jobResult.setEndTime(TimeUtil.getNowTime());
        return jobResult;

    }

}
