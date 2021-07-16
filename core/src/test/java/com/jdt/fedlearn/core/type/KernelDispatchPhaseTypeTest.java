package com.jdt.fedlearn.core.type;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class KernelDispatchPhaseTypeTest {
//    @Test
//    public void KernelDispatchPhaseTypeTest() {
//        KernelDispatchPhaseType type = KernelDispatchPhaseType.controlPhase1;
//        int message = 2;
//        Assert.assertEquals(type.getPhaseValue(),message);
//    }
    private final KernelDispatchPhaseType[] algorithmTypes = {KernelDispatchPhaseType.COMPUTE_LOSS_METRIC, KernelDispatchPhaseType.EMPTY_REQUEST,
        KernelDispatchPhaseType.INFERENCE_FILTER,KernelDispatchPhaseType.INFERENCE_EMPTY_REQUEST, KernelDispatchPhaseType.INFERENCE_EMPTY_REQUEST_1,
        KernelDispatchPhaseType.INFERENCE_RESULT};

    @Test
    public void HorModelPhaseTypeTest() {
        int[] target = {2,1,-1,-2,-3,-4};
        for (int i =0;i < target.length;i++){
            assertEquals(algorithmTypes[i].getPhaseValue(), target[i]);
        }
    }
}
