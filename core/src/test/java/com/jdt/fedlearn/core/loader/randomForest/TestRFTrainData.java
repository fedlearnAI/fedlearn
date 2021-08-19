package com.jdt.fedlearn.core.loader.randomForest;

import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.entity.feature.SingleFeature;
import com.jdt.fedlearn.core.util.DataParseUtil;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class TestRFTrainData {

    RFTrainData df, df_test;
    String[][] rawTable;
    String path;
    Features features;
    List<SingleFeature> featureList = new ArrayList<>();

    @BeforeMethod
    public void setUp() {
        path = "./src/test/resources/regressionA/train0.csv";
        rawTable = DataParseUtil.loadTrainFromFile(path);
        for (int i=0; i<rawTable[0].length; i++) {
            featureList.add(new SingleFeature(rawTable[0][i], "double"));
        }
        features = new Features(featureList, "y");
    }

    @Test(priority = 1)
    public void testLoadData() {
//        df = new DataFrame();
//        df.loadFromFile(path);
    }


    @Test(priority = 2)
    public void testFillNa() {

    }

    @Test
    public void testNew() {
        List<String> categorical_features = new ArrayList<>();
        String[][] data = {{"uid","y"},{"asd","1"}};
        RFTrainData dataFrame = new RFTrainData(data, categorical_features);
        Assert.assertEquals(dataFrame.getTable(),data);
    }

}
