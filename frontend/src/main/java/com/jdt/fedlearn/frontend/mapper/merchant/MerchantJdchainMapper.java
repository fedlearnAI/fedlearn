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

package com.jdt.fedlearn.frontend.mapper.merchant;

import com.jd.blockchain.ledger.TransactionResponse;
import com.jd.blockchain.ledger.TypedKVEntry;
import com.jdt.fedlearn.common.util.JsonUtil;
import com.jdt.fedlearn.frontend.constant.Constant;
import com.jdt.fedlearn.frontend.jdchain.config.JdChainCondition;
import com.jdt.fedlearn.frontend.mapper.JdChainBaseMapper;
import com.jdt.fedlearn.frontend.entity.table.MerchantDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Conditional(JdChainCondition.class)
@Component
public class MerchantJdchainMapper{
    private Logger logger = LoggerFactory.getLogger(MerchantJdchainMapper.class);
    @Value("${jdchain.merchant_table_address}")
    private String merchantTableAddress;
    @Resource
    JdChainBaseMapper jdChainBaseMapper;

    /***
    * @description: 保存jdchain
    * @param merchant
    * @param merchant
    * @return: boolean
    * @author: geyan29
    * @date: 2021/3/8 10:03 上午
    */
    public boolean insertMerchant(MerchantDO merchant) {
        logger.info("jdchain实现！");
        String merchantStr = JsonUtil.object2json(merchant);
        TransactionResponse response = jdChainBaseMapper.saveKV(merchantTableAddress, merchant.getMerCode(), merchantStr);
        if(response != null){
            return response.isSuccess();
        }else{
            return false;
        }
    }

    /***
    * @description: 查询所有值 jdchain
    * @param
    * @return: java.util.List<com.jdt.fedlearn.frontend.entity.table.Merchant>
    * @author: geyan29
    * @date: 2021/3/8 10:03 上午
    */
    public List<MerchantDO> queryAllMerchant() {
        List<MerchantDO> reslt = new ArrayList<>();
        TypedKVEntry[] typedKVEntries = jdChainBaseMapper.queryAllKVByDataAccountAddr(merchantTableAddress);
        if(typedKVEntries != null){
            reslt = Arrays.stream(typedKVEntries)
                    .map(typedKVEntry -> JsonUtil.json2Object((String) typedKVEntry.getValue(), MerchantDO.class))
                    .filter(merchant -> Constant.STATUS_ENABLE.equals(merchant.getStatus())).collect(Collectors.toList());
        }
        return reslt;
    }

    /***
    * @description: 根据id查询最新版本的数据 jdchain
    * @param merCode
    * @return: com.jdt.fedlearn.frontend.entity.table.Merchant
    * @author: geyan29
    * @date: 2021/3/8 10:03 上午
    */
    public MerchantDO queryMerchantById(String merCode){
        String typedKVEntries = jdChainBaseMapper.queryLatestValueByKey(merchantTableAddress, merCode);
        MerchantDO merchant = JsonUtil.json2Object(typedKVEntries, MerchantDO.class);
        return merchant;
    }
}
