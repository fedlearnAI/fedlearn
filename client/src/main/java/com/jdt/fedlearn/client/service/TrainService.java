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
import com.jdt.fedlearn.common.util.FileUtil;
import com.jdt.fedlearn.common.util.JsonUtil;
import com.jdt.fedlearn.common.util.LogUtil;
import com.jdt.fedlearn.common.network.INetWorkService;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.common.TrainInit;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.loader.common.TrainData;
import com.jdt.fedlearn.core.model.*;
import com.jdt.fedlearn.core.model.common.CommonModel;
import com.jdt.fedlearn.core.parameter.SuperParameter;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.common.enums.RunningType;
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
    //从master传过来的是modelId，
    private static ExecutorService pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private static final Map<String, TrainData> dataMap = new ConcurrentHashMap<>();
    //    public static Map<String, SubRequest> requestQueue = new ConcurrentSkipListMap<>();
    public static Map<String, String> responseQueue = new ConcurrentSkipListMap<>();
    //TODO 此处需要入参队列和出餐队列
    //TODO 文件初始化即加载，且不区分算法
    private static INetWorkService netWorkService = INetWorkService.getNetWorkService();

    /**
     * @param trainRequest 服务端请求的数据
     * @return 训练中间结果
     * 主逻辑只负责处理训练迭代过程，其他
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
                //构造model
                Model localModel = CommonModel.constructModel(algorithm);
                modelCache.put(modelToken, localModel);
                //构造trainData
                Message message = Constant.serializer.deserialize(parameterData);
                TrainInit trainInit = (TrainInit) message;
                Features features = trainInit.getFeatureList();
                if (RequestCheck.needBelongCoordinator(features, algorithm, remoteIP)) {
                    logger.error("init error, 协调端需要与有y值的客户端部署在同一方");
                    return "init_failed, 协调端需要与有y值的客户端部署在同一方";
                }
                SuperParameter superParameter = trainInit.getParameter();
                String matchToken = trainInit.getMatchId();
                // 此时是在client端，解密的状态
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
                // TODO 此时mappingResult应该是在client端，解密的状态
                int[] testIndex = trainInit.getTestIndex();
                TrainData trainData = localModel.trainInit(trainPara, mappingResult, testIndex, superParameter, features, others);
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

        //训练中的模型直接加载
        Model model = modelCache.get(modelToken);
        TrainData trainData = dataMap.get(modelToken);

        String result = "";
        logger.info("status:" + status);
        //收到暂停或终止的信号模型保存本地
        if (status.equals(RunningType.SUSPEND)) {
            result = RunningType.SUSPEND.getRunningType();
        } else if (status.equals(RunningType.STOP)) {
            //停止
            result = RunningType.STOP.getRunningType();
            logger.info("stop is success");
        } else if (status.equals(RunningType.COMPLETE)) {
            //训练完成
            modelCache.forceToFile(modelToken);
            modelCache.slim(modelToken);
            dataMap.remove(modelToken);
            result = RunningType.COMPLETE.getRunningType();
            logger.info("client train is complete!!!");
        } else {
            // TODO add parameter check
            //superParameter 是人为指定的超参数，parameters 是训练过程中的迭代参数
            logger.info("trainData: " + trainData);
            // 提交到子线程执行
            if (trainRequest.isSync()) {
                Message restoreMessage = Constant.serializer.deserialize(parameterData);
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
     * @description:将训练完成的数据返回给随机的master
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
        /*获取随机的master*/
        String queryKey = JdChainConstant.INVOKE_RANDOM_TRAINING + JdChainConstant.SEPARATOR + modelToken + JdChainConstant.SEPARATOR + JdChainConstant.SERVER;
        TypedKVEntry typedKVEntry = JdChainUtils.queryByChaincode(queryKey);
        if (typedKVEntry != null) {
            Map<String, Object> map = JsonUtil.json2Object((String) typedKVEntry.getValue(), Map.class);
            String server = JsonUtil.json2Object(map.get(SERVER).toString(), Map.class).get(IDENTITY).toString();
            String url = AppConstant.HTTP_PREFIX + server + JdChainConstant.API_RANDOM_SERVER;
//            String url = "http://127.0.0.1:8092/api/train/random";
            netWorkService.sendAndRecv(url, modelMap);
            logger.info("random server is {} , this key is {}", url, modelToken + "-" + trainRequest.getPhase());
        } else {//找不到选举的master 默认一个或者直接抛出异常
            throw new ServerNotFoundException("master server not found！");
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
            // 释放内存, 清除存放的异步结果
            TrainService.responseQueue.remove(stamp);
        } else {
            modelMap.put(ResponseConstant.CODE, ResponseConstant.DOING_CODE);
            modelMap.put(ResponseConstant.STATUS, ResponseConstant.DOING);
        }
        return modelMap;
    }


}
