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
import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import org.ejml.simple.SimpleMatrix;


import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TypeRandomForest implements Serializable {

    private static final Logger logger = LoggerFactory.getLogger(TypeRandomForest.class);
    private int numTrees;
    private int maxDepth;
    private int numPercentiles;
    private ArrayList<TreeNodeRF> roots = new ArrayList<>();
    private ArrayList<Boolean> isFinished = new ArrayList<>();
    private int numNodesAll = 0;

    /** 目前所有需要分裂的点的队列 */
    private ArrayList<Queue<TreeNodeRF>> aliveNodes = new ArrayList<>();
    public Map<Integer, TreeNodeRF> treeNodeMap = Maps.newLinkedHashMap();

    // inference part

    // statistics
    private ArrayList<Integer> node_count;

    public TypeRandomForest(){}

    public TypeRandomForest(int numTrees,
                            int maxDepth,
                            int maxTreeSamples,
                            int numPercentiles,
                            int numSamples,
                            Random rand) {
        this.numTrees = numTrees;

        // 建树
        for (int i = 0; i < numTrees; i++) {
            List<Integer> sampleId;
            if (maxTreeSamples == -1) {
                sampleId = IntStream.range(0, numSamples).boxed().collect(Collectors.toList());
            } else {
                sampleId = DataUtils.choice(maxTreeSamples, numSamples, rand);
            }
            Collections.sort(sampleId);
            //logger.info(String.format("Tree id: %s, sample size: %s, sample ids: %s", i, sampleId.size(), sampleId.toString()));
            roots.add(new TreeNodeRF(sampleId, 0, i));
            isFinished.add(false);
            Queue<TreeNodeRF> aliveNode = new LinkedList<>();
            aliveNode.offer(roots.get(i));
            aliveNodes.add(aliveNode);
        }

    }

    public TypeRandomForest(int numTrees,
                            int maxDepth,
                            int numPercentiles,
                            Map<?,?> sampleIds,
                            Random rand) {
        this.numTrees = numTrees;
        this.maxDepth = maxDepth;
        this.numPercentiles = numPercentiles;

        // 建树
        for (int i = 0; i < numTrees; i++) {
            List<Integer> sampleId;
            sampleId = DataUtils.stringToSampleId(String.valueOf(sampleIds.get(i)));
            //logger.info(String.format("Tree id: %s, sample size: %s, sample ids: %s", i, sampleId.size(), sampleId.toString()));
            roots.add(new TreeNodeRF(sampleId, 0, i));
            isFinished.add(false);
            Queue<TreeNodeRF> aliveNode = new LinkedList<>();
            aliveNode.offer(roots.get(i));
            aliveNodes.add(aliveNode);
        }

    }

    // 查询没有被分割的节点
    public TreeNodeRF getActiveNode(TreeNodeRF root, int treeID) {
        return aliveNodes.get(treeID).poll();
    }

    public Map<Integer, TreeNodeRF> getTrainNodeAllTrees() {
        // get train node for all trees
        if (treeNodeMap.isEmpty()) {
            for (int i = 0; i < numTrees; i++) {
                if (!isFinished.get(i)) {
                    TreeNodeRF node = getActiveNode(roots.get(i), i);
                    if (node == null) {
                        isFinished.set(i, true);
                    } else {
                        treeNodeMap.put(i, node);
                    }
                }
            }
        }
        return treeNodeMap;
    }


    public void releaseTreeNodeAllTrees() {
        // release data in train node of all trees
        List<Integer> keys = new ArrayList<Integer>(treeNodeMap.keySet());
        for (int i : keys) {
            TreeNodeRF nodei = treeNodeMap.get(i);
            if (!(nodei == null)) {
                numNodesAll = numNodesAll + 1;
                treeNodeMap.remove(i);
            }
        }
    }

    private void tree2map(TreeNodeRF node, Map<String, Map<String, String>> map, int depth, String client) {
        if (node == null) {
            return;
        }
        Map<String, String> elements = new HashMap();
        node_count.set(depth, node_count.get(depth) + 1);
        if (node.isLeaf) {
            if ((client).equals(node.party)) {
                elements.put("prediction", String.valueOf(node.prediction));
                elements.put("isLeaf", "1");
                elements.put("left", "null");
                elements.put("right", "null");
            }
        } else {
            if (node.party != null) {
                if ((client).equals(node.party)) {
                    elements.put("referenceJson", node.referenceJsonStr);
                    ObjectMapper mapper = new ObjectMapper();
                    try {
                        elements.put("party", mapper.writeValueAsString(node.party).replace(" ", "").replace("=",":"));
                    } catch (JsonProcessingException e) {
                        logger.error("JsonProcessingException: ", e);
                    }
                    elements.put("isLeaf", "0");
                }
                tree2map(node.left, map, depth + 1, client);
                tree2map(node.right, map, depth + 1, client);
            }
        }
        if (elements.keySet().size() > 0) {
            elements.put("nodeId", String.valueOf(node.nodeId));
            map.put(String.valueOf(node.nodeId), elements);
        }
    }

    public String tree2json(TreeNodeRF root, String client) {
        // convert tree to json
        String jsonStr;
        Map<String, Map<String, String>> elements = new HashMap();

        // create stats
        node_count = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            node_count.add(0);
        }

        tree2map(root, elements, 0, client);

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            jsonStr = objectMapper.writeValueAsString(elements);
        } catch (Exception e) {
            logger.error("tree2json error", e);
            jsonStr = "";
        }
        return jsonStr;
    }

    public int getNumTrees() {
        return numTrees;
    }

    public void setNumTrees(int numTrees) {
        this.numTrees = numTrees;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    public int getNumPercentiles() {
        return numPercentiles;
    }

    public void setNumPercentiles(int numPercentiles) {
        this.numPercentiles = numPercentiles;
    }

    public ArrayList<TreeNodeRF> getRoots() {
        return roots;
    }

    public void setRoots(ArrayList<TreeNodeRF> roots) {
        this.roots = roots;
    }

    public ArrayList<Boolean> getIsFinished() {
        return isFinished;
    }

    public void setIsFinished(ArrayList<Boolean> isFinished) {
        this.isFinished = isFinished;
    }

    public int getNumNodesAll() {
        return numNodesAll;
    }

    public void setNumNodesAll(int numNodesAll) {
        this.numNodesAll = numNodesAll;
    }

    public ArrayList<Queue<TreeNodeRF>> getAliveNodes() {
        return aliveNodes;
    }

    public void setAliveNodes(ArrayList<Queue<TreeNodeRF>> aliveNodes) {
        this.aliveNodes = aliveNodes;
    }

    public Map<Integer, TreeNodeRF> getTreeNodeMap() {
        return treeNodeMap;
    }

    public void setTreeNodeMap(Map<Integer, TreeNodeRF> treeNodeMap) {
        this.treeNodeMap = treeNodeMap;
    }

    public ArrayList<Integer> getNode_count() {
        return node_count;
    }

    public void setNode_count(ArrayList<Integer> node_count) {
        this.node_count = node_count;
    }
}
