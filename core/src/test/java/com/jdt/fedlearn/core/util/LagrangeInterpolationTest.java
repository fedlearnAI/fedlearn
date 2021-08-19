package com.jdt.fedlearn.core.util;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.math.BigInteger;
import java.util.Arrays;

import static org.testng.Assert.*;

public class LagrangeInterpolationTest {

    @Test
    public void testGenerateCoefficients() {
        LagrangeInterpolation li = new LagrangeInterpolation(new double[]{-2.0, -3.0, -1.0});
        double[] result = li.generateBigCoefficients();
        double[] target = new double[]{1.0, -6.0, 11.0, -6.0};
        for (int i = 0; i < target.length; i++) {
            Assert.assertEquals(result[i], target[i]);
        }
    }

}