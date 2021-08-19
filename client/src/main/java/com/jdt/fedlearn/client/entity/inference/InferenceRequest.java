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

package com.jdt.fedlearn.client.entity.inference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.core.exception.DeserializeException;
import com.jdt.fedlearn.core.type.AlgorithmType;


import java.io.IOException;

public class InferenceRequest {
    private String inferenceId;
    //TODO 移除modelToken
    private String modelToken;
    private AlgorithmType algorithm;
    private int phase;
    private String dataset;
    private String index;
    private String body;

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

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }


    public String getInferenceId() {
        return inferenceId;
    }

    public void setInferenceId(String inferenceId) {
        this.inferenceId = inferenceId;
    }

    public String getDataset() {
        return dataset;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public void parseJson(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        InferenceRequest p1r;
        try {
            p1r = mapper.readValue(jsonStr, InferenceRequest.class);
            this.modelToken = p1r.modelToken;
            this.algorithm = p1r.algorithm;
            this.phase = p1r.phase;
            this.body = p1r.body;
            this.inferenceId = p1r.inferenceId;
            this.dataset = p1r.dataset;
            this.index = p1r.index;
        } catch (IOException e) {
            throw new DeserializeException("inference request parse json error ", e);
        }
    }

    @Override
    public String toString() {
        return "InferenceRequest{" +
                "inferenceId='" + inferenceId + '\'' +
                ", modelToken='" + modelToken + '\'' +
                ", algorithm=" + algorithm +
                ", phase=" + phase +
                ", dataset='" + dataset + '\'' +
                ", index='" + index + '\'' +
                ", data='" + body + '\'' +
                '}';
    }
}
