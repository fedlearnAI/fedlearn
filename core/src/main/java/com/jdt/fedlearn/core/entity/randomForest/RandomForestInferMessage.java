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

package com.jdt.fedlearn.core.entity.randomForest;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.jdt.fedlearn.core.entity.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RandomForestInferMessage implements Message {
    private static final Logger logger = LoggerFactory.getLogger(RandomForestInferMessage.class);
    private List<Integer> featureId;
    private List<Double> thresValue;
    private List<Integer> sampleId = new ArrayList<>();
    private List<Boolean> isLeft = new ArrayList<>();
    private Boolean isFinish = false;
    private List<Double> prediction;

    public RandomForestInferMessage() {
    }

    public String toJson() {
        String jsonStr;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            jsonStr = objectMapper.writeValueAsString(this);
        } catch (Exception e) {
            logger.error("to json error", e);
            jsonStr = null;
        }
        return jsonStr;
    }

    public void parseJson(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        RandomForestInferMessage tmp = null;
        try {
            tmp = mapper.readValue(jsonStr, RandomForestInferMessage.class);
            this.featureId = tmp.featureId;
            this.thresValue = tmp.thresValue;
            this.isLeft = tmp.isLeft;
            this.sampleId = tmp.sampleId;
        } catch (IOException e) {
            logger.error("parse error", e);
        }
    }


    public void setIsLeft(List<Boolean> is_left) {
        this.isLeft = is_left;
    }

    public List<Boolean> getIs_left() {
        return isLeft;
    }

    public List<Integer> getFeatureId() {
        return featureId;
    }

    public List<Integer> getSampleId() {
        return sampleId;
    }

    public List<Double> getThresValue() {
        return thresValue;
    }

    public void setFeatureId(List<Integer> featureId) {
        this.featureId = featureId;
    }

    public void setThresValue(List<Double> thresValue) {
        this.thresValue = thresValue;
    }

    public void setSampleId(List<Integer> sampleId) {
        this.sampleId = sampleId;
    }
}