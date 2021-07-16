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

import com.jdt.fedlearn.common.constant.AppConstant;
import com.jdt.fedlearn.common.entity.CommonResultStatus;
import com.jdt.fedlearn.common.entity.WorkerUnit;
import com.jdt.fedlearn.common.entity.Task;
import com.jdt.fedlearn.common.enums.WorkerCommandEnum;
import com.jdt.fedlearn.common.util.WorkerCommandUtil;

import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.Test;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;

@PrepareForTest({WorkerCommandUtil.class, WorkerManagerTest.class})
public class WorkerManagerTest extends PowerMockTestCase {
    WorkerManager workerManager;
    WorkerUnit workerUnit;

    @BeforeClass
    public void setUp(){
        workerManager = new WorkerManager();
        workerUnit = new WorkerUnit();
        workerUnit.setIp("127.0.0.1");
        workerUnit.setPort(8080);
        workerUnit.setName("testUnit");
        workerManager.addWorkerUnit(workerUnit);
    }

    @Test
    public void updateWorkerUnitStatus() {
        workerManager.updateWorkerUnitStatus(workerUnit,true);
        AtomicBoolean atomicBoolean = workerManager.getWorkerUnitMap().get(workerUnit);
        Assert.assertEquals(atomicBoolean.get(),true);
    }

    @Test
    public void addWorkerUnit() {
        boolean b = workerManager.addWorkerUnit(workerUnit);
        Assert.assertEquals(b,false);
    }

    @Test
    public void getWorkerUnitMap() {
        AtomicBoolean atomicBoolean = workerManager.getWorkerUnitMap().get(workerUnit);
        Assert.assertEquals(atomicBoolean.get(),true);
    }

    @Test
    public void getFirstReadyWorkerUnit() throws IOException {
        mockRequest();
        WorkerUnit firstReadySalveUnit = workerManager.getFirstReadyWorkerUnit(new Task());
        Assert.assertEquals(firstReadySalveUnit, workerUnit);
    }


    private void mockRequest() throws IOException {
        PowerMockito.mockStatic(WorkerCommandUtil.class);
        CommonResultStatus commonResultStatus = new CommonResultStatus();
        Map map = new HashMap();
        map.put(AppConstant.DATA,"true");
        commonResultStatus.setData(map);
        PowerMockito.when(WorkerCommandUtil.request(anyString(),any(WorkerCommandEnum.class),any())).thenReturn(commonResultStatus);
    }
}
