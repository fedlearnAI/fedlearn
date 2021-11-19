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

package com.jdt.fedlearn.core.entity.horizontalZoo;

import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.core.parameter.HorizontalFedAvgPara;

import com.jdt.fedlearn.core.type.HorizontalZooMsgType;


public class HorizontalZooMsgStream implements Message {
    //模型唯一id
    private final String modelToken;
    //
    private final ClientInfo client;
    // 消息类型
    private final HorizontalZooMsgType msgType;

    //训练初始化需要
    private HorizontalFedAvgPara parameter;

    private final String modelName;
    private byte[] modelString;

    private int datasetSize;

    private double gMetric; //global model loss
    private double lMetric; //local model loss

    private String[] inferenceDataUid;

    public HorizontalZooMsgStream(String modelToken, ClientInfo client, HorizontalZooMsgType msgType,
                                  HorizontalFedAvgPara parameter,
                                  String modelName,
                                  byte[] modelString) {
        this.client = client;
        this.parameter = parameter;
        this.modelName = modelName;
        this.msgType = msgType;
        this.modelString = modelString;
        this.modelToken = modelToken;
    }

    public HorizontalZooMsgStream(String modelToken, ClientInfo client, HorizontalZooMsgType msgType,
                                  String modelName,
                                  String[] inferenceDataUid) {
        this.client = client;
        this.modelName = modelName;
        this.msgType = msgType;
        this.inferenceDataUid = inferenceDataUid;
        this.modelToken = modelToken;
    }

    public HorizontalZooMsgStream(String modelToken, ClientInfo client, HorizontalZooMsgType msgType,
                                  HorizontalFedAvgPara parameter,
                                  String modelName,
                                  byte[] modelString,
                                  int datasetSize,
                                  double gMetric,
                                  double lMetric) {
        this.client = client;
        this.parameter = parameter;
        this.modelName = modelName;
        this.msgType = msgType;
        this.modelString = modelString;
        this.modelToken = modelToken;
        this.gMetric = gMetric;
        this.lMetric = lMetric;
        this.datasetSize = datasetSize;
    }

    public ClientInfo getClient() {
        return client;
    }

    public HorizontalFedAvgPara getParameter() {
        return parameter;
    }

    public String getModelToken() {
        return modelToken;
    }

    public String getModelName() {
        return modelName;
    }

    public int getDatasetSize() {
        return datasetSize;
    }

    public byte[] getModelString() {
        return modelString;
    }

    public String[] getInferenceDataUid() {
        return inferenceDataUid;
    }

    public HorizontalZooMsgType getMsgType() {
        return msgType;
    }

    public double getGMetric() {
        return gMetric;
    }

    public double getLMetric() {
        return lMetric;
    }
}

