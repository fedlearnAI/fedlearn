package com.jdt.fedlearn.core.model.serialize;

import com.jdt.fedlearn.core.model.MixGBModel;
import com.jdt.fedlearn.core.model.common.loss.LogisticLoss;
import com.jdt.fedlearn.core.model.common.loss.Loss;
import com.jdt.fedlearn.core.model.common.loss.SquareLoss;
import com.jdt.fedlearn.core.model.common.loss.crossEntropy;
import com.jdt.fedlearn.core.model.common.tree.MixTreeNode;
import com.jdt.fedlearn.core.parameter.MixGBParameter;

import java.util.Map;

public class MixGBSerializer {
    private static final String SEPARATOR = ",";
    private Loss loss;
    private final MixGBParameter mixParams;
    private final Map<Integer, MixTreeNode> finalRecordIdTreeNodeMap;

    public MixGBSerializer(Loss loss, MixGBParameter mixParam, Map<Integer, MixTreeNode> finalRecordIdTreeNodeMap) {
        this.loss = loss;
        this.mixParams = mixParam;
        this.finalRecordIdTreeNodeMap = finalRecordIdTreeNodeMap;
    }

    private static String serializeInternalNode(MixTreeNode node) {
        return "recordId=" + node.getRecordId() + SEPARATOR +
                "depth=" + node.getDepth() + SEPARATOR +
                "splitFeatureType=" + node.getSplitFeatureType() + SEPARATOR +
                "splitFeatureName=" + node.getSplitFeatureName() + SEPARATOR +
                "splitThreshold=" + node.getSplitThreshold() + SEPARATOR +
                "parentRecordId=" + (node.getParent() == null ? "-1" : node.getParent().getRecordId()) + SEPARATOR +
                "leftRecordId=" + (node.getLeftChild() == null ? "-1" : node.getLeftChild().getRecordId()) + SEPARATOR +
                "rightRecordId=" + (node.getRightChild() == null ? "-1" : node.getRightChild().getRecordId()) + "\n";
    }

    private static String serializeLeafNode(MixTreeNode node) {
        StringBuilder sb = new StringBuilder();
        sb.append("recordId=").append(node.getRecordId()).append(SEPARATOR);
        sb.append("depth=").append(node.getDepth()).append(SEPARATOR);
        sb.append(String.format("leaf=%.16f", node.getLeafScore()));
        if (node.getDepth() > 1) {
            if (node.getParent() == null) {
                sb.append(SEPARATOR + "parentRecordId=-1");
            } else {
                sb.append(SEPARATOR + "parentRecordId=" + node.getParent().getRecordId());
            }
        }
        sb.append("\n");
        return sb.toString();
    }

    public String saveMixGBModel() {
        StringBuilder sb = new StringBuilder();
        sb.append("first_round_predict=").append(0).append("\n");
        if (loss instanceof LogisticLoss) {
            sb.append("logloss" + "\n");
        } else if (loss instanceof SquareLoss) {
            sb.append("squareloss" + "\n");
        } else if (loss instanceof crossEntropy) {
            sb.append("crossEntropy").append(SEPARATOR).append(mixParams.getNumClass()).append("\n");
        } else {
            sb.append("\n");
        }
        sb.append("finalRecordIdTreeNodeMap\n");
        if (finalRecordIdTreeNodeMap == null || finalRecordIdTreeNodeMap.isEmpty()) {
            return sb.toString();
        }
        for (Map.Entry<Integer, MixTreeNode> entry : finalRecordIdTreeNodeMap.entrySet()) {
            MixTreeNode node = entry.getValue();
            if (node == null) {
                continue;
            }
            if (node.isLeaf()) {
                sb.append(serializeLeafNode(node));
                continue;
            }
            if (node.getRecordId() >= 0) {
                sb.append(serializeInternalNode(node));
            }
        }
        return sb.toString();
    }

    public MixGBModel loadMixGBModel(String content) {
        StringBuilder modelContent = new StringBuilder();
        //finalRecordIdTreeNodeMap
        String[] lines = content.split("\n");
        int treeStarts = 0;

        if (lines[0].startsWith("first_round_predict")) {
            modelContent.append(lines[0]).append("\n");
            modelContent.append(lines[1]).append("\n");
            treeStarts = 2;
            if ("logloss".equals(lines[1])) {
                this.loss = new LogisticLoss();
            } else if ("squareloss".equals(lines[1])) {
                this.loss = new SquareLoss();
            } else if (lines[1].startsWith("crossEntropy")) {
                String[] elements = lines[1].split(SEPARATOR);
                this.loss = new crossEntropy(Integer.parseInt(elements[1]));
            }
        }
        modelContent.append(lines[treeStarts]).append("\n");
        MixTreeNode node;
        for (int i = treeStarts + 1; i < lines.length; i++) {
            if (lines[i].isEmpty()) {
                continue;
            }
            String[] elements = lines[i].split(SEPARATOR);
            // elements[0] recordId
            int recordId = Integer.parseInt(elements[0].split("=")[1]);
            int depth = Integer.parseInt(elements[1].split("=")[1]);
            // construct node by recordId & depth
            if (!finalRecordIdTreeNodeMap.containsKey(recordId)) {
                node = new MixTreeNode(depth, recordId);
                finalRecordIdTreeNodeMap.put(recordId, node);
            }
            node = finalRecordIdTreeNodeMap.get(recordId);
            if (elements[2].startsWith("leaf")) {
                node.setAsLeaf(Double.parseDouble(elements[2].split("=")[1]));
                // leaf-root
                if (depth <= 1) {
                    continue;
                }
                // regular leaf, let parent finds it
                // (parent recordId could be -1 when the client saves leave nodes from other clients)
                int parentRecordId = Integer.parseInt(elements[3].split("=")[1]);
                if (parentRecordId <= 0) {
                    continue;
                }
                node.setParent(new MixTreeNode(depth - 1, parentRecordId));
                continue;
            }
            // internal node
            node.setSplitFeatureType(Integer.parseInt(elements[2].split("=")[1]));
            node.setSplitFeatureName(elements[3].split("=")[1]);
            node.setSplitThreshold(Double.parseDouble(elements[4].split("=")[1]));
            int parentRecordId = Integer.parseInt(elements[5].split("=")[1]);
            if (parentRecordId > 0) {
                node.setParent(new MixTreeNode(depth - 1, parentRecordId));
            }
            int leftRecordId = Integer.parseInt(elements[6].split("=")[1]);
            if (leftRecordId > 0) {
                node.setLeftChild(new MixTreeNode(depth + 1, leftRecordId));
            }
            int rightRecordId = Integer.parseInt(elements[7].split("=")[1]);
            if (rightRecordId > 0) {
                node.setRightChild(new MixTreeNode(depth + 1, rightRecordId));
            }
        }
        for (Map.Entry<Integer, MixTreeNode> item: finalRecordIdTreeNodeMap.entrySet()) {
            if (item.getValue().getParent() != null && finalRecordIdTreeNodeMap.containsKey(item.getValue().getParent().getRecordId())) {
                item.getValue().setParent(finalRecordIdTreeNodeMap.get(item.getValue().getParent().getRecordId()));
            } else {
                item.getValue().setParent(null);
            }
            if (item.getValue().getLeftChild() != null && finalRecordIdTreeNodeMap.containsKey(item.getValue().getLeftChild().getRecordId())) {
                item.getValue().setLeftChild(finalRecordIdTreeNodeMap.get(item.getValue().getLeftChild().getRecordId()));
            } else {
                item.getValue().setLeftChild(null);
            }
            if (item.getValue().getRightChild() != null && finalRecordIdTreeNodeMap.containsKey(item.getValue().getRightChild().getRecordId())) {
                item.getValue().setRightChild(finalRecordIdTreeNodeMap.get(item.getValue().getRightChild().getRecordId()));
            } else {
                item.getValue().setRightChild(null);
            }
        }
        return new MixGBModel(content, loss, finalRecordIdTreeNodeMap);
    }
}
