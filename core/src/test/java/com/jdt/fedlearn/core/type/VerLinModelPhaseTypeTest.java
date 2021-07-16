package com.jdt.fedlearn.core.type;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class VerLinModelPhaseTypeTest {
//    @Test
//    public void VerLinModelPhaseTypeTest() {
//        VerLinModelPhaseType type = VerLinModelPhaseType.trainPhase1;
//        int message = 1;
//        Assert.assertEquals(type.getPhaseValue(),message);
//    }
    private final VerLinModelPhaseType[] algorithmTypes = {VerLinModelPhaseType.PASSIVE_LOCAL_PREDICT, VerLinModelPhaseType.COMPUTE_DIFFERENT,
        VerLinModelPhaseType.COMPUTE_GRADIENTS, VerLinModelPhaseType.UPDATE_WEIGHTS};

    @Test
    public void VerLinModelPhaseTypeTest () {
        int[] target = {1,2,3,4};
        for (int i =0;i < target.length;i++){
            assertEquals(algorithmTypes[i].getPhaseValue(), target[i]);
        }
    }
}
