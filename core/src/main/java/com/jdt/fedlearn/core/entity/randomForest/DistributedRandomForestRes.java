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

package com.jdt.fedlearn.core.entity.randomForest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;

import java.io.IOException;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DistributedRandomForestRes implements Message {

    private static final Logger logger = LoggerFactory.getLogger(RandomForestRes.class);
    private ClientInfo client;
    // 主被动标志
    private boolean isActive;
    private String body;
    private ArrayList<Integer> sampleId;
    private int treeId;
    private String extraInfo = "";

    public DistributedRandomForestRes() {
    }

    public DistributedRandomForestRes(ClientInfo client_info, String body, boolean isActive) {
        this.client = client_info;
        this.body = body;
        this.isActive = isActive;

    }

    public DistributedRandomForestRes(ClientInfo clientInfo,
                           String body,
                           boolean isActive,
                           ArrayList<Integer> sampleId,
                           int treeId) {
        this.client = clientInfo;
        this.body = body;
        this.isActive = isActive;
        this.sampleId = sampleId;
        this.treeId = treeId;
    }

    public DistributedRandomForestRes(ClientInfo clientInfo,
                           String body,
                           boolean isActive,
                           ArrayList<Integer> sampleId,
                           int treeId,
                           String extraInfo) {
        this.client = clientInfo;
        this.body = body;
        this.isActive = isActive;
        this.sampleId = sampleId;
        this.treeId = treeId;
        this.extraInfo = extraInfo;
    }



    public String toJson() {
        String jsonStr;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            jsonStr = objectMapper.writeValueAsString(this);
        } catch (Exception e) {
            logger.error("to json error", e);
            jsonStr = null;
        }
        return jsonStr;
    }


    public void parseJson(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        DistributedRandomForestRes tmp;
        try {
            tmp = mapper.readValue(jsonStr, DistributedRandomForestRes.class);
            this.client = tmp.client;
            this.body = tmp.body;
            this.isActive = tmp.isActive;
            this.sampleId = tmp.sampleId;
            this.treeId = tmp.treeId;
            this.extraInfo = tmp.extraInfo;
        } catch (IOException e) {
            logger.error("parse error", e);
        }
    }

    public void setBody(String body) {
        this.body = body;
    }

    public ClientInfo getClient() {
        return client;
    }

    public boolean getIsActive() {
        return isActive;
    }

    public String getBody() {
        return body;
    }

    public int getTreeId() {
        return treeId;
    }

    public ArrayList<Integer> getSampleId() {
        return sampleId;
    }

    public void setExtraInfo(String extraInfo) {
        this.extraInfo = extraInfo;
    }

    public String getExtraInfo() {
        return extraInfo;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }


}


