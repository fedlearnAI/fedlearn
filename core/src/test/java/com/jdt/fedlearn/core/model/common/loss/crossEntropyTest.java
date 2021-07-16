package com.jdt.fedlearn.core.model.common.loss;

import com.jdt.fedlearn.core.util.Tool;
import org.testng.Assert;
import org.testng.annotations.Test;

public class crossEntropyTest {
    private final int num = 3;
    private final crossEntropy crossEntropy1 = new crossEntropy(num);

    @Test
    public void testSetNumClass() {
        crossEntropy1.setNumClass(3);
    }

    @Test
    public void testTransform() {
        double[] pred = {0.1, 0.7,   0.6, 0.2,   0.3, 0.1};
        double[] target = {0.3333333333333333,0.3333333333333333,0.3333333333333333,0.3333333333333333,0.3333333333333333,0.3333333333333333};
        double[] res = crossEntropy1.transform(pred);
        for (int i = 0; i < target.length; i++) {
            //System.out.println(res[i]);
            Assert.assertTrue(Tool.approximate(res[i], target[i]));
        }
    }

    @Test
    public void testPostTransform() {
        double[] pred = {0.1, 0.7,   0.6, 0.2,   0.3, 0.1};
        double[] target = {0.3333333333333333,0.3333333333333333,0.3333333333333333,0.3333333333333333,0.3333333333333333,0.3333333333333333};
        double[] res = crossEntropy1.postTransform(pred);
        for (int i = 0; i < res.length; i++) {
            //System.out.println(res[i]);
            Assert.assertTrue(Tool.approximate(res[i], target[i]));
        }
    }

    @Test
    public void testGrad() {
        double[] pred = {0.17142857142857149,-0.07894736842105265,0.17142857142857149,0.17142857142857149,
                -0.08838606351474927,0.13851978069710405,-0.08838606351474927,-0.08838606351474927,
                -0.10147564097937667,-0.10147564097937667,-0.10147564097937667,-0.10147564097937667};
        double[] label = {0,1,0,2};
        double[] target ={-0.6452553157332893,0.32681232307705904,-0.6452553157332893,0.3547446842667107,
                0.32473915757849353,-0.6463444421775,0.32473915757849353,0.32473915757849353,
                0.3205161581547957,0.31953211910044094,0.3205161581547957,-0.6794838418452043};
        double[] res = crossEntropy1.grad(pred, label);
        for (int i = 0; i < res.length; i++) {
            //System.out.println(res[i]);
            Assert.assertTrue(Tool.approximate(res[i], target[i]));
        }
    }

    @Test
    public void testHess() {
        double[] pred = {0.17142857142857149,-0.07894736842105265,0.17142857142857149,0.17142857142857149,
                -0.08838606351474927,0.13851978069710405,-0.08838606351474927,-0.08838606351474927,
                -0.10147564097937667,-0.10147564097937667,-0.10147564097937667,-0.10147564097937667};
        double[] label = {0,1,0,2};
        double[] target = {0.22890089325122243,0.22000602856203505,0.22890089325122243,0.22890089325122243,
                0.21928363711370388,0.22858330424375636,0.21928363711370388,0.21928363711370388,
                0.2177855505164857,0.21743134396362254,0.2177855505164857,0.2177855505164857};
        double[] res = crossEntropy1.hess(pred, label);
        for (int i = 0; i < res.length; i++) {
            //System.out.println(res[i]);
            Assert.assertTrue(Tool.approximate(res[i], target[i]));
        }
    }
}