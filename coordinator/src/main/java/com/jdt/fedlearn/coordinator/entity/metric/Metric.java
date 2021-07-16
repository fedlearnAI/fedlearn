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

package com.jdt.fedlearn.coordinator.entity.metric;

import java.util.List;

/**
 * 指标值
 * name为指标名称
 * metric为该类型指标所有轮数的指标值
 */
public class Metric {

    private String name;

    private List<MetricPair> metric;

    public Metric(String name, List<MetricPair> metric) {
        this.name = name;
        this.metric = metric;
    }

    public String getName() {
        return name;
    }

    public List<MetricPair> getMetric() {
        return metric;
    }

}
