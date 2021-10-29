package com.jdt.fedlearn.core.entity.randomForest;

import com.google.common.collect.Maps;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import static org.testng.Assert.assertEquals;

public class TypeRandomForestTest {
    @Test
    public void testTypeRandomForest() {
        TypeRandomForest typeRandomForest;

        new TypeRandomForest(2, 3, 5, new HashMap<>());
        typeRandomForest = new TypeRandomForest(2, 5, 5, new Random(666));
        assertEquals(typeRandomForest.getActiveNode(new TreeNodeRF(), 0).getNumSamples(), 5);
        assertEquals(typeRandomForest.getTrainNodeAllTrees().size(), 1);
        assertEquals(typeRandomForest.tree2json(new TreeNodeRF(), ""), "{}");
        typeRandomForest.releaseTreeNodeAllTrees();
        typeRandomForest = new TypeRandomForest();

       typeRandomForest.setRoots(new ArrayList<>());
       typeRandomForest.setTreeNodeMap(Maps.newLinkedHashMap());
       typeRandomForest.setIsFinished(new ArrayList<>());
       typeRandomForest.setMaxDepth(0);
       typeRandomForest.setNumPercentiles(0);
       typeRandomForest.setNumTrees(0);
       typeRandomForest.setNumNodesAll(0);
       typeRandomForest.setNode_count(null);
       typeRandomForest.setAliveNodes(new ArrayList<>());

        assertEquals(typeRandomForest.getRoots(), new ArrayList<>());
        assertEquals(typeRandomForest.getTreeNodeMap(), Maps.newLinkedHashMap());
        assertEquals(typeRandomForest.getIsFinished(), new ArrayList<>());
        assertEquals(typeRandomForest.getMaxDepth(), 0);
        assertEquals(typeRandomForest.getNumPercentiles(), 0);
        assertEquals(typeRandomForest.getNumTrees(), 0);
        assertEquals(typeRandomForest.getNumNodesAll(), 0);
        assertEquals(typeRandomForest.getNode_count(), null);
        assertEquals(typeRandomForest.getAliveNodes(), new ArrayList<>());



    }
}