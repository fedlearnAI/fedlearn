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

import com.jdt.fedlearn.common.entity.Job;
import com.jdt.fedlearn.common.entity.JobReq;
import com.jdt.fedlearn.common.entity.JobResult;
import com.jdt.fedlearn.common.enums.TaskTypeEnum;
import org.testng.Assert;
import org.testng.annotations.Test;


public class NameUtilTest {

    @Test
    public void generateJobID() {
        for(int i = 0; i<2 ;i++){
            if(i == 0){
                JobReq jobReq = new JobReq();
                jobReq.setJobId("1");
                String s = NameUtil.generateJobID(jobReq);
                Assert.assertEquals(s,"1");
            }else{
                String s = NameUtil.generateJobID(new JobReq());
                Assert.assertEquals(s.length(),32);
            }
        }
    }

    @Test
    public void generateTaskID() {
        JobReq jobReq = new JobReq();
        jobReq.setJobId("1");
        Job job = new Job(jobReq,new JobResult());
        String s = NameUtil.generateTaskID(job, TaskTypeEnum.MAP, 10);
        Assert.assertTrue(s.contains("@MAP@10@"));
    }
}
