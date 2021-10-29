package com.jdt.fedlearn.core.encryption.differentialPrivacy;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestGammaDP {

    @Test
    public void testGamma(){
        int shape = 2;
        double sigma = 0.2;
        double[] noises = GammaDP.getGammaNoise(shape, sigma, 666);
        Assert.assertEquals(noises.length, shape);
        for(double noise: noises){
            System.out.println(noise);
        }
        shape = 0;
        noises = GammaDP.getGammaNoise(shape, sigma, 666);
        Assert.assertEquals(noises.length, 0);
    }
}
