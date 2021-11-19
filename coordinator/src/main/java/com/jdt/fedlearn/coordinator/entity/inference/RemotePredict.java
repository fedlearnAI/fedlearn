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

package com.jdt.fedlearn.coordinator.entity.inference;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.common.entity.project.PartnerInfoNew;
import com.jdt.fedlearn.common.exception.DeserializeException;


import java.io.Serializable;
import java.util.List;

/**
 * 远端推理请求
 *
 * @Name: RemotePredict
 */

public class RemotePredict implements Serializable {
    /**
     * 文件地址路径
     */
    private String path;
    /**
     * 文件所在参与放地址
     */
    private String userAddress;
    private String modelToken;
    private List<PartnerInfoNew> clientList;
    private boolean secureMode;


    public RemotePredict() {
    }

    public RemotePredict(String path, String modelToken) {
        this.path = path;
        this.modelToken = modelToken;
    }

    public RemotePredict(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            RemotePredict query = mapper.readValue(jsonStr, this.getClass());
            path = query.path;
            userAddress = query.userAddress;
            modelToken = query.modelToken;
            clientList = query.clientList;
            this.secureMode = query.secureMode;
        } catch (RuntimeException | JsonProcessingException e) {
            throw new DeserializeException("remote predict");
        }
    }

    public String getModelToken() {
        return this.modelToken;
    }

    public String getPath() {
        return this.path;
    }

    public List<PartnerInfoNew> getClientList() {
        return clientList;
    }

    public void setClientList(List<PartnerInfoNew> clientList) {
        this.clientList = clientList;
    }

    public String getUserAddress() {
        return userAddress;
    }

    public void setUserAddress(String userAddress) {
        this.userAddress = userAddress;
    }

    public boolean isSecureMode() {
        return secureMode;
    }

    public void setSecureMode(boolean secureMode) {
        this.secureMode = secureMode;
    }
}
