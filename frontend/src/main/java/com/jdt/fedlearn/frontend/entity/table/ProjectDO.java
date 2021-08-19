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
package com.jdt.fedlearn.frontend.entity.table;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jdt.fedlearn.frontend.entity.vo.ProjectListVO;
import com.jdt.fedlearn.frontend.service.IProjectService;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.io.Serializable;

/**
 * <p>
 * 
 * </p>
 *
 * @author geyan
 * @since 2021-07-08
 */
@TableName("project_table")
public class ProjectDO implements Serializable {

    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String taskName;
    private String taskOwner;
    private String partners;
    private LocalDateTime createdTime;
    private LocalDateTime modifiedTime;
    private Integer status;
    private String hasPwd;
    private String taskPwd;
    private String merCode;
    private String visible;
    private String visibleMercode;
    private String inferenceFlag;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getTaskOwner() {
        return taskOwner;
    }

    public void setTaskOwner(String taskOwner) {
        this.taskOwner = taskOwner;
    }

    public String getPartners() {
        return partners;
    }

    public void setPartners(String partners) {
        this.partners = partners;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public LocalDateTime getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(LocalDateTime modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getHasPwd() {
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

    public String getVisibleMercode() {
        return visibleMercode;
    }

    public void setVisibleMercode(String visibleMercode) {
        this.visibleMercode = visibleMercode;
    }

    public String getInferenceFlag() {
        return inferenceFlag;
    }

    public void setInferenceFlag(String inferenceFlag) {
        this.inferenceFlag = inferenceFlag;
    }

    @Override
    public String toString() {
        return "TaskTable{" +
        "id=" + id +
        ", taskName=" + taskName +
        ", taskOwner=" + taskOwner +
        ", partners=" + partners +
        ", createdTime=" + createdTime +
        ", modifiedTime=" + modifiedTime +
        ", status=" + status +
        ", hasPwd=" + hasPwd +
        ", taskPwd=" + taskPwd +
        ", merCode=" + merCode +
        ", visible=" + visible +
        ", visibleMercode=" + visibleMercode +
        ", inferenceFlag=" + inferenceFlag +
        "}";
    }

    public static ProjectListVO convert2TaskVO(ProjectDO task){
        ProjectListVO taskVO = new ProjectListVO();
        taskVO.setOwner(task.getTaskOwner());
        taskVO.setTaskId(task.getId());
        taskVO.setTaskName(task.getTaskName());
        taskVO.setHasPwd(task.getHasPwd());
        taskVO.setVisible(task.getVisible());
//        taskVO.setVisibleMerName(codeConvertName(task.getVisibleMercode()));
        taskVO.setVisibleMerCode(task.getVisibleMercode());
        taskVO.setInferenceFlag(task.getInferenceFlag());
        taskVO.setMerCode(task.getMerCode());
        String partners = task.getPartners();
        if (StringUtils.isNotBlank(partners)) {
            if (partners.contains("[")) {//兼容旧数据，旧数据保存格式为[user1,user2]
                taskVO.setParticipants(partners.substring(1, partners.length() - 1).split(IProjectService.COMMA));
            } else {
                taskVO.setParticipants(partners.split(IProjectService.COMMA));
            }
        } else {
            taskVO.setParticipants(new String[0]);
        }
        return taskVO;
    }
}
