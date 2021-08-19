package com.jdt.fedlearn.core.model.serialize;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.boost.QueryEntry;
import com.jdt.fedlearn.core.exception.SerializeException;
import com.jdt.fedlearn.core.model.common.loss.LogisticLoss;
import com.jdt.fedlearn.core.model.common.loss.Loss;
import com.jdt.fedlearn.core.model.common.loss.SquareLoss;
import com.jdt.fedlearn.core.model.common.loss.crossEntropy;
import com.jdt.fedlearn.core.model.common.tree.Tree;
import com.jdt.fedlearn.core.model.common.tree.TreeNode;

import java.io.IOException;
import java.util.*;

public final class FgbModelSerializer implements ModelSerializer {
    private static final String separator = "#";
    private final List<Tree> trees;
    private final Loss loss;
    private final double firstRoundPred;
    private final double eta;
    private final List<QueryEntry> passiveQueryTable;
    private final List<Double> multiClassUniqueLabelList;

    public FgbModelSerializer(List<Tree> trees, Loss loss, double firstRoundPred, double eta,List<QueryEntry>  passiveQueryTable, List<Double> multiClassUniqueLabelList) {
        this.trees = trees;
        this.loss = loss;
        this.firstRoundPred = firstRoundPred;
        this.eta = eta;
        this.passiveQueryTable = passiveQueryTable;
        this.multiClassUniqueLabelList = multiClassUniqueLabelList;
    }

    public FgbModelSerializer(String content) {
        FgbModelSerializer s = loadModel(content);
        this.trees = s.getTrees();
        this.loss = s.getLoss();
        this.firstRoundPred = s.getFirstRoundPred();
        this.eta = s.getEta();
        this.passiveQueryTable = s.getPassiveQueryTable();
        this.multiClassUniqueLabelList = s.getMultiClassUniqueLabelList();
    }

    public FgbModelSerializer loadModel(String content) {
        String[] lines = content.split("\n");
        int lineStart = 0;
        //前4行依次是first_round_predict,eta,loss,queryTable
        double first_round_predict = Double.parseDouble(lines[lineStart].split("=")[1]);
        double eta = Double.parseDouble(lines[lineStart + 1].split("=")[1]);
        Loss loss = null;
        if ("logloss".equals(lines[lineStart + 2])) {
            loss = new LogisticLoss();
        } else if ("squareloss".equals(lines[2])) {
            loss = new SquareLoss();
        } else {
            loss = new crossEntropy();
        }
        List<QueryEntry>  passiveQueryTable = deserializeQueryTable(lines[lineStart + 3].split("=")[1]);

        List<Double> multiClassUniqueLabelList = new ArrayList<>();
        for (String label : lines[lineStart + 5].split(" ")) {
            if (!"".equals(label)) {
                multiClassUniqueLabelList.add(Double.parseDouble(label));
            }
        }

        ArrayList<Tree> trees = new ArrayList<>();
        String line;
        HashMap<Integer, TreeNode> map = new HashMap<>();
        for (int i = lineStart + 6; i < lines.length; i++) {
            line = lines[i];
            if (line.startsWith("tree")) {
                //store this tree,clear map
                if (!map.isEmpty()) {
                    Queue<TreeNode> queue = new LinkedList<>();
                    TreeNode root = map.get(1);
                    queue.offer(root);
                    while (!queue.isEmpty()) {
                        int cur_level_num = queue.size();
                        while (cur_level_num != 0) {
                            cur_level_num--;
                            TreeNode node = queue.poll();
                            if (node == null) {
                                continue;
                            }
                            if (!node.isLeaf) {
                                node.leftChild = map.get(3 * node.index - 1);
                                node.rightChild = map.get(3 * node.index + 1);
                                queue.offer(node.leftChild);
                                queue.offer(node.rightChild);
                                if (map.containsKey(3 * node.index)) {
                                    node.nanChild = map.get(3 * node.index);
                                    queue.offer(node.nanChild);
                                }
                            }
                        }
                    }

                    trees.add(new Tree(root));
                    map.clear();
                }
            } else {
                //store this node into map
                String[] elements = line.split(separator);
                int index = Integer.parseInt(elements[0]);
                if (elements[1].startsWith("leaf")) {
                    //deserializeLeafNode
                    double leaf_score = Double.parseDouble(elements[1].split("=")[1]);
                    TreeNode node = new TreeNode(index, leaf_score);
                    map.put(index, node);
                } else {
                    double nan_go_to = Double.parseDouble(elements[2].split("=")[1]);
                    String split_info = elements[1].split("]")[0];
                    split_info = split_info.substring(1);
                    String[] strs = split_info.split(",");
                    int split_feature = Integer.parseInt(strs[0]);
                    if ("num".equals(strs[1])) {
//                        double split_threshold = Double.parseDouble(strs[2]);
                        ClientInfo client = new ClientInfo();
                        client.deserialize(strs[2]);
                        int recordId = Integer.parseInt(strs[3]);
                        TreeNode node = new TreeNode(index, split_feature, client, recordId, nan_go_to);
                        map.put(index, node);
                    } else {
                        ArrayList<Double> split_left_child_catvalue = new ArrayList<>();
                        for (int j = 2; j < strs.length; j++) {
                            split_left_child_catvalue.add(Double.parseDouble(strs[j]));
                        }
                        TreeNode node = new TreeNode(index, split_feature, split_left_child_catvalue, nan_go_to);
                        map.put(index, node);
                    }
                }
            }
        }
        return new FgbModelSerializer(trees, loss, first_round_predict, eta, passiveQueryTable, multiClassUniqueLabelList);
    }

    private static List<QueryEntry>  deserializeQueryTable(String content) {
        ObjectMapper mapper = new ObjectMapper();
        List<QueryEntry>  p1r;
        try {
            p1r = mapper.readValue(content, new TypeReference<List<QueryEntry>>() {
            });
            return p1r;
        } catch (IOException e) {
            throw new SerializeException("deserialize QueryTable error");
        }
    }

    private static String serializeLeafNode(TreeNode node) {
        StringBuilder sb = new StringBuilder();
        sb.append(node.index).append(separator).append("leaf=");
        sb.append(String.format("%.16f", node.leafScore));
        return sb.toString();
    }

    private static String serializeInternalNode(TreeNode node) {
        StringBuilder sb = new StringBuilder();
        sb.append(node.index).append(separator).append("[").append(node.splitFeature).append(",");
        if (node.splitLeftChildCatvalue == null) {
            String clientSerialize = null;
            if (null != node.client) {
                clientSerialize = node.client.serialize();
            }
            sb.append("num,").append(clientSerialize).append(",").append(node.recordId).append("]").append(separator);
        } else {
            sb.append("cat");
            for (double catvalue : node.splitLeftChildCatvalue) {
                sb.append(",").append(catvalue);
            }
            sb.append("]").append(separator);
        }
        if (node.nanGoTo == 0) {
            sb.append("missing_go_to=0");
        } else if (node.nanGoTo == 1) {
            sb.append("missing_go_to=1");
        } else if (node.nanGoTo == 2) {
            sb.append("missing_go_to=2");
        } else {
            if (node.leftChild.numSample > node.rightChild.numSample) {
                sb.append("missing_go_to=1");
            } else {
                sb.append("missing_go_to=2");
            }
        }
        return sb.toString();
    }

    private static String serializeQueryTable(List<QueryEntry>  passiveQueryTable) {
        String jsonStr = null;
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            jsonStr = objectMapper.writeValueAsString(passiveQueryTable);
        } catch (Exception e) {
            throw new SerializeException("passiveQueryTable Serialize Exception");
        }
        return jsonStr;
    }

    public String saveModel() {
        return saveModel(this.firstRoundPred, this.eta, this.loss, this.trees, this.passiveQueryTable, this.multiClassUniqueLabelList);
    }

    //Serialize the FederatedGB model into txt file
    public String saveModel(double first_round_predict, double eta, Loss loss, List<Tree> trees, List<QueryEntry>  passiveQueryTable, List<Double> multiClassUniqueLabelList) {
        StringBuilder sb = new StringBuilder();
        sb.append("first_round_predict=").append(first_round_predict).append("\n");
        sb.append("eta=").append(eta).append("\n");
        if (loss instanceof LogisticLoss) {
            sb.append("logloss" + "\n");
        } else if (loss instanceof SquareLoss) {
            sb.append("squareloss" + "\n");
        } else {
            sb.append("crossEntropy" + "\n");
        }
        sb.append("passiveQueryTable=").append(serializeQueryTable(passiveQueryTable)).append("\n");

        sb.append("multiClassUniqueLabelList" + "\n");
        for (double v : multiClassUniqueLabelList) {
            sb.append(v + " ");
        }
        sb.append("\n");

        for (int i = 1; i <= trees.size(); i++) {
            sb.append("tree[" + i + "]:\n");
            Tree tree = trees.get(i - 1);
            TreeNode root = tree.getRoot();
            Queue<TreeNode> queue = new LinkedList<>();
            queue.offer(root);
            while (!queue.isEmpty()) {
                int cur_level_num = queue.size();
                while (cur_level_num != 0) {
                    cur_level_num--;
                    TreeNode node = queue.poll();
                    if (node == null) {
                        continue;
                    }
                    if (node.isLeaf) {
                        sb.append(serializeLeafNode(node)).append("\n");
                    } else {
                        sb.append(serializeInternalNode(node)).append("\n");
                        queue.offer(node.leftChild);
                        queue.offer(node.rightChild);
                    }
                }
            }
        }
        sb.append("tree[end]");
        return sb.toString();
    }

    public static String getSeparator() {
        return separator;
    }

    public List<Tree> getTrees() {
        return trees;
    }

    public Loss getLoss() {
        return loss;
    }

    public double getFirstRoundPred() {
        return firstRoundPred;
    }

    public double getEta() {
        return eta;
    }

    public List<QueryEntry>  getPassiveQueryTable() {
        return passiveQueryTable;
    }

    public List<Double> getMultiClassUniqueLabelList() {
        return multiClassUniqueLabelList;
    }
}
