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

import com.jdt.fedlearn.client.util.ConfigUtil;
import com.jdt.fedlearn.common.constant.AppConstant;
import com.jdt.fedlearn.common.entity.JobResult;
import com.jdt.fedlearn.common.enums.ManagerCommandEnum;
import com.jdt.fedlearn.tools.ManagerCommandUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * @className: ManagerCache
 * @description: 更新manager缓存
 * @author: geyan29
 * @createTime: 2021/4/29 4:45 下午
 */
public class ManagerCache {
    private static final String managerAddress = ConfigUtil.getProperty("manager.address");

    /***
    * @description: 保存缓存
    * @param type
    * @param key
    * @param model
    * @return: void
    * @author: geyan29
    * @date: 2021/4/29 4:45 下午
    */
    public static void putCache(String type ,String key , String model){
        Map map = new HashMap();
        map.put(AppConstant.MANAGER_CACHE_TYPE,type);
        map.put(AppConstant.MANAGER_CACHE_KEY,key);
        map.put(AppConstant.MANAGER_CACHE_VALUE,model);
        ManagerCommandUtil.request(managerAddress, ManagerCommandEnum.PUT_CACHE,map);
    }

    /***
    * @description: 查询缓存
    * @param type
    * @param key
    * @return: java.lang.String
    * @author: geyan29
    * @date: 2021/4/29 4:45 下午
    */
    public static String getCache(String type, String key){
        Map map = new HashMap();
        map.put(AppConstant.MANAGER_CACHE_TYPE,type);
        map.put(AppConstant.MANAGER_CACHE_KEY,key);
        JobResult result = ManagerCommandUtil.request(managerAddress, ManagerCommandEnum.GET_CACHE, map);
        String modelStr = (String) result.getData().get(AppConstant.MANAGER_CACHE_VALUE);
        return modelStr;
    }

    /***
    * @description: 删除缓存
    * @param type
    * @param key
    * @return: void
    * @author: geyan29
    * @date: 2021/4/29 4:45 下午
    */
    public static void delCache(String type ,String key){
        Map map = new HashMap();
        map.put(AppConstant.MANAGER_CACHE_TYPE,type);
        map.put(AppConstant.MANAGER_CACHE_KEY,key);
        ManagerCommandUtil.request(managerAddress, ManagerCommandEnum.DEL_CACHE, map);
    }

    public static Map<String, String> getCacheByModelToken(String type , String modelToken){
        Map map = new HashMap();
        map.put(AppConstant.MANAGER_CACHE_TYPE,type);
        map.put(AppConstant.MANAGER_CACHE_MODEL_TOKEN,modelToken);
        JobResult result =  ManagerCommandUtil.request(managerAddress, ManagerCommandEnum.GET_CACHE_BY_MODEL_TOKEN, map);
        Map<String,String> resultMap = (Map<String, String>) result.getData().get(AppConstant.MANAGER_CACHE_VALUE);
        return resultMap;
    }
}
