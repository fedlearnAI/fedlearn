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

package com.jdt.fedlearn.coordinator.service.train.inner;


import com.jdt.fedlearn.common.constant.ResponseConstant;
import com.jdt.fedlearn.coordinator.entity.metric.Metric;
import com.jdt.fedlearn.coordinator.entity.metric.MetricPair;
import com.jdt.fedlearn.coordinator.entity.train.CommonTrainQuery;
import com.jdt.fedlearn.coordinator.entity.train.TrainProgressRes;
import com.jdt.fedlearn.coordinator.entity.train.TrainStatus;
import com.jdt.fedlearn.coordinator.service.CommonService;
import com.jdt.fedlearn.coordinator.service.TrainService;
import com.jdt.fedlearn.coordinator.service.train.TrainStatusServiceImpl;
import com.jdt.fedlearn.coordinator.service.train.jdchain.ChainTrainProgressInnerServiceImpl;
import com.jdt.fedlearn.coordinator.util.ConfigUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 单个任务训练进度（包括训练完成和训练失败的任务也可以查询）的实现类，仅供内部使用
 * 相对于提供给外部的API接口外，增加了描述信息
 *
 * @author lijingxi
 * @author fanmingjie
 * @see TrainStatusServiceImpl
 */
public class TrainProgressInnerServiceImpl implements TrainService {
    private static final Logger logger = LoggerFactory.getLogger(TrainProgressInnerServiceImpl.class);

    @Override
    public Map<String, Object> service(String content) {
        try {
            CommonTrainQuery subRequest = new CommonTrainQuery();
            subRequest.parseJson(content);
            return queryTrainProgress(subRequest);
        } catch (Exception ex) {
            //TODO 各种错误类型
            logger.error("StateChangeSignal error ", ex);
            return CommonService.fail(StringUtils.EMPTY);
        }
    }

    public Map<String, Object> queryTrainProgress(CommonTrainQuery signal) {
        boolean jdChainAvailable = ConfigUtil.getJdChainAvailable();
        TrainStatus trainStatus;
        //读取标准对外API的输出
        if(jdChainAvailable){
            ChainTrainProgressInnerServiceImpl trainService = new ChainTrainProgressInnerServiceImpl();
            trainStatus = trainService.getTrainProgress(signal);
        }else{
            TrainStatusServiceImpl trainService = new TrainStatusServiceImpl();
            trainStatus = trainService.getTrainProgress(signal);
        }
        //加入 fPercent, describes, 去除不支持的 metrics
        List<Metric> trainMetrics = trainStatus.getTrainMetrics();
        double percent = trainStatus.getPercent();
        List<String> describes = addDesc(percent, trainMetrics, signal.getModelToken());
        TrainProgressRes trainProgressRes = new TrainProgressRes(trainStatus.getPercent(), describes, trainStatus.getRunningType(), trainStatus.getMessage());
        trainProgressRes.setDescribes(describes);
        Map<String, Object> modelMap = new HashMap<>();
        modelMap.put(ResponseConstant.DATA, trainProgressRes);
        modelMap.put(ResponseConstant.STATUS, ResponseConstant.SUCCESS);
        modelMap.put(ResponseConstant.CODE, ResponseConstant.SUCCESS_CODE);

        return modelMap;
    }

    public static List<String> addDesc(double percent, List<Metric> metrics, String modelToken) {
        List<String> describes = new ArrayList<>();
        describes.add("modelToken is: " + modelToken);
        if (percent == 5) {
            describes.add("开始");
            return describes;
        }
        describes.add("开始");
        describes.add("参数初始化成功");
        if (percent == 7) {
            return describes;
        }
        if (metrics != null && metrics.size() > 0) {
            for (int j = 0; j < metrics.get(0).getMetric().size(); j++) {
                StringBuilder stringBuffer = new StringBuilder();
                for (int i = 0; i < metrics.size(); i++) {
                    Metric metric = metrics.get(i);
                    List<MetricPair> metricValue = metric.getMetric();
                    MetricPair tmpMetric = metricValue.get(j);
                    if (i == 0) {
                        stringBuffer.append(String.format("第%s轮，", tmpMetric.roundString()));
                    }
                    stringBuffer.append(metric.getName()).append(": ").append(tmpMetric.metricString());
                    if (i != metrics.size() - 1) {
                        stringBuffer.append("; ");
                    }
                }
                if (!describes.contains(stringBuffer.toString())) {
                    describes.add(" " + stringBuffer);
                }
            }
        }
        if (percent == 100) {
            describes.add("训练结束");
        }
        return describes;
    }


}
