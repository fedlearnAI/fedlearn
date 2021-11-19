package com.jdt.fedlearn.core.entity.boost;

import com.jdt.fedlearn.common.entity.core.ClientInfo;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestGainOutput {
    @Test
    public void construct(){
        GainOutput gainOutput = new GainOutput(new ClientInfo(), "feature", 5, 12.44);

        Assert.assertEquals(gainOutput.getClient(), new ClientInfo());
        Assert.assertEquals(gainOutput.getFeature(), "feature");
        Assert.assertEquals(gainOutput.getSplitIndex(), 5);
        Assert.assertEquals(gainOutput.getGain(), 12.44);

    }

}
