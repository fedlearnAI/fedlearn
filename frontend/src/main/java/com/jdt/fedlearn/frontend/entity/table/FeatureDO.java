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
@TableName("feature_table")
public class FeatureDO implements Serializable {

    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer taskId;

    private String username;

    private String feature;

    private String featureType;

    private String featureDescribe;

    private String depUser;

    private String depFeature;

    private LocalDateTime createdTime;

    private LocalDateTime modifiedTime;

    private Integer status;

    private String isIndex;

    public FeatureDO() {
    }

    public FeatureDO(Integer taskId, String username, String feature, String featureType) {
        this.taskId = taskId;
        this.username = username;
        this.feature = feature;
        this.featureType = featureType;
    }

    public FeatureDO(Integer taskId, String username, String feature, String featureType, String featureDescribe, String isIndex) {
        this.taskId = taskId;
        this.username = username;
        this.feature = feature;
        this.featureType = featureType;
        this.featureDescribe = featureDescribe;
        this.isIndex = isIndex;
    }


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getTaskId() {
        return taskId;
    }

    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFeature() {
        return feature;
    }

    public void setFeature(String feature) {
        this.feature = feature;
    }

    public String getFeatureType() {
        return featureType;
    }

    public void setFeatureType(String featureType) {
        this.featureType = featureType;
    }

    public String getFeatureDescribe() {
        return featureDescribe;
    }

    public void setFeatureDescribe(String featureDescribe) {
        this.featureDescribe = featureDescribe;
    }

    public String getDepUser() {
        return depUser;
    }

    public void setDepUser(String depUser) {
        this.depUser = depUser;
    }

    public String getDepFeature() {
        return depFeature;
    }

    public void setDepFeature(String depFeature) {
        this.depFeature = depFeature;
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

    public String getIsIndex() {
        return isIndex;
    }

    public void setIsIndex(String isIndex) {
        this.isIndex = isIndex;
    }

    @Override
    public String toString() {
        return "FeatureList{" +
                "id=" + id +
                ", taskId=" + taskId +
                ", username=" + username +
                ", feature=" + feature +
                ", featureType=" + featureType +
                ", featureDescribe=" + featureDescribe +
                ", depUser=" + depUser +
                ", depFeature=" + depFeature +
                ", createdTime=" + createdTime +
                ", modifiedTime=" + modifiedTime +
                ", status=" + status +
                ", isIndex=" + isIndex +
                "}";
    }
}
