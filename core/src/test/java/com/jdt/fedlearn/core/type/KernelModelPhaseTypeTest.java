package com.jdt.fedlearn.core.type;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class KernelModelPhaseTypeTest {
    private int[] phases = {1, 2, 3, 4, 5, 6, 7, -1,-2,-3};
    private KernelModelPhaseType[] kernelModelPhaseTypes = new KernelModelPhaseType[phases.length];

    @Test
    public void test(){
        for (int i = 0; i < phases.length ; i++) {
            kernelModelPhaseTypes[i] = KernelModelPhaseType.valueOf(phases[i]);
        }
        for (int i = 0;i < phases.length;i++){
            assertEquals(kernelModelPhaseTypes[i].getPhaseValue(), phases[i]);
        }
    }
}