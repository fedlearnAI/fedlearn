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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;

import java.io.IOException;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import RandomForest;

public class DistributedRandomForestReq implements Message {

    private static final Logger logger = LoggerFactory.getLogger(DistributedRandomForestReq.class);
    private ClientInfo client;
    // 主被动标志
    //private boolean is_active;
    private ArrayList<Integer> sampleId;
    private int treeId;
    private String body;
    private boolean skip = false;
    private String extraInfo;
    private int bestRound;
    // action = {}

    public DistributedRandomForestReq() {
    }

    public DistributedRandomForestReq(ClientInfo clientInfo, String body, int treeId, ArrayList<Integer> sampleId, String extraInfo) {
        this.client = clientInfo;
        this.extraInfo = extraInfo;
        this.treeId = treeId;
        this.body = body;
        this.sampleId = sampleId;
        //this.is_active = is_active;
    }

    public DistributedRandomForestReq(ClientInfo clientInfo, int bestRound) {
        this.client = clientInfo;
        this.bestRound = bestRound;
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
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        DistributedRandomForestReq tmp = null;
        try {
            tmp = mapper.readValue(jsonStr, DistributedRandomForestReq.class);
            this.client = tmp.client;
            this.extraInfo = tmp.extraInfo;
            this.body = tmp.body;
            //this.is_active = tmp.is_active;
            this.treeId = tmp.treeId;
            this.sampleId = tmp.sampleId;
            if (tmp.isSkip()) {
                this.skip = true;
            }
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

    public String getBody() {
        return body;
    }

    public void setSkip(boolean skip) {
        this.skip = skip;
    }

    public boolean isSkip() {
        return skip;
    }

    public ArrayList<Integer> getSampleId() {
        return sampleId;
    }

    public int getTreeId() {
        return treeId;
    }

    public String getExtraInfo() {
        return extraInfo;
    }

    public int getBestRound() {
        return bestRound;
    }

    public void setBestRound(int bestRound) {
        this.bestRound = bestRound;
    }
}