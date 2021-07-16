package com.jdt.fedlearn.core.type;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;


public class FGBDispatchPhaseTypeTest {

    private final FGBDispatchPhaseType[] algorithmTypes = {FGBDispatchPhaseType.FromInit, FGBDispatchPhaseType.ControlPhase1,
        FGBDispatchPhaseType.ControlPhase2, FGBDispatchPhaseType.ControlPhase3, FGBDispatchPhaseType.ControlPhase4, FGBDispatchPhaseType.ControlPhase5};

    @Test
    public void FGBModelPhaseTypeTest() {
        String[] target = {"FromInit","ControlPhase1","ControlPhase2","ControlPhase3","ControlPhase4","ControlPhase5"};
        for (int i =0;i < target.length;i++){
            assertEquals(algorithmTypes[i].toString(), target[i]);
        }
    }

}