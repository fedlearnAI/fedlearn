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

package com.jdt.fedlearn.coordinator.entity.inference;

/**
 * 推理日志查询的请求实体，包含{@code modelToken}、用户名、推理结果、推理开始时间、推理结束时间、页码信息、调用推理用户
 * @author lijingxi
 */
public class InferenceDto {
    private String modelToken;
    private String startTime;
    private String endTime;
    private String pageIndex;
    private String pageSize;
    /*username表示登陆了前端页面的用户，caller表示调用了推理的人 */
    private String caller;

    public String getModelToken() {
        return modelToken;
    }

    public void setModelToken(String modelToken) {
        this.modelToken = modelToken;
    }


    public String getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(String pageIndex) {
        this.pageIndex = pageIndex;
    }

    public String getPageSize() {
        return pageSize;
    }

    public void setPageSize(String pageSize) {
        this.pageSize = pageSize;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }


    public String getCaller() {
        return caller;
    }

    public void setCaller(String caller) {
        this.caller = caller;
    }
}
