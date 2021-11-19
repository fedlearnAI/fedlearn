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
package com.jdt.fedlearn.manager;

import com.jdt.fedlearn.common.constant.ResponseConstant;
import com.jdt.fedlearn.common.entity.*;
import com.jdt.fedlearn.common.enums.BusinessTypeEnum;
import com.jdt.fedlearn.common.enums.ManagerCommandEnum;
import com.jdt.fedlearn.common.enums.ResultTypeEnum;
import com.jdt.fedlearn.manager.service.JobManager;
import com.jdt.fedlearn.manager.spring.SpringBean;
import com.jdt.fedlearn.manager.util.ConfigUtil;

import com.jdt.fedlearn.tools.WorkerCommandUtil;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.any;

@PrepareForTest({WorkerCommandUtil.class})
public class ManagerLocalAppTest extends PowerMockTestCase {
    private ManagerLocalApp managerLocalApp;

    @BeforeClass
    public void setUp(){
        ConfigUtil.init("src/test/resources/conf/manager.properties");
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(SpringBean.class);
        managerLocalApp = applicationContext.getBean("managerLocalApp", ManagerLocalApp.class);
    }

    @Test
    public void init() {
    }

    @Test
    public void close() {
    }

    @Test
    public void process() throws InterruptedException, IOException {
        JobReq jobReq = new JobReq();
        jobReq.setJobId("1");
        jobReq.setManagerCommandEnum(ManagerCommandEnum.DEMO);
        jobReq.setBusinessTypeEnum(BusinessTypeEnum.DEMO);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                PowerMockito.mockStatic(WorkerCommandUtil.class);
                CommonResultStatus commonResultStatus = new CommonResultStatus();
                Map map = new HashMap();
                map.put(ResponseConstant.DATA,"true");
                commonResultStatus.setData(map);
                commonResultStatus.setResultTypeEnum(ResultTypeEnum.SUCCESS);
                try {
                    PowerMockito.when(WorkerCommandUtil.processTaskRequest(any(),any())).thenReturn(commonResultStatus);
                    JobResult result = managerLocalApp.process(jobReq);
                    Assert.assertEquals(ResultTypeEnum.SUCCESS,result.getResultTypeEnum());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
        Thread.sleep(3000l);
        JobManager jobManager = managerLocalApp.getJobManager();
        Job job = jobManager.getJob(jobReq);
        synchronized (job){
            job.getJobResult().setResultTypeEnum(ResultTypeEnum.SUCCESS);
            job.notify();
        }
        Thread.sleep(3000l);
    }


    @Test
    public void getJobManager() {
    }
}
