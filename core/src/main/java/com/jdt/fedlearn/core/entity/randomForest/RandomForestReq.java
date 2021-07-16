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
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.core.encryption.common.Ciphertext;
import com.jdt.fedlearn.core.encryption.common.PublicKey;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.common.CommonRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import RandomForest;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

public class RandomForestReq implements Message {

    private static final Logger logger = LoggerFactory.getLogger(RandomForestReq.class);
    private ClientInfo client;
    private HashMap<Integer, String> tidToXsampleId = new HashMap<>();
    @JsonIgnore
    private HashMap<Integer, ArrayList<Integer>> tidToSampleID = new HashMap<>();

    // 主被动标志
    //private boolean is_active;
    @JsonIgnore
    private ArrayList<Integer> sampleId;
    @JsonIgnore
    private String compressed_sampleID;

    public String getPublickey() {
        return publickey;
    }

    private int treeId;
    private String body;
    private boolean skip = false;
    private String[] encryptY;
    private String publickey;
    private int bestRound;

    public String[] getEncryptY() {
        return encryptY;
    }

    @JsonIgnore
    private String extraInfo;
    // action = {}

    public RandomForestReq(ClientInfo clientInfo, String body, int treeId, ArrayList<Integer> sampleId, String extraInfo) {
        // this is what current req is
        this.client = clientInfo;
        this.extraInfo = extraInfo;
        this.treeId = treeId;
        this.body = body;
        this.sampleId = sampleId;
        //this.is_active = is_active;
    }

    public RandomForestReq(ClientInfo clientInfo, String body, int treeId, ArrayList<Integer> sampleId, String extraInfo, HashMap<Integer, ArrayList<Integer>> tidToSampleID) {
        // this is intermediate req for debuging
        this.client = clientInfo;
        this.extraInfo = extraInfo;
        this.treeId = treeId;
        this.body = body;
        this.sampleId = sampleId;
        //this.is_active = is_active;
        this.tidToSampleID = tidToSampleID;
    }

    public RandomForestReq(ClientInfo clientInfo, String body, int treeId, ArrayList<Integer> sampleId, String extraInfo, HashMap<Integer, ArrayList<Integer>> tidToSampleID, String[] encryptY, String publicKey) {
        // this is intermediate req for debuging
        this.client = clientInfo;
        this.extraInfo = extraInfo;
        this.treeId = treeId;
        this.body = body;
        this.sampleId = sampleId;
        //this.is_active = is_active;
        this.tidToSampleID = tidToSampleID;
        this.encryptY = encryptY;
        this.publickey = publicKey;
    }

    public RandomForestReq(ClientInfo clientInfo, int bestRound) {
        this.client = clientInfo;
        this.bestRound = bestRound;
    }

    public void tidToXsample() {
        for (HashMap.Entry<Integer, ArrayList<Integer>> entry : this.tidToSampleID.entrySet()) {
            Integer _tid = entry.getKey();
            ArrayList<Integer> _sampleID = entry.getValue();
            Integer datasetSize = 40000;
            String XsampleID = DataUtils.sampleIdToString(_sampleID, datasetSize);
            this.tidToXsampleId.put(_tid, XsampleID);
//                this.tid.add(_tid);
//                this.XsampleID.add(XsampleID);
        }
    }

    public void sampleToTid() {
        for (HashMap.Entry<Integer, String> entry : this.tidToXsampleId.entrySet()) {
            Integer _tid = entry.getKey();
            ArrayList<Integer> sampleID = (ArrayList<Integer>) DataUtils.stringToSampleId(entry.getValue());
            this.tidToSampleID.put(_tid, sampleID);
        }
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

    public HashMap<Integer, ArrayList<Integer>> getTidToSampleID() {
        return tidToSampleID;
    }

    public HashMap<Integer, String> getTidToXsampleId() {
        return tidToXsampleId;
    }

    public int getBestRound() {
        return bestRound;
    }

}