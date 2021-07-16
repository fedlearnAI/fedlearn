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
import java.util.List;

/**
 * 任务开始传入的实体类，记录用户名，TaskID，模型，算法参数，交叉验证参数等信息
 *
 * @author wangpeiqi
 * @author lijingxi
 * @since 0.6.7
 */

public class StartTrain {
    private String username;
    private String taskId;
    private String model;
    //
    private List<SingleParameter> algorithmParams;
    //id对齐选项和 交叉验证参数
    private List<SingleParameter> commonParams;

    public StartTrain() {
    }

    public StartTrain(String jsonStr) {
        parseJson(jsonStr);
    }

    public void parseJson(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        StartTrain p3r;
        try {
            p3r = mapper.readValue(jsonStr, StartTrain.class);
            this.username = p3r.username;
            this.taskId = p3r.taskId;
            this.model = p3r.model;
            this.algorithmParams = p3r.algorithmParams;
            this.commonParams = p3r.commonParams;
        } catch (IOException e) {
            throw new DeserializeException(e.getMessage());
        }
    }

    public String getTaskId() {
        return this.taskId;
    }

    public String getModel() {
        return this.model;
    }

    public List<SingleParameter> getAlgorithmParams() {
        return this.algorithmParams;
    }

    public List<SingleParameter> getCommonParams() {
        return this.commonParams;
    }

    public String getUsername() {
        return this.username;
    }
}
