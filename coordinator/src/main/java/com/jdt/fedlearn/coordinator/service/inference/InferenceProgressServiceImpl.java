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

import com.jdt.fedlearn.coordinator.entity.inference.QueryPredict;
import com.jdt.fedlearn.coordinator.service.AbstractDispatchService;
import com.jdt.fedlearn.coordinator.service.CommonService;
import com.jdt.fedlearn.coordinator.service.InferenceService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @Name: InferenceProgressService
 */
public class InferenceProgressServiceImpl implements InferenceService {

    private static final Logger logger = LoggerFactory.getLogger(InferenceProgressServiceImpl.class);

    @Override
    public Map<String, Object> service(String content) {
        try {
            // 处理流程
            final QueryPredict queryPredict = new QueryPredict(content);
            return new AbstractDispatchService() {
                @Override
                public Map dealService() {
                    return InferenceCommonServiceImpl.INFERENCE_SERVICE.predictQuery(queryPredict);
                }
            }.doProcess(true);
        } catch (Exception e) {
            logger.error("批量计算异常", e);
        }
        return CommonService.fail(StringUtils.EMPTY);
    }
}
