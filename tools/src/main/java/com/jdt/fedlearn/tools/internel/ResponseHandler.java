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
package com.jdt.fedlearn.tools.internel;

import com.jdt.fedlearn.common.constant.ResponseConstant;
import com.jdt.fedlearn.tools.serializer.JsonUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * 用于对外的 HTTP 服务, 不适用于系统内部传输
 */
public class ResponseHandler {

    /**
     *
     * @param data 数据，
     * @return 返回结果，包含data， code 和status 三项，其中data是
     */
    public static Map<String, Object> success(Map<String, Object> data) {
        Map<String, Object> modelMap = success();
        modelMap.put(ResponseConstant.DATA, data);
        return modelMap;
    }

    public static Map<String, Object> success() {
        Map<String, Object> modelMap = new HashMap<>();
        modelMap.put(ResponseConstant.STATUS, ResponseConstant.SUCCESS);
        modelMap.put(ResponseConstant.CODE, ResponseConstant.SUCCESS_CODE);
        return modelMap;
    }

    /**
     * 带返回值的成功
     * @param data 返回值
     * @return 返回
     */
    public static Map<String, Object> successResponse(Object data){
        Map<String, Object> modelMap = new HashMap<>();
        modelMap.put(ResponseConstant.STATUS, ResponseConstant.SUCCESS);
        modelMap.put(ResponseConstant.CODE, ResponseConstant.SUCCESS_CODE);
        modelMap.put(ResponseConstant.DATA, data);
        return modelMap;
    }


    public static Map<String, Object> error(int code, String message) {
        Map<String, Object> modelMap = new HashMap<>();
        modelMap.put(ResponseConstant.CODE, code);
        modelMap.put(ResponseConstant.STATUS, message);
        return modelMap;
    }

    public static Map<String, Object> error(String message) {
        return error(-1, message);
    }

    public static String successJson(Map<String, Object> data) {
        Map<String, Object> modelMap = new HashMap<>();
        modelMap.put(ResponseConstant.DATA, data);
        modelMap.put(ResponseConstant.STATUS, ResponseConstant.SUCCESS);
        modelMap.put(ResponseConstant.CODE, ResponseConstant.SUCCESS_CODE);
        return JsonUtil.object2json(modelMap);
    }

    public static String errorJson(int code, String message) {
        Map<String, Object> modelMap = new HashMap<>();
        modelMap.put(ResponseConstant.CODE, code);
        modelMap.put(ResponseConstant.STATUS, message);
        return JsonUtil.object2json(modelMap);
    }

    public static String errorJson(String message) {
        return errorJson(-1, message);
    }
}
