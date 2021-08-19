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

package com.jdt.fedlearn.coordinator.entity.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.core.exception.DeserializeException;
import com.jdt.fedlearn.core.exception.SerializeException;

import java.io.IOException;

/**
 * 请求模型删除的实体类，包含{@code modelToken}和用户名两方面信息
 * @see com.jdt.fedlearn.coordinator.service.system.ModelDeleteServiceImpl
 * @author lijingxi
 */
public class DeleteModelReq {
    private String modelToken;

    public DeleteModelReq() {
    }

    public DeleteModelReq(String jsonStr) {
        parseJson(jsonStr);
    }

    public String getModelToken() {
        return modelToken;
    }

    public void setModelToken(String modelToken) {
        this.modelToken = modelToken;
    }


    public void parseJson(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        DeleteModelReq p3r = null;
        try {
            p3r = mapper.readValue(jsonStr, DeleteModelReq.class);
            this.modelToken = p3r.modelToken;
        } catch (IOException e) {
            throw new DeserializeException(e.getMessage());
        }
    }

    public String toJson() {
        String jsonStr;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            jsonStr = objectMapper.writeValueAsString(this);
        } catch (Exception e) {
            throw new SerializeException("Boost Phase1 Request to json");
        }
        return jsonStr;
    }
}
