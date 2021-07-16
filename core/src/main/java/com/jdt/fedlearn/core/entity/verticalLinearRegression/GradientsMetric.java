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

package com.jdt.fedlearn.core.entity.verticalLinearRegression;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.type.MetricType;
import java.util.Map;

public class GradientsMetric implements Message {
    private ClientInfo client;
    private double[] gradients;
    private Map<MetricType, Double> metric;

    public GradientsMetric(Map<MetricType, Double> metric) {
        this.metric = metric;

    }

    public GradientsMetric(ClientInfo client, double[] gradients, Map<MetricType, Double> metric) {
        this.client = client;
        this.gradients = gradients;
        this.metric = metric;

    }


    public double[] getGradients() {
        return gradients;
    }

    public ClientInfo getClient() {
        return client;
    }

    public Map<MetricType, Double> getMetric() {
        return metric;
    }

}
