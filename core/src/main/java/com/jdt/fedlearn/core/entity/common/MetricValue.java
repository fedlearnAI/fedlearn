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

package com.jdt.fedlearn.core.entity.common;

import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.data.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MetricValue implements Message {
    private static final long serialVersionUID = 3551840761275726582L;
    private Map<MetricType, List<Pair<Integer, Double>>> metrics;
    private Map<MetricType, List<Pair<Integer, String>>> metricsArr;
    private Map<MetricType, List<Pair<Integer, Double>>> validateMetrics;
    private Map<MetricType, List<Pair<Integer, String>>> validateMetricsArr;
    private Map<String, Double> featureImportance;
    private int bestRound;

    public MetricValue(Map<MetricType, List<Pair<Integer, Double>>> metrics) {
        this.metrics = metrics;
    }

    public MetricValue(Map<MetricType, List<Pair<Integer, Double>>> metrics,
                       Map<MetricType, List<Pair<Integer, String>>> metricsArr,
                       Map<MetricType, List<Pair<Integer, Double>>> validateMetrics,
                       Map<MetricType, List<Pair<Integer, String>>> validateMetricsArr
    ) {
        this.metrics = metrics;
        this.metricsArr = metricsArr;
        this.validateMetrics = validateMetrics;
        this.validateMetricsArr = validateMetricsArr;
    }

    public MetricValue(Map<MetricType, List<Pair<Integer, Double>>> metrics,
                       Map<MetricType, List<Pair<Integer, String>>> metricsArr,
                       Map<MetricType, List<Pair<Integer, Double>>> validateMetrics,
                       Map<MetricType, List<Pair<Integer, String>>> validateMetricsArr,
                       Map<String, Double> featureImportance) {
        this.metrics = metrics;
        this.metricsArr = metricsArr;
        this.validateMetrics = validateMetrics;
        this.validateMetricsArr = validateMetricsArr;
        this.featureImportance = featureImportance;
    }

    public MetricValue(Map<MetricType, List<Pair<Integer, Double>>> metrics,
                       Map<MetricType, List<Pair<Integer, String>>> metricsArr,
                       Map<MetricType, List<Pair<Integer, Double>>> validateMetrics,
                       Map<MetricType, List<Pair<Integer, String>>> validateMetricsArr,
                       Map<String, Double> featureImportance,
                       int bestRound) {
        this.metrics = metrics;
        this.metricsArr = metricsArr;
        this.validateMetrics = validateMetrics;
        this.validateMetricsArr = validateMetricsArr;
        this.featureImportance = featureImportance;
        this.bestRound = bestRound;
    }

    public Map<MetricType, List<Pair<Integer, String>>> getMetricsArr() {
        return metricsArr;
    }

    public Map<MetricType, List<Pair<Integer, Double>>> getValidateMetrics() {
        return validateMetrics;
    }

    public Map<MetricType, List<Pair<Integer, String>>> getValidateMetricsArr() {
        return validateMetricsArr;
    }

    public Map<String, Double> featureImportance() {
        return featureImportance;
    }

    public Map<MetricType, List<Pair<Integer, Double>>> getMetrics() {
        return metrics;
    }

    public Map<String, Double> getFeatureImportance() {
        return featureImportance;
    }

    public int getBestRound() {
        return bestRound;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MetricValue that = (MetricValue) o;
        return Objects.equals(metrics, that.metrics) && Objects.equals(metricsArr, that.metricsArr) && Objects.equals(validateMetrics, that.validateMetrics) && Objects.equals(validateMetricsArr, that.validateMetricsArr) && Objects.equals(featureImportance, that.featureImportance);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metrics, metricsArr, validateMetrics, validateMetricsArr, featureImportance);
    }
}
