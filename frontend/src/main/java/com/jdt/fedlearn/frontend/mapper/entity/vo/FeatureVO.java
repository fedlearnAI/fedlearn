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

/**
 * @className: FeatureVo
 * @description: 用于页面展示的VO
 * @author: geyan29
 * @createTime: 2021/1/27 3:48 下午
 */
public class FeatureVO {

    private String taskId;
    private String username;
    private String feature;
    private String feature_type;
    private String feature_describe;
    private String dep_user;
    private String dep_feature;

    public FeatureVO() {
    }

    public FeatureVO(String taskId, String username, String feature, String feature_type) {
        this.taskId = taskId;
        this.username = username;
        this.feature = feature;
        this.feature_type = feature_type;
        this.feature_describe = "";
        this.dep_user = "";
        this.dep_feature = "";
    }

    public FeatureVO(String taskId, String username, String feature, String feature_type, String feature_describe, String dep_user, String dep_feature) {
        this.taskId = taskId;
        this.username = username;
        this.feature = feature;
        this.feature_type = feature_type;
        this.feature_describe = feature_describe;
        this.dep_user = dep_user;
        this.dep_feature = dep_feature;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
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

    public String getFeature_type() {
        return feature_type;
    }

    public void setFeature_type(String feature_type) {
        this.feature_type = feature_type;
    }

    public String getFeature_describe() {
        return feature_describe;
    }

    public void setFeature_describe(String feature_describe) {
        this.feature_describe = feature_describe;
    }

    public String getDep_user() {
        return dep_user;
    }

    public void setDep_user(String dep_user) {
        this.dep_user = dep_user;
    }

    public String getDep_feature() {
        return dep_feature;
    }

    public void setDep_feature(String dep_feature) {
        this.dep_feature = dep_feature;
    }
}
