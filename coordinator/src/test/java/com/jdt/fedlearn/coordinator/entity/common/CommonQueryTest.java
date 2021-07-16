package com.jdt.fedlearn.coordinator.entity.common;

import org.testng.Assert;
import org.testng.annotations.Test;

public class CommonQueryTest {
    @Test
    public void test() {
        CommonQuery commonQuery = new CommonQuery();
        commonQuery.setUsername("user");
        String s = commonQuery.toJson();
        CommonQuery commonQuery1 = new CommonQuery(s);
        Assert.assertEquals(commonQuery1.getUsername(), commonQuery.getUsername());

    }

}