package com.jdt.fedlearn.core.type;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class KernelDispatchJavaPhaseTypeTest {
//    @Test
//    public void KernelDispatchJavaPhaseTypeTest() {
//        KernelDispatchJavaPhaseType type = KernelDispatchJavaPhaseType.controlPhase1;
//        int message = 1;
//        Assert.assertEquals(type.getPhaseValue(),message);
//
//    }

    private final KernelDispatchJavaPhaseType[] algorithmTypes = {KernelDispatchJavaPhaseType.UPDATE_METRIC, KernelDispatchJavaPhaseType.COMPUTE_LOSS,
            KernelDispatchJavaPhaseType.VALIDATION_INIT, KernelDispatchJavaPhaseType.VALIDATION_FILTER,KernelDispatchJavaPhaseType.EMPTY_REQUEST, KernelDispatchJavaPhaseType.EMPTY_REQUEST_1,
            KernelDispatchJavaPhaseType.VALIDATION_RESULT, KernelDispatchJavaPhaseType.INFERENCE_FILTER,KernelDispatchJavaPhaseType.INFERENCE_EMPTY_REQUEST,
            KernelDispatchJavaPhaseType.INFERENCE_EMPTY_REQUEST_1,KernelDispatchJavaPhaseType.INFERENCE_RESULT};

    @Test
    public void HorDispatchPhaseTypeTest() {
        int[] target = {1,2,3,4,5,6,7,-1,-2,-3,-4};
        for (int i =0;i < target.length;i++){
            assertEquals(algorithmTypes[i].getPhaseValue(), target[i]);
        }
    }
}
