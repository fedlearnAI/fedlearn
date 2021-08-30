package com.jdt.fedlearn.core.model.serialize;

import com.jdt.fedlearn.core.model.common.loss.LogisticLoss;
import com.jdt.fedlearn.core.model.common.loss.Loss;
import com.jdt.fedlearn.core.model.common.loss.SquareLoss;
import com.jdt.fedlearn.core.model.common.tree.MixTreeNode;
import com.jdt.fedlearn.core.parameter.MixGBParameter;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author zhangwenxi
 */
public class MixGBSerializerTest {
    private MixGBSerializer mixGBSerializer;
    private Map<Integer, MixTreeNode> curRecordIdTreeNodeMap;
    private String[] lossModelContent;

    @BeforeMethod
    public void setUp() {
        mixGBSerializer = new MixGBSerializer();
        setUpTreeNodeMap();
        setUpContent();
    }

    private void setUpTreeNodeMap() {
        curRecordIdTreeNodeMap = new HashMap<>();
        MixTreeNode node1 = new MixTreeNode(1, 1);
        MixTreeNode node2 = new MixTreeNode(2, 2);
        MixTreeNode node3 = new MixTreeNode(2, 3);
        MixTreeNode node4 = new MixTreeNode(3, 4);
        MixTreeNode node5 = new MixTreeNode(3, 5);
        MixTreeNode node6 = new MixTreeNode(4, 6);
        MixTreeNode node7 = new MixTreeNode(4, 7);
        MixTreeNode node8 = new MixTreeNode(4, 8);
        MixTreeNode node9 = new MixTreeNode(4, 9);
        MixTreeNode node10 = new MixTreeNode(1, 10);
        node3.setAsLeaf(3.0);
        node6.setAsLeaf(6.0);
        node7.setAsLeaf(7.0);
        node8.setAsLeaf(8.0);
        node9.setAsLeaf(9.0);
        node10.setAsLeaf(10.0);
        node6.setParent(new MixTreeNode(3, 4));
//        node7.setParent(new MixTreeNode(3, 4));
//        node8.setParent(new MixTreeNode(3, 5));
        node9.setParent(new MixTreeNode(3, 5));
        node2.setParent(node1);
        node2.setLeftChild(new MixTreeNode(3,4));
        node2.setRightChild(new MixTreeNode(3,5));
        node3.setParent(node1);

        curRecordIdTreeNodeMap.put(1, node1);
        curRecordIdTreeNodeMap.put(2, node2);
        curRecordIdTreeNodeMap.put(3, node3);
        curRecordIdTreeNodeMap.put(6, node6);
        curRecordIdTreeNodeMap.put(7, node7);
        curRecordIdTreeNodeMap.put(8, node8);
        curRecordIdTreeNodeMap.put(9, node9);
    }

    private void setUpContent() {
        lossModelContent = new String[5];
        lossModelContent[0] = "first_round_predict=0\n" +
                "squareloss\n" +
                "recordIdTreeNodeMap\n" +
                "recordId=1,depth=1,splitFeatureType=-1,splitFeatureName=null,splitThreshold=0.0,parentRecordId=-1,leftRecordId=-1,rightRecordId=-1\n" +
                "recordId=2,depth=2,splitFeatureType=-1,splitFeatureName=null,splitThreshold=0.0,parentRecordId=1,leftRecordId=4,rightRecordId=5\n" +
                "recordId=3,depth=2,leaf=3.0000000000000000,parentRecordId=1\n" +
                "recordId=6,depth=4,leaf=6.0000000000000000,parentRecordId=4\n" +
                "recordId=7,depth=4,leaf=7.0000000000000000,parentRecordId=-1\n" +
                "recordId=8,depth=4,leaf=8.0000000000000000,parentRecordId=-1\n" +
                "recordId=9,depth=4,leaf=9.0000000000000000,parentRecordId=5\n";
        lossModelContent[1] = "first_round_predict=0\n" +
                "logloss\n" +
                "recordIdTreeNodeMap\n" +
                "recordId=1,depth=1,splitFeatureType=-1,splitFeatureName=null,splitThreshold=0.0,parentRecordId=-1,leftRecordId=-1,rightRecordId=-1\n" +
                "recordId=2,depth=2,splitFeatureType=-1,splitFeatureName=null,splitThreshold=0.0,parentRecordId=1,leftRecordId=4,rightRecordId=5\n" +
                "recordId=3,depth=2,leaf=3.0000000000000000,parentRecordId=1\n" +
                "recordId=6,depth=4,leaf=6.0000000000000000,parentRecordId=4\n" +
                "recordId=7,depth=4,leaf=7.0000000000000000,parentRecordId=-1\n" +
                "recordId=8,depth=4,leaf=8.0000000000000000,parentRecordId=-1\n" +
                "recordId=9,depth=4,leaf=9.0000000000000000,parentRecordId=5\n";
        lossModelContent[2] = "first_round_predict=0\n" +
                "squareloss\n" +
                "recordIdTreeNodeMap\n" +
                "recordId=1,depth=1,splitFeatureType=-1,splitFeatureName=null,splitThreshold=0.0,parentRecordId=-1,leftRecordId=-1,rightRecordId=-1\n" +
                "recordId=2,depth=2,splitFeatureType=-1,splitFeatureName=null,splitThreshold=0.0,parentRecordId=1,leftRecordId=4,rightRecordId=5\n" +
                "recordId=3,depth=2,leaf=3.0000000000000000,parentRecordId=1\n" +
                "recordId=6,depth=4,leaf=6.0000000000000000,parentRecordId=4\n" +
                "recordId=7,depth=4,leaf=7.0000000000000000,parentRecordId=-1\n" +
                "recordId=8,depth=4,leaf=8.0000000000000000,parentRecordId=-1\n" +
                "recordId=9,depth=4,leaf=9.0000000000000000,parentRecordId=5\n";
        lossModelContent[3] = "first_round_predict=0\n" +
                "\n" +
                "\n" +
                "recordIdTreeNodeMap\n" +
                "recordId=1,depth=1,splitFeatureType=-1,splitFeatureName=null,splitThreshold=0.0,parentRecordId=-1,leftRecordId=-1,rightRecordId=-1\n" +
                "recordId=2,depth=2,splitFeatureType=-1,splitFeatureName=null,splitThreshold=0.0,parentRecordId=1,leftRecordId=4,rightRecordId=5\n" +
                "recordId=3,depth=2,leaf=3.0000000000000000,parentRecordId=1\n" +
                "recordId=6,depth=4,leaf=6.0000000000000000,parentRecordId=4\n" +
                "recordId=7,depth=4,leaf=7.0000000000000000,parentRecordId=-1\n" +
                "recordId=8,depth=4,leaf=8.0000000000000000,parentRecordId=-1\n" +
                "recordId=9,depth=4,leaf=9.0000000000000000,parentRecordId=5\n";
        lossModelContent[4] = "first_round_predict=0\n" +
                "\n" +
                "\n" +
                "recordIdTreeNodeMap\n";
    }

    @Test
    public void testShareStartNodes() {
        int[] startNodes = mixGBSerializer.shareStartNodes(curRecordIdTreeNodeMap);
        int[] correctStartNodes = new int[]{1, 6, 7, 8, 9};
        Assert.assertEquals(startNodes, correctStartNodes);
    }

    @Test
    public void testSaveMixGBModel() {
        Loss[] lossArray = new Loss[]{new SquareLoss(), new LogisticLoss(), new SquareLoss(), null};
        MixGBParameter mixParams = new MixGBParameter();
        for (int i = 0; i < lossArray.length; i++) {
            mixGBSerializer = new MixGBSerializer(lossArray[i], mixParams);
            String content = mixGBSerializer.saveMixGBModel(curRecordIdTreeNodeMap);
            Assert.assertEquals(content, lossModelContent[i]);
        }
        String content = mixGBSerializer.saveMixGBModel(null);
        Assert.assertEquals(content, lossModelContent[4]);
    }

    @Test
    public void testLoadModel() {
//        MixGBSerializer mixGBSerializer = new MixGBSerializer(content);

    }

    @Test
    public void testGetLoss() {
        Loss loss = mixGBSerializer.getLoss();
        Assert.assertNull(loss);

    }

    @Test
    public void testGetNodeList() {
        List<MixTreeNode> treeNodes = mixGBSerializer.getNodeList();
        Assert.assertNull(treeNodes);
        mixGBSerializer = new MixGBSerializer(lossModelContent[0]);
        treeNodes = mixGBSerializer.getNodeList();
        Assert.assertEquals(treeNodes.size(), curRecordIdTreeNodeMap.size());
        List<Integer> nodeIds = treeNodes.parallelStream().map(MixTreeNode::getRecordId).collect(Collectors.toList());
        Assert.assertTrue(nodeIds.containsAll(curRecordIdTreeNodeMap.keySet()));
    }

    @Test
    public void testGetRecordId() {
        int[] recordIds = mixGBSerializer.getRecordId();
        Assert.assertNull(recordIds);
        mixGBSerializer = new MixGBSerializer(lossModelContent[0]);
        recordIds = mixGBSerializer.getRecordId();
        Assert.assertEquals(recordIds.length, curRecordIdTreeNodeMap.size());
        List<Integer> nodeIds = Arrays.stream(recordIds).boxed().collect(Collectors.toList());
        Assert.assertTrue(nodeIds.containsAll(curRecordIdTreeNodeMap.keySet()));
    }
}