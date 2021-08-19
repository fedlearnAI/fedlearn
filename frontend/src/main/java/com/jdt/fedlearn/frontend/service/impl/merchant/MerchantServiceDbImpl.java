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

package com.jdt.fedlearn.frontend.service.impl.merchant;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jdt.fedlearn.frontend.constant.Constant;
import com.jdt.fedlearn.frontend.jdchain.config.JdChainFalseCondition;
import com.jdt.fedlearn.frontend.entity.table.MerchantDO;
import com.jdt.fedlearn.frontend.mapper.merchant.MerchantDbMapper;
import com.jdt.fedlearn.frontend.service.IMerchantService;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @className: MerchantServiceDbImpl
 * @description: 非jdchain，数据库版本的的实现
 * @author: geyan29
 * @createTime: 2021/3/8 11:16 上午
 */
@Conditional(JdChainFalseCondition.class)
@Service
public class MerchantServiceDbImpl implements IMerchantService {
    @Resource
    MerchantDbMapper merchantDbMapper;
    @Override
    public void createMerchant(MerchantDO merchant) {
        merchantDbMapper.insert(merchant);
    }

    @Override
    public List<MerchantDO> queryAllMerchant() {
        List<MerchantDO> merchants = merchantDbMapper.selectList(new QueryWrapper<MerchantDO>().eq("status", Constant.STATUS_ENABLE).orderByDesc("modified_time"));
        return merchants;
    }

    @Override
    public MerchantDO queryMerchantByCode(String merCode) {
        MerchantDO merchant = merchantDbMapper.selectOne(new QueryWrapper<MerchantDO>().eq("mer_code",merCode));
        return merchant;
    }

    @Override
    public void updateMerchant(MerchantDO merchant) {
        merchantDbMapper.updateById(merchant);
    }
}
