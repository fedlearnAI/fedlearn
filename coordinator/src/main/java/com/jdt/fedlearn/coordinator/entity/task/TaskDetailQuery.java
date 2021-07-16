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
import scala.io.BytePickle;


import java.io.IOException;

/**
 * 用于根据id查询任务详情的实体，记录taskID和用户名两方面信息
 *
 * @author lijingxi
 */
public class TaskDetailQuery {
    private String taskId;
    private String username;

    public TaskDetailQuery() {
    }

    public TaskDetailQuery(String taskId, String username) {
        this.taskId = taskId;
        this.username = username;
    }

    public TaskDetailQuery(String jsonStr) {
        parseJson(jsonStr);
    }

    public String getTaskId() {
        return this.taskId;
    }

    public String getUsername() {
        return this.username;
    }

    public void parseJson(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        TaskDetailQuery p3r;
        try {
            p3r = mapper.readValue(jsonStr, TaskDetailQuery.class);
            this.taskId = p3r.taskId;
            this.username = p3r.username;
        } catch (IOException e) {
            throw new DeserializeException(e.getMessage());
        }
    }

}
