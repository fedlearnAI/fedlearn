package com.jdt.fedlearn.core.type;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class VerLRModelPhaseTypeTest {
//    @Test
//    public void VerLRModelPhaseTypeTest() {
//        VerLRModelPhaseType type = VerLRModelPhaseType.trainPhase1;
//        int message = 1;
//        Assert.assertEquals(type.getPhaseValue(),message);
//    }
    private final VerLRModelPhaseType[] algorithmTypes = {VerLRModelPhaseType.PASSIVE_LOCAL_PREDICT, VerLRModelPhaseType.COMPUTE_DIFFERENT,
        VerLRModelPhaseType.COMPUTE_GRADIENTS, VerLRModelPhaseType.UPDATE_WEIGHTS};

    @Test
    public void FGBModelPhaseTypeTest () {
        int[] target = {1,2,3,4};
        for (int i =0;i < target.length;i++){
            assertEquals(algorithmTypes[i].getPhaseValue(), target[i]);
        }
    }
}
