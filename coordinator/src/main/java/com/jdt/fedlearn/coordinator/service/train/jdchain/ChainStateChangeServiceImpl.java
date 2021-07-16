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

package com.jdt.fedlearn.coordinator.service.train.jdchain;

import com.jdt.fedlearn.common.constant.JdChainConstant;
import com.jdt.fedlearn.common.constant.ResponseConstant;
import com.jdt.fedlearn.coordinator.dao.jdchain.ChainTrainMapper;
import com.jdt.fedlearn.coordinator.entity.jdchain.JdChainTaskStatus;
import com.jdt.fedlearn.coordinator.entity.jdchain.JdchainTrainInfo;
import com.jdt.fedlearn.coordinator.entity.train.StateChangeSignal;
import com.jdt.fedlearn.coordinator.entity.train.TrainContext;
import com.jdt.fedlearn.coordinator.network.SendAndRecv;
import com.jdt.fedlearn.coordinator.service.CommonService;
import com.jdt.fedlearn.coordinator.service.TrainService;
import com.jdt.fedlearn.coordinator.type.RunningType;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.core.exception.NotMatchException;
import com.jdt.fedlearn.core.type.AlgorithmType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 区块链版本暂停/停止和恢复，
 * 训练开始后信息主要存储在，区块链上，
 * 其中，停止训练较为简单，只需后续不再迭代即可
 * 暂停和恢复设计到协调端选举，此处设定为暂停前后使用同一个协调端
 */
public class ChainStateChangeServiceImpl implements TrainService {
    private static final Logger logger = LoggerFactory.getLogger(ChainStateChangeServiceImpl.class);

    public static final String ERROR_MSG = "重启异常";
    public static final String SUCCESS_MSG = "重启成功";
    public static final String STOP_SUCCESS = "停止成功";
    public static final String STOP_FAIL = "停止异常，任务不存在";
    public static final String SUSPEND_SUCCESS = "暂停成功";
    public static final String SUSPEND_FAIL = "暂停异常，任务不存在";

    @Override
    public Map<String, Object> service(String content) throws Exception {
        Map<String, Object> resultMap = new HashMap<>();
        try {
            StateChangeSignal subRequest = new StateChangeSignal(content);
            switch (subRequest.getType()) {
                case "stop":
                    return stopTrain(subRequest.getModelToken());
                case "resume":
                    return resumeTrain(subRequest.getModelToken());
                case "suspend":
                    return suspendTrain(subRequest.getModelToken());
                default:
                    throw new NotMatchException("illegal change state type:" + subRequest.getType());
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
        JdChainTaskStatus jdChainTaskStatus = ChainTrainCommonServiceImpl.queryStatusByJdChain(modelId);
        JdchainTrainInfo jdchainTrainInfo = ChainTrainMapper.queryModelById(modelId);
        if (jdChainTaskStatus == null || jdchainTrainInfo == null) {
            logger.info("训练任务不存在");
            modelMap.put(ResponseConstant.STATUS, STOP_FAIL);
            modelMap.put(ResponseConstant.CODE, ResponseConstant.FAIL_CODE);
            return modelMap;
        }
        assert false;
        ChainTrainMapper.updateStatusAndTrainInfo(modelId, jdChainTaskStatus, jdchainTrainInfo, RunningType.STOP);
        RunningType old = ChainTrainCommonServiceImpl.queryStatusByJdChain(modelId).getTrainContext().getRunningType();
        logger.info("modelToken:" + modelId + "，执行状态：" + old);
        logger.info("task:训练终止 , modelToken: {}", modelId);
        modelMap.put(ResponseConstant.STATUS, STOP_SUCCESS);
        modelMap.put(ResponseConstant.CODE, ResponseConstant.SUCCESS_CODE);
        return modelMap;
    }


    /**
     * 根据输入的模型id，将该模型状态修改为暂停状态，
     *
     * @param modelId 模型标识码
     * @return 暂停是否成功
     */
    public Map<String, Object> suspendTrain(String modelId) {
        Map<String, Object> resultMap = new HashMap<>();
        JdChainTaskStatus jdChainTaskStatus = ChainTrainCommonServiceImpl.queryStatusByJdChain(modelId);
        JdchainTrainInfo jdchainTrainInfo = ChainTrainMapper.queryModelById(modelId);
        if (jdChainTaskStatus != null && jdchainTrainInfo != null) {
            ChainTrainMapper.updateStatusAndTrainInfo(modelId, jdChainTaskStatus, jdchainTrainInfo, RunningType.SUSPEND);
            TrainContext trainContext = ChainTrainCommonServiceImpl.queryStatusByJdChain(modelId).getTrainContext();
            RunningType old = trainContext.getRunningType();
            logger.info("modelToken:" + modelId + "，执行状态：" + old);
            resultMap.put(ResponseConstant.STATUS, SUSPEND_SUCCESS);
            resultMap.put(ResponseConstant.CODE, ResponseConstant.SUCCESS_CODE);
        } else {
            logger.info("任务不存在");
            resultMap.put(ResponseConstant.STATUS, SUSPEND_FAIL);
            resultMap.put(ResponseConstant.CODE, ResponseConstant.FAIL_CODE);
        }
        return resultMap;
    }

    /**
     * @param modelId 训练唯一id
     * @return
     */
    public Map<String, Object> resumeTrain(String modelId) {
        Map<String, Object> resultMap = new HashMap<>();
        JdChainTaskStatus jdChainTaskStatus = ChainTrainCommonServiceImpl.queryStatusByJdChain(modelId);
        JdchainTrainInfo jdchainTrainInfo = ChainTrainMapper.queryModelById(modelId);
        if (jdChainTaskStatus != null && jdchainTrainInfo != null) {
            logger.info("in chain");
        } else {
            // 缓存和数据库都不存在
            logger.info("任务不存在");
            resultMap.put(ResponseConstant.STATUS, ERROR_MSG);
            resultMap.put(ResponseConstant.CODE, ResponseConstant.FAIL_CODE);
            return resultMap;
        }
        ChainTrainMapper.updateStatusAndTrainInfo(modelId, jdChainTaskStatus, jdchainTrainInfo, RunningType.RUNNING);
        submitResume(modelId,jdChainTaskStatus,jdchainTrainInfo,RunningType.RUNNING);
        resultMap.put(ResponseConstant.STATUS, SUCCESS_MSG);
        resultMap.put(ResponseConstant.CODE, ResponseConstant.SUCCESS_CODE);
        return resultMap;
    }


    private void submitResume(String modelToken, JdChainTaskStatus jdChainTaskStatus, JdchainTrainInfo jdchainTrainInfo, RunningType runningType) {
        //准备好后，将状态标记为 RUNNING 重新提交训练
        AlgorithmType supportedAlgorithm = AlgorithmType.valueOf(jdchainTrainInfo.getAlgorithm());
        TrainContext trainContext = jdChainTaskStatus.getTrainContext();
        List<CommonRequest> requests = trainContext.getRequests();
        String key = modelToken + JdChainConstant.SEPARATOR + requests.get(0).getPhase(); //此次发送的phase的key
        requests.forEach(r -> r.setSync(true));
        String reqNum = key + JdChainConstant.SEPARATOR + requests.size();//记录发送的请求数
        SendAndRecv.broadcastTrain(requests, modelToken, supportedAlgorithm, runningType, reqNum);
    }

}
