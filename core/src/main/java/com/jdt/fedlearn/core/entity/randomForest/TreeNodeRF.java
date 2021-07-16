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

import java.util.ArrayList;

import com.google.gson.JsonObject;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.grpc.federatedlearning.Matrix;
import com.jdt.fedlearn.grpc.federatedlearning.PaillierVector;
import com.jdt.fedlearn.grpc.federatedlearning.Vector;
import org.ejml.simple.SimpleMatrix;

import java.util.List;

/**
 * 联邦随机森林的树节点类 TreeNodeRF
 */
public class TreeNodeRF {
    // 基础信息：节点id，树id，左子树，右子树
    public long nodeId;
    public long treeId;
    public TreeNodeRF left;
    public TreeNodeRF right;

    // 训练时需要的信息，特征矩阵 Xs 和 label y（raw data以及同态加密后的结果）
//    public Matrix[] Xs;
//    public PaillierVector y_pvec;

    // 节点信息，是否为叶节点，以及节点存储的json信息（分裂信息或预测信息）
    public boolean isLeaf;
    public JsonObject referenceJson;

    // 分裂阈值
    public Double thres;

    // 叶节点预测值
    public Double prediction;

    // 以下为尚未梳理的变量
    public Integer featureId;
    public ArrayList<Integer> sampleIds;
    public Double score;
    public Double percentile;
    public int numSamples;
    public List<ClientInfo> Y1ClientMapping;
    public ClientInfo party;

    public int getNumSamples() {
        return numSamples;
    }

    public Double getScore() {
        return score;
    }

    public TreeNodeRF(ArrayList<Integer> sampleIds, long nodeId, long treeId) {
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
        this.referenceJson = new JsonObject();
        this.numSamples = sampleIds.size();
    }

    public TreeNodeRF() {
    }

    @Override
    public String toString() {
        return "TreeNode{" +
                " treeId=" + treeId +
                ", nodeId=" + nodeId +
                ", level=" + level() +
                ", isLeaf=" + isLeaf +
                ", party=" + party +
                ", featureId=" + featureId +
                ", thres=" + thres +
                ", percentile=" + percentile +
                ", prediction=" + prediction +
                ", score=" + score +
                ", numTreeNodes=" + numTreeNodes() +
                ", referenceJson=" + referenceJson +
                ", numSample=" + numSamples +
                " }";
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

    public void makeLeaf(ClientInfo client) {
        // make a leaf
        isLeaf = true;
        //prediction = DataUtils.mean(DataUtils.toSmpMatrix(y));
        referenceJson.addProperty("is_leaf", 1);
        referenceJson.addProperty("prediction", prediction);
        party = client;
    }


}


