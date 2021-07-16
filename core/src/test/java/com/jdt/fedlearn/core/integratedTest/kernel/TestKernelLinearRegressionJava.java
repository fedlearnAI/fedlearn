package com.jdt.fedlearn.core.integratedTest.kernel;

import com.jdt.fedlearn.core.dispatch.KernelLinearRegressionJava;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.core.entity.common.CommonResponse;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.example.CommonRun;
import com.jdt.fedlearn.core.psi.MappingReport;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.core.type.NormalizationType;
import com.jdt.fedlearn.core.type.data.Tuple2;
import com.jdt.fedlearn.core.util.DataParseUtil;
import com.jdt.fedlearn.core.util.FileUtil;
import com.jdt.fedlearn.core.model.KernelLinearRegressionJavaModel;
import com.jdt.fedlearn.core.model.Model;
import com.jdt.fedlearn.core.parameter.KernelLinearRegressionParameter;
import com.jdt.fedlearn.core.type.MappingType;
import com.jdt.fedlearn.core.type.MetricType;

import java.io.IOException;
import java.util.*;

/**
 * kernelLinearRegression单机测试：
 * 测试方法：
 * 测试流程：
 * 1. 在本机的8891，8892，8893端口启动GRPC服务（此处可以分成3个terminal启动或者使用screen/tmux）：
 * 方法1：手动启动GRPC服务，在本机上运行
 * python3 server.py -P 8891
 * python3 server.py -P 8892
 * python3 server.py -P 8893
 * 方法2：使用docker镜像启动GRPC服务：
 * TBD
 * 注意：如果端口被占用，可以更改端口，但需要修改相应的 client1 - client3 中绑定的端口，
 * 2. 运行main 测试，查看是否运行通过
 **/
public class TestKernelLinearRegressionJava {
    private static final String taskId = "63";
    private static final String modelToken = taskId + "_KernelLinearRegression";
    private List<ClientInfo> clientInfos;
    private static final MetricType[] metrics = new MetricType[]{MetricType.TRAINLOSS, MetricType.RMSE, MetricType.AUC, MetricType.ACC};
    //    private static final MetricType[] metrics = new MetricType[]{MetricType.MACC,MetricType.MERROR,MetricType.MAUC};
//    private static final KernelLinearRegressionParameter parameter = new KernelLinearRegressionParameter(3, 100, 400, 3, 0.005, 200, metrics, "STANDARD");
    private static final KernelLinearRegressionParameter parameter = new KernelLinearRegressionParameter(3, 100, 400, 4, 0.005, 200, metrics, NormalizationType.STANDARD, 1, 0, 2);
    private static final KernelLinearRegressionJava algo = new KernelLinearRegressionJava(parameter);

    //客户端维护
    private static Map<ClientInfo, String[][]> rawDataMap = new HashMap<>();
    private static Map<ClientInfo, Model> modelMap = new HashMap<>();
    private static Map<ClientInfo, String[][]> inferenceDataMap = new HashMap<>();
    //界面传给服务端
    private Map<ClientInfo, Features> featuresMap = new HashMap<>();
    private String[] csvFileTrain = new String[]{"train0.csv", "train1.csv", "train2.csv"};
    private String[] csvFileInference = new String[]{"inference0.csv", "inference1.csv", "inference2.csv"};
    //    private String[] csvFileInference = new String[]{"train00.csv", "train1.csv", "train2.csv"};
    private final String labelTag = "y";
    //    private final String baseDir = "/home/fan/Data/bank_market/onlylabel/";
//    private final String baseDir = "/home/fan/Data/bank_market/";
    private final String baseDir = "./src/test/resources/regressionA/";
//    private final String baseDir = "./src/test/resources/multiClassificationB/";
//    String baseDir = "./src/test/resources/multiClassificationA/";
//    String baseDir = "./src/test/resources/bank/";
//    String baseDir = "/home/fan/Data/digits/";
//    private final String labelTag = "label";


    public void setUp() {
        ClientInfo client1 = new ClientInfo("127.0.0.1", 8891, "http", 1234);
        ClientInfo client2 = new ClientInfo("127.0.0.1", 8892, "http", 1235);
        ClientInfo client3 = new ClientInfo("127.0.0.1", 8893, "http", 1236);
        this.clientInfos = Arrays.asList(client1, client2, client3);

        if (rawDataMap.size() == 0) {
            String[][] data1 = DataParseUtil.loadTrainFromFile(baseDir + csvFileTrain[0]);
            String[][] data2 = DataParseUtil.loadTrainFromFile(baseDir + csvFileTrain[1]);
            String[][] data3 = DataParseUtil.loadTrainFromFile(baseDir + csvFileTrain[2]);
            rawDataMap.put(client1, data1);
            rawDataMap.put(client2, data2);
            rawDataMap.put(client3, data3);

            Features features1 = DataParseUtil.fetchFeatureFromData(data1, labelTag);
            Features features2 = DataParseUtil.fetchFeatureFromData(data2, null);
            Features features3 = DataParseUtil.fetchFeatureFromData(data3, null);
            featuresMap.put(client1, features1);
            featuresMap.put(client2, features2);
            featuresMap.put(client3, features3);
        }

        if (modelMap.size() == 0) {
            modelMap.put(client1, new KernelLinearRegressionJavaModel());
            modelMap.put(client2, new KernelLinearRegressionJavaModel());
            modelMap.put(client3, new KernelLinearRegressionJavaModel());
        }


        for (int i = 0; i < clientInfos.size(); i++) {
            String[][] datai = (DataParseUtil.loadTrainFromFile(baseDir + csvFileInference[i]));
            inferenceDataMap.put(clientInfos.get(i), datai);
        }
    }

    public void testTrain() throws IOException {
        //id and feature prepare
        Tuple2<MappingReport, String[]> mappingOutput = CommonRun.match(MappingType.VERTICAL_MD5, clientInfos, rawDataMap);
        MatchResult matchResult = new MatchResult(mappingOutput._1().getSize());

        System.out.println("-id match end");
        //initial and train
        Map<String, Object> other = new HashMap<>();
        other.put("splitRatio", 0.7);
        List<CommonRequest> initRequests = algo.initControl(clientInfos, matchResult, featuresMap, other);
        CommonRun.train(algo, initRequests, modelMap, rawDataMap, mappingOutput._2());

        //model save
        for (Map.Entry<ClientInfo, Model> x : modelMap.entrySet()) {
            KernelLinearRegressionJavaModel model = (KernelLinearRegressionJavaModel) x.getValue();
            String key = x.getKey().getPort() + "";
            String content = model.serialize();
            FileUtil.saveModel(content, "./" + modelToken + "_" + key + ".model");
        }
    }

    public void testInference() throws IOException {
//        String[] predictUid = new String[]{"291B", "292A","3pa", "293B", "294C"};
        String[] predictUidAll = DataParseUtil.loadInferenceUidList(baseDir + "inference0.csv");
//        String[] predictUidAll = DataParseUtil.loadInferenceUidList(baseDir + "train00.csv");
        String[] predictUid = Arrays.copyOfRange(predictUidAll, 0, 100);
//        String[] predictUid = new String[]{"okU", "1mX", "3PA", "4oX"};
        //model load
        //Map<ClientInfo, KernelLinearRegressionModel> modelMap =
        String[] port = {"127.0.0.1:8891", "127.0.0.1:8892", "127.0.0.1:8893"};
        int porti = 0;
        for (ClientInfo clientInfo : clientInfos) {
            String path = "./" + modelToken + "_" + clientInfo.getPort() + ".model";
            String content = FileUtil.loadModel(path);
            KernelLinearRegressionJavaModel tmp = new KernelLinearRegressionJavaModel();
            tmp.deserialize(content);
            modelMap.put(clientInfo, tmp);
            porti += 1;
        }

        //1. 控制端发送模型参数给主被动端（）
        //2. 控制端接收计算被动端计算结果叠加，
        //3. 输出结果，退出
        //1. 主被动端做样本变换
        //2. 主被动端计算内积
        int p = -1;
        List<CommonRequest> requests = algo.initInference(clientInfos, predictUid);
        List<CommonResponse> responses = new ArrayList<>();
        double[][] sum_u = CommonRun.inference(algo, requests, modelMap, inferenceDataMap).getPredicts();
        System.out.println("=============" + Arrays.deepToString(sum_u));
        double[] pred = new double[]{88.11186981201172, 92.04011535644531, Double.NaN, 86.20804595947266, 91.74789428710938};
        double[] pred50 = new double[]{89.79427433013916, 91.53018486499786, Double.NaN, 87.5040882229805, 91.51673656702042};
    }


    public static void main(String[] args) throws IOException {
        TestKernelLinearRegressionJava testKernelLinearRegression = new TestKernelLinearRegressionJava();
        testKernelLinearRegression.setUp();
        testKernelLinearRegression.testTrain();
        testKernelLinearRegression.testInference();
    }
}
