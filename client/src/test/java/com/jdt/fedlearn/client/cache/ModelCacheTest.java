package com.jdt.fedlearn.client.cache;

import com.jdt.fedlearn.core.model.Model;
import com.jdt.fedlearn.core.type.data.Tuple2;

import java.util.*;
import java.util.stream.Collectors;

public class ModelCacheTest {
    public static void main(String[] args) {
        int capacity = 10;
        Set<String> tokenSet = new HashSet<>();
        tokenSet.add("1");
//        tokenSet.add("2");
//        tokenSet.add("3");
//        tokenSet.add("4");
//        tokenSet.add("5");

        List<String> needToLoad = tokenSet.stream().limit(capacity).collect(Collectors.toList());
        System.out.println(needToLoad.size());

        Queue<Tuple2<String, Model>> modelQueue = new LinkedList<>();
        System.out.println(modelQueue.size());
    }
}
