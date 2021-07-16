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

package com.jdt.fedlearn.frontend.mapper.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jdt.fedlearn.common.util.TimeUtil;
import com.jdt.fedlearn.frontend.util.IdGenerateUtil;

import java.io.Serializable;

/**
 * @className: Account
 * @description: 用户实体类
 * @author: geyan29
 * @createTime: 2021/1/25 4:16 下午
 */
@TableName("account")
public class Account implements Serializable {
    @TableField(value = "id")
    private String userId;
    @TableField(value = "user_name")
    private String username;
    private String password;
    private String createTime;
    private String modifiedTime;
    private String status;
    private String roles;
    private String merCode;
    private String email;
    private String createUser;

    public Account() {
    }

    public Account(String username, String password ,String merCode,String roles) {
        this.userId = IdGenerateUtil.getUUID();
        this.username = username;
        this.password = password;
        this.status = "0";
        this.createTime = TimeUtil.getNowDateStr();
        this.modifiedTime = TimeUtil.getNowDateStr();
        this.merCode = merCode;
        this.roles = roles;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public String getMerCode() {
        return merCode;
    }

    public void setMerCode(String merCode) {
        this.merCode = merCode;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(String modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public String getCreateUser() {
        return createUser;
    }

    public void setCreateUser(String createUser) {
        this.createUser = createUser;
    }
}
