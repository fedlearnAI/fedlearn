
package com.jdt.fedlearn.core.integratedTest.verticalLogistic;

import com.jdt.fedlearn.core.dispatch.VerticalLogisticRegression;
import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.common.entity.core.feature.Features;
import com.jdt.fedlearn.core.example.CommonRun;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.core.type.data.Tuple2;
import com.jdt.fedlearn.core.util.DataParseUtil;
import com.jdt.fedlearn.core.util.FileUtil;
import com.jdt.fedlearn.core.model.Model;
import com.jdt.fedlearn.core.model.VerticalLRModel;
import com.jdt.fedlearn.core.parameter.VerticalLRParameter;
import com.jdt.fedlearn.core.type.MappingType;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.OptimizerType;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 线性回归集成测试，包含除了网络以外的所有
 * 主要是测试了 序列化/反序列化，模型的保存和加载
 */
public class TestVerticalLR {
    //服务端维护
    private static final String trainId = "19";
    private static final String modelToken = trainId + "_123456";
    private static final MetricType[] metrics = new MetricType[]{MetricType.CROSS_ENTRO, MetricType.G_L2NORM};
    private static final VerticalLRParameter parameter = new VerticalLRParameter(0.1, 1, metrics,
            OptimizerType.BatchGD, 0, 2, "L1", 0.2, 1e-8);
    private static final VerticalLogisticRegression regression = new VerticalLogisticRegression(parameter);
    private ClientInfo[] clientInfos;
    private static final Map<ClientInfo, String[][]> rawDataMap = new HashMap<>();
    private static final Map<ClientInfo, String[][]> inferenceRawData = new HashMap<>();
    //    private Map<ClientInfo, Features> featuresMap = new HashMap<>();
    //此处为需要手动配置的四个选项，分别是数据文件夹目录，参与方个数，有label的参与方id，label的名字。

    private final String baseDir = "./src/test/resources/classificationA_Twopartner/";
    private static final int partnerSize = 2;
    private static final int labelIndex = 0;
    private static final String labelName = "Outcome";

//    private final String baseDir = "./src/test/resources/bank/class";
//    private static final int partnerSize = 3;
//    private static final int labelIndex = 0;
//    private static final String labelName = "y";

    public void init() {
        //---------------------------下面不需要手动设置-------------------------------------//
        this.clientInfos = new ClientInfo[partnerSize];
        for (int i = 0; i < partnerSize; i++) {
            this.clientInfos[i] = new ClientInfo("127.0.0.1", 80 + i, "http");
            String fileName = "train" + i + ".csv";
            String[][] data1 = DataParseUtil.loadTrainFromFile(baseDir + fileName);
            rawDataMap.put(clientInfos[i], data1);
            String inferenceFileName = "inference" + i + ".csv";
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
            modelMap.put(client, new VerticalLRModel());
        }

        List<CommonRequest> initRequests = regression.initControl(Arrays.asList(clientInfos.clone()), matchResult, featuresMap, new HashMap<>());
        CommonRun.train(regression, initRequests, modelMap, rawDataMap, commonIds);

        //model save
        for (Map.Entry<ClientInfo, Model> x : modelMap.entrySet()) {
            VerticalLRModel verticalLinearModel = (VerticalLRModel) x.getValue();
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
            VerticalLRModel tmp = new VerticalLRModel();
            tmp.deserialize(content);
            modelMap.put(clientInfo, tmp);
        }

        //根据需要预测的id，和任务id，生成预测请求
//        String[] predictUid = DataParseUtil.loadInferenceUidList(baseDir + "inference0_onlyLabel.csv");
        String[] predictUid = new String[]{"591B", "592B", "66", "593A", "594B"};
        System.out.println("predictUid.length " + predictUid.length);
        long start = System.currentTimeMillis();

        List<CommonRequest> requests = regression.initInference(Arrays.asList(clientInfos.clone()), predictUid,new HashMap<>()); //initial request
        double[][] result = CommonRun.inference(regression, requests, modelMap, inferenceRawData).getPredicts();
        System.out.println((System.currentTimeMillis() - start) + " ms");
        System.out.println("inference result is " + Arrays.toString(result));

//        double[] tmp = DataLoad.loadInferenceYList(baseDir + "0_ori.csv");
//        System.out.println("inference auc is " + Metric.auc(result, tmp));
//        System.out.println("inference accuracy is " + Metric.accuracy(result, tmp));
//        return result;
    }

    public static void main(String[] args) throws IOException {
        TestVerticalLR verticalLR = new TestVerticalLR();
        verticalLR.init();
        verticalLR.testTrainAndTest();
        verticalLR.testInference();
    }
}