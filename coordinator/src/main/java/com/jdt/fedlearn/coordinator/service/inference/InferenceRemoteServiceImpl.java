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

package com.jdt.fedlearn.coordinator.service.inference;

import com.google.common.collect.Maps;
import com.jdt.fedlearn.common.constant.ResponseConstant;
import com.jdt.fedlearn.coordinator.entity.inference.RemotePredict;
import com.jdt.fedlearn.coordinator.service.InferenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * 远端推理的实现类，用于根据存放在客户端的地址进行数据推理
 */
public class InferenceRemoteServiceImpl implements InferenceService {
    private static final Logger logger = LoggerFactory.getLogger(InferenceRemoteServiceImpl.class);

    @Override
    public Map<String, Object> service(String content) {

        Map<String, Object> modelMap = Maps.newHashMap();
        try {
            // 处理流程
            final RemotePredict remotePredict = new RemotePredict(content);
            modelMap.put(ResponseConstant.DATA, InferenceCommonServiceImpl.INFERENCE_SERVICE.predictRemote(remotePredict));
            modelMap.put(ResponseConstant.STATUS, ResponseConstant.SUCCESS);
            modelMap.put(ResponseConstant.CODE, ResponseConstant.SUCCESS_CODE);
        } catch (Exception e) {
            logger.error("远端推理失败", e);
            modelMap.put(ResponseConstant.STATUS, ResponseConstant.FAIL);
            modelMap.put(ResponseConstant.CODE, ResponseConstant.FAIL_CODE);
        }
        return modelMap;
    }
}
