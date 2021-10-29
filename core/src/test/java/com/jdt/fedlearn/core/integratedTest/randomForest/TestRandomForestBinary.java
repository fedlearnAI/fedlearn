package com.jdt.fedlearn.core.integratedTest.randomForest;

import com.jdt.fedlearn.core.dispatch.RandomForest;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.core.type.EncryptionType;
import com.jdt.fedlearn.core.type.data.Tuple2;
import com.jdt.fedlearn.core.util.DataParseUtil;
import com.jdt.fedlearn.core.util.FileUtil;
import com.jdt.fedlearn.core.example.CommonRun;
import com.jdt.fedlearn.core.loader.randomForest.RFTrainData;
import com.jdt.fedlearn.core.model.Model;
import com.jdt.fedlearn.core.model.RandomForestModel;
import com.jdt.fedlearn.core.parameter.RandomForestParameter;
import com.jdt.fedlearn.core.type.MappingType;
import com.jdt.fedlearn.core.type.MetricType;

import java.io.IOException;
import java.util.*;

/**
 * Java版randomforest二分类测试
 */
public class TestRandomForestBinary {
    //服务端维护
    private static String taskId = "182";
    private static String token = taskId + "_RandomForest_Binary";

    private final static MetricType[] metrics = new MetricType[]{MetricType.AUC, MetricType.ACC};

    // 参数
    private static final String loss = "Regression:MSE"; // 回归问题
    private static final RandomForestParameter parameter = new RandomForestParameter(
            5,
            5,
            300,
            50,
            0.8,
            30,
            30,
            "Null",
            10,
            EncryptionType.IterativeAffine,
            metrics,
            loss,
            666);
    private static final RandomForest algo = new RandomForest(parameter);
    private ClientInfo[] clientInfos;
    //此处为需要手动配置的四个选项，分别是数据文件夹目录，参与方个数，有label的参与方id，label的名字。
    private final String baseDir = "./src/test/resources/classificationA/";
    private static final int partnerSize = 3;
    private static final int labelIndex = 0;
    private static final String labelName = "Outcome";

    //客户端维护
    private static Map<ClientInfo, RFTrainData> dataMap = new HashMap<>();
    private static Map<ClientInfo, String[][]> rawDataMap = new HashMap<>();

    public void setUp() {
        //---------------------------下面不需要手动设置-------------------------------------
        List<String> categorical_features = new ArrayList<>(Arrays.asList(parameter.getCat_features().split(",")));
        this.clientInfos = new ClientInfo[partnerSize];
        for (int i = 0; i < partnerSize; i++) {
            this.clientInfos[i] = new ClientInfo("127.0.0.1", 8891 + i, "HTTP", "", String.valueOf(i));
            String fileName = "train" + i + ".csv";
            String[][] data = DataParseUtil.loadTrainFromFile(baseDir + fileName);
            rawDataMap.put(clientInfos[i], data);
            dataMap.put(clientInfos[i], new RFTrainData(data, categorical_features));
        }
    }

    public void testTrainAndTest() throws IOException {
        System.out.println("Random forest test start:");
        ////---------------id match  and feature extract---------------------////
        Tuple2<MatchResult, String[]> mappingOutput = CommonRun.match(MappingType.MD5, Arrays.asList(clientInfos.clone()), rawDataMap);
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
            modelMap.put(client, new RandomForestModel());
        }
        Map<String, Object> other = new HashMap<>();
        other.put("splitRatio", 1.0);
        // initial and train
        List<CommonRequest> initRequests = algo.initControl(Arrays.asList(clientInfos.clone()), matchResult, featuresMap, other);
        CommonRun.train(algo, initRequests, modelMap, rawDataMap, mappingOutput._2());

        //model save
        for (Map.Entry<ClientInfo, Model> x : modelMap.entrySet()) {
            RandomForestModel boostModel = (RandomForestModel) x.getValue();
            String key = x.getKey().getPort() + "";
            String content = boostModel.serialize();
            FileUtil.saveModel(content, "./" + token + "_" + key + ".model");
        }
    }

    public void testInference() throws IOException {
        // load model
        Map<ClientInfo, Model> modelMap = new HashMap<>();
        for (ClientInfo clientInfo : clientInfos) {
            String path = "./" + token + "_" + clientInfo.getPort() + ".model";
            String content = FileUtil.loadModel(path);
            RandomForestModel tmp = new RandomForestModel();
            tmp.deserialize(content);
            modelMap.put(clientInfo, tmp);
        }

        //推理文件加载
        //TODO 推理单元测试逻辑重构，
        Map<ClientInfo, String[][]> inferenceDataMap = new HashMap<>();
        inferenceDataMap.put(clientInfos[0], (DataParseUtil.loadTrainFromFile(baseDir + "inference0.csv")));
        inferenceDataMap.put(clientInfos[1], (DataParseUtil.loadTrainFromFile(baseDir + "inference1.csv")));
        inferenceDataMap.put(clientInfos[2], (DataParseUtil.loadTrainFromFile(baseDir + "inference2.csv")));

//        根据需要预测的id，和任务id，生成预测请求
//        String[] predictUid = new String[]{"591B", "592B", "593A", "594B"};
//        String[] predictUid = new String[]{"2891", "16484"};
        String[] predictUid = new String[]{"591B", "592B", "593A", "594B", "595C",
                "596B",
                "597C",
                "598A",
                "599B"};
        List<CommonRequest> requests = algo.initInference(Arrays.asList(clientInfos.clone()), predictUid, new HashMap<>());
        double[][] result = CommonRun.inference(algo, requests, modelMap, inferenceDataMap).getPredicts();
        System.out.println("inference result is " + Arrays.deepToString(result));
    }

    public static void main(String[] args) throws IOException {
        TestRandomForestBinary testRandomForest = new TestRandomForestBinary();
        testRandomForest.setUp();
        testRandomForest.testTrainAndTest();
        testRandomForest.testInference();
    }
}
