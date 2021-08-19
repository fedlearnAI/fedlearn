package com.jdt.fedlearn.core.model;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.common.InferenceInit;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.entity.kernelLinearRegression.DataUtils;
import com.jdt.fedlearn.core.entity.kernelLinearRegression.InferenceReqAndRes;
import com.jdt.fedlearn.core.entity.kernelLinearRegression.TrainReq;
import com.jdt.fedlearn.core.entity.kernelLinearRegression.TrainRes;
import com.jdt.fedlearn.core.fake.StructureGenerate;
import com.jdt.fedlearn.core.loader.common.CommonInferenceData;
import com.jdt.fedlearn.core.loader.kernelLinearRegression.KernelLinearRegressionTrainData;
import com.jdt.fedlearn.core.math.MathExt;
import com.jdt.fedlearn.core.model.serialize.KernelJavaSerializer;
import com.jdt.fedlearn.core.parameter.KernelLinearRegressionParameter;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.NormalizationType;
import com.jdt.fedlearn.core.type.data.Tuple3;
import com.jdt.fedlearn.grpc.federatedlearning.Vector;
import org.ejml.simple.SimpleMatrix;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;


public class TestKernelLinearRegressionJavaModel {
    private static final String modelId = "123-KernelLinearRegressionJava-2103012220";

    @Test
    public void trainInit() {
        KernelLinearRegressionJavaModel model = new KernelLinearRegressionJavaModel();
        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainClassInputStd();
        String[][] raw = compoundInput._1().get();
        String[] result = compoundInput._2().get();
        Features features = compoundInput._3().get();
        KernelLinearRegressionTrainData trainData = model.trainInit(raw, result,new int[0],new KernelLinearRegressionParameter(),  features, new HashMap<>());
        double[] label = new double[]{1,0,1};
        Assert.assertEquals(trainData.getDatasetSize(), 3);
        Assert.assertEquals(trainData.getFeatureDim(), 2);
        Assert.assertEquals(trainData.getUid(), new String[]{"1B", "2A", "3A"});
        Assert.assertEquals(trainData.getLabel(), label);
    }

    @Test
    public void trainPhase1(){
        KernelLinearRegressionJavaModel model = new KernelLinearRegressionJavaModel();
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        List<Integer> sampleIndex = new ArrayList<>();
        sampleIndex.add(0);
        sampleIndex.add(1);
        sampleIndex.add(2);
        boolean isUpdate = false;
        Message trainReq = new TrainReq(clientInfos.get(0), (double[]) null,sampleIndex,isUpdate);
        int p = 1;
        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainInputStd();
        String[][] raw = compoundInput._1().get();
        String[] result = compoundInput._2().get();
        Features features = compoundInput._3().get();
        Vector[] modelParms = new Vector[1];
        modelParms[0] = DataUtils.arrayToVector(new double[]{0.25,0.2,0.3,0.35});
        KernelLinearRegressionTrainData kernelLinearRegressionTrainData = new KernelLinearRegressionTrainData(raw,result,features);
        KernelLinearRegressionParameter parameter = new KernelLinearRegressionParameter(3, 100, 4, 3, 0.005, 200, new MetricType[]{MetricType.TRAINLOSS,MetricType.AUC}, NormalizationType.STANDARD);
        model.setForUnitTest(kernelLinearRegressionTrainData,parameter,modelParms,sampleIndex,1);
        Message res = model.train(p,trainReq,kernelLinearRegressionTrainData);
        TrainRes resTrainRes = (TrainRes) res;
        boolean isActive = false;
        double[] vector = new double[]{-0.006610813267752591,-0.006610813267752591,-0.006610813267752591};
        double paraNorm = 1.3110855618328105E-4;
        Map<MetricType, List<Double>> metricTypeListMap = new HashMap<>();
        TrainRes target = new TrainRes(clientInfos.get(0),vector,metricTypeListMap,paraNorm,isActive);
        Assert.assertEquals(resTrainRes.getClient(),target.getClient());
    }

    @Test
    public void trainPhase2Active(){
        KernelLinearRegressionJavaModel model = new KernelLinearRegressionJavaModel();
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        List<Integer> sampleIndex = new ArrayList<>();
        sampleIndex.add(0);
        sampleIndex.add(1);
        sampleIndex.add(2);
        double[][] valuelist = new double[][]{{0,0,0}};
        boolean isUpdate = false;
        Message trainReq = new TrainReq(clientInfos.get(0),valuelist,isUpdate);
        int p = 2;
        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainInputStd();
        String[][] raw = compoundInput._1().get();
        String[] result = compoundInput._2().get();
        Features features = compoundInput._3().get();
        KernelLinearRegressionTrainData kernelLinearRegressionTrainData = new KernelLinearRegressionTrainData(raw,result,features);
        KernelLinearRegressionParameter parameter = new KernelLinearRegressionParameter(3, 100, 4, 3, 0.005, 200, new MetricType[]{MetricType.TRAINLOSS,MetricType.AUC}, NormalizationType.STANDARD,1);
        Vector[] modelParms = new Vector[1];
        modelParms[0] = DataUtils.alloneVector((int) parameter.getMapdim(), 0.1);
        model.setForUnitTest(kernelLinearRegressionTrainData,parameter,modelParms,sampleIndex,1);
        boolean isActive = true;
        Message res = model.train(p,trainReq,kernelLinearRegressionTrainData);
        TrainRes resTrainRes = (TrainRes) res;
        String msg = "Phase 2 finish!";
        TrainRes target = new TrainRes(clientInfos.get(0),msg,isActive);
        Assert.assertEquals(resTrainRes.getClient(),target.getClient());
        Assert.assertEquals(resTrainRes.getNumClassRound(),0);
    }

    @Test
    public void trainPhase2(){
        KernelLinearRegressionJavaModel model = new KernelLinearRegressionJavaModel();
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        List<Integer> sampleIndex = new ArrayList<>();
        sampleIndex.add(0);
        sampleIndex.add(1);
        sampleIndex.add(2);
        double[][] valuelist = new double[][]{{0,0,0}};
        boolean isUpdate = true;
        Message trainReq = new TrainReq(clientInfos.get(0),valuelist,isUpdate);
        int p = 2;
        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainInputStd();
        String[][] raw = compoundInput._1().get();
        String[] result = compoundInput._2().get();
        Features features = compoundInput._3().get();
        KernelLinearRegressionTrainData kernelLinearRegressionTrainData = new KernelLinearRegressionTrainData(raw,result,features);
        KernelLinearRegressionParameter parameter = new KernelLinearRegressionParameter(3, 100, 4, 3, 0.005, 200, new MetricType[]{MetricType.TRAINLOSS,MetricType.AUC}, NormalizationType.STANDARD);
        Vector[] modelParms = new Vector[1];
        modelParms[0] = DataUtils.alloneVector((int) parameter.getMapdim(), 0.1);
        model.setForUnitTest(kernelLinearRegressionTrainData,parameter,modelParms,sampleIndex,1);
        Message res = model.train(p,trainReq,kernelLinearRegressionTrainData);
        TrainRes resTrainRes = (TrainRes) res;
        boolean isActive = false;
        String msg = "Phase 2 finish!";
        TrainRes target = new TrainRes(clientInfos.get(0),msg,isActive);
        Assert.assertEquals(resTrainRes.getClient(),target.getClient());
        Assert.assertEquals(resTrainRes.getNumClassRound(),1);
    }






    @Test
    public void KernelApproximationTrain(){
        KernelLinearRegressionJavaModel model = new KernelLinearRegressionJavaModel();
        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainInputStd();
        String[][] raw = compoundInput._1().get();
        String[] result = compoundInput._2().get();
        Features features = compoundInput._3().get();
        KernelLinearRegressionParameter kernelLinearRegressionParameter =  new KernelLinearRegressionParameter(3, 100, 5, 100, 0.005, 200, new MetricType[]{MetricType.TRAINLOSS}, NormalizationType.STANDARD);

        KernelLinearRegressionTrainData trainData = model.trainInit(raw, result, new int[0],kernelLinearRegressionParameter, features, new HashMap<>());
        double[][] data = new double[][]{{1,2,3},{4,5,6}};
        SimpleMatrix simpleMatrix = new SimpleMatrix(data);
        SimpleMatrix res = model.initKernelApproximation(simpleMatrix);
        double[][] resD = DataUtils.smpmatrixToArray(res);
        System.out.println("resD: " + Arrays.deepToString(resD));
        double[][] targetD = new double[][]{{-0.38220433023675404, 0.0026063820944554365, -0.5448879755955053, 0.606147993418652, -0.24644290381559317}, {-0.5643657928856695, 0.1601091497445988, -0.6317011034901413, 0.565267939736407, -0.21451641758115533}};
        SimpleMatrix target = new SimpleMatrix(targetD);
        Assert.assertEquals(res.get(0,0),target.get(0,0));
    }

    @Test
    public void kernelApproximationPhase2(){
        KernelLinearRegressionJavaModel model = new KernelLinearRegressionJavaModel();
        double[][] data = new double[][]{{1,2,3},{4,5,6}};
        SimpleMatrix simpleMatrix = new SimpleMatrix(data);
        double[][] trans = new double[][]{{1,1,2,2},{1,0,0,1}};
        SimpleMatrix transMatrix = new SimpleMatrix(trans);
        double[] bias = new double[]{4.1,2.1,1,2};
        Vector vector = DataUtils.arrayToVector(bias);
        SimpleMatrix res = model.kernelApproximation(simpleMatrix,transMatrix,vector);
        double[][] resD = DataUtils.smpmatrixToArray(res);
        System.out.println("resD : " + Arrays.deepToString(resD));
        double[][] targetD = new double[][]{{0.48404758988035423, -0.7064952400800761, -0.7000304076699752, 0.6789429207843051}, {0.6087953328757344, 0.6952757805494589, -0.64426638672293, -0.5371804747679276}};
        SimpleMatrix target = new SimpleMatrix(targetD);
        Assert.assertEquals(res.get(0,0),target.get(0,0));
    }

    @Test
    public void kernelLinearRegressionInferencePhase1(){
        KernelLinearRegressionJavaModel model = new KernelLinearRegressionJavaModel();
        double[][] data = new double[][]{{1,2,3},{4,5,6}};
        SimpleMatrix simpleMatrix = new SimpleMatrix(data);
        double[] bias = new double[]{4.1,2.1,1};
        Vector vector = DataUtils.arrayToVector(bias);
        Vector res = model.computePredict(simpleMatrix,vector);
        double[] target = new double[]{11.3, 32.9};
        Vector targetVector = DataUtils.arrayToVector(target);
        Assert.assertEquals(res,targetVector);
    }


    @Test
    public void inferenceInit() {
        KernelLinearRegressionJavaModel model = new KernelLinearRegressionJavaModel();
        String[] uidList = new String[]{"aa", "1a", "c3"};
        String[][] data = new String[2][];
        data[0] = new String[]{"aa", "10", "12.1"};
        data[1] = new String[]{"1a", "10", "12.1"};
        Message msg = model.inferenceInit(uidList, data, new HashMap<>());
        com.jdt.fedlearn.core.entity.common.InferenceInitRes res = (com.jdt.fedlearn.core.entity.common.InferenceInitRes) msg;
        Assert.assertFalse(res.isAllowList());
        Assert.assertEquals(res.getUid(), new int[]{2});
    }


    @Test
    public void inference1() {
        double[][] matWeight = new double[][]{{0.1,0.2,0.3,0.4},{0.01,0.02,0.03,0.04}};
        SimpleMatrix TransMat = new SimpleMatrix(matWeight);
        Vector[] modelParms = new Vector[1];
        modelParms[0] = DataUtils.arrayToVector(new double[]{0.25,0.2,0.3,0.35});
        double[] bias = new double[]{0.2,0.4,0.1,0.3};
        Vector biasV = DataUtils.arrayToVector(bias);
        KernelLinearRegressionJavaModel model = new KernelLinearRegressionJavaModel(modelId,TransMat,biasV,4,modelParms);
        String[][] data = new String[3][3];
        data[0] = new String[]{"uid","age","height"};
        data[1] = new String[]{"aa", "10", "1.2"};
        data[2] = new String[]{"1a", "8", "1.1"};
//        StringArray stringArray = new StringArray(new String[]{"aa", "1a"});
        InferenceInit init = new InferenceInit(new String[]{"aa", "1a"});
        CommonInferenceData inferenceData = new CommonInferenceData(data,"uid",new String[0]);
        Message msg = model.inference(-1, init, inferenceData);
        InferenceReqAndRes res = (InferenceReqAndRes) msg;

    }

    @Test
    public void inference2() {
        double[][] matWeight = new double[][]{{0.1,0.2,0.3,0.4},{0.01,0.02,0.03,0.04}};
        SimpleMatrix TransMat = new SimpleMatrix(matWeight);
        Vector[] modelParms = new Vector[1];
        modelParms[0] = DataUtils.arrayToVector(new double[]{0.25,0.2,0.3,0.35});
        double[] bias = new double[]{0.2,0.4,0.1,0.3};
        Vector biasV = DataUtils.arrayToVector(bias);
        KernelLinearRegressionJavaModel model = new KernelLinearRegressionJavaModel(modelId,TransMat,biasV,4,modelParms);
        String[][] data = new String[3][3];
        data[0] = new String[]{"uid","age","height"};
        data[1] = new String[]{"aa", "10", "1.2"};
        data[2] = new String[]{"1a", "8", "1.1"};
        InferenceReqAndRes inferenceReqAndRes = new InferenceReqAndRes(new ClientInfo("127.0.0.1",8094,"http"));
        CommonInferenceData inferenceData = new CommonInferenceData(data,"uid",new String[0]);
        model.setForInferTest(inferenceData,1);
        Message msg2 = model.inference(-2, inferenceReqAndRes, inferenceData);
        InferenceReqAndRes res = (InferenceReqAndRes) msg2;
        Assert.assertNull(res);
    }

    @Test
    public void inference3() {
        double[][] matWeight = new double[][]{{0.1,0.2,0.3,0.4},{0.01,0.02,0.03,0.04}};
        SimpleMatrix TransMat = new SimpleMatrix(matWeight);
        Vector[] modelParms = new Vector[1];
        modelParms[0] = DataUtils.arrayToVector(new double[]{0.25,0.2,0.3,0.35});
        double[] bias = new double[]{0.2,0.4,0.1,0.3};
        Vector biasV = DataUtils.arrayToVector(bias);
        KernelLinearRegressionJavaModel model = new KernelLinearRegressionJavaModel(modelId,TransMat,biasV,4,modelParms);
        String[][] data = new String[3][3];
        data[0] = new String[]{"uid","age","height"};
        data[1] = new String[]{"aa", "10", "1.2"};
        data[2] = new String[]{"1a", "8", "1.1"};
        InferenceReqAndRes inferenceReqAndRes = new InferenceReqAndRes(new ClientInfo("127.0.0.1",8094,"http"));
        CommonInferenceData inferenceData = new CommonInferenceData(data,"uid",new String[0]);
        int numClass =1;
        model.setForInferTest(inferenceData,numClass);
        Message msg3 = model.inference(-3, inferenceReqAndRes, inferenceData);
        InferenceReqAndRes res = (InferenceReqAndRes) msg3;
        System.out.println("res predict : " + res.getPredicts());
        Map<String, Double> predict = new HashMap<>();
        predict.put("aa",-0.34479876270682164);
        predict.put("1a",-0.36955856638707657);
        double[] pre = new double[]{-0.34479876270682164,-0.36955856638707657};
        double[] resPre = MathExt.transpose(res.getPredicts())[0];
        System.out.println("resP " + Arrays.toString(resPre));
        Assert.assertEquals(resPre,pre);
    }


    @Test
    public void serializeAndDeserialize(){
        KernelLinearRegressionJavaModel model = new KernelLinearRegressionJavaModel();
        String content = "modelToken=62_KernelLinearRegression\n" +
                "numClass=1\n"+
                "weight=-12.91338388635032,-491.6280181664785,160.26338717786908,199.487545938004,2.2567552519226552,513.3587616657517,298.1833558164752,88.98096546798264,12.077925742461701,-46.8863860865105\n" +
                "matsize=2,10\n" +
                "matweight=0.08452060657049848,0.09128761787534406,-0.028707863647499533,0.07518594314874759,0.1335473668231534,-0.09499789372646104,0.0599049892177836,0.1204570743449295,0.24820093995603615,-0.07539501059617072,-0.140721021116148,-0.06725299844022434,0.0694659525199853,-0.07186670909137784,-0.04007984801098624,0.07859301876719066,-0.09296545644893407,-0.013778888246642598,0.005442869656443446,-0.09561780298677987\n" +
                "biasweight=4.591117485041855,4.707171442994805,2.188494408434079,5.637758559745272,4.449608312623372,2.2111457602866462,0.7586069841437223,5.340161507623025,0.5228849046590456,5.835496245229808\n" +
                "norm_type=NONE\n" +
                "norm_params_1=0.5011086371864587,0.533484297559456\n" +
                "norm_params_2=0.2937062123684964,0.29909706583021484\n"+
                "isActive=false\n"+
                "multiClassUniqueLabelList=\n";
        double[][] modelParas = new double[][]{{0.1,0.2},{0.2,0.3}};
        double[][] matweight=new double[][]{{0.1,0.2},{0.2,0.3}};
        String modelToken="62_KernelLinearRegression";
        int numClass=1;
        double mapdim=2.0;
        double[] bias=new double[]{0.1,0.2};
        double[] normParams1=new double[]{0.1,0.2};
        double[] normParams2=new double[]{0.1,0.2};
        boolean isActive=true;
        List<Double> multiClassUniqueLabelList = new ArrayList<>();
        multiClassUniqueLabelList.add(1D);
        NormalizationType normalizationType = NormalizationType.STANDARD;
        KernelJavaSerializer kernelJavaSerializer = new KernelJavaSerializer(modelToken, numClass,  mapdim, modelParas, matweight, bias, normalizationType, normParams1, normParams2,  isActive, multiClassUniqueLabelList);
        String str = kernelJavaSerializer.toJson();
        System.out.println(str);
        model.deserialize(str);
        String modelStr = model.serialize();
        System.out.println("content " + str);
        System.out.println("modeS " + modelStr);
        Assert.assertEquals(modelStr,str);
    }

}