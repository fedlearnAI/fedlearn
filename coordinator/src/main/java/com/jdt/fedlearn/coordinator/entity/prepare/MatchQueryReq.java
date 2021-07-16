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

package com.jdt.fedlearn.coordinator.entity.prepare;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.core.exception.DeserializeException;

import java.io.IOException;

/**
 * Id对齐查询请求实体，包含username和matchToken两方面信息
 *
 * @author lijingxi
 */
public class MatchQueryReq {
    private String username;
    private String matchToken;

    public MatchQueryReq() {
    }

    public MatchQueryReq(String jsonStr) {
        parseJson(jsonStr);
    }

    public MatchQueryReq(String username, String matchToken) {
        this.username = username;
        this.matchToken = matchToken;
    }

    public String getUsername() {
        return username;
    }

    public String getMatchToken() {
        return matchToken;
    }

    public void parseJson(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        MatchQueryReq p3r;
        try {
            p3r = mapper.readValue(jsonStr, MatchQueryReq.class);
            this.username = p3r.username;
            this.matchToken = p3r.matchToken;
            if (username == null || matchToken == null) {
                throw new DeserializeException("缺少参数");
            }
        } catch (IOException e) {
            throw new DeserializeException(e.getMessage());
        }
    }
}
