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

import java.io.IOException;

/**
 * 用于查询任务列表的实体，记录包含用户名，查询类别（创建、已加入、可加入三种类别），企业编码的信息
 * @author lijingxi
 */
public class TaskListQuery {
    private String username;
    private String category;
    private String merCode;

    public TaskListQuery() {
    }

    public TaskListQuery(String username, String category, String merCode) {
        this.username = username;
        this.category = category;
        this.merCode = merCode;
    }

    public TaskListQuery(String jsonStr) {
        parseJson(jsonStr);
    }


    public String getUsername() {
        return this.username;
    }

    public String getCategory() {
        return this.category;
    }

    public String getMerCode() {
        return merCode;
    }

    public void setMerCode(String merCode) {
        this.merCode = merCode;
    }

    public void parseJson(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        TaskListQuery p3r;
        try {
            p3r = mapper.readValue(jsonStr, TaskListQuery.class);
            this.username = p3r.username;
            this.category = p3r.category;
            this.merCode = p3r.merCode;
        } catch (IOException e) {
            throw new DeserializeException(e.getMessage());
        }
    }
}
