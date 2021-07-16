package com.jdt.fedlearn.coordinator.util;

import com.jdt.fedlearn.common.util.HttpClientUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.HashMap;
import java.util.Map;

public class HttpUtilTest {
    private static Logger log = LoggerFactory.getLogger(HttpClientUtil.class);

    public static void main(String[] args) {

        String url = "http://localhost:8091/api/query/parameter";
//        String res = HttpUtil.doPost(url, new HashMap<>());
//        System.out.println(res);

//        String url = "http://localhost:8082/api/conf/findConfList?type=1";
//        String getResult = HttpUtil.getData(url);
//        System.out.println(getResult);

//        url = "http://localhost:8082/api/core/login";
        Map<String, Object> map = new HashMap<>();
        map.put("key", "==g43sEvsUcbcunFv3mHkIzlHO4iiUIT R7WwXuSVKTK0yugJnZSlr6qNbxsL8OqCUAFyCDCoRKQ882m6cTTi0q9uCJsq JJvxS+8mZVRP/7lWfEVt8/N9mKplUA68SWJEPSXyz4MDeFam766KEyvqZ99d");
        String postResult = HttpClientUtil.doHttpPost(url,map);
        System.out.println(postResult);

//        url = "http://localhost:8082/api/test/testSendForm?format=json";
//        Map<String, Object> map = new HashMap<>();
//        map.put("name", "测试表单请求");
//        String formResult = HttpUtil.sendxwwwForm(url, map);
//        System.out.println(formResult);


    }

}
