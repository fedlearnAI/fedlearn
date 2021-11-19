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

package com.jdt.fedlearn.core.entity.boost;


import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.core.entity.common.MetricValue;


public class BoostP2Res implements Message {
    private final FeatureLeftGH[] featureGL;
    private MetricValue trainMetric;
    private int workerNum;


    public BoostP2Res(FeatureLeftGH[] featureGL) {
        this.featureGL = featureGL;
    }

    public FeatureLeftGH[] getFeatureGL() {
        return featureGL;
    }

    public void setTrainMetric(MetricValue trainMetric) {
        this.trainMetric = trainMetric;
    }

    public MetricValue getTrainMetric() {
        return trainMetric;
    }

    public int getWorkerNum() {
        return workerNum;
    }

    public void setWorkerNum(int workerNum) {
        this.workerNum = workerNum;
    }
}
