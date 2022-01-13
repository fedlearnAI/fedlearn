package com.jdt.fedlearn.core.math;

import com.jdt.fedlearn.core.encryption.common.Ciphertext;
import com.jdt.fedlearn.core.encryption.common.EncryptionTool;
import com.jdt.fedlearn.core.encryption.common.PrivateKey;
import com.jdt.fedlearn.core.encryption.common.PublicKey;
import com.jdt.fedlearn.core.encryption.javallier.JavallierCiphertext;
import com.jdt.fedlearn.core.encryption.javallier.JavallierTool;
import com.jdt.fedlearn.core.entity.randomForest.DataUtils;
import com.jdt.fedlearn.core.type.data.Tuple2;
import com.jdt.fedlearn.core.util.Tool;
import com.jdt.fedlearn.grpc.federatedlearning.PaillierKeyPublic;
import com.jdt.fedlearn.grpc.federatedlearning.Vector;
import com.n1analytics.paillier.EncryptedNumber;
import com.n1analytics.paillier.PaillierPrivateKey;
import org.ejml.simple.SimpleMatrix;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import static com.jdt.fedlearn.core.math.MathExt.generateNormal;
import static com.jdt.fedlearn.core.math.MathExt.generateUniform;
import static org.testng.Assert.assertEquals;

public class TestMathExt {


    //由于存在精度问题，暂时结果的差的绝对值小于1e-6即认为相等。

    @Test
    public void testCombination() {
        double[] x = new double[]{2.0, 3.0, 4.0};
        List<Tuple2<Double, Double>> y = MathExt.combination_2(x);
        List<Tuple2<Double, Double>> target = new ArrayList<>();
        target.add(new Tuple2(2.0, 2.0));
        target.add(new Tuple2(2.0, 3.0));
        target.add(new Tuple2(2.0, 4.0));
        target.add(new Tuple2(3.0, 3.0));
        target.add(new Tuple2(3.0, 4.0));
        target.add(new Tuple2(4.0, 4.0));
        for (int i = 0; i < y.size(); i++) {
            assertEquals(y.get(i).toString(), target.get(i).toString());
        }
    }

    @Test
    public void testDiffSet2() {
        int[] a = new int[]{1, 2, 3, 4};
        int[] b = new int[]{1, 2};
        int[] c = MathExt.diffSet(a, b);
        Assert.assertTrue(Tool.approximate(c.length,2));
    }

    @Test
    public void testSum2() {
        List<Double> doubles = Arrays.asList(2.3, 4.5);
        Assert.assertTrue(Tool.approximate(MathExt.sum(doubles),6.8));
    }

    @Test
    public void testDot_multiply() {
        double[] line1 = {45.67, 34.56, 100.111};
        double w1 = 12.34;
        double[] res = MathExt.dotMultiply(line1, w1);
        double[] target = {563.5678, 426.4704, 1235.36974};
        for (int i = 0; i < res.length; i++) {
            Assert.assertTrue(Tool.approximate(res[i],target[i]));
        }
    }

    @Test
    public void testDot_multiply1() {
        double[] w2 = {23.22, 3.111, 9.9};
        double[] line1 = {45.67, 34.56, 100.111};

        double res = MathExt.dotMultiply(line1, w2);
        Assert.assertTrue(Tool.approximate(res,2159.07246));
    }

    @Test
    public void testDot_multiply2() {
        final double[] w2 = {23.22, 3.111, 9.9};
        final int[] line2 = {1, 2, 3};
        double res = MathExt.dotMultiply(line2, w2);
        Assert.assertTrue(Tool.approximate(res,59.142));
    }

    @Test
    public void testDot_multiply3() {
        long[] w2 = {23L, 3L, 9L};
        long[] line2 = {1L, 2L, 3L};
        long res = MathExt.dotMultiply(line2, w2);
        Assert.assertEquals(res,56L, 1e-6);

    }

    @Test
    public void testDot_multiply4() {
        int[] w2 = {1, 2, 3};
        double[] line2 = {1.0, 2.0, 3.0};
        double res = MathExt.dotMultiply(w2, line2);
        Assert.assertEquals(res,14.0, 1e-6);
    }

    @Test
    public void testElementwiseMul1() {
        long[] w2 = {23L, 3L, 9L};
        long[] line2 = {1L, 2L, 3L};
        long[] res = MathExt.elementwiseMul(line2, w2);
        long[] target = {23L, 6L, 27L};
        for (int i = 0; i < res.length; i++) {
            Assert.assertEquals(res[i],target[i], 1e-6);
        }
    }

    @Test
    public void testElementwiseMul2() {
        double[] w2 = {23, 3.1, 9};
        double[] line2 = {1.5, 2, 3};
        double[] res = MathExt.elementwiseMul(line2, w2);
        double[] target = {34.5, 6.2, 27};
        for (int i = 0; i < res.length; i++) {
            Assert.assertEquals(res[i],target[i], 1e-6);

        }
    }

    @Test
    public void testElementwiseMul3() {
        double[] w2 = {23, 3.1, 9};
        long[] line2 = {10L, 2L, 3L};
        double[] res = MathExt.elementwiseMul(w2, line2);
        double[] target = {230.0, 6.2, 27.0};
        for (int i = 0; i < res.length; i++) {
            Assert.assertEquals(res[i],target[i], 1e-6);

        }
    }

    @Test
    public void testElementwiseMul4() {
        long[] line2 = {10L, 2L, 3L};
        long w2 = 10L;
        long[] res = MathExt.elementwiseMul(line2, w2);
        long[] target = {100L, 20L, 30L};
        for (int i = 0; i < res.length; i++) {
            Assert.assertEquals(res[i],target[i], 1e-6);
        }
    }

    @Test
    public void testElementwiseMul5() {
        double[] line2 = {10.0, 2.0, 3.0};
        double w2 = 10.0;
        double[] res = MathExt.elementwiseMul(line2, w2);
        double[] target = {100.0, 20.0, 30.0};
        for (int i = 0; i < res.length; i++) {
            Assert.assertEquals(res[i],target[i], 1e-6);
        }
    }


    @Test
    public void testDot_add() {
        double[] w2 = {23.22, 3.111, 9.9};
        double[] line1 = {45.67, 34.56, 100.111};
        double[] res = MathExt.add(line1, w2);
        double[] target = {68.89, 37.671, 110.011};
        for (int i = 0; i < res.length; i++) {
            Assert.assertEquals(res[i],target[i], 1e-6);
        }
    }

    @Test
    public void testDot_add1() {
        final double[] line1 = {45.67, 34.56, 100.111};
        final double w1 = 12.34;
        double[] res = MathExt.add(line1, w1);
        double[] target = {58.01, 46.9, 112.451};
        for (int i = 0; i < res.length; i++) {
            Assert.assertTrue(Tool.approximate(res[i],target[i]));
        }
    }

    @Test
    public void testDot_add2() {
        long[] w2 = {10L, 3L, 9L};
        long[] line1 = {4L, 3L, 10L};
        long[] res = MathExt.add(line1, w2);
        long[] target = {14L, 6L, 19L};
        for (int i = 0; i < res.length; i++) {
            Assert.assertEquals(res[i],target[i], 1e-6);
        }
    }

    @Test
    public void testDot_add3() {
        long[] w2 = {10L, 3L, 9L};
        long[] line1 = {4L, 3L, 10L};
        long[] res = MathExt.add(line1, w2);
        long[] target = {14L, 6L, 19L};
        for (int i = 0; i < res.length; i++) {
            Assert.assertEquals(res[i],target[i], 1e-6);
        }
    }

    @Test
    public void testSub_double() {
        double[] line1 = {45.67, 34.56, 100.111};
        double[] w2 = {23.22, 3.111, 9.9};
        double[] res = MathExt.sub(line1, w2);
        double[] target = {22.45, 31.449, 90.211};
        for (int i = 0; i < res.length; i++) {
            Assert.assertEquals(res[i], target[i], 1e-6);
        }
    }

    @Test
    public void testSub_long() {
        long[] l1 = {2000000L, 3000000L, 4000000L, 5000000L};
        long[] l2 = {1000000L, 2000000L, 1000000L, 2000000L};
        long[] res = MathExt.sub(l1, l2);
        long[] target = {1000000L, 1000000L, 3000000L, 3000000L};
        for (int i = 0; i < res.length; i++) {
            Assert.assertEquals(res[i], target[i], 1e-6);
        }
    }

    @Test
    public void testElemwiseAdd_matD() {
        final double[] line3 = {1.34, 54.34, 321.12};
        final double[] line4 = {43.2, 6.66, 756};
        final double[] line1 = {45.67, 34.56, 100.111};
        final double[][] matrix = {line1, line3, line4};
        final double[][] matrix1 = {line3, line1, line4};
        double[][] res = MathExt.elemwiseAdd(matrix, matrix1);
        double[][] target = {{47.01,88.9,421.231}, {47.01,88.9,421.231}, {86.4, 13.32, 1512}};
        for (int i = 0; i < res.length; i++) {
            for (int j=0; j < res[i].length; j++) {
                Assert.assertEquals(res[i][j], target[i][j], 1e-6);
            }
        }
    }

    @Test
    public void testElemwiseSub_matD() {
        final double[] line3 = {1.34, 54.34, 321.12};
        final double[] line4 = {43.2, 6.66, 756};
        final double[] line1 = {45.67, 34.56, 100.111};
        final double[][] matrix = {line1, line3, line4};
        final double[][] matrix1 = {line3, line1, line4};
        double[][] res = MathExt.elemwiseSub(matrix, matrix1);
        double[][] target = {{44.33,-19.78,-221.009}, {-44.33,19.78,221.009}, {0.0,0.0,0.0}};
        for (int i = 0; i < res.length; i++) {
            for (int j=0; j < res[i].length; j++) {
                Assert.assertEquals(res[i][j], target[i][j], 1e-6);
            }
        }
    }

    @Test
    public void testElemwiseSub_arrD() {
        final double[] line3 = {1.34, 54.34, 321.12};
        final double[] line1 = {45.67, 34.56, 100.111};
        double[] res = MathExt.elemwiseSub(line1, line3);
        double[] target = {44.33,-19.78,-221.009};
        for (int i = 0; i < res.length; i++) {
            Assert.assertEquals(res[i], target[i], 1e-6);
        }
    }

    @Test
    public void testElemwiseAdd_matI() {
        final int[] line2 = {1, 2, 3};
        final int[] line5 = {2, 2, 30};
        final int[] line6 = {1, 20, 3};
        final int[][] matrix2 = {line2, line5, line6};
        final int[][] matrix3 = {line5, line2, line6};
        int[][] res = MathExt.elemwiseAdd(matrix2, matrix3);
        int[][] target = {{3, 4, 33}, {3, 4, 33}, {2, 40, 6}};
        for (int i = 0; i < res.length; i++) {
            for (int j=0; j < res[i].length; j++) {
                Assert.assertEquals(res[i][j], target[i][j], 1e-6);
            }
        }
    }

    @Test
    public void testElemwiseAdd_arrD() {
        final double[] line1 = {45.67, 34.56, 100.111};
        final double[] line3 = {1.34, 54.34, 321.12};
        double[] res = MathExt.elemwiseAdd(line1, line3);
        double[] target = {47.01, 88.90, 421.231};
        for (int i = 0; i < res.length; i++) {
            Assert.assertEquals(res[i], target[i], 1e-6);
        }
    }

    @Test
    public void testElemwiseAdd_arrI() {
        final int[] line5 = {2, 2, 30};
        final int[] line6 = {1, 20, 3};
        int[] res = MathExt.elemwiseAdd(line5, line6);
        int[] target = {3, 22, 33};
        for (int i = 0; i < res.length; i++) {
            assertEquals(res[i],target[i]);
        }
    }

    @Test
    public void testElemwiseMul_arrD() {
        double[] line5 = {2.0, 2.0, 3.0};
        double[] line6 = {1.0, 2.0, 3.0};
        double[] res = MathExt.elemwiseMul(line5, line6);
        double[] target = {2.0, 4.0, 9.0};
        for (int i = 0; i < res.length; i++) {
            assertEquals(res[i],target[i], 1e-6);
        }
    }

    @Test
    public void testElemwiseMul_matD() {
        double[][] m1 = {{2.0, 2.0}, {1.0, 4.0}};
        double[][] m2 = {{1.0, 2.0}, {2.0, 4.0}};
        double[][] res = MathExt.elemwiseMul(m1, m2);
        double[][] target = {{2.0, 4.0}, {2.0, 16.0}};
        for (int i = 0; i < res.length; i++) {
            for (int j=0; j < res[i].length; j++) {
                Assert.assertEquals(res[i][j], target[i][j], 1e-6);
            }
        }
    }

    @Test
    public void testElemwiseMul_matDI() {
        double[][] m1 = {{2.0, 2.0}, {1.0, 4.0}};
        int[][] m2 = {{1, 2}, {2, 4}};
        double[][] res = MathExt.elemwiseMul(m1, m2);
        double[][] target = {{2.0, 4.0}, {2.0, 16.0}};
        for (int i = 0; i < res.length; i++) {
            for (int j=0; j < res[i].length; j++) {
                Assert.assertEquals(res[i][j], target[i][j], 1e-6);
            }
        }
    }

    @Test
    public void testelemwiseInvMul_matD() {
        final double[] line1 = {45.67, 34.56, 100.111};
        final double[] line3 = {1.34, 54.34, 321.12};
        final double[] line4 = {43.2, 6.66, 756};
        final double[][] matrix = {line1, line3, line4};
        final double[][] matrix1 = {line3, line1, line4};
        double[][] res = MathExt.elemwiseInvMul(matrix, matrix1);
        double[][] target = {
                {34.082089552238806,0.6359955833640044,0.31175572994519185},
                {0.029340924020144515,1.5723379629629628,3.2076395201326524},
                {1.0, 1.0, 1.0}
        };
        for (int i = 0; i < res.length; i++) {
            for (int j=0; j < res[i].length; j++) {
                Assert.assertEquals(res[i][j], target[i][j], 1e-6);
            }
        }
    }

    @Test
    public void testelemwiseInvMul_matDI() {
        final double[] line1 = {45.67, 34.56, 100.111};
        final int[] line2 = {1, 2, 3};
        final double[] line3 = {1.34, 54.34, 321.12};
        final double[] line4 = {43.2, 6.66, 756};
        final int[] line5 = {2, 2, 30};
        final int[] line6 = {1, 20, 3};
        final double[][] matrix = {line1, line3, line4};
        final int[][] matrix2 = {line2, line5, line6};
        double[][] res = MathExt.elemwiseInvMul(matrix, matrix2);
        double[][] target = {
                {45.67,17.28,33.370333333333335},
                {0.67,27.17,10.704},
                {43.2,0.333,252.0}
        };
        for (int i = 0; i < res.length; i++) {
            for (int j=0; j < res[i].length; j++) {
                Assert.assertEquals(res[i][j], target[i][j], 1e-6);
            }
        }
    }
    @Test
    public void testelemwiseInvMul_arrD() {
        final double[] line1 = {45.67, 34.56, 100.111};
        final double[] line3 = {1.34, 54.34, 321.12};
        double[] res = MathExt.elemwiseInvMul(line1, line3);
        double[] target = {34.082089552238806,0.6359955833640044,0.31175572994519185};
        for (int i = 0; i < res.length; i++) {
            Assert.assertEquals(res[i], target[i], 1e-6);
        }
    }

    @Test
    public void testelemwiseInvMul_arrL_l() {
        final long[] l1 = {2000000L, 3000000L, 4000000L, 5000000L};
        final long l0 = 1000000L;
        double[] res = MathExt.elemwiseInvMul(l1, l0);
        double[] target = {2.0,3.0,4.0,5.0};
        for (int i = 0; i < res.length; i++) {
            Assert.assertEquals(res[i], target[i], 1e-6);
        }
    }

    @Test
    public void testelemwiseInvMul_arrD_d() {
        double[] line1 = {45.67, 34.56, 100.111};
        double w1 = 12.34;
        double[] res = MathExt.elemwiseInvMul(line1, w1);
        double[] target = {3.70097244732577,2.80064829821718,8.112722852512157};
        for (int i = 0; i < res.length; i++) {
            Assert.assertEquals(res[i], target[i], 1e-6);
        }
    }

    @Test
    public void testelemwiseInvMul_arrD_i() {
        double[] line1 = {45.67, 34.56, 100.111};
        int i1 = 1;
        double[] res = MathExt.elemwiseInvMul(line1, i1);
        double[] target = {45.67, 34.56, 100.111};
        for (int i = 0; i < res.length; i++) {
            Assert.assertEquals(res[i], target[i], 1e-6);
        }
    }

    @Test
    public void testelemwiseInvMul_arrDI() {
        double[] line1 = {45.67, 34.56, 100.111};
        int[] line5 = {2, 2, 30};
        double[] res = MathExt.elemwiseInvMul(line1, line5);
        double[] target = {22.835,17.28,3.3370333333333333};
        for (int i = 0; i < res.length; i++) {
            Assert.assertEquals(res[i], target[i], 1e-6);
        }
    }


    @Test
    public void testMatrixMul() {
        final double[] line1 = {45.67, 34.56, 100.111};
        final double[] w2 = {23.22, 3.111, 9.9};
        final double[] line3 = {1.34, 54.34, 321.12};
        final double[] line4 = {43.2, 6.66, 756};
        final double[][] matrix = {line1, line3, line4};
        double[] res = MathExt.matrixMul(matrix, w2);
        double[] target = {2159.07246, 3379.25454, 8508.22326};
        for (int i = 0; i < res.length; i++) {
            Assert.assertEquals(res[i], target[i], 1e-6);
        }
    }

    @Test
    public void testMatrixMul1() {
        final double[] line1 = {45.67, 34.56, 100.111};
        final double[] line3 = {1.34, 54.34, 321.12};
        final double[] line4 = {43.2, 6.66, 756};
        final double[][] matrix = {line1, line3, line4};
        final double[][] matrix1 = {line3, line1, line4};
        double[][] res = MathExt.matrixMul(matrix, matrix1);
        double[][] target = {{5964.3482, 4342.84066, 93809.30256}, {16355.8874, 4089.4652, 248637.05254}, {33021.2502, 7612.6176, 586075.12326}};
        for (int i = 0; i < res.length; i++) {
            for (int j = 0; j < res[i].length; j++ ){
                Assert.assertEquals(res[i], target[i], 1e-6);
            }
        }
    }

    @Test
    public void testMatrixMul2() {
        final double[] line1 = {45.67, 34.56, 100.111};
        final double[] line3 = {1.34, 54.34, 321.12};
        final double[] line4 = {43.2, 6.66, 756};
        final double[][] matrix = {line1, line3, line4};
        final double[][] matrix1 = {line3, line1, line4};
        SimpleMatrix sm1 = new SimpleMatrix(matrix);
        SimpleMatrix sm2 = new SimpleMatrix(matrix1);
        double[][] res = MathExt.matrixMul(sm1, sm2);
        double[][] target = {{5964.3482, 4342.84066, 93809.30256}, {16355.8874, 4089.4652, 248637.05254}, {33021.2502, 7612.6176, 586075.12326}};
        for (int i = 0; i < res.length; i++) {
            for (int j = 0; j < res[i].length; j++ ){
                Assert.assertEquals(res[i], target[i], 1e-6);
            }
        }
    }



    @Test
    public void testForward1_1() {
        final double[] line1 = {45.67, 34.56, 100.111};
        final double[] line3 = {1.34, 54.34, 321.12};
        final double[] line4 = {43.2, 6.66, 756};
        final double[][] matrix = {line1, line3, line4};
        final double[] w3 = {23.22, 3.111, 9.9, 2.1};
        double[] res = MathExt.forward1(matrix, w3);
        double[] target = {2161.17246, 3381.35454, 8510.32326};
        for (int i = 0; i < res.length; i++) {
            Assert.assertEquals(res[i], target[i], 1e-6);
        }
    }

    @Test
    public void testForward_1() {
        final double[] line1 = {45.67, 34.56, 100.111};
        final double[] line3 = {1.34, 54.34, 321.12};
        final double[] line4 = {43.2, 6.66, 756};
        final double[][] matrix = {line1, line3, line4};
        final double[] w3 = {23.22, 3.111, 9.9, 2.1};
        double[] res = MathExt.forward(matrix, w3);
        double[] target = {2161.17246, 3381.35454, 8510.32326};
        for (int i = 0; i < res.length; i++) {
            Assert.assertEquals(res[i], target[i], 1e-6);
        }
    }

    @Test
    public void testForward_2() {
        final double[] line1 = {45.67, 34.56, 100.111};
        final double[] line3 = {1.34, 54.34, 321.12};
        final double[] line4 = {43.2, 6.66, 756};
        final double[][] matrix = {line1, line3, line4};
        final double[][] w4 = {{23.22, 3.111, 9.9, 2.1}, {2.3, 4.5, 6.7, 9.0}, {12.23, 1.1, 282, 22.01}};
        double[][] res = MathExt.forward(matrix, w4);
        double[][] target = {{2161.17246, 940.3047, 28849.8721}, {3381.35454, 2408.116, 90654.0122}, {8510.32326, 5203.53, 213749.672}};
        for (int i = 0; i < res.length; i++) {
            for (int j = 0; j < res[i].length; j++ ){
                Assert.assertEquals(res[i], target[i], 1e-6);
            }
        }
    }

    @Test
    public void testForward_3() {
        final double[] line1 = {45.67, 34.56, 100.111};
        final double[] w3 = {23.22, 3.111, 9.9, 2.1};
        double res = MathExt.forward(line1, w3);
        double target = 2161.17246;
        Assert.assertEquals(res, target, 1e-6);
    }

    @Test
    public void testForward3() {
        final double[] line1 = {45.67, 34.56, 100.111};
        final double[] line3 = {1.34, 54.34, 321.12};
        final double[] line4 = {43.2, 6.66, 756};
        final double[][] matrix = {line1, line3, line4};
        final double[][] matrix1 = {line3, line1, line4};
        double[][] res = MathExt.forward(matrix, matrix1);
        double[][] target = {
                {34407.95252, 13402.465821, 78643.0296},
                {106393.8056, 34186.94352, 243942.5124},
                {243507.6324, 77987.1406, 574202.5956}
        };
        for (int i = 0; i < res.length; i++) {
            for (int j = 0; j < res[i].length; j++ ){
                Assert.assertEquals(res[i], target[i], 1e-6);
            }
        }

    }


    @Test
    public void testAvgWeight() {
        final double[] w2 = {23.22, 3.111, 9.9};
        double res = MathExt.avgWeight(w2);
        Assert.assertEquals(res, 12.077, 1e-6);
    }

    @Test
    public void testAverage() {
        final double[] w2 = {23.22, 3.111, 9.9};
        double res = MathExt.average(w2);
        Assert.assertEquals(res, 12.077, 1e-6);

        double[] dd = {};
        double res1 = MathExt.average(dd);
        assertEquals(res1, Double.NaN);
    }



    @Test
    public void testAverage1() {
        String[] dd = {};
        String[] dd1 = {"12.3234", "2342.3431", "12.12"};
        double res = MathExt.average(dd);
        assertEquals(res, Double.NaN);
        double res1 = MathExt.average(dd1);
        Assert.assertEquals(res1, 788.9288333333333, 1e-6);
    }

    @Test
    public void testAverage_Enc() {
        // 密钥
        PaillierKeyPublic keyPublic = null;
        String privateKeyString;
        // 生成密钥
        PaillierPrivateKey privateKey = PaillierPrivateKey.create(1024);
        // 待加密信息
        Vector.Builder vectorOrBuilder = Vector.newBuilder();
        double[] yLabel = {1.0, 0.0, 0.0, 1.0};
        for (double v : yLabel) {
            vectorOrBuilder.addValues(v);
        }
        Vector yVec = vectorOrBuilder.build();
        // 定义求平均区间
        int start = 0;
        int end = 2;
        // 加密
        ArrayList<EncryptedNumber> yEnc = new ArrayList<>(Arrays.asList(DataUtils.PaillierEncrypt(yVec, privateKey.getPublicKey(), false)));
        EncryptedNumber[] encryptedNumber = new EncryptedNumber[yEnc.size()];
        for (int j = 0; j < yEnc.size(); j++) {
            encryptedNumber[j] = yEnc.get(j);
        }
        EncryptedNumber res = MathExt.average(encryptedNumber, start, end);
        double decrypted_res = privateKey.decrypt(res).decodeDouble();
        // expected
        double sum = 0.0;
        for (int i = start; i < end; i++) {
            sum += yLabel[i];
        }
        // 解密后平均
        double target = sum/(end-start);
        Assert.assertEquals(decrypted_res, target, 1e-6);

    }


    @Test
    public void testCipherAverage() {
        // 加密准备工作
        EncryptionTool encryptionTool;
        Ciphertext[] encryptDataNew;
        PrivateKey privateKey;
        PublicKey publicKey;
        encryptionTool = new JavallierTool();
        // 待加密数组
        double[] yLabel = {1.0, 1.0, 0.0, 1.0};
        // 加密前准备
        encryptDataNew = new JavallierCiphertext[yLabel.length];
        privateKey =  encryptionTool.keyGenerate(1024, 0);
        publicKey = privateKey.generatePublicKey();
        // 加密过程
        for (int i = 0; i<yLabel.length; i++) {
            encryptDataNew[i] = encryptionTool.encrypt(yLabel[i],privateKey.generatePublicKey());
        }
        // 定义求平均区间
        int start = 0;
        int end = 2;
        // 加密结果
        Ciphertext res = MathExt.average(encryptDataNew, start, end, publicKey, encryptionTool);
        // 解密
        double decrypted_res;
        decrypted_res = encryptionTool.decrypt(res, privateKey);
        double sum = 0.0;
        for (int i = start; i < end; i++) {
            sum += yLabel[i];
        }
        // 解密后平均
        double target = sum/(end-start);
        Assert.assertEquals(decrypted_res, target, 1e-6);
    }

    @Test
    public void testMedian() {
        String[] dd1 = {"12.3234", "2342.3431", "12.12"};
        String[] dd2 = {"12.3234", "2342.3431", "12.12", "234.1"};
        double res = MathExt.median(dd1);
        Assert.assertEquals(res, 12.3234, 1e-6);

        double res1 = MathExt.median(dd2);
        Assert.assertEquals(res1, 123.2117, 1e-6);
    }

    @Test
    public void testMedian1() {
        double[] line1 = {45.67, 34.56, 100.111};
        double res = MathExt.median(line1);
        Assert.assertEquals(res, 45.67, 1e-6);
        double[] w3 = {23.22, 3.111, 9.9, 2.1};
        double res1 = MathExt.median(w3);
        Assert.assertEquals(res1, 6.5055, 1e-6);
    }

    @Test
    public void testIsNumeric() {
        String[] str1 = {"2323.232", "2323E-5", "2323E5", "2323f-5", "+5", "-122.12", ""};
        boolean[] res1 = {true, true, true, false, true, true, false};
        for (int i = 0; i < str1.length; i++) {
            boolean res = MathExt.isNumeric(str1[i]);
            assertEquals(res, res1[i]);
        }
    }

    @Test
    public void testSum() {
        double[] line1 = {45.67, 34.56, 100.111};
        double res = MathExt.sum(line1);
        Assert.assertEquals(res, 180.341, 1e-6);
    }

    @Test
    public void testSum1() {
        List<Double> res1 = new ArrayList<Double>();
        res1.add(123.23);
        res1.add(23.32);
        res1.add(-23.32);
        double res2 = MathExt.sum(res1);
        double target = 123.23;
        Assert.assertEquals(res2, 123.23, 1e-6);

    }

    @Test
    public void testRound() {
        double a = 2323.23259078;
        int b = 3;
        double res = MathExt.round(a, b);
        Assert.assertEquals(res, 2323.233, 1e-6);

    }

    @Test
    public void testMerge() {
        int[] a = {5, 2, 3, 1, 4};
        double[] b = {1.2, 3.4, 3.2, 22, 2.012};
        double[][] res = MathExt.merge(a, b);
        double[][] target = {{5.0, 1.2}, {2.0, 3.4}, {3.0, 3.2}, {1.0, 22.0}, {4.0, 2.012}};
        for (int i = 0; i < res.length; i++) {
            for (int j = 0; j < res[i].length; j++ ){
                Assert.assertEquals(res[i][j], target[i][j], 1e-6);
            }
        }
    }

    @Test
    public void testMerge1() {
        long[] a = {51111111, 2, 3, 132131231, 6};
        double[] b = {1.2, 3.4, 3.2, 22, 2.012};
        double[][] res = MathExt.merge(a, b);
        double[][] target = {{5.1111111E7, 1.2}, {2.0, 3.4}, {3.0, 3.2}, {1.32131231E8, 22.0}, {6.0, 2.012}};
        for (int i = 0; i < res.length; i++) {
            for (int j = 0; j < res[i].length; j++ ){
                Assert.assertEquals(res[i][j], target[i][j], 1e-6);

            }
        }
    }

    @Test
    public void testMerge2() {
        int a = 66;
        String[] b = {"51111", "2", "3", "132131231", "62313"};
        String[] res = MathExt.merge(a, b);
        String[] target = {"66", "51111", "2", "3", "132131231", "62313"};
        assertEquals(res, target);
    }

    @Test
    public void testDiffSet() {
        int[] a = {5, 2, 3, 1, 4};
        int[] b = {25, 1, 31, 3, 8};
        int[] res = MathExt.diffSet(a, b);
        int[] target = {5, 2, 4};
        assertEquals(res, target);
    }

    @Test
    public void testDiffSet_h() {
        int[] a = {5, 2, 3, 1, 4};
        int[] b = {25, 1, 31, 3, 8};
        int[] res = MathExt.diffSet2(a, b);
        int[] target = {5, 2, 4};
        assertEquals(res, target);
    }

    @Test
    public void testDiffSet_l() {
        long[] a = {5L, 2L, 3L, 1L, 4L};
        long[] b = {25L, 1L, 31L, 3L, 8L};
        long[] res = MathExt.diffSet(a, b);
        long[] target = {5L, 2L, 4L};
        assertEquals(res, target);
    }

    @Test
    public void testTranspose() {
        String[][] str = {{"2323.232", "4", "2"}, {"56.4", "43", "1"}};
        String[][] res = MathExt.transpose(str);
        String[][] target = {{"2323.232", "56.4"}, {"4", "43"}, {"2", "1"}};
        for (int i = 0; i < str.length; i++) {
            assertEquals(res[i], target[i]);
        }
    }

    @Test
    public void testTranspose1() {
        double[][] str = {{2323.232, 4.0, 2}, {56.4, 43.1, 1.21}};
        double[][] res = MathExt.transpose(str);
        double[][] target = {{2323.232, 56.4}, {4.0, 43.1}, {2, 1.21}};
        for (int i = 0; i < res.length; i++) {
            for (int j = 0; j < res[i].length; j++ ){
                Assert.assertEquals(res[i][j], target[i][j], 1e-6);

            }
        }
    }

    @Test
    public void testTranspose2() {
        BigDecimal b1 = new BigDecimal(1);
        BigDecimal b2 = new BigDecimal(2);
        BigDecimal b3 = new BigDecimal(3);
        BigDecimal[][] bd = {{b1, b2, b3}, {b3, b2, b1}};
        BigDecimal[][] res = MathExt.transpose(bd);
        BigDecimal[][] target = {{b1, b3}, {b2, b2}, {b3, b1}};
        for (int i = 0; i < res.length; i++) {
            for (int j = 0; j < res[i].length; j++ ){
                Assert.assertEquals(res[i][j], target[i][j]);
            }
        }
    }

    @Test
    public void testStandardDeviation() {
        double[] a = {1.0, 2.0, 3.0};
        double res = MathExt.standardDeviation(a);
        double target = 0.816496580927726;
        Assert.assertEquals(res, target, 1e-6);

    }

    @Test
    public void testMaxIndex() {
        double[] a = {1.0, 2.0, 3.0};
        int res = MathExt.maxIndex(a);
        int target = 2;
        Assert.assertEquals(res, target);

    }



    @Test
    public void testMax() {
        String[] a = {"45.67", "34.56", "100.111"};
        double res = MathExt.max(a);
        Assert.assertEquals(res, 100.111, 1e-6);

    }

    @Test
    public void testBigMax() {
        BigInteger[] b = new BigInteger[]{new BigInteger("-2"), new BigInteger("10"), new BigInteger("-30")};
        BigInteger res = MathExt.max(b);
        Assert.assertEquals(res, new BigInteger("10"));
    }

    @Test
    public void testMax1() {
        double[] a = {45.67, 34.56, 100.111};
        double res = MathExt.max(a);
        Assert.assertEquals(res, 100.111, 1e-6);

    }

    @Test
    public void testMax2() {
        int[] a = {-450, 34, 100};
        int res = MathExt.max(a);
        Assert.assertEquals(res, 100, 1e-6);

    }

    @Test
    public void testMin() {
        String[] a = {"45.67", "34.56", "100.111"};
        double res = MathExt.min(a);
        Assert.assertEquals(res, 34.56, 1e-6);
    }


    @Test
    public void testMin1() {
        double[] a = {45.67, 34.56, 100.111};
        double res = MathExt.min(a);
        Assert.assertEquals(res, 34.56, 1e-6);

    }

    @Test
    public void testMin2() {
        int[] a = {-450, 34, 100};
        int res = MathExt.min(a);
        Assert.assertEquals(res, -450, 1e-6);

    }

    @Test
    public void testCombination_2_d() {
        double[] nums = {12.3, 12.1, 5.43};
        List<Tuple2<Double, Double>> res = new ArrayList<>();
        res = MathExt.combination_2(nums);
        List<Tuple2<Double, Double>> target = new ArrayList<>();
        target.add(new Tuple2(12.3, 12.3));
        target.add(new Tuple2(12.3, 12.1));
        target.add(new Tuple2(12.3, 5.43));
        target.add(new Tuple2(12.1, 12.1));
        target.add(new Tuple2(12.1, 5.43));
        target.add(new Tuple2(5.43, 5.43));
        for (int i = 0; i < res.size(); i++) {
            assertEquals(res.get(i).toString(), target.get(i).toString());
        }
    }

    @Test
    public void testCombination_2_s() {
        String[] nums = {"12.3", "12.1", "5.43"};
        List<Tuple2<String, String>> res = new ArrayList<>();
        res = MathExt.combination_2(nums);
        List<Tuple2<String, String>> target = new ArrayList<>();
        target.add(new Tuple2("12.3", "12.3"));
        target.add(new Tuple2("12.3", "12.1"));
        target.add(new Tuple2("12.3", "5.43"));
        target.add(new Tuple2("12.1", "12.1"));
        target.add(new Tuple2("12.1", "5.43"));
        target.add(new Tuple2("5.43", "5.43"));
        for (int i = 0; i < res.size(); i++) {
            assertEquals(res.get(i).toString(), target.get(i).toString());
        }
    }

    @Test
    public void testCombination_2_i() {
        Integer[] nums = {12, 12, 5};
        List<Tuple2<Integer, Integer>> res = new ArrayList<>();
        res = MathExt.combination_2(nums);
        List<Tuple2<Integer, Integer>> target = new ArrayList<>();
        target.add(new Tuple2(12, 12));
        target.add(new Tuple2(12, 12));
        target.add(new Tuple2(12, 5));
        target.add(new Tuple2(12, 12));
        target.add(new Tuple2(12, 5));
        target.add(new Tuple2(5, 5));
        for (int i = 0; i < res.size(); i++) {
            assertEquals(res.get(i).toString(), target.get(i).toString());
        }
    }

    @Test
    public void testArraySub() {
        double[] a = {2.1, 3.2, 1.0};
        double[] b = {23.11, -23.2, 1.3432};
        double[] res = MathExt.arraySub(b, a);
        double[] target = {21.01, -26.4, 0.3432};
        for (int i = 0; i < res.length; i++) {
            Assert.assertEquals(res[i],target[i], 1e-6);
        }
    }

    @Test
    public void testArraySub1() {
        double[][] str1 = {{2323.232, 4.0, 2}, {56.4, 43.1, 1.21}};
        double[][] str2 = {{2.23, 4.23, 3.123}, {0.4, -213, 82.123}};
        double[][] res = MathExt.arraySub(str1, str2);

        double[][] target = {{2321.002, -0.23, -1.123}, {56.0, 256.1,- 80.913}};
        for (int i = 0; i < res.length; i++) {
            for (int j = 0; j < res[i].length; j++ ){
                Assert.assertEquals(res[i][j],target[i][j], 1e-6);
            }
        }
    }

    @Test
    public void testSigmod() {
        double[] a = {2.1, 3.2, 1.0};
        double[] res = MathExt.sigmod(a);
        double[] target = {0.8909031788043871, 0.9608342772032357, 0.7310585786300049};
        for (int i = 0; i < res.length; i++) {
            Assert.assertEquals(res[i],target[i], 1e-6);
        }
    }

    @Test
    public void testSigmod1() {
        double[][] a = {{2323.232, 4.0, 2}, {56.4, -1, 0}};
        double[][] res = MathExt.sigmod(a);
        double[][] target = {{1.0, 0.9820137900379085, 0.8807970779778823}, {1.0, 0.2689414213699951, 0.5}};
        for (int i = 0; i < res.length; i++) {
            for (int j = 0; j < res[i].length; j++ ){
                Assert.assertEquals(res[i][j],target[i][j], 1e-6);
            }
        }
    }

    @Test
    public void testSoftmax() {
        double[] a = {2.1, 3.2, 1.0};
        double[] res = MathExt.softmax(a);
        double[] target = {0.23057215679279945, 0.6926770395049779, 0.07675080370222267};
        for (int i = 0; i < res.length; i++) {
            Assert.assertEquals(res[i],target[i], 1e-6);

        }
    }

    @Test
    public void testSoftmax1() {
        double[][] a = {{2323.232, 4.0, 2}, {56.4, 43.1, 1.21}};
        double[][] res = MathExt.softmax(a);
        double[][] target = {{1.0, 0.0, 0.0}, {0.9999983255095944, 1.6744904055114587E-6, 1.0746989301370271E-24}};
        for (int i = 0; i < res.length; i++) {
            for (int j = 0; j < res[i].length; j++ ){
                Assert.assertEquals(res[i][j],target[i][j], 1e-6);
            }
        }
    }
    @Test
    public void testBinarySearch() {
        double[] a = {1, 2, 3, 4};
        double b = 2;
        int res = MathExt.binarySearch(a, b);
        int target = 1;
        Assert.assertEquals(res,target);

    }
    @Test
    public void testBinarySearch2() {
        String[] a = {"1", "2", "3", "4"};
        String b = "2";
        int res = MathExt.binarySearch(b, a);
        int target = 1;
        Assert.assertEquals(res,target);

    }

    @Test
    public void trans2DtoArray(){
        double[][] data = new double[][]{{0.2,0.5},{0.6,0.1}};
        double[] res = MathExt.trans2DtoArray(data);
        System.out.println("res : " + Arrays.toString(res));
        double[] array = new double[]{0.2,0.6,0.5,0.1};
        Assert.assertEquals(res,array);
    }


    @Test
    public void testGenerateNormal(){
        int m =3;
        int n =2;
        double scale=0.001;
        double[][] res = generateNormal(m,n,scale);
        System.out.println("res : " + Arrays.deepToString(res));
        double[][] normal = new double[][]{{0.08452060657049848, 0.09128761787534406}, {-0.028707863647499533, 0.07518594314874759}, {0.1335473668231534, -0.09499789372646104}};
        Assert.assertEquals(res.length,normal.length);
    }

    @Test
    public void testGenerateUniform(){
        int m = 3;
        double[] res = generateUniform(m);
        System.out.println("res: " + Arrays.toString(res));
        double[] uniform = new double[]{4.591117485041855, 4.707171442994805, 2.188494408434079};
        Assert.assertEquals(res.length,uniform.length);
    }
}

