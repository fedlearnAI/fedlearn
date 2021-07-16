package com.jdt.fedlearn.core.optimizer;

import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;

import static org.testng.Assert.assertEquals;

public class BatchGDTest {
    BatchGD batchGD1 = new BatchGD();
    BatchGD batchGD2 = new BatchGD(0.1);
    double[][] gredients = {{0.3,0.5,0.12},{1.0,0.5,0.3}};
    double[] gredients1 = {0.3,0.5,0.12};
    @Test
    public void testGetGlobalUpdate() {
        //double[][] res = batchGD1.getGlobalUpdate(gredients);
        double[][] res = batchGD2.getGlobalUpdate(gredients);
        double[][] target = {{-0.03,-0.05,-0.012},{-0.1,-0.05,-0.03}};
        for(int i = 0;i<res.length;i++) {
            assertEquals(res[i], target[i]);
        }
    }

    @Test
    public void testGetGlobalUpdate1() {
        double[] res = batchGD2.getGlobalUpdate(gredients1);
        double[] target = {-0.03,-0.05,-0.012};
        for(int i = 0;i<res.length;i++) {
            assertEquals(res[i], target[i]);
        }
    }

    @Test
    public void testSetLearning_rate() {
        batchGD2.setLearning_rate(0.5);
        double res = batchGD2.getLearning_rate();
        assertEquals(res,0.5);
    }

    @Test
    public void testGetLearning_rate() {
        double res = batchGD1.getLearning_rate();
        assertEquals(res,0.0);
        //默认为0
    }

    @Test
    public void testRandomChoose() {
        Collection<Long>  collection = new ArrayList<>();
        // 添加元素
        collection.add(12232L);
        collection.add(65752L);
        Collection<Long> target = batchGD2.randomChoose(collection);
        System.out.println(target);
        //函数无效
    }
}