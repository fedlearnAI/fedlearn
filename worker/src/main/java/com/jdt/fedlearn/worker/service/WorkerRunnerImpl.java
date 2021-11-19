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

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.jdt.fedlearn.common.constant.ResponseConstant;
import com.jdt.fedlearn.tools.TimeUtil;
import com.jdt.fedlearn.worker.runner.Runner;
import com.jdt.fedlearn.common.entity.CommonResultStatus;
import com.jdt.fedlearn.common.entity.Task;
import com.jdt.fedlearn.common.enums.BusinessTypeEnum;
import com.jdt.fedlearn.common.enums.ExceptionEnum;
import com.jdt.fedlearn.common.enums.ResultTypeEnum;
import com.jdt.fedlearn.common.enums.TaskTypeEnum;
import com.jdt.fedlearn.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: liuzhaojun10
 * @Date: 2020/8/26 09:42
 * @Description:
 */
@Component("workerRunner")
public class WorkerRunnerImpl implements WorkerRunner {

    private static final Logger logger = LoggerFactory.getLogger(WorkerRunnerImpl.class);
    //executor table
    private Table<BusinessTypeEnum, TaskTypeEnum, Runner> runnerTable = HashBasedTable.create();
    // worker是否ready 和manager的lock进行配合，确认salve是否可以运行task
//    在多线程下面， 用的是同一个workerRunner， 为了避免这种情况 改为map。
//    private Map<Integer, Boolean> isReadyMap = new HashMap<>();


    public WorkerRunnerImpl() {
    }

    /**
     * Gets the value of runnerTable.
     *
     * @return the value of runnerTable
     */
    public Table<BusinessTypeEnum, TaskTypeEnum, Runner> getRunnerTable() {
        return runnerTable;
    }

    @Override
    public Boolean isReady(int localPort) {
//        synchronized (isReadyMap) {
//            if (!isReadyMap.containsKey(localPort)) {
//                isReadyMap.put(localPort, true);
//            }
//            return isReadyMap.get(localPort);
//
//        }
        return true;
    }

    /**
     * 增加自动以的runner
     *
     * @param businessTypeEnum
     * @param taskTypeEnum
     * @param runner
     * @return
     */
    @Override
    public Runner addRunner(BusinessTypeEnum businessTypeEnum, TaskTypeEnum taskTypeEnum, Runner runner) {
        return runnerTable.put(businessTypeEnum, taskTypeEnum, runner);
    }

    @Override
    public CommonResultStatus run(Task task) {
        logger.info("start to run task {} in {}", task.getTaskId(), task.getWorkerUnit());
        CommonResultStatus commonResultStatus = new CommonResultStatus();
        commonResultStatus.setStartTime(TimeUtil.getNowTime());
        CommonResultStatus result = null;
        try {
//            if ( isReadyMap.get(localPort) == false) {
//                throw new BusinessException("worker " + task.getWorkerUnit() + "is not ready， which can not run task ", ExceptionEnum.ARCH_ERROR);
//            }
//            isReadyMap.put(localPort, false);
            BusinessTypeEnum businessTypeEnum = task.getJob().getJobReq().getBusinessTypeEnum();

            if (businessTypeEnum == null) {
                businessTypeEnum = BusinessTypeEnum.FED_LEARNING;
            }
            Runner runner = runnerTable.get(businessTypeEnum, task.getTaskTypeEnum());
            if (runner == null) {
                throw new BusinessException("invalid task type: " + task.getTaskTypeEnum(), ExceptionEnum.ARCH_ERROR);
            }
            result = runner.run(task);
        } catch (Exception e) {
            logger.error("worker runner 运行异常", e);
            commonResultStatus.setResultTypeEnum(ResultTypeEnum.BUS_FAIL);
            Map<String, Object> map = new HashMap<>();
            map.put(ResponseConstant.MESSAGE, e.getMessage());
            map.put(ResponseConstant.CODE, ResultTypeEnum.BUS_FAIL.getCode());
            commonResultStatus.setData(map);
            commonResultStatus.setEndTime(TimeUtil.getNowTime());
            return commonResultStatus;
        } finally {
//            isReadyMap.put(localPort, true);
            logger.info("end to run task {} in {}", task.getTaskId(), task.getWorkerUnit());
        }
        return result;

    }


}
