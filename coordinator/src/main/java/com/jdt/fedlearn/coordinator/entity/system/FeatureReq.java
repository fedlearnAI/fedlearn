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
import com.jdt.fedlearn.common.exception.DeserializeException;

import java.io.IOException;

/**
 * 查询数据源的请求实体，包含用户名，客户端url，任务id，任务密码四方面信息
 */
public class FeatureReq {
    private String url;

    public FeatureReq() {
    }

    public FeatureReq(String jsonStr) {
        parseJson(jsonStr);
    }

    public void parseJson(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        FeatureReq p3r = null;
        try {
            p3r = mapper.readValue(jsonStr, FeatureReq.class);
            this.url = p3r.url;
        } catch (IOException e) {
            throw new DeserializeException(e.getMessage());
        }
    }


    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }


}
