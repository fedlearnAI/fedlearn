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

import com.google.common.collect.Maps;
import com.jdt.fedlearn.coordinator.constant.Constant;
import com.jdt.fedlearn.coordinator.constant.RequestConstant;
import com.jdt.fedlearn.coordinator.entity.common.ResponseBase;
import com.jdt.fedlearn.coordinator.entity.system.DeleteModelReq;
import com.jdt.fedlearn.coordinator.entity.table.PartnerProperty;
import com.jdt.fedlearn.coordinator.service.AbstractDispatchService;
import com.jdt.fedlearn.coordinator.service.InferenceService;
import com.jdt.fedlearn.coordinator.network.SendAndRecv;
import com.jdt.fedlearn.coordinator.dao.db.PartnerMapper;
import com.jdt.fedlearn.coordinator.dao.db.TrainMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模型删除接口实现类，用于根据
 */
public class ModelDeleteServiceImpl implements InferenceService {

    public static final String MODEL_TOKEN = "modelToken";

    @Override
    public Map<String, Object> service(String content) throws Exception {
        DeleteModelReq deleteModelReq = new DeleteModelReq(content);
        return new AbstractDispatchService() {
            @Override
            public Map dealService() {
                return Maps.newHashMap();
            }
        }.doProcess(deleteModel(deleteModelReq));
    }

    /**
     * @param modelInfo 要删除的model信息
     * @return 删除结果
     */
    public boolean deleteModel(DeleteModelReq modelInfo) {
        String modelToken = modelInfo.getModelToken();
        String taskId = modelToken.split("-")[0];
        String username = modelInfo.getUsername();
        TrainMapper.deleteModel(modelToken);
        List<PartnerProperty> partnerProperties = PartnerMapper.selectPartnerList(taskId, username);
        Map<String, Object> request = new HashMap<>();
        request.put(MODEL_TOKEN, modelToken);
        //TODO clientInfo
        String response = SendAndRecv.send(partnerProperties.get(0).toClientInfo(), RequestConstant.DELETE_MODEL, Constant.HTTP_POST, request);
        ResponseBase responseBase = new ResponseBase(response);
        if (responseBase.getCode() == 0) {
            return true;
        }
        return false;
    }
}
