package com.jdt.fedlearn.tools.internel;

import com.jdt.fedlearn.common.constant.ResponseConstant;
import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.tools.serializer.JsonUtil;
import com.jdt.fedlearn.tools.serializer.JavaSerializer;
import com.jdt.fedlearn.tools.serializer.Serializer;

import java.util.HashMap;
import java.util.Map;

/**
 * 简化内部交互 返回结果的构造，返回结果分为三部分，
 * code 是标志码， 0为正常返回，负数为异常返回， -1为请求参数异常， -2为内部处理异常
 * status 是状态消息， code 为0时，status为 success或其他描述， code为负数时， status为报错原因
 * data 是数据，需要为 Message 类型， 特殊情况可为空
 */
public class ResponseConstruct {
    private static final Serializer serializer = new JavaSerializer();

    /**
     * @param message 数据，
     * @return 返回结果，包含data， code 和status 三项，其中data是
     */
    public static Map<String, Object> success(Message message) {
        Map<String, Object> modelMap = success();
        String dataStr = serializer.serialize(message);
        modelMap.put(ResponseConstant.DATA, dataStr);
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
