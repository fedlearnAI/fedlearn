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
package com.jdt.fedlearn.frontend.controller;

import com.jdt.fedlearn.common.util.TimeUtil;
import com.jdt.fedlearn.frontend.constant.Constant;
import com.jdt.fedlearn.frontend.constant.ResponseHandler;
import com.jdt.fedlearn.frontend.entity.table.AccountDO;
import com.jdt.fedlearn.frontend.entity.table.MerchantDO;
import com.jdt.fedlearn.frontend.entity.MerchantAdmin;
import com.jdt.fedlearn.frontend.service.IAccountService;
import com.jdt.fedlearn.frontend.service.IMerchantService;
import com.jdt.fedlearn.frontend.util.IdGenerateUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商户相关
 */
@Controller
@RequestMapping("api")
public class MerchantController {
    @Resource
    IMerchantService merchantService;
    @Resource
    IAccountService accountService;
    /***
     * @description: 创建商户
     * @param merchantAdmin
     * @return: org.springframework.http.ResponseEntity<org.springframework.ui.ModelMap>
     * @author: geyan29
     * @date: 2021/3/5 11:12 上午
     */
    @RequestMapping(value = "merchant/create", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> createMerchantAdmin(@Validated @RequestBody MerchantAdmin merchantAdmin) {
        MerchantDO merchant = merchantAdmin.getMerchant();
        MerchantDO oldMerchant = merchantService.queryMerchantByCode(merchant.getMerCode());
        if(oldMerchant != null){
            return ResponseEntity.status(HttpStatus.OK).body(ResponseHandler.existResponse("企业编码已存在！"));
        }
        AccountDO accountDO = merchantAdmin.getAccount();
        AccountDO oldAccountDO = accountService.queryAccount(accountDO.getUsername());
        if(oldAccountDO != null){
            return ResponseEntity.status(HttpStatus.OK).body(ResponseHandler.existResponse("管理员账号已存在！"));
        }
        merchant.setId(IdGenerateUtil.getUUID());
        merchant.setCreateTime(TimeUtil.getNowDateStr());
        merchant.setModifiedTime(TimeUtil.getNowDateStr());
        merchant.setStatus(merchantAdmin.getStatus());
        merchantService.createMerchant(merchantAdmin.getMerchant());

        accountDO.setUserId(IdGenerateUtil.getUUID());
        accountDO.setRoles(Constant.ROLE_ADMIN);
        accountDO.setCreateTime(TimeUtil.getNowDateStr());
        accountDO.setModifiedTime(TimeUtil.getNowDateStr());
        accountDO.setStatus(merchantAdmin.getStatus());
        accountDO.setMerCode(merchant.getMerCode());
        accountDO.setCreateUser(merchantAdmin.getCurrentUser());
        accountService.createAccount(accountDO);
        ModelMap res = ResponseHandler.successResponse();
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    /***
    * @description: 修改企业信息 只能修改企业名称和状态
    * @param merchant
    * @return: org.springframework.http.ResponseEntity<org.springframework.ui.ModelMap>
    * @author: geyan29
    * @date: 2021/3/9 3:27 下午
    */
    @RequestMapping(value = "merchant/update", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> updateMerchant(@Validated @RequestBody MerchantDO merchant) {
        String merCode = merchant.getMerCode();
        MerchantDO oldMerchant = merchantService.queryMerchantByCode(merCode);
        oldMerchant.setName(merchant.getName());
        oldMerchant.setStatus(merchant.getStatus());
        oldMerchant.setModifiedTime(TimeUtil.getNowDateStr());
        merchantService.updateMerchant(oldMerchant);
        /*同步修改企业下所有用户状态*/
        accountService.updateAccountByMerCode(merCode,merchant.getStatus());
        ModelMap res = ResponseHandler.successResponse();
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    private static final String MERCHANT_LIST = "merchantList";
    /**
     * 查询所有商户列表
     * @author geyan
     * @return org.springframework.http.ResponseEntity<org.springframework.ui.ModelMap>
     */
    @RequestMapping(value = "merchant/list", produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> queryMerchantList() {
        List<MerchantDO> merchants = merchantService.queryAllMerchant();
        Map map = new HashMap();
        map.put(MERCHANT_LIST, merchants);
        ModelMap res = ResponseHandler.successResponse(map);
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }
}
