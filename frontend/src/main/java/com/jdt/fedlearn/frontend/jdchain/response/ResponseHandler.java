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

package com.jdt.fedlearn.frontend.jdchain.response;

import org.springframework.ui.ModelMap;

import java.util.HashMap;

/**
 * @className: ResponseHandler
 * @description: 返回前端值处理器
 * @author: geyan29
 * @createTime: 2021/1/25 5:55 下午
 */
public class ResponseHandler {
    /**返回数据*/
    private static String DATA = "data";
    /**返回状态码*/
    private static String STATUS = "status";
    /**返回代码*/
    private static String CODE = "code";

    private static Integer SUCCESS_CODE = 0;
    private static Integer FAIL_CODE = -1;
    private static Integer EXIST_CODE = -2;

    public static String SUCCESS_STATUS="success";
    private static String FAIL_STATUS="fail";
    private static String EXIST_STATUS="exist";

    /**
    * @description: 成功返回值
    * @param data
    * @return: org.springframework.ui.ModelMap
    * @author: geyan29
    * @date: 2021/1/25 5:57 下午
    */
    public static ModelMap successResponse(Object data){
        ModelMap modelMap = new ModelMap();
        modelMap.put(CODE,SUCCESS_CODE);
        modelMap.put(STATUS,SUCCESS_STATUS);
        modelMap.put(DATA,data);
        return modelMap;
    }

    /**
    * @description: 不带返回值的成功
    * @return: org.springframework.ui.ModelMap
    * @author: geyan29
    * @date: 2021/1/25 6:25 下午
    */
    public static ModelMap successResponse(){
        ModelMap modelMap = new ModelMap();
        modelMap.put(CODE,SUCCESS_CODE);
        modelMap.put(STATUS,SUCCESS_STATUS);
        modelMap.put(DATA,new HashMap<>());
        return modelMap;
    }

    /**
     * @className: ResponseHandler
     * @description: 失败返回值
     * @author: geyan29
     * @createTime: 2021/1/25 6:01 下午
     */
    public static ModelMap failResponse(){
        ModelMap modelMap = new ModelMap();
        modelMap.put(CODE,FAIL_CODE);
        modelMap.put(STATUS,FAIL_STATUS);
        modelMap.put(DATA,null);
        return modelMap;
    }
    /**
    * @description: 带返回值的失败
    * @param object
    * @return: org.springframework.ui.ModelMap
    * @author: geyan29
    * @date: 2021/1/25 6:09 下午
    */
    public static ModelMap failResponse(Object object){
        ModelMap modelMap = new ModelMap();
        modelMap.put(CODE,FAIL_CODE);
        modelMap.put(STATUS,FAIL_STATUS);
        modelMap.put(DATA,object);
        return modelMap;
    }

    /**
    * @description: 已存在的返回
    * @param
    * @return: org.springframework.ui.ModelMap
    * @author: geyan29
    * @date: 2021/1/25 6:26 下午
    */
    public static ModelMap existResponse(){
        ModelMap modelMap = new ModelMap();
        modelMap.put(CODE,EXIST_CODE);
        modelMap.put(STATUS,EXIST_STATUS);
        return modelMap;
    }
    public static ModelMap existResponse(String msg){
        ModelMap modelMap = new ModelMap();
        modelMap.put(CODE,EXIST_CODE);
        modelMap.put(STATUS,EXIST_STATUS);
        modelMap.put(DATA,msg);
        return modelMap;
    }
}
