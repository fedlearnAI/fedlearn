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

package com.jdt.fedlearn.coordinator.entity.table;

import com.jdt.fedlearn.common.enums.RunningType;
import com.jdt.fedlearn.common.entity.SingleParameter;
import com.jdt.fedlearn.core.entity.common.MetricValue;
import com.jdt.fedlearn.core.type.AlgorithmType;

import java.util.List;

/**
 * 训练中和训练完成的信息，与数据库表结构对应，用于持久化存储
 */
public class TrainInfo {
    private String modelToken;
    private AlgorithmType algorithmType;
    private String matchId;
    private List<SingleParameter> hyperParameter;
    private MetricValue metricInfo;
    private RunningType runningType;
    private int percent;
    private long startTime;
    private long endTime;

    public TrainInfo() {
    }

    public TrainInfo(String modelToken, MetricValue metricInfo, RunningType runningType, int percent) {
        this.modelToken = modelToken;
        this.metricInfo = metricInfo;
        this.runningType = runningType;
        this.percent = percent;
    }

    public TrainInfo(String modelToken, String matchId, AlgorithmType algorithmType, List<SingleParameter> hyperParameter, long startTime, long endTime) {
        this.modelToken = modelToken;
        this.matchId = matchId;
        this.algorithmType = algorithmType;
        this.hyperParameter = hyperParameter;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public TrainInfo(String modelToken, AlgorithmType algorithmType, List<SingleParameter> hyperParameter, MetricValue metricInfo, long startTime, long endTime, RunningType runningType, int percent) {
        this.modelToken = modelToken;
        this.algorithmType = algorithmType;
        this.hyperParameter = hyperParameter;
        this.metricInfo = metricInfo;
        this.startTime = startTime;
        this.endTime = endTime;
        this.runningType = runningType;
        this.percent = percent;
    }

    public TrainInfo(String modelToken, AlgorithmType algorithmType, List<SingleParameter> hyperParameter, String matchId) {
        this.modelToken = modelToken;
        this.algorithmType = algorithmType;
        this.hyperParameter = hyperParameter;
        this.startTime = System.currentTimeMillis();
        this.matchId = matchId;
    }

    public String getModelToken() {
        return modelToken;
    }

    public AlgorithmType getAlgorithmType() {
        return algorithmType;
    }

    public String getMatchId() {
        return matchId;
    }

    public List<SingleParameter> getHyperParameter() {
        return hyperParameter;
    }

    public MetricValue getMetricInfo() {
        return metricInfo;
    }

    public RunningType getRunningType() {
        return runningType;
    }

    public int getPercent() {
        return percent;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setModelToken(String modelToken) {
        this.modelToken = modelToken;
    }

    public void setAlgorithmType(AlgorithmType algorithmType) {
        this.algorithmType = algorithmType;
    }

    public void setHyperParameter(List<SingleParameter> hyperParameter) {
        this.hyperParameter = hyperParameter;
    }

    public void setMetricInfo(MetricValue metricInfo) {
        this.metricInfo = metricInfo;
    }

    public void setRunningType(RunningType runningType) {
        this.runningType = runningType;
    }

    public void setPercent(int percent) {
        this.percent = percent;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
}
