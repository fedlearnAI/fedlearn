package com.jdt.fedlearn.core.type;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class KernelJavaModelPhaseTypeTest {
    private int[] phases = {1, 2, 3, 4, 5, 6, 7, -1,-2,-3};
    private KernelJavaModelPhaseType[] kernelJavaModelPhaseTypes = new KernelJavaModelPhaseType[phases.length];

    @Test
    public void test(){
        for (int i = 0; i < phases.length ; i++) {
            kernelJavaModelPhaseTypes[i] = KernelJavaModelPhaseType.valueOf(phases[i]);
        }
        for (int i = 0;i < phases.length;i++){
            assertEquals(kernelJavaModelPhaseTypes[i].getPhaseValue(), phases[i]);
        }
    }
}