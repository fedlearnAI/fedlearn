package com.jdt.fedlearn.core.integratedTest.mixGBoost;

import com.jdt.fedlearn.core.dispatch.MixGBoost;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.math.MathExt;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.core.type.ObjectiveType;
import com.jdt.fedlearn.core.type.data.Tuple2;
import com.jdt.fedlearn.core.util.DataParseUtil;
import com.jdt.fedlearn.core.util.FileUtil;
import com.jdt.fedlearn.core.example.CommonRun;
import com.jdt.fedlearn.core.metrics.Metric;
import com.jdt.fedlearn.core.model.MixGBModel;
import com.jdt.fedlearn.core.model.Model;
import com.jdt.fedlearn.core.parameter.MixGBParameter;
import com.jdt.fedlearn.core.type.MappingType;
import com.jdt.fedlearn.core.type.MetricType;

import java.io.IOException;
import java.util.*;


/**
 * MixGBoost 回归
 * TODO 在测试中增加交叉验证选项，
 */
public class TestMixGBMixData2 {
    //服务端维护
    private final String modelToken = "52" + "_" + AlgorithmType.MixGBoost;
    private static final MetricType[] metrics = new MetricType[]{MetricType.MAPE};
    private static MixGBParameter parameter = new MixGBParameter(1.0, 1.0,
            10, 1, 0, ObjectiveType.regSquare, metrics,
            3, 10, 0.3, 33, 0.6, "", 512);

    private static final MixGBoost boost = new MixGBoost(parameter);
    private ClientInfo[] clientInfos;
    //此处为需要手动配置的四个选项，分别是数据文件夹目录，参与方个数，有label的参与方id，label的名字。
    private final String baseDir = "./src/test/resources/regressionB/";
    private static final int partnerSize = 3;
    //    private static final int labelIndex = 0;
    private static final String labelName = "40";

    private static final Map<ClientInfo, String[][]> rawDataMap = new HashMap<>();
    private static final Map<ClientInfo, String[][]> inferenceRawData = new HashMap<>();

    public void init() {
        //---------------------------下面不需要手动设置-------------------------------------//
        this.clientInfos = new ClientInfo[partnerSize];
        for (int i = 0; i < partnerSize; i++) {
            this.clientInfos[i] = new ClientInfo("127.0.0.1", 80 + i, "http");
            String fileName = "train" + i + ".csv";
            String[][] data1 = DataParseUtil.loadTrainFromFile(baseDir + fileName);
            rawDataMap.put(clientInfos[i], data1);

            String inferenceFileName = "test" + i + ".csv";
            String[][] inferenceData = DataParseUtil.loadTrainFromFile(baseDir + inferenceFileName);
            inferenceRawData.put(clientInfos[i], inferenceData);
        }
        //设置客户端是否有label
//        clientInfos[0].setHasLabel(true);
    }

    public void testTrainAndTest() throws IOException {
        ////-----------id match and feature process-------------////////////////
        System.out.println(rawDataMap.size());
        Tuple2<MatchResult, String[]> mappingOutput = CommonRun.match(MappingType.DH, Arrays.asList(clientInfos.clone()), rawDataMap);
        String[] commonIds = mappingOutput._2();
        MatchResult matchResult = mappingOutput._1();
        Map<ClientInfo, Features> featuresMap = new HashMap<>();
        for (Map.Entry<ClientInfo, String[][]> entry : rawDataMap.entrySet()) {
            Features features = DataParseUtil.fetchFeatureFromData(entry.getValue(), labelName);
            featuresMap.put(entry.getKey(), features);
        }
        //设置哪方那个特征是label
//        featuresMap.get(clientInfos[labelIndex]).setLabel(labelName);
        ////----------id match end------------/////////////////

        Map<ClientInfo, Model> modelMap = new HashMap<>(); //每个客户端维护自己的，所以此处有n份
        for (ClientInfo client : clientInfos) {
            modelMap.put(client, new MixGBModel());
        }

        List<CommonRequest> initRequests = boost.initControl(Arrays.asList(clientInfos.clone()), matchResult, featuresMap, new HashMap<>());

        CommonRun.train(boost, initRequests, modelMap, rawDataMap, commonIds);

        //model save
        for (Map.Entry<ClientInfo, Model> x : modelMap.entrySet()) {
            MixGBModel boostModel = (MixGBModel) x.getValue();
//            System.out.println(x.getKey().url() + " =-S1A=-=-: " + boostModel.s1A);
//            System.out.println(x.getKey().url() + " =-S1B=-=-: " + boostModel.s1B);
//            System.out.println(x.getKey().url() + " =-S1C=-=-: " + boostModel.s1C);
            String key = x.getKey().getPort() + "";
            String content = boostModel.serialize();
            System.out.println(content);
            FileUtil.saveModel(content, "./" + modelToken + "_" + key + ".model");
        }
    }

    public void testInference() throws IOException {
        // 加载模型
        Map<ClientInfo, Model> modelMap = new HashMap<>();
        for (ClientInfo clientInfo : clientInfos) {
            String path = "./" + modelToken + "_" + clientInfo.getPort() + ".model";
            String content = FileUtil.loadModel(path);
            MixGBModel tmp = new MixGBModel();
            tmp.deserialize(content);
            modelMap.put(clientInfo, tmp);
        }
        //根据需要预测的id，和任务id，生成预测请求
        String[] predictUid = DataParseUtil.loadInferenceUidList(baseDir + "test0.csv");
        System.out.println("predictUid.length " + predictUid.length);
        long start = System.currentTimeMillis();
        double[] label = Arrays.stream(DataParseUtil.loadInferenceLabelList(baseDir + "test0.csv")).mapToDouble(Double::doubleValue).toArray();

        List<CommonRequest> requests = boost.initInference(Arrays.asList(clientInfos.clone()), predictUid, new HashMap<>()); //initial request
        double[][] result = CommonRun.inference(boost, requests, modelMap, inferenceRawData).getPredicts();

        for (MetricType metricType : metrics) {
            double value = Metric.calculateMetric(metricType, MathExt.transpose(result)[0], label);
            System.out.println("inference " + metricType.getMetric() + ": " + value);
        }
        System.out.println("inference result is ");
        for (int i = 0; i < result.length; i++) {
            System.out.println(Arrays.toString(result[i]));
        }
    }

    public static void main(String[] args) throws IOException {
        TestMixGBMixData2 mixGBoostRegHorizontal = new TestMixGBMixData2();
        mixGBoostRegHorizontal.init();
        mixGBoostRegHorizontal.testTrainAndTest();
        mixGBoostRegHorizontal.testInference();
    }
}
