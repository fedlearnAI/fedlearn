package com.jdt.fedlearn.core.type;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class KernelDispatchPhaseTypeTest {
//    @Test
//    public void KernelDispatchJavaPhaseTypeTest() {
//        KernelDispatchJavaPhaseType type = KernelDispatchJavaPhaseType.controlPhase1;
//        int message = 1;
//        Assert.assertEquals(type.getPhaseValue(),message);
//
//    }

    private final KernelDispatchPhaseType[] algorithmTypes = {KernelDispatchPhaseType.UPDATE_METRIC, KernelDispatchPhaseType.COMPUTE_LOSS,
            KernelDispatchPhaseType.VALIDATION_INIT, KernelDispatchPhaseType.VALIDATION_FILTER, KernelDispatchPhaseType.EMPTY_REQUEST,
            KernelDispatchPhaseType.VALIDATION_RESULT, KernelDispatchPhaseType.INFERENCE_FILTER, KernelDispatchPhaseType.INFERENCE_EMPTY_REQUEST, KernelDispatchPhaseType.INFERENCE_RESULT};

    @Test
    public void HorDispatchPhaseTypeTest() {
        int[] target = {1,2,3,4,5,7,-1,-2,-4};
        for (int i =0;i < target.length;i++){
            assertEquals(algorithmTypes[i].getPhaseValue(), target[i]);
        }
    }
}
