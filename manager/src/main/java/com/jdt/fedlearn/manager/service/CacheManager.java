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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @className: CacheManager
 * @description: 保存worker需要缓存的数据
 * @author: geyan29
 * @createTime: 2021/4/29 4:39 下午
 */
@Component
public class CacheManager {

    /* 保存树的id*/
    public static Map<String,String> treeIdCacheMap = new ConcurrentHashMap<>(16);
    /* 保存训练结果所在worker的地址*/
    public static Map<String,String> resultAddressCacheMap = new ConcurrentHashMap<>(16);
    /* 保存init的model及TrainData所在worker的地址*/
    public static Map<String,String> modelAddressCacheMap = new ConcurrentHashMap<>(16);

    /**
    * @description: 添加缓存
    * @param param 参数
    * @return: void
    */
    public void putCache(Map<String,String> param){
        String type = param.get(AppConstant.MANAGER_CACHE_TYPE);
        String key = param.get(AppConstant.MANAGER_CACHE_KEY);
        String value = param.get(AppConstant.MANAGER_CACHE_VALUE);
        if(AppConstant.MODEL_ADDRESS_CACHE.equals(type)){
            modelAddressCacheMap.put(key,value);
        }else if(AppConstant.MODEL_COUNT_CACHE.equals(type)){
            treeIdCacheMap.put(key,value);
        }else if(AppConstant.RESULT_ADDRESS_CACHE.equals(type)){
            resultAddressCacheMap.put(key,value);
        }
    }

    /**
    * @description: 查询缓存
    * @param param
    * @return: java.lang.String
    * @author: geyan29
    * @date: 2021/4/29 4:40 下午
    */
    public String getCache(Map<String,String> param){
        String type = param.get(AppConstant.MANAGER_CACHE_TYPE);
        String key = param.get(AppConstant.MANAGER_CACHE_KEY);
        String result = null;
        if(AppConstant.MODEL_ADDRESS_CACHE.equals(type)){
             result = modelAddressCacheMap.get(key);
        }else if(AppConstant.MODEL_COUNT_CACHE.equals(type)){
             result = treeIdCacheMap.get(key);
        }else if(AppConstant.RESULT_ADDRESS_CACHE.equals(type)){
            result = resultAddressCacheMap.get(key);
        }
        return result;
    }

    /**
    * @description: 删除缓存
    * @param param
    * @return: java.lang.String
    * @author: geyan29
    * @date: 2021/4/29 4:40 下午
    */
    public Object delCache(Map<String,String> param){
        String type = param.get(AppConstant.MANAGER_CACHE_TYPE);
        String key = param.get(AppConstant.MANAGER_CACHE_KEY);
        Object modelStr = null;
        if(AppConstant.MODEL_ADDRESS_CACHE.equals(type)){
             modelStr = modelAddressCacheMap.remove(key);
        }else if(AppConstant.MODEL_COUNT_CACHE.equals(type)){
             modelStr = treeIdCacheMap.remove(key);
        }else if(AppConstant.RESULT_ADDRESS_CACHE.equals(type)){
            modelStr = resultAddressCacheMap.remove(key);
        }
        return modelStr;
    }
}
