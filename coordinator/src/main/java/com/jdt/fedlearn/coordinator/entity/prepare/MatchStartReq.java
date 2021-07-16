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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.core.exception.DeserializeException;

import java.io.Serializable;

/**
 * ID对齐请求实体，包含username，taskId，matchAlgorithm三方面信息
 * 含有三个构造方法，其中包含一个以json作为输入格式的构造方法
 * @author lijingxi
 */
public class MatchStartReq implements Serializable {
    private String username;
    private String taskId;
    private String matchAlgorithm;

    public MatchStartReq() {
    }

    public MatchStartReq(String username, String taskId, String matchAlgorithm) {
        this.username = username;
        this.taskId = taskId;
        this.matchAlgorithm = matchAlgorithm;
    }

    public MatchStartReq(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            MatchStartReq query = mapper.readValue(jsonStr, this.getClass());
            taskId = query.taskId;
            username = query.username;
            matchAlgorithm = query.matchAlgorithm;
        } catch (RuntimeException | JsonProcessingException e) {
            throw new DeserializeException("deserialize error with:", e);
        }
    }

    public String getUsername() {
        return username;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getMatchAlgorithm() {
        return matchAlgorithm;
    }
}
