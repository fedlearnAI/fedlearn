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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.common.util.TimeUtil;
import com.jdt.fedlearn.coordinator.dao.db.InferenceLogMapper;
import com.jdt.fedlearn.coordinator.entity.inference.InferenceDto;
import com.jdt.fedlearn.coordinator.entity.table.InferenceEntity;
import com.jdt.fedlearn.coordinator.entity.inference.InferenceInfoDto;
import com.jdt.fedlearn.coordinator.entity.inference.InferenceResp;
import com.jdt.fedlearn.coordinator.service.AbstractDispatchService;
import com.jdt.fedlearn.coordinator.service.InferenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 推理日志查询的实现类，实现分页查询预测信息
 * 包含{@code queryInferenceLog}方法实现分页从数据库中调取查询预测信息
 */
public class InferenceLogQueryServiceImpl implements InferenceService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public Map<String, Object> service(String content) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        InferenceDto query = mapper.readValue(content, InferenceDto.class);
        final InferenceResp inferenceResp = queryInferenceLog(query);
        return new AbstractDispatchService() {
            @Override
            public Object dealService() {
                return inferenceResp;
            }
        }.doProcess(true);
    }

    /**
     * 分页查询预测信息
     *
     * @param inferenceDto 分页查询请求
     * @return 查询结果
     */
    public InferenceResp queryInferenceLog(InferenceDto inferenceDto) {
        InferenceResp build = null;
        try {
            final List<InferenceEntity> inferenceList = InferenceLogMapper.getInferenceList(inferenceDto);
            List<InferenceInfoDto> inferenceInfoResult = new ArrayList<>();
            if (inferenceList.size() > 0) {
                for (InferenceEntity inferenceEntity : inferenceList) {
                    final InferenceInfoDto inferenceInfoDto = new InferenceInfoDto();
                    inferenceInfoDto.setInferenceId(inferenceEntity.getInferenceId());
                    inferenceInfoDto.setStartTime(TimeUtil.defaultFormat(inferenceEntity.getStartTime()));
                    inferenceInfoDto.setEndTime(TimeUtil.defaultFormat(inferenceEntity.getEndTime()));
                    inferenceInfoDto.setCaller(inferenceEntity.getCaller());
                    inferenceInfoDto.setInferenceResult(inferenceEntity.getInferenceResult());
                    inferenceInfoDto.setRequestNum(inferenceEntity.getRequestNum());
                    inferenceInfoDto.setResponseNum(inferenceEntity.getResponseNum());
                    inferenceInfoResult.add(inferenceInfoDto);
                }
            }
            final Integer inferenceCount = InferenceLogMapper.getInferenceCount(inferenceDto);
            build = new InferenceResp();
            build.setInferenceList(inferenceInfoResult);
            build.setInferenceCount(inferenceCount);
        } catch (Exception e) {
            logger.error("查询推理信息异常", e);
        }
        return build;
    }
}
