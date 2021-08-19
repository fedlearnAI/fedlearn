package com.jdt.fedlearn.frontend.util;


import com.jdt.fedlearn.common.util.JsonUtil;
import org.springframework.ui.ModelMap;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class TestJsonUtil {

    @Test
    public void testMap2Json() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "admin");
        String res = JsonUtil.object2json(map);
        String realRes = "{\"name\":\"admin\"}";
        Assert.assertEquals(res, realRes);
    }

    @Test
    public void testParseJson() {
        String jsonStr = "{\"name\":\"admin\"}";
        ModelMap modelMap = JsonUtil.json2Object(jsonStr,ModelMap.class);

        ModelMap res = new ModelMap();
        res.put("name", "admin");
        Assert.assertEquals(modelMap, res);
    }
}
