package com.jdt.fedlearn.core.type;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class VerLRDispatchPhaseTypeTest {
//    @Test
//    public void VerLRDispatchPhaseTypeTest() {
//        VerLRDispatchPhaseType type = VerLRDispatchPhaseType.controlPhase1;
//        int message = 1;
//        Assert.assertEquals(type.getPhaseValue(),message);
//    }
    private final VerLRDispatchPhaseType[] algorithmTypes = {VerLRDispatchPhaseType.UPDATE_METRIC, VerLRDispatchPhaseType.SEND_LOSS,
        VerLRDispatchPhaseType.SEND_GRADIENTS, VerLRDispatchPhaseType.UPDATE_GRADIENTS, VerLRDispatchPhaseType.PREDICT_RESULT,VerLRDispatchPhaseType.EMPTY_REQUEST};

    @Test
    public void FGBModelPhaseTypeTest () {
        int[] target = {1,2,3,4,-2,-3};
        for (int i =0;i < target.length;i++){
            assertEquals(algorithmTypes[i].getPhaseValue(), target[i]);
        }
    }
}
