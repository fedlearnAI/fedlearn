package com.jdt.fedlearn.core.loader.boost;

import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.fake.StructureGenerate;
import com.jdt.fedlearn.core.model.common.tree.sampling.ColSampler;
import com.jdt.fedlearn.core.type.data.Tuple3;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;

public class TestBoostTrainData {

    @Test
    public void construct() {
        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainInputStd();
        String[][] input = compoundInput._1().get();
        String[] idMap = compoundInput._2().get();
        Features features = compoundInput._3().get();

        BoostTrainData boostTrainData = new BoostTrainData(input, idMap, features, new ArrayList<>());
        System.out.println(boostTrainData.getDatasetSize() + "," + boostTrainData.getFeatureDim());

        Assert.assertEquals(boostTrainData.getFeatureDim(), 3);
        Assert.assertEquals(boostTrainData.getDatasetSize(), 3);
        System.out.println(Arrays.toString(boostTrainData.getUid()));
        Assert.assertEquals(boostTrainData.getUid(), new String[]{"1", "100", "10003"});
    }

    @Test
    public void getFeature() {
        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainInputStd();
        String[][] input = compoundInput._1().get();
        String[] idMap = compoundInput._2().get();
        Features features = compoundInput._3().get();

        BoostTrainData boostTrainData = new BoostTrainData(input, idMap, features, new ArrayList<>());
        System.out.println(boostTrainData.getDatasetSize() + "," + boostTrainData.getFeatureDim());

        double[][] res = boostTrainData.getFeature(new int[]{1,2},1);
        double[] a1 = new double[]{1.0, 29.0};
        double[] a2 = new double[]{2.0, 12.0};
        Assert.assertEquals(res[0], a1);
        Assert.assertEquals(res[1], a2);
        System.out.println(Arrays.deepToString(res));

    }

    @Test
    public void getSample(){
        BoostTrainData boostTrainData = StructureGenerate.getBoostTrainData();
        Assert.assertEquals(boostTrainData.getColSampler().getColSelected(), new ColSampler(0,0).getColSelected());
//        Assert.assertEquals(boostTrainData.getRowSampler(), new RowSampler(0,0));
    }

}
