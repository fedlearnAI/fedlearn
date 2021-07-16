package com.jdt.fedlearn.core.encryption.differentialPrivacy;

import org.testng.annotations.Test;

public class TestLaplace {

    @Test
    public void laplaceMechanismNoise(){
        double x = Laplace.laplaceMechanismNoise(1, 0.1);
        System.out.println(x);
    }
}