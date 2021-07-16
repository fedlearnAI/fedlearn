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
package com.jdt.fedlearn.worker.entity.inference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.core.exception.DeserializeException;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.worker.util.ExceptionUtil;

import java.io.IOException;

public class InferenceRequest {
    private String inferenceId;
    //TODO 移除modelToken
    private String modelToken;
    private AlgorithmType algorithm;
    private int phase;
    private String data;


    public InferenceRequest() {
    }

    public InferenceRequest(String jsonStr) {
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


    public String getInferenceId() {
        return inferenceId;
    }

    public void setInferenceId(String inferenceId) {
        this.inferenceId = inferenceId;
    }

    public void parseJson(String jsonStr){
        ObjectMapper mapper = new ObjectMapper();
        InferenceRequest p1r;
        try {
            p1r = mapper.readValue(jsonStr, InferenceRequest.class);
            this.modelToken = p1r.modelToken;
            this.algorithm = p1r.algorithm;
            this.phase = p1r.phase;
            this.data = p1r.data;
            this.inferenceId = p1r.inferenceId;
        } catch (IOException e) {
            throw new DeserializeException(ExceptionUtil.getExInfo(e));
        }
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("SubRequest{");
        sb.append("modelToken='").append(modelToken).append('\'');
        sb.append(", algorithm=").append(algorithm);
        sb.append(", phase=").append(phase);
        sb.append(", data='").append(data).append('\'');
        sb.append(", inferenceId='").append(inferenceId).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
