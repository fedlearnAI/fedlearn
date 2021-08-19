package com.jdt.fedlearn.core.integratedTest.verticalFDNN;

import com.jdt.fedlearn.core.dispatch.VerticalFDNN;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.example.CommonRun;
import com.jdt.fedlearn.core.loader.randomForest.RFTrainData;
import com.jdt.fedlearn.core.model.Model;
import com.jdt.fedlearn.core.model.VerticalFDNNModel;
import com.jdt.fedlearn.core.parameter.VerticalFDNNParameter;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.core.type.MappingType;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.data.Tuple2;
import com.jdt.fedlearn.core.util.DataParseUtil;
import com.jdt.fedlearn.core.util.FileUtil;

import java.io.IOException;
import java.util.*;

public class TestVerticalFDNN {
    //服务端维护
    private final String taskId = "181";
    private final long unixTime = System.currentTimeMillis() / 1000L;
    private final String modelToken = taskId + "_" + unixTime + "_VerticalFDNN_Binary";

    private final static MetricType[] metrics = new MetricType[]{MetricType.RMSE, MetricType.MAPE, MetricType.MAAPE, MetricType.AUC, MetricType.ACC};
    // 参数
    private static final VerticalFDNNParameter parameter = new VerticalFDNNParameter(
            32,
            5
    );
    private static final VerticalFDNN algo = new VerticalFDNN(parameter);
    private ClientInfo[] clientInfos;
    //此处为需要手动配置的四个选项，分别是数据文件夹目录，参与方个数，有label的参与方id，label的名字。
    private final String baseDir = "./src/test/resources/classificationA/";
    private static final int partnerSize = 2;
    private static final int labelIndex = 0;
    private static final String labelName = "Outcome";

    //客户端维护
    private static Map<ClientInfo, RFTrainData> dataMap = new HashMap<>();
    private static Map<ClientInfo, String[][]> rawDataMap = new HashMap<>();

    public void setUp() {
        //---------------------------下面不需要手动设置-------------------------------------
        this.clientInfos = new ClientInfo[partnerSize];
        for (int i = 0; i < partnerSize; i++) {
            this.clientInfos[i] = new ClientInfo("127.0.0.1", 8891 + i, "http", "", String.valueOf(i));
            String fileName = "train" + i + ".csv";
            String[][] data = DataParseUtil.loadTrainFromFile(baseDir + fileName);
            rawDataMap.put(clientInfos[i], data);
            dataMap.put(clientInfos[i], new RFTrainData(data));
        }
    }

    public void testTrainAndTest() throws IOException {
        System.out.println("FDNN test start:");
        ////---------------id match  and feature extract---------------------////
        Tuple2<MatchResult, String[]> mappingOutput = CommonRun.match(MappingType.MD5, Arrays.asList(clientInfos.clone()), rawDataMap);
        String[] commonIds = mappingOutput._2();

        MatchResult matchResult = mappingOutput._1();
        Map<ClientInfo, Features> featuresMap = new HashMap<>();
        for (Map.Entry<ClientInfo, String[][]> entry : rawDataMap.entrySet()) {
            Features features1 = DataParseUtil.fetchFeatureFromData(entry.getValue());
            featuresMap.put(entry.getKey(), features1);
        }
        //设置哪方那个特征是label
        featuresMap.get(clientInfos[labelIndex]).setLabel(labelName);
        ////---------------id match and feature extract  end---------------------////

        //model create
        Map<ClientInfo, Model> modelMap = new HashMap<>(); //每个客户端维护自己的，所以此处有n份
        for (ClientInfo client : clientInfos) {
            modelMap.put(client, new VerticalFDNNModel());
        }

        // initial and train
        List<CommonRequest> initRequests = algo.initControl(Arrays.asList(clientInfos.clone()), matchResult, featuresMap, new HashMap<>());
        CommonRun.train(algo, initRequests, modelMap, rawDataMap, commonIds);

        //model save
        for (Map.Entry<ClientInfo, Model> x : modelMap.entrySet()) {
            VerticalFDNNModel verticalFDNNModel = (VerticalFDNNModel) x.getValue();
            String key = x.getKey().getPort() + "";
            String content = verticalFDNNModel.serialize();
            FileUtil.saveModel(content, "./" + modelToken + "_" + key + ".model");
        }
    }

    public void testInference() throws IOException {
        // load model
        Map<ClientInfo, Model> modelMap = new HashMap<>();
        for (ClientInfo clientInfo : clientInfos) {
            String path = "./" + modelToken + "_" + clientInfo.getPort() + ".model";
            String content = FileUtil.loadModel(path);
            VerticalFDNNModel tmp = new VerticalFDNNModel();
            tmp.deserialize(content);
            modelMap.put(clientInfo, tmp);
        }

        //推理文件加载
        //TODO 推理单元测试逻辑重构，
        Map<ClientInfo, String[][]> inferenceDataMap = new HashMap<>();
        inferenceDataMap.put(clientInfos[0], (DataParseUtil.loadTrainFromFile(baseDir + "inference0.csv")));
        inferenceDataMap.put(clientInfos[1], (DataParseUtil.loadTrainFromFile(baseDir + "inference1.csv")));

//        根据需要预测的id，和任务id，生成预测请求
//        String[] predictUid = new String[]{"591B", "592B", "593A", "594B"};
//        String[] predictUid = new String[]{"2891", "16484"};
        String[] predictUid = new String[]{"591B", "592B", "593A", "594B"};
        List<CommonRequest> requests = algo.initInference(Arrays.asList(clientInfos.clone()), predictUid,new HashMap<>());
        double[][] result = CommonRun.inference(algo, requests, modelMap, inferenceDataMap).getPredicts();
        System.out.println("inference result is " + Arrays.toString(result));
    }

    public static void main(String[] args) throws IOException {
        TestVerticalFDNN testVerticalFDNN = new TestVerticalFDNN();
        testVerticalFDNN.setUp();
        testVerticalFDNN.testTrainAndTest();
        testVerticalFDNN.testInference();
    }
}
