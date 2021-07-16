package com.jdt.fedlearn.core.entity.localModel;

import org.ejml.simple.SimpleMatrix;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;

public class TestLocalNullModel {

    SimpleMatrix X;
    SimpleMatrix y;

    @BeforeMethod
    public void setUp() {
        int n = 10;
        int m = 1;
        X = new SimpleMatrix(n, m);
        y = new SimpleMatrix(n, 1);
        for (int i=0; i<n; i++) {
            for (int k=0; k<m; k++) {
                X.set(i, k, i);
            }
            y.set(i, 0, i);
        }
    }

    @Test
    public void test() {
        LocalNullModel model = new LocalNullModel();
        model.train(X, y);
        String s = model.serialize();
        LocalNullModel model1 = new LocalNullModel();
        model1 = model1.deserialize(s);
        System.out.println(Arrays.toString(model1.batchPredict(X)));
    }
}
