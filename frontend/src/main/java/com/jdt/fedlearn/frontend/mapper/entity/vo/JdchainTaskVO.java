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

package com.jdt.fedlearn.frontend.mapper.entity.vo;

public class JdchainTaskVO {
    private String owner;
    private String taskName;
    private String taskId;
    private String[] participants;
    private String hasPwd;
    private String visible;
    /*企业编码列表*/
    private String visibleMerCode;
    private String visibleMerName;
    /*字段值 1，所有人能进行推理 2，只有创建方可以推理*/
    private String inferenceFlag;

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String[] getParticipants() {
        return participants;
    }

    public void setParticipants(String[] participants) {
        this.participants = participants;
    }

    public String getHasPwd() {
        return hasPwd;
    }

    public void setHasPwd(String hasPwd) {
        this.hasPwd = hasPwd;
    }

    public String getVisible() {
        return visible;
    }

    public void setVisible(String visible) {
        this.visible = visible;
    }

    public String getVisibleMerCode() {
        return visibleMerCode;
    }

    public void setVisibleMerCode(String visibleMerCode) {
        this.visibleMerCode = visibleMerCode;
    }

    public String getVisibleMerName() {
        return visibleMerName;
    }

    public void setVisibleMerName(String visibleMerName) {
        this.visibleMerName = visibleMerName;
    }

    public String getInferenceFlag() {
        return inferenceFlag;
    }

    public void setInferenceFlag(String inferenceFlag) {
        this.inferenceFlag = inferenceFlag;
    }
}
