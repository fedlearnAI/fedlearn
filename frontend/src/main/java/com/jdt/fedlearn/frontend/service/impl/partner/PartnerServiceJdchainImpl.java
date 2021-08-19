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
package com.jdt.fedlearn.frontend.service.impl.partner;

import com.jdt.fedlearn.common.entity.project.PartnerDTO;
import com.jdt.fedlearn.frontend.jdchain.config.JdChainCondition;
import com.jdt.fedlearn.frontend.mapper.partner.PartnerJdchainMapper;
import com.jdt.fedlearn.frontend.service.IPartnerService;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Conditional(JdChainCondition.class)
@Service
public class PartnerServiceJdchainImpl implements IPartnerService {
    @Resource
    PartnerJdchainMapper partnerJdchainMapper;

    @Override
    public List<PartnerDTO> queryPartnerDTOList(String taskId) {
        List<PartnerDTO> clientInfos = partnerJdchainMapper.queryJdchainPartnerDTOList(taskId);
        return clientInfos;
    }

    @Override
    public PartnerDTO queryPartnerDTO(String taskId, String username) {
        PartnerDTO partnerDTO = partnerJdchainMapper.queryJdchainPartnerDTO(taskId, username);
        return partnerDTO;
    }


}
