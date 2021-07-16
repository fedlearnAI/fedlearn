package com.jdt.fedlearn.core.type;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class VerFDMMDispatchPhaseTypeTest {
//    @Test
//    public void VerDispatchPhaseTypeTest() {
//        VerDispatchPhaseType type = VerDispatchPhaseType.controlInitPassive;
//        int message = 0;
//        Assert.assertEquals(type.getPhaseValue(),message);
//    }
    private final VerFDMMDispatchPhaseType[] algorithmTypes = { VerFDMMDispatchPhaseType.controlInitPassive,VerFDMMDispatchPhaseType.controlInitActive,
        VerFDMMDispatchPhaseType.controlPhase2, VerFDMMDispatchPhaseType.controlPhase3, VerFDMMDispatchPhaseType.controlPhase4, VerFDMMDispatchPhaseType.controlPhase99, VerFDMMDispatchPhaseType.inferencePhase1,
        VerFDMMDispatchPhaseType.inferencePhase2, VerFDMMDispatchPhaseType.inferencePhase3, VerFDMMDispatchPhaseType.inferencePhase4};

    @Test
    public void VerDispatchPhaseTypeTest () {
        int[] target = {0,1,2,3,4,99,-1,-2,-3,-4};
        for (int i =0;i < target.length;i++){
            assertEquals(algorithmTypes[i].getPhaseValue(), target[i]);
        }
    }
}
