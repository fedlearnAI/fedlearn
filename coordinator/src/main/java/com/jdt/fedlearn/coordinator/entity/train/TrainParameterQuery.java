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

package com.jdt.fedlearn.coordinator.entity.train;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 训练不同阶段参数请求实体，包含modelToken和请求阶段类型
 *
 * @author lijingxi
 */
public class TrainParameterQuery {
    private String modelToken;


    public TrainParameterQuery() {
    }

    public TrainParameterQuery(String modelToken) {
        this.modelToken = modelToken;
    }

    public static TrainParameterQuery parseJson(String jsonStr) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        return mapper.readValue(jsonStr, TrainParameterQuery.class);

    }

    public String getModelToken() {
        return this.modelToken;
    }


}
