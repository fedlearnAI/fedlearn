package com.jdt.fedlearn.core.encryption.differentialPrivacy;

import org.testng.annotations.Test;
import org.testng.Assert;

public class TestRandomDP {

    @Test
    public void testRandomizedResponseForNumeric(){
        double[][] data = new double[][]{{0.0, 1.0, 2.0}, {2.0, 1.0, 2.0}};
        double[][] res = RandomDP.randomizedResponseForNumeric(data, 0.1);
        double expEpsilon = Math.exp(0.1);
        double candidateValue = (expEpsilon + 1) / (expEpsilon - 1) * 3;
        for (double[] re : res) {
            for (double v : re) {
                if(v != 0){
                    Assert.assertEquals(Math.abs(v), candidateValue, 0.000001);
                }
            }
        }
        res = RandomDP.randomizedResponseForNumeric(data, 0);
    }

}