package com.jdt.fedlearn.core.type;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class VerFDNNModelPhaseTypeTest {
//    @Test  有问题
//    public void VerFDNNModelPhaseTypeTest() {
//        VerFDNNModelPhaseType type = VerFDNNModelPhaseType.trainPhase0Passive1;
//        int message = 1;
//        Assert.assertEquals(type.getPhaseValue(),message);
//    }
    private final VerFDNNModelPhaseType[] algorithmTypes = {VerFDNNModelPhaseType.trainPhase0Passive, VerFDNNModelPhaseType.trainPhase0Passive1,
        VerFDNNModelPhaseType.trainPhase2, VerFDNNModelPhaseType.trainPhase3, VerFDNNModelPhaseType.trainPhase4,VerFDNNModelPhaseType.trainPhase99,
        VerFDNNModelPhaseType.inferenceInit, VerFDNNModelPhaseType.inferenceInit1, VerFDNNModelPhaseType.inferenceInit2};

    @Test
    public void VerFDNNModelPhaseTypeTest () {
        int[] target = {0,1,2,3,4,99,-1,-2,-3};
        for (int i =0;i < target.length;i++){
            assertEquals(algorithmTypes[i].getPhaseValue(), target[i]);
        }
    }
}
