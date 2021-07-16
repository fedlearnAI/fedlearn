package com.jdt.fedlearn.core.type;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class HorDispatchPhaseTypeTest {
//    @Test
//    public void HorDispatchPhaseTypeTest() {
//        HorDispatchPhaseType type = HorDispatchPhaseType.createNullRequest;
//        int message = 1;
//        Assert.assertEquals(type.getPhaseValue(),message);
//
//    }
    private final HorDispatchPhaseType[] algorithmTypes = {HorDispatchPhaseType.createNullRequest, HorDispatchPhaseType.transferModels,
        HorDispatchPhaseType.predict, HorDispatchPhaseType.getPredictResults};

    @Test
    public void HorDispatchPhaseTypeTest() {
        int[] target = {1,2,-2,-3};
        for (int i =0;i < target.length;i++){
            assertEquals(algorithmTypes[i].getPhaseValue(), target[i]);
        }
    }
}
