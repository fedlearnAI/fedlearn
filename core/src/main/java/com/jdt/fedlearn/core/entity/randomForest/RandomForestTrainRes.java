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
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.RFDispatchPhaseType;
import com.jdt.fedlearn.core.type.data.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RandomForestTrainRes implements Message {

    private static final Logger logger = LoggerFactory.getLogger(RandomForestTrainRes.class);
    @JsonIgnore
    private Map<Integer, List<Integer>> tidToSampleId = new HashMap<>();
    private Map<Integer, String> tidToXsampleId = new HashMap<>();
    private ClientInfo client;
    private boolean isActive;
    private String body;
    private int numTrees;
    private List<Integer>[] featureIds;
    private boolean isInit;
    private String[] encryptionLabel;
    private String[][] disEncryptionLabel;
    private String publicKey;
    private Map<String, String> splitMessageMap;
    private Map<String, Double> featureImportance = new HashMap<>();
    private Map<MetricType, List<Pair<Integer, Double>>> trainMetric;
    private Map<MetricType, List<Pair<Integer, String>>> trainMetric2Dim;
    private RFDispatchPhaseType messageType;
    private Map<String, Map<Integer, List<Integer>>> tidToSampleIds;
    private Map<String, String> jsonForest;
    private Map<Integer, double[]> maskLeft;
    private String[] splitMess;
    private String[] treeIds;
    private SubModel subModel;

    public RandomForestTrainRes() {
    }
    public RandomForestTrainRes(ClientInfo clientInfo) {
        this.client = clientInfo;
    }

    public RandomForestTrainRes(String body) {
        this.body = body;
    }

    public RandomForestTrainRes(ClientInfo clientInfo, boolean isInit, String body) {
        this.client = clientInfo;
        this.body = body;
        this.isInit = isInit;
    }

    public RandomForestTrainRes(ClientInfo client_info, String body, boolean isActive) {
        this.client = client_info;
        this.body = body;
        this.isActive = isActive;

    }

    public RandomForestTrainRes(ClientInfo client_info, String body, boolean isActive, int numTree) {
        this.client = client_info;
        this.body = body;
        this.isActive = isActive;
        this.numTrees = numTree;
    }

    public RandomForestTrainRes(ClientInfo clientInfo, String body, boolean isActive, Map<String, String> splitMessage) {
        this.client = clientInfo;
        this.body = body;
        this.isActive = isActive;
        this.splitMessageMap = splitMessage;
    }
    
    public RandomForestTrainRes(ClientInfo clientInfo,
                                boolean isInit,
                                String body,
                                String[] encryptionLabel,
                                String publicKey,
                                Map<Integer, List<Integer>> tidToSampleId) {
        this.client = clientInfo;
        this.body = body;
        this.isInit = isInit;
        this.encryptionLabel = encryptionLabel;
        this.publicKey = publicKey;
        this.tidToSampleId = tidToSampleId;
    }

    public RandomForestTrainRes(ClientInfo clientInfo,
                                boolean isInit,
                                String body,
                                String[][] disEncryptionLabel,
                                String publicKey,
                                Map<Integer, List<Integer>> tidToSampleId) {
        this.client = clientInfo;
        this.body = body;
        this.isInit = isInit;
        this.disEncryptionLabel = disEncryptionLabel;
        this.publicKey = publicKey;
        this.tidToSampleId = tidToSampleId;
    }

    // for phase1
    public RandomForestTrainRes(ClientInfo clientInfo,
                                boolean isInit,
                                String body,
                                Map<MetricType, List<Pair<Integer, Double>>> trainMetric,
                                Map<MetricType, List<Pair<Integer, String>>> trainMetric2Dim,
                                Map<String, Double> featureImportance,
                                Map<Integer, List<Integer>> tidToSampleId) {
        this.client = clientInfo;
        this.body = body;
        this.isInit = isInit;
        this.trainMetric = trainMetric;
        this.trainMetric2Dim = trainMetric2Dim;
        this.featureImportance = featureImportance;
        this.tidToSampleId = tidToSampleId;

    }



    public Map<Integer, List<Integer>> getTidToSampleId() {
        return tidToSampleId;
    }

    public void setTidToSampleId(Map<Integer, List<Integer>> tidToSampleId) {
        this.tidToSampleId = tidToSampleId;
    }

    public Map<Integer, String> getTidToXsampleId() {
        return tidToXsampleId;
    }

    public void setTidToXsampleId(Map<Integer, String> tidToXsampleId) {
        this.tidToXsampleId = tidToXsampleId;
    }

    public ClientInfo getClient() {
        return client;
    }

    public void setClient(ClientInfo client) {
        this.client = client;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public int getNumTrees() {
        return numTrees;
    }

    public void setNumTrees(int numTrees) {
        this.numTrees = numTrees;
    }

    public List<Integer>[] getFeatureIds() {
        return featureIds;
    }

    public void setFeatureIds(List<Integer>[] featureIds) {
        this.featureIds = featureIds;
    }

    public boolean isInit() {
        return isInit;
    }

    public void setInit(boolean init) {
        isInit = init;
    }

    public String[] getEncryptionLabel() {
        return encryptionLabel;
    }

    public void setEncryptionLabel(String[] encryptionLabel) {
        this.encryptionLabel = encryptionLabel;
    }

    public String[][] getDisEncryptionLabel() {
        return disEncryptionLabel;
    }

    public void setDisEncryptionLabel(String[][] disEncryptionLabel) {
        this.disEncryptionLabel = disEncryptionLabel;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public Map<String, String> getSplitMessageMap() {
        return splitMessageMap;
    }

    public void setSplitMessageMap(Map<String, String> splitMessageMap) {
        this.splitMessageMap = splitMessageMap;
    }

    public Map<String, Double> getFeatureImportance() {
        return featureImportance;
    }

    public void setFeatureImportance(Map<String, Double> featureImportance) {
        this.featureImportance = featureImportance;
    }

    public Map<MetricType, List<Pair<Integer, Double>>> getTrainMetric() {
        return trainMetric;
    }

    public void setTrainMetric(Map<MetricType, List<Pair<Integer, Double>>> trainMetric) {
        this.trainMetric = trainMetric;
    }

    public Map<MetricType, List<Pair<Integer, String>>> getTrainMetric2Dim() {
        return trainMetric2Dim;
    }

    public void setTrainMetric2Dim(Map<MetricType, List<Pair<Integer, String>>> trainMetric2Dim) {
        this.trainMetric2Dim = trainMetric2Dim;
    }

    public RFDispatchPhaseType getMessageType() {
        return messageType;
    }

    public void setMessageType(RFDispatchPhaseType messageType) {
        this.messageType = messageType;
    }

    public Map<String, Map<Integer, List<Integer>>> getTidToSampleIds() {
        return tidToSampleIds;
    }

    public void setTidToSampleIds(Map<String, Map<Integer, List<Integer>>> tidToSampleIds) {
        this.tidToSampleIds = tidToSampleIds;
    }

    public Map<String, String> getJsonForest() {
        return jsonForest;
    }

    public void setJsonForest(Map<String, String> jsonForest) {
        this.jsonForest = jsonForest;
    }

    public Map<Integer, double[]> getMaskLeft() {
        return maskLeft;
    }

    public void setMaskLeft(Map<Integer, double[]> maskLeft) {
        this.maskLeft = maskLeft;
    }

    public String[] getSplitMess() {
        return splitMess;
    }

    public void setSplitMess(String[] splitMess) {
        this.splitMess = splitMess;
    }

    public String[] getTreeIds() {
        return treeIds;
    }

    public void setTreeIds(String[] treeIds) {
        this.treeIds = treeIds;
    }

    public SubModel getSubModel() {
        return subModel;
    }

    public void setSubModel(SubModel subModel) {
        this.subModel = subModel;
    }
}


