package com.jdt.fedlearn.core.model.common.loss;

import org.testng.annotations.Test;

import java.util.Arrays;

import static org.testng.Assert.*;

public class TestLogisticLoss {


    @Test
    public void testTransform() {
        double[] pre = new double[]{0,1,0,1,1};
        LogisticLoss logisticLoss = new LogisticLoss();
        double[] res = logisticLoss.transform(pre);
        System.out.println("trans: " + Arrays.toString(res));
        double[] target = new double[]{0.5, 0.7310585786300049, 0.5, 0.7310585786300049, 0.7310585786300049};
        for (int i = 0; i < res.length; i++) {
            assertEquals(target[i],res[i]);
        }
    }

    @Test
    public void testLogTransform() {
        double[] pre = new double[]{1.2, 2.3, 4.2, 2.6};
        LogisticLoss logisticLoss = new LogisticLoss();
        double[] res = logisticLoss.logTransform(pre);
        System.out.println("res:" + Arrays.toString(res));
        double[] target = new double[]{0.1823215567939546, 0.8329091229351039, 1.4350845252893227, 0.9555114450274363};
        for (int i = 0; i < res.length; i++) {
            assertEquals(target[i],res[i]);
        }
    }

    @Test
    public void testExpTransform() {
        double[] pre = new double[]{1.2, 2.3, 4.2, 2.6};
        LogisticLoss logisticLoss = new LogisticLoss();
        double[] res = logisticLoss.expTransform(pre);
        System.out.println("res: " + Arrays.toString(res));
        double[] target = new double[]{3.3201169227365472, 9.974182454814718, 66.68633104092515, 13.463738035001692};
        for (int i = 0; i < res.length; i++) {
            assertEquals(target[i],res[i]);
        }
    }

    @Test
    public void testGrad() {
        double[] pre = new double[]{1.2, 2.3, 4.2, 2.6};
        double[] label = new double[]{1.3, 2.2, 4.2, 2.5};
        LogisticLoss logisticLoss = new LogisticLoss();
        double[] res = logisticLoss.grad(pre, label);
        System.out.println("grad: " + Arrays.toString(res));
        double[] target = new double[]{-0.5314752165009825, -1.2911229610148562, -3.214774031693273, -1.5691384203433467};
        for (int i = 0; i < res.length; i++) {
            assertEquals(target[i],res[i]);
        }
    }

    @Test
    public void testHess() {
        double[] pre = new double[]{1.2, 2.3, 4.2, 2.6};
        double[] label = new double[]{1.3, 2.2, 4.2, 2.5};
        LogisticLoss logisticLoss = new LogisticLoss();
        double[] res = logisticLoss.hess(pre, label);
        System.out.println("hess: " + Arrays.toString(res));
        double[] target = new double[]{0.17789444064680576, 0.08281956699074118, 0.014555759680799234, 0.06435829917577342};
        for (int i = 0; i < res.length; i++) {
            assertEquals(target[i],res[i]);
        }
    }

    @Test
    public void testGainScoreDelta(){
        LogisticLoss logisticLoss = new LogisticLoss();
        double gainDelta = logisticLoss.getGainDelta(10, 1.0);
        double scoreDelta = logisticLoss.getLeafScoreDelta(10, 1.0);
        assertEquals(gainDelta, 10 / 2.0);
        assertEquals(scoreDelta, 1.0);
        gainDelta = logisticLoss.getGainDelta(0, 0);
        assertEquals(gainDelta, 0);
        scoreDelta = logisticLoss.getLeafScoreDelta(0, 0);
    }
}