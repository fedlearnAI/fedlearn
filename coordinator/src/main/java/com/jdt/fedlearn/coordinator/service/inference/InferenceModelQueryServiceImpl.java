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
import com.jdt.fedlearn.coordinator.dao.db.TrainMapper;
import com.jdt.fedlearn.coordinator.dao.jdchain.ChainTrainMapper;
import com.jdt.fedlearn.coordinator.entity.jdchain.JdchainTrainInfo;
import com.jdt.fedlearn.coordinator.entity.common.CommonQuery;
import com.jdt.fedlearn.coordinator.service.InferenceService;
import com.jdt.fedlearn.coordinator.util.ConfigUtil;
import com.jdt.fedlearn.coordinator.type.RunningType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Name: InferenceModelQueryService
 */
public class InferenceModelQueryServiceImpl implements InferenceService {

    public static final String MODELS = "models";

    @Override
    public Map<String, Object> service(String content) {
        CommonQuery commonQuery = new CommonQuery(content);
        return finishedModel(commonQuery);
    }

    public Map<String, Object> finishedModel(CommonQuery query) {
        //
        String username = query.getUsername();
        Map<String, Object> modelMap = new HashMap<>();
        HashMap<String, Object> data = new HashMap<>();
        List<String> modelNames;
        if(ConfigUtil.getJdChainAvailable()){
            List<JdchainTrainInfo> jdchainTrainInfos = ChainTrainMapper.queryAllTrainByOwner(username);
            modelNames = jdchainTrainInfos.parallelStream()
                    .filter(trainInfo -> RunningType.COMPLETE.toString().equals(trainInfo.getRunningType()==null?"":trainInfo.getRunningType().toString()))
                    .map(JdchainTrainInfo::getModelToken)
                    .collect(Collectors.toList());
        }else{
            modelNames = TrainMapper.getModelsByUser(username);
        }
        data.put(MODELS, modelNames);
        modelMap.put(ResponseConstant.STATUS, ResponseConstant.SUCCESS);
        modelMap.put(ResponseConstant.CODE, ResponseConstant.SUCCESS_CODE);
        modelMap.put(ResponseConstant.DATA, data);
        return modelMap;
    }
}
