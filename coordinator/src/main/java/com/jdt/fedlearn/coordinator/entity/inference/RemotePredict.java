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
import com.jdt.fedlearn.core.exception.DeserializeException;


import java.io.Serializable;

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
     * 用户名
     */
    private String username;
    private String modelToken;

    public RemotePredict() {
    }

    public RemotePredict(String path, String username, String modelToken) {
        this.path = path;
        this.username = username;
        this.modelToken = modelToken;
    }

    public RemotePredict(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            RemotePredict query = mapper.readValue(jsonStr, this.getClass());
            path = query.path;
            username = query.username;
            modelToken = query.modelToken;
        } catch (RuntimeException | JsonProcessingException e) {
            throw new DeserializeException("remote predict");
        }
    }

    public String getModelToken() {
        return this.modelToken;
    }

    public String getUsername() {
        return this.username;
    }

    public Object getPath() {
        return this.path;
    }
}
