/* Copyright 2020 The FedLearn Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.jdt.fedlearn.client.service;

import com.jd.blockchain.ledger.TypedKVEntry;
import com.jdt.fedlearn.client.cache.TrainDataCache;
import com.jdt.fedlearn.client.cache.ModelCache;
import com.jdt.fedlearn.client.constant.Constant;
import com.jdt.fedlearn.client.dao.CsvReader;
import com.jdt.fedlearn.client.dao.IdMatchProcessor;
import com.jdt.fedlearn.client.entity.train.QueryProgress;
import com.jdt.fedlearn.client.entity.train.TrainRequest;
import com.jdt.fedlearn.client.exception.ServerNotFoundException;
import com.jdt.fedlearn.client.util.JdChainUtils;
import com.jdt.fedlearn.client.multi.TrainProcess;
import com.jdt.fedlearn.client.util.*;
import com.jdt.fedlearn.common.constant.AppConstant;
import com.jdt.fedlearn.common.constant.JdChainConstant;
import com.jdt.fedlearn.common.constant.ResponseConstant;
import com.jdt.fedlearn.tools.FileUtil;
import com.jdt.fedlearn.tools.LogUtil;
import com.jdt.fedlearn.tools.serializer.JsonUtil;
import com.jdt.fedlearn.tools.network.INetWorkService;
import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.core.entity.common.TrainInit;
import com.jdt.fedlearn.common.entity.core.feature.Features;
import com.jdt.fedlearn.core.loader.common.TrainData;
import com.jdt.fedlearn.core.model.*;
import com.jdt.fedlearn.core.model.common.CommonModel;
import com.jdt.fedlearn.core.parameter.HyperParameter;
import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;
import com.jdt.fedlearn.common.enums.RunningType;
import com.jdt.fedlearn.tools.serializer.KryoUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;



/**
 *
 */
public class TrainService {
    private static final Logger logger = LoggerFactory.getLogger(TrainService.class);
    //???master???????????????modelId???
    private static ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private static final Map<String, TrainData> dataMap = new ConcurrentHashMap<>();
    //    public static Map<String, SubRequest> requestQueue = new ConcurrentSkipListMap<>();
    public static Map<String, String> responseQueue = new ConcurrentSkipListMap<>();
    //TODO ???????????????????????????????????????
    //TODO ?????????????????????????????????????????????
    private static INetWorkService netWorkService = INetWorkService.getNetWorkService();

    /**
     * @param trainRequest ????????????????????????
     * @return ??????????????????
     * ???????????????????????????????????????????????????
     */
    public String train(TrainRequest trainRequest, String remoteIP) {
        String modelToken = trainRequest.getModelToken();
        int phase = trainRequest.getPhase();
        RunningType status = trainRequest.getStatus();
        AlgorithmType algorithm = trainRequest.getAlgorithm();
        String parameterData = trainRequest.getData();
        logger.info("parameterData:" + LogUtil.logLine(parameterData));
        boolean flag = ConfigUtil.getJdChainAvailable();
        ModelCache modelCache = ModelCache.getInstance();
        if (!modelCache.contain(modelToken) && !dataMap.containsKey(modelToken)) {
            try {
                //??????model
                Model localModel = CommonModel.constructModel(algorithm);
                modelCache.put(modelToken, localModel);
                //??????trainData
                Message message = KryoUtil.readFromString(parameterData);
                TrainInit trainInit = (TrainInit) message;
                Features features = trainInit.getFeatureList();
                if (RequestCheck.needBelongCoordinator(features, algorithm, remoteIP)) {
                    logger.error("init error, ?????????????????????y?????????????????????????????????");
                    return "init_failed, ?????????????????????y?????????????????????????????????";
                }
                HyperParameter hyperParameter = trainInit.getParameter();
                String matchToken = trainInit.getMatchId();
                // ????????????client?????????????????????
                String[] mappingResult = IdMatchProcessor.loadResult(matchToken);
                String dataset = trainRequest.getDataset();
                logger.info("dataset:" + dataset);
                String[][] trainPara = null;
                if (FileUtil.isFile(dataset)) {
                    CsvReader csvReader = new CsvReader();
                    trainPara = csvReader.loadData(dataset);
                } else {
                    trainPara = TrainDataCache.getTrainData(modelToken, dataset);
                }

                Map<String, Object> others = trainInit.getOthers();
                List<AlgorithmType> needDistributedKeys = Arrays.asList(AlgorithmType.MixGBoost, AlgorithmType.LinearRegression);
                if (needDistributedKeys.contains(algorithm)) {
                    String keyPath = ConfigUtil.getClientConfig().getModelDir();
                    String pubkeyContent = FileUtil.loadClassFromFile(keyPath + Constant.PUB_KEY);
                    String prikeyContent = FileUtil.loadClassFromFile(keyPath + Constant.PRIV_KEY);
                    others.put("pubKeyStr", pubkeyContent);
                    others.put("privKeyStr", prikeyContent);
                }
                // TODO ??????mappingResult????????????client?????????????????????
                int[] testIndex = trainInit.getTestIndex();
                TrainData trainData = localModel.trainInit(trainPara, mappingResult, testIndex, hyperParameter, features, others);
                dataMap.put(modelToken, trainData);
                String data = AppConstant.INIT_SUCCESS;
                if (flag) {
                    post2Server(data, trainRequest);
                }
                return data;
            } catch (IOException e) {
                logger.error("init error: ", e);
                return AppConstant.INIT_FAILED;
            }
        }

        //??????????????????????????????
        Model model = modelCache.get(modelToken);
        TrainData trainData = dataMap.get(modelToken);
        String result = "";
        logger.info("status:" + status);
        //????????????????????????????????????????????????
        if (status.equals(RunningType.SUSPEND)) {
            result = RunningType.SUSPEND.getRunningType();
        } else if (status.equals(RunningType.STOP)) {
            //??????
            result = RunningType.STOP.getRunningType();
            logger.info("stop is success");
        } else if (status.equals(RunningType.COMPLETE)) {
            //????????????
            modelCache.forceToFile(modelToken);
            modelCache.slim(modelToken);
            dataMap.remove(modelToken);
            result = RunningType.COMPLETE.getRunningType();
            logger.info("client train is complete!!!");
        } else {
            // TODO add parameter check
            //HyperParameter ??????????????????????????????parameters ?????????????????????????????????
            logger.info("trainData: " + trainData);
            // ????????????????????????
            if (trainRequest.isSync()) {
                Message restoreMessage = KryoUtil.readFromString(parameterData);
                Message data = null;
                if (model != null) {
                    data = model.train(phase, restoreMessage, trainData);
                }
                String strMessage = Constant.serializer.serialize(data);
                if (flag) {
                    post2Server(strMessage, trainRequest);
                }
                return strMessage;
            }
            String stamp = UUID.randomUUID().toString();
            submit(model, stamp, modelToken, phase, parameterData, trainData);
            result = "{\"stamp\": \"" + stamp + "\"}";
            logger.info("stamp is:" + result);
        }
        return result;
    }

    /**
     * @param data
     * @param trainRequest
     * @className TrainService
     * @description:??????????????????????????????????????????master
     * @return: void
     * @author: geyan29
     * @date: 2021/01/11 10:13
     **/
    private static final String SERVER = "server";
    private static final String IDENTITY = "identity";
    private static final String DATA = "data";
    private static final String PHASE = "phase";
    private static final String MODEL_TOKEN = "modelToken";
    private static final String CLIENT_INFO = "clientInfo";
    private static final String REQUEST_NUM = "reqNum";

    private void post2Server(String data, TrainRequest trainRequest) {
        Map<String, Object> modelMap = new HashMap<>();
        String modelToken = trainRequest.getModelToken();
        modelMap.put(DATA, data);
        modelMap.put(PHASE, trainRequest.getPhase());
        modelMap.put(MODEL_TOKEN, trainRequest.getModelToken());
        modelMap.put(CLIENT_INFO, trainRequest.getClientInfo());
        modelMap.put(REQUEST_NUM, trainRequest.getReqNum());
        /*???????????????master*/
        String queryKey = JdChainConstant.INVOKE_RANDOM_TRAINING + JdChainConstant.SEPARATOR + modelToken + JdChainConstant.SEPARATOR + JdChainConstant.SERVER;
        TypedKVEntry typedKVEntry = JdChainUtils.queryByChaincode(queryKey);
        if (typedKVEntry != null) {
            Map<String, Object> map = JsonUtil.json2Object((String) typedKVEntry.getValue(), Map.class);
            String server = JsonUtil.json2Object(map.get(SERVER).toString(), Map.class).get(IDENTITY).toString();
            String url = AppConstant.HTTP_PREFIX + server + JdChainConstant.API_RANDOM_SERVER;
//            String url = "http://127.0.0.1:8092/api/train/random";
            netWorkService.sendAndRecv(url, modelMap);
            logger.info("random server is {} , this key is {}", url, modelToken + "-" + trainRequest.getPhase());
        } else {//??????????????????master ????????????????????????????????????
            throw new ServerNotFoundException("master server not found???");
        }
    }

    public static void submit(Model model, String stamp, String modelToken, int phase, String parameterData, TrainData trainData) {
        logger.info("global parameters stamp:" + stamp + " modelToken:" + modelToken + " phase: " + phase);
        try {
            pool.execute(new TrainProcess(model, stamp, modelToken, phase, parameterData, trainData));
        } catch (Exception e) {
            logger.error("submit error: ", e);
        }
    }


    public Map<String, Object> queryProgress(QueryProgress query) {
        Map<String, Object> modelMap = new HashMap<>();
        String stamp = query.getStamp();
        if (TrainService.responseQueue.containsKey(stamp)) {
            modelMap.put(ResponseConstant.CODE, ResponseConstant.SUCCESS_CODE);
            modelMap.put(ResponseConstant.STATUS, ResponseConstant.COMPLETE);
            modelMap.put(ResponseConstant.DATA, TrainService.responseQueue.get(stamp));
            // ????????????, ???????????????????????????
            TrainService.responseQueue.remove(stamp);
        } else {
            modelMap.put(ResponseConstant.CODE, ResponseConstant.DOING_CODE);
            modelMap.put(ResponseConstant.STATUS, ResponseConstant.DOING);
        }
        return modelMap;
    }


}
