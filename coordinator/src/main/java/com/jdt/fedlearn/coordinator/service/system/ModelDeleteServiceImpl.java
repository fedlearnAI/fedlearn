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
import com.jdt.fedlearn.coordinator.entity.system.DeleteModelReq;
import com.jdt.fedlearn.coordinator.service.AbstractDispatchService;
import com.jdt.fedlearn.coordinator.service.InferenceService;
import com.jdt.fedlearn.coordinator.dao.db.TrainMapper;

import java.util.Map;

/**
 * 模型删除接口实现类，用于根据
 */
public class ModelDeleteServiceImpl implements InferenceService {

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
        return TrainMapper.deleteModel(modelToken);
    }
}
