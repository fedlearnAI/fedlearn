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

import com.jdt.fedlearn.common.constant.JdChainConstant;
import com.jdt.fedlearn.common.enums.RunningType;
import com.jdt.fedlearn.coordinator.entity.jdchain.JdChainTaskStatus;
import com.jdt.fedlearn.common.entity.SingleParameter;
import com.jdt.fedlearn.coordinator.entity.train.StartValues;
import com.jdt.fedlearn.coordinator.network.SendAndRecv;
import com.jdt.fedlearn.coordinator.service.train.jdchain.ChainTrainCommonServiceImpl;
import com.jdt.fedlearn.core.dispatch.common.DispatcherFactory;
import com.jdt.fedlearn.core.dispatch.common.Control;
import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.common.entity.core.feature.Features;
import com.jdt.fedlearn.core.parameter.HyperParameter;
import com.jdt.fedlearn.core.parameter.common.CommonParameter;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 内部类 用于异步的去执行区块链训练
 *
 * @author geyan29
 * @author fanmingjie
 **/
public class ChainMultiTrain implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final String modelToken;

    public ChainMultiTrain(String modelToken) {
        this.modelToken = modelToken;
    }

    @Override
    public void run() {
        JdChainTaskStatus jdChainTaskStatus = ChainTrainCommonServiceImpl.queryStatusByJdChain(modelToken);
        assert jdChainTaskStatus != null;
        StartValues values = jdChainTaskStatus.getTrainContext().getValues();
        AlgorithmType supportedAlgorithm = values.getSupportedAlgorithm();
        List<ClientInfo> clientInfos = values.getClientInfos();
        Map<ClientInfo, Features> features = new HashMap<>();
        IntStream.range(0, values.getFeature().size()).forEach(i ->
                features.put(clientInfos.get(i), values.getFeature().get(i))
        );
        MatchResult idMap = values.getIdMap();
        Map<String, Object> algorithmParamMap = values.getParameter().stream().collect(Collectors.toMap(SingleParameter::getField, SingleParameter::getValue));
        HyperParameter hyperParameter = CommonParameter.parseParameter(algorithmParamMap, supportedAlgorithm);

        Control algorithm = DispatcherFactory.getDispatcher(supportedAlgorithm, hyperParameter);
        try {
            List<CommonRequest> requests;
            int p = 0;
            Map<String, Object> others = new HashMap<>();
            others.put("splitRatio", "0.7");
            requests = algorithm.initControl(clientInfos, idMap, features, others);
            RunningType nowStatus = RunningType.RUNNING;
            //phase=0 对应客户端只加载数据 返回的事initSuccess
            requests.forEach(r -> r.setSync(true)); //将请求设为同步请求
            //计数 保证发送的请求数和接受的请求数一致
            String key = modelToken + JdChainConstant.SEPARATOR + p;
            String reqNum = key + JdChainConstant.SEPARATOR + requests.size();
            SendAndRecv.broadcastTrain(requests, modelToken, supportedAlgorithm, nowStatus, reqNum, values.getDataset());
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }
}

