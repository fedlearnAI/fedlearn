
package com.jdt.fedlearn.core.integratedTest.horizontalFL;

import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.core.example.CommonRun;
import com.jdt.fedlearn.core.model.HorizontalFedAvgModel;
import com.jdt.fedlearn.core.parameter.HorizontalFedAvgPara;
import com.jdt.fedlearn.core.dispatch.HorizontalFedAvg;

import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.common.entity.core.feature.Features;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.core.type.MappingType;
import com.jdt.fedlearn.core.type.data.Tuple2;
import com.jdt.fedlearn.core.util.DataParseUtil;

import com.jdt.fedlearn.core.loader.randomForest.RFInferenceData;
import com.jdt.fedlearn.core.entity.common.CommonResponse;

import com.jdt.fedlearn.core.model.Model;
import com.jdt.fedlearn.core.type.MetricType;

import java.util.*;

/**
 * 水平联邦学习框架单机测试：
 *      测试方法：
 *          测试流程：
 *              1. 在本机的8891，8892，8893端口启动GRPC服务（此处可以分成3个terminal启动或者使用screen/tmux）：
 *                  方法1：手动启动GRPC服务，在本机上运行
 *                      python3 server.py -P 8891
 *                      python3 server.py -P 8892
 *                      python3 server.py -P 8893
 *
 *                  注意：如果 8891，8892或8893端口被占用，可以更改端口，但需要修改相应的 setUp 函数中的 client1 - client3 中绑定的端口，
 *                       以及 "modelMap.put(client1, new HorizontalFedAvgModel("127.0.0.1:8891"));" 这行代码和下面的2行代码中初始化Model的地址。
 *              2. 运行 testTrainRegressor 测试，查看是否运行通过
 *              3. 运行 testTrainClassfier 测试，查看是否运行通过
 *              4. 运行 testInference 测试，查看是否运行通过
 * Note: 暂时实现训练部分。水平联邦的推理是纯本地的，目前还不清楚具体场景是否联邦环境中调用的需求，所以完整的推理模块暂时未实现。
 **/
public class TestHorizontalZoo {
    private List<ClientInfo> clientInfos;
    //private static MetricType[] metrics = new MetricType[]{MetricType.RMSE, MetricType.MAPE, MetricType.MAAPE};
    private static MetricType[] metrics = new MetricType[]{MetricType.RMSE};

    private  HorizontalFedAvgPara hflParameter;
    private String modelName;
    private HorizontalFedAvg master;

    //客户端维护
    private static Map<ClientInfo, Model> modelMap = new HashMap<>(); //每个客户端维护自己的，所以此处有三份

    //private HorizontalTrainData[] dataArray = new HorizontalTrainData[3];
    private static final Map<ClientInfo, String[][]> rawDataMap = new HashMap<>();
    //private static Map<ClientInfo, DataFrame> validDataMap = new HashMap<>(); //validateDataset
    //private String[] dataArray = new String[3];
    //private Features[] features = new Features[3];

    //数据加载,客户端维护
    private String labelName;
    private String baseDir;
    private String[] fileNameList;
    private static ClientInfo client1 = new ClientInfo("127.0.0.1", 81, "http");
    private static ClientInfo client2 = new ClientInfo("127.0.0.1", 82, "http");
    private static ClientInfo client3 = new ClientInfo("127.0.0.1", 83, "http");
    //界面传给服务端
    private Map<ClientInfo, Features> featuresMap = new HashMap<>();

    public void init() {
        this.clientInfos = Arrays.asList(client1, client2, client3);

        //ArrayList<String> categorical_features = new ArrayList<>(Arrays.asList(hflParameter.getCatFeatures()));
        //ArrayList<String> categorical_features = new ArrayList<>(Arrays.asList(hflParameter.getCat_features()));
        this.modelMap.put(client1, new HorizontalFedAvgModel("127.0.0.1:8891"));
        this.modelMap.put(client2, new HorizontalFedAvgModel("127.0.0.1:8892"));
        this.modelMap.put(client3, new HorizontalFedAvgModel("127.0.0.1:8893"));
    }

    public void testTrainClassfier() {
        baseDir = "./src/test/resources/HFL/mnist/";
        fileNameList = new String[]{"mnistTrain283.csv", "mnistTrain787.csv", "mnistTrain1078.csv"};
        //String[] predictFileNameList = new String[]{"mnistTest.csv", "mnistTest.csv", "mnistTest.csv"};
        labelName = "y";
        modelName = "SGDClassifier";

        loadData();
        testTrain(MetricType.AUC, "Classification:Cross entropy", 10);
    }

    public void testTrainRegressor() {
        //1:14554
        //2:3063
        //3:324
        baseDir = "./src/test/resources/HFL/hori/";
        fileNameList = new String[]{"train0.csv", "train1.csv", "train2.csv"};
        labelName = "y";

        modelName = "SGDRegressor";
        loadData();
        testTrain(MetricType.RMSE, "Regression:MSE", 10);
    }

    private void loadData() {
        //三份数据
        String trainFile1 = baseDir + fileNameList[0];
        String trainFile2 = baseDir + fileNameList[1];
        String trainFile3 = baseDir + fileNameList[2];

        String[][] data1 = DataParseUtil.loadTrainFromFile(trainFile1, labelName);
        String[][] data2 = DataParseUtil.loadTrainFromFile(trainFile2, labelName);
        String[][] data3 = DataParseUtil.loadTrainFromFile(trainFile3, labelName);
        Features features1 = DataParseUtil.fetchFeatureFromData(data1,labelName);
        Features features2 = DataParseUtil.fetchFeatureFromData(data2,labelName);
        Features features3 = DataParseUtil.fetchFeatureFromData(data3,labelName);

        rawDataMap.put(client1, data1);
        rawDataMap.put(client2, data2);
        rawDataMap.put(client3, data3);

        featuresMap.put(client1, features1);
        featuresMap.put(client2, features2);
        featuresMap.put(client3, features3);
        //System.out.println("----------------loadData-------------------\n ");
    }

    private void testTrain(MetricType metricTp, String lossName, int numRound) {
        System.out.println("----------------"+modelName+" train start-------------------");
        /* initialization */
        hflParameter = new HorizontalFedAvgPara(
                numRound, 3, 1, 10, 2,
                new MetricType[]{metricTp}, lossName);
        master = new HorizontalFedAvg(hflParameter, modelName);

        Tuple2<MatchResult, String[]> mappingOutput = CommonRun.match(MappingType.EMPTY, clientInfos, rawDataMap);
        // TODO check是否可以是modelName作为taskId
        MatchResult matchResult = mappingOutput._1();
        List<CommonRequest> initRequests = master.initControl(clientInfos, matchResult, featuresMap, null);
        CommonRun.train(master, initRequests, modelMap,  rawDataMap, mappingOutput._2());

        //TODO 除了训练结果以外的参数清除
        System.out.println("----------------"+modelName+" train end-------------------\n\n\n\n");
    }

    public void testInference() {
        modelName = "SGDClassifier";
        System.out.println("----------------"+modelName+" predict start-------------------");
        // initialization
        hflParameter = new HorizontalFedAvgPara();
        master = new HorizontalFedAvg(hflParameter, modelName);

        // 加载推理文件
        baseDir = "./src/test/resources/HFL/mnist/";
        Map<ClientInfo, RFInferenceData> inferenceDataMap = new HashMap<>();
        inferenceDataMap.put(clientInfos.get(0), new RFInferenceData(DataParseUtil.loadTrainFromFile(baseDir + "mnistTest.csv")));
        inferenceDataMap.put(clientInfos.get(1), new RFInferenceData(DataParseUtil.loadTrainFromFile(baseDir + "mnistTest.csv")));
        inferenceDataMap.put(clientInfos.get(2), new RFInferenceData(DataParseUtil.loadTrainFromFile(baseDir + "mnistTest.csv")));

        //根据需要预测的id，和任务id，生成预测请求
        String[] predictUid = new String[]{"12467", "54044", "13460", "42983", "47471", "31524"};
        //String[] predictUid = new String[]{};
        long start = System.currentTimeMillis();

        List<CommonRequest> requests = master.initInference(clientInfos, predictUid,new HashMap<>()); //initial request
        List<CommonResponse> responses = new ArrayList<>();
        while (master.isInferenceContinue()) {
            responses = new ArrayList<>();
            for (CommonRequest request : requests) {
                ClientInfo client = request.getClient();
                /////////////////////below is client code
                RFInferenceData inferenceData = inferenceDataMap.get(client);
                HorizontalFedAvgModel model = (HorizontalFedAvgModel)modelMap.get(request.getClient());
                //model.setModelName(modelName);//多机环境不需要
                Message resData = model.inference(request.getPhase(), request.getBody(), inferenceData);
                ///////////////////////
                CommonResponse response = new CommonResponse(request.getClient(), resData);
                responses.add(response);
            }
            //根据responses构造 request，并重置responses
            requests = master.inferenceControl(responses);
        }
        /*double[] result = master.postInferenceControl(responses);
        System.out.println("inference result is " + Arrays.toString(result));*/
        System.out.println("----------------"+modelName+" predict end-------------------\n\n\n\n");
    }

    public static void main(String[] args) {
        TestHorizontalZoo horizontalZoo = new TestHorizontalZoo();
        horizontalZoo.init();
        horizontalZoo.testTrainClassfier();
        horizontalZoo.testInference();
    }

}
