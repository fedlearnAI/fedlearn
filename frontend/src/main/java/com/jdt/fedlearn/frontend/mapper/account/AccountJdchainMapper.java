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

package com.jdt.fedlearn.frontend.mapper.account;

import com.jd.blockchain.ledger.TransactionResponse;
import com.jd.blockchain.ledger.TypedKVEntry;
import com.jdt.fedlearn.common.util.JsonUtil;
import com.jdt.fedlearn.common.util.TimeUtil;
import com.jdt.fedlearn.frontend.constant.Constant;
import com.jdt.fedlearn.frontend.jdchain.config.JdChainCondition;
import com.jdt.fedlearn.frontend.mapper.JdChainBaseMapper;
import com.jdt.fedlearn.frontend.mapper.entity.Account;
import com.jdt.fedlearn.frontend.util.IdGenerateUtil;
import com.jdt.fedlearn.frontend.util.MD5EncryptUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @className: AccountMapper
 * @description:
 * @author: geyan29
 * @createTime: 2021/1/25 4:01 下午
 */
@Conditional(JdChainCondition.class)
@Component
public class AccountJdchainMapper implements InitializingBean {

    private String userTableAddress;

    @Value("${jdchain.user_table_address}")
    public void setUserTableAddress(String userTableAddress) {
        this.userTableAddress = userTableAddress;
    }

    @Value("${jdchain.default_user}")
    private String username;
    @Value("${jdchain.default_password}")
    private String password;

    @Resource
    JdChainBaseMapper jdChainBaseMapper;
    /**
     * @param
     * @description: 用于初始化用户
     * @return: void
     * @author: geyan29
     * @date: 2021/1/25 4:35 下午
     */
    public void afterPropertiesSet() throws Exception {
        Account account = queryAccount(username);
        if (account == null) {
            account = new Account();
            account.setUserId(IdGenerateUtil.getUUID());
            account.setUsername(username);
            account.setPassword(MD5EncryptUtil.MD5(password));
            account.setCreateTime(TimeUtil.getNowDateStr());
            account.setModifiedTime(TimeUtil.getNowDateStr());
            account.setStatus(Constant.STATUS_ENABLE);
            account.setRoles(Constant.ROLE_SUPER_ADMIN);
            account.setMerCode(Constant.ROLE_SUPER_ADMIN);
            insertAccount(account);
        }
    }

    /**
     * @param account
     * @description: 插入用户账户
     * @return: boolean
     * @author: geyan29
     * @date: 2021/1/25 4:27 下午
     */
    public boolean insertAccount(Account account) {
        String accountStr = JsonUtil.object2json(account);
        TransactionResponse response = jdChainBaseMapper.saveKV(userTableAddress, account.getUsername(), accountStr);
        if(response != null){
            return response.isSuccess();
        }else {
            return false;
        }
    }

    /**
     * @param userName
     * @description: 通过用户名查询用户
     * @return: com.jdt.fedlearn.frontend.mapper.entity.Account
     * @author: geyan29
     * @date: 2021/1/25 4:34 下午
     */
    public Account queryAccount(String userName) {
        String result = jdChainBaseMapper.queryLatestValueByKey(userTableAddress, userName);
        if (!StringUtils.isBlank(result)) {
            Account account = JsonUtil.json2Object(result, Account.class);
            return account;
        }
        return null;
    }

    /**
     * @param
     * @description: 查询链上的所有用户信息
     * @return: java.util.List<com.jdt.fedlearn.frontend.mapper.entity.Account>
     * @author: geyan29
     * @date: 2021/1/25 6:47 下午
     */
    public List<Account> queryAllAccount() {
        TypedKVEntry[] typedKVEntries = jdChainBaseMapper.queryAllKVByDataAccountAddr(userTableAddress);
        if(typedKVEntries !=  null){
            List<Account> list = Arrays.stream(typedKVEntries).map(typedKVEntry -> JsonUtil.json2Object((String) typedKVEntry.getValue(), Account.class)).collect(Collectors.toList());
            return list;
        }else {
            return null;
        }
    }

    /***
    * @description: 通过merCode查询用户列表
    * @param merCode
    * @return: java.util.List<com.jdt.fedlearn.frontend.mapper.entity.Account>
    * @author: geyan29
    * @date: 2021/3/11 2:09 下午
    */
    public List<Account> queryAllAccountByMerCode(String merCode) {
        TypedKVEntry[] typedKVEntries = jdChainBaseMapper.queryAllKVByDataAccountAddr(userTableAddress);
        if(typedKVEntries !=  null){
            List<Account> list = Arrays.stream(typedKVEntries).map(typedKVEntry -> JsonUtil.json2Object((String) typedKVEntry.getValue(), Account.class))
                    .filter(account -> (account.getMerCode()!= null && merCode.equals(account.getMerCode())))
                    .sorted(Comparator.comparing(Account::getModifiedTime, Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());
            return list;
        }else {
            return null;
        }
    }
}
