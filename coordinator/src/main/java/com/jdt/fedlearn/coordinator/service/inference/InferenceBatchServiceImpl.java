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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Maps;
import com.jdt.fedlearn.common.util.LogUtil;
import com.jdt.fedlearn.coordinator.entity.inference.InferenceRequest;
import com.jdt.fedlearn.coordinator.entity.inference.InferenceRes;
import com.jdt.fedlearn.coordinator.entity.inference.SingleInferenceRes;
import com.jdt.fedlearn.coordinator.service.AbstractDispatchService;
import com.jdt.fedlearn.coordinator.service.CommonService;
import com.jdt.fedlearn.coordinator.service.IDispatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * InferenceBatchService
 *
 * @author wangpeiqi
 */
public class InferenceBatchServiceImpl implements IDispatchService {
    private static final Logger logger = LoggerFactory.getLogger(InferenceBatchServiceImpl.class);
    private static final String PREDICT = "predict";
    private static final String UID = "uid";
    private static final String SCORE = "score";
    private static final String HEADER = "header";

    @Override
    public Map<String, Object> service(String content){
        Map<String, Object> modelMap = Maps.newHashMap();
        try {
            InferenceRequest query = new InferenceRequest(content);
            Map<String, Object> data = batchInference(query);
            return new AbstractDispatchService() {
                @Override
                public Map<String, Object> dealService() {
                    return data;
                }
            }.doProcess(true);
        } catch (Exception ex) {
            logger.error("InferenceBatchServiceImpl Exception :",ex);
            CommonService.exceptionProcess(ex, modelMap);
        }
        return modelMap;
    }

    /**
     * @param request 用户端发起的推理请求
     * @return 推理结果
     */
    public Map<String, Object> batchInference(InferenceRequest request) {
        // 调用推理
        InferenceRes predict = InferenceCommonService.commonInference(request.getModelToken(), request.getUid(), request.getClientList(), request.isSecureMode());

        //组装返回结果
        List<Map<String, Object>> res = new ArrayList<>();
        Map<String, Object> header = new HashMap<>();
        header.put(UID, HEADER);
        header.put(SCORE, predict.getScoreNameList());
        res.add(header);
        for (SingleInferenceRes inferenceRes : predict.getInferenceResList()) {
            Map<String, Object> row = new HashMap<>();
            row.put(UID, inferenceRes.getUid());
            row.put(SCORE, inferenceRes.getScore());
            res.add(row);
        }

        //将推理结果插入数据库
//        insertInferenceLog(request, inferenceId, startTime, "success");
        Map<String, Object> data = new HashMap<>();
        data.put(PREDICT, res);
        return data;
    }
}
