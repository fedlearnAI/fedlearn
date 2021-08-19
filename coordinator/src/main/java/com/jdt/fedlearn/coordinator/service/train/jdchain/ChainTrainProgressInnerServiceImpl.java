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

import com.jdt.fedlearn.coordinator.entity.jdchain.JdChainTaskStatus;
import com.jdt.fedlearn.coordinator.entity.train.CommonTrainQuery;
import com.jdt.fedlearn.coordinator.entity.train.TrainContext;
import com.jdt.fedlearn.coordinator.entity.train.TrainStatus;
import com.jdt.fedlearn.coordinator.service.AbstractDispatchService;
import com.jdt.fedlearn.coordinator.service.TrainService;
import com.jdt.fedlearn.coordinator.service.train.TrainStatusServiceImpl;
import com.jdt.fedlearn.core.entity.common.MetricValue;

import java.util.Map;

/**
 * @className: ChainTrainProgressInnerServiceImpl
 * @description: 查询训练进度信息
 * @author: geyan29
 * @createTime: 2021/1/29 5:00 下午
 */
public class ChainTrainProgressInnerServiceImpl implements TrainService {

    @Override
    public Map<String, Object> service(String content) {
        boolean flag = true;
        CommonTrainQuery subRequest = new CommonTrainQuery();
        subRequest.parseJson(content);
        TrainStatus trainStatus = getTrainProgress(subRequest);
        if (trainStatus == null) {
            flag = false;
        }
        return new AbstractDispatchService() {
            @Override
            public Object dealService() {
                return trainStatus;
            }
        }.doProcess(flag);
    }

    public TrainStatus getTrainProgress(CommonTrainQuery subRequest){
        String token = subRequest.getModelToken();
        JdChainTaskStatus jdChainTaskStatus = ChainTrainCommonServiceImpl.queryStatusByJdChain(token);
        TrainContext trainContext = jdChainTaskStatus.getTrainContext();
        TrainStatus trainStatus = new TrainStatus();
        trainStatus.setMessage(TrainStatusServiceImpl.OK);
        trainStatus.setPercent(trainContext.getPercent());
        trainStatus.setRunningType(trainContext.getRunningType());
        MetricValue metricValue = trainContext.getMetrics();
        if (metricValue == null) {
            trainStatus.setTrainMetrics(null);
            trainStatus.setValidationMetrics(null);
            trainStatus.setFeatureImportance(null);
            trainStatus.setBestRound(0);
        } else {
            trainStatus.setTrainMetrics(TrainStatusServiceImpl.getCacheMetric(trainContext.getMetrics(), TrainStatusServiceImpl.TRAIN));
            trainStatus.setValidationMetrics(TrainStatusServiceImpl.getCacheMetric(trainContext.getMetrics(), TrainStatusServiceImpl.VALIDATION));
            trainStatus.setFeatureImportance(TrainStatusServiceImpl.getCacheFeatureMetric(trainContext.getMetrics()));
        }
        return trainStatus;
    }
}
