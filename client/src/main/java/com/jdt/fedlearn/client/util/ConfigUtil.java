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

package com.jdt.fedlearn.client.util;

import ch.qos.logback.core.joran.spi.JoranException;
import com.jdt.fedlearn.client.cache.InferenceDataCache;
import com.jdt.fedlearn.client.cache.ModelCache;
import com.jdt.fedlearn.client.cache.TrainDataCache;
import com.jdt.fedlearn.client.constant.Constant;
import com.jdt.fedlearn.client.entity.local.UpdateDataSource;
import com.jdt.fedlearn.client.entity.source.*;
import com.jdt.fedlearn.common.entity.jdchain.JdChainConfig;
import com.jdt.fedlearn.common.exception.ConfigParseException;
import com.jdt.fedlearn.client.type.SourceType;
import com.jdt.fedlearn.common.util.LogbackConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;


/**
 *
 */
public class ConfigUtil {
    private static final Logger logger = LoggerFactory.getLogger(ConfigUtil.class);
    /**
     * 配置文件必须参数
     */
    public static final String APP_NAME = "app.name";
    public static final String APP_PORT = "app.port";
    public static final String LOG_SETTINGS = "log.settings";
    public static final String MASTER_ADDRESS = "master.address";

    /**
     * 配置相关参数
     */
    private static FullConfig config;
    private static Properties properties;
    private static String filePath;


    public static boolean init(String filePath) throws IOException, JoranException {
        boolean isInit = true;
        ConfigUtil.filePath = filePath;

        properties = new Properties();
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            properties.load(bufferedReader);
            config = parse(properties);

            if (!checkConfigValid(properties)) {
                throw new ConfigParseException("配置文件格式校验失败");
            }
        } catch (IOException e) {
            isInit = false;
            logger.error("配置文件格式校验失败: ", e);
        }
        //logback配置文件加载
        LogbackConfigLoader.load(getProperty(LOG_SETTINGS));
        //
        ModelCache.start();
        trainConfigList();
        return isInit;
    }

    public static FullConfig parse(Properties properties) {
        String appName = properties.getProperty("app.name");
        String appPort = properties.getProperty("app.port");
        String logSettings = properties.getProperty("log.settings");
        String masterAddress = properties.getProperty("master.address");
        String token = properties.getProperty("auth.token");
        List<DataSourceConfig> trainSources = trainConfigList();
        List<DataSourceConfig> testSources = trainConfigList();
        List<DataSourceConfig> inferenceSources = trainConfigList();

        return new FullConfig(appName, appPort, logSettings, masterAddress, token, trainSources, testSources, inferenceSources);
    }

    /**
     * 重新加载配置，可以重新制定路径
     *
     * @param filePath
     */
    public static boolean reload(String filePath) {
        // 如果入参为空，使用默认filepath
        boolean isReload;
        try {
            if (Objects.isNull(filePath) || filePath.isEmpty()) {
                isReload = init(ConfigUtil.filePath);
            } else {
                // 重新加载配置文件
                isReload = init(filePath);
            }
        } catch (Exception e) {
            isReload = false;
            logger.error("重新加载配置文件失败", e);
        }
        return isReload;
    }

    /**
     * 校验配置文件格式
     *
     * @return
     */
    public static boolean checkConfigValid(Properties properties) {
        final Properties filterProperties = Optional.ofNullable(properties)
                .filter(a -> a.containsKey(APP_NAME) && Objects.nonNull(a.getProperty(APP_NAME)))
                .filter(b -> b.containsKey(APP_PORT) && Objects.nonNull(b.getProperty(APP_PORT)))
                .filter(c -> c.containsKey(LOG_SETTINGS) && Objects.nonNull(c.getProperty(LOG_SETTINGS)))
                .filter(d -> d.containsKey(MASTER_ADDRESS) && Objects.nonNull(d.getProperty(MASTER_ADDRESS)))
                .orElse(null);
        return Objects.nonNull(filterProperties);
    }

    public static String getConfigFile() {
        return filePath;
    }

    public static String getProperty(String propertyName) {
        String res = properties.getProperty(propertyName);
        if (res == null) {
            logger.error("key do not exist: " + propertyName);
            return null;
        }
        return res.trim();
    }

    // idMatch结果输出的配置文件加载 - 当前默认只用csv储存
    public static String getIdMatchDir() {
        return getProperty("idMatch.dir");
    }

    public static String getModelDir() {
        return getProperty("model.dir");
    }


    public static String getDubboUrl() {
        return getProperty("dubbo.url");
    }

    public static String getHttpUrl() {
        return getProperty("http.url");
    }

    public static boolean useTrainUid2Inference() {
        return Boolean.parseBoolean(ConfigUtil.getProperty("inference.allowTrainUid"));
    }

    public static DbConfig getInferenceDbProperties() {
        boolean flag = hasCacheInferenceData();
        if (!flag) {
            return getDbProperties("inference");
        } else {
            DbConfig dbConfig = new DbConfig();
            List<UpdateDataSource> dataSourceList = InferenceDataCache.dataSourceMap.get(InferenceDataCache.INFERENCE_DATA_SOURCE);
            UpdateDataSource updateDataSource = dataSourceList.get(0);
            dbConfig.setDriver(updateDataSource.getDriver());
            dbConfig.setUrl(updateDataSource.getUrl());
            dbConfig.setPassword(updateDataSource.getPassword());
            dbConfig.setUsername(updateDataSource.getUsername());
            return dbConfig;
        }
    }

    public static int getPortElseDefault() {
        String strPort = getProperty(APP_PORT);
        if (strPort != null) {
            return Integer.parseInt(strPort);
        }
        return Constant.DEFAULT_PORT;
    }

    public static String inferenceBaseDir() {
        boolean flag = hasCacheInferenceData();
        if (!flag) {
            return getProperty("inference.base");
        } else {
            return InferenceDataCache.dataSourceMap.get(InferenceDataCache.INFERENCE_DATA_SOURCE).get(0).getBase();
        }
    }

    public static String getInferenceFileName() {
        boolean flag = hasCacheInferenceData();
        if (!flag) {
            return getProperty("inference.dataset1");
        } else {
            return InferenceDataCache.dataSourceMap.get(InferenceDataCache.INFERENCE_DATA_SOURCE).get(0).getDataset();
        }

    }

    public static String validateBaseDir() {
        boolean flag = hasCacheValidationData();
        if (!flag) {
            return getProperty("validate.base");
        } else {
            return InferenceDataCache.dataSourceMap.get(InferenceDataCache.VALIDATION_DATA_SOURCE).get(0).getBase();
        }
    }

    public static String getValidateFileName() {
        boolean flag = hasCacheValidationData();
        if (!flag) {
            return getProperty("validate.dataset1");
        } else {
            return InferenceDataCache.dataSourceMap.get(InferenceDataCache.VALIDATION_DATA_SOURCE).get(0).getDataset();
        }

    }


    public static DbConfig getTrainDbProperties() {
        return getDbProperties("train");
    }

    private static DbConfig getDbProperties(String type) {
        DbConfig dbConfig = new DbConfig();
        dbConfig.setDriver(getProperty(type + ".driver"));
        dbConfig.setUrl(getProperty(type + ".url"));
        dbConfig.setUsername(getProperty(type + ".username"));
        dbConfig.setPassword(getProperty(type + ".password"));
        return dbConfig;
    }

    private static boolean keyExist(String key) {
        return properties.getProperty(key) != null;
    }

    public static String inferenceSourceType() {
        boolean flag = hasCacheInferenceData();
        if (!flag) {
            return getProperty("inference.data.source");
        } else {
            return InferenceDataCache.dataSourceMap.get(InferenceDataCache.INFERENCE_DATA_SOURCE).get(0).getSource();
        }
    }

    public static String validateSourceType() {
        boolean flag = hasCacheValidationData();
        if (!flag) {
            return getProperty("validate.data.source");
        } else {
            return InferenceDataCache.dataSourceMap.get(InferenceDataCache.VALIDATION_DATA_SOURCE).get(0).getSource();
        }
    }

    // key 是 数据集名称，value是数据集各项配置
    public static List<DataSourceConfig> trainConfigList() {
        List<DataSourceConfig> res = new ArrayList<>();
        //
        for (int i = 1; i < 20; i++) {
            String key = "train" + i;
            if (keyExist(key + ".source")) {
                String sourceType = getProperty(key + ".source");
                DataSourceConfig trainConfig = loadByType(sourceType, key);
                res.add(trainConfig);
            }
        }
        TrainDataCache.dataSourceMap.put(TrainDataCache.TRAIN_DATA_SOURCE, res);
        return res;
    }

    private static DataSourceConfig loadByType(String sourceType, String key) {
        if (SourceType.CSV.getSourceType().equalsIgnoreCase(sourceType)) {
            return readCsvConfig(key);
        } else if (SourceType.MYSQL.getSourceType().equalsIgnoreCase(sourceType)) {
            return readDbConfig(key);
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

    private static DbSourceConfig readDbConfig(String key) {
        String driver = getProperty(key + ".driver");
        String url = getProperty(key + ".url");
        String username = getProperty(key + ".username");
        String password = getProperty(key + ".password");
        String table = getProperty(key + ".table");
        return new DbSourceConfig(driver, username, password, url, table);
    }

    public static boolean getJdChainAvailable() {
        return Boolean.parseBoolean(getProperty("jdchain.available"));
    }

    /**
     * @param
     * @description: 加载jdchain的配置
     * @return: com.jdt.fedlearn.master.jdchain.JdChainConfig
     * @author: geyan29
     * @date: 2021/2/2 4:32 下午
     */
    public static JdChainConfig getJdChainConfig() {
        JdChainConfig jdChainConfig = new JdChainConfig();
        jdChainConfig.setGatewayIp(getProperty("jdchain.gateway_ip"));
        jdChainConfig.setGatewayPort(getProperty("jdchain.gateway_port"));
        jdChainConfig.setGatewaySecure(getProperty("jdchain.gateway_secure"));
        jdChainConfig.setUserPubkey(getProperty("jdchain.user_pubkey"));
        jdChainConfig.setUserPrivkey(getProperty("jdchain.user_privkey"));
        jdChainConfig.setUserPrivpwd(getProperty("jdchain.user_privpwd"));
        jdChainConfig.setLedgerAddress(getProperty("jdchain.ledger_address"));
        jdChainConfig.setContractAddress(getProperty("jdchain.contract_address"));
        jdChainConfig.setDataAccountAddress(getProperty("jdchain.data_account_address"));
        jdChainConfig.setEventAccountAddress(getProperty("jdchain.event_account_address"));
        return jdChainConfig;
    }

    public static String getInferenceTable(String key) {
        boolean flag = hasCacheInferenceData();
        if (!flag) {
            return getProperty(key);
        } else {
            List<UpdateDataSource> dataSourceList = InferenceDataCache.dataSourceMap.get(InferenceDataCache.INFERENCE_DATA_SOURCE);
            return dataSourceList.get(0).getTable();
        }
    }

    private static boolean hasCacheInferenceData() {
        List<UpdateDataSource> dataSourceList = InferenceDataCache.dataSourceMap.get(InferenceDataCache.INFERENCE_DATA_SOURCE);
        if (dataSourceList == null || dataSourceList.size() < 1) {
            return false;
        } else {
            return true;
        }
    }

    private static boolean hasCacheValidationData() {
        List<UpdateDataSource> dataSourceList = InferenceDataCache.dataSourceMap.get(InferenceDataCache.VALIDATION_DATA_SOURCE);
        if (dataSourceList == null || dataSourceList.size() < 1) {
            return false;
        } else {
            return true;
        }
    }

    public static FullConfig getConfig() {
        return config;
    }
}
