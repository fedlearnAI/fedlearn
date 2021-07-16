package com.jdt.fedlearn.client.util;

import java.util.HashMap;
import java.util.Map;

public class CastUtilTest {
    public static void main(String[] args) {
        Map<String,Object> map = new HashMap<>();
        map.put("key", 1);
        map.get("key");
        System.out.println(Integer.parseInt(map.get("key").toString()));
    }
}
