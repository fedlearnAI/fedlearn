//package com.jdd.ml.federated.core;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.jdd.ml.federated.core.entity.ClientInfo;
//import com.jdd.ml.federated.core.fake.DataLoad;
//import com.jdd.ml.federated.core.util.Tool;
//import com.jdd.ml.federated.core.algorithm.LinearRegression;
//import com.jdd.ml.federated.core.entity.common.CommonRequest;
//import com.jdd.ml.federated.core.entity.common.CommonResponse;
//import com.jdd.ml.federated.core.entity.feature.Features;
//import com.jdd.ml.federated.core.entity.feature.SingleFeature;
//import com.jdd.ml.federated.core.exception.SerializeException;
//import com.jdd.ml.federated.core.load.linearRegression.LinearTrainData;
//import com.jdd.ml.federated.core.model.LinearRegressionModel;
//import com.jdd.ml.federated.core.parameter.LinearParameter;
//import com.jdd.ml.federated.core.util.IdFeatMapper;
//import org.testng.annotations.Test;
//
//
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.util.*;
//
//import static java.lang.Math.sqrt;
//
//public class TestDifferentialPrivacyMixedLinR {
//    private String taskId;
//    private List<ClientInfo> clientInfos;
//    private LinearParameter parameter = new LinearParameter();
//    private static KeyPair keyPair = Pailliee.generateKey();
//    //界面端传给服务端
//    private Map<ClientInfo, Features> features = new HashMap<>();
//    //
//    //在实际的分布式任务中可能会有多个任务，应该用list保存modelId
//    private static String modelId;
//    private static Map<ClientInfo, LinearTrainData> trainDataMap = new HashMap<>();
//
//    private Map<ClientInfo, int [][]> K = new HashMap<>();
//    private Map<ClientInfo, double[]> H = new HashMap<>();
//    private Map<ClientInfo, Map<Integer, Integer>> featMaps = new HashMap<>();
//    private Map<ClientInfo, Map<Integer, Integer>> idMaps = new HashMap<>();
//    private Map<Integer, Integer> featID = new HashMap<>();
//    private int all_M;
//    private int all_N;
//
//    @Test
//    public void trainDPTest() throws Exception {
//        double[] DPparameters = new double[]{0, 1000};
//        setUp("client");
//        List<List<String>> models = new ArrayList<>();
//        for (double p : DPparameters){
//            System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++");
//            System.out.println("Differential_privacy_parameter: " + p);
//            parameter.setDifferentialPrivacyParameter(p);
//            List<String> model = trainTest();
//            System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++");
//            System.out.println();
//            models.add(model);
//        }
//        setUp("test");
//        for (List<String> model : models){
//            inferenceTest(model);
//        }
//    }
//
//    public void setUp(String tesk) throws Exception {
//        this.taskId = "19";
//        trainDataMap.clear();
//        features.clear();
//        ClientInfo party1 = new ClientInfo("127.0.0.1", 80, "http", 0, 0);
//        ClientInfo party2 = new ClientInfo("127.0.0.1", 81, "http", 1, 0);
//        ClientInfo party3 = new ClientInfo("127.0.0.1", 82, "http", 2, 0);
//        this.clientInfos = Arrays.asList(party1, party2, party3);
//        String[][] data1;
//        String[][] data2;
//        String[][] data3;
//        String baseData = "src/test/resources/media_all_mix_0.3/";
//        if (tesk.equals("client")){
//            data1 = DataLoad.loadTrainFromFile(baseData + "jd_tain_data_pre_clean_unfe_cl0_train_0.3.csv");
//            data2 = DataLoad.loadTrainFromFile(baseData + "jd_tain_data_pre_clean_unfe_cl1_train_0.3.csv");
//            data3 = DataLoad.loadTrainFromFile(baseData + "jd_tain_data_pre_clean_unfe_cl2_train_0.3.csv");
//        }
//        else {
//            data1 = DataLoad.loadTrainFromFile(baseData + "jd_tain_data_pre_clean_unfe_cl0_test_0.3.csv");
//            data2 = DataLoad.loadTrainFromFile(baseData + "jd_tain_data_pre_clean_unfe_cl1_test_0.3.csv");
//            data3 = DataLoad.loadTrainFromFile(baseData + "jd_tain_data_pre_clean_unfe_cl2_test_0.3.csv");
//        }
//
//        trainDataMap.put(party1, new LinearTrainData(data1));
//        trainDataMap.put(party2, new LinearTrainData(data2));
//        trainDataMap.put(party3, new LinearTrainData(data3));
//        //特征
//        features.put(party1, new Features(Arrays.asList(
//                new SingleFeature( "0", "string", 0, 1),
//                new SingleFeature( "14", "string", 1, 1),
//                new SingleFeature( "22", "string", 2, 1),
//                new SingleFeature( "1", "string", 3, 1),
//                new SingleFeature( "32", "string", 4, 1),
//                new SingleFeature( "5", "string", 5, 1),
//                new SingleFeature( "12", "string", 6, 1),
//                new SingleFeature( "31", "string", 7, 1),
//                new SingleFeature( "39", "string", 8, 1),
//                new SingleFeature( "27", "string", 9, 1),
//                new SingleFeature( "9", "string", 10, 1),
//                new SingleFeature( "35", "string", 11, 1),
//                new SingleFeature( "26", "string", 12, 1),
//                new SingleFeature( "3", "string", 13, 1),
//                new SingleFeature( "13", "string", 14, 1),
//                new SingleFeature( "21", "string", 15, 1),
//                new SingleFeature( "28", "string", 16, 1),
//                new SingleFeature( "15", "string", 17, 1),
//                new SingleFeature( "16", "string", 18, 1),
//                new SingleFeature( "24", "string", 19, 1),
//                new SingleFeature( "18", "string", 20, 1),
//                new SingleFeature( "7", "string", 21, 1),
//                new SingleFeature( "2", "string", 22, 1),
//                new SingleFeature( "8", "string", 23, 1)), "40"));
//        features.put(party2, new Features(Arrays.asList(
//                new SingleFeature( "29", "string", 0, 1),
//                new SingleFeature( "19", "string", 1, 1),
//                new SingleFeature( "22", "string", 2, 1),
//                new SingleFeature( "1", "string", 3, 1),
//                new SingleFeature( "5", "string", 4, 1),
//                new SingleFeature( "31", "string", 5, 1),
//                new SingleFeature( "39", "string", 6, 1),
//                new SingleFeature( "27", "string", 7, 1),
//                new SingleFeature( "26", "string", 8, 1),
//                new SingleFeature( "3", "string", 9, 1),
//                new SingleFeature( "28", "string", 10, 1),
//                new SingleFeature( "15", "string", 11, 1),
//                new SingleFeature( "34", "string", 12, 1),
//                new SingleFeature( "7", "string", 13, 1),
//                new SingleFeature( "11", "string", 14, 1),
//                new SingleFeature( "23", "string", 15, 1),
//                new SingleFeature( "17", "string", 16, 1),
//                new SingleFeature( "8", "string", 17, 1),
//                new SingleFeature( "33", "string", 18, 1),
//                new SingleFeature( "20", "string", 19, 1)), "40"));
//        features.put(party3, new Features(Arrays.asList(
//                new SingleFeature( "38", "string", 0, 1),
//                new SingleFeature( "22", "string", 1, 1),
//                new SingleFeature( "4", "string", 2, 1),
//                new SingleFeature( "1", "string", 3, 1),
//                new SingleFeature( "5", "string", 4, 1),
//                new SingleFeature( "31", "string", 5, 1),
//                new SingleFeature( "6", "string", 6, 1),
//                new SingleFeature( "39", "string", 7, 1),
//                new SingleFeature( "27", "string", 8, 1),
//                new SingleFeature( "10", "string", 9, 1),
//                new SingleFeature( "30", "string", 10, 1),
//                new SingleFeature( "26", "string", 11, 1),
//                new SingleFeature( "3", "string", 12, 1),
//                new SingleFeature( "28", "string", 13, 1),
//                new SingleFeature( "15", "string", 14, 1),
//                new SingleFeature( "25", "string", 15, 1),
//                new SingleFeature( "7", "string", 16, 1),
//                new SingleFeature( "36", "string", 17, 1),
//                new SingleFeature( "8", "string", 18, 1),
//                new SingleFeature( "37", "string", 19, 1)), "40"));
//
//        /**
//         * get global statistics
//         * all_M: total number of unique features, should be returned by an ID-Mapping function
//         * all N: total number of unique instances, should be returned by an ID-Mapping function.
//         *        here only for vertical case.
//         * tmpFMap: maps local feature id to the global feature id.
//         *          [0, #localFeature) -> [0, #globalFeature)
//         * tmpK: indicates how many times the feature is overlapped globally. 1 means it is a private feature.
//         * featID: Map<SingleFeature, int>, map if all unique features and its position in the global feature table
//         */
//        for (ClientInfo info : clientInfos) {
//            trainDataMap.get(info).init(null, features.get(info));
//        }
//        int cnt = 0;
//        for (ClientInfo info : clientInfos) {
//            for (SingleFeature singleFeat: features.get(info).getFeatureList()) {
//                if( !featID.containsKey(singleFeat.getId()) ) {
//                    featID.put(singleFeat.getId(), cnt);
//                    cnt += 1;
//                }
//            }
//        }
//        getK_H_featMap_idMap();
//    }
//
//    public void getK_H_featMap_idMap() throws Exception{
//        int [][] resK;
//        List<String []> featNameList = new ArrayList<> ();
//        List<String []> idNameList = new ArrayList<> ();
//        List<double []> labelList = new ArrayList<> ();
//        for (ClientInfo info : clientInfos) {
//            List<SingleFeature> feats = features.get(info).getFeatureList();
//            int featSize = feats.size();
//            String[] featName = new String[featSize];
//            int cnt = 0;
//            for(SingleFeature f: feats) {
//                featName[cnt] = f.getName();
//                cnt += 1;
//            }
//
//            long[] ids = trainDataMap.get(info).getUid();
//            assert ids != null;
//            String[] idName = new String [ids.length];
//            cnt = 0;
//            for(long id: ids) {
//                idName[cnt] =  Long.toString(id);
//                cnt += 1;
//            }
//            featNameList.add(featName);
//            idNameList.add(idName);
//            labelList.add(trainDataMap.get(info).getLabel());
//        }
//        int cnt = 0;
//        IdFeatMapper mapper = new IdFeatMapper(featNameList, idNameList, labelList);
//        all_M = mapper.getM();
//        all_N = mapper.getN();
//        for(ClientInfo info: clientInfos) {
//            resK = mapper.getK(featNameList.get(cnt), idNameList.get(cnt));
//
//            int [] tmpF =  mapper.getAllFeatNamePosition( featNameList.get(cnt) );
//            int [] tmpI =  mapper.getAllIdNamePosition( idNameList.get(cnt) );
//            Map<Integer, Integer> fMap_tmp = new HashMap<>();
//            Map<Integer, Integer> idMap_tmp = new HashMap<>();
//            for(int i = 0; i < all_M; i++){ fMap_tmp.put(i, -1); }
//            for(int i = 0; i < all_N; i++){ idMap_tmp.put(i, -1); }
//            for(int i = 0; i < all_N; i++) { idMap_tmp.put(i, -1); }
//
//            int cnt1 = 0;
//            for(int item: tmpF) {
//                fMap_tmp.put(item, cnt1);
//                cnt1 += 1;
//            }
//            cnt1 = 0;
//            for(int item: tmpI) {
//                idMap_tmp.put(item, cnt1);
//                cnt1 += 1;
//            }
//            featMaps.put(info, fMap_tmp);
//            idMaps.put(info, idMap_tmp);
//            H.put(info, mapper.getH(idNameList.get(cnt)));
//            K.put(info, resK);
////            System.out.println(toJsons(resK)+ "\n");
//            cnt += 1;
//        }
//
//    }
//
//    public List<String> trainTest() throws IOException {
//        long start = System.currentTimeMillis();
//        //id 对齐
//        Map<ClientInfo, LinearRegressionModel> clientMap = new HashMap<>();
//        ClientInfo info = clientInfos.get(0);
//
//        int cnt = 0;
//        ArrayList <ClientInfo> clients = new ArrayList<>();
//        // same as clientInfos.remove(0);
//        for (ClientInfo clientInfo : clientInfos) {
//            if(cnt > 0) {
//                clients.add(clientInfo);
//            }
//            cnt += 1;
//        }
//
//        // 此处使master获得所有方的 H
//        // TODO: 可能需要更改
//        double [] h_master = H.get(info).clone();
//        for (ClientInfo clientInfo : clients) {
//            double [] h_tmp = H.get(clientInfo);
//            for(int i = 0; i < all_N; i++) {
//                if(h_tmp[i] != 0d) {
//                    h_master[i] = h_tmp[i];
//                }
//            }
//        }
//
//        boolean useEnc = true;
//        boolean useParallel = true;
//        double [] initWeight = Tool.initWeight(all_M);
//        LinearRegression master =  null;
////                new LinearRegression(
////                K.get(info),
////                null,
////                featMaps.get(info),
////                3,
////                3,
////                all_M,
////                all_N,
////                trainDataMap.get(info).getTable(),
////                features.get(info),
////                useEnc,
////                useParallel,
////                initWeight
////        );
//        master.setKeyPair(keyPair);
//
//        for (ClientInfo clientInfo : clients) {
//            // the others are actual clients
//            clientMap.put(clientInfo,
//                    new LinearRegressionModel(
//                            K.get(clientInfo),
//                            featMaps.get(clientInfo),
//                            idMaps.get(clientInfo),
//                            3,
//                            all_M,
//                            all_N,
//                            trainDataMap.get(clientInfo).getTable(),
//                            features.get(info),
//                            useEnc,
//                            useParallel,
//                            initWeight
//                    )
//            );
//        }
//
//        List<CommonResponse> responses = new ArrayList<>();
//        for (ClientInfo clientInfo : clients) {
//            CommonResponse response = new CommonResponse(clientInfo, toJsons(""));
//            responses.add(response);
//        }
//        int p = 1;
//        while (!master.isStop()) {
//            List<CommonRequest> requests = master.control(taskId, p, responses);
//            responses.clear();
//            for (CommonRequest request : requests) {
//                String jsonData = request.getBody();
//                //////////////客户端代码///////////////
//                LinearRegressionModel model = clientMap.get(request.getClient());
//                LinearTrainData trainData = trainDataMap.get(request.getClient());
//                String map = model.client(taskId, p, jsonData, trainData);
//                ///////////////////////////
//                CommonResponse response = new CommonResponse(request.getClient(), map);
//                responses.add(response);
//                if(p==1){
//                    System.out.println("\tweight = " + toJsons(model.weight) +
//                            "\n\tg = " + toJsons(model.g));
//                }
//            }
//            if(p==1) {
//                System.out.println("iterNum = " + master.iterNum + "\tp = " + p + "\tLoss = " + master.getMetric() +
//                        "\n\tweight = " + toJsons(master.weight) +
//                        "\n\tg = " + toJsons(master.getG()));
//            }
//            p = master.getNextPhase(p, responses);
//        }
//
//        //model save
//        List<String> save_model = new ArrayList<>();
//        save_model.add(master.serialize());
//        master.save("master_model");
//        for (ClientInfo clientInfo : clients) {
//            clientMap.get(clientInfo).save("client_model_" + clientInfo.getRank());
//            save_model.add(clientMap.get(clientInfo).serialize());
//        }
//        System.out.println("========full client end========\n consumed time in seconds:");
//        System.out.println((System.currentTimeMillis() - start) / 1000.0);
//        return save_model;
//    }
//
//    public void inferenceTest(List<String> string_model) throws IOException {
//        //id 对齐
//        Map<ClientInfo, LinearRegressionModel> clientMap = new HashMap<>();
//        ClientInfo info = clientInfos.get(0);
//
//        int cnt = 0;
//        ArrayList <ClientInfo> clients = new ArrayList<>();
//        // same as clientInfos.remove(0);
//        for (ClientInfo clientInfo : clientInfos) {
//            if(cnt > 0) {
//                clients.add(clientInfo);
//            }
//            cnt += 1;
//        }
//
//        // 此处使master获得所有方的 H
//        // TODO: 可能需要更改
//        double [] h_master = H.get(info).clone();
//        for (ClientInfo clientInfo : clients) {
//            double [] h_tmp = H.get(clientInfo);
//            for(int i = 0; i < all_N; i++) {
//                if(h_tmp[i] != 0d) {
//                    h_master[i] = h_tmp[i];
//                }
//            }
//        }
//        Files.write(Paths.get("master_model"), string_model.get(0).getBytes());
//        LinearRegression master =  new LinearRegression(
//                trainDataMap.get(info).getTable(), "master_model",
//                all_N,
//                featMaps.get(info),
//                idMaps.get(info));
//
//        for (ClientInfo clientInfo : clients) {
//            // the others are actual clients
//            Files.write(Paths.get("client_model_" + clientInfo.getRank()), string_model.get(clientInfo.getRank()).getBytes());
//            clientMap.put(clientInfo,
//                    new LinearRegressionModel(
//                            trainDataMap.get(clientInfo).getTable(),
//                            "client_model_"+clientInfo.getRank(),
//                            all_N,
//                            featMaps.get(clientInfo),
//                            idMaps.get(clientInfo)));
//        }
//
//        int p = 1;
//        List<CommonRequest> requests;
//        List<CommonResponse> responses = new ArrayList<>();
//
//        for(ClientInfo client: clients){
//            LinearRegressionModel model = clientMap.get(client);
//            String map = model.inference(p, null, null);
//            CommonResponse response = new CommonResponse(client, map);
//            responses.add(response);
//        }
//        requests = master.inferenceControl("", 1, responses);
//
//        // get prediction result
//        double[] y_hat = LinearRegression.parse1dDouble(requests.get(0).getBody());
//        double mse = 0d;
//        for(int i = 0; i < all_N; i++) {
//            double d = y_hat[i]-h_master[i];
//            mse += d * d;
//        }
//        mse /= all_N;
//        System.out.println("The final RMSE is " + sqrt(mse) + "\tMSE is " + mse);
//    }
//
//    private static String toJsons(Object obj) {
//        String jsonStr;
//        ObjectMapper objectMapper = new ObjectMapper();
//        try {
//            jsonStr = objectMapper.writeValueAsString(obj);
//        } catch (JsonProcessingException e) {
//            throw new SerializeException("Error when Serialize");
//        }
//        return jsonStr;
//    }
//
//}
