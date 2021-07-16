package com.jdt.fedlearn.core.type;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class NormalizationTypeTest {
    private final NormalizationType[] normalizationTypes = {NormalizationType.MINMAX, NormalizationType.STANDARD, NormalizationType.NONE};

    @Test
    public void testGetNormalizationType() {
        String[] target = {"MINMAX","STANDARD","NONE"};
        for (int i = 0;i < target.length;i++){
            assertEquals(normalizationTypes[i].getNormalizationType(),target[i]);
        }
    }
}