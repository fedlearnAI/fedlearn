package com.jdt.fedlearn.core.integratedTest.linearRegression;

import com.jdt.fedlearn.core.dispatch.DistributedKeyGeneCoordinator;
import com.jdt.fedlearn.core.dispatch.mixLinear.LinearRegression;
import com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillier;
import com.jdt.fedlearn.core.encryption.nativeLibLoader;
import com.jdt.fedlearn.common.entity.core.ClientInfo;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.core.entity.common.InferenceInit;
import com.jdt.fedlearn.core.entity.common.TrainInit;
import com.jdt.fedlearn.common.entity.core.feature.Features;
import com.jdt.fedlearn.common.entity.core.feature.SingleFeature;
import com.jdt.fedlearn.core.example.CommonRun;
import com.jdt.fedlearn.core.example.CommonRunKeyGene;
import com.jdt.fedlearn.core.model.Model;
import com.jdt.fedlearn.core.model.mixLinear.LinearRegressionModel;
import com.jdt.fedlearn.core.parameter.LinearParameter;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;
import com.jdt.fedlearn.core.type.MappingType;
import com.jdt.fedlearn.core.type.data.Tuple2;
import com.jdt.fedlearn.core.util.DataParseUtil;
import com.jdt.fedlearn.core.util.FileUtil;

import java.io.IOException;
import java.util.*;

import static com.jdt.fedlearn.core.util.TypeConvUtils.parse1dDouble;
import static com.jdt.fedlearn.core.util.TypeConvUtils.parse1dString;
import static java.lang.Math.sqrt;

public class TestMixedLinearRegression {
    private static final String taskId = "call-911";
    private final String modelToken = taskId + "_" + AlgorithmType.LinearRegression;
    private List<ClientInfo> clientList;
    private final LinearParameter parameter = new LinearParameter();
    Map<ClientInfo, String[][]> rawDataMapTrain = new HashMap<>();
    Map<ClientInfo, String[][]> rawDataMapInfer = new HashMap<>();
    private final Map<ClientInfo, Features> trainFeatureList = new HashMap<>();
    private String[] inferIdList;

    private String[] allAddr;
    private final int bitLen = 128;
    private final DistributedPaillier.DistPaillierPubkey  pubkey   = new DistributedPaillier.DistPaillierPubkey();
    private final DistributedPaillier.DistPaillierPrivkey privkey1 = new DistributedPaillier.DistPaillierPrivkey();
    private final DistributedPaillier.DistPaillierPrivkey privkey2 = new DistributedPaillier.DistPaillierPrivkey();
    private final DistributedPaillier.DistPaillierPrivkey privkey3 = new DistributedPaillier.DistPaillierPrivkey();
    private final DistributedPaillier.DistPaillierPrivkey[] allSk = new DistributedPaillier.DistPaillierPrivkey[3];


    public void setUp(int dataSetID) throws Exception {
        try {
            nativeLibLoader.load();
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
        }

        final String dataBase = "./src/test/resources/";

        ClientInfo party1 = new ClientInfo("127.0.0.1", 80, "http", "", "0");
        ClientInfo party2 = new ClientInfo("127.0.0.2", 80, "http", "", "1");
        ClientInfo party3 = new ClientInfo("127.0.0.3", 80, "http", "", "2");
        this.clientList = Arrays.asList(party1, party2, party3);

        allAddr = new String[clientList.size()];
        int cnt = 0;
        for(ClientInfo client: clientList) {
            allAddr[cnt++] = client.getIp()+client.getPort();
        }

        String baseData;
        String label_name;

        String fNameSuffix;
        String[] fNameListTrain;
        String[] fNameListTest;
        String[] inferId = new String[0];

        if (dataSetID == 1) {
            // mix data
            baseData = dataBase + "house/";
            fNameListTrain = new String[]{"reg0_train.csv", "reg1_train.csv", "reg2_train.csv"};
            fNameListTest = new String[]{"reg0_test.csv", "reg1_test.csv", "reg2_test.csv"};
            inferId = new String[]{"42NL", "8637999yz"};
//            inferId = new String[]{"asdfasdfasdf"};

            label_name = "y";
            fNameSuffix = "";
        } else if (dataSetID == 2) {
            // mixed data
            baseData = dataBase + "/regression_7333_7333/";
            fNameListTrain = new String[]{"df_reg_0_700_train.csv", "df_reg_1_700_train.csv", "df_reg_2_700_train.csv"};
            fNameListTest = new String[]{"df_reg_0_700_test.csv", "df_reg_1_700_test.csv", "df_reg_2_700_test.csv"};
            label_name = "8";
            fNameSuffix = "";
        }
//        else if (dataSetID == 6) { // plz add new test data here
//        }
        else {
            throw new Exception("no such dataset");
        }

        cnt = 0;
        for (ClientInfo cl : clientList) {
            rawDataMapTrain.put(cl, DataParseUtil.loadTrainFromFile(baseData + fNameListTrain[cnt] + fNameSuffix));

            String[][] rawDataMapInferTmp = DataParseUtil.loadTrainFromFile(baseData + fNameListTest[cnt] + fNameSuffix);
            Features trainFeature = loadFeatFromdataLin(rawDataMapTrain.get(cl)[0], label_name);
            Features inferFeature = loadFeatFromdataLin(rawDataMapInferTmp[0], label_name);
            if (inferFeature.hasLabel()) {
                rawDataMapInfer.put(cl, (String[][]) splitLastColumn(rawDataMapInferTmp).get("dataWithoutY"));
            } else {
                rawDataMapInfer.put(cl, rawDataMapInferTmp);
            }
            trainFeatureList.put(cl, trainFeature);
            cnt++;
        }
        if (inferId.length != 0) {
            inferIdList = inferId;
        } else {
            inferIdList = rawDataMapInfer.values().stream()
                    .map(x -> x[(x.length + 1) / 2][0])
                    .toArray(String[]::new);
        }

        DistributedKeyGeneCoordinator coordinator = new DistributedKeyGeneCoordinator(1000, clientList.size(),
                bitLen, allAddr, true, "KeyGeneInMixLinear"+System.currentTimeMillis());
        CommonRunKeyGene.generate(coordinator, clientList.toArray(new ClientInfo[0]));

        pubkey.loadClassFromFile("pubKey");
        privkey1.loadClassFromFile("privKey-" + 1);
        privkey2.loadClassFromFile("privKey-" + 2);
        privkey3.loadClassFromFile("privKey-" + 3);
        allSk[0] = privkey1;
        allSk[1] = privkey2;
        allSk[2] = privkey3;
    }


    /*
     * =========================
     * Training Test
     * =========================
     */
    public void trainTestCommon() throws IOException {
        parameter.setMaxEpoch(2);
        Tuple2<MatchResult, String[]> mappingOutput = CommonRun.match(MappingType.EMPTY, clientList, rawDataMapTrain);
        String[] commonIds = mappingOutput._2();

        LinearRegression master = new LinearRegression(parameter);
        MatchResult matchResult = mappingOutput._1();

        Map<String, Object>  others = new HashMap<>();
        others.put("pubKeyStr" , pubkey.toJson());

        List<CommonRequest> oldInitRequests = master.initControl(clientList, matchResult, trainFeatureList, others);
        List<CommonRequest> newInitRequests = new ArrayList<>();
        int cnt = 0;
        for(CommonRequest request: oldInitRequests) {
            TrainInit oldTrainInit = ((TrainInit) request.getBody());
            Map<String, Object>  clientOthers = ((TrainInit) request.getBody()).getOthers();
            clientOthers.put("privKeyStr", allSk[cnt].toJson());
            clientOthers.put("pubKeyStr" , pubkey.toJson());
            TrainInit newInit = new TrainInit(oldTrainInit.getParameter(), oldTrainInit.getFeatureList(), oldTrainInit.getMatchId(), clientOthers);
            CommonRequest newRequest = CommonRequest.buildTrainInitial(request.getClient(), newInit);
            newInitRequests.add(newRequest);
            cnt += 1;
        }

        Map<ClientInfo, Model> clientMap = new HashMap<>();
        for (ClientInfo client : clientList) {
            clientMap.put(client, new LinearRegressionModel());
        }

        CommonRun.train(master, newInitRequests, clientMap, rawDataMapTrain, commonIds);

        //model save
        for (Map.Entry<ClientInfo, Model> x : clientMap.entrySet()) {
            LinearRegressionModel clientModel = (LinearRegressionModel) x.getValue();
            System.out.println(x.getKey().url());
            System.out.println(x.getKey().url());
            System.out.println(x.getKey().url());
            String key = x.getKey().getIp() + x.getKey().getPort() + "";
            String content = clientModel.serialize();
            FileUtil.saveModel(content, "./" + modelToken + "_" + key + ".model");
        }
    }

    /*
     * =========================
     * Inference Test
     * =========================
     */
    public void inferenceTest() throws IOException {
        Map<ClientInfo, Model> modelMap = new HashMap<>();
        for (ClientInfo clientInfo : clientList) {
            String path = "./" + modelToken + "_" + clientInfo.getIp() + clientInfo.getPort() + ".model";
            String content = FileUtil.loadModel(path);

            LinearRegressionModel clientModelsFromFile = new LinearRegressionModel();
            clientModelsFromFile.deserialize(content);
            modelMap.put(clientInfo, clientModelsFromFile);
        }

        LinearRegression master = new LinearRegression(parameter);
        Map<String, Object> others = new HashMap<>();
        others.put("pubKeyStr", pubkey.toJson());
        List<CommonRequest> oldInitRequests = master.initInference(clientList, inferIdList, others);
        List<CommonRequest> newInitRequests = new ArrayList<>();
        int cnt = 0;
        for(CommonRequest request: oldInitRequests) {
            InferenceInit oldTrainInit = ((InferenceInit) request.getBody());
            Map<String, Object>  clientOthers = ((InferenceInit) request.getBody()).getOthers();
            clientOthers.put("privKeyStr", allSk[cnt].toJson());
            clientOthers.put("pubKeyStr" , pubkey.toJson());
            InferenceInit newInit = new InferenceInit(oldTrainInit.getUid(), clientOthers);
            CommonRequest newRequest = CommonRequest.buildTrainInitial(request.getClient(), newInit);
            newInitRequests.add(newRequest);
            cnt += 1;
        }

        double[][] finalYHat;
        finalYHat = CommonRun.inference(master, newInitRequests, modelMap, rawDataMapInfer).getPredicts();
        System.out.println(Arrays.toString(Arrays.stream(finalYHat).mapToDouble(x -> x[0]).toArray()));
    }

    public static void main(String[] args) throws IOException {
        final int[] testDataSets = new int[]{1};
        for (int i : testDataSets) {
            System.out.println("\n\n========================== Running test dataset: " + i + " ==========================\n\n");
            TestMixedLinearRegression newTest = new TestMixedLinearRegression();
            try {
                newTest.setUp(i);
            } catch (Exception e) {
                e.printStackTrace();
            }
            newTest.trainTestCommon();
            newTest.inferenceTest();
        }
    }

    public static Features loadFeatFromdataLin(String[] firstLine, String label) {
        List<SingleFeature> r = new ArrayList<>();
        boolean haslabel = false;

        for (int i = 0; i < firstLine.length; i++) {

            if (firstLine[i].contains(label)) {
                r.add(new SingleFeature(firstLine[i], "string", i - 1, 1));
                haslabel = true;
            } else {
                r.add(new SingleFeature(firstLine[i], "string", i - 1, 1));
            }

        }
        if (haslabel) {
            return new Features(r, label);
        } else {
            return new Features(r);
        }
    }

    public static Map<String, Object> splitLastColumn(String[][] rawTable) {
        int datasetSize = rawTable.length;
        int featureDim = rawTable[0].length - 1; // feature 去除最后一列
        String[][] sample = new String[datasetSize][featureDim];
        String[] groundTruth = new String[datasetSize];

        for (int row = 0; row < datasetSize; row++) {
            String[] strs = rawTable[row];
            for (int col = 0; col < featureDim; col++) {
                if (null == strs[col + 1] || strs[col + 1].isEmpty()) {
                    sample[row][col] = String.valueOf(0d);
                } else {
                    sample[row][col] = strs[col];
                }
            }
            groundTruth[row] = strs[featureDim];
        }
        Map<String, Object> res = new HashMap<>();
        res.put("dataWithoutY", sample);
        res.put("y", groundTruth);
        return res;
    }

    /**
     * debug时返回每个client对于全局H的RMSE
     */
    public static void getClassRes(ClientInfo[] clients, String[] final_res, double[] h_master) {
        int cntt = 0;
        for (ClientInfo client : clients) {
            double[] y_true_local = parse1dDouble(parse1dString(final_res[cntt])[0]);
            double[] y_hat = parse1dDouble(parse1dString(final_res[cntt])[1]);
            double mse_global = 0d;
            double mse_local = 0d;
            assert (y_hat.length == h_master.length);
            int cnt = 0;
            for (int i = 0; i < h_master.length; i++) {
                if (!Double.isNaN(y_hat[i])) {
                    double d = y_hat[i] - h_master[i];
                    mse_global += d * d;
                    cnt += 1;
                }
            }
            mse_global /= (cnt + 0.001);
            cnt = 0;
            for (int i = 0; i < h_master.length; i++) {
                if (!Double.isNaN(y_hat[i])) {
                    double d = y_hat[i] - y_true_local[i];
                    mse_local += d * d;
                    cnt += 1;
                }
            }
            mse_local /= (cnt + 0.001);

            cntt += 1;
            System.out.println("Client " + client.getIp() + client.getPort() + " : The final RMSE is " + sqrt(mse_global) + "\tMSE is " + mse_global);
            System.out.println("\tThe final RMSE_local is " + sqrt(mse_local) + "\tMSE_local is " + mse_local);
        }
    }
}
