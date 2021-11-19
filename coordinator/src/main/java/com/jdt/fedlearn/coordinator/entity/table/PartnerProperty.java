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

package com.jdt.fedlearn.coordinator.entity.table;

import com.jdt.fedlearn.common.entity.core.ClientInfo;

import java.util.Map;
import java.util.Random;

/**
 * 客户端属性, 包括这个客户端属于哪个用户，哪个任务，地址，唯一标识码，该客户的该任务对应的客户端绑定的数据集名称等信息
 * @since 0.6.6
 */
@Deprecated
public class PartnerProperty {
    private String id;
    private int taskId;
    private String username;
    private String protocol;
    private String clientIp;
    private int port;
    private int token;
    private String dataset;
    private String status;
    private Random random = new Random();

    public PartnerProperty(int taskId, String username, Map<String, String> clientInfo, String dataset) {
        this.taskId = taskId;
        this.username = username;
        this.clientIp = clientInfo.get("ip");
        this.port = Integer.parseInt(clientInfo.get("port"));
        this.protocol = clientInfo.get("protocol");
        this.dataset = dataset;
        this.token = generateToken();
    }

    public PartnerProperty(int taskId, String username, String protocol, String clientIp, int port, int token, String dataset) {
        this.taskId = taskId;
        this.username = username;
        this.protocol = protocol;
        this.clientIp = clientIp;
        this.port = port;
        this.dataset = dataset;
        this.token = token;
    }


    public PartnerProperty(String username, String protocol, String clientIp, int port, int token, String dataset) {
        this.username = username;
        this.protocol = protocol;
        this.clientIp = clientIp;
        this.port = port;
        this.dataset = dataset;
        this.token = token;
    }

    public PartnerProperty(String protocol, String clientIp, int port, int token, String dataset) {
        this.protocol = protocol;
        this.clientIp = clientIp;
        this.port = port;
        this.token = token;
        this.dataset = dataset;
    }

    private int generateToken() {
        return random.nextInt();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
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

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getProtocol() {
        return protocol;
    }

    public int getToken() {
        return token;
    }

    public String getDataset() {
        return dataset;
    }

    public String getStatus() {
        return status;
    }

    public ClientInfo toClientInfo() {
        return new ClientInfo(clientIp, port, protocol, "");
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public void setToken(int token) {
        this.token = token;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
