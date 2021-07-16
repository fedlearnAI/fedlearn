package com.jdt.fedlearn.core.type;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class RFModelPhaseTypeTest {

    private int[] phases = {1, 2, 3, 4, 5, 6, 7, 8, 9, 99};
    private  RFModelPhaseType[] rfModelPhaseTypes = new  RFModelPhaseType[phases.length];


    @Test
    public void test() {
        for (int i = 0; i < phases.length; i++) {
            rfModelPhaseTypes[i] = RFModelPhaseType.valueOf(phases[i]);
        }
        for (int i = 0;i < phases.length;i++){
            assertEquals(rfModelPhaseTypes[i].getPhaseValue(), phases[i]);
        }
    }
}