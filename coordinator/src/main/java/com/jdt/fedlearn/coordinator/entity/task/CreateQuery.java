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
import com.jdt.fedlearn.core.exception.DeserializeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * 创建任务实体，包含客户端名称，任务名称，数据集信息，特征信息，密码信息，可见权限信息，推理权限信息
 * <p>在创建任务{@link com.jdt.fedlearn.coordinator.service.task.TaskCreateImpl}时使用此实体</p>
 * @author lijingxi
 */
public class CreateQuery {
    private static final Logger logger = LoggerFactory.getLogger(CreateQuery.class);

    private String username;
    private String taskName;
    private Map<String, String> clientInfo;
    private String dataset;
    private CreateFeatures features;

    //以下为权限部分
    private String hasPwd;
    private String taskPwd;
    /*创建方的code*/
    private String merCode;
    /*字典值 1，公开 2，私密 3，部分可见 4，部分不可见*/
    private String visible;
    /*企业编码列表*/
    private String visibleMerCode;
    /*字段值 1，所有人能进行推理 2，只有创建方可以推理*/
    private String inferenceFlag;

    public CreateQuery() {
    }

    public CreateQuery(String username, Map<String, String> clientInfo, CreateFeatures features, String dataset) {
        this.username = username;
        this.clientInfo = clientInfo;
        this.features = features;
        this.dataset = dataset;
    }

    public CreateQuery(String jsonStr) {
        parseJson(jsonStr);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public Map<String, String> getClientInfo() {
        return clientInfo;
    }

    public void setClientInfo(Map<String, String> clientInfo) {
        this.clientInfo = clientInfo;
    }

    public CreateFeatures getFeatures() {
        return features;
    }

    public String getDataset() {
        return dataset;
    }

    public String getHasPwd() {
        return hasPwd;
    }

    public String getTaskPwd() {
        return taskPwd;
    }

    public String getMerCode() {
        return merCode;
    }

    public String getVisible() {
        return visible;
    }

    public String getVisibleMerCode() {
        return visibleMerCode;
    }

    public String getInferenceFlag() {
        return inferenceFlag;
    }

    public void parseJson(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        CreateQuery p3r;
        try {
            p3r = mapper.readValue(jsonStr, CreateQuery.class);
            this.username = p3r.username;
            this.taskName = p3r.taskName;
            this.clientInfo = p3r.clientInfo;
            this.features = p3r.features;
            this.dataset = p3r.dataset;
            this.hasPwd = p3r.hasPwd;
            this.taskPwd = p3r.taskPwd;
            this.merCode = p3r.merCode;
            this.visible = p3r.visible;
            this.visibleMerCode = p3r.visibleMerCode;
            this.inferenceFlag = p3r.inferenceFlag;
        } catch (IOException e) {
            logger.error("parse json string error", e);
            logger.error("input json is:" + jsonStr);
            throw new DeserializeException(e.getMessage());
        }
    }
}
