package com.jdt.fedlearn.core.loader.common;


import com.jdt.fedlearn.core.entity.feature.SingleFeature;
import com.jdt.fedlearn.core.fake.DataGenerate;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class TestCommonTestData {
    public String baseDir = "./src/test/resources/classificationA/";
    public String fileName = "train0_missing.csv";

    @Test
    public void fakeAndScan() {
        String[][] rawTable = DataGenerate.fakeRawTable(5, 4);
        List<SingleFeature> featureList = new ArrayList<>();
        featureList.add(new SingleFeature("uid", "string"));
        featureList.add(new SingleFeature("x1", "float"));
        featureList.add(new SingleFeature("x2", "float"));
        featureList.add(new SingleFeature("y", "int"));

        CommonTestData testData = new CommonTestData(rawTable);
        Assert.assertEquals(testData.getFeatureDim(), 2);
        Assert.assertEquals(testData.getDatasetSize(), 4);
        Assert.assertEquals(testData.getUid(), new String[]{"a1", "a2", "a3", "a4"});
        System.out.println(Arrays.toString(testData.getLabel()));
        System.out.println(Arrays.toString(testData.getFeatureName()));

    }
}