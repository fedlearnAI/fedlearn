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

package com.jdt.fedlearn.client.cache;

import com.jdt.fedlearn.client.constant.Constant;
import com.jdt.fedlearn.client.entity.local.UpdateDataSource;
import com.jdt.fedlearn.client.type.SourceType;
import com.jdt.fedlearn.client.util.ConfigUtil;
import com.jdt.fedlearn.client.dao.*;
import com.jdt.fedlearn.common.util.CacheUtil;
import com.jdt.fedlearn.core.entity.common.InferenceInit;
import com.jdt.fedlearn.core.loader.common.CommonLoad;
import com.jdt.fedlearn.core.loader.common.InferenceData;
import com.jdt.fedlearn.core.type.AlgorithmType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;


/**
 * 推理数据缓存和更新
 * 目前推理模块主要是实现 高性能大并发推理，会占用较多内存进行缓存
 */
public class InferenceDataCache {
    private static final Logger logger = LoggerFactory.getLogger(InferenceDataCache.class);
    public static final CacheUtil INFERENCE_CACHE = new CacheUtil(Constant.MAX_CACHE_SIZE, TimeUnit.SECONDS.toSeconds(Constant.MAX_CACHE_SECONDS));
    public static final String INFERENCE_DATA_SOURCE = "inferenceDataSource";
    public static final String VALIDATION_DATA_SOURCE = "validationDataSource";
    public static Map<String, List<UpdateDataSource>> dataSourceMap = new ConcurrentHashMap<>();

    private static String[][] loadInferenceData(String[] uidArray) {
        String inferenceSource = ConfigUtil.inferenceSourceType();
        DataReader reader;
        //TODO 以反射实现
        if (SourceType.CSV.getSourceType().equalsIgnoreCase(inferenceSource)) {
            reader = new CsvReader();
        } else if (SourceType.MYSQL.getSourceType().equalsIgnoreCase(inferenceSource)) {
            reader = new MysqlReader();
        } else if (SourceType.HTTP.getSourceType().equalsIgnoreCase(inferenceSource)) {
            reader = new HttpReader();
        } else if (SourceType.EMPTY.getSourceType().equalsIgnoreCase(inferenceSource)) {
            reader = new EmptyReader();
        } else if (SourceType.HDFS.getSourceType().equalsIgnoreCase(inferenceSource)) {
            reader = new HdfsReader();
        } else {
            throw new UnsupportedOperationException();
        }
        return reader.loadInference(uidArray);
    }

    private static String[][] loadValidationData(String[] uidArray) {
        String validateSource = ConfigUtil.validateSourceType();
        DataReader reader;
        //TODO 以反射实现
        if (SourceType.CSV.getSourceType().equalsIgnoreCase(validateSource)) {
            reader = new CsvReader();
        } else if (SourceType.MYSQL.getSourceType().equalsIgnoreCase(validateSource)) {
            reader = new MysqlReader();
        } else if (SourceType.HTTP.getSourceType().equalsIgnoreCase(validateSource)) {
            reader = new HttpReader();
        } else if (SourceType.EMPTY.getSourceType().equalsIgnoreCase(validateSource)) {
            reader = new EmptyReader();
        } else if (SourceType.HDFS.getSourceType().equalsIgnoreCase(validateSource)) {
            reader = new HdfsReader();
        } else {
            throw new UnsupportedOperationException();
        }
        return reader.loadValidate(uidArray);
    }


    public static List<Integer> checkAndCache(String inferenceId, AlgorithmType algorithm, InferenceInit init) {
        String[][] sample = loadInferenceData(init.getUid());
        List<Integer> filterIndexList = InferenceDataCache.checkUid(inferenceId, init.getUid(), sample);
        cacheInferenceData(inferenceId, algorithm, sample);
        return filterIndexList;
    }

    public static String[][] loadAndCache(String inferenceId, AlgorithmType algorithm, String[] uid) {
        String[][] sample = loadInferenceData(uid);
        cacheInferenceData(inferenceId, algorithm, sample);
        return sample;
    }

    public static String[][] loadAndCachValidate(String inferenceId, AlgorithmType algorithm, String[] uid, String labelName) {
        String[][] sample = loadValidationData(uid);
        Map<String, String> labelMap = getLabel(sample, labelName);
        String[][] removeLabelData = removeLabel(sample, labelName);
        cacheValidateData(inferenceId, algorithm, removeLabelData, labelMap);
        return removeLabelData;
    }

    public static InferenceData fetchInferenceData(String inferenceId) {
        return (InferenceData) INFERENCE_CACHE.getValue(inferenceId);
    }

    public static void updateInferenceData(String inferenceId, InferenceData inferenceData) {
        INFERENCE_CACHE.putValue(inferenceId, inferenceData);
    }

    private static void cacheInferenceData(String inferenceId, AlgorithmType algorithm, String[][] sample) {
        if (null == sample) {
            throw new UnsupportedOperationException("sample is null");
        }
        //logger.info("need to predict line:" + Arrays.deepToString(sample[0]));
        if (sample.length > 1) {
            logger.info("inferenceid : " + inferenceId + " need to  predict line:" + Arrays.deepToString(sample[1]));
//            logger.info("inferenceid : " + inferenceId + " need to  predict line:" + Arrays.deepToString(sample));
            logger.info("inferenceid : " + inferenceId + "need to predict sample size:" + sample.length);
        }
        InferenceData inferenceData = CommonLoad.constructInference(algorithm, sample);
        INFERENCE_CACHE.putValue(inferenceId, inferenceData);
    }

    private static void cacheValidateData(String inferenceId, AlgorithmType algorithm, String[][] sample,  Map<String, String> labelMap) {
        if (null == sample) {
            throw new UnsupportedOperationException("sample is null");
        }
        //logger.info("need to predict line:" + Arrays.deepToString(sample[0]));
        if (sample.length > 1) {
            logger.info("inferenceid : " + inferenceId + " need to  predict line:" + Arrays.deepToString(sample[1]));
//            logger.info("inferenceid : " + inferenceId + " need to  predict line:" + Arrays.deepToString(sample));
            logger.info("inferenceid : " + inferenceId + "need to predict sample size:" + sample.length);
        }
        InferenceData inferenceData = CommonLoad.constructInference(algorithm, sample);
        INFERENCE_CACHE.putValue(inferenceId, inferenceData);
        if (labelMap != null) {
            INFERENCE_CACHE.putValue("labelMap", labelMap);
        }
    }

    //
    public static List<Integer> checkUid(String inferenceId, String[] uid, String[][] inferenceCacheFile) {
        List<Integer> filterIndexList = new ArrayList<>();
        boolean useTrainUid2Inference = ConfigUtil.useTrainUid2Inference();
        long s2 = System.currentTimeMillis();
        if (!useTrainUid2Inference) {
            //判断uid是否存在于训练数据,
            filterIndexList = TrainDataCache.checkUidNotTrain(uid);
        }
        logger.info("inferenceid : " + inferenceId + " after checkUidNotTrain size:" + filterIndexList.size());
        logger.info("inferenceid : " + inferenceId + " TrainDataCache.checkUidNotTrain cost time: " + (System.currentTimeMillis() - s2) + " ms");
        //判断uid数据是否存在（可预测）
        long s3 = System.currentTimeMillis();
//        String[][] inferenceCacheFile = getInferenceData(inferenceId, uid);
        filterIndexList.addAll(checkUidExist(uid, inferenceCacheFile));
        logger.info("inferenceid : " + inferenceId + " checkUidExist in test data, cost time: " + (System.currentTimeMillis() - s3) + " ms");
        return filterIndexList;
    }

    //推断过程预处理，返回无需预测的uid索引
    private static List<Integer> checkUidExist(String[] uidList, String[][] inferenceCacheFile) {
        List<Integer> indexList = new ArrayList<>();
        Set<String> inferenceSet = new HashSet<>();
//        String[][] inferenceCacheFile = getInferenceData(inferenceId, uidList);
        // 如果查询结果返回空，那么需要过滤所以uid, 需要全部返回
        if (inferenceCacheFile == null || inferenceCacheFile.length == 0) {
            IntStream.range(0, uidList.length).forEach(i -> indexList.add(i));
            return indexList;
        }
        logger.info("inferenceCacheFile:" + Arrays.deepToString(inferenceCacheFile[0]));
        if (inferenceCacheFile.length > 1) {
            logger.info("inferenceCacheFile:" + Arrays.deepToString(inferenceCacheFile[1]));
        }
        for (String[] row : inferenceCacheFile) {
            inferenceSet.add(row[0]);
        }
        logger.info("checkUidExist inferenceSet size :" + inferenceSet.size());
        logger.info("checkUidExist uidList length :" + uidList.length);
        for (int i = 0; i < uidList.length; i++) {
            String id = uidList[i];
            if (!inferenceSet.contains(id)) {
                indexList.add(i);
            }
        }
        return indexList;
    }

    // 删除label后的数据集
    private static String[][] removeLabel(String[][] rawData, String labelName) {
        int labelIndex = getLabelIndex(rawData, labelName);
        String[][] res;
        if (labelIndex == rawData[0].length)  {
            res = new String[rawData.length][rawData[0].length];
        } else {
            res = new String[rawData.length][rawData[0].length - 1];
        }
        for (String[] resRow: res) {
            Arrays.fill(resRow, "");
        }
        for (int row = 0; row < rawData.length; row++) {
            int afterLabel = 0;
            for (int col = 0; col < rawData[row].length; col++) {
                if (col == labelIndex) {
                    afterLabel = 1;
                    continue;
                }
                if (afterLabel == 1) {
                    res[row][col-1] = rawData[row][col];
                } else {
                    res[row][col] = rawData[row][col];
                }
            }
        }
        return res;
    }
    // 获取label
    private static Map<String, String> getLabel(String[][] rawData, String labelName) {
        int labelIndex = getLabelIndex(rawData, labelName);
        if (labelIndex == rawData[0].length) {
            return null;
        }
        Map<String, String> labelMap = new HashMap<>();
        for (int row = 1; row < rawData.length; row++) {
            labelMap.put(rawData[row][0], rawData[row][labelIndex]);

        }
        return labelMap;
    }
    // 计算label所在列
    private static int getLabelIndex(String[][] rawData, String labelName) {
        int labelIndex = rawData[0].length;
        for (int i = 0; i < rawData[0].length; i++) {
            if (rawData[0][i].equals(labelName)) {
                labelIndex = i;
                break;
            }
        }
        return labelIndex;
    }

}
