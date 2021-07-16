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

import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.grpc.federatedlearning.Vector;
import com.jdt.fedlearn.grpc.federatedlearning.PaillierKeyPublic;
import com.jdt.fedlearn.grpc.federatedlearning.PaillierVector;
import com.jdt.fedlearn.core.encryption.Decryptor;
import com.jdt.fedlearn.core.encryption.Encryptor;
import com.n1analytics.paillier.EncryptedNumber;
import org.ejml.simple.SimpleMatrix;


import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TypeRandomForest {

    private static final Logger logger = LoggerFactory.getLogger(TypeRandomForest.class);
    //private final int numTrees;
    private int numTrees;
    private int maxDepth;
    private int maxSampledFeatures;
    private int maxTreeSamples;
    private int numPercentiles;
    private int nJobs;
    private int[] numFeatures;
    private ArrayList<TreeNodeRF> roots = new ArrayList<>();
    private ArrayList<Boolean> isFinished = new ArrayList<>();
    private Encryptor encryptor;
    private PaillierKeyPublic keyPublic;
    private Decryptor decryptor;

    // add simple matrix X
    public SimpleMatrix X_train, y_train, X_test, y_test;

    // add encrypt vector y
    private int numParties;
    private ArrayList<Integer>[][] sampledFeatureIds;
    private int numTotalFeatures;
    private int numNodesAll = 0;
    //private ArrayList<Tuple2<Integer, Integer>> featureInfo = new ArrayList<>();

    public TreeNodeRF currentNode = null;
    public Map<Integer, TreeNodeRF> treeNodeMap = Maps.newLinkedHashMap();

    // inference part
    private SimpleMatrix[] Xs_inference;
    private SimpleMatrix y_pred_list;
    private TreeNodeRF[][] inference_node;
    private List<TreeNodeRF> tmp_infer_node;
    private List<Integer> tmp_sampleId;
    private int numInferenceSamples;

    // stop
    public boolean trainStop = false;
    public boolean inferStop = false;

    // statistics
    private ArrayList<Integer> node_count;

    public TypeRandomForest(int numTrees,
                            int maxDepth,
                            int maxSampledFeatures,
                            int maxTreeSamples,
                            int numPercentiles,
                            int numSamples,
                            int nJobs,
                            Random rand) {
        this.numTrees = numTrees;
        this.maxDepth = maxDepth;
        this.maxTreeSamples = maxTreeSamples;
        this.maxSampledFeatures = maxSampledFeatures;
        this.numPercentiles = numPercentiles;
        this.nJobs = nJobs;

        // 建树
        for (int i = 0; i < numTrees; i++) {
            ArrayList<Integer> sampleId;
            if (maxTreeSamples == -1) {
                sampleId = (ArrayList<Integer>) IntStream.range(0, numSamples).boxed().collect(Collectors.toList());
            } else {
                sampleId = DataUtils.choice(maxTreeSamples, numSamples, rand);
            }
            Collections.sort(sampleId);
            //logger.info(String.format("Tree id: %s, sample size: %s, sample ids: %s", i, sampleId.size(), sampleId.toString()));
            roots.add(new TreeNodeRF(sampleId, 0, i));
            isFinished.add(false);
        }

    }

    public TypeRandomForest(int numTrees, int numInferenceSamples) {
        this.numTrees = numTrees;
        this.numInferenceSamples = numInferenceSamples;
    }

    public int getNumPercentiles() {
        return numPercentiles;
    }

    public ArrayList<TreeNodeRF> getRoots() {
        return roots;
    }

    public int getNumTrees() {
        return numTrees;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    // 查询没有被分割的节点
    public TreeNodeRF getActiveNode(TreeNodeRF root) {
        if (root == null) {
            return null;
        }
        if ((root.left == null) && (root.right == null)) {
            if (root.isLeaf) {
                return null;
            } else {
                return root;
            }
        }
        // try left child
        TreeNodeRF left = getActiveNode(root.left);
        if (left == null) {
            // try right child
            TreeNodeRF right = getActiveNode(root.right);
            if (right == null) {
                // check if node is leaf
                return null;
            } else {
                return right;
            }
        } else {
            return left;
        }
    }

    public TreeNodeRF getTrainNode() {
        if (currentNode == null) {
            for (int i = 0; i < numTrees; i++) {
                if (!isFinished.get(i)) {
                    currentNode = getActiveNode(roots.get(i));
                    if (currentNode == null) {
                        isFinished.set(i, true);
                    } else {
                        //logger.info(String.format("Get a node from %sth tree", i));
                        return currentNode;
                    }
                }
            }

        } else {
            return currentNode;
        }
        return null;
    }

    public Map<Integer, TreeNodeRF> getTrainNodeAllTrees() {
        // get train node for all trees
        if (treeNodeMap.isEmpty()) {
            for (int i = 0; i < numTrees; i++) {
                if (!isFinished.get(i)) {
                    TreeNodeRF node = getActiveNode(roots.get(i));
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
//                nodei.Xs = null;
//                nodei.y_pvec = null;
                numNodesAll = numNodesAll + 1;
                treeNodeMap.remove(i);
            }
        }
    }


    // inference 部分
    public void initInference(SimpleMatrix[] Xs) {
        // 根据树的数量建立 y_pred
        y_pred_list = new SimpleMatrix(numTrees, Xs[0].numRows());
        inference_node = new TreeNodeRF[numTrees][];
        y_pred_list.fill(0);
        for (int i = 0; i < numTrees; i++) {
            inference_node[i] = new TreeNodeRF[Xs[0].numRows()];
            for (int k = 0; k < Xs[0].numRows(); k++) {
                inference_node[i][k] = roots.get(i);
            }
        }
    }

    public void initInference() {
        // 根据树的数量建立 y_pred
        y_pred_list = new SimpleMatrix(numTrees, numInferenceSamples);
        inference_node = new TreeNodeRF[numTrees][];
        y_pred_list.fill(0);
        for (int i = 0; i < numTrees; i++) {
            inference_node[i] = new TreeNodeRF[numInferenceSamples];
            for (int k = 0; k < numInferenceSamples; k++) {
                inference_node[i][k] = roots.get(i);
            }
        }
    }

    public List<TreeNodeRF> getInferenceNode() {
        tmp_infer_node = new ArrayList<>();
        tmp_sampleId = new ArrayList<>();
        for (int i = 0; i < inference_node.length; i++) {
            for (int k = 0; k < inference_node[i].length; k++) {
                if (!(inference_node[i][k] == null)) {
                    // 判断是否为叶节点，如果为叶节点则更新数据
                    if (inference_node[i][k].isLeaf) {
                        y_pred_list.set(i, k, inference_node[i][k].prediction);
                        // 已经赋值完毕，set null
                        inference_node[i][k] = null;
                    } else {
                        tmp_infer_node.add(inference_node[i][k]);
                        tmp_sampleId.add(k);
                    }
                }
            }
        }
        return tmp_infer_node;
    }

    public List<Integer> getTmp_sampleId() {
        return tmp_sampleId;
    }


    public void updateInferenceNode(Map<String, List<Boolean>> is_left) {
        Map<String, Integer> idx = new HashMap<>();
        for (String ci : is_left.keySet()) {
            idx.put(ci, 0);
        }
        for (int i = 0; i < tmp_infer_node.size(); i++) {
            TreeNodeRF nodei = tmp_infer_node.get(i);
            ClientInfo party = nodei.party;
            TreeNodeRF tmp_node = inference_node[(int) nodei.treeId][tmp_sampleId.get(i)];
            // TODO: ticket-2020-11-22 现在先用 .url() 来唯一确定 client，后续会使用其他更robust的方式比如 .equals()
            if (is_left.get(party.url()).get(idx.get(party.url()))) {
                inference_node[(int) nodei.treeId][tmp_sampleId.get(i)] = tmp_node.left;
            } else {
                inference_node[(int) nodei.treeId][tmp_sampleId.get(i)] = tmp_node.right;
            }
            // TODO: ticket-2020-11-22 现在先用 .url() 来唯一确定 client，后续会使用其他更robust的方式比如 .equals()
            idx.put(party.url(), idx.get(party.url()) + 1);
        }
    }

    public SimpleMatrix getPrediction() {
        //logger.info("y_pred_list");
        //y_pred_list.transpose().print();
        SimpleMatrix average_vector = new SimpleMatrix(1, numTrees);
        average_vector.fill(1. / average_vector.numCols());
        SimpleMatrix prediction = average_vector.mult(y_pred_list);
        //prediction.transpose().print();
        return prediction;
    }

    public void tree2map(TreeNodeRF node, Map<String, Map<String, String>> map, int depth, ClientInfo client) {
        if (node == null) {
            return;
        }
        Map<String, String> elements = new HashMap();
        node_count.set(depth, node_count.get(depth) + 1);
        if (node.isLeaf) {
            if ((client.getIp() + ":" + client.getPort())
                    .equals(node.party.getIp() + ":" + node.party.getPort())) {
                elements.put("prediction", String.valueOf(node.prediction));
                elements.put("isLeaf", "1");
                elements.put("left", "null");
                elements.put("right", "null");
            }
        } else {
            if (node.party != null) {
                if ((client.getIp() + ":" + client.getPort())
                        .equals(node.party.getIp() + ":" + node.party.getPort())) {
                    elements.put("referenceJson", String.valueOf(node.referenceJson));
                    ObjectMapper mapper = new ObjectMapper();
                    try {
                        elements.put("party", mapper.writeValueAsString(node.party));
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

    public String tree2json(TreeNodeRF root, ClientInfo client) {
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

    public TreeNodeRF map2tree(Map<String, Map<String, String>> map, int treeId, int nodeId) {
        if (map.containsKey(String.valueOf(nodeId))) {
            // has node
            TreeNodeRF node = new TreeNodeRF();
            node.treeId = treeId;
            node.nodeId = nodeId;
            Map<String, String> tmp = map.get(String.valueOf(nodeId));
            if ("1".equals(tmp.get("isLeaf"))) {
                // is leaf
                node.isLeaf = true;
                node.prediction = Double.parseDouble(tmp.get("prediction"));
                node.left = null;
                node.right = null;
            } else {
                node.referenceJson = JsonParser.parseString(tmp.get("referenceJson")).getAsJsonObject();
                JsonObject party = JsonParser.parseString(tmp.get("party")).getAsJsonObject();
                node.party = new ClientInfo(party.get("ip").getAsString(),
                        party.get("port").getAsInt(),
                        party.get("protocol").getAsString(),
                        party.get("uniqueId").getAsInt());
                // TODO: hasLabel ???????????
                //node.party.setHasLabel(party.get("hasLabel").getAsBoolean());
                node.featureId = node.referenceJson.get("feature_opt").getAsInt();
                node.thres = node.referenceJson.get("value_opt").getAsDouble();
            }
            node.left = map2tree(map, treeId, nodeId * 2 + 1);
            node.right = map2tree(map, treeId, nodeId * 2 + 2);
            return node;
        } else {
            return null;
        }
    }

    public TreeNodeRF json2tree(String s, int treeId) {
        TreeNodeRF root = new TreeNodeRF();
        root.treeId = treeId;
        ObjectMapper mapper = new ObjectMapper();
        TypeReference<HashMap<String, Map<String, String>>> typeRef
                = new TypeReference<HashMap<String, Map<String, String>>>() {
        };
        try {
            Map<String, Map<String, String>> map = mapper.readValue(s, typeRef);
            root = map2tree(map, treeId, 0);
        } catch (JsonProcessingException e) {
            logger.error("json2tree error", e);
        }
        return root;
    }

    public void load(Map<String, String> map) {
        numTrees = Integer.parseInt(map.get("numTrees"));
        roots = new ArrayList<>();
        for (int i = 0; i < numTrees; i++) {
            roots.add(json2tree(map.get(String.format("Tree%s", i)), i));
        }
        return;
    }

}
