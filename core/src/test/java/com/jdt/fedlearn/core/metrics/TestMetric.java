package com.jdt.fedlearn.core.metrics;

import com.jdt.fedlearn.core.model.common.loss.Loss;
import com.jdt.fedlearn.core.model.common.loss.crossEntropy;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.util.Tool;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;

public class TestMetric {

    @Test
    public void testCalculateMetric() {
        System.out.println("test calculateMetric");
        double[] labels = new double[]{1, 0, 1, 1, 1, 1, 0, 0};
        double[] preds = new double[]{0.6, 0.8, 0.7, 0.5, 0.5, 0.4, 0.3, 0.1};
        MetricType[] ems = {MetricType.ACC, MetricType.ERROR};
        Map<MetricType, Double> map = Metric.calculateMetric(ems, preds, labels);
        // expected
        Map<MetricType, Double> target = new HashMap<MetricType, Double>();
        target.put(MetricType.ACC, 0.5);
        target.put(MetricType.ERROR, 0.5);
        Assert.assertTrue(map.equals(target));

    }

    //由于存在精度问题，暂时使用此函数计算差的绝对值小于1e-6即认为相等。
    @Test
    public void testROC() {
        System.out.println("test ROC");
        double[] labels = new double[]{1, 1, 1, 1, 1, 0, 0, 0, 0, 0};
        double[] preds = new double[]{0.1, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1, 0};
        Double[][] rocPoints = Metric.calculateRocCurve(preds, labels);
        for (Double[] rocPoint : rocPoints) {
            System.out.println("X(False Positive Rate): " + rocPoint[0] + "   Y(True Positive Rate): " + rocPoint[1]);
        }
    }

    @Test
    public void testKS() {
        System.out.println("test KS");
        double[] labels = new double[]{1, 1, 1, 1, 1, 0, 0, 0, 0, 0};
        double[] preds = new double[]{0.1, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1, 0};
        Double[][] ksPoints = Metric.calculateKSCurve(preds, labels);
        for (Double[] ksPoint : ksPoints) {
            System.out.println("X(Predict Value): " + ksPoint[0] + "   Y(KS Value TPR-FPR): " + ksPoint[1]);
        }
    }

    @Test
    public void testKS2() {
        System.out.println("test KS");
        double[] labels = new double[]{1, 1, 1, 1, 1, 0, 0, 0, 0, 0};
        double[] preds = new double[]{0.1, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1, 0};
        double ks = Metric.KS(preds, labels);
        Assert.assertEquals(ks, 0.8, 1e-6);
    }


    @Test
    public void testCalculateLocalMetricSumPart() {
        double[] labels = new double[]{1, 0, 1, 1, 0, 1, 0, 0};
        double[] preds = new double[]{0.6, 0.8, 0.7, 0.5, 0.5, 0.4, 0.3, 0.1};
        double[] weights = new double[]{1, 1, 1, 1, 1, 1, 1, 1};
        // expected
        Map<MetricType, Double> map = new HashMap<MetricType, Double>();
        map.put(MetricType.ACC, 4.0);
        map.put(MetricType.MSE, 1.85);
        MetricType[] mts = {MetricType.ACC, MetricType.MSE};
        Map<MetricType, Double> res = Metric.calculateLocalMetricSumPart(mts, preds, labels, weights);
        Assert.assertTrue(map.equals(res));
    }

    @Test
    public void calculateGlobalMetric() {
        double[] labels = new double[]{1, 0, 1, 1, 0, 1, 0, 0};
        double[] preds = new double[]{0.6, 0.8, 0.7, 0.5, 0.5, 0.4, 0.3, 0.1};
        double[] weights = new double[]{1, 1, 1, 1, 1, 1, 1, 1};
        double res = Metric.calculateGlobalMetric(MetricType.ACC, Metric.calculateLocalMetricSumPart(MetricType.ACC, preds, labels, weights), preds.length);
        Assert.assertEquals(res, 0.5, 1e-6);
    }

    @Test
    public void testRecall() {
        System.out.println("test Recall");
        double[] labels = new double[]{1, 1, 1, 1, 1, 0, 0, 0, 0, 0};
        double[] preds = new double[]{0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1, 0};
        double recall = Metric.calculateMetric(MetricType.RECALL, preds, labels);
        //System.out.println("Recall value is: " + recall);
        Assert.assertTrue(Tool.approximate(recall, 0.8));
    }

    @Test
    public void testPrecision() {
        System.out.println("test Precision");
        double[] labels = new double[]{1, 1, 1, 1, 1, 0, 0, 0, 0, 0};
        double[] preds = new double[]{0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1, 0};
        double precision = Metric.calculateMetric(MetricType.PRECISION, preds, labels);
        //System.out.println("Precision value is: " + precision);
        Assert.assertTrue(Tool.approximate(precision, 1.0));
    }

    @Test
    public void testF1() {
        System.out.println("test F1");
        double[] labels = new double[]{1, 1, 1, 1, 1, 0, 0, 0, 0, 0};
        double[] preds = new double[]{0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1, 0};
        double f1 = Metric.calculateMetric(MetricType.F1, preds, labels);
        //System.out.println("F1 value is: " + f1);
        Assert.assertTrue(Tool.approximate(f1, 0.888888888888889));

    }

    @Test
    public void testR2() {
        System.out.println("test R2");
        double[] labels = new double[]{1, 1, 1, 1, 1, 0, 0, 0, 0, 0};
        double[] preds = new double[]{0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1, 0};
        double r2 = Metric.calculateMetric(MetricType.R2, preds, labels);
        //System.out.println("R2 value is: " + r2);
        Assert.assertTrue(Tool.approximate(r2, 0.66));
    }

    @Test
    public void testAUC() {
        System.out.println("test AUC");
        double[] labels = new double[]{1, 0, 1, 1, 0, 1, 0};
        double[] preds = new double[]{0.9, 0.8, 0.7, 0.5, 0.5, 0.4, 0.3};
        double auc = Metric.calculateMetric(MetricType.AUC, preds, labels);
        //System.out.println("AUC value is: " + auc);
        Assert.assertTrue(Tool.approximate(auc, 0.625));
    }

    @Test
    public void testMAE() {
        System.out.println("test MAE");
        double[] labels = new double[]{1, 0, 1, 1, 0, 1, 0};
        double[] preds = new double[]{0.9, 0.8, 0.7, 0.5, 0.5, 0.4, 0.3};
        double mae = Metric.calculateMetric(MetricType.MAE, preds, labels);
        //System.out.println("MAE value is: " + mae);
        Assert.assertTrue(Tool.approximate(mae, 0.4428571428571429));
    }

    @Test
    public void testMAAPE() {
        System.out.println("test MAAPE");
        double[] labels = new double[]{1, 0, 1, 1, 0, 1, 0};
        double[] preds = new double[]{0.9, 0.8, 0.7, 0.5, 0.5, 0.4, 0.3};
        double maape = Metric.calculateMetric(MetricType.MAAPE, preds, labels);
        //System.out.println("MAAPE value is: " + maape);
        Assert.assertTrue(Tool.approximate(maape, 87.25116480893014));
    }

    @Test
    public void testMAPE() {
        System.out.println("test MAPE");
        double[] labels = new double[]{1, 0, 1, 1, 0, 1, 0};
        double[] preds = new double[]{0.9, 0.8, 0.7, 0.5, 0.5, 0.4, 0.3};
        double mape = Metric.calculateMetric(MetricType.MAPE, preds, labels);
        //System.out.println("MAPE value is: " + mape);
        Assert.assertTrue(Tool.approximate(mape, 21.428571428571427));
    }

    @Test
    public void testMSE() {
        System.out.println("test MSE");
        double[] labels = new double[]{1, 0, 1, 1, 0, 1, 0};
        double[] preds = new double[]{0.9, 0.8, 0.7, 0.5, 0.5, 0.4, 0.3};
        double mse = Metric.calculateMetric(MetricType.MSE, preds, labels);

        //System.out.println("MSE value is: " + mse);
        Assert.assertTrue(Tool.approximate(mse, 0.24142857142857146));
    }

    @Test
    public void testRMSE() {
        System.out.println("test RMSE");
        double[] labels = new double[]{1, 0, 1, 1, 0, 1, 0};
        double[] preds = new double[]{0.9, 0.8, 0.7, 0.5, 0.5, 0.4, 0.3};
        double rmse = Metric.calculateMetric(MetricType.RMSE, preds, labels);
        //System.out.println("RMSE value is: " + rmse);
        Assert.assertTrue(Tool.approximate(rmse, 0.4913538149119954));
    }

    @Test
    public void testACC() {
        System.out.println("test ACC");
        double[] labels = new double[]{1, 0, 1, 1, 1, 1, 0, 0};
        double[] preds = new double[]{0.6, 0.8, 0.7, 0.5, 0.5, 0.4, 0.3, 0.1};
        double acc = Metric.calculateMetric(MetricType.ACC, preds, labels);

        //System.out.println("ACC value is: " + acc);
        Assert.assertTrue(Tool.approximate(acc, 0.5));
    }

    @Test
    public void testERROR() {
        System.out.println("test ERROR");
        double[] labels = new double[]{1, 0, 1, 1, 1, 1, 0, 0};
        double[] preds = new double[]{0.6, 0.8, 0.7, 0.5, 0.5, 0.4, 0.3, 0.1};
        double error = Metric.calculateMetric(MetricType.ERROR, preds, labels);
        //System.out.println("ERROR value is: " + error);
        Assert.assertTrue(Tool.approximate(error, 0.5));
    }

    @Test
    public void test_f1() {
        System.out.println("test ERROR");
        double[] labels = new double[]{1, 1, 1, 1, 1, 0, 0, 0, 0, 0};
        double[] preds = new double[]{0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2, 0.1, 0};
        double f1 = Metric.calculateMetric(MetricType.F1, preds, labels);
        //System.out.println("F1 value is: " + f1);
        Assert.assertEquals(f1, 0.888888888888889, 1e-6);
    }

    @Test
    public void testPrecision2() {
        System.out.println("test Precision");
        double[] labels = new double[]{1, 1, 1, 1, 1, 0, 0, 0};
        double[] preds = new double[]{0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2};
        double precision = Metric.calculateMetric(MetricType.PRECISION, preds, labels);
        //System.out.println("Precision value is: " + precision);
        Assert.assertTrue(Tool.approximate(precision, 1.0));
    }

    @Test
    public void testcalculateMetricFromGlobalDiff() {
        MetricType[] mts = {MetricType.RMSE, MetricType.MSE};
        double[] diff = new double[]{0.6, 0.0, -0.6, 0.5, -0.5, 0.4, -0.3, 0.1};
        Map<MetricType, Double> res = Metric.calculateMetricFromGlobalDiff(mts, diff);
        Map<MetricType, Double> map = new HashMap<>();
        map.put(MetricType.RMSE, 0.4301162633521313);
        map.put(MetricType.MSE, 0.185);
        Assert.assertEquals(res, map);

    }

    @Test
    public void testCalculateMetricFromGlobalDiff_mse() {
        System.out.println("test calculateMetricFromGlobalDiff");
        double[] diff = new double[]{0.6, 0.0, -0.6, 0.5, -0.5, 0.4, -0.3, 0.1};
        double mseFromDiff = Metric.calculateMetricFromGlobalDiff(MetricType.MSE, diff);
        Assert.assertEquals(mseFromDiff, 0.185, 1e-6);
    }

    @Test
    public void testCalculateMetricFromGlobalDiff_mae() {
        System.out.println("test calculateMetricFromGlobalDiff");
        double[] diff = new double[]{0.6, 0.0, -0.6, 0.5, -0.5, 0.4, -0.3, 0.1};
        double mseFromDiff = Metric.calculateMetricFromGlobalDiff(MetricType.MAE, diff);
        Assert.assertEquals(mseFromDiff, 0.375, 1e-6);
    }

    @Test
    public void testCalculateMetricFromGlobalDiff_rmse() {
        System.out.println("test calculateMetricFromGlobalDiff");
        double[] diff = new double[]{0.6, 0.0, -0.6, 0.5, -0.5, 0.4, -0.3, 0.1};
        double mseFromDiff = Metric.calculateMetricFromGlobalDiff(MetricType.RMSE, diff);
        Assert.assertEquals(mseFromDiff, 0.4301162633521313, 1e-6);
    }

    @Test
    public void sumLocalAccuracy() {
        System.out.println("test sumLocalAccuracy");
        double[] labels = new double[]{1, 0, 1, 1, 0, 1, 0, 0};
        double[] preds = new double[]{0.6, 0.8, 0.7, 0.5, 0.5, 0.4, 0.3, 0.1};
        double[] weights = new double[]{1, 1, 1, 1, 1, 1, 1, 1};
        double sumlocalacc = Metric.calculateLocalMetricSumPart(MetricType.ACC, preds, labels, weights);
        //System.out.println("sumLocalAccuracy value is: " + sumlocalacc);
        Assert.assertTrue(Tool.approximate(sumlocalacc, 4.0));
    }

    @Test
    public void sumLocalSquareError() {
        System.out.println("test sumLocalAccuracy");
        double[] labels = new double[]{1, 0, 1, 1, 0, 1, 0, 0};
        double[] preds = new double[]{0.6, 0.8, 0.7, 0.5, 0.5, 0.4, 0.3, 0.1};
        double[] weights = new double[]{1, 1, 1, 1, 1, 1, 1, 1};
        double sumlocalSquareError = Metric.calculateLocalMetricSumPart(MetricType.MSE, preds, labels, weights);
        //System.out.println("sumLocalSquareError value is: " + sumlocalSquareError);
        Assert.assertTrue(Tool.approximate(sumlocalSquareError, 1.85));
    }

    @Test
    public void testsumLocalAbsolutePercentageError() {
        System.out.println("test sumLocalAbsolutePercentageError");
        double[] labels = new double[]{1, 0.1, 1, 1, 0.2, 1, 0.9, 0.3};
        double[] preds = new double[]{0.6, 0.8, 0.7, 0.5, 0.5, 0.4, 0.3, 0.1};
        double[] weight = new double[]{1, 1, 1, 1, 1, 1, 1, 1};
        double sumLocalAPE = Metric.calculateLocalMetricSumPart(MetricType.MAPE, preds, labels, weight);
        //System.out.println("sumLocalAbsolutePercentageError value is: " + sumLocalAPE);
        Assert.assertTrue(Tool.approximate(sumLocalAPE, 11.633333333333333));
    }

    @Test
    public void testsumLocalMaape() {
        System.out.println("test sumLocalMaape");
        double[] labels = new double[]{1, 0, 1, 1, 0, 1, 0, 0};
        double[] preds = new double[]{0.6, 0.8, 0.7, 0.5, 0.5, 0.4, 0.3, 0.1};
        double[] weight = new double[]{1, 1, 1, 1, 1, 1, 1, 1};
        double sumLocalmaape = Metric.calculateLocalMetricSumPart(MetricType.MAAPE, preds, labels, weight);
//        double sumLocalmaape = Metric.sumLocalMaape(preds, labels, weight);
        //System.out.println("sumLocalMaape value is: " + sumLocalmaape);
        Assert.assertTrue(Tool.approximate(sumLocalmaape, 7.959215588041209));
    }

    @Test
    public void testsumLocalAbsoluteError() {
        System.out.println("test sumLocalAbsoluteError");
        double[] labels = new double[]{1, 0, 1, 1, 0, 1, 0, 0};
        double[] preds = new double[]{0.6, 0.8, 0.7, 0.5, 0.5, 0.4, 0.3, 0.1};
        double[] weight = new double[]{1, 1, 1, 1, 1, 1, 1, 1};
        double sumLocalAE = Metric.sumLocalAbsoluteError(preds, labels, weight);
        //System.out.println("sumLocalAbsoluteError value is: " + sumLocalAE);
        Assert.assertTrue(Tool.approximate(sumLocalAE, 3.5));
    }

    @Test
    public void mean_square_errorFromDiff() {
        System.out.println("test mean_square_errorFromDiff");
        double[] diff = new double[]{0.6, 0.0, -0.6, 0.5, -0.5, 0.4, -0.3, 0.1};
        double mseFromDiff = Metric.mean_square_errorFromDiff(diff);
        //System.out.println("mean_square_errorFromDiff value is: " + mseFromDiff);
        Assert.assertTrue(Tool.approximate(mseFromDiff, 0.185));
    }

    @Test
    public void root_mean_square_errorFromDiff() {
        System.out.println("test root_mean_square_errorFromDiff");
        double[] diff = new double[]{0.6, 0.0, -0.6, 0.5, -0.5, 0.4, -0.3, 0.1};
        double rmseFromDiff = Metric.root_mean_square_errorFromDiff(diff);
        //System.out.println("root_mean_square_errorFromDiff value is: " + rmseFromDiff);
        Assert.assertTrue(Tool.approximate(rmseFromDiff, 0.4301162633521313));
    }

    @Test
    public void mean_absolute_errorFromDiff() {
        System.out.println("test mean_absolute_errorFromDiff");
        double[] diff = new double[]{0.6, 0.0, -0.6, 0.5, -0.5, 0.4, -0.3, 0.1};
        double maeFromDiff = Metric.mean_absolute_errorFromDiff(diff);
        //System.out.println("mean_absolute_errorFromDiff value is: " + maeFromDiff);
        Assert.assertTrue(Tool.approximate(maeFromDiff, 0.375));
    }

    @Test
    public void testMerror() {
        System.out.println("test merror");
        // 3 uids, 5 classes
        double[] labels = new double[]{0, 3, 4};
        double[] preds = new double[]{0.1, 0.1, 0.1, 0.15, 0.15, 0.15, 0.25, 0.25, 0.25, 0.3, 0.3, 0.3, 0.2, 0.2, 0.2};
        double merror = Metric.calculateMetric(MetricType.MERROR, preds, labels);
        System.out.println("MERROR value is: " + merror);
        Assert.assertTrue(Tool.approximate(merror, 0.6666666666666667));

        double[] pred4 = new double[]{0.17142857142857149, -0.07894736842105265, 0.17142857142857149, 0.17142857142857149,
                -0.08838606351474927, 0.13851978069710405, -0.08838606351474927, -0.08838606351474927,
                -0.10147564097937667, -0.10147564097937667, -0.10147564097937667, -0.10147564097937667};
        double[] label4 = new double[]{0, 1, 0, 0};
        Loss loss = new crossEntropy(3);
        double merror4 = Metric.calculateMetric(MetricType.MERROR, loss.transform(pred4), label4);
        System.out.println("MERROR value is: " + merror4);
        Assert.assertTrue(Tool.approximate(merror4, 0.0));
    }


    @Test
    public void macc() {
        System.out.println("test mACC");
        double[] pred4 = new double[]{0.17142857142857149, -0.07894736842105265, 0.17142857142857149, 0.17142857142857149,
                -0.08838606351474927, 0.13851978069710405, -0.08838606351474927, -0.08838606351474927,
                -0.10147564097937667, -0.10147564097937667, -0.10147564097937667, -0.10147564097937667};
        double[] label4 = new double[]{0, 1, 0, 0};
        double merror4 = Metric.calculateMetric(MetricType.MACC, pred4, label4);
        System.out.println("MACC value is: " + merror4);
        Assert.assertTrue(Tool.approximate(merror4, 1));
    }

    @Test
    public void testRae() {
        double[] pred4 = new double[]{0.17142857142857149, 0.77894736842105265};
        double[] label4 = new double[]{0, 1};
        double res = Metric.calculateMetric(MetricType.RAE, pred4, label4);
        Assert.assertEquals(res, 0.3924812030075189, 1e-6);
    }

    @Test
    public void testRrse() {
        double[] pred4 = new double[]{0.17142857142857149, 0.77894736842105265};
        double[] label4 = new double[]{0, 1};
        double res = Metric.calculateMetric(MetricType.RRSE, pred4, label4);
        Assert.assertEquals(res, 0.15650404206003737, 1e-6);
    }

    @Test
    public void testConfusionMatrix() {
        double[] pred4 = new double[]{0.17142857142857149, 0.77894736842105265, 0.6, 0.7};
        double[] label4 = new double[]{0, 1, 0, 1};
        Double[][] res = Metric.confusionMatrix(pred4, label4);
        double[][] target = new double[][]{{1, 0}, {1, 2}};
        Assert.assertEquals(res, target);
    }

    @Test
    public void testmRecall() {
        double[][] pre = new double[][]{{0.1,0.6},{ 0.5,0.2},{ 0.3,0.2}};
        double[] label = {1, 0};
        List<Double> m = new ArrayList<>();
        m.add(2d);
        m.add(1d);
        m.add(3d);
        Double[][] res = Metric.mRecall(pre, label, m);
        System.out.println("res" + Arrays.deepToString(res));
        double[][] target = new double[][]{{2,1,3}, {1, 1,0}};
        Assert.assertEquals(res, target);
    }

    @Test
    public void testmF1() {
        double[][] pre = new double[][]{{0.1,0.6},{ 0.5,0.2},{ 0.3,0.2}};
        double[] label = {1, 0};
        List<Double> m = new ArrayList<>();
        m.add(2d);
        m.add(1d);
        m.add(3d);
        Double[][] res = Metric.mF1(pre, label, m);
        System.out.println("res" + Arrays.deepToString(res));
        double[][] target = new double[][]{{2,1,3}, {1, 1,0}};
        Assert.assertEquals(res, target);
    }

    @Test
    public void testmPrecision() {
        double[][] pre = new double[][]{{0.1,0.6},{ 0.5,0.2},{ 0.3,0.2}};
        double[] label = {1, 0};
        List<Double> m = new ArrayList<>();
        m.add(2d);
        m.add(1d);
        m.add(3d);
        Double[][] res = Metric.mPrecision(pre, label, m);
        System.out.println("res" + Arrays.deepToString(res));
        double[][] target = new double[][]{{2,1,3}, {1, 1,0}};
        Assert.assertEquals(res, target);
    }

    @Test
    public void testmKs() {
        double[][] pre = new double[][]{{0.1,0.6},{ 0.5,0.2},{ 0.3,0.2}};
        double[] label = {1, 0};
        List<Double> m = new ArrayList<>();
        m.add(2d);
        m.add(1d);
        m.add(3d);
        Double[][] res = Metric.mKs(pre, label, m);
        System.out.println("res" + Arrays.deepToString(res));
        double[][] target = new double[][]{{2,1,3}, {1, 1,0}};
        Assert.assertEquals(res, target);
    }

    @Test
    public void testmAcc() {
        double[][] pre = new double[][]{{0.1,0.6},{ 0.5,0.2},{ 0.3,0.2}};
        double[] label = {1, 0};
        List<Double> m = new ArrayList<>();
        m.add(2d);
        m.add(1d);
        m.add(3d);
        Double[][] res = Metric.mAccuracy(pre, label, m);
        System.out.println("res" + Arrays.deepToString(res));
        double[][] target = new double[][]{{2,1,3}, {1, 0.5,1.0}};
        Assert.assertEquals(res, target);
    }

    @Test
    public void testMConfusionMatrix() {
        double[][] pre = new double[][]{{0.1,0.6},{ 0.5,0.2},{ 0.3,0.2}};
        double[] label = {1, 0};
        List<Double> m = new ArrayList<>();
        m.add(2d);
        m.add(1d);
        m.add(3d);
        Double[][] res = Metric.mConfusionMatrix(pre, label,m);
        System.out.println("res" + Arrays.deepToString(res));
        double[][] target = new double[][]{{2,1,3},{1,0,0}, {0,1,0},{0, 0,0}};
        Assert.assertEquals(res, target);
    }

    @Test
    public void testmAuc() {
//        double[][] pre = new double[][]{{0.1, 0.5, 0.3}, {0.6, 0.2, 0.2}};
        double[][] pre = new double[][]{{0.1,0.6},{ 0.5,0.2},{ 0.3,0.2}};
        double[] label = {1, 0};
        List<Double> m = new ArrayList<>();
        m.add(2d);
        m.add(1d);
        m.add(3d);
        Double[][] res = Metric.mAuc(pre, label, m);
        System.out.println("res" + Arrays.deepToString(res));
        double[][] target = new double[][]{{2,1,3}, {1, 1,0}};
        Assert.assertEquals(res, target);
    }



//    @Test
//    public void testMlogloss() {
//        System.out.println("test mlogloss");
//        // 3 uids, 5 classes
//        double[] labels = new double[]{0, 3, 4};
//        double[] preds = new double[]{0.1, 0.1, 0.1, 0.15, 0.15, 0.15, 0.25, 0.25, 0.25, 0.3, 0.3, 0.3, 0.2, 0.2, 0.2};
//        double mlogloss = Metric.calculateMetric(MetricType.MLOGLOSS, preds, labels);
//        System.out.println("MLOGLOSS value is: " + mlogloss);
//    }
}
