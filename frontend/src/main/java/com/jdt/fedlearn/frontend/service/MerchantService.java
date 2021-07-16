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

import com.jdt.fedlearn.frontend.mapper.entity.Merchant;

import java.util.List;

public interface MerchantService {

     void createMerchant(Merchant merchant);
    /***
     * @description: 查询企业列表
     * @param
     * @return: java.util.List<com.jdt.fedlearn.frontend.mapper.entity.Merchant>
     * @author: geyan29
     * @date: 2021/3/8 10:01 上午
     */
     List<Merchant> queryAllMerchant();

    /***
     * @description: 通过id查询企业对象
     * @param code
     * @return: com.jdt.fedlearn.frontend.mapper.entity.Merchant
     * @author: geyan29
     * @date: 2021/3/8 10:01 上午
     */
     Merchant queryMerchantByCode(String code);

    /***
     * @description: 更新企业对象，因为链上没有update方法，只是以新版本替换旧版本所有还是create
     * @param merchant
     * @return: void
     * @author: geyan29
     * @date: 2021/3/8 10:02 上午
     */
     void updateMerchant(Merchant merchant);
}
