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
package com.jdt.fedlearn.worker.service;

import com.google.common.collect.Maps;
import com.jdt.fedlearn.client.cache.ModelCache;
import com.jdt.fedlearn.client.cache.TrainDataCache;
import com.jdt.fedlearn.client.dao.*;
import com.jdt.fedlearn.client.entity.source.DataSourceConfig;
import com.jdt.fedlearn.client.type.SourceType;
import com.jdt.fedlearn.client.util.ConfigUtil;
import com.jdt.fedlearn.common.constant.CacheConstant;
import com.jdt.fedlearn.common.network.INetWorkService;
import com.jdt.fedlearn.core.entity.distributed.InitResult;
import com.jdt.fedlearn.core.loader.common.TrainData;
import com.jdt.fedlearn.worker.cache.ManagerCache;
import com.jdt.fedlearn.worker.constant.Constant;
import com.jdt.fedlearn.worker.entity.train.QueryProgress;
import com.jdt.fedlearn.worker.multi.TrainProcess;
import com.jdt.fedlearn.common.enums.RunningType;
import com.jdt.fedlearn.common.constant.AppConstant;
import com.jdt.fedlearn.common.util.*;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.common.TrainInit;
import com.jdt.fedlearn.core.model.Model;
import com.jdt.fedlearn.core.model.common.CommonModel;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.worker.util.ExceptionUtil;
import com.jdt.fedlearn.common.constant.ResponseConstant;
import com.jdt.fedlearn.common.entity.CommonResultStatus;
import com.jdt.fedlearn.common.entity.TrainRequest;
import com.jdt.fedlearn.common.enums.WorkerCommandEnum;
import com.jdt.fedlearn.common.util.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;


/**
 *
 */
public class TrainService {
    private static final Logger logger = LoggerFactory.getLogger(TrainService.class);
    public static Map<String, String> responseQueue = new ConcurrentSkipListMap<>();
    public static Map<String, String> modelCache = new ConcurrentSkipListMap<>();
    public static Map<String, String> trainDataCache = new ConcurrentSkipListMap<>();
    private INetWorkService netWorkService = INetWorkService.getNetWorkService();

    /**
     * @param trainRequest 服务端请求的数据
     * @return 训练中间结果
     * 主逻辑只负责处理训练迭代过程，其他
     */
    public String train(TrainRequest trainRequest) {
        String modelToken = trainRequest.getModelToken();
        String requestId = trainRequest.getRequestId();
        int phase = trainRequest.getPhase();
        RunningType status = trainRequest.getStatus();
        AlgorithmType algorithm = trainRequest.getAlgorithm();
        String parameterData = trainRequest.getData();
        String dataset = trainRequest.getDataset();
        logger.info("parameterData:" + LogUtil.logLine(parameterData));

        String modelAddressKey = CacheConstant.getModelAddressKey(modelToken, requestId);
        String modelAddress = ManagerCache.getCache(AppConstant.MODEL_ADDRESS_CACHE, modelAddressKey);
        if (modelAddress == null) {
            return initModel(requestId, algorithm, parameterData, modelToken, dataset);
        }
        Map<String, String> modelAndDataMap = getModelAndData(modelAddress, modelToken, requestId);
        String modelStr = modelAndDataMap.get(AppConstant.MODEL_KEY);
        String dataStr = modelAndDataMap.get(AppConstant.TRAIN_DATA_KEY);
        String result = "";
        //训练中的模型直接加载
        Model model = null;
        try {
            model = (Model) SerializationUtils.deserialize(modelStr);
            logger.info("modelAddressKey={}, phase={}, status={}", modelAddressKey, phase, status);
            String stamp = UUID.randomUUID().toString();
            if (status.equals(RunningType.COMPLETE)) {
                result = RunningType.COMPLETE.getRunningType();
                ModelDao.saveModel(modelToken, model);
                ModelCache modelCache = ModelCache.getInstance();
                modelCache.put(modelToken, model);
                /* 训练结束清除所有缓存*/
                clearManagerCache(modelAddressKey, modelToken);
                logger.info("client train is complete!!!");
                return result;
            } else {
                // 提交到子线程执行
                if (trainRequest.isSync()) {
                    Message restoreMessage = Constant.serializer.deserialize(parameterData);
                    Message data = model.train(phase, restoreMessage, (TrainData) SerializationUtils.deserialize(dataStr));
                    String strMessage = Constant.serializer.serialize(data);
                    return strMessage;
                }
                TrainProcess trainProcess = new TrainProcess(model, stamp, modelToken, phase, parameterData, (TrainData) SerializationUtils.deserialize(dataStr), requestId);
                trainProcess.run();
                //更新model
                String modelString = SerializationUtils.serialize(model);
                updateModel(modelToken, requestId, modelString);
            }
            String trainResultAddressKey = CacheConstant.getTrainResultAddressKey(stamp);
            ManagerCache.putCache(AppConstant.RESULT_ADDRESS_CACHE, trainResultAddressKey, IpAddressUtil.getLocalHostLANAddress().getHostAddress() + AppConstant.COLON + ConfigUtil.getPortElseDefault());
            // 最后返回异步的stamp
            result = "{\"stamp\": \"" + stamp + "\"}";
            logger.info("stamp is:" + result);
        } catch (ClassNotFoundException | IOException e) {
            logger.error("train error!", e);
        }
        return result;
    }

    private void updateModel(String modelToken, String requestId, String modelString) {
        String modelKey = CacheConstant.getModelAddressKey(modelToken, requestId);
        String modelAddress = ManagerCache.getCache(AppConstant.MODEL_ADDRESS_CACHE, modelKey);
        String address = IpAddressUtil.getLocalHostLANAddress().getHostAddress() + AppConstant.COLON + ConfigUtil.getPortElseDefault();
        // 判断是结果地址是不是在本地
        if (StringUtils.equals(modelAddress, address)) {
            TrainService.updateLocalModel(modelKey, modelString);
        } else {
            // 调用http 接口更新
            Map<String, String> params = new HashMap<>(8);
            params.put(AppConstant.MODEL_UPDATE_KEY, modelKey);
            params.put(AppConstant.MODEL_UPDATE_VALUE, modelString);
            netWorkService.sendAndRecv(AppConstant.HTTP_PREFIX + modelAddress + AppConstant.SLASH + WorkerCommandEnum.API_MODEL_UPDATE.getCode(), params);
        }
    }

    /**
     * 训练结束清除manager缓存
     *
     * @param modelKey
     * @param modelToken
     */
    private void clearManagerCache(String modelKey, String modelToken) {
        /* 删除缓存在manager的model*/
        ManagerCache.delCache(AppConstant.MODEL_ADDRESS_CACHE, modelKey);
        ManagerCache.delCache(AppConstant.MODEL_COUNT_CACHE, CacheConstant.getTreeKey(modelToken));
        modelCache.remove(modelKey);
        trainDataCache.remove(modelKey);
    }

    /**
     * 异步查询训练结果
     *
     * @param query
     * @return
     */
    public Map<String, Object> queryProgress(QueryProgress query) {
        Map<String, Object> modelMap = new HashMap<>();
        String stamp = query.getStamp();
        try {
            String trainResultAddressKey = CacheConstant.getTrainResultAddressKey(stamp);
            String resultAddress = ManagerCache.getCache(AppConstant.RESULT_ADDRESS_CACHE, trainResultAddressKey);
            // 删除缓存
            ManagerCache.delCache(AppConstant.RESULT_ADDRESS_CACHE, trainResultAddressKey);
            String address = IpAddressUtil.getLocalHostLANAddress().getHostAddress() + AppConstant.COLON + ConfigUtil.getPortElseDefault();
            // 判断是结果地址是不是在本地
            String trainResult = null;
            if (StringUtils.equals(resultAddress, address)) {
                trainResult = TrainService.getTrainResult(stamp);
            } else {
                Map<String, Object> param = Maps.newHashMap();
                param.put("stamp", stamp);
                // 调用http 接口查询
                String remoteTrainResult = netWorkService.sendAndRecv(AppConstant.HTTP_PREFIX + resultAddress + AppConstant.SLASH + WorkerCommandEnum.API_TRAIN_RESULT_QUERY.getCode(), param);
                String finalResult = GZIPCompressUtil.unCompress(remoteTrainResult);
                CommonResultStatus commonResultStatus = JsonUtil.json2Object(finalResult, CommonResultStatus.class);
                trainResult = (String) commonResultStatus.getData().get(ResponseConstant.DATA);
            }
            if (trainResult != null) {
                modelMap.put(ResponseConstant.CODE, ResponseConstant.SUCCESS_CODE);
                modelMap.put(ResponseConstant.STATUS, ResponseConstant.COMPLETE);
                // todo
                modelMap.put(ResponseConstant.DATA, trainResult);
            } else {
                modelMap.put(ResponseConstant.CODE, ResponseConstant.DOING_CODE);
                modelMap.put(ResponseConstant.STATUS, ResponseConstant.DOING);
            }
        } catch (Exception e) {
            logger.error("异步获取信息异常", e);
        }
        return modelMap;
    }

    /**
     * 获取训练结果方法
     *
     * @param stamp
     * @return
     */
    public static String getTrainResult(String stamp) {
        if (StringUtils.isBlank(stamp)) {
            return StringUtils.EMPTY;
        }
        String trainResultKey = CacheConstant.getTrainResultKey(stamp);
        String trainResult = responseQueue.get(trainResultKey);
        if (StringUtils.isBlank(trainResult)) {
            return StringUtils.EMPTY;
        }
        // 查到结果
        responseQueue.remove(trainResultKey);
        return trainResult;
    }

    /***
     * @description: 第一次调用时处理model
     * @param algorithm
     * @param parameterData
     * @param modelToken
     * @return: java.lang.String
     * @author: geyan29
     * @date: 2021/4/28 6:49 下午
     */
    private String initModel(String requestId, AlgorithmType algorithm, String parameterData, String modelToken, String dataset) {
        try {
            Message message = Constant.serializer.deserialize(parameterData);
            TrainInit trainInit = (TrainInit) message;
            String matchToken = trainInit.getMatchId();
            String[] mapResOri = IdMatchProcessor.loadResult(matchToken);
            Model localModel = CommonModel.constructModel(algorithm);
            String[][] rawData = getRawData(localModel, requestId, dataset, trainInit, mapResOri);
            InitResult initResult = localModel.initMap(requestId, rawData, trainInit, mapResOri);
            TrainData trainData = initResult.getTrainData();
            Model model = initResult.getModel();
            List<String> modelIDs = initResult.getModelIDs();
            String trainDataStr = SerializationUtils.serialize(trainData);
            String modelStr = SerializationUtils.serialize(model);

            /* 将返回的model和trainData保存在本地 并通知manager保存结果的地址*/
//            String modelAndTrainDataKey = CacheConstant.getModelAndTrainDataKey(modelToken,requestId);
            String modelAddressKey = CacheConstant.getModelAddressKey(modelToken, requestId);
            modelCache.put(modelAddressKey, modelStr);
            trainDataCache.put(modelAddressKey, trainDataStr);

            String address = IpAddressUtil.getLocalHostLANAddress().getHostAddress() + AppConstant.COLON + ConfigUtil.getPortElseDefault();
            //保存model及trainData的worker
            ManagerCache.putCache(AppConstant.MODEL_ADDRESS_CACHE, modelAddressKey, address);

            //保存拆分的份数
            String treeKey = CacheConstant.getTreeKey(modelToken);
            ManagerCache.putCache(AppConstant.MODEL_COUNT_CACHE, treeKey, JsonUtil.object2json(modelIDs));

            return AppConstant.INIT_SUCCESS;
        } catch (Exception e) {
            logger.error(ExceptionUtil.getExInfo(e));
            return AppConstant.INIT_FAILED;
        }
    }

    private String[][] getRawData(Model localModel, String requestId, String dataset, TrainInit trainInit, String[] mapResOri) throws IOException {
        Map<Long, String> idMap = new HashMap<>();
        for (int i = 0; i < mapResOri.length; i++) {
            idMap.put((long) i, mapResOri[i]);
        }
        DataReader reader = getReader(dataset);
        String[][] newIndexId = reader.readDataIndex(dataset, idMap);
        String[][] sortedIndexId = Arrays.stream(newIndexId).parallel().sorted(Comparator.comparing(x -> x[0])).toArray(String[][]::new);
        List<Integer> sortedIndexList = Arrays.stream(sortedIndexId).parallel().map(x -> Integer.valueOf(x[1])).collect(Collectors.toList());
        //下面改到core
        ArrayList<Integer> sampleData = localModel.dataIdList(requestId, trainInit, sortedIndexList);
        //读取采样的数据集
        return reader.readDataLine(dataset, sampleData);
    }


    /**
     * @param dataset
     * @description: 根据文件名获取reader
     * @return: com.jdd.ml.federated.client.dao.DataReader
     * @author: geyan29
     * @date: 2021/5/18 4:39 下午
     */
    private DataReader getReader(String dataset) {
        List<DataSourceConfig> trainConfigs = TrainDataCache.dataSourceMap.get(TrainDataCache.TRAIN_DATA_SOURCE);
        DataSourceConfig trainConfig = null;
        for (DataSourceConfig config : trainConfigs) {
            if (config.getDataName().equals(dataset)) {
                trainConfig = config;
            }
        }
        if (trainConfig != null) {
            SourceType sourceType = trainConfig.getSourceType();
            DataReader reader;
            if (SourceType.CSV.equals(sourceType)) {
                reader = new CsvReader(trainConfig);
            } else if (SourceType.HDFS.equals(sourceType)) {
                reader = new HdfsReader(trainConfig);
            } else {
                reader = new CsvReader(trainConfig);
            }
            return reader;
        } else {
            throw new RuntimeException("getReader error,trainConfig is null!");
        }
    }

    /**
     * 获取worker保存的 model和trainData
     *
     * @param modelAddress
     * @param modelToken
     * @param requestId
     * @return
     */
    private Map<String, String> getModelAndData(String modelAddress, String modelToken, String requestId) {
        Map<String, String> result;
        String modelAndTrainDataKey = CacheConstant.getModelAddressKey(modelToken, requestId);
        String address = IpAddressUtil.getLocalHostLANAddress().getHostAddress() + AppConstant.COLON + ConfigUtil.getPortElseDefault();
        // 判断是结果地址是不是在本地
        if (StringUtils.equals(modelAddress, address)) {
            result = TrainService.getLocalModelAndData(modelAndTrainDataKey);
        } else {
            // 调用http 接口查询
            String remoteModelResult = netWorkService.sendAndRecv(AppConstant.HTTP_PREFIX + modelAddress + AppConstant.SLASH + WorkerCommandEnum.API_MODEL_DATA_QUERY.getCode(), modelAndTrainDataKey);
            String finalResult = GZIPCompressUtil.unCompress(remoteModelResult);
            CommonResultStatus commonResultStatus = JsonUtil.json2Object(finalResult, CommonResultStatus.class);
            result = (Map<String, String>) commonResultStatus.getData().get(ResponseConstant.DATA);

        }
        return result;
    }

    public static String getLocalModel(String key) {
        String modelStr = modelCache.get(key);
        return modelStr;
    }

    public static String getLocalTrainData(String key) {
        String trainDataStr = trainDataCache.get(key);
        return trainDataStr;
    }

    public static void updateLocalModel(String key, String value) {
        modelCache.put(key, value);
    }

    public static void updateLocalTrainData(String key, String value) {
        trainDataCache.put(key, value);
    }

    public static Map<String, String> getLocalModelAndData(String key) {
        String modelStr = modelCache.get(key);
        String trainDataStr = trainDataCache.get(key);
        Map<String, String> result = new HashMap<>(16);
        result.put(AppConstant.MODEL_KEY, modelStr);
        result.put(AppConstant.TRAIN_DATA_KEY, trainDataStr);
        return result;
    }
}
