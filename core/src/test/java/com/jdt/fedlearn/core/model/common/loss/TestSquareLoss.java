package com.jdt.fedlearn.core.model.common.loss;

import org.testng.annotations.Test;

import java.util.Arrays;

import static org.testng.Assert.*;

public class TestSquareLoss {

    @Test
    public void testTransform() {
        double[] pred = new double[]{1.2, 1.5, 2.2, 3.0};
        SquareLoss squareLoss = new SquareLoss();
        double[] res = squareLoss.transform(pred);
        System.out.println("res: " + Arrays.toString(res));
        double[] target = new double[]{1.2, 1.5, 2.2, 3.0};
        for (int i = 0; i < pred.length; i++) {
            assertEquals(target[i], res[i]);
        }
    }

    @Test
    public void testGetLoss() {
        double[] pred = new double[]{1, 2, 3, 4};
        double[] label = new double[]{2, 2, 1, 3};
        SquareLoss squareLoss = new SquareLoss();
        double res = squareLoss.getLoss(pred, label);
        System.out.println("res: " + res);
        double target = 0.75;
        assertEquals(target, res);
    }

    @Test
    public void testLogTransform() {
        double[] pred = new double[]{1, 2, -1, -3};
        SquareLoss squareLoss = new SquareLoss();
        double[] res = squareLoss.logTransform(pred);
        System.out.println("res:" + Arrays.toString(res));
        double[] target = new double[]{0.0, 0.6931471805599453, 0.0, 0.0};
        for (int i = 0; i < pred.length; i++) {
            assertEquals(target[i], res[i]);
        }
    }

    @Test
    public void testExpTransform() {
        double[] pred = new double[]{1, 2, 3, 4};
        SquareLoss squareLoss = new SquareLoss();
        double[] res = squareLoss.expTransform(pred);
        System.out.println("res: " + Arrays.toString(res));
        double[] target = new double[]{2.718281828459045, 7.38905609893065, 20.085536923187668, 54.598150033144236};
        for (int i = 0; i < pred.length; i++) {
            assertEquals(target[i], res[i]);
        }
    }

    @Test
    public void testGrad() {
        double[] pred = new double[]{1, 2, 3, 4};
        double[] label = new double[]{1.1, 1.8, 3.1, 4};
        SquareLoss squareLoss = new SquareLoss();
        double[] res = squareLoss.grad(pred, label);
        System.out.println("res: " + Arrays.toString(res));
        double[] target = new double[]{-0.1, 0.2, -0.1, 0};
        for (int i = 0; i < pred.length; i++) {
            assertEquals(res[i], target[i], 1e-8);
        }
    }

    @Test
    public void testHess() {
        double[] pred = new double[]{1, 2, 3, 4};
        double[] label = new double[]{1.1, 1.8, 3.1, 4};
        SquareLoss squareLoss = new SquareLoss();
        double[] res = squareLoss.hess(pred, label);
        System.out.println("res: " + Arrays.toString(res));
        double[] target = new double[]{1, 1, 1, 1};
        for (int i = 0; i < pred.length; i++) {
            assertEquals(target[i], res[i]);
        }
    }
}