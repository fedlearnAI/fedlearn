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
package com.jdt.fedlearn.worker.util;

import ch.qos.logback.core.joran.spi.JoranException;
import com.jdt.fedlearn.common.util.LogbackConfigLoader;
import com.jdt.fedlearn.worker.constant.Constant;
import com.jdt.fedlearn.worker.entity.source.CsvSourceConfig;
import com.jdt.fedlearn.worker.entity.source.DataSourceConfig;
import com.jdt.fedlearn.worker.entity.source.HdfsSourceConfig;
import com.jdt.fedlearn.worker.type.SourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;


/**
 * 配置文件相关加载
 */
public class ConfigUtil{
    private static final Logger logger = LoggerFactory.getLogger(ConfigUtil.class);
    /**
     * 配置文件必须参数
     */
    public static  String APP_PORT = "app.port";
    private static Properties properties = null;

    public static void init(String filePath){
        properties = new Properties();
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            properties.load(bufferedReader);
            LogbackConfigLoader.load(getProperty("log.settings"));
        } catch (IOException | JoranException e) {
            logger.error("other exception:", e);
        }
    }

    /**
     * 读取配置文件
     * @param propertyName
     * @return
     */
    public static String getProperty(String propertyName) {
        String res = properties.getProperty(propertyName);
        if (res == null) {
            logger.error("key do not exist:" + propertyName);
            return null;
        }
        return res.trim();
    }

    /**
     * 模型保存路径
     * @return
     */
    public static String getModelDir() {
        return getProperty("model.dir");
    }

    /**
     * 是否允许预测训练集中的uid
     * @return
     */
    public static boolean useTrainUid2Inference() {
        return Boolean.parseBoolean(getProperty("inference.allowTrainUid"));
    }
    // idMatch结果输出的配置文件加载 - 当前默认只用csv储存
    public static String getIdMatchDir() {
        return getProperty("idMatch.dir");
    }

    /**
     * 获取端口号
     * @return
     */
    public static int getPortElseDefault() {
        String strPort = getProperty(APP_PORT);
        if (strPort != null) {
            return Integer.parseInt(strPort);
        }
        return Constant.DEFAULT_PORT;
    }

    /**
     * 推理文件路径
     * @return
     */
    public static String inferenceBaseDir() {
        return getProperty("inference.base");
    }

    /**
     * 推理数据集
     * @return
     */
    public static String getInferenceFileName() {
        return getProperty("inference.dataset1");
    }

    /**
     * @param key
     * @return
     */
    public static boolean keyExist(String key) {
        return getProperty(key) != null;
    }

    public static String inferenceSourceType() {
        return getProperty("inference.data.source");
    }

    /**
     * key 是 数据集名称，value是数据集各项配置
     * @return
     */
    public static List<DataSourceConfig> trainConfigList() {
        List<DataSourceConfig> res = new ArrayList<>();
        //
        for (int i = 1; i < 10; i++) {
            String key = "train" + i;
            if (keyExist(key + ".source")) {
                String sourceType = getProperty(key + ".source");
                DataSourceConfig trainConfig = loadByType(sourceType, key);
                res.add(trainConfig);
            }
        }
        return res;
    }

    /**
     * 根据文件类型获取对应配置
     * @param sourceType
     * @param key
     * @return
     */
    private static DataSourceConfig loadByType(String sourceType, String key) {
        if (SourceType.CSV.getSourceType().equalsIgnoreCase(sourceType)) {
            return readCsvConfig(key);
        } else if (SourceType.HDFS.getSourceType().equalsIgnoreCase(sourceType)) {
            return readHdfsConfig(key);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    private static CsvSourceConfig readCsvConfig(String key) {
        String base = getProperty(key + ".base");
        String dataset = getProperty(key + ".dataset");
        return new CsvSourceConfig(base, dataset);
    }

    private static HdfsSourceConfig readHdfsConfig(String key) {
        String base = getProperty(key + ".base");
        String dataset = getProperty(key + ".dataset");
        return new HdfsSourceConfig(base, dataset);
    }

    public static String validateBaseDir() {
        return getProperty("validate.base");
    }

    public static String getValidateFileName() {
        return getProperty("validate.dataset1");

    }

    public static String validateSourceType() {
        return getProperty("validate.data.source");

    }


}
