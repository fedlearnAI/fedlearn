package com.jdt.fedlearn.core.encryption.differentialPrivacy;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestGaussianDP {

    @Test
    public void testGaussian(){
        int shape = 5;
        double sigma = 0.1;
        double[] noises = GaussianDP.getGaussianMechanismNoise(shape, sigma, 666);
        Assert.assertEquals(noises.length, shape);
        for (double noise : noises) {
            System.out.println(noise);
        }
        shape = 0;
        noises = GaussianDP.getGaussianMechanismNoise(shape, sigma, 666);
        Assert.assertEquals(noises.length, 0);
    }

}
