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
package com.jdt.fedlearn.worker.cache;

import com.jdt.fedlearn.common.entity.Task;
import com.jdt.fedlearn.common.entity.TaskResultData;
import com.jdt.fedlearn.common.enums.ExceptionEnum;
import com.jdt.fedlearn.common.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: liuzhaojun10
 * @Date: 2020/8/26 11:15
 * @Description: cache没问题， 但是整体上有个问题，
 *
 */
@Component
public class WorkerResultCache {
    // taskid, taskData
    private Map<String, TaskResultData> taskResultDataMap = new HashMap<>();


    public void put(TaskResultData taskResultData) {
        if(StringUtils.isEmpty(taskResultData.getTaskId())){
            throw new BusinessException("task id 不能为空", ExceptionEnum.DATA_ERROR);
        }
        taskResultDataMap.put(taskResultData.getTaskId(), taskResultData);
    }

    public TaskResultData get(Task task) {
        return taskResultDataMap.get(task.getTaskId());
    }

    public TaskResultData remove(Task task) {
        return taskResultDataMap.remove(task.getTaskId());
    }


}
