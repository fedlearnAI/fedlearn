package com.jdt.fedlearn.core.integratedTest.verticalLinear;

import com.jdt.fedlearn.core.dispatch.VerticalLinearRegression;
import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.common.entity.core.feature.Features;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;
import com.jdt.fedlearn.core.type.data.Tuple2;
import com.jdt.fedlearn.core.util.DataParseUtil;
import com.jdt.fedlearn.core.metrics.Metric;
import com.jdt.fedlearn.core.example.CommonRun;
import com.jdt.fedlearn.core.model.Model;
import com.jdt.fedlearn.core.model.VerticalLinearModel;
import com.jdt.fedlearn.core.parameter.VerticalLinearParameter;
import com.jdt.fedlearn.core.type.MappingType;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.OptimizerType;
import com.jdt.fedlearn.core.util.FileUtil;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class TestVerticalLinearAB {
    //服务端维护
    private static final String trainId = "19";
    private static final String modelToken = trainId + "_" + AlgorithmType.VerticalLinearRegression;
    private static final MetricType[] metrics = new MetricType[]{MetricType.RMSE};
    private static final VerticalLinearParameter parameter = new VerticalLinearParameter(0.1, 0.2, metrics, OptimizerType.BatchGD, 0, 10, "L1", 0.001, 0.0);
    private static final VerticalLinearRegression regression = new VerticalLinearRegression(parameter);
    private static final Map<ClientInfo, String[][]> inferenceRawData = new HashMap<>();
    private ClientInfo[] clientInfos;
    private static Map<ClientInfo, String[][]> rawDataMap = new HashMap<>();
    //    private Map<ClientInfo, Features> featuresMap = new HashMap<>();
    //此处为需要手动配置的四个选项，分别是数据文件夹目录，参与方个数，有label的参与方id，label的名字。
    private String baseDir = "./src/test/resources/regressionA_TwoPartner/";
    //    private String baseDir = "D:\\Data\\public_data\\housing_price\\fetch_california_housing\\ab\\AB_300\\";
    private static final int partnerSize = 2;
    private static final int labelIndex = 0;
    private static final String labelName = "y";

    public void init() {
        //---------------------------下面不需要手动设置-------------------------------------//
        this.clientInfos = new ClientInfo[partnerSize];
        for (int i = 0; i < partnerSize; i++) {
            this.clientInfos[i] = new ClientInfo("127.0.0.1", 80 + i, "http");
            String fileName = "train" + i + ".csv";
            String[][] data1 = DataParseUtil.loadTrainFromFile(baseDir + fileName);
            rawDataMap.put(clientInfos[i], data1);
            String inferenceFileName = "inference" + (i) + ".csv";
            String[][] inferenceData = DataParseUtil.loadTrainFromFile(baseDir + inferenceFileName);
            inferenceRawData.put(clientInfos[i], inferenceData);
        }
        //设置客户端是否有label
//        clientInfos[labelIndex].setHasLabel(true);
    }

    public void testTrainAndTest() throws IOException {
        ////-----------id match and feature process-------------////////////////
        Tuple2<MatchResult, String[]> mappingOutput = CommonRun.match(MappingType.MD5, Arrays.asList(clientInfos.clone()), rawDataMap);
        String[] commonIds = mappingOutput._2();

        MatchResult matchResult = mappingOutput._1();
        Map<ClientInfo, Features> featuresMap = new HashMap<>();
        for (Map.Entry<ClientInfo, String[][]> entry : rawDataMap.entrySet()) {
            Features features = DataParseUtil.fetchFeatureFromData(entry.getValue());
            featuresMap.put(entry.getKey(), features);
        }
        //设置哪方那个特征是label
        featuresMap.get(clientInfos[labelIndex]).setLabel(labelName);
        ////----------id match end------------/////////////////

        Map<ClientInfo, Model> modelMap = new HashMap<>(); //每个客户端维护自己的，所以此处有n份
        for (ClientInfo client : clientInfos) {
            modelMap.put(client, new VerticalLinearModel());
        }

        List<CommonRequest> initRequests = regression.initControl(Arrays.asList(clientInfos.clone()), matchResult, featuresMap, new HashMap<>());
        CommonRun.train(regression, initRequests, modelMap, rawDataMap, commonIds);

        //model save
        for (Map.Entry<ClientInfo, Model> x : modelMap.entrySet()) {
            VerticalLinearModel verticalLinearModel = (VerticalLinearModel) x.getValue();
            System.out.println(x.getKey().url());
            System.out.println(x.getKey().url());
            System.out.println(x.getKey().url());
            String key = x.getKey().getPort() + "";
            String content = verticalLinearModel.serialize();
            FileUtil.saveModel(content, "./" + modelToken + "_" + key + ".model");
        }
    }

    public void testInference() throws IOException {
        // 加载模型
        Map<ClientInfo, Model> modelMap = new HashMap<>();
        for (ClientInfo clientInfo : clientInfos) {
            String path = "./" + modelToken + "_" + clientInfo.getPort() + ".model";
            String content = FileUtil.loadModel(path);
            VerticalLinearModel tmp = new VerticalLinearModel();
            tmp.deserialize(content);
            modelMap.put(clientInfo, tmp);
        }
//        String[] predictUid = DataParseUtil.loadInferenceUidList(baseDir + "inference0.csv");
        String[] predictUid = new String[]{"291B", "292", "293B", "294C", "295B", "296"};
        System.out.println("predictUid.length " + predictUid.length);
        long start = System.currentTimeMillis();

        List<CommonRequest> requests = regression.initInference(Arrays.asList(clientInfos.clone()), predictUid,new HashMap<>()); //initial request
        double[][] result = CommonRun.inference(regression, requests, modelMap, inferenceRawData).getPredicts();
        System.out.println((System.currentTimeMillis() - start) + " ms");

        System.out.println("inference result is " + Arrays.toString(result));
//        double[] label =loadInferenceLabelList1(baseDir + "inference0_ori.csv");
//        double[] label =loadInferenceLabelList1(baseDir + "train0.csv");
//        System.out.println("inference label is " + Arrays.toString(label));
//        testMetric("reg:square",result,label);
//        Assert.assertEquals(result[0], 88.51921904333341);
    }

    public static void main(String[] args) throws IOException {
        TestVerticalLinearAB verticalLinear = new TestVerticalLinearAB();
        verticalLinear.init();
        verticalLinear.testTrainAndTest();
        verticalLinear.testInference();
    }

    public static double[] loadInferenceLabelList1(String path) throws IOException {
        List<Double> res = new ArrayList<>();

        //根据uid，从文件中加载数据，
//        String fullPath = DataLoad.class.getResource(path).getPath();

//        FileReader reader = new FileReader(fullPath);
        FileReader reader = new FileReader(path);
        BufferedReader br = new BufferedReader(reader);
        String line = br.readLine();
        System.out.println(line);
        while ((line = br.readLine()) != null) {
//            long uid = Long.parseLong(line);
            double uid = Double.parseDouble(line.split(",")[line.split(",").length - 1]);
            res.add(uid);
        }
        reader.close();
        br.close();
//        System.out.println("=============" + res.toArray(new Double[0]));
        double[] ress = new double[res.size()];
        for (int i = 0; i < res.size(); i++) {
            ress[i] = res.get(i);
        }
        return ress;


//        return res.toArray(new Double[0]);
    }

    public static void testMetric(String objective, double[] result, double[] label) {
        if (objective.equals("multi:softmax") || objective.equals("multi:softprob")) {
//            double[] resultReshape = Arrays.stream(MathExt.transpose(Tool.reshape(result, label.length))).flatMapToDouble(Arrays::stream).toArray();
//            double testMacc = Metric.calculateMetric(MetricType.MACC,resultReshape,label);
//            double testMerror = Metric.calculateMetric(MetricType.MERROR,resultReshape,label);
            double testMacc = Metric.calculateMetric(MetricType.MACC, result, label);
            double testMerror = Metric.calculateMetric(MetricType.MERROR, result, label);
            System.out.println("test's macc is " + testMacc + "  merror is " + testMerror);
        } else if (objective.equals("binary:logistic") || objective.equals("reg:logistic")) {
            double testAcc = Metric.calculateMetric(MetricType.ACC, result, label);
            double testAuc = Metric.calculateMetric(MetricType.AUC, result, label);
            double testRecall = Metric.calculateMetric(MetricType.RECALL, result, label);
            double testF1 = Metric.calculateMetric(MetricType.F1, result, label);
            System.out.println("test's acc is " + testAcc + "  auc is " + testAuc + " recall is " + testRecall + " F1 is " + testF1);
        } else {
            double testRmse = Metric.calculateMetric(MetricType.RMSE, result, label);
            double testMape = Metric.calculateMetric(MetricType.MAPE, result, label);
            double testMaape = Metric.calculateMetric(MetricType.MAAPE, result, label);
            System.out.println("test's rmse is " + testRmse + "  mape is " + testMape + " maape is " + testMaape);
        }
    }


}