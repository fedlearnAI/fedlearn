package com.jdt.fedlearn.core.example.fgb;

import com.jdt.fedlearn.core.dispatch.FederatedGB;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.example.CommonRun;
import com.jdt.fedlearn.core.model.DistributedFederatedGBModel;
import com.jdt.fedlearn.core.model.Model;
import com.jdt.fedlearn.core.parameter.FgbParameter;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.core.type.*;
import com.jdt.fedlearn.core.type.data.Tuple2;
import com.jdt.fedlearn.core.util.DataParseUtil;
import com.jdt.fedlearn.core.util.FileUtil;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FederatedGB 二分类
 */
public class RunDistributedFederatedGB {
    //服务端维护
    private ClientInfo[] clientInfos;
    //此处为需要手动配置的四个选项，分别是数据文件夹目录，参与方个数，有label的参与方id(从0开始排)，label的名字。
    private final String baseDir;
    private final int partnerSize;
    private final int labelIndex;
    private final String labelName;
    private final String uidName; //TODO 需要指定各方的uidName
    private static final Map<ClientInfo, String[][]> rawDataMap = new HashMap<>();
    private static final Map<ClientInfo, String[][]> inferenceRawData = new HashMap<>();

    public RunDistributedFederatedGB(String baseDir, int partnerSize, int labelIndex, String labelName, String uidName) {
        this.baseDir = baseDir;
        this.partnerSize = partnerSize;
        this.labelIndex = labelIndex;
        this.labelName = labelName;
        this.uidName = uidName;
        init();
    }

    private void init() {
        //---------------------------下面不需要手动设置-------------------------------------
        this.clientInfos = new ClientInfo[partnerSize];
        for (int i = 0; i < partnerSize; i++) {
            this.clientInfos[i] = new ClientInfo("127.0.0.1", 80 + i, "http", "", String.valueOf(i + 1));
            String fileName = "train" + i + ".csv";
            String[][] data = DataParseUtil.loadTrainFromFile(baseDir + fileName);
            rawDataMap.put(clientInfos[i], data);

            String inferenceFileName = "inference" + i + ".csv";
            String[][] inferenceData = DataParseUtil.loadTrainFromFile(baseDir + inferenceFileName);
            inferenceRawData.put(clientInfos[i], inferenceData);
        }
    }


    public void trainAndTest(String trainId, FederatedGB boost) throws IOException {
        System.out.println("secure boost binary classification test start:");
        //-----------------id match and feature extract---------------------////
        // coordinate端singleMatch生成的内容是MappingOutput
        Tuple2<MatchResult, String[]> mappingReport = CommonRun.match(MappingType.MD5, Arrays.asList(clientInfos.clone()), rawDataMap);
        // match之后生成的是MatchResult
        MatchResult matchResult = mappingReport._1();

        Map<ClientInfo, Features> featuresMap = new HashMap<>();
        for (Map.Entry<ClientInfo, String[][]> entry : rawDataMap.entrySet()) {
            Features features1 = DataParseUtil.fetchFeatureFromData(entry.getValue());
            featuresMap.put(entry.getKey(), features1);
        }
        // 设置哪方那个特征是label
        featuresMap.get(clientInfos[labelIndex]).setLabel(labelName);
        ////---------------id match and feature extract end---------------------////

        // initial and train
        List<CommonRequest> initRequests = boost.initControl(Arrays.asList(clientInfos.clone()), matchResult, featuresMap, new HashMap<>());
        Map<ClientInfo, Model> modelMap = CommonRun.trainNew(boost, clientInfos, initRequests, mappingReport._2(), rawDataMap);

        //model save
        for (Map.Entry<ClientInfo, Model> x : modelMap.entrySet()) {
            DistributedFederatedGBModel boostModel = (DistributedFederatedGBModel) x.getValue();
            String key = x.getKey().getPort() + "";
            String content = boostModel.serialize();
            FileUtil.saveModel(content, "./" + trainId + "_" + key + ".model");
        }
    }

    public void inference(String trainId, FederatedGB boost, String[] predictUid) throws IOException {
        // load model
        Map<ClientInfo, Model> modelMap = new HashMap<>();
        for (ClientInfo clientInfo : clientInfos) {
            String path = "./" + trainId + "_" + clientInfo.getPort() + ".model";
            String content = FileUtil.loadModel(path);
            DistributedFederatedGBModel tmp = new DistributedFederatedGBModel();
            tmp.deserialize(content);
            modelMap.put(clientInfo, tmp);
        }
        List<CommonRequest> requests = boost.initInference(Arrays.asList(clientInfos.clone()), predictUid,null);
        double[][] result = CommonRun.inference(boost, requests, modelMap, inferenceRawData).getPredicts();
        System.out.println("inference result is " + Arrays.deepToString(result));
        System.out.println("secure boost binary classification test end:");
    }

    private static void runFederatedGBBinary() throws IOException {
        //唯一id
        String trainId = "19_Binary" + "_" + AlgorithmType.FederatedGB;

        //数据集和label
        String baseDir = "./src/test/resources/classificationA/";
        final int partnerSize = 3;
        final int labelIndex = 0;
        final String labelName = "Outcome";
        RunDistributedFederatedGB runFederatedGB = new RunDistributedFederatedGB(baseDir, partnerSize, labelIndex, labelName, "uid");

        //参数
        final MetricType[] metrics = new MetricType[]{MetricType.ACC, MetricType.AUC, MetricType.F1, MetricType.RECALL};
        final FgbParameter parameter = new FgbParameter.Builder(3, metrics, ObjectiveType.binaryLogistic).firstRoundPred(FirstPredictType.ZERO).minSampleSplit(10).colSample(1).rowSample(1).maxDepth(5).build();
        final FederatedGB boost = new FederatedGB(parameter,AlgorithmType.DistributedFederatedGB);
        runFederatedGB.trainAndTest(trainId, boost);

        //推理数据
        String[] predictUid = new String[]{"591B", "592B", "1179", "593A", "594B"};
        runFederatedGB.inference(trainId, boost, predictUid);
//        predictUid = new String[]{"594B","593A",  "1179", "592B", "591B"};
//        runFederatedGB.inference(trainId, boost, predictUid);
    }

    private static void runFederatedGBBinary2() throws IOException {
        //唯一id
        String trainId = "192_Binary" + AlgorithmType.FederatedGB;

        //数据集和label
        String baseDir = "./src/test/resources/classificationA_Small/";
        final int partnerSize = 3;
        final int labelIndex = 0;
        final String labelName = "Outcome";
        RunDistributedFederatedGB runFederatedGB = new RunDistributedFederatedGB(baseDir, partnerSize, labelIndex, labelName, "uid");

        //参数
        final MetricType[] metrics = new MetricType[]{MetricType.ACC, MetricType.AUC, MetricType.F1, MetricType.RECALL};
        final FgbParameter parameter = new FgbParameter.Builder(2, metrics, ObjectiveType.binaryLogistic).maxDepth(3).build();
        final FederatedGB boost = new FederatedGB(parameter,AlgorithmType.DistributedFederatedGB);
        runFederatedGB.trainAndTest(trainId, boost);

        //推理数据
        String[] predictUid = new String[]{"1B", "592B", "2A", "3A", "4A"};
        runFederatedGB.inference(trainId, boost, predictUid);
    }


    public static void runFederatedGBMulti() throws IOException {
        String trainId = 17 + "_Multi_" + AlgorithmType.FederatedGB;
        //数据集和label
        String baseDir = "./src/test/resources/multiClassificationA/";
//        String baseDir = "./src/test/resources/digits/";
        final int partnerSize = 3;
        final int labelIndex = 0;
        final String labelName = "label";
        RunDistributedFederatedGB runFederatedGB = new RunDistributedFederatedGB(baseDir, partnerSize, labelIndex, labelName, "uid");

        //参数
        final MetricType[] metrics = new MetricType[]{MetricType.MACC, MetricType.MERROR};
        final FgbParameter parameter = new FgbParameter.Builder(3, metrics,  ObjectiveType.multiSoftmax).numClass(40).maxDepth(3).build();
        final FederatedGB boost = new FederatedGB(parameter,AlgorithmType.DistributedFederatedGB);
        runFederatedGB.trainAndTest(trainId, boost);

        //推理数据
        String[] predictUid = new String[]{"13", "14", "15", "16"};
        runFederatedGB.inference(trainId, boost, predictUid);
    }

    public static void runFederatedGBPoisson() throws IOException {
        String trainId = 16 + "_Poisson_" + AlgorithmType.FederatedGB;
        //数据集和label
        String baseDir = "./src/test/resources/regressionE/";
        final int partnerSize = 3;
        final int labelIndex = 0;
        final String labelName = "y";
        RunDistributedFederatedGB runFederatedGB = new RunDistributedFederatedGB(baseDir, partnerSize, labelIndex, labelName, "uid");

        //参数
        final MetricType[] metrics = new MetricType[]{MetricType.MAPE, MetricType.MAAPE};
        final FgbParameter parameter = new FgbParameter.Builder(5, metrics, ObjectiveType.countPoisson).build();
        final FederatedGB boost = new FederatedGB(parameter,AlgorithmType.DistributedFederatedGB);
        runFederatedGB.trainAndTest(trainId, boost);

        //推理数据
        String[] predictUid = new String[]{"bllXflZCdhNevVSt", "iY1fkFgggCED1yBU", "fR8yOfL8cQUo7eTw"};
        runFederatedGB.inference(trainId, boost, predictUid);
    }


    public static void runFederatedGBRegression() throws IOException {
        //唯一id
        String trainId = 18 + "_Regression" + AlgorithmType.FederatedGB;

        //数据集和label
        String baseDir = "./src/test/resources/regressionA/";
        final int partnerSize = 3;
        final int labelIndex = 0;
        final String labelName = "y";
        RunDistributedFederatedGB runFederatedGB = new RunDistributedFederatedGB(baseDir, partnerSize, labelIndex, labelName, "uid");

        //参数
        final MetricType[] metrics = new MetricType[]{MetricType.MAPE, MetricType.RMSE};
        final FgbParameter parameter = new FgbParameter.Builder(10, metrics, ObjectiveType.regSquare).build();
        final FederatedGB boost = new FederatedGB(parameter,AlgorithmType.DistributedFederatedGB);
        runFederatedGB.trainAndTest(trainId, boost);

        //推理数据
        String[] predictUid = new String[]{"291B", "TEST"};
        runFederatedGB.inference(trainId, boost, predictUid);
    }

    public static void main(String[] args) throws IOException {
//        runFederatedGBBinary2();
        runFederatedGBBinary();
//        runFederatedGBMulti();
//        [0.61939520729632, 0.25947167408555105, NaN, 0.5760989380907545, 0.1309618225508566]
    }
}
