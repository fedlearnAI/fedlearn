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
package com.jdt.fedlearn.common.enums;

import com.jdt.fedlearn.common.constant.TaskPriorityConstant;

/**
 * @Description: 任务类型
 */
public enum TaskTypeEnum {

    SINGLE("SINGLE", "单路任务，没有 Init map reduce等", TaskPriorityConstant.INIT_TASK_PRIORITY),
    INIT("INIT", "job切分为tasks", TaskPriorityConstant.INIT_TASK_PRIORITY),
    MAP("MAP", "map类任务", TaskPriorityConstant.MAP_TASK_PRIORITY),
    REDUCE("REDUCE", "reduce类任务", TaskPriorityConstant.REDUCE_TASK_PRIORITY),
    /**
     * 返回通知
     */
    FINISH("FINISH", "finish类任务，返回通知", TaskPriorityConstant.FINISH_TASK_PRIORITY);

    private final String code;
    private final String desc;
    private final int priority;

    TaskTypeEnum(String code, String desc, int priority) {
        this.code = code;
        this.desc = desc;
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }


}
