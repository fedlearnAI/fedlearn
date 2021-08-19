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

import com.jdt.fedlearn.frontend.jdchain.config.JdChainCondition;
import com.jdt.fedlearn.frontend.entity.table.MerchantDO;
import com.jdt.fedlearn.frontend.mapper.merchant.MerchantJdchainMapper;
import com.jdt.fedlearn.frontend.service.IMerchantService;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
/**
 * @className: MerchantServiceJdchianImpl
 * @description: jdchain版本的实现
 * @author: geyan29
 * @createTime: 2021/3/8 11:16 上午
 */
@Conditional(JdChainCondition.class)
@Service
public class MerchantServiceJdchainImpl implements IMerchantService {
    @Resource
    MerchantJdchainMapper merchantJdchainMapper;
    /***
    * @description: 创建企业对象
    * @param merchant
    * @return: void
    * @author: geyan29
    * @date: 2021/3/8 10:01 上午
    */
    public void createMerchant(MerchantDO merchant) {
        merchantJdchainMapper.insertMerchant(merchant);
    }

    /***
    * @description: 查询企业列表
    * @param
    * @return: java.util.List<com.jdt.fedlearn.frontend.entity.table.Merchant>
    * @author: geyan29
    * @date: 2021/3/8 10:01 上午
    */
    public List<MerchantDO> queryAllMerchant() {
        List<MerchantDO> merchants = merchantJdchainMapper.queryAllMerchant();
        return merchants;
    }

    /***
    * @description: 通过id查询企业对象
    * @param id
    * @return: com.jdt.fedlearn.frontend.entity.table.Merchant
    * @author: geyan29
    * @date: 2021/3/8 10:01 上午
    */
    public MerchantDO queryMerchantByCode(String id){
        MerchantDO merchant = merchantJdchainMapper.queryMerchantById(id);
        return merchant;
    }

    /***
    * @description: 更新企业对象，因为链上没有update方法，只是以新版本替换旧版本所有还是create
    * @param merchant
    * @return: void
    * @author: geyan29
    * @date: 2021/3/8 10:02 上午
    */
    public void updateMerchant(MerchantDO merchant){
        merchantJdchainMapper.insertMerchant(merchant);
    }
}
