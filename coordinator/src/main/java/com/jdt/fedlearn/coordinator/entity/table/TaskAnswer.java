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

import com.jdt.fedlearn.coordinator.service.task.TaskListImpl;

import java.util.List;

/**
 * 用于查询任务信息的实体， 包含TaskID, 创建方，参与方列表，密码，任务密码，企业编码，可见权限，可见企业编码列表，推理权限的信息
 * @author lijingxi
 */
public class TaskAnswer {
    private int taskId;
    private String taskName;
    private String owner;
    private String[] participants;
    private String hasPwd;
    private String taskPwd;
    private String merCode;
    private String visible;
    private String visibleMerCode;
    private String inferenceFlag;

    public TaskAnswer() {
    }

    public TaskAnswer(Integer id, String task_name, String task_owner, String partners ,String hasPwd, String merCode, String visible, String visibleMerCode, String inferenceFlag) {
        this.taskId = id;
        this.taskName = task_name;
        this.owner = task_owner;
        this.participants = TaskListImpl.parsePartners(partners);
        this.hasPwd = hasPwd;
        this.merCode = merCode;
        this.visible = visible;
        this.visibleMerCode = visibleMerCode;
        this.inferenceFlag = inferenceFlag;
    }

    public TaskAnswer(Integer id, String task_name, String task_owner, List<String> partners , String hasPwd, String merCode, String visible, String visibleMerCode, String inferenceFlag) {
        this.taskId = id;
        this.taskName = task_name;
        this.owner = task_owner;
        this.participants = partners.toArray(new String[0]);
        this.hasPwd = hasPwd;
        this.merCode = merCode;
        this.visible = visible;
        this.visibleMerCode = visibleMerCode;
        this.inferenceFlag = inferenceFlag;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String[] getParticipants() {
        return participants;
    }

    public void setParticipants(String[] participants) {
        this.participants = participants;
    }

    public void setParticipants(String parts) {
        this.participants = TaskListImpl.parsePartners(parts);
    }

    public String getHasPwd() {
        return hasPwd;
    }

    public void setHasPwd(String hasPwd) {
        this.hasPwd = hasPwd;
    }

    public String getMerCode() {
        return merCode;
    }

    public void setMerCode(String merCode) {
        this.merCode = merCode;
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

    public String getTaskPwd() {
        return taskPwd;
    }

    public void setTaskPwd(String taskPwd) {
        this.taskPwd = taskPwd;
    }

    public String getInferenceFlag() {
        return inferenceFlag;
    }

    public void setInferenceFlag(String inferenceFlag) {
        this.inferenceFlag = inferenceFlag;
    }
}
