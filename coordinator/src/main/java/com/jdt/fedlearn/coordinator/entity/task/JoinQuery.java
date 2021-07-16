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

package com.jdt.fedlearn.coordinator.entity.task;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.exception.DeserializeException;
import com.jdt.fedlearn.core.exception.SerializeException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 加入已创建任务请求的实体类，记录taskID， 用户名，数据集信息，客户端信息，特征信息，任务密码，企业编码
 *
 * @author lijingxi
 */
public class JoinQuery implements Message {
    private int taskId;
    private String username;
    private String dataset;
    private Map<String, String> clientInfo;
    private JoinFeatures features;
    private String taskPwd;
    private String merCode;

    public JoinQuery() {
    }

    public JoinQuery(int taskId, String username, String dataset, Map<String, String> clientInfo, JoinFeatures features, String taskPwd, String merCode) {
        this.taskId = taskId;
        this.username = username;
        this.dataset = dataset;
        this.clientInfo = clientInfo;
        this.features = features;
        this.taskPwd = taskPwd;
        this.merCode = merCode;
    }

    public JoinQuery(String jsonStr) {
        parseJson(jsonStr);
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Map<String, String> getClientInfo() {
        return clientInfo;
    }

    public void setClientInfo(Map<String, String> clientInfo) {
        this.clientInfo = clientInfo;
    }

    public JoinFeatures getFeatures() {
        return features;
    }

    public String getDataset() {
        return dataset;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    public String getTaskPwd() {
        return taskPwd;
    }

    public void setTaskPwd(String taskPwd) {
        this.taskPwd = taskPwd;
    }

    public String getMerCode() {
        return merCode;
    }

    public void setMerCode(String merCode) {
        this.merCode = merCode;
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


    public void parseJson(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        JoinQuery p3r;
        try {
            p3r = mapper.readValue(jsonStr, JoinQuery.class);
            this.taskId = p3r.taskId;
            this.username = p3r.username;
            this.clientInfo = p3r.clientInfo;
            this.features = p3r.features;
            this.dataset = p3r.dataset;
            this.taskPwd = p3r.taskPwd;
            this.merCode = p3r.merCode;
        } catch (IOException e) {
            throw new DeserializeException("JoinQuery ");
        }
    }
}
