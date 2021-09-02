package com.jdt.fedlearn.coordinator.service;

import com.jdt.fedlearn.coordinator.exception.NotAcceptableException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;


public class CommonServiceTest {

    @Test
    public void testFail() {
        Map<String, Object> res = CommonService.fail("000");
        Assert.assertEquals(res.get("code"), "000");
        Assert.assertEquals(res.get("status"), "fail");

    }

    @Test
    public void testExceptionProcess() {
        Map<String, Object> ResMap = CommonService.exceptionProcess(new NotAcceptableException(), new HashMap<>());
        Assert.assertEquals(ResMap.get("code"), -1);
        Map<String, Object> ResMap2 = CommonService.exceptionProcess(new Exception(), new HashMap<>());
        Assert.assertEquals(ResMap2.get("code"), -4);
    }
}