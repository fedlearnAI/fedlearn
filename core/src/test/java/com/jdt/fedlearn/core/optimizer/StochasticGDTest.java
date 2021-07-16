package com.jdt.fedlearn.core.optimizer;

import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class StochasticGDTest {

    @Test
    public void testGetGlobalUpdate() {
        StochasticGD stochasticGD = new StochasticGD(0.1);
        double[] gredients = {0.1,0.2,0.3};
        double[] res = stochasticGD.getGlobalUpdate(gredients);
        double[] target = {-0.01,-0.02,-0.03};
        Assert.assertEquals(res, target , 1E-8);
    }

    @Test
    public void testTestGetGlobalUpdate() {
    }

    @Test
    public void testRandomChoose() {
    }
}