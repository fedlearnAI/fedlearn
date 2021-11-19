package com.jdt.fedlearn.core.type;

import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.testng.Assert.*;

public class AlgorithmTypeTest {
    private final AlgorithmType[] algorithmTypes = {AlgorithmType.VerticalLinearRegression, AlgorithmType.LinearRegression,
            AlgorithmType.FederatedGB, AlgorithmType.MixGBoost};

    @Test
    public void getAlgorithm() {
        String[] target = {"VerticalLinearRegression", "LinearRegression", "FederatedGB", "MixGBoost"};
        for (int i = 0; i < target.length; i++) {
            assertEquals(algorithmTypes[i].getAlgorithm(), target[i]);
        }
    }

    @Test
    public void fullAlgorithm() {
        System.out.println(Arrays.toString(AlgorithmType.getAlgorithms()));
    }
}