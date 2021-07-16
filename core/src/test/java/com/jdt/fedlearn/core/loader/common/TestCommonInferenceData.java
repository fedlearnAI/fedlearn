package com.jdt.fedlearn.core.loader.common;

import com.jdt.fedlearn.core.fake.StructureGenerate;
import com.jdt.fedlearn.core.type.data.Tuple3;
import com.jdt.fedlearn.core.util.DataParseUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestCommonInferenceData {
    public String baseDir = "./src/test/resources/regressionA/";
    public String fileName = "inference0.csv";

    @Test
    public void construct(){
        Tuple3<String[][], String, String[]> inferenceInput = StructureGenerate.inferenceInputStd();
        String[][] data = inferenceInput._1().get();
        String idColumnName = inferenceInput._2().get();
        String[] trainFeatures = inferenceInput._3().get();
        CommonInferenceData inferenceData = new CommonInferenceData(data, idColumnName,trainFeatures);

        Assert.assertEquals(inferenceData.getDatasetSize(), 4);
        Assert.assertEquals(inferenceData.getFeatureDim(), 3);
        Assert.assertEquals(inferenceData.getFeatureName(), new String[]{"HouseAge", "Longitude", "AveOccup"});
        Assert.assertEquals(inferenceData.getUid(), new String[]{"1", "100","10003", "8088"});
        //TODO add more assert
    }

    @Test
    public void testFilterOtherUidData() {
        String[][] data1 = DataParseUtil.loadTrainFromFile(baseDir + fileName);
        CommonInferenceData data = new CommonInferenceData(data1);
        System.out.println("Origin infer data size : " + data.datasetSize);
        System.out.println("Origin infer data uid: ");
        for (int i = 0; i < data.datasetSize; i++) {
            System.out.print(data.uid[i] + " ");
        }
        System.out.println();

        String[] saveUid = new String[]{"291B", "292A" ,"293B"};
        System.out.println("should save uid : ");
        for (int i = 0; i < saveUid.length; i++) {
            System.out.print(saveUid[i] + " ");
        }
        System.out.println();
        System.out.println();

         data.filterOtherUid(saveUid);
        System.out.println("After filter, infer data size : " + data.getDatasetSize());
        System.out.println("After filter, infer data uid: ");
        for (int i = 0; i < data.getDatasetSize(); i++) {
            System.out.print(data.getUid()[i] + " ");
        }
        System.out.println();
    }

}