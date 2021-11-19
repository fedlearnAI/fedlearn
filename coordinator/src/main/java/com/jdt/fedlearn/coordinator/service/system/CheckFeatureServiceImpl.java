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

package com.jdt.fedlearn.coordinator.service.system;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.common.constant.ResponseConstant;
import com.jdt.fedlearn.coordinator.constant.RequestConstant;
import com.jdt.fedlearn.coordinator.entity.system.FeatureMapDTO;
import com.jdt.fedlearn.coordinator.entity.system.FeatureReq;
import com.jdt.fedlearn.coordinator.exception.UnauthorizedException;
import com.jdt.fedlearn.coordinator.network.SendAndRecv;
import com.jdt.fedlearn.coordinator.service.IDispatchService;
import com.jdt.fedlearn.tools.internel.ResponseInternal;
import com.jdt.fedlearn.tools.serializer.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.jdt.fedlearn.core.util.Tool;

/**
 * 查询数据源的接口实现类
 */
public class CheckFeatureServiceImpl implements IDispatchService {
    private static final Logger logger = LoggerFactory.getLogger(CheckFeatureServiceImpl.class);

    @Override
    public Map<String, Object> service(String content) {
        Map<String, Object> modelMap = new HashMap<>();
        Map<String, Object> request = JsonUtil.object2map(content);
        try {
            request.get("expr");
            request.get("featureList");
            Tool.checkExpression((String)request.get("expr"), (List<String>)request.get("featureList"));
            modelMap.put(ResponseConstant.DATA, "success");
            modelMap.put(ResponseConstant.STATUS, ResponseConstant.SUCCESS);
            modelMap.put(ResponseConstant.CODE, ResponseConstant.SUCCESS_CODE);
        } catch (Exception e) {
            logger.error(e.getMessage());
            modelMap.put(ResponseConstant.DATA, e.getMessage());
            modelMap.put(ResponseConstant.STATUS, ResponseConstant.FAIL);
            modelMap.put(ResponseConstant.CODE, ResponseConstant.FAIL_CODE);
        }
        return modelMap;
    }

}
