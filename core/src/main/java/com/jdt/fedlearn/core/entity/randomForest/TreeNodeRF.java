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

import java.io.Serializable;
import java.util.ArrayList;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jdt.fedlearn.core.entity.ClientInfo;

import java.util.List;

/**
 * 联邦随机森林的树节点类 TreeNodeRF
 */
public class TreeNodeRF implements Serializable {
    // 基础信息：节点id，树id，左子树，右子树
    public int nodeId;
    public int treeId;
    public TreeNodeRF left;
    public TreeNodeRF right;

    // 节点信息，是否为叶节点，以及节点存储的json信息（分裂信息或预测信息）
    public boolean isLeaf;
//    public JsonObject referenceJson;
    public String referenceJsonStr;

    // 分裂阈值
    public Double thres;

    // 叶节点预测值
    public Double prediction;

    public Integer featureId;
    public List<Integer> sampleIds;
    public Double score;
    public Double percentile;
    public int numSamples;
    public List<ClientInfo> Y1ClientMapping;
    public String party;



    public TreeNodeRF(List<Integer> sampleIds, int nodeId, int treeId) {
        this.nodeId = nodeId;
        this.treeId = treeId;
        this.left = null;
        this.right = null;
        this.isLeaf = false;
        this.party = null;
        this.featureId = null;
        this.sampleIds = sampleIds;
        this.score = null;
        this.percentile = null;
        this.thres = null;
        this.prediction = null;
        this.numSamples = sampleIds.size();
        this.referenceJsonStr = null;
    }

    public TreeNodeRF() {
    }


    public long level() {
        long i = 0;
        while (1L << i <= nodeId + 1L) {
            i++;
        }
        return i - 1L;
    }

    private long numDescendants() {
        int n = 0;
        if (left != null) {
            n += left.numDescendants() + 1;
        }
        if (right != null) {
            n += right.numDescendants() + 1;
        }
        return n;
    }

    public long numTreeNodes() {
        return 1 + numDescendants();
    }

    public void makeLeaf(String client) {
        // make a leaf
        isLeaf = true;
        JsonObject referenceJson = new JsonObject();
        if (referenceJsonStr != null) {
            referenceJson = JsonParser.parseString(referenceJsonStr).getAsJsonObject();
        }
        referenceJson.addProperty("is_leaf", 1);
        referenceJson.addProperty("prediction", prediction);
        party = client;
        referenceJsonStr =referenceJson.toString();

    }

    public int getNumSamples() {
        return numSamples;
    }

    public Double getScore() {
        return score;
    }
}


