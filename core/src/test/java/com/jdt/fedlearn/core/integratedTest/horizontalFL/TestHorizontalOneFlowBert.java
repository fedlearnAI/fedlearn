package com.jdt.fedlearn.core.integratedTest.horizontalFL;

import com.jdt.fedlearn.core.example.CommonRun;
import com.jdt.fedlearn.core.loader.horizontalZoo.HorizontalDataFrame;
import com.jdt.fedlearn.core.model.HorizontalFedAvgModel;
import com.jdt.fedlearn.core.parameter.HorizontalFedAvgPara;
import com.jdt.fedlearn.core.dispatch.HorizontalFedAvg;

import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.common.entity.core.feature.Features;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.core.util.DataParseUtil;

import com.jdt.fedlearn.core.model.Model;
import com.jdt.fedlearn.core.type.MetricType;

import java.util.*;

/**
 * 水平联邦学习框架单机测试：
 *      测试方法：
 *          测试流程：
 *              1. 在本机的8891，8892 端口启动GRPC服务（此处可以分成3个terminal启动或者使用screen/tmux）：
 *                  方法1：手动启动GRPC服务，在本机上运行
 *                      CUDA_VISIBLE_DEVICES=0 python3 server.py -P 8891
 *                      CUDA_VISIBLE_DEVICES=1 python3 server.py -P 8892
 * 
 *
 *                  注意：如果 8891，8892或8893端口被占用，可以更改端口，但需要修改相应的 setUp 函数中的 client1 - client3 中绑定的端口，
 *                       以及 "modelMap.put(client1, new HorizontalFedAvgModel("127.0.0.1:8891"));" 这行代码和下面的2行代码中初始化Model的地址。
 *              2. 运行 estHorizontalOneFlowBert  测试，查看是否运行通过
 *             
 * Note: 暂时实现训练部分。水平联邦的推理是纯本地的，目前还不清楚具体场景是否联邦环境中调用的需求，所以完整的推理模块暂时未实现。
 **/

public class TestHorizontalOneFlowBert {
    //服务端维护
    private List<ClientInfo> clientInfos;
    //private static MetricType[] metrics = new MetricType[]{MetricType.RMSE, MetricType.MAPE, MetricType.MAAPE};
    private static MetricType[] metrics = new MetricType[]{MetricType.RMSE};

    private  HorizontalFedAvgPara hflParameter;
    private String modelName;
    private HorizontalFedAvg master;

    //客户端维护
    private static Map<ClientInfo, Model> modelMap = new HashMap<>(); //每个客户端维护自己的，所以此处有三份

    //private HorizontalTrainData[] dataArray = new HorizontalTrainData[3];
    private static Map<ClientInfo, HorizontalDataFrame> trainDataMap = new HashMap<>(); //training data
    private static final Map<ClientInfo, String[][]> rawDataMap = new HashMap<>();
    //private static Map<ClientInfo, DataFrame> validDataMap = new HashMap<>(); //validateDataset

    //数据加载,客户端维护
    private String labelName;
    private String baseDir;
    private String[] fileNameList;
    private static ClientInfo client1 = new ClientInfo("127.0.0.1", 81, "http");
    private static ClientInfo client2 = new ClientInfo("127.0.0.1", 82, "http");
    //界面传给服务端
    private Map<ClientInfo, Features> featuresMap = new HashMap<>();

    public void setUp() {
        this.clientInfos = Arrays.asList(client1, client2);
        this.modelMap.put(client1, new HorizontalFedAvgModel("127.0.0.1:8891"));
        this.modelMap.put(client2, new HorizontalFedAvgModel("127.0.0.1:8892"));
        
    }

    public void testTrainClassifier() {
        baseDir = "./src/test/resources/HFL/mnist/";
        fileNameList = new String[]{"mnistTrain283.csv", "mnistTrain787.csv", "mnistTrain1078.csv"};
        //String[] predictFileNameList = new String[]{"mnistTest.csv", "mnistTest.csv", "mnistTest.csv"};
        labelName = "y";
        modelName = "OneFlowBertClassifier";

        loadData();
        testTrain(MetricType.ACC, "Classification:Cross entropy", 5);
    }
    
    
    private void loadData() {
        //三份数据
        String trainFile1 = baseDir + fileNameList[0];
        String trainFile2 = baseDir + fileNameList[1];
        // String trainFile3 = baseDir + fileNameList[2];

        String[][] data1 = DataParseUtil.loadTrainFromFile(trainFile1, labelName);
        String[][] data2 = DataParseUtil.loadTrainFromFile(trainFile2, labelName);
        
        Features features1 = DataParseUtil.fetchFeatureFromData(data1,labelName);
        Features features2 = DataParseUtil.fetchFeatureFromData(data2,labelName);
       

        rawDataMap.put(client1, data1);
        rawDataMap.put(client2, data2);
        
        trainDataMap.put(client1, new HorizontalDataFrame(data1,labelName));
        trainDataMap.put(client2, new HorizontalDataFrame(data2,labelName));
        

        featuresMap.put(client1, features1);
        featuresMap.put(client2, features2);
        



        //System.out.println("----------------loadData-------------------\n ");
    }

    private void testTrain(MetricType metricTp, String lossName, int numRoud) {
        System.out.println("----------------"+modelName+" train start-------------------");
        /* initialization */
        hflParameter = new HorizontalFedAvgPara(
                numRoud, 2, 1, 10, 2,
                new MetricType[]{metricTp}, lossName);
        master = new HorizontalFedAvg(hflParameter, modelName);


        // @numRound = 100;
        // @numClients = 3;
        // @fraction = 1.0;
        // @batchSize = 50;

        //请求初始化
        Map<Long, String> localIdMap = new HashMap<>();
        for (int i = 0; i < trainDataMap.values().stream().findFirst().get().getTable().length - 1; i++) {
            localIdMap.put((long) i, i + "");
        }
        String[] commonIDs = localIdMap.values().toArray(new String[0]);
        MatchResult matchResult = new MatchResult(localIdMap.size());
        List<CommonRequest> initRequests = master.initControl(clientInfos, matchResult, featuresMap, null);
        CommonRun.train(master, initRequests, modelMap,  rawDataMap, commonIDs);
        //TODO 除了训练结果以外的参数清除
        System.out.println("----------------"+modelName+" train end-------------------\n\n\n\n");
        //testInference();
    }

    public static void main(String[] args) {
        TestHorizontalOneFlowBert horizontalOneFlowBert = new TestHorizontalOneFlowBert();
        horizontalOneFlowBert.setUp();
        horizontalOneFlowBert.testTrainClassifier();
//        horizontalOneFlowBert
    }
}
