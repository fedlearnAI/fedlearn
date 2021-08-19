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

import com.jdt.fedlearn.common.enums.RunningType;
import com.jdt.fedlearn.coordinator.entity.metric.*;
import com.jdt.fedlearn.coordinator.entity.table.TrainInfo;
import com.jdt.fedlearn.coordinator.entity.train.CommonTrainQuery;
import com.jdt.fedlearn.coordinator.entity.train.TrainContext;
import com.jdt.fedlearn.coordinator.entity.train.TrainStatus;
import com.jdt.fedlearn.coordinator.service.AbstractDispatchService;
import com.jdt.fedlearn.coordinator.service.TrainService;
import com.jdt.fedlearn.coordinator.dao.db.TrainMapper;
import com.jdt.fedlearn.core.entity.common.MetricValue;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.data.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.IntStream;

/**
 * 训练进度和指标更新
 **/
public class TrainStatusServiceImpl implements TrainService {
    private static final Logger logger = LoggerFactory.getLogger(TrainStatusServiceImpl.class);

    public static final String OK = "ok";
    public static final String TRAIN = "train";
    public static final String VALIDATION = "validation";
    public static final String FEATURE_IMPORTANCE = "featureImportance";

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

    /**
     * 查询并更新训练进度
     *
     * @param subRequest 查询请求
     * @return 训练状态
     */
    public TrainStatus getTrainProgress(CommonTrainQuery subRequest) {
        TrainStatus trainStatus = new TrainStatus();
        String token = subRequest.getModelToken();
        try {
            if (TrainCommonServiceImpl.trainContextMap.containsKey(token)) {
                TrainContext status = TrainCommonServiceImpl.trainContextMap.get(token);
                trainStatus.setMessage(OK);
                trainStatus.setPercent(status.getPercent());
                trainStatus.setRunningType(status.getRunningType());
                MetricValue metricValue = status.getMetrics();
                if (metricValue == null) {
                    trainStatus.setTrainMetrics(null);
                    trainStatus.setValidationMetrics(null);
                    trainStatus.setFeatureImportance(null);
                    trainStatus.setBestRound(0);
                } else {
                    trainStatus.setTrainMetrics(getCacheMetric(status.getMetrics(), TRAIN));
                    trainStatus.setValidationMetrics(getCacheMetric(status.getMetrics(), VALIDATION));
                    trainStatus.setFeatureImportance(getCacheFeatureMetric(status.getMetrics()));
//                    trainStatus.setBestRound(status.getMetrics().getBestRound());
                }
            } else {//缓存中为空 则查询数据库
                TrainInfo trainInfo = TrainMapper.getTrainInfoByToken(token);
                if (trainInfo != null && trainInfo.getModelToken().split(",")[0] != null) {
                    trainStatus.setMessage(OK);
                    trainStatus.setPercent(100);
                    trainStatus.setRunningType(RunningType.COMPLETE);
                    MetricValue metricValue = trainInfo.getMetricInfo();
                    trainStatus.setTrainMetrics(getCacheMetric(metricValue, TRAIN));
                    trainStatus.setValidationMetrics(getCacheMetric(metricValue, VALIDATION));
                    trainStatus.setFeatureImportance(getCacheFeatureMetric(metricValue));
//                    trainStatus.setBestRound(metricValue.getBestRound());
                } else {//数据库为空 返回空
                    return trainStatus;
                }
            }
            return trainStatus;
        } catch (Exception ex) {
            logger.error("other exception:", ex);
            return null;
        }
    }


    /**
     * 从core包传过来的全部指标信息中解析训练指标
     *
     * @param metricValue 全部指标：训练指标、验证指标、特征重要性
     * @return 训练指标
     */

    public static List<Metric> getCacheMetric(MetricValue metricValue, String type) {
        List<Metric> Metric = new ArrayList<>();
        Map<MetricType, List<Pair<Integer, Double>>> Metrics = new HashMap<>();
        Map<MetricType, List<Pair<Integer, String>>> ArrMetrics = new HashMap<>();
        if (metricValue == null) {
            return Metric;
        }
        if (TRAIN.equals(type)) {
            if (metricValue.getMetrics() != null) {
                Metrics = metricValue.getMetrics();
            }
            if (metricValue.getMetricsArr() != null) {
                ArrMetrics = metricValue.getMetricsArr();
            }
        } else if (VALIDATION.equals(type)) {
            if (metricValue.getValidateMetrics() != null) {
                Metrics = metricValue.getValidateMetrics();
            }
            if (metricValue.getValidateMetricsArr() != null) {
                ArrMetrics = metricValue.getValidateMetricsArr();
            }
        }
        for (Map.Entry<MetricType, List<Pair<Integer, Double>>> entry : Metrics.entrySet()) {
            List<MetricPair> singleMetric = new ArrayList<>();
            IntStream.range(0, entry.getValue().size()).forEach(x -> singleMetric.add(new SingleMetric(entry.getValue().get(x).getKey(), entry.getValue().get(x).getValue())));
            Metric.add(new Metric(entry.getKey().getMetric(), singleMetric));
        }
        for (Map.Entry<MetricType, List<Pair<Integer, String>>> entry : ArrMetrics.entrySet()) {
            List<MetricPair> arrayMetrics = new ArrayList<>();
            IntStream.range(0, entry.getValue().size()).forEach(x -> arrayMetrics.add(new ArrayMetric(entry.getValue().get(x).getKey(), entry.getValue().get(x).getValue())));
            Metric.add(new Metric(entry.getKey().getMetric(), arrayMetrics));
        }
        return Metric;
    }

    /**
     * 从core包传过来的全部指标信息中解析特征重要性
     *
     * @param metricValue 全部指标：训练指标、验证指标、特征重要性
     * @return 特征重要性
     */
    public static List<Metric> getCacheFeatureMetric(MetricValue metricValue) {
        List<Metric> featrueMetrics = new ArrayList<>();
        List<Pair<String, Double>> featureList = new ArrayList<>();
        Map<String, Double> featureImportance = new HashMap<>();
        if (metricValue == null) {
            return featrueMetrics;
        }
        if (metricValue.featureImportance() != null) {
            featureImportance = metricValue.featureImportance();
        }
        for (Map.Entry<String, Double> entry : featureImportance.entrySet()) {
            Pair<String, Double> temp = new Pair<>(entry.getKey(), entry.getValue());
            featureList.add(temp);
        }
        List<MetricPair> featrueMetric = new ArrayList<>();
        IntStream.range(0, featureList.size()).forEach(x -> featrueMetric.add(new FeatureMetric(featureList.get(x).getKey(), featureList.get(x).getValue())));
        featrueMetrics.add(new Metric(FEATURE_IMPORTANCE, featrueMetric));
        return featrueMetrics;
    }

}
