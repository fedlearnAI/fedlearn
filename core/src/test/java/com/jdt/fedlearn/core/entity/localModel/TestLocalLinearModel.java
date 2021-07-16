package com.jdt.fedlearn.core.entity.localModel;

import org.ejml.simple.SimpleMatrix;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;

public class TestLocalLinearModel {

    SimpleMatrix X;
    SimpleMatrix y;

    @BeforeMethod
    public void setUp() {
        int n = 10;
        int m = 10;
        X = new SimpleMatrix(n, m);
        y = new SimpleMatrix(m, 1);
        for (int i=0; i<10; i++) {
            for (int k=0; k<10; k++) {
                X.set(i, k, i);
            }
            y.set(i, 0, i);
        }
    }

    @Test
    public void test() {
        LocalLinearModel model = new LocalLinearModel();
        model.train(X, y);
        String s = model.serialize();
        LocalLinearModel model1 = new LocalLinearModel();
        model1 = model1.deserialize(s);
        System.out.println(Arrays.toString(model1.batchPredict(X)));
    }
}
