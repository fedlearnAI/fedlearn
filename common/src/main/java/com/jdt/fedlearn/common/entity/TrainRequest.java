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
package com.jdt.fedlearn.common.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;
import com.jdt.fedlearn.common.entity.core.type.ReduceType;
import com.jdt.fedlearn.common.enums.RunningType;
import com.jdt.fedlearn.common.exception.DeserializeException;

import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;

/**
 *@Description: 任务请求
 */
public class TrainRequest implements Serializable {
    private String modelToken;
    private AlgorithmType algorithm;
    private int phase;
    private String data;
    private int dataNum;
    private int dataIndex;
    private boolean isGzip;
    private RunningType status;
    private boolean isSync;
    private String requestId;
    private ReduceType reduceType;
    private String dataset;

    public String getDataset() {
        return dataset;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TrainRequest that = (TrainRequest) o;
        return phase == that.phase &&
                dataNum == that.dataNum &&
                dataIndex == that.dataIndex &&
                isGzip == that.isGzip &&
                isSync == that.isSync &&
                Objects.equals(modelToken, that.modelToken) &&
                algorithm == that.algorithm &&
                Objects.equals(data, that.data) &&
                status == that.status &&
                Objects.equals(requestId, that.requestId) &&
                Objects.equals(reduceType, that.reduceType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modelToken, algorithm, phase, data, dataNum, dataIndex, isGzip, status, isSync, requestId, reduceType);
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

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public ReduceType getReduceType() {
        return reduceType;
    }

    public void setReduceType(ReduceType reduceType) {
        this.reduceType = reduceType;
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
            this.requestId = p1r.requestId;
            this.reduceType = p1r.reduceType;
        } catch (IOException e) {
            throw new DeserializeException("DeserializeException", e);
        }
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
        sb.append(", requestId=").append(requestId);
        sb.append(", isSync='").append(isSync).append('\'');
        sb.append(", reduceType='").append(reduceType).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
