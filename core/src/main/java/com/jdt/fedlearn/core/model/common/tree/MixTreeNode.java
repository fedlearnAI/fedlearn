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

package com.jdt.fedlearn.core.model.common.tree;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.type.data.DoubleTuple2;

import java.util.HashSet;
import java.util.Set;

/**
 * @author zhangwenxi
 */
public class MixTreeNode {   // 服务端和客户端都需要用到
    private int depth;
    private boolean isLeaf;
    /**
     * when split finding,
     * record the best threshold, gain, missing value's branch for each feature
     */
    private double tempVerticalGain;
    private double tempHorizontalGain;
    private DoubleTuple2 nodeGH;

    /**
     * splitFeatureType 0 横向 1 纵向
     */
    private int splitFeatureType;
    private String splitFeatureName;
    private double splitThreshold;
    private String verticalSplitFeatureName;
    private double verticalSplitThreshold;

    private MixTreeNode leftChild;
    private MixTreeNode rightChild;
    private MixTreeNode parent;
    /**
     * leaf node
     */
    private double leafScore;

    /**
     * 当前节点的实例空间
     */
    private Set<Integer> instanceIdSpaceSet;
    private Set<Integer> verticalInstanceIdSpaceSet;
    /**
     * 处理的客户端唯一标记
     */
    private int recordId;
    private ClientInfo client;

    public MixTreeNode(int depth) {
        this.depth = depth;
        this.isLeaf = false;
        //0 横向 1 纵向
        this.splitFeatureType = -1;
        this.tempHorizontalGain = -Double.MAX_VALUE;
        this.tempVerticalGain = -Double.MAX_VALUE;
        this.recordId = -1;
    }

    public MixTreeNode(int depth, int recordId) {
        this.depth = depth;
        this.recordId = recordId;
        this.isLeaf = false;
        this.splitFeatureType = -1;
        this.instanceIdSpaceSet = new HashSet<>();
        this.tempHorizontalGain = -Double.MAX_VALUE;
        this.tempVerticalGain = -Double.MAX_VALUE;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public int getRecordId() {
        return recordId;
    }

    public void setRecordId(int recordId) {
        this.recordId = recordId;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public void setAsLeaf(double leafScore) {
        isLeaf = true;
        setLeafScore(leafScore);
    }

    public double getLeafScore() {
        return leafScore;
    }

    public void setLeafScore(double leafScore) {
        this.leafScore = leafScore;
    }

    public MixTreeNode getLeftChild() {
        return leftChild;
    }

    public void setRightChild(MixTreeNode rightChild) {
        this.rightChild = rightChild;
    }

    public MixTreeNode getRightChild() {
        return rightChild;
    }

    public void setParent(MixTreeNode parent) {
        this.parent = parent;
    }

    public MixTreeNode getParent() {
        return parent;
    }

    public void setLeftChild(MixTreeNode leftChild) {
        this.leftChild = leftChild;
    }

    public int getSplitFeatureType() {
        return splitFeatureType;
    }

    public void setSplitFeatureType(int value) {
        this.splitFeatureType = value;
    }

    public double getTempVerticalGain() {
        return tempVerticalGain;
    }

    public void setTempVerticalGain(double value) {
        this.tempVerticalGain = value;
    }

    public double getTempHorizontalGain() {
        return tempHorizontalGain;
    }

    public void setTempHorizontalGain(double value) {
        this.tempHorizontalGain = value;
    }

    public double getSplitThreshold() {
        return splitThreshold;
    }

    public void setSplitThreshold(double value) {
        this.splitThreshold = value;
    }

    public String getSplitFeatureName() {
        return splitFeatureName;
    }

    public void setVerticalSplitFeatureName(String name) {
        this.verticalSplitFeatureName = name;
    }

    public double getVerticalSplitThreshold() {
        return verticalSplitThreshold;
    }

    public void setVerticalSplitThreshold(double value) {
        this.verticalSplitThreshold = value;
    }

    public String getVerticalSplitFeatureName() {
        return verticalSplitFeatureName;
    }

    public void setSplitFeatureName(String name) {
        this.splitFeatureName = name;
    }

    public ClientInfo getClient() {
        return client;
    }

    public void setClient(ClientInfo client) {
        this.client = client;
    }

    public void setNodeGH(DoubleTuple2 nodeGH) {
        this.nodeGH = nodeGH;
    }

    public DoubleTuple2 getNodeGH() {
        return nodeGH;
    }

    public Set<Integer> getInstanceIdSpaceSet() {
        return instanceIdSpaceSet;
    }

    public Set<Integer> getVerticalInstanceIdSpaceSet() {
        return verticalInstanceIdSpaceSet;
    }

    public void setInstanceIdSpaceSet(Set<Integer> instanceIdSpaceSet) {
        this.instanceIdSpaceSet = instanceIdSpaceSet;
    }

    public void setVerticalInstanceIdSpaceSet(Set<Integer> verticalInstanceIdSpaceSet) {
        this.verticalInstanceIdSpaceSet = verticalInstanceIdSpaceSet;
    }

    public MixTreeNode backTrackingTreeNode() {
        MixTreeNode curTreeNode = this;
        MixTreeNode parentNode;
        while (true) {
            parentNode = curTreeNode.getParent();
            if (parentNode == null) {
                return null;
            }
            if (parentNode.getLeftChild() == curTreeNode) {
                if (parentNode.getRightChild() == null) {
                    MixTreeNode newNode = new MixTreeNode(curTreeNode.getDepth());
                    newNode.setParent(parentNode);
                    parentNode.setRightChild(newNode);
                    newNode.instanceIdSpaceSet = new HashSet<>(parentNode.instanceIdSpaceSet);
                    newNode.instanceIdSpaceSet.removeAll(curTreeNode.instanceIdSpaceSet);
                    // save memory cost
                    curTreeNode.instanceIdSpaceSet = null;
                    if (curTreeNode.getRightChild() != null) {
                        curTreeNode.getRightChild().instanceIdSpaceSet = null;
                    }
                }
                curTreeNode = parentNode.getRightChild();
                break;
            }
            curTreeNode = parentNode;
        }
        return curTreeNode;
    }
}