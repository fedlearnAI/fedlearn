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

import com.jdt.fedlearn.common.constant.ResponseConstant;
import com.jdt.fedlearn.common.intf.IAlgorithm;
import com.jdt.fedlearn.common.util.TimeUtil;
import com.jdt.fedlearn.worker.cache.WorkerResultCache;
import com.jdt.fedlearn.worker.runner.Runner;
import com.jdt.fedlearn.worker.service.AlgorithmService;
import com.jdt.fedlearn.worker.service.WorkerRunner;
import com.jdt.fedlearn.common.entity.CommonResultStatus;
import com.jdt.fedlearn.common.entity.Task;
import com.jdt.fedlearn.common.entity.TaskResultData;
import com.jdt.fedlearn.common.enums.*;
import com.jdt.fedlearn.common.util.WorkerCommandUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Thread.sleep;

/**
 * @Author:liuzhaojun10
 * @Date: 2020/8/20 13:09
 * @Description: reduce执行
 */
@Component
public class ReduceRunnerImpl implements Runner {


    private final static Logger logger = LoggerFactory.getLogger(ReduceRunnerImpl.class);
    @Resource
    private WorkerResultCache workerResultCache;

    @Resource
    private WorkerRunner workerRunner;

    /**
     * 在runner 中心注册服务
     */
    @PostConstruct
    @Override
    public void register() {
        workerRunner.addRunner(BusinessTypeEnum.FED_LEARNING, TaskTypeEnum.REDUCE, this);
    }

    /**
     * 服务器执行下一个任务
     *
     * @param task
     */
    @Override
    public CommonResultStatus run(Task task) {
        TaskResultData taskResultData = new TaskResultData();
        CommonResultStatus commonResultStatus = new CommonResultStatus();
        commonResultStatus.setEndTime(TimeUtil.getNowTime());
        commonResultStatus.setResultTypeEnum(ResultTypeEnum.SUCCESS);
        if(task.getSubRequest().getStatus().equals(RunningType.COMPLETE)){
            Map<String, Object> modelMap = new HashMap<>();
            modelMap.put(ResponseConstant.STATUS, ResponseConstant.SUCCESS);
            modelMap.put(ResponseConstant.CODE, ResponseConstant.SUCCESS_CODE);
            modelMap.put(ResponseConstant.DATA, RunningType.COMPLETE.getRunningType());
            //存储结果
            taskResultData.setTaskId(task.getTaskId());
            taskResultData.setTaskResultStatus(commonResultStatus);
            taskResultData.getModelMap().put(ResponseConstant.DATA, modelMap);
            logger.info("save result data");
            workerResultCache.put(taskResultData);
            return commonResultStatus;
        }
        Object finalResult;
        List<Task> preTaskList = task.getPreTaskList();
        List<Object> result = new ArrayList<>();

        //获取原数据 可能是map的结果 也可能是reduce的结果
        for (Task preTask : preTaskList) {
            TaskResultData preData = WorkerCommandUtil.processTaskRequestData(preTask, WorkerCommandEnum.GET_TASK_RESULT, TaskResultData.class);
            if(preData != null){
                final Map<String, Object> data = (Map<String, Object>)preData.getModelMap().get(IAlgorithm.DATA);
                result.add(data);
                logger.debug("get pre data {} ", preData.getTaskId());
            }
        }

        AlgorithmService algorithmService = new AlgorithmService();
        finalResult = algorithmService.reduce(result,task);
        logger.info("run reduce {}", task.getTaskId());

        //存储结果
        taskResultData.setTaskId(task.getTaskId());
        taskResultData.setTaskResultStatus(commonResultStatus);
        taskResultData.getModelMap().put(ResponseConstant.DATA, finalResult);
        logger.info("save result data");
        workerResultCache.put(taskResultData);

        return commonResultStatus;
    }


}
