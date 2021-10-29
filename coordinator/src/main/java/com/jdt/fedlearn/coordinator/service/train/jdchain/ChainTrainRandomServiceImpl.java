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
import com.jdt.fedlearn.common.constant.AppConstant;
import com.jdt.fedlearn.common.constant.JdChainConstant;
import com.jdt.fedlearn.common.entity.jdchain.JdchainTask;
import com.jdt.fedlearn.common.enums.RunningType;
import com.jdt.fedlearn.common.util.JsonUtil;
import com.jdt.fedlearn.common.util.TimeUtil;
import com.jdt.fedlearn.coordinator.entity.jdchain.JdChainTrainRequest;
import com.jdt.fedlearn.coordinator.entity.jdchain.JdchainTrainInfo;
import com.jdt.fedlearn.coordinator.entity.train.TrainContext;
import com.jdt.fedlearn.coordinator.exception.UnknownInterfaceException;
import com.jdt.fedlearn.coordinator.network.SendAndRecv;
import com.jdt.fedlearn.core.dispatch.common.Control;
import com.jdt.fedlearn.core.dispatch.common.DispatcherFactory;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.base.SingleElement;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.core.entity.common.CommonResponse;
import com.jdt.fedlearn.core.entity.serialize.JavaSerializer;
import com.jdt.fedlearn.core.entity.serialize.Serializer;
import com.jdt.fedlearn.core.parameter.HyperParameter;
import com.jdt.fedlearn.core.parameter.common.CommonParameter;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.coordinator.dao.jdchain.ChainTaskMapper;
import com.jdt.fedlearn.coordinator.dao.jdchain.ChainTrainMapper;
import com.jdt.fedlearn.coordinator.entity.jdchain.JdChainTaskStatus;
import com.jdt.fedlearn.common.entity.SingleParameter;
import com.jdt.fedlearn.coordinator.service.CommonService;
import com.jdt.fedlearn.coordinator.service.TrainService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * @className: ChainTrainRandomServiceImpl
 * @description: 接收client发送的训练调用
 * @author: geyan29
 * @createTime: 2021/1/29 5:01 下午
 */
public class ChainTrainRandomServiceImpl implements TrainService {
    private static final Logger logger = LoggerFactory.getLogger(ChainTrainRandomServiceImpl.class);
    public static volatile Map<String, List<CommonResponse>> PHASE_RESPONSE_MAP = new ConcurrentHashMap<>(64);
    private static final Serializer serializer = new JavaSerializer();
    private static ExecutorService fixedThreadPool = Executors.newFixedThreadPool(10);


    @Override
    public Map<String, Object> service(String content) {
        Map<String, Object> modelMap = Maps.newHashMap();
        try {
            randomTrain(content);
            return modelMap;
        } catch (Exception ex) {
            if (CommonService.exceptionProcess(ex, modelMap) == null) {
                throw ex;
            }
        }
        return CommonService.fail(StringUtils.EMPTY);
    }

    /**
     * @param content
     * @className TrainRandomStartServiceImpl
     * @description:mock 接收客户端的请求并进行下一步训练
     * @return: void
     * @author: geyan29
     * @date: 2021/01/07 15:29
     **/
    private void randomTrain(String content) {
        JdChainTrainRequest jdChainTrainRequest = JsonUtil.json2Object(content,JdChainTrainRequest.class);
        ClientInfo clientInfo = jdChainTrainRequest.getClientInfo();
        String modelToken = jdChainTrainRequest.getModelToken();
        String data = jdChainTrainRequest.getData();
        Message message;
        if (AppConstant.INIT_SUCCESS.equals(data)) {
            message = new SingleElement(data);
        } else {
            message = serializer.deserialize(data);
        }
        int p = jdChainTrainRequest.getPhase();
        String requestNum = jdChainTrainRequest.getReqNum();
        String key = modelToken + JdChainConstant.SEPARATOR + p;
        CommonResponse commonResponse = new CommonResponse(clientInfo, message);
        synchronized (PHASE_RESPONSE_MAP) {
            if (PHASE_RESPONSE_MAP.containsKey(key)) {
                List<CommonResponse> responses = PHASE_RESPONSE_MAP.get(key);
                responses.add(commonResponse);
                PHASE_RESPONSE_MAP.put(key, responses);
            } else {
                List<CommonResponse> responses = new ArrayList<>();
                responses.add(commonResponse);
                PHASE_RESPONSE_MAP.put(key, responses);
                return;
            }
        }
        //判断当前phase是否都返回了
        logger.info("requestNum={},key={}", requestNum, key);
        int count = Integer.parseInt(requestNum.substring(requestNum.lastIndexOf(JdChainConstant.SEPARATOR) + 1));
        if (requestNum.contains(key) && count == PHASE_RESPONSE_MAP.get(key).size()) {
            //选举一个server 用于client下一个phase 必须保证多个client都已经返回才能进行
            ChainTrainCommonServiceImpl.randomServerByJdchain(modelToken);
            logger.info("all response success ，phase = {}", p);
            JdchainTrainInfo trainInfo = ChainTrainMapper.queryModelById(modelToken);
            post2Client(modelToken, p, trainInfo);
        } else {
            logger.info("key = {} ,phase = {},request number = {} , response size= {}", key, p, count, PHASE_RESPONSE_MAP.get(key).size());
        }

    }

    /**
     * @param modelToken
     * @param p
     * @param trainInfo
     * @className TrainRandomStartServiceImpl
     * @description:mock 请求客户端发起下一个phase的训练
     * @return: void
     * @author: geyan29
     * @date: 2021/01/07 15:29
     **/
    public static void post2Client(String modelToken, int p, JdchainTrainInfo trainInfo) {
        String oldKey = modelToken + JdChainConstant.SEPARATOR + p;
        List<CommonResponse> responses = PHASE_RESPONSE_MAP.get(oldKey);
        //删除当前key 一次训练相同的phase可能会有多次
        PHASE_RESPONSE_MAP.remove(oldKey);
//        StartTrain req = JsonUtil.json2Object((String) paramMap.get(JdChainConstant.REQUEST), StartTrain.class);
        AlgorithmType supportedAlgorithm = AlgorithmType.valueOf(trainInfo.getAlgorithm());
        List<SingleParameter> algorithmParams = trainInfo.getParameterFieldList();
        Map<String, Object> algorithmParamMap = algorithmParams.stream().collect(Collectors.toMap(SingleParameter::getField, SingleParameter::getValue));
        HyperParameter parameter = CommonParameter.parseParameter(algorithmParamMap, trainInfo.getAlgorithm());
        Control algorithm = DispatcherFactory.getDispatcher(supportedAlgorithm, parameter);

        List<CommonRequest> requests = algorithm.control(responses);
        boolean flag = algorithm.isContinue();
        ChainTrainCommonServiceImpl.updateMetricValue(modelToken, algorithm.readMetrics());
        if (flag) {
            if (isInterrupted(modelToken)) {
                processInterrupt(modelToken, requests);
            } else if (requests != null) {
                int phase = requests.get(0).getPhase();
                String key = modelToken + JdChainConstant.SEPARATOR + phase; //此次发送的phase的key
                requests.forEach(r -> r.setSync(true));
                String reqNum = key + JdChainConstant.SEPARATOR + requests.size();//记录发送的请求数
                /* 单起线程向client发送请求，避免client和master循环等待，导致http连接被消耗光*/
                fixedThreadPool.execute(() -> SendAndRecv.broadcastTrain(requests, modelToken, supportedAlgorithm, RunningType.RUNNING, reqNum));
            }
        } else {
            complete(modelToken,requests,trainInfo,p,supportedAlgorithm);
        }
    }

    /**
     * 判断是否中断,所有非 Running状态都是中断
     *
     * @return 是否中断
     */
    public static boolean isInterrupted(String modelToken) {
        JdChainTaskStatus jdChainTaskStatus = ChainTrainCommonServiceImpl.queryStatusByJdChain(modelToken);
        assert jdChainTaskStatus != null;
        TrainContext trainContext = jdChainTaskStatus.getTrainContext();
        RunningType trainStatus = trainContext.getRunningType();
        return RunningType.STOP.equals(trainStatus)
                || RunningType.SUSPEND.equals(trainStatus);
    }

    /**
     * 中断状态处理
     */
    public static void processInterrupt(String modelToken, List<CommonRequest> requests) {
        // TODO 是否缓存
        JdChainTaskStatus jdChainTaskStatus = ChainTrainCommonServiceImpl.queryStatusByJdChain(modelToken);
        assert jdChainTaskStatus != null;
        TrainContext trainContext = jdChainTaskStatus.getTrainContext();
        RunningType nowStatus = trainContext.getRunningType();
        logger.info("nowStatus " + nowStatus);
        if (RunningType.STOP.equals(nowStatus)) {
            ChainTrainCommonServiceImpl.updateRequetsAndRunningType(modelToken, requests, RunningType.STOP);
//            removeTrainProcessResult(modelToken);
        } else if (RunningType.SUSPEND.equals(nowStatus)) {
            //更新全局变量
            ChainTrainCommonServiceImpl.updateRequetsAndRunningType(modelToken, requests, RunningType.SUSPEND);
            //通知客户端状态
        } else {
            //exception
            throw new UnknownInterfaceException("未知中断状态 ！");
        }
    }

    /**
     * 训练结束相关操作
     * @param modelToken
     * @param requests
     * @param trainInfo
     * @param p
     * @param supportedAlgorithm
     */
    private static void complete(String modelToken, List<CommonRequest> requests, JdchainTrainInfo trainInfo, int p, AlgorithmType supportedAlgorithm) {
        JdChainTaskStatus jdChainTaskStatus = ChainTrainCommonServiceImpl.queryStatusByJdChain(modelToken);
        assert jdChainTaskStatus != null;
        TrainContext trainContextEnd = jdChainTaskStatus.getTrainContext();
        trainContextEnd.setPercent(100);
        trainContextEnd.setRunningType(RunningType.COMPLETE);
        for (CommonRequest request : requests) {//同步客户端 生成模型
            requests.forEach(r -> r.setSync(true));
            String response = SendAndRecv.send(request.getClient(), modelToken, p, supportedAlgorithm, request.getBody(), RunningType.COMPLETE);
            if (response.equals(RunningType.COMPLETE.getRunningType())) {
                logger.info(request.getClient() + " train is complete!!!");
            }
        }
        //前端查询，训练结束
        jdChainTaskStatus.setModifyTime(TimeUtil.getNowTime());
        JdchainTask jdchainTask = ChainTaskMapper.queryById(trainInfo.getTaskId());
        JdchainTrainInfo trainInfoNew = new JdchainTrainInfo(modelToken, trainInfo.getTaskId(), supportedAlgorithm.name(), trainContextEnd.getParameterFieldList(),
                TimeUtil.parseStrToData(jdChainTaskStatus.getStartTime()), TimeUtil.parseStrToData(jdChainTaskStatus.getModifyTime()),
                jdchainTask.getTaskName(), jdchainTask.getPartners(), jdchainTask.getUsername(), RunningType.COMPLETE, trainContextEnd.getPercent(), trainContextEnd.getMetrics());
        /* 都存入训练信息 便于查询训练列表及训练详情复杂处理*/
        ChainTrainMapper.insertTrainInfo(trainInfoNew);
        //保存状态信息
        ChainTrainCommonServiceImpl.putStatus2JdChain(modelToken, jdChainTaskStatus);
    }

}
