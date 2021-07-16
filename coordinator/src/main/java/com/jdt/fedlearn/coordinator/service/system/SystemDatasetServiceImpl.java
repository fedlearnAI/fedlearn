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
import com.jdt.fedlearn.coordinator.dao.db.TaskMapper;
import com.jdt.fedlearn.coordinator.entity.common.Response;
import com.jdt.fedlearn.coordinator.entity.system.FeatureMapDTO;
import com.jdt.fedlearn.coordinator.entity.system.FeatureReq;
import com.jdt.fedlearn.coordinator.entity.table.TaskAnswer;
import com.jdt.fedlearn.coordinator.exception.UnauthorizedException;
import com.jdt.fedlearn.coordinator.service.SystemService;
import com.jdt.fedlearn.coordinator.constant.RequestConstant;
import com.jdt.fedlearn.coordinator.network.SendAndRecv;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 查询数据源的接口实现类
 */
public class SystemDatasetServiceImpl implements SystemService {
    private static final Logger logger = LoggerFactory.getLogger(SystemDatasetServiceImpl.class);

    @Override
    public Map<String, Object> service(String content) {
        Map<String, Object> modelMap = new HashMap<>();
        FeatureReq req = new FeatureReq(content);
        try {
            FeatureMapDTO featureMapDto = queryDataset(req);
            modelMap.put(ResponseConstant.DATA, featureMapDto);
            modelMap.put(ResponseConstant.STATUS, ResponseConstant.SUCCESS);
            modelMap.put(ResponseConstant.CODE, ResponseConstant.SUCCESS_CODE);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (UnauthorizedException e) {
            modelMap.put(ResponseConstant.DATA, e.getMessage());
            modelMap.put(ResponseConstant.STATUS, ResponseConstant.FAIL);
            modelMap.put(ResponseConstant.CODE, ResponseConstant.FAIL_CODE);
        }
        return modelMap;
    }

    /**
     * @param featureReq 请求
     * @return 查询结果
     */
    public FeatureMapDTO queryDataset(FeatureReq featureReq) throws JsonProcessingException, UnauthorizedException {
        if(StringUtils.isNotEmpty(featureReq.getTaskPwd())){
            String taskId = featureReq.getTaskId();
            TaskAnswer taskAnswer = TaskMapper.selectTaskById(Integer.parseInt(taskId));
            if(!featureReq.getTaskPwd().equals(taskAnswer.getTaskPwd())){
                throw new UnauthorizedException("任务密码错误,无法加入任务！");
            }
        }
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        final String url = featureReq.getClientUrl() + RequestConstant.QUERY_DATASET;
        Response result = SendAndRecv.sendPost(url, new HashMap());
        logger.info("queryDataset result:{}", result);
        final FeatureMapDTO response = mapper.readValue(result.getData(), FeatureMapDTO.class);
        //  TODO 需要简单判断
        return response;
    }
}
