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

package com.jdt.fedlearn.coordinator.entity.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.exception.SerializeException;

import java.io.IOException;
import java.util.List;

/**
 * 通用请求，包括taskList和type
 * taskList指需要查询的task列表
 * type指任务状态， TODO 需要改成枚举类
 */
public class CommonQuery implements Message {
    private List<String> taskList;
    private String type;

    public CommonQuery() {
    }

    public CommonQuery(List<String> taskList, String type) {
        this.taskList = taskList;
        this.type = type;
    }

    public CommonQuery(String jsonStr) {
        parseJson(jsonStr);
    }


    public List<String> getTaskList() {
        return taskList;
    }

    public String getType() {
        return type;
    }

    public void parseJson(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        CommonQuery p3r = null;
        try {
            p3r = mapper.readValue(jsonStr, CommonQuery.class);
            this.taskList = p3r.taskList;
            this.type = p3r.type;
        } catch (IOException e) {
            throw new SerializeException("predict Phase1 Request to json");
        }
    }

}
