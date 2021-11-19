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

import com.jdt.fedlearn.common.constant.ResponseConstant;
import com.jdt.fedlearn.tools.TokenUtil;
import com.jdt.fedlearn.tools.internel.ResponseConstruct;
import com.jdt.fedlearn.coordinator.allocation.ResourceManager;
import com.jdt.fedlearn.coordinator.entity.inference.*;
import com.jdt.fedlearn.coordinator.service.InferenceService;
import com.jdt.fedlearn.tools.internel.ResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 远端推理的实现类，用于根据存放在客户端的地址进行数据推理
 * 分为以下步骤
 * 1.根据用户请求的地址和路径，客户端读取uid文件
 * 2.协调段根据读取的uid列表，进行推理
 * 3.将推理结果发送给上文的客户端，客户端存储到文件中
 * 4.重复上述步骤直至全部uid推理完成
 */
public class InferenceRemoteServiceImpl implements InferenceService {
    private static final Logger logger = LoggerFactory.getLogger(InferenceRemoteServiceImpl.class);

    private static final String INFERENCE_ID = "inferenceId";
    private static final String START_TIME = "startTime";
    private static final String PERCENT = "percent";
    private static final String PREDICT_INFO = "predictInfo";


    @Override
    public Map<String, Object> service(String content) {
        try {
            final RemotePredict remotePredict = new RemotePredict(content);
            Map<String, Object> res = predict(remotePredict);
            return ResponseHandler.success(res);
        } catch (Exception e) {
            logger.error("远端推理失败", e);
            return ResponseConstruct.error(ResponseConstant.FAIL_CODE, ResponseConstant.FAIL);
        }
    }

    /**
     * 为client 推送地址
     *
     * @param remotePredict 远端推理请求
     * @return 推理结果报告
     */
    public Map<String, Object> predict(RemotePredict remotePredict) {
        String inferenceId = TokenUtil.generateInferenceId(remotePredict.getModelToken());

        buildPercentStart(inferenceId);
        ResourceManager.submitInference(inferenceId, remotePredict);
        return (Map<String, Object>) ResourceManager.CACHE.getValue(inferenceId);
    }

    /**
     * 推理开始，构造推理结果进度
     *
     * @param inferenceId 推理唯一id
     */
    public void buildPercentStart(String inferenceId) {
        Map<String, Object> map = new HashMap<>();
        map.put(INFERENCE_ID, inferenceId);
        map.put(PERCENT, 0);
        map.put(PREDICT_INFO, "开始推理");
        map.put(START_TIME, System.currentTimeMillis());
        ResourceManager.CACHE.putValue(inferenceId, map);
        //InferenceInfoCache cache = new InferenceInfoCache(startTime, 0, "开始推理");
    }
}
