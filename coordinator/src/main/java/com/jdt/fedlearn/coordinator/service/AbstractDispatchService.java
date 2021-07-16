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

import java.util.HashMap;
import java.util.Map;

/**
 * 处理调用结果抽象类
 *
 * AbstractDispatchService
 */
public abstract class AbstractDispatchService implements IAbstractDispatchService {

    /**
     * 流程模板方法
     */
    public Map<String, Object> doProcess(boolean isSuccess) {
        // 如果失败，建议传入空对象，而不是null, 否则需要返回具体结果对象
        return isSuccess ? success(dealService()) : fail();
    }

    /**
     * 处理成功
     *
     * @return modelMap
     */
    private Map<String, Object> success(Object resultData) {
        Map<String, Object> modelMap = new HashMap<>();
        modelMap.put(ResponseConstant.STATUS, ResponseConstant.SUCCESS);
        modelMap.put(ResponseConstant.CODE, ResponseConstant.SUCCESS_CODE);
        modelMap.put(ResponseConstant.DATA, resultData);
        return modelMap;
    }

    /**
     * 处理失败
     *
     * @return modelMap
     */
    private Map<String, Object> fail() {
        Map<String, Object> modelMap = Maps.newHashMap();
        modelMap.put(ResponseConstant.STATUS, ResponseConstant.FAIL);
        modelMap.put(ResponseConstant.CODE, ResponseConstant.FAIL_CODE);
        modelMap.put(ResponseConstant.DATA, Maps.newHashMap());
        return modelMap;
    }
}
