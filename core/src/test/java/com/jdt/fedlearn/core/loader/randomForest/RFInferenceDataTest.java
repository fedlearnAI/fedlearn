package com.jdt.fedlearn.core.loader.randomForest;

import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.fake.StructureGenerate;
import com.jdt.fedlearn.core.loader.mixGBoost.MixGBTrainData;
import com.jdt.fedlearn.core.type.data.Tuple3;
import com.jdt.fedlearn.core.entity.randomForest.DataUtils;
import org.ejml.simple.SimpleMatrix;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Map;

import static org.testng.Assert.*;

public class RFInferenceDataTest {

    @Test
    public void testGetUidFeature() {
        Tuple3<String[][], String, String[]> compoundInput = StructureGenerate.inferenceInputStd();
        String[][] input = compoundInput._1().get();
        RFInferenceData data = new RFInferenceData(input);
        String[][] res = data.getUidFeature();
        String[] x1 = new String[]{"1", "21.0", "-122.22", "2.109841828"};
        String[] x2 = new String[]{"100", "29.0", "-122.25", "1.8432"};
        String[] x3 = new String[]{"10003", "12.0", "-121.03", "2.848056537"};
        String[] x4 = new String[]{"8088", "34.0", "-118.21", "3.88172043"};
        String[][] target = new String[][]{x1, x2, x3, x4};
        for (int i = 0; i < res.length; i++) {
            Assert.assertEquals(res[i], target[i]);
        }
    }

    @Test
    public void testInit() {
        Tuple3<String[][], String, String[]> compoundInput = StructureGenerate.inferenceInputStd();
        String[][] input = compoundInput._1().get();
        RFInferenceData data = new RFInferenceData(input);
        data.init();
        Assert.assertEquals(data.numCols(), 3);
        Assert.assertEquals(data.numRows(), 4);
    }

    @Test
    public void testFillna() {
        Tuple3<String[][], String, String[]> compoundInput = inferenceInputStdHasNull();
        String[][] input = compoundInput._1().get();
        RFInferenceData data = new RFInferenceData(input);
        data.init();
        data.fillna(0);
        String[] uids = {"1","100","8088"};
        SimpleMatrix resSmpMat = data.selectToSmpMatrix(uids);
        double[] x1 = new double[]{21.0, -122.22, 2.109841828};
        double[] x2 = new double[]{0, -122.25, 1.8432};
        double[] x4 = new double[]{34.0, 0, 3.88172043};
        double[][] x = new double[][]{x1, x2, x4};
        for (int i = 0; i < x.length; i++) {
            for (int j = 0; j < x[i].length; j++) {
                Assert.assertEquals(resSmpMat.get(i,j),x[i][j]);
            }
        }
    }

    @Test
    public void testSelectToSmpMatrix() {
        Tuple3<String[][], String, String[]> compoundInput = StructureGenerate.inferenceInputStd();
        String[][] input = compoundInput._1().get();
        RFInferenceData data = new RFInferenceData(input);
        data.init();
        String[] uids = {"100","1","8088"};
        SimpleMatrix resSmpMat = data.selectToSmpMatrix(uids);
        double[] x1 = new double[]{21.0, -122.22, 2.109841828};
        double[] x2 = new double[]{29.0, -122.25, 1.8432};
        double[] x4 = new double[]{34.0, -118.21, 3.88172043};
        double[][] x = new double[][]{x2, x1, x4};
        for (int i = 0; i < x.length; i++) {
            for (int j = 0; j < x[i].length; j++) {
                Assert.assertEquals(resSmpMat.get(i,j),x[i][j]);
            }
        }

    }


    public static Tuple3<String[][], String, String[]> inferenceInputStdHasNull() {
        String[] x0 = new String[]{"uid", "HouseAge", "Longitude", "AveOccup"};
        String[] x1 = new String[]{"1", "21", "-122.22", "2.109841828"};
        String[] x2 = new String[]{"100", "", "-122.25", "1.8432"};
        String[] x3 = new String[]{"10003", "12", "-121.03", "2.848056537"};
        String[] x4 = new String[]{"8088", "34", "", "3.88172043"};

        String[][] input = new String[][]{x0, x1, x2, x3, x4};
        //uid 列的名称
        String idColumnName = "uid";
        //训练特征顺序，如果推理数据集顺序与此顺序不一致，需要调换推理集特征顺序
        String[] featureList = new String[]{"uid", "HouseAge", "Longitude", "AveOccup"};
        System.out.println("end");
        return new Tuple3<>(input, idColumnName, featureList);
    }
}