package com.jdt.fedlearn.client.service;

import org.testng.annotations.Test;

import java.util.*;
import java.util.stream.Collectors;

public class TrainServiceTest {

    @Test
    public void push() {
        int uid = 12;
        double score = 1231.123117923791723923;
        Map map = new HashMap();
        map.put("uid", uid);
        map.put("score", score);
        ArrayList picArray = new ArrayList();
        picArray.add(map);
    }

    @Test
    public void t(){
        Set<String> tokenSet = new HashSet<>();
        List<String> needToLoad = tokenSet.stream().limit(5).collect(Collectors.toList());

        for (String modelToken:needToLoad) {
            System.out.println(modelToken);
        }
    }
}