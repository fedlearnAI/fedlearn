package com.jdt.fedlearn.core.type;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class VerLinDispatchPhaseTypeTest {
//    @Test
//    public void VerLinRegDispatchPhaseTypeTest() {
//        VerLinRegDispatchPhaseType type = VerLinRegDispatchPhaseType.controlPhase1;
//        int message = 1;
//        Assert.assertEquals(type.getPhaseValue(),message);
//    }
    private final  VerLinDispatchPhaseType[] algorithmTypes = {VerLinDispatchPhaseType.UPDATE_METRIC, VerLinDispatchPhaseType.SEND_LOSS,
        VerLinDispatchPhaseType.SEND_GRADIENTS, VerLinDispatchPhaseType.UPDATE_GRADIENTS, VerLinDispatchPhaseType.PREDICT_RESULT, VerLinDispatchPhaseType.EMPTY_REQUEST};

    @Test
    public void FGBModelPhaseTypeTest () {
        int[] target = {1,2,3,4,-1,-2};
        for (int i =0;i < target.length;i++){
            assertEquals(algorithmTypes[i].getPhaseValue(), target[i]);
        }
    }
}
