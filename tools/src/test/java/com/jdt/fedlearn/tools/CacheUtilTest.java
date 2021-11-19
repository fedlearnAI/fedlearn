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


import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class CacheUtilTest {
    CacheUtil cacheUtil;
    private static final String CACHE_KEY = "test";
    private static final String CACHE_VALUE = "test1111";

    @BeforeClass
    public void setUp(){
        cacheUtil = new CacheUtil(100L, TimeUnit.SECONDS.toSeconds(30));
        cacheUtil.putValue(CACHE_KEY,CACHE_VALUE);
       String res = (String) cacheUtil.getValue(CACHE_KEY);
        System.out.println(res);
    }

    @Test
    public void getValue() {
        Object value = cacheUtil.getValue(CACHE_KEY);
        Assert.assertEquals(CACHE_VALUE,value);
    }

    @Test
    public void containsKey(){
        System.out.println(cacheUtil.constainsKey(CACHE_KEY));
    }
    @Test
    public void testGetValue() throws Exception {
        String NO_VALUE = "noValue";
        Callable callable = () -> NO_VALUE;
        Object noValue = cacheUtil.getValue("no", callable);
        Assert.assertEquals(NO_VALUE,noValue);
    }

    @Test
    public void putValue() {
        String key = "key";
        String value = "value";
        cacheUtil.putValue(key,value);
        String value1 = (String) cacheUtil.getValue(key);
        Assert.assertEquals(value,value1);
    }

}
