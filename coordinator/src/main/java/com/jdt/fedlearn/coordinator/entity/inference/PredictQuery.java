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

package com.jdt.fedlearn.coordinator.entity.inference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.exception.SerializeException;

import java.io.IOException;

public class PredictQuery implements Message {
    private String[] uid;
    private String username;
    private String model;
    private String inferenceId;

    public PredictQuery() {
    }

    public PredictQuery(String jsonStr) {
        parseJson(jsonStr);
    }

    public void parseJson(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        PredictQuery p3r = null;
        try {
            p3r = mapper.readValue(jsonStr, PredictQuery.class);
            this.uid = p3r.uid;
            this.model = p3r.model;
            this.username = p3r.username;
            this.inferenceId = p3r.inferenceId;
        } catch (IOException e) {
            throw new SerializeException("predict Phase1 Request to json");
        }
    }

    public String toJson() {
        String jsonStr;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            jsonStr = objectMapper.writeValueAsString(this);
        } catch (Exception e) {
            throw new SerializeException("Boost Phase1 Request to json");
        }
        return jsonStr;
    }

    public String[] getUid() {
        return uid;
    }

    public void setUid(String[] uid) {
        this.uid = uid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getInferenceId() {
        return inferenceId;
    }

    public void setInferenceId(String inferenceId) {
        this.inferenceId = inferenceId;
    }
}
