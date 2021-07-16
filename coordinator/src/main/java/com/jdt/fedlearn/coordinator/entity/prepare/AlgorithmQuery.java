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

package com.jdt.fedlearn.coordinator.entity.prepare;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.core.exception.DeserializeException;

import java.io.Serializable;

/**
 * 算法类型查询实体
 *
 */

public class AlgorithmQuery implements Serializable {
    private String algorithmType;
    private Integer taskId;

    public AlgorithmQuery() {
    }

    public AlgorithmQuery(String algorithmType, Integer taskId) {
        this.algorithmType = algorithmType;
        this.taskId = taskId;
    }

    public AlgorithmQuery(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            AlgorithmQuery query = mapper.readValue(jsonStr, this.getClass());
            this.algorithmType = query.algorithmType;
            this.taskId = query.taskId;
        } catch (Exception e) {
            throw new DeserializeException(e.getMessage());
        }
    }

    public String getAlgorithmType() {
        return this.algorithmType;
    }

    public Integer getTaskId() {
        return this.taskId;
    }
}