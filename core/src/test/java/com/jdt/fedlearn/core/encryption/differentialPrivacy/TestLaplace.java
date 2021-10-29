package com.jdt.fedlearn.core.encryption.differentialPrivacy;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TestLaplace {

    @Test
    public void laplaceMechanismNoise(){
        double x = Laplace.laplaceMechanismNoise(1, 0.1);
        System.out.println(x);
        x = Laplace.laplaceMechanismNoise(0, 0);
        Assert.assertEquals(x, 0);
    }

    @Test
    public void laplaceMechanismNoiseV1(){
        double x = Laplace.laplaceMechanismNoiseV1(1.0 / 100.0, 0.1);
        System.out.println(x);
        x = Laplace.laplaceMechanismNoiseV1(1.0/100.0, 0);
        System.out.println(x);
        x = Laplace.laplaceMechanismNoiseV1(0, 0);
        Assert.assertEquals(x, 0);
    }
}