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


import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.core.entity.common.MetricValue;


public class BoostP3Res implements Message {
    private ClientInfo client;
    private String feature;
    private int index;
    private double gain ;
    private MetricValue trainMetric;
    private int workerNum;
    private SubModel subModel;

    public BoostP3Res() {
    }

    public BoostP3Res(ClientInfo client, String feature, int index) {
        this.client = client;
        this.feature = feature;
        this.index = index;
    }

    public BoostP3Res(ClientInfo client, String feature, int index, double gain) {
        this.client = client;
        this.feature = feature;
        this.index = index;
        this.gain = gain;
    }

    public ClientInfo getClient() {
        return client;
    }

    public String getFeature() {
        return feature;
    }

    public int getIndex() {
        return index;
    }

    public void setTrainMetric(MetricValue trainMetric) {
        this.trainMetric = trainMetric;
    }

    public MetricValue getTrainMetric() {
        return trainMetric;
    }

    public double getGain() {
        return gain;
    }

    public int getWorkerNum() {
        return workerNum;
    }

    public void setWorkerNum(int workerNum) {
        this.workerNum = workerNum;
    }

    public SubModel getSubModel() {
        return subModel;
    }

    public void setSubModel(SubModel subModel) {
        this.subModel = subModel;
    }
}
