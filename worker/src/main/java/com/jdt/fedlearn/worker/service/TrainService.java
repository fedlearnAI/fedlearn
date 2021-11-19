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

import com.carrotsearch.sizeof.RamUsageEstimator;
import com.google.common.collect.Maps;
import com.jdt.fedlearn.client.cache.TrainDataCache;
import com.jdt.fedlearn.client.dao.*;
import com.jdt.fedlearn.client.entity.source.DataSourceConfig;
import com.jdt.fedlearn.client.type.SourceType;
import com.jdt.fedlearn.client.util.ConfigUtil;
import com.jdt.fedlearn.client.util.PacketUtil;
import com.jdt.fedlearn.common.constant.CacheConstant;
import com.jdt.fedlearn.tools.network.INetWorkService;
import com.jdt.fedlearn.core.entity.distributed.InitResult;
import com.jdt.fedlearn.core.loader.common.TrainData;
import com.jdt.fedlearn.core.model.DistributedFederatedGBModel;
import com.jdt.fedlearn.core.model.DistributedRandomForestModel;
import com.jdt.fedlearn.tools.serializer.JsonUtil;
import com.jdt.fedlearn.tools.serializer.KryoUtil;
import com.jdt.fedlearn.worker.cache.ManagerCache;
import com.jdt.fedlearn.worker.constant.Constant;
import com.jdt.fedlearn.worker.entity.train.QueryProgress;
import com.jdt.fedlearn.worker.multi.TrainProcess;
import com.jdt.fedlearn.common.enums.RunningType;
import com.jdt.fedlearn.common.constant.AppConstant;
import com.jdt.fedlearn.tools.*;
import com.jdt.fedlearn.common.entity.core.Message;
import com.jdt.fedlearn.core.entity.common.TrainInit;
import com.jdt.fedlearn.core.model.Model;
import com.jdt.fedlearn.core.model.common.CommonModel;
import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;
import com.jdt.fedlearn.worker.util.ExceptionUtil;
import com.jdt.fedlearn.common.constant.ResponseConstant;
import com.jdt.fedlearn.common.entity.CommonResultStatus;
import com.jdt.fedlearn.common.entity.TrainRequest;
import com.jdt.fedlearn.common.enums.WorkerCommandEnum;
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
    public static Map<String, Model> modelCache = new ConcurrentSkipListMap<>();
    public static Map<String, TrainData> trainDataCache = new ConcurrentSkipListMap<>();
    public static Map<String, Message> subMessageCache = new ConcurrentSkipListMap<>();
    private static INetWorkService netWorkService = INetWorkService.getNetWorkService();

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
        if (RunningType.COMPLETE.equals(status)) {
            logger.info("client train is complete!!!");
            return RunningType.COMPLETE.getRunningType();
        }
        AlgorithmType algorithm = trainRequest.getAlgorithm();
        String dataset = trainRequest.getDataset();
        String parameterData;
        modelCache.keySet().stream().forEach(key -> logger.info("modelCache的key:{}", key));
        trainDataCache.keySet().stream().forEach(key -> logger.info("trainDataCache的key:{}", key));
        logger.info("modelCache size:{},length:{}", RamUsageEstimator.humanSizeOf(modelCache), modelCache.size());
        logger.info("trainData size:{},length:{}", RamUsageEstimator.humanSizeOf(trainDataCache), trainDataCache.size());
        if (algorithm.equals(AlgorithmType.DistributedFederatedGB) && phase != 0) {
            String phaseString = String.valueOf(phase);
            String messageAddressKey = CacheConstant.getModelAddressKey(modelToken, phaseString);
            String messageAddress = ManagerCache.getCache(AppConstant.MODEL_MESSAGE_CACHE, messageAddressKey);
            parameterData = getMessageData(messageAddress, modelToken, phaseString);
        } else {
            parameterData = trainRequest.getData();
        }
        logger.info("parameterData:" + LogUtil.logLine(parameterData));
        String modelAddressKey = CacheConstant.getModelAddressKey(modelToken, requestId);
        String modelAddress = ManagerCache.getCache(AppConstant.MODEL_ADDRESS_CACHE, modelAddressKey);
        if (modelAddress == null) {
            return initModel(requestId, algorithm, parameterData, modelToken, dataset, phase);
        }
        Map<String, Object> modelAndDataMap = getModelAndData(modelAddress, modelToken, requestId);
        Model model = (Model) modelAndDataMap.get(AppConstant.MODEL_KEY);
        logger.info("缓存中序列化的单个model大小：{}， phase：{}", RamUsageEstimator.humanSizeOf(model), phase);
        TrainData trainData = (TrainData) modelAndDataMap.get(AppConstant.TRAIN_DATA_KEY);
        logger.info("缓存中序列化的单个trainData大小：{}, phase:{}", RamUsageEstimator.humanSizeOf(trainData), phase);
        String result = "";
        //训练中的模型直接加载
        try {
            logger.info("modelAddressKey={}, phase={}, status={}", modelAddressKey, phase, status);
            String stamp = UUID.randomUUID().toString();
            // 提交到子线程执行
            if (trainRequest.isSync()) {
                Message message = subTrain(model, phase, modelToken, parameterData, trainData);
                String strMessage = Constant.serializer.serialize(message);
                return strMessage;
            }
            TrainProcess trainProcess = new TrainProcess(model, stamp, modelToken, phase, parameterData, trainData, requestId);
            trainProcess.run();
            //更新model
            updateModel(modelToken, requestId, model);
            String trainResultAddressKey = CacheConstant.getTrainResultAddressKey(stamp);
            ManagerCache.putCache(AppConstant.RESULT_ADDRESS_CACHE, trainResultAddressKey, IpAddressUtil.getLocalHostLANAddress().getHostAddress() + AppConstant.COLON + ConfigUtil.getPortElseDefault());
            // 最后返回异步的stamp
            result = "{\"stamp\": \"" + stamp + "\"}";
            logger.info("stamp is:" + result);
            RuntimeStatusService.releaseMem(modelToken, phase);
        } finally {
            logger.info("train执行结束，token:{},phase:{}", modelToken, phase);
        }
        return result;
    }

    private void updateModel(String modelToken, String requestId, Model model) {
        String modelKey = CacheConstant.getModelAddressKey(modelToken, requestId);
        String modelAddress = ManagerCache.getCache(AppConstant.MODEL_ADDRESS_CACHE, modelKey);
        String address = IpAddressUtil.getLocalHostLANAddress().getHostAddress() + AppConstant.COLON + ConfigUtil.getPortElseDefault();
        // 判断是结果地址是不是在本地
        if (StringUtils.equals(modelAddress, address)) {
            TrainService.updateLocalModel(modelKey, model);
        } else {
            // 调用http 接口更新
            Map<String, String> params = new HashMap<>(8);
            params.put(AppConstant.MODEL_UPDATE_KEY, modelKey);
            String modelString = KryoUtil.writeToString(model);
            params.put(AppConstant.MODEL_UPDATE_VALUE, modelString);
            logger.info("训练过程--------更新model地址；{},集合大小：{}，model大小：{},model长度：{}", modelAddress, RamUsageEstimator.humanSizeOf(params), RamUsageEstimator.humanSizeOf(modelString), modelString.length());
            netWorkService.sendAndRecv(AppConstant.HTTP_PREFIX + modelAddress + AppConstant.SLASH + WorkerCommandEnum.API_MODEL_UPDATE.getCode(), params);
        }
    }

    /**
     * 训练结束清除manager缓存
     *
     * @param modelToken
     */
    public static void clearCache(String modelToken) {
        RuntimeStatusService.cleanMapByToken(modelToken);
        clearInitCache(modelToken);
    }

    /**
     * 训练结束清楚init缓存的model和trainData
     *
     * @param modelToken 训练的modelToken
     */
    public static void clearInitCache(String modelToken) {
        modelCache.entrySet().parallelStream().filter(e -> e.getKey().contains(modelToken)).forEach(e -> modelCache.remove(e.getKey()));
        trainDataCache.entrySet().parallelStream().filter(e -> e.getKey().contains(modelToken)).forEach(e -> trainDataCache.remove(e.getKey()));
        subMessageCache.entrySet().parallelStream().filter(e -> e.getKey().contains(modelToken)).forEach(e -> subMessageCache.remove(e.getKey()));
        logger.info("modelCache size :{},trainDataCache size：{},subMessageCache size：{}", modelCache.size(), trainDataCache.size(), subMessageCache.size());
    }

    /**
     * 训练结束清除manager缓存
     *
     * @param modelKey
     */
    public void clearMessageCache(String modelKey) {
        /* 删除缓存在manager的model*/
        ManagerCache.delCache(AppConstant.MODEL_MESSAGE_CACHE, modelKey);
        AlgorithmService.messageBodyCache.remove(modelKey);
        logger.info("deleted modelKey:{}", modelKey);
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
                CommonResultStatus commonResultStatus = WorkerCommandUtil.request(AppConstant.HTTP_PREFIX + resultAddress, WorkerCommandEnum.API_TRAIN_RESULT_QUERY, param);
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
     * @param stamp stamp标志
     * @return 训练结果
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

    /**
     * 远程获取训练结果 分包处理
     *
     * @param stamp stamp标志
     * @return 分包结果
     */
    public static String getRemoteTrainResult(String stamp) {
        String trainResultKey = CacheConstant.getTrainResultKey(stamp);
        String trainResult = responseQueue.get(trainResultKey);
        String result = PacketUtil.splitData(trainResult);
        return trainResult;
    }

    /**
     * 获取训练结果方法
     *
     * @param stamp
     * @return
     */
    public static void updateTrainResult(String stamp, String strMessage) {
        if (StringUtils.isBlank(stamp) || StringUtils.isBlank(strMessage)) {
            throw new UnsupportedOperationException("stamp/message is null");
        }
        String trainResultKey = CacheConstant.getTrainResultKey(stamp);
        TrainService.responseQueue.put(trainResultKey, strMessage);
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
    private String initModel(String requestId, AlgorithmType algorithm, String parameterData, String modelToken, String dataset, int phase) {
        logger.info("initModel开始，token:{},phase:{},requestId:{}", modelToken, phase, requestId);
        try {
            Message message = KryoUtil.readFromString(parameterData);
            TrainInit trainInit = (TrainInit) message;
            String matchToken = trainInit.getMatchId();
            String[] mapResOri = IdMatchProcessor.loadResult(matchToken);
            Model localModel = CommonModel.constructModel(algorithm);
            String[][] rawData = getRawData(localModel, requestId, dataset, trainInit, mapResOri);
            InitResult initResult = localModel.initMap(requestId, rawData, trainInit, mapResOri);
            TrainData trainData = initResult.getTrainData();
            Model model = initResult.getModel();
            List<String> modelIDs = initResult.getModelIDs();

            /* 将返回的model和trainData保存在本地 并通知manager保存结果的地址*/
//            String modelAndTrainDataKey = CacheConstant.getModelAndTrainDataKey(modelToken,requestId);
            String modelAddressKey = CacheConstant.getModelAddressKey(modelToken, requestId);
            modelCache.put(modelAddressKey, model);
            trainDataCache.put(modelAddressKey, trainData);

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
        } finally {
            logger.info("initModel执行结束，token:{},phase:{},requestId:{}", modelToken, phase, requestId);
            RuntimeStatusService.releaseMem(modelToken, phase);
        }
    }

    /***
     * 获取需要读取的二维数组
     * @param localModel model
     * @param requestId 请求id
     * @param dataset 数据文件名
     * @param trainInit 初始化请求
     * @param mapResOri id对齐结果
     * @return 二维数组
     * @throws IOException
     */
    private static String FEATURE_INDEXS = "featureindexs";

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
        // todo 两个算法需要做兼容
        if (localModel instanceof DistributedRandomForestModel) {
            return reader.readDataLine(dataset, sampleData);
        } else if (localModel instanceof DistributedFederatedGBModel) {
            List<Integer> sampleCols = (ArrayList<Integer>) trainInit.getOthers().get(FEATURE_INDEXS);
            logger.info("featureindexs: " + sampleCols);
            //读取采样的数据集
            return reader.readDataCol(dataset, sampleData, sampleCols);
        } else {
            return new String[0][0];
        }
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
    private Map<String, Object> getModelAndData(String modelAddress, String modelToken, String requestId) {
        Map<String, Object> result;
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
            String data = (String) commonResultStatus.getData().get(ResponseConstant.DATA);
            result = KryoUtil.readFromString(data);
        }
        return result;
    }


    /**
     * 获取worker保存的 messageData
     *
     * @param modelAddress
     * @param modelToken
     * @param requestId
     * @return
     */
    private String getMessageData(String modelAddress, String modelToken, String requestId) {
        String result;
        String modelMessageDataKey = CacheConstant.getModelAddressKey(modelToken, requestId);
        String address = IpAddressUtil.getLocalHostLANAddress().getHostAddress() + AppConstant.COLON + ConfigUtil.getPortElseDefault();
        // 判断是结果地址是不是在本地
        if (StringUtils.equals(modelAddress, address)) {
            logger.info("get message data in local");
            result = getLocalMessageData(modelMessageDataKey);
        } else if (AlgorithmService.messageBodyCache.containsKey(modelMessageDataKey)) {
            logger.info("get message data in cache");
            result = getLocalMessageData(modelMessageDataKey);
        } else {
            // 调用http 接口查询
            String remoteModelResult = netWorkService.sendAndRecv(AppConstant.HTTP_PREFIX + modelAddress + AppConstant.SLASH + WorkerCommandEnum.API_MESSAGE_DATA_QUERY.getCode(), modelMessageDataKey);
            String finalResult = GZIPCompressUtil.unCompress(remoteModelResult);
            CommonResultStatus commonResultStatus = JsonUtil.json2Object(finalResult, CommonResultStatus.class);
            result = (String) commonResultStatus.getData().get(ResponseConstant.DATA);
            AlgorithmService.messageBodyCache.put(modelMessageDataKey, result);
            logger.info("put message data in cache");
        }
        return result;
    }

    public static Model getLocalModel(String key) {
        return modelCache.get(key);
    }

    public static TrainData getLocalTrainData(String key) {
        return trainDataCache.get(key);
    }

    public static void updateLocalModel(String key, Model value) {
        logger.info("要更新的key:{}", key);
        modelCache.keySet().stream().forEach(k -> logger.info("modelCache的key:{}", k));
        logger.info("modelCache size:{},length:{}", RamUsageEstimator.humanSizeOf(modelCache), modelCache.size());
        logger.info("原始value大小:{}", RamUsageEstimator.humanSizeOf(modelCache.get(key)));
        logger.info("更新value大小:{}", RamUsageEstimator.humanSizeOf(value));
        modelCache.put(key, value);
        logger.info("modelCache size:{},length:{}", RamUsageEstimator.humanSizeOf(modelCache), modelCache.size());
    }

    public static void updateLocalTrainData(String key, TrainData value) {
        trainDataCache.put(key, value);
    }

    public static Map<String, Object> getLocalModelAndData(String key) {
        Map<String, Object> result = new HashMap<>(16);
        result.put(AppConstant.MODEL_KEY, modelCache.get(key));
        result.put(AppConstant.TRAIN_DATA_KEY, trainDataCache.get(key));
        return result;
    }

    public static String getLocalMessageData(String key) {
        return AlgorithmService.messageBodyCache.get(key);
    }

    public static List<Message> getLocalSubMessage(String modelToken) {
        return TrainService.subMessageCache.entrySet()
                .parallelStream()
                .filter(e -> e.getKey().contains(modelToken))
                .map(e -> e.getValue())
                .collect(Collectors.toList());
    }

    public static Message subTrain(Model model, int phase, String modelToken, String parameterData, TrainData trainData) {
        Message restoreMessage = KryoUtil.readFromString(parameterData);
        //当前worker保存的subGh的list
        List<Message> subMessageList = getLocalSubMessage(modelToken);
        logger.info("subMessageList size is :{} ms", subMessageList.size());
        long start = System.currentTimeMillis();
        model.updateSubMessage(subMessageList);
        long end = System.currentTimeMillis();
        logger.info("update部分gh耗时:{} ms", (end - start));
        long s1 = System.currentTimeMillis();
        Message trainResult = model.train(phase, restoreMessage, trainData);
        long e1 = System.currentTimeMillis();
        logger.info("训练耗时:{} ms", (e1 - s1));
        Map<String, List<int[]>> instancesMap = model.getInstanceLists(trainResult);
        if (instancesMap != null && instancesMap.size() < 1) {
            return trainResult;
        }
        //所有保存了subGh的worker地址
        Map<String, String> cacheByModelToken = ManagerCache.getCacheByModelToken(AppConstant.SUB_MESSAGE_CACHE, modelToken);
        String address = IpAddressUtil.getLocalHostLANAddress().getHostAddress() + AppConstant.COLON + ConfigUtil.getPortElseDefault();
        //其余worker的地址
        List<String> addressList = cacheByModelToken.entrySet()
                .parallelStream()
                .filter(e -> !e.getValue().equals(address))
                .map(e -> e.getValue())
                .collect(Collectors.toList()).stream().distinct().collect(Collectors.toList());
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("instancesMap", instancesMap);
        paramMap.put("modelToken", modelToken);
        String paramMapStr = KryoUtil.writeToString(paramMap);
        long s2 = System.currentTimeMillis();
        List<Message> collect = addressList.parallelStream().map(s -> {
            String jieguo = netWorkService.sendAndRecv(AppConstant.HTTP_PREFIX + s + AppConstant.SLASH + WorkerCommandEnum.API_TRAIN_SUB.getCode(), paramMapStr);
            String finalResult = GZIPCompressUtil.unCompress(jieguo);
            CommonResultStatus commonResultStatus = JsonUtil.json2Object(finalResult, CommonResultStatus.class);
            String subResultStr = (String) commonResultStatus.getData().get(ResponseConstant.DATA);
            return (Message) KryoUtil.readFromString(subResultStr);
        }).collect(Collectors.toList());
        long e2 = System.currentTimeMillis();
        logger.info("部分计算总耗时:{} ms", (e2 - s2));
        long s3 = System.currentTimeMillis();
        Message message = model.mergeSubResult(trainResult, collect);
        long e3 = System.currentTimeMillis();
        logger.info(" merge部分结果耗时:{} ms", (e3 - s3));
        return message;
    }
}
