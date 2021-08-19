package com.jdt.fedlearn.core.model;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.base.StringArray;
import com.jdt.fedlearn.core.entity.common.InferenceInit;
import com.jdt.fedlearn.core.entity.common.InferenceInitRes;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.entity.kernelLinearRegression.DataUtils;
import com.jdt.fedlearn.core.entity.kernelLinearRegression.InferenceReqAndRes;
import com.jdt.fedlearn.core.fake.StructureGenerate;
import com.jdt.fedlearn.core.loader.common.CommonInferenceData;
import com.jdt.fedlearn.core.loader.kernelLinearRegression.KernelLinearRegressionTrainData;
import com.jdt.fedlearn.core.type.data.Tuple3;
import com.jdt.fedlearn.grpc.federatedlearning.Vector;
import org.ejml.simple.SimpleMatrix;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;


public class TestKernelLinearRegressionModel {
    private static final String modelId = "123-FederatedGB-2103012220";

//    @Test
    public void trainInit() {
        KernelLinearRegressionModel model = new KernelLinearRegressionModel();
        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainInputStd();
        String[][] raw = compoundInput._1().get();
        String[] result = compoundInput._2().get();
        Features features = compoundInput._3().get();
        Vector[] modelParms = new Vector[1];
        modelParms[0] = DataUtils.arrayToVector(new double[]{0.25,0.2,0.3,0.35});
        KernelLinearRegressionTrainData trainData = new KernelLinearRegressionTrainData(raw,result,features);
        double[] label = new double[]{1,0,1};
        Assert.assertEquals(trainData.getDatasetSize(), 3);
        Assert.assertEquals(trainData.getFeatureDim(), 2);
        Assert.assertEquals(trainData.getUid(), new String[]{"1B", "2A", "3A"});
        Assert.assertEquals(trainData.getLabel(), label);
    }

    @Test
    public void inferenceInit() {
        KernelLinearRegressionModel model = new KernelLinearRegressionModel();
        String[] uidList = new String[]{"aa", "1a", "c3"};
        String[][] data = new String[2][];
        data[0] = new String[]{"aa", "10", "12.1"};
        data[1] = new String[]{"1a", "10", "12.1"};
        Message msg = model.inferenceInit(uidList, data, new HashMap<>());
        InferenceInitRes res = (InferenceInitRes) msg;
        Assert.assertFalse(res.isAllowList());
        Assert.assertEquals(res.getUid(), new int[]{2});
    }


//    @Test
    public void inference1() {
        double[][] matWeight = new double[][]{{0.1,0.2,0.3,0.4},{0.01,0.02,0.03,0.04}};
        SimpleMatrix TransMat = new SimpleMatrix(matWeight);
        Vector modelParm = DataUtils.arrayToVector(new double[]{0.25,0.2,0.3,0.35});
        double[] bias = new double[]{0.2,0.4,0.1,0.3};
        Vector biasV = DataUtils.arrayToVector(bias);
        KernelLinearRegressionModel model = new KernelLinearRegressionModel(modelId,TransMat,biasV,4,modelParm);
        String[][] data = new String[3][3];
        data[0] = new String[]{"uid","age","height"};
        data[1] = new String[]{"aa", "10", "1.2"};
        data[2] = new String[]{"1a", "8", "1.1"};
        InferenceInit init = new InferenceInit(new String[]{"aa", "1a"});
        CommonInferenceData inferenceData = new CommonInferenceData(data,"uid",new String[0]);
        Message msg = model.inference(-1, init, inferenceData);
        InferenceReqAndRes res = (InferenceReqAndRes) msg;
        Assert.assertNull(res);
    }

//    @Test
    public void inference2() {
        double[][] matWeight = new double[][]{{0.1,0.2,0.3,0.4},{0.01,0.02,0.03,0.04}};
        SimpleMatrix TransMat = new SimpleMatrix(matWeight);
        Vector modelParm = DataUtils.arrayToVector(new double[]{0.25,0.2,0.3,0.35});
        double[] bias = new double[]{0.2,0.4,0.1,0.3};
        Vector biasV = DataUtils.arrayToVector(bias);
        KernelLinearRegressionModel model = new KernelLinearRegressionModel(modelId,TransMat,biasV,4,modelParm);
        String[][] data = new String[3][3];
        data[0] = new String[]{"uid","age","height"};
        data[1] = new String[]{"aa", "10", "1.2"};
        data[2] = new String[]{"1a", "8", "1.1"};
        InferenceReqAndRes inferenceReqAndRes = new InferenceReqAndRes(new ClientInfo("127.0.0.1",8094,"http"));
        CommonInferenceData inferenceData = new CommonInferenceData(data,"uid",new String[0]);
        StringArray stringArray = new StringArray(new String[]{"aa", "1a"});
        InferenceInit init = new InferenceInit(new String[]{"aa", "1a"});
        Message msg = model.inference(-1, init, inferenceData);
        Message msg2 = model.inference(-2, inferenceReqAndRes, inferenceData);
        Message msg3 = model.inference(-3, inferenceReqAndRes, inferenceData);
        InferenceReqAndRes res = (InferenceReqAndRes) msg3;
        System.out.println("res predict : " + res.getPredict().values().toString());
        Map<String, Double> predict = new HashMap<>();
        predict.put("aa",-0.34479876270682164);
        predict.put("1a",-0.36955856638707657);
        Assert.assertEquals(res.getPredict(),predict);
    }

}