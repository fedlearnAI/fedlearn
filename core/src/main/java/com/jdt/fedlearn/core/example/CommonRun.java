package com.jdt.fedlearn.core.example;

import com.jdt.fedlearn.core.dispatch.common.Control;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.base.SingleElement;
import com.jdt.fedlearn.core.entity.common.*;
import com.jdt.fedlearn.core.entity.psi.MatchInit;
import com.jdt.fedlearn.core.entity.serialize.JavaSerializer;
import com.jdt.fedlearn.core.entity.serialize.Serializer;
import com.jdt.fedlearn.core.loader.common.CommonLoad;
import com.jdt.fedlearn.core.loader.common.InferenceData;
import com.jdt.fedlearn.core.loader.common.TrainData;
import com.jdt.fedlearn.core.model.Model;
import com.jdt.fedlearn.core.model.common.CommonModel;
import com.jdt.fedlearn.core.psi.*;
import com.jdt.fedlearn.core.type.MappingType;
import com.jdt.fedlearn.core.type.data.Pair;
import com.jdt.fedlearn.core.type.data.Tuple2;

import java.util.*;
import java.util.stream.Collectors;


/**
 * 通用算法运行函数，本类不维护任何全局状态，各个调用方自行维护，
 */
public class CommonRun {
    private static final Serializer serializer = new JavaSerializer();

    /**
     * @param algorithm    chosen algorithm
     * @param initRequests initial request
     * @param commonIds    公共的id
     * @param rawDataMap   2-d array
     */
    public static Map<ClientInfo, Model> trainNew(Control algorithm, ClientInfo[] clientInfos, List<CommonRequest> initRequests, String[] commonIds, Map<ClientInfo, String[][]> rawDataMap) {
        // model create
        Map<ClientInfo, Model> modelMap = new HashMap<>(); //每个客户端维护自己的，所以此处有n份
        for (ClientInfo client : clientInfos) {
            Model model = CommonModel.constructModel(algorithm.getAlgorithmType());
            modelMap.put(client, model);
        }
        System.out.println("train start:");
        Map<ClientInfo, TrainData> dataMap = new HashMap<>();
        long start = System.currentTimeMillis();
        boolean clientInit = false;
        List<CommonRequest> requests = initRequests;
        while (algorithm.isContinue()) {
            List<CommonResponse> responses = new ArrayList<>();
            if (!clientInit) {
                for (CommonRequest request : initRequests) {
                    TrainInit req = (TrainInit) (request.getBody());
                    Map<String, Object> other = req.getOthers();
                    ClientInfo client = request.getClient();
                    Model model = modelMap.get(client);
                    String[][] rawData = rawDataMap.get(client);
                    int[] testIndex = req.getTestIndex();
                    TrainData trainData = model.trainInit(rawData, commonIds, testIndex, req.getParameter(), req.getFeatureList(), other);
                    dataMap.put(client, trainData);
                    //TODO remove hardcode
                    responses.add(new CommonResponse(request.getClient(), new SingleElement("init_success")));
                }
                clientInit = true;
            } else {
                for (CommonRequest request : requests) {
                    Message message = request.getBody();
                    ClientInfo client = request.getClient();
                    //-----------------client process------------------//
                    Model model = modelMap.get(client);
                    int p = request.getPhase();
                    Message answer = model.train(p, message, dataMap.get(client));
                    ///mock receive message serialize and deserialize
                    String strMessage = serializer.serialize(answer);
                    Message restoreMessage = serializer.deserialize(strMessage);
                    //-----------------client process end--------------//
                    CommonResponse response = new CommonResponse(request.getClient(), restoreMessage);
                    responses.add(response);
                }
            }
            requests = algorithm.control(responses);
        }
        System.out.println("----------------full client end-------------------\n consumed time in seconds:");
        System.out.println((System.currentTimeMillis() - start) / 1000.0);
        return modelMap;
    }


    /**
     * @param algorithm    chosen algorithm
     * @param initRequests initial request
     * @param modelMap     key is client address, value is initialized model
     * @param rawDataMap   2-d array
     * @param commonIds    公共的id
     */
    public static void train(Control algorithm, List<CommonRequest> initRequests,
                             Map<ClientInfo, Model> modelMap, Map<ClientInfo, String[][]> rawDataMap, String[] commonIds) {
        //1.train test split,
        System.out.println("train start:");
        Map<ClientInfo, TrainData> dataMap = new HashMap<>();
        long start = System.currentTimeMillis();
        boolean clientInit = false;
        List<CommonRequest> requests = initRequests;
        while (algorithm.isContinue()) {
            List<CommonResponse> responses = new ArrayList<>();
            if (!clientInit) {
                for (CommonRequest request : initRequests) {
                    TrainInit req = (TrainInit) (request.getBody());
                    Map<String, Object> other = req.getOthers();
                    ClientInfo client = request.getClient();
                    Model model = modelMap.get(client);
                    String[][] rawData = rawDataMap.get(client);
                    ////////////----client code ----------------/////////////
                    int[] testIndex = req.getTestIndex();
                    TrainData trainData = model.trainInit(rawData, commonIds, testIndex, req.getParameter(),
                            req.getFeatureList(), other);
                    dataMap.put(client, trainData);
                    /////////client code end -----------//////////////
                    //TODO remove hardcode
                    responses.add(new CommonResponse(request.getClient(), new SingleElement("init_success")));
                }
                clientInit = true;
            } else {
                for (CommonRequest request : requests) {
                    Message message = request.getBody();
                    ClientInfo client = request.getClient();
                    //-----------------client process------------------//
                    Model model = modelMap.get(client);
                    int p = request.getPhase();
                    Message answer = model.train(p, message, dataMap.get(client));
                    ///mock receive message serialize and deserialize
                    String strMessage = serializer.serialize(answer);
                    //-----------------client process end--------------//
                    Message restoreMessage = serializer.deserialize(strMessage);
                    CommonResponse response = new CommonResponse(request.getClient(), restoreMessage);
                    responses.add(response);
                }
            }
            requests = algorithm.control(responses);
        }
        System.out.println("----------------full client end-------------------\n consumed time in seconds:");
        System.out.println((System.currentTimeMillis() - start) / 1000.0);
    }

    /**
     * @param mappingType  对齐方式
     * @param clientInfos  客户端信息
     * @param trainDataMap 数据
     * @return 对齐结果
     */
    //TODO 根据最新的match修改
    public static Tuple2<MappingReport, String[]> match(MappingType mappingType,
                                             List<ClientInfo> clientInfos, Map<ClientInfo, String[][]> trainDataMap) {
        System.out.println("mappingType:" + mappingType);
        Prepare uidMatch = CommonPrepare.construct(mappingType);
        Map<ClientInfo, PrepareClient> matchClientMap = clientInfos
                .stream()
                .map(x -> new Pair<>(x, CommonPrepare.constructClient(mappingType)))
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue));

        List<CommonRequest> matchRequests = uidMatch.masterInit(clientInfos);
        List<CommonResponse> matchResponses = new ArrayList<>();
        boolean clientInit = false;
        while (uidMatch.isContinue()) {
            matchResponses = new ArrayList<>();
            if (!clientInit) {
                for (CommonRequest request : matchRequests) {
                    MatchInit req = (MatchInit) (request.getBody());
                    Map<String, Object> other = req.getOthers();
                    ClientInfo client = request.getClient();
                    PrepareClient model = matchClientMap.get(client);
                    String[][] rawData = trainDataMap.get(client);
                    String[] tmp = getUidColumn(rawData);
                    Message trainData = model.init(tmp, other);
                    matchResponses.add(new CommonResponse(request.getClient(), trainData));
                }
                clientInit = true;
            } else {
                for (CommonRequest request : matchRequests) {
                    ClientInfo client = request.getClient();
                    PrepareClient uidMatchClient = matchClientMap.get(client);
                    String[][] rawData = trainDataMap.get(client);
                    String[] tmp = getUidColumn(rawData);
                    int p = request.getPhase();
                    Message body = uidMatchClient.client(p, request.getBody(), tmp);
                    String strBody = serializer.serialize(body);
                    Message restoreBody = serializer.deserialize(strBody);
                    matchResponses.add(new CommonResponse(client, restoreBody));
                }
            }
            if (uidMatch.isContinue()) {
                matchRequests = uidMatch.master(matchResponses);
            }
        }
        for (ClientInfo clientInfo : clientInfos) {
            PrepareClient uidMatchClient = matchClientMap.get(clientInfo);
            //TODO 安排路径真实储存下来
            String[] commonIds = uidMatchClient.getCommonIds();
            System.out.println("Id对齐结果： " + Arrays.toString(commonIds));
        }
        // MappingReport来自于master端，commonIDs来自于client端
        Tuple2<MappingReport, String[]> result = new Tuple2<>(uidMatch.postMaster(matchResponses), matchClientMap.get(clientInfos.get(0)).getCommonIds());
        return result;
    }

    private static String[] getUidColumn(String[][] data) {
        return getUidColumn(data, "uid");
    }

    private static String[] getUidColumn(String[][] data, String uidColumnName) {
        String[] inst_id_list = new String[data.length - 1];
        for (int i = 1; i < data.length; i++) {
            inst_id_list[i - 1] = data[i][0];
        }
        return inst_id_list;
    }


    public static PredictRes inference(Control algorithm, List<CommonRequest> initRequests,
                                       Map<ClientInfo, Model> modelMap, Map<ClientInfo, String[][]> rawDataMap) {
        System.out.println("inference start:");
        long start = System.currentTimeMillis();

        //raw data parse
        Map<ClientInfo, InferenceData> inferenceDataMap = new HashMap<>();
        for (Map.Entry<ClientInfo, String[][]> entry : rawDataMap.entrySet()) {
            InferenceData inferenceData = CommonLoad.constructInference(algorithm.getAlgorithmType(), entry.getValue());
            inferenceDataMap.put(entry.getKey(), inferenceData);
        }
        List<CommonRequest> requests = initRequests;
        List<CommonResponse> responses = new ArrayList<>();
        boolean inferenceClientInit = false;
        while (algorithm.isInferenceContinue()) {
            responses = new ArrayList<>();
            if (!inferenceClientInit) {
                for (CommonRequest request : requests) {
                    ClientInfo client = request.getClient();
                    InferenceInit init = (InferenceInit) request.getBody();
                    Model model = modelMap.get(client);
                    Message initRes = model.inferenceInit(init.getUid(), rawDataMap.get(client), init.getOthers());
                    responses.add(new CommonResponse(request.getClient(), initRes));
                }
                inferenceClientInit = true;
            } else {
                for (CommonRequest request : requests) {
                    Message jsonData = request.getBody();
                    ClientInfo client = request.getClient();
                    //-----------------client process------------------//
                    Model model = modelMap.get(client);
                    InferenceData inferenceData = inferenceDataMap.get(client);
                    int p = request.getPhase();
                    Message answer = model.inference(p, jsonData, inferenceData);
                    assert answer != null;
                    //---------------序列化反序列化测试-------------//
                    String strMessage = serializer.serialize(answer);
                    Message restore = serializer.deserialize(strMessage);
                    //-----------------client process end--------------//
                    CommonResponse response = new CommonResponse(request.getClient(), restore);
                    responses.add(response);
                }
            }
            requests = algorithm.inferenceControl(responses);
        }
        System.out.println("----------------client execute end-------------------\n consumed time in seconds:");
        System.out.println((System.currentTimeMillis() - start) / 1000.0);
        return algorithm.postInferenceControl(responses);
    }
}
