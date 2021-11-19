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

package com.jdt.fedlearn.common.entity.project;


import com.jdt.fedlearn.common.entity.core.ClientInfo;

public class PartnerDTO {
    private String ip;
    private int port;
    private String protocol;
    private int token;
    private String dataset;

    public PartnerDTO() {

    }

    public PartnerDTO(String ip, int port, String protocol, int token, String dataset) {
        this.ip = ip;
        this.port = port;
        this.protocol = protocol;
        this.token = token;
        this.dataset = dataset;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
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

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public int getToken() {
        return token;
    }

    public void setToken(int token) {
        this.token = token;
    }

    public String getDataset() {
        return dataset;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    public ClientInfo toClientInfo() {
        return new ClientInfo(ip, port, protocol, "");
    }


}
