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

package com.jdt.fedlearn.coordinator.entity.train;

import com.jdt.fedlearn.common.enums.RunningType;
import com.jdt.fedlearn.coordinator.entity.metric.Metric;

import java.util.List;
import java.util.Map;

/**
 * 训练状态信息，包含　type, percent and message
 */
public class TrainStatus {
    private RunningType runningType;
    private int percent;
    private String message;
    private Map<String, List<Metric>> metrics; //TODO remove
    private String token; //TODO remove
    private List<Metric> trainMetrics;
    private List<Metric> validationMetrics;
    private List<Metric> featureImportance;
    private int bestRound;

    public TrainStatus() {

    }

    public TrainStatus(RunningType runningType, int percent) {
        this.runningType = runningType;
        this.percent = percent;
        this.message = "";
    }

    public TrainStatus(String runningType, int percent, String message) {
        this.runningType = RunningType.valueOf(runningType);
        this.percent = percent;
        this.message = message;
    }

    public TrainStatus(RunningType runningType, int percent, String message) {
        this.runningType = runningType;
        this.percent = percent;
        this.message = message;
    }

    public RunningType getRunningType() {
        return runningType;
    }

    public void setRunningType(RunningType runningType) {
        this.runningType = runningType;
    }

    public int getPercent() {
        return percent;
    }

    public void setPercent(int percent) {
        this.percent = percent;
    }

    public void incPercent() {
        this.percent += 1;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, List<Metric>> getMetrics() {
        return metrics;
    }

    public void setMetrics(Map<String, List<Metric>> metrics) {
        this.metrics = metrics;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public List<Metric> getTrainMetrics() {
        return trainMetrics;
    }

    public void setTrainMetrics(List<Metric> trainMetrics) {
        this.trainMetrics = trainMetrics;
    }

    public List<Metric> getValidationMetrics() {
        return validationMetrics;
    }

    public void setValidationMetrics(List<Metric> validationMetrics) {
        this.validationMetrics = validationMetrics;
    }

    public List<Metric> getFeatureImportance() {
        return featureImportance;
    }

    public void setFeatureImportance(List<Metric> featureImportance) {
        this.featureImportance = featureImportance;
    }

    public int getBestRound() {
        return bestRound;
    }

    public void setBestRound(int bestRound) {
        this.bestRound = bestRound;
    }
}
