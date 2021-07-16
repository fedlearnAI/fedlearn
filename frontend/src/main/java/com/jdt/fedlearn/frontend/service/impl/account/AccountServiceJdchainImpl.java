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

package com.jdt.fedlearn.frontend.service.impl.account;

import com.jdt.fedlearn.frontend.jdchain.config.JdChainCondition;
import com.jdt.fedlearn.frontend.mapper.entity.Account;
import com.jdt.fedlearn.frontend.mapper.account.AccountJdchainMapper;
import com.jdt.fedlearn.frontend.service.AccountService;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Conditional(JdChainCondition.class)
@Service
public class AccountServiceJdchainImpl implements AccountService {
    @Resource
    AccountJdchainMapper accountJdchainMapper;
    @Override
    public boolean createAccount(Account account) {
        boolean flag = accountJdchainMapper.insertAccount(account);
        return flag;
    }

    @Override
    public Account queryAccount(String userName) {
        Account account = accountJdchainMapper.queryAccount(userName);
        return account;
    }

    @Override
    public List<Account> queryAllAccount() {
        List<Account> accounts = accountJdchainMapper.queryAllAccount();
        return accounts;
    }

    /***
    * @description: 更新区块链相当于插入一个新版本
    * @param account
    * @return: boolean
    * @author: geyan29
    * @date: 2021/3/9 5:16 下午
    */
    @Override
    public boolean updateAccount(Account account) {
        return this.createAccount(account);
    }

    @Override
    public List<Account> queryAllAccountByMerCode(String merCode) {
        List<Account> accounts = accountJdchainMapper.queryAllAccountByMerCode(merCode);
        return accounts;
    }

    @Override
    public void updateAccountByMerCode(String merCode ,String status) {
        List<Account> list = this.queryAllAccountByMerCode(merCode);
        if(list != null){
            list.stream().forEach(account -> {
                account.setStatus(status);
                updateAccount(account);
            });
        }
    }
}
