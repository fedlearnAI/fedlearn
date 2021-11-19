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
import com.jdt.fedlearn.core.type.data.StringTuple2;


public class EncryptedGradHess implements Message {
    private ClientInfo client;
    private int[] instanceSpace;
    private StringTuple2[] gh;
    private String pubKey;
    private boolean newTree;
    private MetricValue trainMetric;
    private int workerNum;
    private SubModel subModel;
    private int modelId;
    private int instanceMin;
    private int instanceMax;

    public EncryptedGradHess() {
    }

    public EncryptedGradHess(ClientInfo client, int[] instanceSpace, StringTuple2[] gh, String pubKey, boolean newTree) {
        this.client = client;
        this.instanceSpace = instanceSpace;
        this.gh = gh;
        this.pubKey = pubKey;
        this.newTree = newTree;
    }

    public EncryptedGradHess(ClientInfo client, int[] instanceSpace) {
        this.client = client;
        this.instanceSpace = instanceSpace;
    }

    public EncryptedGradHess(ClientInfo client, int[] instanceSpace, int workerNum) {
        this.client = client;
        this.instanceSpace = instanceSpace;
        this.workerNum = workerNum;
    }

    public ClientInfo getClient() {
        return client;
    }

    public int[] getInstanceSpace() {
        return instanceSpace;
    }

    public String getPubKey() {
        return pubKey;
    }

    public StringTuple2[] getGh() {
        return gh;
    }

    public void setGh(StringTuple2[] gh) {
        this.gh = gh;
    }

    public boolean getNewTree() {
        return newTree;
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

    public SubModel getSubModel() {
        return subModel;
    }

    public void setSubModel(SubModel subModel) {
        this.subModel = subModel;
    }

    public int getModelId() {
        return modelId;
    }

    public void setModelId(int modelId) {
        this.modelId = modelId;
    }

    public int getInstanceMin() {
        return instanceMin;
    }

    public void setInstanceMin(int instanceMin) {
        this.instanceMin = instanceMin;
    }

    public int getInstanceMax() {
        return instanceMax;
    }

    public void setInstanceMax(int instanceMax) {
        this.instanceMax = instanceMax;
    }

}
