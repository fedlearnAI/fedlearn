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

package com.jdt.fedlearn.common.entity.jdchain;
import java.util.Date;
import java.util.List;

public class JdchainTask {
    private String taskId;
    private String username;
    private String taskName;
    private List<String> partners;
    private List<ClientInfoFeatures> clientInfoFeatures;
    private Date createTime;
    private Date updateTime;
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

    public JdchainTask() {
    }

    public JdchainTask(String taskId, String username, String taskName) {
        this.taskId = taskId;
        this.username = username;
        this.taskName = taskName;
        this.createTime = new Date();
        this.updateTime = new Date();
    }


    public JdchainTask(String taskId, String username, String taskName,String hasPwd,String taskPwd,String visible,String visibleMerCode,String inferenceFlag,String merCode) {
        this.taskId = taskId;
        this.username = username;
        this.taskName = taskName;
        this.createTime = new Date();
        this.updateTime = new Date();
        this.hasPwd = hasPwd;
        this.taskPwd = taskPwd;
        this.visible = visible;
        this.visibleMerCode = visibleMerCode;
        this.inferenceFlag = inferenceFlag;
        this.merCode = merCode;
    }


    public JdchainTask(String taskId, String username, String taskName, List<String> partners, List<ClientInfoFeatures> clientInfoFeatures) {
        this.taskId = taskId;
        this.username = username;
        this.taskName = taskName;
        this.partners = partners;
        this.clientInfoFeatures = clientInfoFeatures;
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


    public List<String> getPartners() {
        return partners;
    }

    public void setPartners(List<String> partners) {
        this.partners = partners;
    }

    public List<ClientInfoFeatures> getClientInfoFeatures() {
        return clientInfoFeatures;
    }

    public void setClientInfoFeatures(List<ClientInfoFeatures> clientInfoFeatures) {
        this.clientInfoFeatures = clientInfoFeatures;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String isHasPwd() {
        return hasPwd;
    }

    public void setHasPwd(String hasPwd) {
        this.hasPwd = hasPwd;
    }

    public String getTaskPwd() {
        return taskPwd;
    }

    public void setTaskPwd(String taskPwd) {
        this.taskPwd = taskPwd;
    }

    public String getHasPwd() {
        return hasPwd;
    }

    public String getVisible() {
        return visible;
    }

    public void setVisible(String visible) {
        this.visible = visible;
    }

    public String getInferenceFlag() {
        return inferenceFlag;
    }

    public void setInferenceFlag(String inferenceFlag) {
        this.inferenceFlag = inferenceFlag;
    }

    public String getMerCode() {
        return merCode;
    }

    public void setMerCode(String merCode) {
        this.merCode = merCode;
    }

    public String getVisibleMerCode() {
        return visibleMerCode;
    }

    public void setVisibleMerCode(String visibleMerCode) {
        this.visibleMerCode = visibleMerCode;
    }
}
