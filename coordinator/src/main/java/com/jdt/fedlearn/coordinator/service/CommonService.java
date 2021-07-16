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

package com.jdt.fedlearn.coordinator.service;


import com.google.common.collect.Maps;
import com.jdt.fedlearn.common.constant.ResponseConstant;
import com.jdt.fedlearn.coordinator.exception.ForbiddenException;
import com.jdt.fedlearn.coordinator.exception.NotAcceptableException;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;


/**
 * common service 只实现公共接口，start和predict接口自己实现
 */
public class CommonService {
    /**
     * 请求失败
     *
     * @return Map
     */
    public static Map<String, Object> fail(String failCode) {
        Map<String, Object> modelMap = Maps.newHashMap();
        modelMap.put(ResponseConstant.STATUS, ResponseConstant.FAIL);
        modelMap.put(ResponseConstant.CODE, StringUtils.isNotBlank(failCode) ? failCode : ResponseConstant.FAIL_CODE);
        return modelMap;
    }

    /**
     * 公共异常处理逻辑
     *
     * @param ex       异常
     * @param modelMap 异常
     * @return 公共异常处理结果
     */
    public static Map<String, Object> exceptionProcess(Exception ex, Map<String, Object> modelMap) {
        if (ex instanceof NotAcceptableException || ex instanceof ForbiddenException) {
            modelMap.put(ResponseConstant.CODE, -1);
            modelMap.put(ResponseConstant.STATUS, ex.getMessage());
            return modelMap;
        } else {
            return null;
        }
    }
}