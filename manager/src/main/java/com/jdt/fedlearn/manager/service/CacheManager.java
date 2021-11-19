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
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @className: CacheManager
 * @description: 保存worker需要缓存的数据
 * @author: geyan29
 * @createTime: 2021/4/29 4:39 下午
 */
@Component
public class CacheManager {

    /* 保存树的id*/
    public static Map<String, String> treeIdCacheMap = new ConcurrentHashMap<>(16);
    /* 保存训练结果所在worker的地址*/
    public static Map<String, String> resultAddressCacheMap = new ConcurrentHashMap<>(16);
    /* 保存init的model及TrainData所在worker的地址*/
    public static Map<String, String> modelAddressCacheMap = new ConcurrentHashMap<>(16);
    /* 保存map message所在worker的地址*/
    public static Map<String, String> messageAddressCacheMap = new ConcurrentHashMap<>(16);
    /* 保存拆分后的message所在worker地址*/
    public static Map<String, String> subMessageAddressCacheMap = new ConcurrentHashMap<>(16);

    /**
     * @param param 参数
     * @description: 添加缓存
     * @return: void
     */
    public void putCache(Map<String, String> param) {
        String type = param.get(AppConstant.MANAGER_CACHE_TYPE);
        String key = param.get(AppConstant.MANAGER_CACHE_KEY);
        String value = param.get(AppConstant.MANAGER_CACHE_VALUE);
        if (AppConstant.MODEL_ADDRESS_CACHE.equals(type)) {
            modelAddressCacheMap.put(key, value);
        } else if (AppConstant.MODEL_COUNT_CACHE.equals(type)) {
            treeIdCacheMap.put(key, value);
        } else if (AppConstant.RESULT_ADDRESS_CACHE.equals(type)) {
            resultAddressCacheMap.put(key, value);
        } else if (AppConstant.MODEL_MESSAGE_CACHE.equals(type)) {
            messageAddressCacheMap.put(key, value);
        } else if (AppConstant.SUB_MESSAGE_CACHE.equals(type)) {
            subMessageAddressCacheMap.put(key,value);
        }
    }

    /**
     * @param param
     * @description: 查询缓存
     * @return: java.lang.String
     * @author: geyan29
     * @date: 2021/4/29 4:40 下午
     */
    public String getCache(Map<String, String> param) {
        String type = param.get(AppConstant.MANAGER_CACHE_TYPE);
        String key = param.get(AppConstant.MANAGER_CACHE_KEY);
        String result = null;
        if (AppConstant.MODEL_ADDRESS_CACHE.equals(type)) {
            result = modelAddressCacheMap.get(key);
        } else if (AppConstant.MODEL_COUNT_CACHE.equals(type)) {
            result = treeIdCacheMap.get(key);
        } else if (AppConstant.RESULT_ADDRESS_CACHE.equals(type)) {
            result = resultAddressCacheMap.get(key);
        } else if (AppConstant.MODEL_MESSAGE_CACHE.equals(type)) {
            result = messageAddressCacheMap.get(key);
        } else if (AppConstant.SUB_MESSAGE_CACHE.equals(type)) {
            result = subMessageAddressCacheMap.get(key);
        }
        return result;
    }

    /**
     * @param param
     * @description: 删除缓存
     * @return: java.lang.String
     * @author: geyan29
     * @date: 2021/4/29 4:40 下午
     */
    public Object delCache(Map<String, String> param) {
        String type = param.get(AppConstant.MANAGER_CACHE_TYPE);
        String key = param.get(AppConstant.MANAGER_CACHE_KEY);
        Object modelStr = null;
        if (AppConstant.MODEL_ADDRESS_CACHE.equals(type)) {
            modelStr = modelAddressCacheMap.remove(key);
        } else if (AppConstant.MODEL_COUNT_CACHE.equals(type)) {
            modelStr = treeIdCacheMap.remove(key);
        } else if (AppConstant.RESULT_ADDRESS_CACHE.equals(type)) {
            modelStr = resultAddressCacheMap.remove(key);
        } else if (AppConstant.MODEL_MESSAGE_CACHE.equals(type)) {
            modelStr = messageAddressCacheMap.remove(key);
        } else if (AppConstant.SUB_MESSAGE_CACHE.equals(type)) {
            modelStr = subMessageAddressCacheMap.remove(key);
        }
        return modelStr;
    }

    public void delCache(String type, String modelToken) {
        if (AppConstant.MODEL_ADDRESS_CACHE.equals(type)) {
            modelAddressCacheMap.keySet().parallelStream().filter(k -> k.contains(modelToken)).forEach(k -> modelAddressCacheMap.remove(k));
        } else if (AppConstant.MODEL_COUNT_CACHE.equals(type)) {
            treeIdCacheMap.keySet().parallelStream().filter(k -> k.contains(modelToken)).forEach(k -> treeIdCacheMap.remove(k));
        } else if (AppConstant.RESULT_ADDRESS_CACHE.equals(type)) {

        } else if (AppConstant.MODEL_MESSAGE_CACHE.equals(type)) {
            messageAddressCacheMap.keySet().parallelStream().filter(k -> k.contains(modelToken)).forEach(k -> messageAddressCacheMap.remove(k));
        } else if (AppConstant.SUB_MESSAGE_CACHE.equals(type)) {
            subMessageAddressCacheMap.keySet().parallelStream().filter(k -> k.contains(modelToken)).forEach(k -> subMessageAddressCacheMap.remove(k));
        }
    }

    public Map<String,String> getCacheByModelToken(Map<String,String> param){
        String type = param.get(AppConstant.MANAGER_CACHE_TYPE);
        String modelToken = param.get(AppConstant.MANAGER_CACHE_MODEL_TOKEN);
        Map<String, String> result = new HashMap<>();
        if(AppConstant.SUB_MESSAGE_CACHE.equals(type)){
            result = subMessageAddressCacheMap.entrySet().parallelStream().filter(e -> e.getKey().contains(modelToken)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        return result;
    }
}
