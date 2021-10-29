package com.jdt.fedlearn.core.model.common.loss;
import com.jdt.fedlearn.core.util.Tool;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class TestCrossEntropy {
    public static void testTransform(Loss loss, double[] pred, int dataSize, int numClass) {
        double[] trans = loss.transform(pred);

        System.out.println("pred (numClass=" + numClass + " * dataSize=" + dataSize + ")");
        print(pred);

        System.out.println("transform result:");
        print(trans);

        for (int i = 0; i < dataSize; i++) {
            double sum = 0;
            for (int j = 0; j < numClass; j++) {
                sum += trans[j * dataSize + i];
            }
            System.out.print("uid" + i + "_sum = " + sum + "; ");
            assert sum == 1.0;
        }
        System.out.println();
    }

    public static void testGradHess(Loss loss, double[] pred, double[] label, int dataSize, int numClass) {
        double[][] grad = Tool.reshape(loss.grad(pred, label), numClass);
        double[][] hess = Tool.reshape(loss.hess(pred, label), numClass);

        System.out.println("grad");
        print(grad);
        System.out.println("hess");
        print(hess);
    }

    public static void main(String[] args) {
        int numClass = 3;
        Loss loss = new crossEntropy(numClass);

        // Pred : flatten nClass * dataSize
        // uid 0: 0.1 0.6 0.3
        // uid 1: 0.7 0.3 0.1
        double[] pred1 = new double[]{0.1,0.7,  0.6,0.2,  0.3,0.1};
        double[] label1 = new double[]{1, 0};
        testTransform(loss, pred1, label1.length, numClass);
        testGradHess(loss, pred1, label1, label1.length, numClass);

        // Pred : flatten nClass * dataSize
        // uid 0: 10 19 25
        // uid 1: 30 22 32
        // uid 2: 25 33 35
        // uid 3: 5 16 78
        double[] pred2 = new double[]{10,30,25,5,  19,22,33,16,  25,32,35,78};
        double[] label2 = new double[]{2, 0, 1, 2};
        testTransform(loss, pred2, label2.length, numClass);
        testGradHess(loss, pred2, label2, label2.length, numClass);

        // Pred : flatten nClass * dataSize
        // uid 0: 0 0 5
        // uid 1: 5 0 0
        double[] pred3 = new double[]{0, 5, 0, 0, 5, 0};
        double[] label3_1 = new double[]{2,0};
        double[] label3_2 = new double[]{1,1};
        double[] label3_3 = new double[]{2,1};
        testTransform(loss, pred3, label3_1.length, numClass);
        testGradHess(loss, pred3, label3_1, label3_1.length, numClass);
        testTransform(loss, pred3, label3_2.length, numClass);
        testGradHess(loss, pred3, label3_2, label3_2.length, numClass);
        testTransform(loss, pred3, label3_3.length, numClass);
        testGradHess(loss, pred3, label3_3, label3_3.length, numClass);

        double[] pred4 = new double[]{0.17142857142857149,-0.07894736842105265,0.17142857142857149,0.17142857142857149,
                -0.08838606351474927,0.13851978069710405,-0.08838606351474927,-0.08838606351474927,
                -0.10147564097937667,-0.10147564097937667,-0.10147564097937667,-0.10147564097937667};
        double[] label4 = new double[]{0,1,0,0};
        testTransform(loss, pred4, label4.length, numClass);
        testGradHess(loss, pred4, label4, label4.length, numClass);

    }

    public static void print(double[] v) {
        for (double iv: v) {
            System.out.print(iv + " ");
        }
        System.out.println();
    }

    public static void print(double[][] v) {
        for (double[] iv: v) {
            for (double ijv: iv) {
                System.out.print(ijv + " ");
            }
            System.out.println();
        }
        System.out.println();
    }

    @Test
    public void testGainScoreDelta(){
        crossEntropy loss = new crossEntropy(3);
        double gainDelta = loss.getGainDelta(10, 1.0);
        double scoreDelta = loss.getLeafScoreDelta(10, 1.0);
        assertEquals(gainDelta, 10 / 2.0);
        assertEquals(scoreDelta, 1.0);
        gainDelta = loss.getGainDelta(0, 0);
        assertEquals(gainDelta, 0);
        scoreDelta = loss.getLeafScoreDelta(0, 0);
    }

}