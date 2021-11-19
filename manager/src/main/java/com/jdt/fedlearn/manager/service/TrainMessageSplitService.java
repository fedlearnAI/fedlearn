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
package com.jdt.fedlearn.manager.service;

import com.jdt.fedlearn.common.constant.AppConstant;
import com.jdt.fedlearn.common.constant.CacheConstant;
import com.jdt.fedlearn.common.entity.CommonResultStatus;
import com.jdt.fedlearn.common.enums.ResultTypeEnum;
import com.jdt.fedlearn.common.enums.WorkerCommandEnum;
import com.jdt.fedlearn.tools.network.INetWorkService;
import com.jdt.fedlearn.tools.GZIPCompressUtil;
import com.jdt.fedlearn.tools.serializer.JsonUtil;
import com.jdt.fedlearn.tools.serializer.KryoUtil;
import com.jdt.fedlearn.tools.WorkerCommandUtil;
import com.jdt.fedlearn.core.entity.boost.EncryptedGradHess;
import com.jdt.fedlearn.manager.util.ConfigUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @className: TrainMessageSplitService
 * @description: 平均的像各个worker分发拆分的message
 * @author: geyan29
 * @createTime: 2021/11/5 4:25 下午
 */
@Service
public class TrainMessageSplitService {
    @Resource
    CacheManager cacheManager;
    public static INetWorkService netWorkService = INetWorkService.getNetWorkService();
    public static Map<String, Integer> workerCount = new ConcurrentHashMap<>();
    private static Logger logger = LoggerFactory.getLogger(TrainMessageSplitService.class);

    static {
        String workerProperties = ConfigUtil.getWorkerProperties();
        String[] workers = StringUtils.split(workerProperties, AppConstant.SPLIT);
        Arrays.stream(workers).forEach(worker -> workerCount.put(worker, 0));
    }

    /***
     * @description: 将subMessage平均的发送到各个worker
     * @param content
     * @return: java.util.Map<java.lang.String, java.lang.Object>
     * @author: geyan29
     * @date: 2021/11/5 4:24 下午
     */
    public Map<String, Object> splitMessage(String content) {

        Map<String, Object> resultMap = new HashMap<>();
        Map<String, Object> subMessageMap = KryoUtil.readFromString(content);
        String modelToken = (String) subMessageMap.get("modelToken");

        EncryptedGradHess message = (EncryptedGradHess) subMessageMap.get("message");
        int modelId = message.getModelId();
        String key = CacheConstant.getSubMessageKey(modelToken, String.valueOf(modelId));
        Map<String, String> map = new HashMap<>();
        map.put(AppConstant.MANAGER_CACHE_TYPE, AppConstant.SUB_MESSAGE_CACHE);
        map.put(AppConstant.MANAGER_CACHE_KEY, key);
        String workerAddress = cacheManager.getCache(map);
//        cacheManager.delCache(AppConstant.SUB_MESSAGE_CACHE, modelToken);
        if (workerAddress != null) {
            logger.info("clear worker gh :{}, key is {} ", workerAddress, key);
            netWorkService.sendAndRecv(AppConstant.HTTP_PREFIX + workerAddress + AppConstant.SLASH + WorkerCommandEnum.API_SUB_MESSAGE_DATA_DELETE.getCode(), key);
        }
        //取worker保存的最小的个数
        String useWorker;
        synchronized (workerCount) {
            int minCount = workerCount.entrySet().parallelStream().mapToInt(e -> e.getValue()).min().getAsInt();
            List<String> collect = workerCount.entrySet().parallelStream().filter(e -> e.getValue().equals(minCount)).map(Map.Entry::getKey).collect(Collectors.toList());
            useWorker = collect.get(0);
            logger.info("update worker :{} , key is {}, model id is {}", useWorker, key, modelId);
            String result = netWorkService.sendAndRecv(AppConstant.HTTP_PREFIX + useWorker + AppConstant.SLASH + WorkerCommandEnum.API_TRAIN_SPLIT_DATA.getCode(), content);
            CommonResultStatus commonResultStatus = JsonUtil.json2Object(GZIPCompressUtil.unCompress(result), CommonResultStatus.class);
            if (commonResultStatus.getResultTypeEnum().equals(ResultTypeEnum.SUCCESS)) {
                resultMap = commonResultStatus.getData();
                workerCount.put(useWorker, minCount + 1);
            }
        }
        logger.info("workerCount 情况：{}", useWorker);
        Map param = new HashMap();
        param.put(AppConstant.MANAGER_CACHE_TYPE, AppConstant.SUB_MESSAGE_CACHE);
        param.put(AppConstant.MANAGER_CACHE_KEY, key);
        param.put(AppConstant.MANAGER_CACHE_VALUE, useWorker);
        cacheManager.putCache(param);
        logger.info("manager缓存：{}",CacheManager.subMessageAddressCacheMap);
        return resultMap;
    }


    /**
     * @param modelToken
     * @description: 清除workerCount的计数
     * @return: void
     * @author: geyan29
     * @date: 2021/11/5 4:24 下午
     */
    public void deleteCache(String modelToken) {
        workerCount.keySet().parallelStream().filter(k -> k.contains(modelToken)).forEach(k -> workerCount.remove(k));
    }
}
