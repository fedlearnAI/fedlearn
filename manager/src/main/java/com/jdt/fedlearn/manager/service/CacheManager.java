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

    /* 保存每棵树训练所需要的model*/
    public static Map<String,String> modelCacheMap = new ConcurrentHashMap<>(16);
    /* 保存树的id*/
    public static Map<String,String> treeIdCacheMap = new ConcurrentHashMap<>(16);
    /* 保存训练结果所在worker的地址*/
    public static Map<String,String> addressCacheMap = new ConcurrentHashMap<>(16);
    /* 保存首次训练标识*/
    public static Map<String,String> firstCacheMap = new ConcurrentHashMap<>(16);

    /**
    * @description: 添加缓存
    * @param param 参数
    * @return: void
    */
    public void putCache(Map<String,String> param){
        String type = param.get(AppConstant.MANAGER_CACHE_TYPE);
        String key = param.get(AppConstant.MANAGER_CACHE_KEY);
        String value = param.get(AppConstant.MANAGER_CACHE_VALUE);
        if(AppConstant.MODEL_CACHE.equals(type)){
            modelCacheMap.put(key,value);
        }else if(AppConstant.TREE_CACHE.equals(type)){
            treeIdCacheMap.put(key,value);
        }else if(AppConstant.ADDRESS_CACHE.equals(type)){
            addressCacheMap.put(key,value);
        }else if(AppConstant.FIRST_CACHE.equals(type)){
            firstCacheMap.put(key,value);
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
        String modelStr = null;
        if(AppConstant.MODEL_CACHE.equals(type)){
             modelStr = modelCacheMap.get(key);
        }else if(AppConstant.TREE_CACHE.equals(type)){
             modelStr = treeIdCacheMap.get(key);
        }else if(AppConstant.ADDRESS_CACHE.equals(type)){
            modelStr = addressCacheMap.get(key);
        }else if(AppConstant.FIRST_CACHE.equals(type)){
            modelStr = firstCacheMap.get(key);
        }
        return modelStr;
    }

    /**
    * @description: 删除缓存
    * @param param
    * @return: java.lang.String
    * @author: geyan29
    * @date: 2021/4/29 4:40 下午
    */
    public String delCache(Map<String,String> param){
        String type = param.get(AppConstant.MANAGER_CACHE_TYPE);
        String key = param.get(AppConstant.MANAGER_CACHE_KEY);
        String modelStr = null;
        if(AppConstant.MODEL_CACHE.equals(type)){
             modelStr = modelCacheMap.remove(key);
        }else if(AppConstant.TREE_CACHE.equals(type)){
             modelStr = treeIdCacheMap.remove(key);
        }else if(AppConstant.ADDRESS_CACHE.equals(type)){
            modelStr = addressCacheMap.remove(key);
        }else if(AppConstant.FIRST_CACHE.equals(type)){
            modelStr = firstCacheMap.remove(key);
        }
        return modelStr;
    }
}
