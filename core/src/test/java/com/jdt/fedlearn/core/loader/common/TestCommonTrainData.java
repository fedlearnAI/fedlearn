package com.jdt.fedlearn.core.loader.common;

import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.entity.feature.SingleFeature;
import com.jdt.fedlearn.core.fake.DataGenerate;
import com.jdt.fedlearn.core.util.DataParseUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;

public class TestCommonTrainData {
    public String baseDir = "./src/test/resources/classificationA/";
    public String fileName = "train0_missing.csv";

    @Test
    public void fakeAndScan() {
        String[][] rawTable = DataGenerate.fakeRawTable(5, 4);
        String[] idMap = DataGenerate.fakeIdMapByTable(rawTable);
        List<SingleFeature> featureList = new ArrayList<>();
        featureList.add(new SingleFeature("uid", "string"));
        featureList.add(new SingleFeature("x1", "float"));
        featureList.add(new SingleFeature("x2", "float"));
        featureList.add(new SingleFeature("y", "int"));
        Features features = new Features(featureList, "y");

        CommonTrainData trainData = new CommonTrainData(rawTable, idMap, features);
        Assert.assertEquals(trainData.getFeatureDim(), 2);
        Assert.assertEquals(trainData.getDatasetSize(), 4);
        Assert.assertTrue(trainData.hasLabel);
        Assert.assertEquals(trainData.getFeatureName(), new String[]{"x1", "x2"});
        Assert.assertEquals(trainData.fullInstance, new int[]{0, 1, 2, 3});
        Assert.assertEquals(trainData.getLabel(), new double[]{1, 1, 1, 1});
        Assert.assertEquals(trainData.getUid(), new String[]{"a1", "a2", "a3", "a4"});
        double[][] sample = new double[4][2];
        sample[0] = new double[]{2.0, 3.0};
        sample[1] = new double[]{3.0, 4.0};
        sample[2] = new double[]{4.0, 5.0};
        sample[3] = new double[]{5.0, 6.0};
        for (int i = 0; i < sample.length; i++) {
            Assert.assertEquals(trainData.getSample()[i], sample[i]);
        }
    }

    @Test
    public void fakeNoLabelAndScan() {
        String[][] rawTable = DataGenerate.fakeRawTableWithoutLabel(5, 3);
        String[] idMap = DataGenerate.fakeIdMapByTable(rawTable);
        List<SingleFeature> featureList = new ArrayList<>();
        featureList.add(new SingleFeature("uid", "string"));
        featureList.add(new SingleFeature("x1", "float"));
        featureList.add(new SingleFeature("x2", "float"));
        Features features = new Features(featureList, null);

        CommonTrainData trainData = new CommonTrainData(rawTable, idMap, features);
        Assert.assertEquals(trainData.getFeatureDim(), 2);
        Assert.assertEquals(trainData.getDatasetSize(), 4);
        Assert.assertFalse(trainData.hasLabel);
        Assert.assertEquals(trainData.featureName, new String[]{"x1", "x2"});
        Assert.assertEquals(trainData.fullInstance, new int[]{0, 1, 2, 3});
        Assert.assertEquals(trainData.uid, new String[]{"a1", "a2", "a3", "a4"});
        double[][] sample = new double[4][2];
        sample[0] = new double[]{2.0, 3.0};
        sample[1] = new double[]{3.0, 4.0};
        sample[2] = new double[]{4.0, 5.0};
        sample[3] = new double[]{5.0, 6.0};
        for (int i = 0; i < sample.length; i++) {
            Assert.assertEquals(trainData.getSample()[i], sample[i]);
        }
    }

    //TODO

    /**
     * 第一行为header，第一列为uid，有label但是label不在最后一列，
     * uid, x1, y, x3
     * a1, 2, 1, 4
     * a2, 3, 1, 5
     * 目前返回featureName为uid, x1,y, sample为[[2,1],[3,1]]
     */
    @Test
    public void fakeUnOrderLabelAndScan() {
        String[][] rawTable = DataGenerate.fakeRawTableUnOrder(5, 4);
        String[]idMap = DataGenerate.fakeIdMapByTable(rawTable);
        List<SingleFeature> featureList = new ArrayList<>();
        featureList.add(new SingleFeature("uid", "string"));
        featureList.add(new SingleFeature("x1", "float"));
        featureList.add(new SingleFeature("y", "int"));
        featureList.add(new SingleFeature("x3", "float"));
        Features features = new Features(featureList, "y");

        CommonTrainData trainData = new CommonTrainData(rawTable, idMap, features);
        Assert.assertEquals(trainData.featureDim, 2);
        Assert.assertEquals(trainData.datasetSize, 4);
        Assert.assertTrue(trainData.hasLabel);
        Assert.assertEquals(trainData.featureName, new String[]{"x1", "x3"});
        Assert.assertEquals(trainData.fullInstance, new int[]{0, 1, 2, 3});
        Assert.assertEquals(trainData.label, new double[]{1, 1, 1, 1});
        Assert.assertEquals(trainData.uid, new String[]{"a1", "a2", "a3", "a4"});
        double[][] sample = new double[4][2];
        sample[0] = new double[]{2.0, 4.0};
        sample[1] = new double[]{3.0, 5.0};
        sample[2] = new double[]{4.0, 6.0};
        sample[3] = new double[]{5.0, 7.0};
        for (int i = 0; i < sample.length; i++) {
            Assert.assertEquals(trainData.sample[i], sample[i]);
        }
    }


    @Test
    public void fakeOnlyLabelAndScan() {
        String[][] rawTable = DataGenerate.fakeRawTableOnlyLabel(5);
        String[] idMap = DataGenerate.fakeIdMapByTable(rawTable);
        List<SingleFeature> featureList = new ArrayList<>();
        featureList.add(new SingleFeature("uid", "string"));
        featureList.add(new SingleFeature("y", "int"));
        Features features = new Features(featureList, "y");

        CommonTrainData trainData = new CommonTrainData(rawTable, idMap, features);
        Assert.assertEquals(trainData.featureDim, 0);
        Assert.assertEquals(trainData.datasetSize, 4);
        Assert.assertTrue(trainData.hasLabel);
        Assert.assertEquals(trainData.featureName, new String[]{});
        Assert.assertEquals(trainData.fullInstance, new int[]{0, 1, 2, 3});
        Assert.assertEquals(trainData.label, new double[]{1, 1, 1, 1});
        Assert.assertEquals(trainData.uid, new String[]{"a1", "a2", "a3", "a4"});
        Assert.assertEquals(trainData.sample, new double[0][]);
    }


    @Test
    public void loadAndScan() {
        String[][] rawTable = DataParseUtil.loadTrainFromFile(baseDir + fileName);
    }


}