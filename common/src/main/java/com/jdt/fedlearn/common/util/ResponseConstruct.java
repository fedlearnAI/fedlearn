package com.jdt.fedlearn.common.util;

import com.jdt.fedlearn.common.constant.ResponseConstant;

import java.util.HashMap;
import java.util.Map;

/**
 * 简化返回结果的构造
 */
public class ResponseConstruct {

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
