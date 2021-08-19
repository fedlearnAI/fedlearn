package com.jdt.fedlearn.core.entity.mixGB;

import com.jdt.fedlearn.core.entity.mixGBoost.BoostInferQueryReqBody;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author zhangwenxi
 */
public class BoostInferQueryReqBodyTest {
    @Test
    public void testGetRecordId() {
        BoostInferQueryReqBody reqBody = new BoostInferQueryReqBody(null, 1);
        Assert.assertEquals(reqBody.getRecordId(), 1);
    }

    @Test
    public void testSetRecordId() {
        BoostInferQueryReqBody reqBody = new BoostInferQueryReqBody(null, 1);
        reqBody.setRecordId(2);
        Assert.assertEquals(reqBody.getRecordId(), 2);
    }

    @Test
    public void testGetInstanceId() {
        String[] instanceId = new String[]{"test"};
        BoostInferQueryReqBody reqBody = new BoostInferQueryReqBody(instanceId, 0);
        Assert.assertEquals(reqBody.getInstanceId(), instanceId);
    }
}