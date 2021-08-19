package com.jdt.fedlearn.core.entity.randomForest;

import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;

public class TreeNodeRFTest {
    @Test
    public void testNewTreeNodeRF(){
        List<Integer> sampleIds = new ArrayList<>();
        sampleIds.add(1);

        new TreeNodeRF();
        TreeNodeRF treeNodeRF1 = new TreeNodeRF(sampleIds, 0, 1);
        TreeNodeRF treeNodeRF2 = new TreeNodeRF(sampleIds, 1, 1);
        TreeNodeRF treeNodeRF3 = new TreeNodeRF(sampleIds, 2, 1);
        treeNodeRF1.left = treeNodeRF2;
        treeNodeRF2.right = treeNodeRF3;
        assertEquals(treeNodeRF2.level(), 1);
        assertEquals(treeNodeRF1.numTreeNodes(), 3);
        treeNodeRF1.referenceJsonStr = "{\"is_leaf\":1,\"prediction\":null}";
        treeNodeRF1.makeLeaf("");
        assertTrue(treeNodeRF1.isLeaf);
        assertNull(treeNodeRF1.getScore());
    }


}