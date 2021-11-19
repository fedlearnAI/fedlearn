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
package com.jdt.fedlearn.tools;

import com.jdt.fedlearn.common.constant.AppConstant;
import com.jdt.fedlearn.common.entity.Job;
import com.jdt.fedlearn.common.entity.JobReq;
import com.jdt.fedlearn.common.enums.TaskTypeEnum;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

/**
 * @Description: 统一命名工具类
 */
public class NameUtil {

    /**
     * 生成jobId
     * @param jobReq job请求
     * @return jobID
     */
    public static String generateJobID(JobReq jobReq) {
        if (StringUtils.isNotEmpty(jobReq.getJobId())) {
            return jobReq.getJobId();
        } else {
            String s = UUID.randomUUID().toString();
            return s.replace("-", "");
        }
    }

    /**
     * 生成taskId
     * @param job job
     * @param taskTypeEnum 任务类型
     * @param priority 任务优先级
     * @return taskID
     */
    public static String generateTaskID(Job job, TaskTypeEnum taskTypeEnum, int priority) {
        return job.getJobReq().getJobId() + AppConstant.AT_SPLIT
                + taskTypeEnum.getCode() + AppConstant.AT_SPLIT
                + priority + AppConstant.AT_SPLIT
                + System.nanoTime();
    }
}


