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

/**
 * 特征重要性，x代表特征名，y代表该特征重要性
 */
public class FeatureMetric implements MetricPair {

    private String x;
    private double y;

    public FeatureMetric(String x, double y) {
        this.x = x;
        this.y = y;
    }

    public String getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public String roundString(){
        return x;
    }
    public String metricString(){
        return String.valueOf(y);
    }
}

