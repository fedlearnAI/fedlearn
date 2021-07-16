package com.jdt.fedlearn.common.enums;

import org.testng.Assert;
import org.testng.annotations.Test;

public class LocalUrlTypeTest {
    @Test
    public void testValueOf(){
        System.out.println(LocalUrlType.CONFIG_QUERY.toString());
        String path = "/local/config/query";
        LocalUrlType type = LocalUrlType.urlOf(path);
        Assert.assertEquals(LocalUrlType.CONFIG_QUERY, type);
    }
}
