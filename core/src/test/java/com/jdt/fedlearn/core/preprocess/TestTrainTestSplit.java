package com.jdt.fedlearn.core.preprocess;

import org.testng.annotations.Test;

import java.util.ArrayList;

import static com.jdt.fedlearn.core.preprocess.TrainTestSplit.trainTestSplit;

public class TestTrainTestSplit {

    @Test
    public void testTrainTestSpilt() {
        ArrayList<Long> idList = new ArrayList<>();
        for (int i=0;i<100;i++){
            idList.add(Long.valueOf(i));
        }
    }
}