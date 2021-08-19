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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.common.entity.project.PartnerInfoNew;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.exception.SerializeException;

import java.io.IOException;
import java.util.List;


/**
 * 推理请求,参数包括，
 * 需要推理的uid 列表
 * 模型token
 */
public class InferenceRequest implements Message {
    private String[] uid;
    private String modelToken;
    private List<PartnerInfoNew> clientList;
    private boolean secureMode;

    public InferenceRequest() {
    }

    public InferenceRequest(String jsonStr) {
        parseJson(jsonStr);
    }

    public void parseJson(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        InferenceRequest p3r = null;
        try {
            p3r = mapper.readValue(jsonStr, InferenceRequest.class);
            this.uid = p3r.uid;
            this.modelToken = p3r.modelToken;
            this.clientList = p3r.clientList;
            this.secureMode =p3r.secureMode;
        } catch (IOException e) {
            throw new SerializeException("predict Phase1 Request to json");
        }
    }

    public String[] getUid() {
        return uid;
    }

    public void setUid(String[] uid) {
        this.uid = uid;
    }


    public String getModelToken() {
        return modelToken;
    }

    public void setModelToken(String modelToken) {
        this.modelToken = modelToken;
    }

    public List<PartnerInfoNew> getClientList() {
        return clientList;
    }

    public void setClientList(List<PartnerInfoNew> clientList) {
        this.clientList = clientList;
    }

    public boolean isSecureMode() {
        return secureMode;
    }

    public void setSecureMode(boolean secureMode) {
        this.secureMode = secureMode;
    }
}
