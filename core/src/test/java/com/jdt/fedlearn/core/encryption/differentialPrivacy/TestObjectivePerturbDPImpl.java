package com.jdt.fedlearn.core.encryption.differentialPrivacy;

import com.jdt.fedlearn.core.type.DifferentialPrivacyType;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestObjectivePerturbDPImpl {

    @Test
    public void testGenerateNoises(){
        IDifferentialPrivacy dp = DifferentialPrivacyFactory.createDifferentialPrivacy(DifferentialPrivacyType.OBJECTIVE_PERTURB);
        dp.init(3, 100, 1000, 0.2, 1e-8, 0.1, 0.1, 666);
        dp.generateNoises();
        double[] noises = dp.getNoises();
        Assert.assertEquals(noises.length, 3);
        for(double noise: noises){
            System.out.println(noise);
        }
        dp.init(3, 100, 1000, 0, 0, 0 ,0, 666);
        dp.generateNoises();
        noises = dp.getNoises();
        Assert.assertEquals(noises.length, 3);
    }

    @Test
    public void testAddNoises(){
        IDifferentialPrivacy dp = DifferentialPrivacyFactory.createDifferentialPrivacy(DifferentialPrivacyType.OBJECTIVE_PERTURB);
        double[] origin = new double[]{0.2, 0.5, 0.6};
        dp.init(origin.length, 100, 1000, 0.2, 1e-8, 0.1, 0.1, 666);
        dp.generateNoises();
        double[] noises = dp.getNoises();
        Assert.assertEquals(noises.length, origin.length);
        for(double noise: noises){
            System.out.println(noise);
        }
        dp.addNoises(origin, origin);
        origin = new double[0];
        dp.init(origin.length, 100, 1000, 0.2, 1e-8, 0.1, 0.1, 666);
        dp.generateNoises();
        noises = dp.getNoises();
        Assert.assertEquals(noises.length, origin.length);
        dp.addNoises(origin, origin);
    }

    @Test
    public void testAdd2DNoises(){
        IDifferentialPrivacy dp = DifferentialPrivacyFactory.createDifferentialPrivacy(DifferentialPrivacyType.OBJECTIVE_PERTURB);
        dp.init(6, 100, 1000, 0.2, 1e-8, 0.1, 0.1, 666);
        dp.generateNoises();
        double[] noises = dp.getNoises();
        Assert.assertEquals(noises.length, 6);
        for(double noise: noises){
            System.out.println(noise);
        }
        double[][] origin = new double[][]{{1}, {1}};
        double[][] copy = new double[][]{{1}, {1}};
        int index = 1;
        dp.addNoises(origin, origin, index);
        for(int i = 0; i < origin.length; i++){
            for(int j = 0; j < origin[i].length; j++){
                System.out.println(origin[i][j]);
                Assert.assertEquals(origin[i][j], copy[i][j] - noises[index * origin.length + i] / 1000);
            }
        }
    }
}
