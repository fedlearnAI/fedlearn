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

import com.github.benmanes.caffeine.cache.Cache;
import com.jdt.fedlearn.common.util.TimeUtil;
import com.jdt.fedlearn.frontend.constant.Constant;
import com.jdt.fedlearn.frontend.jdchain.response.ResponseHandler;
import com.jdt.fedlearn.frontend.mapper.entity.Account;
import com.jdt.fedlearn.frontend.service.AccountService;
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
import java.util.*;

/**
 * @className: AccountController
 * @description: 用户相关
 * @author: geyan29
 * @createTime: 2021/3/9 4:17 下午
 */
@Controller
@RequestMapping("api")
public class AccountController {

    @Resource
    AccountService accountService;
    @Resource
    Cache<String, Object> caffeineCache;

    /***
    * @description: 用户注册及添加接口
    * @param account
    * @return: org.springframework.http.ResponseEntity<org.springframework.ui.ModelMap>
    * @author: geyan29
    * @date: 2021/3/9 4:18 下午
    */
    @RequestMapping(value = "auth/register", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> register(@Validated @RequestBody Account account) {
        ModelMap res;
        String userName = account.getUsername();
        Account oldAccount = accountService.queryAccount(userName);
        if (oldAccount != null) {
            res = ResponseHandler.existResponse();
        } else {
            Account createUser = accountService.queryAccount(account.getCreateUser());
            account.setUserId(IdGenerateUtil.getUUID());
            account.setCreateTime(TimeUtil.getNowDateStr());
            account.setModifiedTime(TimeUtil.getNowDateStr());
            if(createUser != null){
                account.setMerCode(createUser.getMerCode());
            }
            if (accountService.createAccount(account)) {
                res = ResponseHandler.successResponse();
            } else {
                res = ResponseHandler.failResponse();
            }
        }
        return ResponseEntity.status(HttpStatus.OK).body(res);
    }

    private static final String ROLE = "role";
    private static final String USERNAME = "username";
    @RequestMapping(value = "auth/login", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> login(@Validated @RequestBody Account account) {
        ModelMap result = null;
        String userName = account.getUsername();
        Account oldAccount = accountService.queryAccount(userName);
        if (oldAccount == null || !account.getPassword().equalsIgnoreCase(oldAccount.getPassword())) {
            result = ResponseHandler.failResponse("用户名或密码错误！");
        } else if(oldAccount.getStatus().equals(Constant.STATUS_DISABLE)){
            result = ResponseHandler.failResponse("用户状态不可用！");
        } else {
            String token = UUID.randomUUID().toString();
            HashMap data = new HashMap(8);
            data.put(Constant.CACHE_KEY, token);
            data.put(ROLE,oldAccount.getRoles());
            data.put(USERNAME,oldAccount.getUsername());
            result = ResponseHandler.successResponse(data);
            caffeineCache.put(token, userName);
        }
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    /**
     * @className ForwardController
     * @description: 查询用户列表
     * @return: org.springframework.http.ResponseEntity<org.springframework.ui.ModelMap>
     * @author: geyan29
     * @date: 2020/12/22 11:01 上午
     **/
    private static final String USER_LIST = "userList";
    @RequestMapping(value = "user/list", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> queryUserList(@Validated @RequestBody Account account) {
        account = accountService.queryAccount(account.getUsername());
        if(account != null){
            List<Account> accounts = accountService.queryAllAccountByMerCode(account.getMerCode());
            Map map = new HashMap();
            map.put(USER_LIST, accounts);
            ModelMap res = ResponseHandler.successResponse(map);
            return ResponseEntity.status(HttpStatus.OK).body(res);
        }else{
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    /***
     * @description: 修改用户密码
     * @param account
     * @return: org.springframework.http.ResponseEntity<org.springframework.ui.ModelMap>
     * @author: geyan29
     * @date: 2021/3/5 9:50 上午
     */
    @RequestMapping(value = "auth/update", method = RequestMethod.POST, produces = "application/json; charset=utf-8")
    @ResponseBody
    public ResponseEntity<ModelMap> update(@Validated @RequestBody Account account) {
        ModelMap res;
        Account oldAccount = accountService.queryAccount(account.getUsername());
        if(oldAccount != null){
            oldAccount.setStatus(account.getStatus());
            oldAccount.setRoles(account.getRoles());
            oldAccount.setModifiedTime(TimeUtil.getNowDateStr());
            oldAccount.setEmail(account.getEmail());
            boolean flag = accountService.updateAccount(oldAccount);
            if(flag){
                res = ResponseHandler.successResponse();
            }else {
                res = ResponseHandler.failResponse();
            }
            return ResponseEntity.status(HttpStatus.OK).body(res);
        }else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }
}
