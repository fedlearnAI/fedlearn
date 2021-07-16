package com.jdt.fedlearn.core.type;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class HorModelPhaseTypeTest {
//    @Test
//    public void HorModelPhaseTypeTest() {
//        HorModelPhaseType type = HorModelPhaseType.loadTrainUpdate;
//        int message = 2;
//        Assert.assertEquals(type.getPhaseValue() ,message);
//
//    }
    private final HorModelPhaseType[] algorithmTypes = {HorModelPhaseType.Null_1, HorModelPhaseType.loadTrainUpdate,
        HorModelPhaseType.loadTrainUpdate_1};

    @Test
    public void HorModelPhaseTypeTest() {
        int[] target = {1,2,3};
        for (int i =0;i < target.length;i++){
            assertEquals(algorithmTypes[i].getPhaseValue(), target[i]);
        }
    }
}
