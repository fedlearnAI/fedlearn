package com.jdt.fedlearn.core.type;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class OptimizerTypeTest {
    private final OptimizerType[] optimizerTypes = {OptimizerType.NEWTON,OptimizerType.BFGS, OptimizerType.BatchGD,OptimizerType.StochasticGD};
    @Test
    public void testGetOptimizer() {
        String[] target = {"NEWTON","bfgs","batchGD","StochasticGD"};
        for (int i = 0;i < target.length;i++){
            assertEquals(optimizerTypes[i].getOptimizer(),target[i]);
        }
    }
}