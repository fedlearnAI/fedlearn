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
package com.jdt.fedlearn.frontend.service;

import com.jdt.fedlearn.frontend.mapper.entity.Account;

import java.util.List;

public interface AccountService {
    /**
     * @param account
     * @description: 插入用户账户
     * @return: boolean
     * @author: geyan29
     * @date: 2021/1/25 4:27 下午
     */
    boolean createAccount(Account account);

    /**
     * @param userName
     * @description: 通过用户名查询用户
     * @return: com.jdt.fedlearn.frontend.mapper.entity.Account
     * @author: geyan29
     * @date: 2021/1/25 4:34 下午
     */
    Account queryAccount(String userName);

    /**
     * @param
     * @description: 查询链上的所有用户信息
     * @return: java.util.List<com.jdt.fedlearn.frontend.mapper.entity.Account>
     * @author: geyan29
     * @date: 2021/1/25 6:47 下午
     */
    List<Account> queryAllAccount();

    /***
    * @description: 更新用户
    * @param account
    * @return: boolean
    * @author: geyan29
    * @date: 2021/3/9 5:15 下午
    */
    boolean updateAccount(Account account);

    List<Account> queryAllAccountByMerCode(String merCode);

    void updateAccountByMerCode(String merCode,String status);
}
