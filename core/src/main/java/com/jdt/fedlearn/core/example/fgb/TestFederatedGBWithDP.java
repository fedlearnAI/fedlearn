//package com.jdt.fedlearn.core.example.fgb;
//
//import com.jdt.fedlearn.core.dispatch.FederatedGB;
//import com.jdt.fedlearn.common.entity.core.ClientInfo;
//import com.jdt.fedlearn.core.util.DataLoad;
//import com.jdt.fedlearn.core.example.CommonRun;
//import com.jdt.fedlearn.core.model.FederatedGBModel;
//import com.jdt.fedlearn.core.parameter.FgbParameter;
//import com.jdt.fedlearn.core.entity.common.CommonRequest;
//import com.jdt.fedlearn.common.entity.core.feature.Features;
//import com.jdt.fedlearn.core.loader.boost.BoostInferenceData;
//import com.jdt.fedlearn.core.metrics.Metric;
//import com.jdt.fedlearn.core.model.Model;
//import com.jdt.fedlearn.core.psi.MappingOutput;
//import com.jdt.fedlearn.core.type.BitLengthType;
//import com.jdt.fedlearn.core.type.MappingType;
//import com.jdt.fedlearn.core.type.MetricType;
//import com.jdt.fedlearn.core.type.ObjectiveType;
//
//import java.util.*;
//
///**
// * DifferentialPrivacy
// * TODO
// */
//
//public class TestFederatedGBWithDP {
//    //服务端维护
//    private ClientInfo[] clientInfos;
//    private static final MetricType[] metrics = new MetricType[]{MetricType.MAPE, MetricType.MAAPE};
//    private static final FgbParameter parameter = new FgbParameter(1, 1.0, 0.0, 7, 0.3, 33, ObjectiveType.regSquare, metrics, BitLengthType.bit1024, new String[0], 0 ,0);
//    private static FederatedGB boost = new FederatedGB(parameter);
//    String baseDir = "./src/test/resources/regressionA/";
//
//    //界面传给服务端
//    private Map<ClientInfo, Features> featuresMap = new HashMap<>();
//
//    //客户端维护
//    private static Map<ClientInfo, Model> modelMap = new HashMap<>(); //每个客户端维护自己的，所以此处有三份
//    private static Map<ClientInfo, String[][]> dataMap = new HashMap<>();
//    private static List<Double> train_rmse = new ArrayList<>();
//    private static List<Double> train_mape = new ArrayList<>();
//    private static List<Double> train_maape = new ArrayList<>();
//    private static List<Double> test_rmse = new ArrayList<>();
//    private static List<Double> test_mape = new ArrayList<>();
//    private static final int labelIndex = 0;
//    private static final String labelName = "y";
//
//    public void trainDPTest() {
//        double[][] DPparameters = new double[][]{{0,0}, {0.01,0}};//, {0.02,0}, {0,0.01}, {0,0.02}, {0.01, 0.01}, {0.02, 0.02}};
//        for (double[] p : DPparameters){
//            System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++");
//            System.out.println("Randomized_response_probability: " + p[0]);
//            System.out.println("Differential_privacy_parameter: " + p[1]);
//            reset(p);
//            setUp("client");
//            testTrain();
////            setUp("test");
//            testInference();
//            System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++");
//            System.out.println();
//        }
//        for (int i = 0;i < train_mape.size();i++){
//            System.out.printf("round: %d, rmse: %f, mape: %f, maape: %f \n", i, train_rmse.get(i), train_mape.get(i), train_maape.get(i));
//        }
//        System.out.println();
//        for (int i = 0;i < test_mape.size();i++){
//            System.out.printf("round: %d, rmse: %f, mape: %f \n", i, test_rmse.get(i), test_mape.get(i));
//        }
//    }
//
//
//    public void setUp(String task) {
//        ClientInfo client1 = new ClientInfo("127.0.0.1", 80, "http");
//        ClientInfo client2 = new ClientInfo("127.0.0.1", 81, "http");
//        ClientInfo client3 = new ClientInfo("127.0.0.1", 82, "http");
//        this.clientInfos = new ClientInfo[]{client1, client2, client3};
//
//        //三份数据，其中第一份拥有label
//        List<String> categorical_features = new ArrayList<>(Arrays.asList(parameter.getCatFeatures().split(",")));
//        String baseDir = "./src/test/resources/regressionA/";
////        String baseDir = "./src/test/resources/classificationA/";
////        String baseDir = "/Users/page/data/fl/omo_match/v17k/";
//        if (dataMap.size() == 0 || task.equals("client")) {
//            String[][] data1 = DataLoad.loadTrainFromFile(baseDir + "train0.csv");
//            String[][] data2 = DataLoad.loadTrainFromFile(baseDir + "train1.csv");
//            String[][] data3 = DataLoad.loadTrainFromFile(baseDir + "train2.csv");
//
//            dataMap.put(client1, data1);
//            dataMap.put(client2, data2);
//            dataMap.put(client3, data3);
//        }else {
//            dataMap.clear();
//            String[][] data1 = DataLoad.loadTestFromFile(baseDir + "inference1.csv", true);
//            String[][] data2 = DataLoad.loadTrainFromFile(baseDir + "inference2.csv");
//            String[][] data3 = DataLoad.loadTrainFromFile(baseDir + "inference3.csv");
//
//            dataMap.put(client1, data1);
//            dataMap.put(client2, data2);
//            dataMap.put(client3, data3);
//        }
//        if (modelMap.size() == 0 || task.equals("client")) {
//            modelMap.put(client1, new FederatedGBModel());
//            modelMap.put(client2, new FederatedGBModel());
//            modelMap.put(client3, new FederatedGBModel());
//        }
//    }
//
//
//    public void testTrain() {
//        MappingOutput idMap = CommonRun.match(MappingType.VERTICAL_MD5, Arrays.asList(clientInfos.clone()), dataMap);
//
//        Map<ClientInfo, Features> featuresMap = new HashMap<>();
//        for (Map.Entry<ClientInfo, String[][]> entry : dataMap.entrySet()) {
//            Features features = DataLoad.loadFeatureFromData(entry.getValue());
//            featuresMap.put(entry.getKey(), features);
//        }
//        //设置哪方那个特征是label
//        featuresMap.get(clientInfos[labelIndex]).setLabel(labelName);
//
//        //请求初始化
//        List<CommonRequest> initRequests = boost.initControl(Arrays.asList(clientInfos), idMap.getResult(), featuresMap, new HashMap<>());
//        //树的建立过程，第一次建立根节点，然后依次从上往下，从左往右建立节点
//        CommonRun.TrainAndTest(boost, initRequests, modelMap, dataMap);
////        train_rmse.add(boost.getScore()[0]);
////        train_mape.add(boost.getScore()[1]);
////        train_maape.add(boost.getScore()[2]);
////        boost.cleanScore();
//        //训练完成后，保存模型
//    }
//
//    public void testInference() {
////        testTrain();
//        //根据需要预测的id，和任务id，生成预测请求
////        int len = dataMap.get(clientInfos.get(0)).label.length;
////        long[] predictUid = new long[len];
////        for (int i=0;i<len;i++){
////            predictUid[i] = i;
////        }
//
//        // 加载推理文件
//        Map<ClientInfo, BoostInferenceData> inferenceDataMap = new HashMap<>();
//
//        inferenceDataMap.put(clientInfos[0], new BoostInferenceData(DataLoad.loadTrainFromFile(baseDir + "inference1.csv")));
//        inferenceDataMap.put(clientInfos[1], new BoostInferenceData(DataLoad.loadTrainFromFile(baseDir + "inference2.csv")));
//        inferenceDataMap.put(clientInfos[2], new BoostInferenceData(DataLoad.loadTrainFromFile(baseDir + "inference3.csv")));
//
//
//
//        String[][] data1 = DataLoad.loadTrainFromFile(baseDir + "inference1.csv");
//        String[] predictUid = new String[data1.length - 1];
//        double[] label = new double[data1.length - 1];
//        for (int i = 1;i < data1.length;i++){
//            predictUid[i - 1] = (data1[i][0]);
//            label[i - 1] = Double.parseDouble(data1[i][data1[i].length - 1]);
//        }
//        long start = System.currentTimeMillis();
//
//        List<CommonRequest> requests = boost.initInference(Arrays.asList(clientInfos.clone()), predictUid,false);
//
//        double[] result = CommonRun.inference(boost, requests, modelMap, dataMap);
//        String[][] d = dataMap.get(clientInfos[0]);
//        double[] l = new double[data1.length - 1];
//
//        //double[] label = dataMap.get(clientInfos.get(0)).label;
//        double test_metric = Metric.calculateMetric(MetricType.RMSE, result, label);
//        System.out.println(String.format("test's rmse is " + test_metric));
//        double _test_mape = Metric.calculateMetric(MetricType.MAPE, result, label);
//        System.out.println(String.format("test's mape is " + _test_mape));
//        test_rmse.add(test_metric);
//        test_mape.add(_test_mape);
//        System.out.println("test's predict is " + Arrays.toString(result));
//        System.out.println("----------------full client end-------------------\n consumed time in seconds:");
//        System.out.println((System.currentTimeMillis() - start) / 1000.0);
//    }
//
//    public void reset(double[] DPparameter){
//        //parameter.setRandomized_response_probability(DPparameter[0]);
//        //parameter.setDifferential_privacy_parameter(DPparameter[1]);
//        boost = new FederatedGB(parameter);
//        modelMap.clear();
//        dataMap.clear();
//    }
//
//    public static void main(String[] args) {
//
//    }
//
//}
