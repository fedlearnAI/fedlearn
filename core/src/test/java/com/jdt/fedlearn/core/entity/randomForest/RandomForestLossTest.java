package com.jdt.fedlearn.core.entity.randomForest;

import org.ejml.simple.SimpleMatrix;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class RandomForestLossTest {

    @Test
    public void testBagging() {
        RandomForestLoss loss = new RandomForestLoss("Regression:MSE");
        double[][] y = {{1.0},{0.0}};
        SimpleMatrix label = new SimpleMatrix(y);
        double res = loss.bagging(label);
        assertEquals(res, 0.5);
        try {
            new RandomForestLoss("");
        } catch (Exception e) {
            assertEquals(e.getMessage(), "Unsupported loss type!");
        }
    }

    @Test
    public void testGetLossTypeId() {
        RandomForestLoss loss = new RandomForestLoss("Regression:MSE");
        double res = loss.getLossTypeId();
        assertEquals(1.0, res);
    }
}