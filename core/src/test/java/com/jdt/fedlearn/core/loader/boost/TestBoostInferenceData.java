package com.jdt.fedlearn.core.loader.boost;

import com.jdt.fedlearn.core.fake.StructureGenerate;
import com.jdt.fedlearn.core.type.data.Tuple3;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;

public class TestBoostInferenceData {

    @Test
    public void construct() {
        Tuple3<String[][], String, String[]> compoundInput = StructureGenerate.inferenceInputStd();
        String[][] input = compoundInput._1().get();
        String uidName = compoundInput._2().get();
        String[] features = compoundInput._3().get();

        BoostInferenceData boostTrainData = new BoostInferenceData(input);

        Assert.assertEquals(boostTrainData.getFeatureDim(), 3);
        Assert.assertEquals(boostTrainData.getDatasetSize(), 4);
        System.out.println(Arrays.toString(boostTrainData.getUid()));
        Assert.assertEquals(boostTrainData.getUid(), new String[]{"1", "100", "10003", "8088"});
    }

    @Test
    public void computeUidIndex(){
        Tuple3<String[][], String, String[]> compoundInput = StructureGenerate.inferenceInputStd();
        String[][] input = compoundInput._1().get();
        String uidName = compoundInput._2().get();
        String[] features = compoundInput._3().get();

        BoostInferenceData boostInferenceData = new BoostInferenceData(input);
        boostInferenceData.computeUidIndex(new String[]{"1", "100"});
        System.out.println(Arrays.toString(boostInferenceData.getFakeIdIndex()));
        Assert.assertEquals(boostInferenceData.getFakeIdIndex(), new int[]{0,1});
    }
}
