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
import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.common.entity.core.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RandomForestTrainReq implements Message {

    private static final Logger logger = LoggerFactory.getLogger(RandomForestTrainReq.class);
    private ClientInfo client;
    private final Map<Integer, String> tidToXsampleId = new HashMap<>();
    private Map<Integer, List<Integer>> tidToSampleID = new HashMap<>();
    private String body;
    private String[] bodyAll;
    private boolean skip = false;
    private String[] encryptY;
    private String publickey;
    private String[][] distributedEncryptY;
    private List<ClientInfo> clientInfos = new ArrayList<>();
    private List<String[]> allTreeIds = new ArrayList<>();
    private List<Map<Integer, double[]>> maskLefts;
    private List<String[]> splitMessages;
    private Map<ClientInfo, List<Integer>[]> clientFeatureMap = new HashMap<>();
    private int numTrees;
    private String[] treeIds;

    public RandomForestTrainReq() {};


    public RandomForestTrainReq(ClientInfo clientInfo) {
        this.client = clientInfo;
    }

    public RandomForestTrainReq(ClientInfo clientInfo, String body) {
        this.client = clientInfo;
        this.body = body;
    }

    public RandomForestTrainReq(ClientInfo clientInfo, String body, int numTrees) {
        this.client = clientInfo;
        this.body = body;
        this.numTrees = numTrees;
    }

    public RandomForestTrainReq(ClientInfo clientInfo, boolean skip) {
        this.client = clientInfo;
        this.skip = skip;
    }

    public RandomForestTrainReq(ClientInfo clientInfo, Map<Integer, List<Integer>> tidToSampleID) {
        this.client = clientInfo;
        this.tidToSampleID = tidToSampleID;
    }


    public RandomForestTrainReq(ClientInfo clientInfo, String body, Map<Integer, List<Integer>> tidToSampleID) {
        this.client = clientInfo;
        this.body = body;
        this.tidToSampleID = tidToSampleID;
    }

    public RandomForestTrainReq(ClientInfo clientInfo, String[] bodyAll, List<ClientInfo> clientInfos) {
        this.client = clientInfo;
        this.bodyAll = bodyAll;
        this.clientInfos = clientInfos;
    }

    public RandomForestTrainReq(ClientInfo clientInfo, Map<Integer, List<Integer>> tidToSampleID, Map<ClientInfo, List<Integer>[]> clientFeatureMap) {
        this.client = clientInfo;
        this.tidToSampleID = tidToSampleID;
        this.clientFeatureMap = clientFeatureMap;
    }

    public RandomForestTrainReq(ClientInfo clientInfo, Map<Integer, List<Integer>> tidToSampleID, String[] encryptY, String publicKey, Map<ClientInfo, List<Integer>[]> clientFeatureMap) {
        this.client = clientInfo;
        this.tidToSampleID = tidToSampleID;
        this.encryptY = encryptY;
        this.publickey = publicKey;
        this.clientFeatureMap = clientFeatureMap;
    }

    public RandomForestTrainReq(ClientInfo clientInfo, Map<Integer, List<Integer>> tidToSampleID, String[][] DistributedEncryptY, String publicKey, Map<ClientInfo, List<Integer>[]> clientFeatureMap) {
        this.client = clientInfo;
        this.tidToSampleID = tidToSampleID;
        this.distributedEncryptY = DistributedEncryptY;
        this.publickey = publicKey;
        this.clientFeatureMap = clientFeatureMap;
    }

    public RandomForestTrainReq(ClientInfo clientInfo, List<String[]> allTreeIds, List<ClientInfo> clientInfos, List<Map<Integer, double[]>> maskLefts, List<String[]> splitMessages) {
        this.client = clientInfo;
        this.allTreeIds = allTreeIds;
        this.clientInfos = clientInfos;
        this.maskLefts = maskLefts;
        this.splitMessages = splitMessages;
    }

//    public void tidToXsample() {
//        for (HashMap.Entry<Integer, List<Integer>> entry : this.tidToSampleID.entrySet()) {
//            Integer _tid = entry.getKey();
//            List<Integer> _sampleID = entry.getValue();
//            Integer datasetSize = 40000;
//            String XsampleID = DataUtils.sampleIdToString(_sampleID, datasetSize);
//            this.tidToXsampleId.put(_tid, XsampleID);
//        }
//    }
//
//    public void sampleToTid() {
//        for (HashMap.Entry<Integer, String> entry : this.tidToXsampleId.entrySet()) {
//            Integer _tid = entry.getKey();
//            List<Integer> sampleID = DataUtils.stringToSampleId(entry.getValue());
//            this.tidToSampleID.put(_tid, sampleID);
//        }
//    }

    public ClientInfo getClient() {
        return client;
    }

    public void setClient(ClientInfo client) {
        this.client = client;
    }

    public Map<Integer, String> getTidToXsampleId() {
        return tidToXsampleId;
    }

    public Map<Integer, List<Integer>> getTidToSampleID() {
        return tidToSampleID;
    }

    public void setTidToSampleID(Map<Integer, List<Integer>> tidToSampleID) {
        this.tidToSampleID = tidToSampleID;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String[] getBodyAll() {
        return bodyAll;
    }

    public void setBodyAll(String[] bodyAll) {
        this.bodyAll = bodyAll;
    }

    public boolean isSkip() {
        return skip;
    }

    public void setSkip(boolean skip) {
        this.skip = skip;
    }

    public String[] getEncryptY() {
        return encryptY;
    }

    public void setEncryptY(String[] encryptY) {
        this.encryptY = encryptY;
    }

    public String getPublickey() {
        return publickey;
    }

    public void setPublickey(String publickey) {
        this.publickey = publickey;
    }

    public String[][] getDistributedEncryptY() {
        return distributedEncryptY;
    }

    public void setDistributedEncryptY(String[][] distributedEncryptY) {
        this.distributedEncryptY = distributedEncryptY;
    }

    public List<ClientInfo> getClientInfos() {
        return clientInfos;
    }

    public void setClientInfos(List<ClientInfo> clientInfos) {
        this.clientInfos = clientInfos;
    }

    public List<String[]> getAllTreeIds() {
        return allTreeIds;
    }

    public void setAllTreeIds(List<String[]> allTreeIds) {
        this.allTreeIds = allTreeIds;
    }

    public List<Map<Integer, double[]>> getMaskLefts() {
        return maskLefts;
    }

    public void setMaskLefts(List<Map<Integer, double[]>> maskLefts) {
        this.maskLefts = maskLefts;
    }

    public List<String[]> getSplitMessages() {
        return splitMessages;
    }

    public void setSplitMessages(List<String[]> splitMessages) {
        this.splitMessages = splitMessages;
    }

    public Map<ClientInfo, List<Integer>[]> getClientFeatureMap() {
        return clientFeatureMap;
    }

    public void setClientFeatureMap(Map<ClientInfo, List<Integer>[]> clientFeatureMap) {
        this.clientFeatureMap = clientFeatureMap;
    }

    public int getNumTrees() {
        return numTrees;
    }

    public void setNumTrees(int numTrees) {
        this.numTrees = numTrees;
    }

    public String[] getTreeIds() {
        return treeIds;
    }

    public void setTreeIds(String[] treeIds) {
        this.treeIds = treeIds;
    }
}