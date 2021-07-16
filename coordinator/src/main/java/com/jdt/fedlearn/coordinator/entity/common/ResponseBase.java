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

package com.jdt.fedlearn.coordinator.entity.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.core.exception.DeserializeException;

import java.io.IOException;

public class ResponseBase {
    private int code;
    private String status;
    private String data;

    public ResponseBase(int code, String status, String data) {
        this.code = code;
        this.status = status;
        this.data = data;
    }

    public ResponseBase() {
    }


    public ResponseBase(String jsonStr) {
        parseJson(jsonStr);
    }

    public int getCode() {
        return code;
    }

    public String getStatus() {
        return status;
    }

    public String getData() {
        return data;
    }

    public void parseJson(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        ResponseBase p3r;
        try {
            p3r = mapper.readValue(jsonStr, ResponseBase.class);
            this.code = p3r.code;
            this.status = p3r.status;
            this.data = p3r.data;
        } catch (IOException e) {
            throw new DeserializeException(e.getMessage());
        }
    }
}
