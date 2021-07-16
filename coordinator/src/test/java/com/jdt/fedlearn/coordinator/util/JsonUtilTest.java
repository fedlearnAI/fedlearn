package com.jdt.fedlearn.coordinator.util;

import com.jdt.fedlearn.common.util.JsonUtil;
import com.jdt.fedlearn.core.dispatch.FederatedGB;
import com.jdt.fedlearn.core.parameter.FgbParameter;
import org.testng.Assert;
import org.testng.annotations.Test;


import java.util.HashMap;
import java.util.Map;

public class JsonUtilTest {
    @Test
    public void testObject2json(){
        Map map = new HashMap();
        map.put("algorithm", "VerticalLinearRegression");
        String res = JsonUtil.object2json(map);
        Assert.assertEquals(res, "{\"algorithm\":\"VerticalLinearRegression\"}");
        System.out.println(res);
        FgbParameter parameter = new FgbParameter();
        String res2 = JsonUtil.object2json(parameter);
        System.out.println(res2);


    }

    @Test
    public void testParseJson(){
        String string = "{\"algorithm\":\"VerticalLinearRegression\"}";
        Map res = JsonUtil.parseJson(string);
        System.out.println("res : " + res);
        Map map = new HashMap();
        map.put("algorithm", "VerticalLinearRegression");
        Assert.assertEquals(res,map);
    }


    @Test
    public void testJson2Object() {
        Map map = new HashMap();
        map.put("algorithm", "VerticalLinearRegression");
        Map res = JsonUtil.json2Object("{\"algorithm\":\"VerticalLinearRegression\"}", HashMap.class);
        Assert.assertEquals(res, map);



    }

}
