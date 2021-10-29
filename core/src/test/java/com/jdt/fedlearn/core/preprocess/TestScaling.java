package com.jdt.fedlearn.core.preprocess;

import com.jdt.fedlearn.core.util.Tool;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestScaling {
    double[][] table = {{1.1,2,3}, {2,2,0.3}, {5,2,2}};
    Scaling scaling = new Scaling(table);
    @Test
    public void testMinMaxScaling() {
        scaling.minMaxScaling(0,1);
//        for (double[] res1 :table) {
//            for (double res2 :res1)
//                System.out.println(res2);
//        }
        double[][] target = {{0,0.0,1.0}, {0.23076923076923077,0.0,0.0}, {1.0,0.0,0.6296296296296296}};
        for (int i =0;i<table.length;i++) {
            Assert.assertTrue(Tool.approximate(table[i],target[i]));
//            assertEquals(table[i],target[i]);
        }
    }
    //程序输出：{{0.0,0.0,1.0}, {0.23076923076923078,0.0,0.0}, {1.0,0.0,0.6296296296296295}}
    //如不考虑精度，则完全正确。

}