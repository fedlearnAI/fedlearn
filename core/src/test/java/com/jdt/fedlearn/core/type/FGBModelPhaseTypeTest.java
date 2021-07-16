package com.jdt.fedlearn.core.type;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;


public class FGBModelPhaseTypeTest {
//    @Test
//    public void FGBModelPhaseTypeTest() {
//
//        FGBModelPhaseType type = FGBModelPhaseType.trainPhase1;
//        int message = 1;
//        Assert.assertEquals(type.getPhaseValue(),message);
//
//    }
    private final FGBModelPhaseType[] algorithmTypes = {FGBModelPhaseType.trainPhase1, FGBModelPhaseType.trainPhase2,
        FGBModelPhaseType.req1, FGBModelPhaseType.req2, FGBModelPhaseType.res1get};

    @Test
    public void FGBModelPhaseTypeTest () {
        int[] target = {1,2,3,4,5};
        for (int i =0;i < target.length;i++){
            assertEquals(algorithmTypes[i].getPhaseValue(), target[i]);
        }
    }
//    @Test
//    public void fullAlgorithm() {
//        System.out.println(Arrays.toString(AlgorithmType.getAlgorithms()));
//    }

}
