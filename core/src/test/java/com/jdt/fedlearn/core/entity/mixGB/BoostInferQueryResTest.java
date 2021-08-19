package com.jdt.fedlearn.core.entity.mixGB;

import com.jdt.fedlearn.core.entity.mixGBoost.BoostInferQueryRes;
import com.jdt.fedlearn.core.entity.mixGBoost.BoostInferQueryResBody;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author zhangwenxi
 */
public class BoostInferQueryResTest {

    @Test
    public void testGetBodies() {
        BoostInferQueryResBody[] boostInferQueryResBodies = new BoostInferQueryResBody[1];
        BoostInferQueryRes res = new BoostInferQueryRes(boostInferQueryResBodies, new int[0]);
        Assert.assertEquals(res.getBodies(), boostInferQueryResBodies);
    }
}