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
import com.jdt.fedlearn.client.cache.TrainDataCache;
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
 * 初始化时，从文件加载配置，并赋值给FullConfig类对象，后续统一在
 */
public class ConfigUtil {
    private static final Logger logger = LoggerFactory.getLogger(ConfigUtil.class);
    /**
     * 配置文件必须参数
     */
    private static final String APP_NAME = "app.name";
    private static final String APP_PORT = "app.port";
    private static final String LOG_SETTINGS = "log.settings";
    private static final String MASTER_ADDRESS = "master.address";
    private static final String AUTH_TOKEN = "auth.token";
    private static final String MODEL_DIR = "model.dir";
    private static final String MATCH_DIR = "match.dir";
    private static final String INFERENCE_ALLOW_TRAIN_UID = "inference.allowTrainUid";
    private static final String MASTER_BELONG = "master.belong";


    /**
     * 配置相关参数
     */
    private static ClientConfig clientConfig;
    private static Properties properties;
    private static String filePath;

    private ConfigUtil() {
    }


    public static boolean init(String filePath) throws IOException, JoranException {
        boolean isInit = true;
        ConfigUtil.filePath = filePath;

        properties = new Properties();
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            properties.load(bufferedReader);
            clientConfig = parse(properties);
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
        return isInit;
    }

    private static ClientConfig parse(Properties properties) {
        String appName = properties.getProperty(APP_NAME);
        int appPort = Integer.parseInt(properties.getProperty(APP_PORT));
        String logSettings = properties.getProperty(LOG_SETTINGS);
        String masterAddress = properties.getProperty(MASTER_ADDRESS);
        String token = properties.getProperty(AUTH_TOKEN);
        List<DataSourceConfig> trainSources = trainConfigList();
        List<DataSourceConfig> testSources = new ArrayList<>();
        List<DataSourceConfig> inferenceSources = inferenceConfigList();
        ClientConfig clientConfig = new ClientConfig(appName, appPort, logSettings, masterAddress, token, trainSources, testSources, inferenceSources);
        clientConfig.setModelDir(properties.getProperty(MODEL_DIR));
        clientConfig.setMatchDir(properties.getProperty(MATCH_DIR));
        clientConfig.setAllowTrainUid(Boolean.parseBoolean(properties.getProperty(INFERENCE_ALLOW_TRAIN_UID)));
        clientConfig.setMasterBelong(properties.getProperty(MASTER_BELONG));

        return clientConfig;
    }

    /**
     * 校验配置文件格式
     *
     * @return
     */
    private static boolean checkConfigValid(Properties properties) {
        final Properties filterProperties = Optional.ofNullable(properties)
                .filter(a -> a.containsKey(APP_NAME) && Objects.nonNull(a.getProperty(APP_NAME)))
                .filter(b -> b.containsKey(APP_PORT) && Objects.nonNull(b.getProperty(APP_PORT)))
                .filter(c -> c.containsKey(LOG_SETTINGS) && Objects.nonNull(c.getProperty(LOG_SETTINGS)))
                .filter(d -> d.containsKey(MASTER_ADDRESS) && Objects.nonNull(d.getProperty(MASTER_ADDRESS)))
                .orElse(null);
        return Objects.nonNull(filterProperties);
    }

    public static String getProperty(String propertyName) {
        String res = properties.getProperty(propertyName);
        if (res == null) {
            logger.error("key do not exist: " + propertyName);
            return null;
        }
        return res.trim();
    }

    private static final String INFERENCE = "inference";
    private static final String SOURCE = ".source";
    private static List<DataSourceConfig> inferenceConfigList() {
        List<DataSourceConfig> res = new ArrayList<>();
        //
        for (int i = 1; i < 2; i++) {
            String key = INFERENCE + i;
            if (keyExist(key + SOURCE)) {
                String sourceType = getProperty(key + SOURCE);
                DataSourceConfig inferenceConfig = loadByType(sourceType, key);
                res.add(inferenceConfig);
            }
        }
        return res;
    }

    public static boolean keyExist(String key) {
        return properties.getProperty(key) != null;
    }

    private static final String TRAIN = "train";
    // key 是 数据集名称，value是数据集各项配置
    private static List<DataSourceConfig> trainConfigList() {
        List<DataSourceConfig> res = new ArrayList<>();
        //
        for (int i = 1; i < 20; i++) {
            String key = TRAIN + i;
            if (keyExist(key + SOURCE)) {
                String sourceType = getProperty(key + SOURCE);
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

    private static final String BASE = ".base";
    private static final String DATASET = ".dataset";
    private static final String URI = ".uri";
    private static final String USER = ".user";
    private static final String DRIVER = ".driver";
    private static final String URL = ".url";
    private static final String USERNAME = "username";
    private static final String PASS_KEY = "password";
    private static final String TABLE = "table";
    private static CsvSourceConfig readCsvConfig(String key) {
        String base = getProperty(key + BASE);
        String dataset = getProperty(key + DATASET);
        return new CsvSourceConfig(base, dataset);
    }

    private static HdfsSourceConfig readHdfsConfig(String key) {
        String base = getProperty(key + BASE);
        String dataset = getProperty(key + DATASET);
        String uri = ConfigUtil.getProperty(key + URI);
        String user = ConfigUtil.getProperty(key + USER);
        return new HdfsSourceConfig(base, uri, user, dataset);
    }

    private static DbSourceConfig readDbConfig(String key) {
        String driver = getProperty(key + DRIVER);
        String url = getProperty(key + URL);
        String username = getProperty(key + USERNAME);
        String password = getProperty(key + PASS_KEY);
        String table = getProperty(key + TABLE);
        return new DbSourceConfig(driver, username, password, url, table);
    }

    public static ClientConfig getClientConfig() {
        return clientConfig;
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

    /**
     * 获取端口号
     * @return
     */
    public static int getPortElseDefault() {
        String strPort = getProperty(APP_PORT);
        if (strPort != null) {
            return Integer.parseInt(strPort);
        }
        return 0;
    }
    /**
     * 模型保存路径
     * @return
     */
    public static String getModelDir() {
        return getProperty(MODEL_DIR);
    }
}
