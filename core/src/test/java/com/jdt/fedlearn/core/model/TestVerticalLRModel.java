package com.jdt.fedlearn.core.model;

import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.base.DoubleArray;
import com.jdt.fedlearn.core.entity.base.StringArray;
import com.jdt.fedlearn.core.entity.common.InferenceInitRes;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.fake.StructureGenerate;
import com.jdt.fedlearn.core.loader.common.CommonInferenceData;
import com.jdt.fedlearn.core.loader.verticalLinearRegression.VerticalLinearTrainData;
import com.jdt.fedlearn.core.parameter.VerticalLRParameter;
import com.jdt.fedlearn.core.preprocess.Scaling;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.core.type.data.Tuple3;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashMap;


public class TestVerticalLRModel {
    @Deprecated
    private static final String modelId = "123" + "_" + AlgorithmType.VerticalLR;
    private String[][] raw;  // raw data: each row is a data point
    private String[] result; // id match info
    private Features features;  // feature and label column names
    private int[] testIndex;

    @BeforeMethod
    public void setup() {
        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainClassInputStd();
        raw = compoundInput._1().get(); // raw data: each row is a data point
        result = compoundInput._2().get(); // id match info
        features = compoundInput._3().get(); // feature and label column names
        testIndex = new int[0];
    }

    @Test
    public void trainInit() {
        VerticalLRModel model = new VerticalLRModel();

        VerticalLinearTrainData trainData = model.trainInit(raw, result, testIndex, new VerticalLRParameter(),  features, new HashMap<>());
        double[] label = new double[]{1, 0, 1};
        Scaling scaling = trainData.getScaling();
        double[] scales = new double[]{0.14285714285714285, 0.01020408163265306};
        double[] X_max = new double[]{8, 183};
        double[] X_min = new double[]{1, 85};
        Assert.assertEquals(trainData.getDatasetSize(), 3);
        Assert.assertEquals(trainData.getFeatureDim(), 2);
        Assert.assertEquals(trainData.getUid(), new String[]{"1B", "2A", "3A"});
        Assert.assertEquals(trainData.getLabel(), label);
        Assert.assertEquals(scaling.getScales(), scales);
        Assert.assertEquals(scaling.getX_min(), X_min);
        Assert.assertEquals(scaling.getX_max(), X_max);
       // Assert.assertNull(model.getDifferentialPrivacy());
        double[] noises = model.getDifferentialPrivacy().getNoises();
        Assert.assertEquals(noises.length, 3);
//        for(double noise: noises){
//            System.out.println(noise);
//        }
    }

    @Test
    public void inferenceInit() {
        VerticalLRModel model = new VerticalLRModel();
        String[] uidList = new String[]{"aa", "1a", "c3"};
        String[][] data = new String[2][];
        data[0] = new String[]{"aa", "10", "12.1"};
        data[1] = new String[]{"1a", "10", "12.1"};
        Message msg = model.inferenceInit(uidList, data, new HashMap<>());
        InferenceInitRes res = (InferenceInitRes) msg;
        Assert.assertFalse(res.isAllowList());
        Assert.assertEquals(res.getUid(), new int[]{2});
    }


    @Test
    public void inference1() {
        double[] weight = new double[]{0.1, 0.5, 1};
        Scaling scaling = new Scaling();
        double[] scales = new double[]{0.14285714285714285, 0.01020408163265306};
        double[] xMin = new double[]{1, 85};
        scaling.setScales(scales);
        scaling.setX_min(xMin);
        VerticalLRModel model = new VerticalLRModel(modelId, weight, scaling);
        String[][] data = new String[3][3];
        data[0] = new String[]{"uid", "age", "height"};
        data[1] = new String[]{"aa", "10", "1.2"};
        data[2] = new String[]{"1a", "8", "1.1"};
        StringArray stringArray = new StringArray(new String[]{"aa", "1a"});
        CommonInferenceData inferenceData = new CommonInferenceData(data, "uid", new String[0]);
        DoubleArray res = model.inferencePhase1(stringArray, inferenceData);
        double[] predict = new double[]{0.7010204081632654, 0.6719387755102041};
        System.out.println("predict : " + Arrays.toString(res.getData()));
        Assert.assertEquals(res.getData(), predict);
    }

    @Test
    public void inference() {
        double[] weight = new double[]{0.1, 0.5, 1};
        Scaling scaling = new Scaling();
        double[] scales = new double[]{0.14285714285714285, 0.01020408163265306};
        double[] xMin = new double[]{1, 85};
        scaling.setScales(scales);
        scaling.setX_min(xMin);
        VerticalLRModel model = new VerticalLRModel(modelId, weight, scaling);
        String[][] data = new String[3][3];
        data[0] = new String[]{"uid", "age", "height"};
        data[1] = new String[]{"aa", "10", "1.2"};
        data[2] = new String[]{"1a", "8", "1.1"};
        StringArray stringArray = new StringArray(new String[]{"aa", "1a"});
        CommonInferenceData inferenceData = new CommonInferenceData(data, "uid", new String[0]);
        Message msg = model.inference(-2, stringArray, inferenceData);
        DoubleArray linearN1Response = (DoubleArray) msg;
        double[] predict = new double[]{0.7010204081632654, 0.6719387755102041};
        System.out.println("predict : " + Arrays.toString(linearN1Response.getData()));
        Assert.assertEquals(linearN1Response.getData(), predict);
    }
}