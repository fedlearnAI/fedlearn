package com.jdt.fedlearn.core.encryption.differentialPrivacy;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestExponentialDP {

    @Test
    public void testExponentialNoise(){
        int shape = 3;
        double sigma = 0.3;
        double[] noises = Exponential.getExponentialMechanismNoise(shape, sigma);
        Assert.assertEquals(noises.length, shape);
        for(double noise: noises){
            System.out.println(noise);
        }
        shape = 0;
        noises = Exponential.getExponentialMechanismNoise(shape, sigma);
        Assert.assertEquals(noises.length, 0);
    }

    @Test
    public void testExponential(){
        double[] values = new double[]{0.2, 2.4, 5.0, 38.0, 34.0, 12.0, 14.0, 16.7, -0.44, -0.47, -0.44, -0.47};
        for(int i = 0; i < 10; i++){
            int index = Exponential.exponentialMechanismIndex(values, 1.5, 0.69);
            System.out.println(index);
        }
        int index = Exponential.exponentialMechanismIndex(values, 0, 0.69);
        Assert.assertEquals(index, 3);
        double[] emptyValues = new double[0];
        index = Exponential.exponentialMechanismIndex(emptyValues, 0, 0.69);
        Assert.assertEquals(index, -1);

    }

}
