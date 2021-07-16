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

import java.io.IOException;

/**
 * 查询用户训练列表的请求实体
 *
 * @author lijingxi
 */
public class TrainListReq {
    private String username;
    private String taskId;

    public TrainListReq() {
    }

    public TrainListReq(String username, String taskId) {
        this.username = username;
        this.taskId = taskId;
    }

    public TrainListReq(String jsonStr) {
        parseJson(jsonStr);
    }

    public String getUsername() {
        return this.username;
    }

    public String getTaskId() {
        return this.taskId;
    }


    public void parseJson(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        TrainListReq p3r;
        try {
            p3r = mapper.readValue(jsonStr, TrainListReq.class);
            this.username = p3r.username;
            this.taskId = p3r.taskId;
        } catch (IOException e) {
            throw new DeserializeException(e.getMessage());
        }
    }
}
