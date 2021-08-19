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
import com.jdt.fedlearn.common.util.TimeUtil;
import com.jdt.fedlearn.common.util.TokenUtil;
import com.jdt.fedlearn.coordinator.allocation.ResourceManager;
import com.jdt.fedlearn.coordinator.dao.db.TrainMapper;
import com.jdt.fedlearn.coordinator.entity.table.TrainInfo;
import com.jdt.fedlearn.coordinator.entity.train.StartValues;
import com.jdt.fedlearn.coordinator.entity.train.TrainContext;
import com.jdt.fedlearn.coordinator.entity.train.StartTrain;
import com.jdt.fedlearn.coordinator.service.CommonService;
import com.jdt.fedlearn.coordinator.service.TrainService;
import com.jdt.fedlearn.core.type.AlgorithmType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 开始模型训练的实现类，支持多线程训练
 * <p>{@code start}方法请求开始训练，开始训练的时候回生成一个{@code modelToken}并更新全局上下文将训练状态设置为READY，开启一个线程进行模型训练</p>
 * @see com.jdt.fedlearn.coordinator.allocation.MultiTrain
 * @author lijingxi
 */
public class TrainStartServiceImpl implements TrainService {
    private static final Logger logger = LoggerFactory.getLogger(TrainStartServiceImpl.class);

    @Override
    public Map<String, Object> service(String content) {
        Map<String, Object> modelMap = new HashMap<>();
        try {
            StartTrain subRequest = new StartTrain(content);
            modelMap = start(subRequest);
            return modelMap;
        } catch (Exception ex) {
            //TODO 将能处理的异常在try里面处理，没有见过的异常再进入catch
            if (CommonService.exceptionProcess(ex, modelMap) == null) {
                throw ex;
            }
        }
        return CommonService.fail(StringUtils.EMPTY);
    }

    /**
     * 请求开始训练，开始训练的时候回生成一个{@code modelToken}并更新全局上下文将训练状态设置为READY，开启一个线程进行模型训练
     * @param req 请求体
     * @return modelMap
     */
    public static Map<String, Object> start(StartTrain req) {
        Map<String, Object> res = new HashMap<>();
        //同一 taskId 下，只支持单一任务训练
        String taskId = req.getTaskId();
        AlgorithmType supportedAlgorithm = AlgorithmType.valueOf(req.getModel());
        String modelToken = TokenUtil.generateTrainId(taskId, supportedAlgorithm);

        StartValues values = TrainCommonServiceImpl.startPrepare(req);
        //TODO 将用户选择的参数持久化存储
        TrainInfo trainInfo = new TrainInfo(modelToken, supportedAlgorithm, req.getAlgorithmParams(), System.currentTimeMillis());
        TrainMapper.insertTrainInfo(trainInfo);
        // 更新全局上下文, 并将状态设置为 READY
        TrainContext trainContext = new TrainContext(values, RunningType.READY, TimeUtil.getNowTime(), req.getAlgorithmParams());
        trainContext.setPercent(5);
        TrainCommonServiceImpl.trainContextMap.put(modelToken, trainContext);
        ResourceManager.submitTrain(modelToken); // 开启一个新线程
        res.put("modelToken", modelToken);
        Map<String, Object> modelMap = new HashMap<>();
        modelMap.put(ResponseConstant.DATA, res);
        modelMap.put(ResponseConstant.STATUS, ResponseConstant.SUCCESS);
        modelMap.put(ResponseConstant.CODE, ResponseConstant.SUCCESS_CODE);
        return modelMap;
    }

}
