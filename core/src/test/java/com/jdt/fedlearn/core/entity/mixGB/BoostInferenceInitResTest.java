package com.jdt.fedlearn.core.entity.mixGB;

import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * @author zhangwenxi
 */
public class BoostInferenceInitResTest {
    @Test
    public void testSetContent() {
        BoostInferenceInitRes res = new BoostInferenceInitRes(false, null, "");
        res.setContent("tree");
        Assert.assertEquals(res.getContent(), "tree");
    }
}