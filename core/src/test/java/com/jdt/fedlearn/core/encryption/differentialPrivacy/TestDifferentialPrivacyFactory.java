package com.jdt.fedlearn.core.encryption.differentialPrivacy;


import com.jdt.fedlearn.core.type.DifferentialPrivacyType;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestDifferentialPrivacyFactory {

    @Test
    public void testCreateDP(){
        DifferentialPrivacyType[] types = {DifferentialPrivacyType.OUTPUT_PERTURB, DifferentialPrivacyType.OBJECTIVE_PERTURB};
        IDifferentialPrivacy dp = DifferentialPrivacyFactory.createDifferentialPrivacy(types[0]);
        Assert.assertEquals(dp.getClass(), OutputPerturbDPImpl.class);
        dp = DifferentialPrivacyFactory.createDifferentialPrivacy(types[1]);
        Assert.assertEquals(dp.getClass(), ObjectivePerturbDPImpl.class);
    }

}
