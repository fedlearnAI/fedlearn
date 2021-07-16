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

package com.jdt.fedlearn.coordinator.entity.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.core.exception.DeserializeException;

import java.io.IOException;

/**
 * 查询数据源的请求实体，包含用户名，客户端url，任务id，任务密码四方面信息
 */
public class FeatureReq {

    private String username;
    private String clientUrl;
    private String taskId;
    private String taskPwd;

    public FeatureReq() {
    }

//    public FeatureReq(String username, String clientUrl) {
//        this.username = username;
//        this.clientUrl = clientUrl;
//    }


    public FeatureReq(String username, String clientUrl, String taskId, String taskPwd) {
        this.username = username;
        this.clientUrl = clientUrl;
        this.taskId = taskId;
        this.taskPwd = taskPwd;
    }

    public FeatureReq(String jsonStr) {
        parseJson(jsonStr);
    }

    public void parseJson(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        FeatureReq p3r = null;
        try {
            p3r = mapper.readValue(jsonStr, FeatureReq.class);
            this.username = p3r.username;
            this.clientUrl = p3r.clientUrl;
            this.taskPwd = p3r.taskPwd;
            this.taskId = p3r.taskId;
        } catch (IOException e) {
            throw new DeserializeException(e.getMessage());
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getClientUrl() {
        return clientUrl;
    }

    public void setClientUrl(String clientUrl) {
        this.clientUrl = clientUrl;
    }

    public String getTaskPwd() {
        return taskPwd;
    }

    public void setTaskPwd(String taskPwd) {
        this.taskPwd = taskPwd;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
}
