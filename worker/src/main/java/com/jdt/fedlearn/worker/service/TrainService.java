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
import com.jdt.fedlearn.core.type.data.Tuple2;
import com.jdt.fedlearn.core.util.Tool;
import com.jdt.fedlearn.worker.cache.ManagerCache;
import com.jdt.fedlearn.worker.cache.ModelCache;
import com.jdt.fedlearn.worker.constant.CacheConstant;
import com.jdt.fedlearn.worker.constant.Constant;
import com.jdt.fedlearn.worker.dao.*;
import com.jdt.fedlearn.worker.entity.source.DataSourceConfig;
import com.jdt.fedlearn.worker.entity.train.QueryProgress;
import com.jdt.fedlearn.worker.multi.TrainProcess;
import com.jdt.fedlearn.common.enums.RunningType;
import com.jdt.fedlearn.worker.type.SourceType;
import com.jdt.fedlearn.common.constant.AppConstant;
import com.jdt.fedlearn.common.util.*;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.common.TrainInit;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.model.Model;
import com.jdt.fedlearn.core.model.DistributedRandomForestModel;
import com.jdt.fedlearn.core.model.common.CommonModel;
import com.jdt.fedlearn.core.parameter.SuperParameter;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.worker.util.ConfigUtil;
import com.jdt.fedlearn.worker.util.ExceptionUtil;
import com.jdt.fedlearn.common.constant.ResponseConstant;
import com.jdt.fedlearn.common.entity.CommonResultStatus;
import com.jdt.fedlearn.common.entity.TrainRequest;
import com.jdt.fedlearn.common.enums.WorkerCommandEnum;
import com.jdt.fedlearn.grpc.federatedlearning.PaillierKeyPublic;
import com.jdt.fedlearn.common.util.HttpClientUtil;
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

        String modelKey = CacheConstant.getMoldeKey(modelToken, requestId);
        String modelResult = ManagerCache.getCache(AppConstant.MODEL_CACHE,modelKey);
        if (modelResult == null) {
            return initModel(algorithm,parameterData,modelToken,dataset);
        }
        String result = "";
        //训练中的模型直接加载
        DistributedRandomForestModel model = null;
        try {
            model = (DistributedRandomForestModel) SerializationUtils.deserialize(modelResult);
            logger.info("moldeKey={}, phase={}, status={}", modelKey, phase,status);
            String stamp = UUID.randomUUID().toString();
            if (status.equals(RunningType.COMPLETE)) {
                result = RunningType.COMPLETE.getRunningType();
                // 保存结果
                String trainResultKey = CacheConstant.getTrainResultKey(stamp);
                TrainService.responseQueue.put(trainResultKey, result);
                ModelDao.saveModel(modelToken, model.serialize());
                ModelCache modelCache = ModelCache.getInstance();
                modelCache.put(modelToken, model);
                /* 训练结束清除所有缓存*/
                clearManagerCache(modelKey,modelToken);
                logger.info("client train is complete!!!");
            } else {
                // 提交到子线程执行
                if (trainRequest.isSync()) {
                    Message restoreMessage = Constant.serializer.deserialize(parameterData);
                    Message data =  model.train(phase, restoreMessage, null);
                    String strMessage =  Constant.serializer.serialize(data);
                    return strMessage;
                }
                TrainProcess trainProcess = new TrainProcess(model, stamp, modelToken, phase, parameterData, null, requestId);
                trainProcess.run();
            }
            String trainResultAddressKey = CacheConstant.getTrainResultAddressKey(stamp);
            ManagerCache.putCache(AppConstant.ADDRESS_CACHE,trainResultAddressKey,IpAddress.getLocalHostLANAddress().getHostAddress()+":"+ ConfigUtil.getPortElseDefault());
            // 最后返回异步的stamp
            result = "{\"stamp\": \"" + stamp + "\"}";
            logger.info("stamp is:" + result);
        } catch (ClassNotFoundException | IOException e) {
            logger.error("train error!",e);
        }
        return result;
    }

    /**
     * 训练结束清除manager缓存
     * @param modelKey
     * @param modelToken
     */
    private void clearManagerCache(String modelKey,String modelToken) {
        /* 删除缓存在manager的model*/
        ManagerCache.delCache(AppConstant.MODEL_CACHE,modelKey);
        ManagerCache.delCache(AppConstant.TREE_CACHE, CacheConstant.getTreeKey(modelToken));
        ManagerCache.delCache(AppConstant.FIRST_CACHE, CacheConstant.getIsFirst(modelToken));
    }

    /**
     * 异步查询训练结果
     * @param query
     * @return
     */
    public Map<String, Object> queryProgress(QueryProgress query) {
        Map<String, Object> modelMap = new HashMap<>();
        String stamp = query.getStamp();
        try {
            String trainResultAddressKey = CacheConstant.getTrainResultAddressKey(stamp);
            String resultAddress = ManagerCache.getCache(AppConstant.ADDRESS_CACHE,trainResultAddressKey);
            // 删除缓存
            ManagerCache.delCache(AppConstant.ADDRESS_CACHE,trainResultAddressKey);
            String address = IpAddress.getLocalHostLANAddress().getHostAddress() + ":" + ConfigUtil.getPortElseDefault();
            // 判断是结果地址是不是在本地
            String trainResult = null;
            if (StringUtils.equals(resultAddress, address)) {
                trainResult = TrainService.getTrainResult(stamp);
            } else {
                Map<String, Object> param = Maps.newHashMap();
                param.put("stamp", stamp);
                // 调用http 接口查询
                String remoteTrainResult = HttpClientUtil.doHttpPost("http://" + resultAddress + "/" + WorkerCommandEnum.API_TRAIN_RESULT_QUERY.getCode(), param);
                String finalResult = HttpClientUtil.unCompress(remoteTrainResult);
                CommonResultStatus commonResultStatus = JsonUtil.json2Object(finalResult, CommonResultStatus.class);
                trainResult = (String) commonResultStatus.getData().get(ResponseConstant.DATA);
            }
            if (trainResult != null) {
                modelMap.put(ResponseConstant.CODE, ResponseConstant.SUCCESS_CODE);
                modelMap.put(ResponseConstant.STATUS, "COMPLETE");
                // todo
                modelMap.put(ResponseConstant.DATA, trainResult);
            } else {
                modelMap.put(ResponseConstant.CODE, ResponseConstant.DOING_CODE);
                modelMap.put(ResponseConstant.STATUS, "DOING");
            }
        } catch (Exception e){
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
    private String initModel(AlgorithmType algorithm, String parameterData, String modelToken, String dataset){
        try {
            Model localModel = null;
            //构造trainData
            Message message = Constant.serializer.deserialize(parameterData);
            TrainInit trainInit = (TrainInit)message;
            SuperParameter superParameter = trainInit.getParameter();
//            Map<Long, String> idMap = trainInit.getIdMap().getContent();

            String matchToken = trainInit.getMatchId();
            // 此时是在client端，解密的状态
            String[] mapResOri = IdMatchProcessor.loadResult(matchToken);
            String[] mapRes;

            int[] testIndex = trainInit.getTestIndex();
            Tuple2<String[],String[]> trainTestUid = Tool.splitUid(mapResOri,testIndex);
            String[] testId = trainTestUid._2();
            mapRes = trainTestUid._1();

            Map<Long, String> idMap = new HashMap<>();
            for (int i = 0; i < mapRes.length; i++) {
                idMap.put((long) i, mapRes[i]);
            }
            Features features = trainInit.getFeatureList();
            Map<String, Object> others = trainInit.getOthers();

            DataReader reader = getReader(dataset);
            // 获取到idMap 对应数据的索引
            long s = System.currentTimeMillis();
            String[][] newIndexId = reader.readDataIndex(dataset, idMap);
            String[][] sortedIndexId = Arrays.stream(newIndexId).parallel().sorted(Comparator.comparing(x -> x[0])).toArray(String[][]::new);
            List<Integer> sortedIndexList = Arrays.stream(sortedIndexId).parallel().map(x -> Integer.valueOf(x[1])).collect(Collectors.toList());

            // 获取验证数据


            Map<Long, String> validationIdMap = new HashMap();
            for(int i = 0; i < testId.length; ++i) {
                validationIdMap.put((long)i, testId[i]);
            }
            String[][] validationLinesArr = reader.readDataIndex(dataset, validationIdMap);
            List<Integer> validationLinesList = Arrays.stream(validationLinesArr).parallel().map(x -> Integer.valueOf(x[1])).collect(Collectors.toList());
            List<Integer> validationLines = new ArrayList<>();
            validationLines.add(0);
            // 因为添加了一行头部，整体需要
            List<Integer>  validationLinesData = validationLinesList.stream().map(integer -> integer + 1).collect(Collectors.toList());
            validationLines.addAll(validationLinesData);
            String[][] validationData = reader.readDataLine(dataset, validationLines);

            long d = System.currentTimeMillis();
            logger.info("read index cost: ", d-s);
            Map<Long, ArrayList<Integer>> sampleIdsMap = (Map<Long, ArrayList<Integer>>) trainInit.getOthers().get("sampleIds");
            Set<Long> treeIdsSet = sampleIdsMap.keySet();
            String treeKey = CacheConstant.getTreeKey(modelToken);
            ManagerCache.putCache(AppConstant.TREE_CACHE,treeKey, JsonUtil.object2json(treeIdsSet));
            int idx = 0;
            String privateKey = "null";
            PaillierKeyPublic keyPublic = null;
            for (Map.Entry<Long, ArrayList<Integer>> entry : sampleIdsMap.entrySet()) { //构造model
                localModel = CommonModel.constructModel(algorithm);
                ArrayList<Integer> sampleRandomIndex = entry.getValue();
                // 和idMap的索引获取交集
                List<Integer> intersection = sampleRandomIndex.parallelStream().map(x->sortedIndexList.get(x)).collect(Collectors.toList());
                // 防止乱序
                Collections.sort(intersection);
                // 添加第一行头
                ArrayList<Integer> feature = new ArrayList<>();
                feature.add(0);
                // 因为添加了一行头部，整体需要
                List<Integer> mapSampleRandomIndex = intersection.stream().map(integer -> integer + 1).collect(Collectors.toList());
                feature.addAll(mapSampleRandomIndex);
                //读取采样的数据集
                String[][] trainPara = reader.readDataLine(dataset, feature);
                logger.info("read file cost: ", d-s);
                String isDistributed = "true";
                others.put("isDistributed", isDistributed);
                localModel.trainInit(trainPara, mapResOri, testIndex, superParameter, features, others);
                if (idx == 0) {
                    privateKey = ((DistributedRandomForestModel)localModel).getPrivateKeyString();
                    keyPublic = ((DistributedRandomForestModel)localModel).getKeyPublic();
                } else {
                    ((DistributedRandomForestModel)localModel).setPrivateKeyString(privateKey);
                    ((DistributedRandomForestModel)localModel).setKeyPublic(keyPublic);
                }
                ((DistributedRandomForestModel)localModel).setValidationData(validationData);
                ((DistributedRandomForestModel)localModel).setTestId(testId);
                String moldeTreeKey = CacheConstant.getMoldeKey(modelToken, entry.getKey().toString());
                /* 将model保存在manager*/
                ManagerCache.putCache(AppConstant.MODEL_CACHE,moldeTreeKey,SerializationUtils.serialize(localModel));
                idx++;
            }
            return AppConstant.INIT_SUCCESS;
        } catch (Exception e) {
            logger.error(ExceptionUtil.getExInfo(e));
            return AppConstant.INIT_FAILED;
        }
    }

    /**
    * @description: 根据文件名获取reader
    * @param dataset
    * @return: com.jdd.ml.federated.client.dao.DataReader
    * @author: geyan29
    * @date: 2021/5/18 4:39 下午
    */
    private DataReader getReader(String dataset){
        List<DataSourceConfig> trainConfigs = ConfigUtil.trainConfigList();
        DataSourceConfig trainConfig = null;
        for (DataSourceConfig config : trainConfigs) {
            if (config.getDataName().equals(dataset)) {
                trainConfig = config;
            }
        }
        if(trainConfig != null){
            SourceType sourceType = trainConfig.getSourceType();
            DataReader reader;
            if (SourceType.CSV.equals(sourceType)) {
                reader = new CsvReader(trainConfig);
            } else if(SourceType.HDFS.equals(sourceType)){
                reader = new HdfsReader(trainConfig);
            }else{
                reader = new CsvReader(trainConfig);
            }
            return reader;
        }else{
            throw new RuntimeException("getReader error,trainConfig is null!");
        }
    }

}
