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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.core.exception.DeserializeException;

/**
 * 训练不同阶段参数请求实体，包含modelToken和请求阶段类型
 * @author lijingxi
 */
public class TrainParameterQuery {
    private String modelToken;
    /**
     * type
     * 1 查看参数
     * 2 已完成
     * 3 重启
     */
    private String type;

    public TrainParameterQuery() {
    }

    public TrainParameterQuery(String modelToken, String type) {
        this.modelToken = modelToken;
        this.type = type;
    }

    public TrainParameterQuery(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            TrainParameterQuery query = mapper.readValue(jsonStr, this.getClass());
            modelToken = query.modelToken;
            type = query.type;
        } catch (Exception e) {
            throw new DeserializeException("train query");
        }
    }

    public String getModelToken() {
        return this.modelToken;
    }


    public String getType() {
        return this.type;
    }
}
