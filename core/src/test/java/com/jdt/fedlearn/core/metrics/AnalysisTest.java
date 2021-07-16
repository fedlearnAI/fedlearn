package com.jdt.fedlearn.core.metrics;


import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @Classname AnalysisTest
 * @Description TODO
 * @Date 2020/11/10 下午5:13
 * @Created by zhangwenxi3
 */
public class AnalysisTest {

    @Test
    public void testWoe2() {
        System.out.println("Test woe");
        double[] arrayY    = new double[]{1, 0, 0, 1, 1, 0, 1};
        double[] feaValues = new double[]{0, 0, 1, 1, 0, 5, 5};
        int numBins = 6;
        double[] res = Analysis.woe2(arrayY, feaValues, numBins);
        double[] target = {-0.405465, 0.287682, 0.0, 0.0, 0.0, 0.287682};
        for (int i = 0; i < res.length; i++) {
            Assert.assertEquals(res[i], target[i],1e-6);
            System.out.println(res[i]+","+target[i]);
        }
    }
    @Test
    public void testInformationValue2() {
        System.out.println("Test woe");
        double[] arrayY    = new double[]{1, 0, 0, 1, 1, 0, 1};
        double[] feaValues = new double[]{0, 0, 1, 1, 0, 5, 5};
        int numBins = 6;
        double res = Analysis.IV2(arrayY, feaValues, numBins);
        double target = 0.1155245300933242;
        Assert.assertEquals(res, target, 1e-6);
    }

    @Test
    public void testKs() {
        double[] labels = new double[]{1,1,1,1,1,0,0,0,0,0};
        double[] preds = new double[]{0.1,0.8,0.7,0.6,0.5,0.4,0.3,0.2,0.1,0};
        double res = Analysis.ks(preds, labels);
        double exp = 0.8;
        Assert.assertEquals(res, exp, 1e-6);
    }
    @Test
    public void testCalculateKSCurve() {
        System.out.println("test KS Curve");
        double[] labels = new double[]{1,1,1,0};
        double[] preds = new double[]{0.7,0.6,0.5,0.4};
        double[][] ksPoints = Analysis.calculateKSCurve(preds, labels);
        // expected
        double[][] exp_res = {{1.0, 0.0}, {0.7, 0.3333333333}, {0.6, 0.6666666666}, {0.5, 1.0}, {0.4, 0.0}};
        for (int i = 0; i < ksPoints.length; i++) {
            for (int j = 0; j < ksPoints[0].length; j++) {
                Assert.assertEquals(ksPoints[i][j], exp_res[i][j], 1e-6);
            }
        }

    }
    @Test
    public void testCovariance() {
        System.out.println("Test Covariance");
        double[] labels = new double[]{1,1,1,0};
        double[] preds = new double[]{0.7,0.6,0.5,0.4};
        double cov = Analysis.covariance(preds, labels);
        double target = 0.05;
        Assert.assertEquals(cov, target, 1e-6);
        System.out.println(cov);
    }
    @Test
    public void testPearson() {
        System.out.println("Test Pearson");
        double[] labels = new double[]{1,1,1,0};
        double[] preds = new double[]{0.7,0.6,0.5,0.4};
        double cor = Analysis.pearson(preds, labels);
        double target = 1.032795558989;
        Assert.assertEquals(cor, target, 1e-6);
        System.out.println(cor);
    }
}