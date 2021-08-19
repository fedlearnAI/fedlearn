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

import com.jdt.fedlearn.client.dao.CsvReader;
import com.jdt.fedlearn.client.dao.DataReader;
import com.jdt.fedlearn.client.dao.HdfsReader;
import com.jdt.fedlearn.client.dao.MysqlReader;
import com.jdt.fedlearn.client.entity.Feature;
import com.jdt.fedlearn.client.entity.source.DataSourceConfig;
import com.jdt.fedlearn.client.type.SourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 根据指定的文件名加载训练文件，
 * 可以根据配置加载多份训练文件，支持从不同的数据源和数据类型加载文件
 * 最终的返回结果均为<code>String[][]</code>
 * 通过唯一的 <code>modelToken</code>来保证同一个训练只会采用同一份数据，
 * 当 <code>modelToken</code> 更新时，会去检测是否用的同一份数据，如果不是同一份，则重新加载数据
 */

public class TrainDataCache {
    private static final Logger logger = LoggerFactory.getLogger(TrainDataCache.class);
    private static final Object dataNameLock = new Object();
    private static final Object datasetLock = new Object();
    public static Map<String, String[][]> cacheFileMap = new ConcurrentHashMap<>();
    public static final String TRAIN_DATA_SOURCE = "trainDataSource";
    public static Map<String, List<DataSourceConfig>> dataSourceMap = new ConcurrentHashMap<>();

    private static String[][] loadFullData(String dataName) throws IOException {
        //TODO 后续根据传入的 dataName 确定使用哪个 config
//        List<DataSourceConfig> trainConfigs = ConfigUtil.trainConfigList();
        List<DataSourceConfig> trainConfigs = dataSourceMap.get(TRAIN_DATA_SOURCE);
        DataSourceConfig trainConfig = null;
        for (DataSourceConfig config : trainConfigs) {
            if (config.getDataName().equals(dataName)) {
                trainConfig = config;
            }
        }
        if (trainConfig == null) {
            logger.error("not matched dataset:" + dataName + " use first config instead");
            trainConfig = trainConfigs.get(0);
        }
        SourceType sourceType = trainConfig.getSourceType();
        DataReader reader;
        if (SourceType.CSV.equals(sourceType)) {
            reader = new CsvReader();
        } else if (SourceType.MYSQL.equals(sourceType)) {
            reader = new MysqlReader();
        } else if (SourceType.HDFS.equals(sourceType)) {
            reader = new HdfsReader();
        } else {
            throw new UnsupportedOperationException("unsupported source type: " + sourceType);
        }
        return reader.loadTrain(trainConfig);
    }


    public static String[][] getTrainData(String matchToken, String dataName) throws IOException {
        if (!cacheFileMap.containsKey(matchToken)) {
            synchronized (dataNameLock) {
                if (!cacheFileMap.containsKey(dataName)) {
                    String[][] cacheFile = loadFullData(dataName);
                    cacheFileMap.put(dataName, cacheFile);
                }
            }
        }
        String[][] trainData = cacheFileMap.get(dataName);
        logger.info("trainData size:" + trainData.length);
        return trainData;
    }

    // 返回
    public static List<Integer> checkUidNotTrain(String[] uidList) {
        String[][] cacheFile = cacheFileMap.values().iterator().next();
        List<Integer> indexList = new ArrayList<>();
        if (cacheFile == null) {
            return indexList;
        }

        Set<String> trainSet = new HashSet<>();
        for (String[] row : cacheFile) {
            trainSet.add(row[0]);
        }
        for (int i = 0; i < uidList.length; i++) {
            String id = uidList[i];
            if (trainSet.contains(String.valueOf(id))) {
                indexList.add(i);
            }
        }
        return indexList;
    }


    public static Map loadLabelMap(String label) {
        String[][] cacheFile = cacheFileMap.values().iterator().next();
        Map<String, String> labelMap = new HashMap<>();
        List<String> labels = Arrays.asList(cacheFile[0]);
        int index = labels.indexOf(label);
        if (index >= 0) {
            for (int i = 1, len = cacheFile.length; i < len; i++) {// 从1开始，去掉表头
                String[] row = cacheFile[i];
                String uid = String.valueOf(row[0]);
                String value = row[index];
                labelMap.put(uid, value);
            }
            return labelMap;
        }
        return null;
    }

    // TODO 和训练数据读取分离，只读取指定列
    public static String[] loadTrainDataUid(String dataset, String uidName) throws IOException {
        String[][] trainData = readFullTrainData("", dataset);
        int index = 0;
        String[] header = trainData[0];
        for (int i = 0; i < header.length; i++) {
            if (uidName != null && uidName.equals(header[i])) {
                index = i;
            }
        }
        String[] inst_id_list = new String[trainData.length - 1];
        for (int i = 1; i < trainData.length; i++) {
            inst_id_list[i - 1] = trainData[i][index];
        }
        return inst_id_list;
    }

    public static String[] getFirstColumnUid(String[][] data) {
        String[] inst_id_list = new String[data.length - 1];
        for (int i = 1; i < data.length; i++) {
            inst_id_list[i - 1] = data[i][0];
        }
        return inst_id_list;
    }


    //根据id加载数据
    public static String[][] readFullTrainData(String modelToken, String dataName) throws IOException {
        if (!cacheFileMap.containsKey(dataName)) {
            synchronized (dataNameLock) {
                if (!cacheFileMap.containsKey(dataName)) {
                    String[][] cacheFile = loadFullData(dataName);
                    cacheFileMap.put(dataName, cacheFile);
                }
            }
        }
        String[][] trainData = cacheFileMap.get(dataName);
        logger.info("trainData size:" + trainData.length);
        return trainData;
    }

    public static List<Feature> loadHeader(DataSourceConfig config) {
        List<Feature> res = new ArrayList<>();
        SourceType sourceType = config.getSourceType();
        DataReader reader;
        switch (sourceType) {
            case CSV: {
                reader = new CsvReader();
                break;
            }
            case MYSQL: {
                reader = new MysqlReader();
                break;
            }
            case HDFS: {
                reader = new HdfsReader();
                break;
            }
            default: {
                throw new UnsupportedOperationException("unsupported source type: " + sourceType);
            }
        }
        String[] header = reader.loadHeader(config);
        for (String feature : header) {
            //TODO 后续采用启发式方法确定字段类型
            res.add(new Feature(feature, "float"));
        }
        return res;
    }
}
