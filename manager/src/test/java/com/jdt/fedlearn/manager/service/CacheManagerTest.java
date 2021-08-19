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

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;


public class CacheManagerTest {

    private String CACHE_KEY = "test";
    private String CACHE_VALUE = "test";

    @BeforeClass
    public void setUp(){
        CacheManager cacheManager = new CacheManager();
        Map<String,String> param = new HashMap();
        param.put(AppConstant.MANAGER_CACHE_KEY,CACHE_KEY);
        param.put(AppConstant.MANAGER_CACHE_VALUE,CACHE_VALUE);
        cacheManager.putCache(param);
    }

    @Test
    public void putCache() {
        String test = CacheManager.modelAddressCacheMap.get(CACHE_KEY);
        Assert.assertEquals(CACHE_VALUE,test);
    }

    @Test
    public void getCache() {
        putCache();
        Map<String,String> param = new HashMap();
        param.put(AppConstant.MANAGER_CACHE_KEY,CACHE_KEY);
        CacheManager cacheManager = new CacheManager();
        String test = cacheManager.getCache(param);
        Assert.assertEquals(CACHE_VALUE,test);
    }

    @Test
    public void delCache() {
        putCache();
        Map<String,String> param = new HashMap();
        param.put(AppConstant.MANAGER_CACHE_KEY,CACHE_KEY);
        CacheManager cacheManager = new CacheManager();
        cacheManager.delCache(param);
        String cache = cacheManager.getCache(param);
        Assert.assertTrue(cache == null);
    }
}
