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

package com.jdt.fedlearn.coordinator.service.train;

import com.jdt.fedlearn.common.constant.ResponseConstant;
import com.jdt.fedlearn.common.enums.RunningType;
import com.jdt.fedlearn.coordinator.allocation.ResourceManager;
import com.jdt.fedlearn.coordinator.dao.UniversalMapper;
import com.jdt.fedlearn.coordinator.entity.table.TrainInfo;
import com.jdt.fedlearn.coordinator.entity.train.StateChangeSignal;
import com.jdt.fedlearn.coordinator.entity.train.TrainContext;
import com.jdt.fedlearn.coordinator.service.CommonService;
import com.jdt.fedlearn.coordinator.service.TrainService;
import com.jdt.fedlearn.core.exception.NotMatchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author wangpeiqi
 * @version 0.8.2
 * 训练状态变更
 */
public class StateChangeServiceImpl implements TrainService {
    private static final Logger logger = LoggerFactory.getLogger(StateChangeServiceImpl.class);

    private static final String ERROR_MSG = "重启异常";
    private static final String SUCCESS_MSG = "重启成功";
    private static final String STOP_SUCCESS = "停止成功";
    private static final String STOP_FAIL = "停止异常，任务不存在";
    private static final String SUSPEND_SUCCESS = "暂停成功";
    private static final String SUSPEND_FAIL = "暂停异常，任务不存在";

    @Override
    public Map<String, Object> service(String content) throws Exception {
        Map<String, Object> resultMap = new HashMap<>();
        try {
            StateChangeSignal subRequest = new StateChangeSignal(content);
            RunningType runningType = RunningType.valueOf(subRequest.getType().toUpperCase());
            switch (runningType) {
                case STOP:
                    return stopTrain(subRequest.getModelToken());
                case RESUME:
                    return resumeTrain(subRequest.getModelToken());
                case SUSPEND:
                    return suspendTrain(subRequest.getModelToken());
                default:
                    throw new NotMatchException("");
            }
        } catch (Exception e) {
            if (CommonService.exceptionProcess(e, resultMap) == null) {
                throw e;
            }
        }
        return resultMap;
    }

    /**
     * 停止任务，只需要将任务状态 修改为 STOP 即可。
     *
     * @param modelId 模型唯一id
     * @return 终止是否成功
     */
    public Map<String, Object> stopTrain(String modelId) {
        Map<String, Object> modelMap = new HashMap<>();

        if (!TrainCommonServiceImpl.isExist(modelId)) {
            logger.info("训练任务不存在");
            modelMap.put(ResponseConstant.STATUS, STOP_FAIL);
            modelMap.put(ResponseConstant.CODE, ResponseConstant.FAIL_CODE);
            return modelMap;
        }
        TrainCommonServiceImpl.updateRunningType(modelId, RunningType.STOP);
        logger.info("task:训练终止 , modelToken: {}", modelId);
        modelMap.put(ResponseConstant.STATUS, STOP_SUCCESS);
        modelMap.put(ResponseConstant.CODE, ResponseConstant.SUCCESS_CODE);
        return modelMap;
    }

    /**
     * @param modelId 训练唯一id
     * @return
     */
    public Map<String, Object> resumeTrain(String modelId) {
        Map<String, Object> resultMap = new HashMap<>();
        // 检查该训练是否存在
        if (TrainCommonServiceImpl.isExist(modelId)) {
            logger.info("in memory");
        } else if (UniversalMapper.isModelExist(modelId)) {
            // 从数据库加载训练状态
            logger.info("从数据库中加载训练状态 ");
            TrainInfo trainInfo = UniversalMapper.getModelToken(modelId);
            TrainContext trainContext = new TrainContext(trainInfo.getRunningType(), String.valueOf(trainInfo.getTrainStartTime()), trainInfo.getPercent(), trainInfo.getMetricInfo(), trainInfo.getHyperParameter());
            TrainCommonServiceImpl.trainContextMap.put(modelId, trainContext);
        } else {
            // 缓存和数据库都不存在
            logger.info("任务不存在");
            resultMap.put(ResponseConstant.STATUS, ERROR_MSG);
            resultMap.put(ResponseConstant.CODE, ResponseConstant.FAIL_CODE);
            return resultMap;
        }
        //准备好后，将状态标记为 RUNNING 重新提交训练
        TrainContext trainContext = TrainCommonServiceImpl.trainContextMap.get(modelId);
        trainContext.setRunningType(RunningType.RUNNING);
        ResourceManager.submitTrain(modelId);
        resultMap.put(ResponseConstant.STATUS, SUCCESS_MSG);
        resultMap.put(ResponseConstant.CODE, ResponseConstant.SUCCESS_CODE);
        return resultMap;
    }


    /**
     * 根据输入的模型id，将该模型状态修改为暂停状态，
     *
     * @param modelId 模型标识码
     * @return 暂停是否成功
     */
    public Map<String, Object> suspendTrain(String modelId) {
        Map<String, Object> resultMap = new HashMap<>();
        if (TrainCommonServiceImpl.isExist(modelId)) {
            TrainCommonServiceImpl.updateRunningType(modelId, RunningType.SUSPEND);
            RunningType old = TrainCommonServiceImpl.trainContextMap.get(modelId).getRunningType();
            logger.info("modelToken:{}，执行状态：{}",modelId,old);
            resultMap.put(ResponseConstant.STATUS, SUSPEND_SUCCESS);
            resultMap.put(ResponseConstant.CODE, ResponseConstant.SUCCESS_CODE);
        } else {
            logger.info("任务不存在");
            resultMap.put(ResponseConstant.STATUS, SUSPEND_FAIL);
            resultMap.put(ResponseConstant.CODE, ResponseConstant.FAIL_CODE);
        }
        return resultMap;
    }
}
