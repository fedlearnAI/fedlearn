package com.jdt.fedlearn.core.model.serialize;

import com.jdt.fedlearn.core.model.common.loss.LogisticLoss;
import com.jdt.fedlearn.core.model.common.loss.Loss;
import com.jdt.fedlearn.core.model.common.loss.SquareLoss;
import com.jdt.fedlearn.core.model.common.loss.crossEntropy;
import com.jdt.fedlearn.core.model.common.tree.MixTreeNode;
import com.jdt.fedlearn.core.parameter.MixGBParameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zhangwenxi
 */
public class MixGBSerializer implements ModelSerializer {
    private static final String SEPARATOR = ",";
    private static final String EQUAL_SIGN = "=";
    private static final String RECORD_ID = "recordId";
    private static final String DEPTH = "depth";
    private static final String FEATURE_TYPE = "splitFeatureType";
    private static final String FEATURE_NAME = "splitFeatureName";
    private static final String FEATURE_THRESHOLD = "splitThreshold";
    private static final String LEFT_RECORD_ID = "leftRecordId";
    private static final String RIGHT_RECORD_ID = "rightRecordId";
    private static final String PARENT_RECORD_ID = "parentRecordId";
    private static final String FIRST_PRED = "first_round_predict";
    private static final String MAP = "recordIdTreeNodeMap";
    private static final String LEAF = "leaf";

    private Loss loss;
    private MixGBParameter mixParams;
    private List<MixTreeNode> nodeList;
    private int[] recordId;

    public MixGBSerializer() {
    }

    public MixGBSerializer(Loss loss, MixGBParameter mixParam) {
        this.loss = loss;
        this.mixParams = mixParam;
    }

    public MixGBSerializer(String content) {
        Map<Integer, MixTreeNode> recordIdTreeNodeMap = loadModel(content);
        nodeList = new ArrayList<>(recordIdTreeNodeMap.values());
        recordId = recordIdTreeNodeMap.keySet().stream().mapToInt(Integer::intValue).toArray();
    }

    private static String serializeInternalNode(MixTreeNode node) {
        return RECORD_ID + EQUAL_SIGN + node.getRecordId() + SEPARATOR +
                DEPTH + EQUAL_SIGN + node.getDepth() + SEPARATOR +
                FEATURE_TYPE + EQUAL_SIGN + node.getSplitFeatureType() + SEPARATOR +
                FEATURE_NAME + EQUAL_SIGN + node.getSplitFeatureName() + SEPARATOR +
                FEATURE_THRESHOLD + EQUAL_SIGN + node.getSplitThreshold() + SEPARATOR +
                PARENT_RECORD_ID + EQUAL_SIGN + (node.getParent() == null ? "-1" : node.getParent().getRecordId()) + SEPARATOR +
                LEFT_RECORD_ID + EQUAL_SIGN + (node.getLeftChild() == null ? "-1" : node.getLeftChild().getRecordId()) + SEPARATOR +
                RIGHT_RECORD_ID + EQUAL_SIGN + (node.getRightChild() == null ? "-1" : node.getRightChild().getRecordId()) + "\n";
    }

    private static String serializeLeafNode(MixTreeNode node) {
        StringBuilder sb = new StringBuilder();
        sb.append(RECORD_ID).append(EQUAL_SIGN).append(node.getRecordId()).append(SEPARATOR);
        sb.append(DEPTH).append(EQUAL_SIGN).append(node.getDepth()).append(SEPARATOR);
        sb.append(String.format("leaf=%.16f", node.getLeafScore()));
        if (node.getDepth() > 1) {
            if (node.getParent() == null) {
                sb.append(SEPARATOR).append(PARENT_RECORD_ID).append(EQUAL_SIGN).append("-1");
            } else {
                sb.append(SEPARATOR).append(PARENT_RECORD_ID).append(EQUAL_SIGN).append(node.getParent().getRecordId());
            }
        }
        sb.append("\n");
        return sb.toString();
    }

    public int[] shareStartNodes(Map<Integer, MixTreeNode> recordIdTreeNodeMap) {
        if (recordIdTreeNodeMap == null || recordIdTreeNodeMap.isEmpty()) {
            return new int[0];
        }
        Map<Integer, MixTreeNode> localNodes = new HashMap<>(recordIdTreeNodeMap);
        return recordIdTreeNodeMap.entrySet().parallelStream()
                .filter(entry -> isStartNode(entry.getValue(), localNodes))
                .mapToInt(Map.Entry::getKey)
                .toArray();
    }

    private boolean isStartNode(MixTreeNode node, Map<Integer, MixTreeNode> recordIdTreeNodeMap) {
        /* leaf root */
        if (node.isLeaf() && node.getDepth() == 1) {
            return false;
        }
        return node.getParent() == null || !recordIdTreeNodeMap.containsKey(node.getParent().getRecordId());
    }

    public String saveMixGBModel(Map<Integer, MixTreeNode> recordIdTreeNodeMap) {
        StringBuilder sb = new StringBuilder();
        sb.append(FIRST_PRED).append(EQUAL_SIGN).append(0).append("\n");
        if (loss == null) {
            sb.append("\n");
        }
        if (loss instanceof LogisticLoss) {
            sb.append("logloss" + "\n");
        } else if (loss instanceof SquareLoss) {
            sb.append("squareloss" + "\n");
        } else if (loss instanceof crossEntropy) {
            sb.append("crossEntropy").append(SEPARATOR).append(mixParams.getNumClass()).append("\n");
        } else {
            sb.append("\n");
        }
        sb.append(MAP).append("\n");
        if (recordIdTreeNodeMap == null) {
            return sb.toString();
        }
        for (Map.Entry<Integer, MixTreeNode> entry : recordIdTreeNodeMap.entrySet()) {
            MixTreeNode node = entry.getValue();
            assert node != null;
            if (node.isLeaf()) {
                sb.append(serializeLeafNode(node));
            } else if (node.getRecordId() >= 0) {
                sb.append(serializeInternalNode(node));
            }
        }
        return sb.toString();
    }

    private void setLoss(String lossLine) {
        if ("logloss".equals(lossLine)) {
            this.loss = new LogisticLoss();
        } else if ("squareloss".equals(lossLine)) {
            this.loss = new SquareLoss();
        } else if (lossLine.startsWith("crossEntropy")) {
            String[] elements = lossLine.split(SEPARATOR);
            this.loss = new crossEntropy(Integer.parseInt(elements[1]));
        }
    }

    public Loss getLoss() {
        return loss;
    }

    private Map<Integer, MixTreeNode> loadModel(String content) {
        if (content.isEmpty()) {
            return new HashMap<>();
        }
        String[] lines = content.split("\n");
        Map<Integer, MixTreeNode> recordIdTreeNodeMap = new HashMap<>();
        int treeStarts = 0;

        if (lines[0].startsWith(FIRST_PRED)) {
            treeStarts = 2;
            setLoss(lines[1]);
        }

        MixTreeNode node;
        for (int i = treeStarts + 1; i < lines.length; i++) {
            if (lines[i].isEmpty()) {
                continue;
            }
            String[] elements = lines[i].split(SEPARATOR);
            /* elements[0] recordId */
            int nodeId = Integer.parseInt(elements[0].split(EQUAL_SIGN)[1]);
            int depth = Integer.parseInt(elements[1].split(EQUAL_SIGN)[1]);
            /* construct node by recordId & depth, put it into recordIdTreeNodeMap*/
            node = recordIdTreeNodeMap.computeIfAbsent(nodeId, key -> new MixTreeNode(depth, nodeId));
            if (elements[2].startsWith(LEAF)) {
                /* leaf-root */
                node.setAsLeaf(Double.parseDouble(elements[2].split(EQUAL_SIGN)[1]));
                /*
                   regular leaf, let parent finds it
                   (parent recordId could be -1 when the client saves leave nodes from other clients)
                */
                if (depth > 1 && Integer.parseInt(elements[3].split(EQUAL_SIGN)[1]) > 0) {
                    int parentId = Integer.parseInt(elements[3].split(EQUAL_SIGN)[1]);
                    MixTreeNode parent = noInsertIfAbsent(recordIdTreeNodeMap, parentId, depth - 1);
                    node.setParent(parent);
                }
            } else {
                /* internal node */
                node.setSplitFeatureType(Integer.parseInt(elements[2].split(EQUAL_SIGN)[1]));
                node.setSplitFeatureName(elements[3].split(EQUAL_SIGN)[1]);
                node.setSplitThreshold(Double.parseDouble(elements[4].split(EQUAL_SIGN)[1]));
                setNodeParentChildren(recordIdTreeNodeMap, node,
                        elements[5].split(EQUAL_SIGN)[1],
                        elements[6].split(EQUAL_SIGN)[1],
                        elements[7].split(EQUAL_SIGN)[1]);
            }
        }
        setFinalParentChildren(recordIdTreeNodeMap);
        return recordIdTreeNodeMap;
    }

    private MixTreeNode noInsertIfAbsent(Map<Integer, MixTreeNode> recordIdTreeNodeMap, int recordID, int depth) {
        if (recordID < 0) {
            return null;
        }
        if (recordIdTreeNodeMap != null && recordIdTreeNodeMap.containsKey(recordID)) {
            return recordIdTreeNodeMap.get(recordID);
        }
        return new MixTreeNode(depth, recordID);
    }

    private void setNodeParentChildren(Map<Integer, MixTreeNode> recordIdTreeNodeMap, MixTreeNode node, String parentStr, String leftStr, String rightStr) {
        int parentRecordId = Integer.parseInt(parentStr);
        int leftRecordId = Integer.parseInt(leftStr);
        int rightRecordId = Integer.parseInt(rightStr);

        int depth = node.getDepth();
        MixTreeNode parent = noInsertIfAbsent(recordIdTreeNodeMap, parentRecordId, depth - 1);
        node.setParent(parent);
        MixTreeNode left = noInsertIfAbsent(recordIdTreeNodeMap, leftRecordId, depth + 1);
        node.setLeftChild(left);
        MixTreeNode right = noInsertIfAbsent(recordIdTreeNodeMap, rightRecordId, depth + 1);
        node.setRightChild(right);
    }

    private void setFinalParentChildren(Map<Integer, MixTreeNode> recordIdTreeNodeMap) {
        recordIdTreeNodeMap.forEach((key, value) -> {
            if (value.getParent() != null &&
                    recordIdTreeNodeMap.containsKey(value.getParent().getRecordId())) {
                value.setParent(recordIdTreeNodeMap.get(value.getParent().getRecordId()));
            }
            if (value.getLeftChild() != null
                    && recordIdTreeNodeMap.containsKey(value.getLeftChild().getRecordId())) {
                value.setLeftChild(recordIdTreeNodeMap.get(value.getLeftChild().getRecordId()));
            }
            if (value.getRightChild() != null
                    && recordIdTreeNodeMap.containsKey(value.getRightChild().getRecordId())) {
                value.setRightChild(recordIdTreeNodeMap.get(value.getRightChild().getRecordId()));
            }
        });
    }

    public List<MixTreeNode> getNodeList() {
        return nodeList;
    }

    public int[] getRecordId() {
        return recordId;
    }
}
