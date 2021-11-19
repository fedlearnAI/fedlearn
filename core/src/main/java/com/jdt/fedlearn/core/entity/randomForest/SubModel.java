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

import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.core.entity.boost.QueryEntry;
import com.jdt.fedlearn.core.model.common.tree.TreeNode;

import java.io.Serializable;
import java.util.*;

public class SubModel implements Serializable {
    List<Integer> treeIDs = new ArrayList<>();
    Map<String, Double> localTrees = new HashMap<>();
    Map<Integer, Boolean> isFinished = new HashMap<>();
    Map<Integer, TreeNodeRF> roots = new HashMap<>();
    Map<Integer, TreeNodeRF> treeNodeMap = new HashMap<>();
    Map<Integer, TreeNodeRF> currentNodeMap = new HashMap<>();
    Map<Integer, Queue<TreeNodeRF>> aliveNodes = new HashMap<>();

    public SubModel() {
    }


    public SubModel(Map<String, Double> localTrees, Map<Integer, Boolean> isFinished, Map<Integer, TreeNodeRF> roots, Map<Integer, TreeNodeRF> treeNodeMap, Map<Integer, TreeNodeRF> currentNodeMap, Map<Integer, Queue<TreeNodeRF>> aliveNodes, List<Integer> treeIDs) {
        this.localTrees = localTrees;
        this.isFinished = isFinished;
        this.roots = roots;
        this.treeNodeMap = treeNodeMap;
        this.currentNodeMap = currentNodeMap;
        this.aliveNodes = aliveNodes;
        this.treeIDs = treeIDs;
    }

    public Map<String, Double> getLocalTrees() {
        return localTrees;
    }

    public void setLocalTrees(Map<String, Double> localTrees) {
        this.localTrees = localTrees;
    }

    public Map<Integer, Boolean> getIsFinished() {
        return isFinished;
    }

    public void setIsFinished(Map<Integer, Boolean> isFinished) {
        this.isFinished = isFinished;
    }

    public Map<Integer, TreeNodeRF> getRoots() {
        return roots;
    }

    public void setRoots(Map<Integer, TreeNodeRF> roots) {
        this.roots = roots;
    }

    public Map<Integer, TreeNodeRF> getTreeNodeMap() {
        return treeNodeMap;
    }

    public void setTreeNodeMap(Map<Integer, TreeNodeRF> treeNodeMap) {
        this.treeNodeMap = treeNodeMap;
    }

    public Map<Integer, TreeNodeRF> getCurrentNodeMap() {
        return currentNodeMap;
    }

    public void setCurrentNodeMap(Map<Integer, TreeNodeRF> currentNodeMap) {
        this.currentNodeMap = currentNodeMap;
    }

    public Map<Integer, Queue<TreeNodeRF>> getAliveNodes() {
        return aliveNodes;
    }

    public void setAliveNodes(Map<Integer, Queue<TreeNodeRF>> aliveNodes) {
        this.aliveNodes = aliveNodes;
    }

    public List<Integer> getTreeIDs() {
        return treeIDs;
    }

    public void setTreeIDs(List<Integer> treeIDs) {
        this.treeIDs = treeIDs;
    }
}
