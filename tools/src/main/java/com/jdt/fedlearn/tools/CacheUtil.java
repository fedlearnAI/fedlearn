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


import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * description:  Guava 本地缓存 -> Cache类型
 * 用于SpringBoot项目中,启用单例模式 项目启动时进行初始化
 * pay attention -> A. 注意不要重复实例化, 最好交由IOC管理
 * B. 注意如果是写操作则获取缓存值后拷贝一份副本，然后传递该副本，进行修改操作
 * C. 支持自定义call回调
 *
 * @author dongluning
 * @version  2020-09-11 10:16 0.6.1
 */
public class CacheUtil {
    /***
     * 构造方法 - 进行初始化
     * @param maxSize      最大容量
     * @param invalidTime  刷新时间 | 基于分钟级别
     */
    public CacheUtil(long maxSize, long invalidTime) {
        init(maxSize, invalidTime);
    }

    /***
     * 初始化
     */
    private void init(long maxSize, long invalidTime) {
        // 缓存
        cache = CacheBuilder.newBuilder()
                // 设置缓存在写入invalidTime分钟后失效
                .expireAfterWrite(invalidTime, TimeUnit.SECONDS)
                // 设置缓存个数
                .maximumSize(maxSize)
                .concurrencyLevel(Runtime.getRuntime().availableProcessors())
                .recordStats()
                .build();
    }


    /***
     * Guava Cache类型缓存
     */
    private Cache cache;

    /**
     * 对外暴露的方法 -> 从缓存中取value，没取到会返回null
     */
    public Object getValue(String key) {
        return cache.getIfPresent(key);
    }

    /**
     * 对外暴露的方法 -> 从缓存中取value，没取到会执行call
     */
    public Object getValue(String key, Callable callable) throws Exception {
        return cache.get(key, callable);
    }

    /**
     * 对外暴露的方法 -> put
     */
    public void putValue(String key, Object value) {
        cache.put(key, value);
    }


    /**
     * 对外暴露的方法 -> 判断是否存在key
     */
    public boolean constainsKey(String key) {
        return cache.asMap().containsKey(key);
    }
}
