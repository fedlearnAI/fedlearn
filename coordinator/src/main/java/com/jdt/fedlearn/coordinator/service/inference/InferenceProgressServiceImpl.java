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

import com.jdt.fedlearn.coordinator.allocation.ResourceManager;
import com.jdt.fedlearn.coordinator.dao.db.InferenceLogMapper;
import com.jdt.fedlearn.coordinator.entity.inference.QueryPredict;
import com.jdt.fedlearn.coordinator.entity.table.InferenceEntity;
import com.jdt.fedlearn.coordinator.service.AbstractDispatchService;
import com.jdt.fedlearn.coordinator.service.CommonService;
import com.jdt.fedlearn.coordinator.service.IDispatchService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @Name: InferenceProgressService
 */
public class InferenceProgressServiceImpl implements IDispatchService {

    private static final Logger logger = LoggerFactory.getLogger(InferenceProgressServiceImpl.class);
    private static final String INFERENCE_ID = "inferenceId";
    private static final String START_TIME = "startTime";
    private static final String PERCENT = "percent";
    private static final String PREDICT_INFO = "predictInfo";
    private static final String END_TIME = "endTime";
    private static final String TEN = "10";
    private static final String INFERENCE_SUCCESS = "推理完成";
    @Override
    public Map<String, Object> service(String content) {
        try {
            // 处理流程
            final QueryPredict queryPredict = new QueryPredict(content);
            return new AbstractDispatchService() {
                @Override
                public Map dealService() {
                    return predictQuery(queryPredict);
                }
            }.doProcess(true);
        } catch (Exception e) {
            logger.error("批量计算异常", e);
        }
        return CommonService.fail(StringUtils.EMPTY);
    }

    /**
     * 推理进度查询，包括三种，
     * 正在推理的，推理完成的，推理失败的
     *
     * @param queryPredict 推理进度查询请求体
     * @return 推理进度查询结果
     */
    public Map<String, Object> predictQuery(QueryPredict queryPredict) {
        Map<String, Object> map = null;
        if (ResourceManager.CACHE.constainsKey(queryPredict.getInferenceId())) {
            map = (Map<String, Object>) ResourceManager.CACHE.getValue(queryPredict.getInferenceId());
        } else {
            InferenceEntity inferenceLog = InferenceLogMapper.getInferenceLog(queryPredict.getInferenceId());
            map = new HashMap<>();
            if (inferenceLog.getInferenceId() == null) {
                map.put(INFERENCE_ID, queryPredict.getInferenceId());
                map.put(PERCENT, "-");
                map.put(PREDICT_INFO, "推理失败");
            } else {
                map.put(INFERENCE_ID, queryPredict.getInferenceId());
                map.put(START_TIME, inferenceLog.getStartTime().getTime());
                map.put(END_TIME, inferenceLog.getEndTime().getTime());
                map.put(PERCENT, "100");
                map.put(PREDICT_INFO, INFERENCE_SUCCESS);
            }
        }
        return map;
    }
}
