package com.jdt.fedlearn.core.integratedTest.randomForest;

import com.jdt.fedlearn.core.dispatch.RandomForest;
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
import com.jdt.fedlearn.core.metrics.Metric;
import com.jdt.fedlearn.core.model.Model;
import com.jdt.fedlearn.core.model.RandomForestModel;
import com.jdt.fedlearn.core.parameter.RandomForestParameter;
import com.jdt.fedlearn.core.type.MappingType;
import com.jdt.fedlearn.core.type.MetricType;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * 随机森林单机测试：
 * 测试方法：
 * 测试流程：
 * 1. 在本机的8891，8892，8893端口启动GRPC服务（此处可以分成3个terminal启动或者使用screen/tmux）：
 * 方法1：手动启动GRPC服务，在本机上运行
 * python3 server.py -P 8891
 * python3 server.py -P 8892
 * python3 server.py -P 8893
 * 方法2：使用docker镜像启动GRPC服务：
 * TBD
 * 注意：如果 端口被占用，可以更改端口，但需要修改相应的 setUp 函数中的 client1 - client3 中绑定的端口，
 * 2. 运行 main 测试，查看是否运行通过
 **/
public class TestRandomForest {
    private static final Logger logger = Logger.getLogger(TestRandomForest.class.getName());

    //服务端维护
    private static final String taskId = "18";
    private static final String token = taskId + "_RandomForestBinary";
    private static MetricType[] metrics = new MetricType[]{MetricType.RMSE, MetricType.MAPE, MetricType.MAAPE, MetricType.AUC, MetricType.ACC};

    // 参数
    private static final String loss = "Regression:MSE"; // 回归问题
    private static final RandomForestParameter parameter = new RandomForestParameter(
            2,
            5,
            1000,
            25,
            1,
            30,
            30,
            "Null",
            10,
            EncryptionType.Paillier,
            metrics,
            loss);
    private static final RandomForest algo = new RandomForest(parameter);
    private ClientInfo[] clientInfos;
    //此处为需要手动配置的四个选项，分别是数据文件夹目录，参与方个数，有label的参与方id，label的名字。
    private final String baseDir = "./src/test/resources/regressionA/";
    private static final int partnerSize = 3;
    private static final int labelIndex = 0;
    private static final String labelName = "y";

    //客户端维护
    private static Map<ClientInfo, DataFrame> dataMap = new HashMap<>();
    private static Map<ClientInfo, String[][]> rawDataMap = new HashMap<>();

    public TestRandomForest() throws IOException {
    }

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
            modelMap.put(client, new RandomForestModel(client.getIp() + ":" + client.getPort()));
        }

        // initial and train
        List<CommonRequest> initRequests = algo.initControl(Arrays.asList(clientInfos.clone()), matchResult, featuresMap, new HashMap<>());
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
        String[] predictUid = new String[]{"291B", "292A"};
        List<CommonRequest> requests = algo.initInference(Arrays.asList(clientInfos.clone()), predictUid);
        double[][] result = CommonRun.inference(algo, requests, modelMap, inferenceDataMap).getPredicts();
        System.out.println("inference result is " + Arrays.toString(result));
    }

    public void testAUC() {
        double[] label = new double[]{0, 0, 0, 0, 0, 1};
        double[] pred = new double[]{0.1, 0, 0, 0, 0, 0};
        double auc = Metric.auc(pred, label);
        logger.info("AUC = " + auc);
    }

    public static void main(String[] args) throws IOException {
        TestRandomForest testRandomForest = new TestRandomForest();
        testRandomForest.setUp();
        testRandomForest.testTrainAndTest();
        testRandomForest.testInference();
    }
}
