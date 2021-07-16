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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.core.encryption.common.Ciphertext;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.jdt.fedlearn.core.entity.common.CommonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RandomForestRes implements Message {

    private static final Logger logger = LoggerFactory.getLogger(RandomForestRes.class);
    private ClientInfo client;
    // 主被动标志
    private boolean isActive;
    private String body;
    @JsonIgnore
    private ArrayList<Integer> sampleId;
    @JsonIgnore
    private int treeId;
    @JsonIgnore
    private String extraInfo = "";
    @JsonIgnore
    HashMap<Integer, ArrayList<Integer>> tidToSampleId = new HashMap<Integer, ArrayList<Integer>>();;
    HashMap<Integer, String> tidToXsampleId = new HashMap<Integer,String>();;
    private int numTree;

    public int getNumTree() {
        return numTree;
    }

    public RandomForestRes() {
    }

    public RandomForestRes(ClientInfo client_info, String body, boolean isActive, int numTree) {
        this.client = client_info;
        this.body = body;
        this.isActive = isActive;
        this.numTree = numTree;
    }

    public RandomForestRes(ClientInfo client_info, String body, boolean isActive) {
        this.client = client_info;
        this.body = body;
        this.isActive = isActive;

    }

    public RandomForestRes(ClientInfo clientInfo,
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

    public RandomForestRes(ClientInfo clientInfo,
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

    public RandomForestRes(ClientInfo clientInfo,
                           String body,
                           boolean isActive,
                           ArrayList<Integer> sampleId,
                           int treeId,
                           String extraInfo,
                           HashMap<Integer,ArrayList<Integer>> tidToSampleId) {
        this.client = clientInfo;
        this.body = body;
        this.isActive = isActive;
        this.sampleId = sampleId;
        this.treeId = treeId;
        this.extraInfo = extraInfo;
        this.tidToSampleId = tidToSampleId;
    }

    public RandomForestRes(ClientInfo clientInfo,
                           String body,
                           boolean isActive,
                           ArrayList<Integer> sampleId,
                           int treeId,
                           String extraInfo,
                           HashMap<Integer,ArrayList<Integer>> tidToSampleId,
                           int numTree) {
        this.client = clientInfo;
        this.body = body;
        this.isActive = isActive;
        this.sampleId = sampleId;
        this.treeId = treeId;
        this.extraInfo = extraInfo;
        this.tidToSampleId = tidToSampleId;
        this.numTree = numTree;
    }



    public String toJson() {
        return this.toJsonV1();
    }

    public String toJsonV1() {
        String jsonStr;
        for (HashMap.Entry<Integer, ArrayList<Integer>> entry : this.tidToSampleId.entrySet()) {
            Integer _tid = entry.getKey();
            ArrayList<Integer> _sampleID = entry.getValue();
            Integer datasetSize = 400000;
            String XsampleID = DataUtils.sampleIdToString( _sampleID, datasetSize);
            this.tidToXsampleId.put( _tid, XsampleID );
//                this.tid.add(_tid);
//                this.XsampleID.add(XsampleID);
        }
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            jsonStr = objectMapper.writeValueAsString(this);
        } catch (Exception e) {
            logger.error("to json error", e);
            jsonStr = null;
        }
        assert jsonStr != null;
        logger.info("RandomForestReq serialization size: " + jsonStr.length());
        return jsonStr;
    }


    public void parseJson(String jsonStr) {
        parseJsonV1(jsonStr);
    }


    public void parseJsonV1(String jsonStr) {
        ObjectMapper mapper = new ObjectMapper();
        RandomForestRes tmp = null;
        try {
            tmp = mapper.readValue(jsonStr, RandomForestRes.class);
            this.client = tmp.client;
            this.body = tmp.body;
            this.isActive = tmp.isActive;
            this.tidToXsampleId = tmp.tidToXsampleId;
            for (HashMap.Entry<Integer, String> entry : this.tidToXsampleId.entrySet()) {
                Integer _tid = entry.getKey();
                ArrayList<Integer> sampleID = (ArrayList<Integer>) DataUtils.stringToSampleId(entry.getValue());
                this.tidToSampleId.put(_tid, sampleID);
            }
            this.extraInfo = tmp.extraInfo;
            this.numTree = tmp.numTree;
        } catch (IOException e) {
            logger.error("parse error", e);
        }
    }

    public void setClient(ClientInfo client) { this.client = client; }

    public void setBody(String body) { this.body = body; }

    public ClientInfo getClient() { return client; }

    public boolean getIsActive() { return isActive; }

    public String getBody() { return body; }

    public int getTreeId() {
        return treeId;
    }

    public ArrayList<Integer> getSampleId() {
        return sampleId;
    }

    public String getExtraInfo() {
        return extraInfo;
    }


    public HashMap<Integer, ArrayList<Integer>> getTidToSampleId() {
        return tidToSampleId;
    }

    public HashMap<Integer, String> getTidToXsampleId() {
        return tidToXsampleId;
    }
}


