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

package com.jdt.fedlearn.client.entity.train;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.common.exception.DeserializeException;
import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;
import com.jdt.fedlearn.common.enums.RunningType;

import java.io.IOException;

public class TrainRequest {
    private String modelToken;
    private AlgorithmType algorithm;
    private int phase;
    private String data;
    private int dataNum;
    private int dataIndex;
    private boolean isGzip;
    private RunningType status;
    private boolean isSync;
    private ClientInfo clientInfo;
    private String reqNum;
    private String dataset;

    public TrainRequest() {
    }

    public TrainRequest(String jsonStr) {
        parseJson(jsonStr);
    }

    public String getModelToken() {
        return modelToken;
    }

    public void setModelToken(String modelToken) {
        this.modelToken = modelToken;
    }

    public AlgorithmType getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(AlgorithmType algorithm) {
        this.algorithm = algorithm;
    }

    public int getPhase() {
        return phase;
    }

    public void setPhase(int phase) {
        this.phase = phase;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public int getDataNum() {
        return dataNum;
    }

    public void setDataNum(int dataNum) {
        this.dataNum = dataNum;
    }

    public int getDataIndex() {
        return dataIndex;
    }

    public void setDataIndex(int dataIndex) {
        this.dataIndex = dataIndex;
    }

    @JsonProperty("isGzip")
    public boolean isGzip() {
        return isGzip;
    }

    public boolean getIsGzip() {
        return isGzip;
    }

    public void setGzip(boolean gzip) {
        isGzip = gzip;
    }

    public RunningType getStatus() {
        return status;
    }

    public void setStatus(RunningType status) {
        this.status = status;
    }

    @JsonProperty("isSync")
    public boolean isSync() {
        return isSync;
    }

    public void setSync(boolean sync) {
        isSync = sync;
    }

    public String getReqNum() {
        return reqNum;
    }

    public void setReqNum(String reqNum) {
        this.reqNum = reqNum;
    }

    public void parseJson(String jsonStr){
        ObjectMapper mapper = new ObjectMapper();
        TrainRequest p1r;
        try {
            p1r = mapper.readValue(jsonStr, TrainRequest.class);
            this.modelToken = p1r.modelToken;
            this.algorithm = p1r.algorithm;
            this.phase = p1r.phase;
            this.data = p1r.data;
            this.dataNum = p1r.dataNum;
            this.dataIndex = p1r.dataIndex;
            this.isGzip = p1r.isGzip;
            this.status = p1r.status;
            this.isSync = p1r.isSync;
            this.clientInfo = p1r.clientInfo;
            this.reqNum = p1r.reqNum;
            this.dataset = p1r.dataset;
        } catch (IOException e) {
            throw new DeserializeException("train reqeuest error : ", e);
        }
    }

    public ClientInfo getClientInfo() {
        return clientInfo;
    }

    public void setClientInfo(ClientInfo clientInfo) {
        this.clientInfo = clientInfo;
    }

    public String getDataset() {
        return dataset;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("SubRequest{");
        sb.append("modelToken='").append(modelToken).append('\'');
        sb.append(", algorithm=").append(algorithm);
        sb.append(", phase=").append(phase);
        sb.append(", data='").append(data).append('\'');
        sb.append(", dataNum=").append(dataNum);
        sb.append(", dataIndex=").append(dataIndex);
        sb.append(", isGzip=").append(isGzip);
        sb.append(", status=").append(status);
        sb.append(", isSync='").append(isSync).append('\'');
        sb.append(", dataset='").append(dataset).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
