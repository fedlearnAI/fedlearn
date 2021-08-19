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
package com.jdt.fedlearn.frontend.entity.table;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jdt.fedlearn.common.entity.project.PartnerDTO;

import java.time.LocalDateTime;
import java.io.Serializable;

/**
 * <p>
 * 
 * </p>
 *
 * @author geyan
 * @since 2021-07-08
 */
@TableName("partner_table")
public class PartnerDO implements Serializable {

    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer taskId;

    private String username;

    private String clientIp;

    private Integer clientPort;

    private String protocol;

    private LocalDateTime createdTime;

    private LocalDateTime modifiedTime;

    private Integer status;

    private String dataset;

    private Integer token;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getTaskId() {
        return taskId;
    }

    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public Integer getClientPort() {
        return clientPort;
    }

    public void setClientPort(Integer clientPort) {
        this.clientPort = clientPort;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public LocalDateTime getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(LocalDateTime modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getDataset() {
        return dataset;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    public Integer getToken() {
        return token;
    }

    public void setToken(Integer token) {
        this.token = token;
    }

    @Override
    public String toString() {
        return "ClientInfo{" +
        "id=" + id +
        ", taskId=" + taskId +
        ", username=" + username +
        ", clientIp=" + clientIp +
        ", clientPort=" + clientPort +
        ", protocol=" + protocol +
        ", createdTime=" + createdTime +
        ", modifiedTime=" + modifiedTime +
        ", status=" + status +
        ", dataset=" + dataset +
        ", token=" + token +
        "}";
    }

    public static PartnerDTO convert2PartnerDTO(PartnerDO c) {
        PartnerDTO partnerDTO = new PartnerDTO(c.getClientIp(),c.getClientPort(),c.getProtocol(),c.getToken(),c.getDataset());
        return partnerDTO;
    }
}
