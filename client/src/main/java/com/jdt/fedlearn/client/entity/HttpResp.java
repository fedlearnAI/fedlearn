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

package com.jdt.fedlearn.client.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.common.exception.DeserializeException;

import java.io.IOException;

public class HttpResp {
    private int code;
    private HttpData data;
    private String message;
    private String status;

    public HttpResp() {
    }

    public HttpResp(String jsonStr) {
        parseJson(jsonStr);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public HttpData getData() {
        return data;
    }

    public void setData(HttpData data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void parseJson(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        HttpResp p1r;
        try {
            p1r = mapper.readValue(jsonStr, HttpResp.class);
            this.code = p1r.code;
            this.status = p1r.status;
            this.message = p1r.message;
            this.data = p1r.data;
        } catch (IOException e) {
            throw new DeserializeException("Http response parse json error : ", e);
        }

    }

}
