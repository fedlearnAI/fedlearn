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

import com.jd.blockchain.ledger.TransactionResponse;
import com.jd.blockchain.ledger.TypedKVEntry;
import com.jdt.fedlearn.common.constant.JdChainConstant;
import com.jdt.fedlearn.common.entity.jdchain.JdchainTask;
import com.jdt.fedlearn.common.enums.RunningType;
import com.jdt.fedlearn.tools.TimeUtil;
import com.jdt.fedlearn.tools.serializer.JsonUtil;
import com.jdt.fedlearn.coordinator.dao.jdchain.ChainTaskMapper;
import com.jdt.fedlearn.coordinator.dao.jdchain.ChainTrainMapper;
import com.jdt.fedlearn.coordinator.entity.jdchain.JdChainTaskStatus;
import com.jdt.fedlearn.coordinator.entity.jdchain.JdchainTrainInfo;
import com.jdt.fedlearn.coordinator.entity.train.TrainContext;
import com.jdt.fedlearn.coordinator.exception.jdchain.RandomServerException;
import com.jdt.fedlearn.coordinator.exception.jdchain.StartTrainException;
import com.jdt.fedlearn.coordinator.util.JdChainUtils;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.core.entity.common.MetricValue;
import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;
import com.jdt.fedlearn.core.entity.serialize.JsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class ChainTrainCommonServiceImpl {
    private static final Logger logger = LoggerFactory.getLogger(ChainTrainCommonServiceImpl.class);
    private static final JsonSerializer jsonSerializer = new JsonSerializer();
    /**
     * @param modelToken
     * @param jdChainTaskStatus
     * @className ChainTrainStartServiceImpl
     * @description: 将训练过程中的状态数据放到链上
     * @return: void
     * @author: geyan29
     * @date: 2021/01/13 11:56
     **/
    public static void putStatus2JdChain(String modelToken, JdChainTaskStatus jdChainTaskStatus) {
        String status = jsonSerializer.serialize(jdChainTaskStatus);
        TransactionResponse transactionResponse = JdChainUtils.invokeStarttraining(modelToken, JdChainConstant.STATUS_SUFFIX, status);
        if (!transactionResponse.isSuccess()) {
            throw new StartTrainException("train status put jdchain error！");
        }
        logger.info("train status put jdchain success");
    }

    /**
     * @className ForwardController
     * @description: 查询训练的状态信息用于渲染页面
     * @param modelToken
     * @return:
     * @author: geyan29
     * @date: 2020/12/22 11:01 上午
     **/
    private static final String PARAMS = "params";
    public static JdChainTaskStatus queryStatusByJdChain(String modelToken) {
        String fname = JdChainConstant.INVOKE_START_TRAINING;
        String queryKey = fname + JdChainConstant.SEPARATOR + JdChainConstant.SERVER +
                JdChainConstant.SEPARATOR + modelToken + JdChainConstant.SEPARATOR + JdChainConstant.STATUS_SUFFIX;
        TypedKVEntry typedKVEntry = JdChainUtils.queryByChaincode(queryKey);
        JdChainTaskStatus taskStatus = new JdChainTaskStatus();
        if(typedKVEntry != null){
            String result = (String) typedKVEntry.getValue();
            Map map = JsonUtil.json2Object(result,Map.class);
            String params = (String) map.get(PARAMS);
             taskStatus = (JdChainTaskStatus) jsonSerializer.deserialize(params);
        }
        return taskStatus;
    }

    /**
     * @param metricValue
     * @className ChainTrainRandomServiceImpl
     * @description:训练每个phase结束后，更新metric、进度等信息
     * @return: void
     * @author: geyan29
     * @date: 2021/01/08 16:35
     **/
    public static void updateMetricValue(String modelToken, MetricValue metricValue) {
        JdChainTaskStatus jdChainTaskStatus = ChainTrainCommonServiceImpl.queryStatusByJdChain(modelToken);
        assert jdChainTaskStatus != null;
        TrainContext trainContext = jdChainTaskStatus.getTrainContext();
        int percent = trainContext.getPercent();
        MetricValue current = trainContext.getMetrics();
        try {
            if (percent < 95) {
                percent += 1;
            }
            logger.info("modelToken=【{}】,进度:【{}】", modelToken, percent);
            if (current == null || !current.equals(metricValue)) {
                current = metricValue;
            }
            trainContext.updatePercentAndMetrics(percent, current);
            jdChainTaskStatus.setModifyTime(TimeUtil.getNowTime());
            ChainTrainCommonServiceImpl.putStatus2JdChain(modelToken, jdChainTaskStatus);
        } catch (Exception e) {
            logger.error("updateTaskStatus error!!" + e.getMessage());
        }
    }



    public static void updateRequetsAndRunningType(String modelToken, List<CommonRequest> requests, RunningType runningType) {
        JdChainTaskStatus jdChainTaskStatus = ChainTrainCommonServiceImpl.queryStatusByJdChain(modelToken);
        JdchainTrainInfo trainInfo = ChainTrainMapper.queryModelById(modelToken);
        AlgorithmType supportedAlgorithm = AlgorithmType.valueOf(trainInfo.getAlgorithm());
        JdchainTask jdchainTask = ChainTaskMapper.queryById(trainInfo.getTaskId());
        assert jdChainTaskStatus != null;
        TrainContext trainContext = jdChainTaskStatus.getTrainContext();
        try {
            trainContext.setRequests(requests);
            trainContext.setRunningType(runningType);
            jdChainTaskStatus.setModifyTime(TimeUtil.getNowTime());
            ChainTrainCommonServiceImpl.putStatus2JdChain(modelToken, jdChainTaskStatus);
            JdchainTrainInfo trainInfoNew = new JdchainTrainInfo(modelToken, trainInfo.getTaskId(), supportedAlgorithm.name(), trainInfo.getParameterFieldList(),
                    TimeUtil.parseStrToData(jdChainTaskStatus.getStartTime()), TimeUtil.parseStrToData(jdChainTaskStatus.getModifyTime()),
                    jdchainTask.getTaskName(), jdchainTask.getPartners(), jdchainTask.getUsername(), runningType, trainInfo.getPercent(), trainInfo.getMetrics());
            /* 都存入训练信息 便于查询训练列表及训练详情复杂处理*/
            ChainTrainMapper.insertTrainInfo(trainInfoNew);
        } catch (Exception e) {
            logger.error("updateTaskStatus error!!" + e.getMessage());
        }
    }



    /**
     * @param modelToken
     * @className ChainTrainStartServiceImpl
     * @description:从所有注册的server中，选举一个server作为下次发起训练的server
     * @return: void
     * @author: geyan29
     * @date: 2021/01/08 15:48
     **/
    public static void randomServerByJdchain(String modelToken) {
        //随机一个server
        String username = JdChainConstant.SERVER;
        //1,随机服务端发起训练
        TransactionResponse response = JdChainUtils.invokeRandomtraining(username, modelToken, modelToken);
        logger.info("random server success ? {}", response.isSuccess());
        if (!response.isSuccess()) {
            throw new RandomServerException("random server error, please check it");
        }
    }


}
