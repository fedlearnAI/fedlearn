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

import com.jdt.fedlearn.common.entity.project.FeatureDTO;
import com.jdt.fedlearn.common.entity.project.PartnerDTO;
import com.jdt.fedlearn.frontend.entity.table.FeatureDO;
import com.jdt.fedlearn.frontend.entity.table.PartnerDO;
import com.jdt.fedlearn.frontend.jdchain.config.JdChainCondition;
import com.jdt.fedlearn.frontend.mapper.feature.FeatureJdchainMapper;
import com.jdt.fedlearn.frontend.mapper.partner.PartnerJdchainMapper;
import com.jdt.fedlearn.frontend.service.IFeatureService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Conditional(JdChainCondition.class)
@Service
public class FeatureServiceJdchainImpl implements IFeatureService {
    @Resource
    FeatureJdchainMapper featureJdchainMapper;
    @Resource
    PartnerJdchainMapper partnerJdchainMapper;

    @Override
    public List<String> queryFeatureAnswer(String taskId) {
        List<FeatureDO> featureAnswers = featureJdchainMapper.queryFeatureAnswerByTaskId((taskId));
        return featureAnswers
                .stream()
                .filter(a -> StringUtils.isEmpty(a.getDepUser()) && StringUtils.isEmpty(a.getDepFeature()))
                .map(FeatureDO::getFeature)
                .collect(Collectors.toList());
    }

    @Override
    public List<FeatureDTO> queryFeatureDTOList(String taskId) {
        List<FeatureDTO> featuresList = new ArrayList<>();
        List<PartnerDTO> partnerInfos = partnerJdchainMapper.queryJdchainPartnerDTOList(taskId);
        for (PartnerDTO partnerInfo : partnerInfos) {
            FeatureDTO features = featureJdchainMapper.queryFeaturesByTaskId(taskId, partnerInfo);
            featuresList.add(features);
        }
        return featuresList;
    }

    @Override
    public FeatureDTO queryFeatureDTO(String taskId, PartnerDO partnerDO) {
        PartnerDTO partnerDTO = partnerJdchainMapper.queryJdchainPartnerDTO(taskId, partnerDO.getUsername());
        return featureJdchainMapper.queryFeaturesByTaskId(taskId, partnerDTO);
    }

    @Override
    public List<FeatureDO> queryFeaturesByTaskId(String taskId) {
        return null;
    }
}
