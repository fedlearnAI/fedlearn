package com.jdt.fedlearn.coordinator.entity.system;

import com.jdt.fedlearn.common.util.JsonUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

public class FeatureReqTest {
    @Test
    public void testAll() {
        FeatureReq featureReq = new FeatureReq();
        featureReq.setClientUrl("url");
        featureReq.setTaskId("1");
        featureReq.setTaskPwd("pwd");
        featureReq.setUsername("user");
        String s = JsonUtil.object2json(featureReq);
        FeatureReq featureReq2 = new FeatureReq("", "", "", "");
        FeatureReq featureReq1 = new FeatureReq(s);
        Assert.assertEquals(featureReq.getClientUrl(), featureReq1.getClientUrl());
        Assert.assertEquals(featureReq.getTaskId(), featureReq1.getTaskId());
        Assert.assertEquals(featureReq.getTaskPwd(), featureReq1.getTaskPwd());
        Assert.assertEquals(featureReq.getUsername(), featureReq1.getUsername());


    }

}