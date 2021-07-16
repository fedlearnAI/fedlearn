package com.jdt.fedlearn.core.entity.mixGB;

import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * @author zhangwenxi
 */
public class BoostInferQueryResTest {

    @Test
    public void testGetBodies() {
        BoostInferQueryResBody[] boostInferQueryResBodies = new BoostInferQueryResBody[1];
        BoostInferQueryRes res = new BoostInferQueryRes(boostInferQueryResBodies);
        Assert.assertEquals(res.getBodies(), boostInferQueryResBodies);
    }
}