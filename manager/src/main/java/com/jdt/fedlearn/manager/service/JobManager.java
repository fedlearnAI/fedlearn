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
package com.jdt.fedlearn.manager.service;

import com.jdt.fedlearn.common.entity.Job;
import com.jdt.fedlearn.common.entity.JobReq;
import com.jdt.fedlearn.common.entity.JobResult;
import com.jdt.fedlearn.common.util.NameUtil;
import com.jdt.fedlearn.common.util.TimeUtil;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @Description: job 管理类， 暂时作用不大
 */
@Component
public class JobManager {

    private final Map<String, Job> jobMap = new HashMap<>();

    /**
     * 增加job
     *
     * @param jobReq job请求
     * @return 返回增加job
     */
    public Job addJob(JobReq jobReq) {
        if (StringUtils.isEmpty(jobReq.getJobId())) {
            jobReq.setJobId(NameUtil.generateJobID(jobReq));
        }
        //当同一个job来的使用，不用反复增加， 最后集中一次清理
        if (!jobMap.containsKey(jobReq.getJobId())) {
            Job job = new Job(jobReq, new JobResult());
            job.getJobResult().setStartTime(TimeUtil.getNowTime());
            jobMap.put(jobReq.getJobId(), job);

        }
        return jobMap.get(jobReq.getJobId());
    }

    /**
     * 删除job
     *
     * @param job 任务
     * @return 删除之后的job
     */
    public Job removeJob(Job job) {
        return jobMap.remove(job.getJobReq().getJobId());
    }

    public Job getJob(JobReq jobReq) {
        return jobMap.get(jobReq.getJobId());
    }
}
