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

package com.jdt.fedlearn.core.entity.boost;

import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.core.model.common.tree.Tree;
import com.jdt.fedlearn.core.model.common.tree.TreeNode;
import com.jdt.fedlearn.core.type.data.StringTuple2;

import java.io.Serializable;

import java.util.ArrayList;
import java.util.List;

public class SubModel implements Serializable {
    private String privateKey;
    private String keyPublic;
    private TreeNode currentNode;
    private int recordId;
    //phase3
    private ClientInfo clientInfo;
    private int splitFeature;
    private double gain;
    // phase5
    private double[][] grad;
    private double[][] hess;
    //phase4
    private List<QueryEntry> passiveQueryTable;
    private List<Tree> trees;
    private StringTuple2[] subGH;
    private int instanceMin;
    private int instanceMax;


    public SubModel() {
    }


    public SubModel(TreeNode currentNode) {
        this.currentNode = currentNode;
    }

    public SubModel(String privateKey, String keyPublic, TreeNode currentNode) {
        this.privateKey = privateKey;
        this.keyPublic = keyPublic;
        this.currentNode = currentNode;
    }

    public SubModel(ClientInfo clientInfo, int splitFeature, double gain) {
        this.clientInfo = clientInfo;
        this.splitFeature = splitFeature;
        this.gain = gain;
    }

    public SubModel(double[][] grad, double[][] hess, int recordId, List<Tree> trees) {
        this.grad = grad;
        this.hess = hess;
        this.recordId = recordId;
        this.trees = trees;
    }

    public SubModel(List<QueryEntry> passiveQueryTable) {
        this.passiveQueryTable = passiveQueryTable;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getKeyPublic() {
        return keyPublic;
    }

    public void setKeyPublic(String keyPublic) {
        this.keyPublic = keyPublic;
    }

    public TreeNode getCurrentNode() {
        return currentNode;
    }

    public void setCurrentNode(TreeNode currentNode) {
        this.currentNode = currentNode;
    }

    public ClientInfo getClientInfo() {
        return clientInfo;
    }

    public void setClientInfo(ClientInfo clientInfo) {
        this.clientInfo = clientInfo;
    }

    public int getSplitFeature() {
        return splitFeature;
    }

    public void setSplitFeature(int splitFeature) {
        this.splitFeature = splitFeature;
    }

    public double getGain() {
        return gain;
    }

    public void setGain(double gain) {
        this.gain = gain;
    }

    public double[][] getGrad() {
        return grad;
    }

    public void setGrad(double[][] grad) {
        this.grad = grad;
    }

    public double[][] getHess() {
        return hess;
    }

    public void setHess(double[][] hess) {
        this.hess = hess;
    }

    public List<QueryEntry> getPassiveQueryTable() {
        return passiveQueryTable;
    }

    public void setPassiveQueryTable(List<QueryEntry> passiveQueryTable) {
        this.passiveQueryTable = passiveQueryTable;
    }

    public int getRecordId() {
        return recordId;
    }

    public void setRecordId(int recordId) {
        this.recordId = recordId;
    }

    public List<Tree> getTrees() {
        return trees;
    }

    public void setTrees(List<Tree> trees) {
        this.trees = trees;
    }

    public StringTuple2[] getSubGH() {
        return subGH;
    }

    public void setSubGH(StringTuple2[] subGH) {
        this.subGH = subGH;
    }

    public int getInstanceMin() {
        return instanceMin;
    }

    public void setInstanceMin(int instanceMin) {
        this.instanceMin = instanceMin;
    }

    public int getInstanceMax() {
        return instanceMax;
    }

    public void setInstanceMax(int instanceMax) {
        this.instanceMax = instanceMax;
    }
}
