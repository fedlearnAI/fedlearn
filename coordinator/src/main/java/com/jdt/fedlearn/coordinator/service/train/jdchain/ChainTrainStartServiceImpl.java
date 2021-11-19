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

import com.google.common.collect.Maps;
import com.jdt.fedlearn.common.constant.ResponseConstant;
import com.jdt.fedlearn.common.entity.SingleParameter;
import com.jdt.fedlearn.common.entity.jdchain.JdchainTask;
import com.jdt.fedlearn.common.enums.RunningType;
import com.jdt.fedlearn.coordinator.allocation.ResourceManager;
import com.jdt.fedlearn.coordinator.entity.jdchain.JdchainTrainInfo;
import com.jdt.fedlearn.coordinator.entity.train.*;
import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;
import com.jdt.fedlearn.coordinator.dao.jdchain.ChainTaskMapper;
import com.jdt.fedlearn.coordinator.dao.jdchain.ChainTrainMapper;
import com.jdt.fedlearn.coordinator.entity.jdchain.JdChainTaskStatus;
import com.jdt.fedlearn.coordinator.service.CommonService;
import com.jdt.fedlearn.coordinator.service.TrainService;
import com.jdt.fedlearn.coordinator.service.train.TrainCommonServiceImpl;
import com.jdt.fedlearn.tools.TimeUtil;
import com.jdt.fedlearn.tools.TokenUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


/**
 * 区块链版本开始训练
 *
 * @author geyan29
 * @author fanmingjie
 * @version 0.8.2
 **/
public class ChainTrainStartServiceImpl implements TrainService {
    private static final Logger logger = LoggerFactory.getLogger(ChainTrainStartServiceImpl.class);

    @Override
    public Map<String, Object> service(String content) {
        Map<String, Object> modelMap = Maps.newHashMap();
        try {
            logger.info("content:" + content);
            StartTrain subRequest = new StartTrain(content);
            String modelToken = trainStart(subRequest);
            Map<String, Object> res = new HashMap<>();
            res.put("modelToken", modelToken);
            modelMap.put(ResponseConstant.DATA, res);
            modelMap.put(ResponseConstant.STATUS, ResponseConstant.SUCCESS);
            modelMap.put(ResponseConstant.CODE, ResponseConstant.SUCCESS_CODE);
            return modelMap;
        } catch (Exception ex) {
            if (CommonService.exceptionProcess(ex, modelMap) == null) {
                throw ex;
            }
        }
        return CommonService.fail(StringUtils.EMPTY);
    }

    /**
     * @param req 请求 区块链版本的开始训练
     * @author geyan29
     * @author fanmingjie
     **/
    private String trainStart(StartTrain req) {
        StartValues values = TrainCommonServiceImpl.startPrepare(req);
        String modelToken = TokenUtil.generateTrainId(req.getTaskId(), AlgorithmType.valueOf(req.getModel()).getAlgorithm());
        List<SingleParameter> algorithmParams = req.getAlgorithmParams();
//        algorithmParams.add(req.getCommonParams().get(0));
        // 更新全局上下文, 并将状态设置为 READY
        TrainContext trainContext = new TrainContext(values, RunningType.READY, TimeUtil.getNowTime(), algorithmParams);
        trainContext.setPercent(5);

        JdChainTaskStatus jdChainTaskStatus = new JdChainTaskStatus(TimeUtil.getNowTime(), TimeUtil.getNowTime(), trainContext);
        //训练过程数据放入jdchain
        ChainTrainCommonServiceImpl.putStatus2JdChain(modelToken, jdChainTaskStatus);
        //随机一个server
        ChainTrainCommonServiceImpl.randomServerByJdchain(modelToken);
        //训练
        ResourceManager.submitChainTrain(modelToken);
        /* 开始训练后保存JdchainTrainInfo 用于查询训练过程中的信息 */
        JdchainTask jdchainTask = ChainTaskMapper.queryById(req.getTaskId());
        JdchainTrainInfo trainInfo = new JdchainTrainInfo(modelToken, req.getTaskId(), req.getModel(), trainContext.getParameterFieldList(),
                TimeUtil.parseStrToData(jdChainTaskStatus.getStartTime()), TimeUtil.parseStrToData(jdChainTaskStatus.getModifyTime()),
                jdchainTask.getTaskName(), jdchainTask.getPartners(), jdchainTask.getUsername(), RunningType.RUNNING, trainContext.getPercent(), trainContext.getMetrics());
//        ChainTrainRandomServiceImpl.chainTrainContextMap.put(modelToken, trainInfo);
        ChainTrainMapper.insertTrainInfo(trainInfo);
        return modelToken;
    }

}

