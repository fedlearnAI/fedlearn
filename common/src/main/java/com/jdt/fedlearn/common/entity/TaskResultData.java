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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description: 任务结果数据, 本地存储，
 * 1. 本地存储任务计算结果
 * 2. 其他worker 或者master 调用时，返回计算结果
 * 3. master 清除计算结果
 */
public class TaskResultData implements Serializable {
    //任务id
    private String taskId;
    // 任务结果状态
    private CommonResultStatus commonResultStatus;
    //原始结果信息
    private Map<String, Object> modelMap = new HashMap<>();

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public CommonResultStatus getTaskResultStatus() {
        return commonResultStatus;
    }

    public void setTaskResultStatus(CommonResultStatus commonResultStatus) {
        this.commonResultStatus = commonResultStatus;
    }

    public Map<String, Object> getModelMap() {
        return modelMap;
    }

    public void setModelMap(Map<String, Object> modelMap) {
        this.modelMap = modelMap;
    }
}
