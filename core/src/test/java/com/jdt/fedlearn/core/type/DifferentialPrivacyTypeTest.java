package com.jdt.fedlearn.core.type;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;

public class DifferentialPrivacyTypeTest {

    private final DifferentialPrivacyType[] differentialPrivacyTypes = {DifferentialPrivacyType.OBJECTIVE_PERTURB,
            DifferentialPrivacyType.OUTPUT_PERTURB};

    @Test
    public void getDifferential(){
        String[] types = {"ObjectivePerturb", "OutputPerturb"};
        for(int i = 0; i < types.length; i++){
            Assert.assertEquals(differentialPrivacyTypes[i].getDifferentialPrivacyType(), types[i]);
        }
    }

    @Test
    public void fullAlgorithm() {
        System.out.println(Arrays.toString(DifferentialPrivacyType.getDifferentialPrivacies()));
    }

}
