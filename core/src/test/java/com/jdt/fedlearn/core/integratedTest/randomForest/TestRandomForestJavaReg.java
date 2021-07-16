package com.jdt.fedlearn.core.integratedTest.randomForest;

import com.jdt.fedlearn.core.dispatch.RandomForestJava;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.psi.MappingReport;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.core.type.EncryptionType;
import com.jdt.fedlearn.core.type.data.Tuple2;
import com.jdt.fedlearn.core.util.DataParseUtil;
import com.jdt.fedlearn.core.util.FileUtil;
import com.jdt.fedlearn.core.example.CommonRun;
import com.jdt.fedlearn.core.loader.randomForest.DataFrame;
import com.jdt.fedlearn.core.model.Model;
import com.jdt.fedlearn.core.model.RandomForestJavaModel;
import com.jdt.fedlearn.core.parameter.RandomForestParameter;
import com.jdt.fedlearn.core.type.MappingType;
import com.jdt.fedlearn.core.type.MetricType;

import java.io.IOException;
import java.util.*;

/**
 * Java版randomForest回归测试
 */
public class TestRandomForestJavaReg {
    //服务端维护
    private static String taskId = "182";
    private static final String token = taskId + "_RandomForest_Binary";
    private final static MetricType[] metrics = new MetricType[]{MetricType.RMSE, MetricType.MAPE, MetricType.MAAPE, MetricType.AUC, MetricType.ACC};

    // 参数
    private static final String loss = "Regression:MSE"; // 回归问题
    private static final RandomForestParameter parameter = new RandomForestParameter(
            5,
            10,
            200,
            1,
            .8,
            30,
            5,
            "Null",
            10,
            EncryptionType.IterativeAffine,
            metrics,
            loss,
            1024);
    private static final RandomForestJava algo = new RandomForestJava(parameter);
    private ClientInfo[] clientInfos;
    //此处为需要手动配置的四个选项，分别是数据文件夹目录，参与方个数，有label的参与方id，label的名字。
    private final String baseDir = "./src/test/resources/regressionA/";
    private static final int partnerSize = 3;
    private static final int labelIndex = 0;
    private static final String labelName = "y";

    //客户端维护
    private static Map<ClientInfo, DataFrame> dataMap = new HashMap<>();
    private static Map<ClientInfo, String[][]> rawDataMap = new HashMap<>();

    public void setUp() {
        //---------------------------下面不需要手动设置-------------------------------------
        List<String> categorical_features = new ArrayList<>(Arrays.asList(parameter.getCat_features().split(",")));
        this.clientInfos = new ClientInfo[partnerSize];
        for (int i = 0; i < partnerSize; i++) {
            this.clientInfos[i] = new ClientInfo("127.0.0.1", 8891 + i, "http", i);
            String fileName = "train" + i + ".csv";
            String[][] data = DataParseUtil.loadTrainFromFile(baseDir + fileName);
            rawDataMap.put(clientInfos[i], data);
            dataMap.put(clientInfos[i], new DataFrame(data, categorical_features));
        }
    }

    public void testTrainAndTest() throws IOException {
        System.out.println("Random forest test start:");
        ////---------------id match  and feature extract---------------------////
        Tuple2<MappingReport, String[]> mappingOutput = CommonRun.match(MappingType.VERTICAL_MD5, Arrays.asList(clientInfos.clone()), rawDataMap);
        MatchResult matchResult = new MatchResult(mappingOutput._1().getSize());
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
            modelMap.put(client, new RandomForestJavaModel());
        }
        Map<String, Object> other = new HashMap<>();
        other.put("splitRatio", 0.7);
        // initial and train
        List<CommonRequest> initRequests = algo.initControl(Arrays.asList(clientInfos.clone()), matchResult, featuresMap, other);
        CommonRun.train(algo, initRequests, modelMap, rawDataMap, mappingOutput._2());

        //model save
        for (Map.Entry<ClientInfo, Model> x : modelMap.entrySet()) {
            RandomForestJavaModel boostModel = (RandomForestJavaModel) x.getValue();
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
            RandomForestJavaModel tmp = new RandomForestJavaModel();
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
        String[] predictUid = new String[]{"291B", "292A"};
        List<CommonRequest> requests = algo.initInference(Arrays.asList(clientInfos.clone()), predictUid);
        double[][] result = CommonRun.inference(algo, requests, modelMap, inferenceDataMap).getPredicts();
        System.out.println("inference result is " + Arrays.deepToString(result));
    }

    public static void main(String[] args) throws IOException {
        TestRandomForestJavaReg testRandomForest = new TestRandomForestJavaReg();
        testRandomForest.setUp();
        testRandomForest.testTrainAndTest();
        testRandomForest.testInference();
    }
}
