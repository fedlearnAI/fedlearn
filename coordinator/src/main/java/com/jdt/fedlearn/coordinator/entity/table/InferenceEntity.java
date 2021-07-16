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

package com.jdt.fedlearn.coordinator.entity.table;

import java.util.Date;

/**
 * 记录推理信息的实体，记录信息包括：id、推理id、模型token、taskId、用户名、开始时间、结束时间、推理结果、状态、创建时间、修改时间、请求数、返回数、调用人
 */

public class InferenceEntity {
    private String id;
    private String inferenceId;
    private String modelToken;
    private String taskId;
    private String userName;
    private Date startTime;
    private Date endTime;
    private String inferenceResult;
    private String status;
    private Date createdTime;
    private Date modifiedTime;
    private int requestNum;
    private int responseNum;
    /*username表示登陆了前端页面的用户，caller表示调用了推理的人 */
    private String caller;

    public InferenceEntity() {
    }

    public InferenceEntity(String userName, String modelToken, String inferenceId, Date startTime, Date endTime, String inferenceResult, int requestNum, int responseNum) {
        this.inferenceId = inferenceId;
        this.modelToken = modelToken;
        this.userName = userName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.inferenceResult = inferenceResult;
        this.requestNum = requestNum;
        this.responseNum = responseNum;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getInferenceId() {
        return inferenceId;
    }

    public void setInferenceId(String inferenceId) {
        this.inferenceId = inferenceId;
    }

    public String getModelToken() {
        return modelToken;
    }

    public void setModelToken(String modelToken) {
        this.modelToken = modelToken;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getInferenceResult() {
        return inferenceResult;
    }

    public void setInferenceResult(String inferenceResult) {
        this.inferenceResult = inferenceResult;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }

    public Date getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(Date modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public int getRequestNum() {
        return requestNum;
    }

    public void setRequestNum(int requestNum) {
        this.requestNum = requestNum;
    }

    public int getResponseNum() {
        return responseNum;
    }

    public void setResponseNum(int responseNum) {
        this.responseNum = responseNum;
    }

    public String getCaller() {
        return caller;
    }

    public void setCaller(String caller) {
        this.caller = caller;
    }
}
