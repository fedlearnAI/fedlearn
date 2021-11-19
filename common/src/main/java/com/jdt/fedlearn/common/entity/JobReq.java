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
package com.jdt.fedlearn.common.entity;

import com.jdt.fedlearn.common.enums.BusinessTypeEnum;
import com.jdt.fedlearn.common.enums.ManagerCommandEnum;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * @Description: job请求参数
 */
public class JobReq implements Serializable {

    //用户名称
    private String username;
    //command
    private ManagerCommandEnum managerCommandEnum;
    // demo or 实际业务
    private BusinessTypeEnum businessTypeEnum;
    //任务id
    private String jobId;

    //主要请求内容
    private TrainRequest trainRequest;

    //其他参数
    private Map<String, Object> params;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public TrainRequest getSubRequest() {
        return trainRequest;
    }

    public void setSubRequest(TrainRequest trainRequest) {
        this.trainRequest = trainRequest;
    }

    public ManagerCommandEnum getManagerCommandEnum() {
        return managerCommandEnum;
    }

    public void setManagerCommandEnum(ManagerCommandEnum managerCommandEnum) {
        this.managerCommandEnum = managerCommandEnum;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public BusinessTypeEnum getBusinessTypeEnum() {
        return businessTypeEnum;
    }

    public void setBusinessTypeEnum(BusinessTypeEnum businessTypeEnum) {
        this.businessTypeEnum = businessTypeEnum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JobReq jobReq = (JobReq) o;
        return Objects.equals(username, jobReq.username) &&
                managerCommandEnum == jobReq.managerCommandEnum &&
                businessTypeEnum == jobReq.businessTypeEnum &&
                Objects.equals(jobId, jobReq.jobId) &&
                Objects.equals(trainRequest, jobReq.trainRequest) &&
                Objects.equals(params, jobReq.params);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, managerCommandEnum, businessTypeEnum, jobId, trainRequest, params);
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }
}
