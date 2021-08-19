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

package com.jdt.fedlearn.frontend.service.impl.feature;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jdt.fedlearn.common.entity.project.FeatureDTO;
import com.jdt.fedlearn.common.entity.project.SingleFeatureDTO;
import com.jdt.fedlearn.frontend.entity.table.FeatureDO;
import com.jdt.fedlearn.frontend.entity.table.PartnerDO;
import com.jdt.fedlearn.frontend.jdchain.config.JdChainFalseCondition;
import com.jdt.fedlearn.frontend.mapper.feature.FeatureDbMapper;
import com.jdt.fedlearn.frontend.service.IFeatureService;
import com.jdt.fedlearn.frontend.service.IPartnerService;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Conditional(JdChainFalseCondition.class)
@Service
public class FeatureServiceDbImpl implements IFeatureService {

    @Resource
    FeatureDbMapper featureMapper;
    @Resource
    IPartnerService partnerService;

    @Override
    public List<String> queryFeatureAnswer(String taskId) {
        List<FeatureDO> features = this.queryFeaturesByTaskId(taskId);
        List<String> collect = features.parallelStream().map(FeatureDO::getFeature).collect(Collectors.toList());
        return collect;
    }

    @Override
    public List<FeatureDTO> queryFeatureDTOList(String taskId) {
        List<FeatureDTO> featuresList = new ArrayList<>();
        List<PartnerDO> partnerTables = partnerService.queryPartnersByTaskId(taskId);
        for (PartnerDO partner : partnerTables) {
            FeatureDTO featureDTO = queryFeatureDTO(taskId, partner);
            featuresList.add(featureDTO);
        }
        return featuresList;
    }


    @Override
    public FeatureDTO queryFeatureDTO(String taskId, PartnerDO partnerDO) {
        AtomicReference<String> uidName = new AtomicReference<>("");
        List<FeatureDO> features = featureMapper.selectList(new QueryWrapper<FeatureDO>().eq(COLUMN_TASK_ID, taskId).eq(COLUMN_USERNAME, partnerDO.getUsername()));
        List<SingleFeatureDTO> collect = features.parallelStream().map(f -> {
            if (Boolean.parseBoolean(f.getIsIndex())) {
                uidName.set(f.getFeature());
            }
            return new SingleFeatureDTO(f.getFeature(), f.getFeatureType());
        }).collect(Collectors.toList());
        return new FeatureDTO(collect, uidName.get());
    }

    @Override
    public void saveBatch(List<FeatureDO> list) {
        list.forEach(f -> featureMapper.insert(f));
    }

    @Override
    public List<FeatureDO> queryFeaturesByTaskId(String taskId) {
        QueryWrapper wrapper = new QueryWrapper<FeatureDO>();
        wrapper.eq(COLUMN_TASK_ID, taskId);
        List features = featureMapper.selectList(wrapper);
        return features;
    }
}
