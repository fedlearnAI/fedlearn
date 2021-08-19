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

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.jdt.fedlearn.frontend.jdchain.config.JdChainFalseCondition;
import com.jdt.fedlearn.frontend.entity.table.AccountDO;
import com.jdt.fedlearn.frontend.mapper.account.AccountDbMapper;
import com.jdt.fedlearn.frontend.service.IAccountService;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Conditional(JdChainFalseCondition.class)
@Service
public class AccountServiceDbImpl implements IAccountService {
    @Resource
    AccountDbMapper accountDbMapper;
    @Override
    public boolean createAccount(AccountDO accountDO) {
        int insert = accountDbMapper.insert(accountDO);
        return insert>0;
    }

    @Override
    public AccountDO queryAccount(String userName) {
        AccountDO accountDO = accountDbMapper.selectOne(new QueryWrapper<AccountDO>().eq("user_name", userName));
        return accountDO;
    }

    @Override
    public List<AccountDO> queryAllAccount() {
        List<AccountDO> accountDOS = accountDbMapper.selectList(null);
        return accountDOS;
    }

    @Override
    public boolean updateAccount(AccountDO accountDO) {
        int i = accountDbMapper.update(accountDO,new UpdateWrapper<AccountDO>().eq("id", accountDO.getUserId()));
        return i>0;
    }

    @Override
    public List<AccountDO> queryAllAccountByMerCode(String merCode) {
        List<AccountDO> list = accountDbMapper.selectList(new QueryWrapper<AccountDO>().eq("mer_code",merCode).orderByDesc("modified_time"));
        return list;
    }

    @Override
    public void updateAccountByMerCode(String merCode, String status) {
        List<AccountDO> list = this.queryAllAccountByMerCode(merCode);
        list.stream().forEach(account -> {
            account.setStatus(status);
            this.updateAccount(account);
        });
    }
}
