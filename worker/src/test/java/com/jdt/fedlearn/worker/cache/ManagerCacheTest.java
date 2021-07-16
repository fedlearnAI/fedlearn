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

import com.jdt.fedlearn.common.constant.AppConstant;
import com.jdt.fedlearn.common.entity.JobResult;
import com.jdt.fedlearn.common.util.ManagerCommandUtil;
import com.jdt.fedlearn.worker.util.ConfigUtil;

import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;


@PrepareForTest({ManagerCommandUtil.class})
public class ManagerCacheTest extends PowerMockTestCase {
    private static final String cacheValue = "test";

    @BeforeMethod
    public void setUp(){

        ConfigUtil.init("src/test/resources/conf/worker.properties");

        PowerMockito.mockStatic(ManagerCommandUtil.class);
        JobResult result = new JobResult();
        Map<String ,Object> map = new HashMap<>();
        map.put(AppConstant.MANAGER_CACHE_VALUE,cacheValue);
        result.setData(map);
        PowerMockito.when(ManagerCommandUtil.request(Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(result);
    }

    @Test
    public void putCache() {
        ManagerCache.putCache("type","key","value");
    }

    @Test
    public void getCache() {
        String cache = ManagerCache.getCache("type", "key");
        Assert.assertEquals(cache,cacheValue);
    }

    @Test
    public void delCache() {
        ManagerCache.delCache("type","key");
    }
}
