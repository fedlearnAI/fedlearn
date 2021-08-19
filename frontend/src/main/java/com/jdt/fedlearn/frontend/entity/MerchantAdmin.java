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
package com.jdt.fedlearn.frontend.entity;

import com.jdt.fedlearn.frontend.entity.table.AccountDO;
import com.jdt.fedlearn.frontend.entity.table.MerchantDO;

public class MerchantAdmin {
    private String currentUser;
    private String status;
    private MerchantDO merchant;
    private AccountDO account;

    public MerchantDO getMerchant() {
        return merchant;
    }

    public void setMerchant(MerchantDO merchant) {
        this.merchant = merchant;
    }

    public AccountDO getAccount() {
        return account;
    }

    public void setAccount(AccountDO account) {
        this.account = account;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(String currentUser) {
        this.currentUser = currentUser;
    }
}
