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

package com.jdt.fedlearn.coordinator.allocation;

import com.jdt.fedlearn.common.enums.RunningType;
import com.jdt.fedlearn.common.entity.SingleParameter;
import com.jdt.fedlearn.common.util.FileUtil;
import com.jdt.fedlearn.coordinator.entity.table.TrainInfo;
import com.jdt.fedlearn.coordinator.entity.train.StartValues;
import com.jdt.fedlearn.coordinator.entity.train.TrainContext;
import com.jdt.fedlearn.coordinator.exception.UnknownInterfaceException;
import com.jdt.fedlearn.coordinator.network.SendAndRecv;
import com.jdt.fedlearn.core.dispatch.common.DispatcherFactory;
import com.jdt.fedlearn.core.entity.common.MetricValue;
import com.jdt.fedlearn.core.exception.NotMatchException;
import com.jdt.fedlearn.core.parameter.common.CommonParameter;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.coordinator.service.train.TrainCommonServiceImpl;
import com.jdt.fedlearn.core.dispatch.common.Control;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.core.entity.common.CommonResponse;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.parameter.SuperParameter;
import com.jdt.fedlearn.coordinator.dao.db.TrainMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import com.jdt.fedlearn.core.psi.MatchResult;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 多线程训练支持，
 * 正常的训练过程为 ready --> initial -- training -- complete -- validate
 * 允许训练暂停和恢复训练，以及终止训练等，
 * 提交训练前，需要把训练所需数据放入 TrainContext，并将训练状态设置为需要开始的状态
 * <p>
 * 进入子线程前，percent=5, init 完成后 percent=7, 主训练流程中，percent每轮+=1,
 */
public class MultiTrain implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(MultiTrain.class);
    private final String modelToken;

    public MultiTrain(String modelToken) {
        this.modelToken = modelToken;
    }

    private void updateTrainContext(TrainContext context) {
        TrainCommonServiceImpl.trainContextMap.put(modelToken, context);
    }

    private void updatePercentAndMetrics(MetricValue metricValue) {
        //update task status TODO 加入启发式训练状态更新，即根据任务总迭代次数，单词迭代时间等，保证进度条均匀
        TrainContext trainContext = TrainCommonServiceImpl.trainContextMap.get(modelToken);
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
            TrainCommonServiceImpl.trainContextMap.put(modelToken, trainContext);
        } catch (Exception e) {
            logger.error("update train Status error!!", e);
        }
    }

    private void trainInit() {
        //从全局变量读取上下文
        TrainContext context = TrainCommonServiceImpl.trainContextMap.get(modelToken);

        //训练初始化过程
        StartValues startValues = context.getValues();
        AlgorithmType algorithmType = startValues.getSupportedAlgorithm();

        Map<String, Object> algorithmParamMap = startValues.getParameter().stream().collect(Collectors.toMap(SingleParameter::getField, SingleParameter::getValue));
        SuperParameter superParameter = CommonParameter.parseParameter(algorithmParamMap, algorithmType);

        List<ClientInfo> clientInfos = startValues.getClientInfos();
        MatchResult idMap = startValues.getIdMap();
        Map<ClientInfo, Features> features = new HashMap<>();
        IntStream.range(0, startValues.getFeature().size()).forEach(i ->
                features.put(clientInfos.get(i), startValues.getFeature().get(i))
        );

        Control dispatcher = DispatcherFactory.getDispatcher(algorithmType, superParameter);
        Map<String, Object> others = new HashMap<>();
        List<AlgorithmType> needDistributedKeys = Arrays.asList(AlgorithmType.MixGBoost, AlgorithmType.LinearRegression);
        if (needDistributedKeys.contains(algorithmType)) {
            String content = FileUtil.loadClassFromFile("/export/data/pubkey");
            others.put("pubKeyStr", content);
        }
        if (algorithmParamMap.containsKey("crossValidation")) {
            double spiltRatio = Double.parseDouble(algorithmParamMap.get("crossValidation").toString());
            others.put("splitRatio", spiltRatio);
        } else {
            others.put("splitRatio", 1.0);
        }
        // requests包含TrainInit
        List<CommonRequest> requests = dispatcher.initControl(clientInfos, idMap, features, others);

        //更新上下文并保存到全局参数，后续已无需 StartValues，此处可将其设置为 null
        context.setRunningType(RunningType.RUNNING);
        context.updateRequestsAndDispatcher(requests, dispatcher);
        context.setPercent(7);
        updateTrainContext(context);
    }

    private void trainMain() {
        //从全局变量读取上下文
        TrainContext context = TrainCommonServiceImpl.trainContextMap.get(modelToken);
        Control algorithm = context.getDispatcher();
        List<CommonRequest> requests = context.getRequests();
        AlgorithmType algorithmType = algorithm.getAlgorithmType();

        while (algorithm.isContinue()) {
            //中断条件判断及处理
            if (isInterrupted()) {
                processInterrupt(requests, algorithmType, algorithm);
                break;
            }
            //todo 保证给各客户端发送的状态一致 dataset只发一次
            List<CommonResponse> responses = SendAndRecv.broadcastTrain(requests, modelToken, algorithmType, RunningType.RUNNING, "", context.getValues().getDataset());
            requests = algorithm.control(responses);
            //更新状态
            updatePercentAndMetrics(algorithm.readMetrics());
        }
        if (!isInterrupted()) {
            //更新全局变量
            context.updateRequestsAndDispatcher(requests, algorithm);
            updateTrainContext(context);
        }
    }

    /**
     * TODO 此处需要重新设计，
     */
    private void trainAfter() {
        if (isInterrupted()) {
            return;
        }
        TrainContext context = TrainCommonServiceImpl.trainContextMap.get(modelToken);
        List<CommonRequest> requests = context.getRequests();
        AlgorithmType algorithmType = context.getDispatcher().getAlgorithmType();

        //TODO 判断save 和 notifyClient 的返回状态
        notifyClient(requests, RunningType.COMPLETE, algorithmType);


        context.setPercent(100);
        context.setRunningType(RunningType.COMPLETE);

        TrainCommonServiceImpl.trainContextMap.put(modelToken, context);
        persistSave(context);
        removeTrainProcessResult();
    }

    @Override
    public void run() {
        // 从全局状态读取训练所需状态数据和运行到的步骤, 包含id对齐结果
        TrainContext context = TrainCommonServiceImpl.trainContextMap.get(modelToken);
        RunningType runningType = context.getRunningType();

        //只有是ready 或者 running 状态时，才能开始运行。
        // ready状态意味着是初始化提交请求，running状态意味着恢复训练
        if (RunningType.READY.equals(runningType)) {
            trainInit();
            trainMain();
        } else if (RunningType.RUNNING.equals(runningType)) {
            trainMain();
        } else {
            throw new NotMatchException("不满足运行条件");
        }
        trainAfter();
    }

    /**
     * 判断是否中断,所有非 Running状态都是中断
     *
     * @return 是否中断
     */
    private boolean isInterrupted() {
        if (!TrainCommonServiceImpl.trainContextMap.containsKey(modelToken)) {
            return true;
        }
        TrainContext context = TrainCommonServiceImpl.trainContextMap.get(modelToken);
        RunningType trainStatus = context.getRunningType();
        return RunningType.STOP.equals(trainStatus)
                || RunningType.SUSPEND.equals(trainStatus);
    }

    /**
     * 中断状态处理
     */
    private void processInterrupt(List<CommonRequest> requests, AlgorithmType algorithmType, Control algorithm) {
        TrainContext context = TrainCommonServiceImpl.trainContextMap.get(modelToken);
        RunningType nowStatus = context.getRunningType();
        if (RunningType.STOP.equals(nowStatus)) {
            //通知客户端并移除服务端全局状态
            notifyClient(requests, RunningType.STOP, algorithmType);
            persistSave(context);
            removeTrainProcessResult();
        } else if (RunningType.SUSPEND.equals(nowStatus)) {
            //更新全局变量
            context.updateRequestsAndDispatcher(requests, algorithm);
            updateTrainContext(context);
            //通知客户端状态
            notifyClient(requests, RunningType.SUSPEND, algorithmType);
            persistSave(context);
        } else {
            //exception
            throw new UnknownInterfaceException("未知中断状态 ！");
        }
    }

    //TODO 实际不需要发送这么多参数
    private void notifyClient(List<CommonRequest> requests, RunningType type, AlgorithmType algorithmType) {
        for (CommonRequest request : requests) {
            String response = SendAndRecv.send(request.getClient(), modelToken, request.getPhase(), algorithmType, request.getBody(), type);
            if (RunningType.STOP.getRunningType().equals(response)) {
                logger.info("{} stop is success", request.getClient());
            }
            if (RunningType.COMPLETE.getRunningType().equals(response)) {
                logger.info("{} train is complete!!!", request.getClient());
            }
        }
    }

    private boolean persistSave(TrainContext context) {
        //TODO 优化写入数据库
        TrainInfo trainInfo = new TrainInfo(modelToken, context.getMetrics(), context.getRunningType(), context.getPercent());
        TrainMapper.updateTrainInfo(trainInfo);

        return true;
    }

    /**
     * 移除训练中间运行结果
     */
    private void removeTrainProcessResult() {
        TrainCommonServiceImpl.trainContextMap.remove(modelToken);
    }
}

