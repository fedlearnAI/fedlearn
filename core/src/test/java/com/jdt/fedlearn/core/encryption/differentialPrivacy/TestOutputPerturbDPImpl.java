package com.jdt.fedlearn.core.encryption.differentialPrivacy;

import com.jdt.fedlearn.core.type.DifferentialPrivacyType;
import org.jblas.DoubleMatrix;
import org.jblas.Solve;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestOutputPerturbDPImpl {

    @Test
    public void testGenerateNoises(){
        IDifferentialPrivacy dp = DifferentialPrivacyFactory.createDifferentialPrivacy(DifferentialPrivacyType.OUTPUT_PERTURB);
        dp.init(3, 100, 1000, 0.2, 1e-8, 0.1, 0.1, 666);
        dp.generateNoises();
        double[] noises = dp.getNoises();
        Assert.assertEquals(noises.length, 3);
        for(double noise: noises){
            System.out.println(noise);
        }
        dp.init(3, 0, 1000, 0, 0, 0, 0., 666);
        dp.generateNoises();
        noises = dp.getNoises();
        Assert.assertEquals(noises.length, 3);
    }

    @Test
    public void  testAddNoises(){
        IDifferentialPrivacy dp = DifferentialPrivacyFactory.createDifferentialPrivacy(DifferentialPrivacyType.OUTPUT_PERTURB);
        dp.init(3, 100, 1000, 0.2, 1e-8, 0.1, 0.1, 666);
        dp.generateNoises();
        double[] noises = dp.getNoises();
        Assert.assertEquals(noises.length, 3);
        for(double noise: noises){
            System.out.println(noise);
        }
        double[] origin = new double[]{0.2, 0.3, 0.4};
        double[] copy = origin.clone();
        dp.addNoises(origin, origin);
        for(int i = 0; i < origin.length; i++){
            System.out.println(origin[i]);
            Assert.assertEquals(origin[i], copy[i] + noises[i]);
        }
        // 多次添加应该和第一次相同，不应该相加多次
        dp.addNoises(origin, origin);
        for(int i = 0; i < origin.length; i++){
            System.out.println(origin[i]);
            Assert.assertEquals(origin[i], copy[i] + noises[i]);
        }
    }

    @Test
    public void testAdd2DNoises(){
        IDifferentialPrivacy dp = DifferentialPrivacyFactory.createDifferentialPrivacy(DifferentialPrivacyType.OUTPUT_PERTURB);
        dp.init(6, 100, 1000, 0.2, 1e-8, 0.1, 0.1, 666);
        dp.generateNoises();
        double[] noises = dp.getNoises();
        Assert.assertEquals(noises.length, 6);
        for(double noise: noises){
            System.out.println(noise);
        }
        double[][] origin = new double[][]{{3, 4}, {1,2}, {0,0}};
        double[][] copy = new double[][]{{3, 4}, {1,2}, {0,0}};
        dp.addNoises(origin, origin, 0);
        for(int i = 0; i < origin.length; i++){
            for(int j = 0; j < origin[i].length; j++){
                System.out.println(origin[i][j]);
                Assert.assertEquals(origin[i][j], copy[i][j] + noises[i * origin[i].length + j]);
            }
        }
        dp.addNoises(origin, origin, 0);
        for(int i = 0; i < origin.length; i++){
            for(int j = 0; j < origin[i].length; j++){
                System.out.println(origin[i][j]);
                Assert.assertEquals(origin[i][j], copy[i][j] + noises[i * origin[i].length + j]);
            }
        }
    }
}
