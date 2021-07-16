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

import com.jdt.fedlearn.common.util.ToString;

import java.util.Objects;

/**
 * @Description: 任务入口总入口
 */
public class Job extends ToString {
    //请求参数
    private JobReq jobReq;
    //结果参数
    private JobResult jobResult;

    public Job() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        };
        Job job = (Job) o;
        return Objects.equals(jobReq, job.jobReq) &&
                Objects.equals(jobResult, job.jobResult);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobReq, jobResult);
    }


    /**
     * 任务构造函数
     * @param jobReq    job请求
     * @param jobResult job处理结果
     */
    public Job(JobReq jobReq, JobResult jobResult) {
        this.jobReq = jobReq;
        this.jobResult = jobResult;
    }

    public JobReq getJobReq() {
        return jobReq;
    }


    public JobResult getJobResult() {
        return jobResult;
    }
}
