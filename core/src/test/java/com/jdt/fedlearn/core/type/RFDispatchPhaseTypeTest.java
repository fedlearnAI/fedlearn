package com.jdt.fedlearn.core.type;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class RFDispatchPhaseTypeTest {
    private int[] phases = {1, 2, 3, 4, 5, 6, 7, 8, 9, 99};
    private RFDispatchPhaseType[] rfDispatchPhaseTypes = new RFDispatchPhaseType[phases.length];


    @Test
    public void test() {
        for (int i = 0; i < phases.length; i++) {
            rfDispatchPhaseTypes[i] = RFDispatchPhaseType.valueOf(phases[i]);
        }
        for (int i = 0;i < phases.length;i++){
            assertEquals(rfDispatchPhaseTypes[i].getPhaseValue(), phases[i]);
        }
    }

}