package com.jdt.fedlearn.client.cache;


import org.testng.annotations.Test;;

import java.util.Arrays;

public class TrainDataCacheTest {
    @Test
    public void test1() {
        String[] uidList = new String[]{"we","er","retw"};
        System.out.println(Arrays.toString(uidList).length());
    }

//    public String[][] predict(String uid) throws IOException {
//        String inferenceId = uid;
//
//        String[][] sample = InferenceDataCache. (inferenceId, new String[]{"0", "1", "4", "3", "2", "test", "test1", "43d10e5db296ec539471cf4b34ecdab6", "5f5fca0ee771920529af94cdac721e8d", "cae0438a6ccf8e0e1cbd89e9536567d4", "edbc75733a46ea392a892d3880efe2c8", "2e7d4bacccc7cd8f16da28378b421fc6"});
//        return sample;
//    }
}
