package com.jdt.fedlearn.core.preprocess;

import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.common.InferenceInitRes;

import java.util.*;
import java.util.stream.IntStream;

public class InferenceFilter {
    public static Message filter(String[] uidList, String[][] inferenceCacheFile){
        //判断uid数据是否存在（可预测）
        List<Integer> indexList = new ArrayList<>();
        Set<String> inferenceSet = new HashSet<>();
        // 如果查询结果返回空，那么需要过滤所以uid, 需要全部返回
        if (inferenceCacheFile == null || inferenceCacheFile.length == 0) {
            IntStream.range(0, uidList.length).forEach(i -> indexList.add(i));
            return new InferenceInitRes(false, indexList);
        }
        for (String[] row : inferenceCacheFile) {
            inferenceSet.add(row[0]);
        }
        for (int i = 0; i < uidList.length; i++) {
            String id = uidList[i];
            if (!inferenceSet.contains(id)) {
                indexList.add(i);
            }
        }
        return new InferenceInitRes(false, indexList);
    }
}
