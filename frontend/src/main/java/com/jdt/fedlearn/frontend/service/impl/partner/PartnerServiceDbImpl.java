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

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jdt.fedlearn.common.entity.project.PartnerDTO;
import com.jdt.fedlearn.frontend.entity.table.PartnerDO;
import com.jdt.fedlearn.frontend.jdchain.config.JdChainFalseCondition;
import com.jdt.fedlearn.frontend.mapper.partner.PartnerDbMapper;
import com.jdt.fedlearn.frontend.service.IPartnerService;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author geyan
 * @since 2021-07-08
 */
@Conditional(JdChainFalseCondition.class)
@Service
public class PartnerServiceDbImpl implements IPartnerService {
    @Resource
    PartnerDbMapper partnerDbMapper;

    @Override
    public List<PartnerDO> queryPartnersByTaskId(String taskId) {
        List<PartnerDO> clientInfos = partnerDbMapper.selectList(new QueryWrapper<PartnerDO>().eq(COLUMN_TASK_ID, taskId));
        return clientInfos;
    }

    @Override
    public void save(PartnerDO clientInfo) {
        partnerDbMapper.insert(clientInfo);
    }

    @Override
    public List<PartnerDTO> queryPartnerDTOList(String taskId) {
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq(COLUMN_TASK_ID,taskId);
        List<PartnerDO> clientInfos = partnerDbMapper.selectList(queryWrapper);
        List<PartnerDTO> collect = clientInfos.parallelStream().map(c -> {
            PartnerDTO partnerDTO = PartnerDO.convert2PartnerDTO(c);
            return partnerDTO;
        }).collect(Collectors.toList());
        return collect;
    }

    @Override
    public PartnerDTO queryPartnerDTO(String taskId, String username) {
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq(COLUMN_TASK_ID,taskId);
        queryWrapper.eq(COLUMN_USERNAME,username);
        PartnerDO partner = partnerDbMapper.selectOne(queryWrapper);
        PartnerDTO partnerDTO = PartnerDO.convert2PartnerDTO(partner);
        return partnerDTO;
    }

}
