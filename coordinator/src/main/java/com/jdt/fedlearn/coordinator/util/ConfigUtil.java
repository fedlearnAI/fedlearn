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

package com.jdt.fedlearn.coordinator.util;

import com.jdt.fedlearn.common.entity.jdchain.JdChainConfig;
import com.jdt.fedlearn.coordinator.constant.Constant;
import com.jdt.fedlearn.coordinator.entity.DbConfig;
import com.jdt.fedlearn.common.enums.DbType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;


/**
 * 从配置文件读取具体配置，同时添加部分config块读取方法，
 */

public class ConfigUtil {
    private static final Logger logger = LoggerFactory.getLogger(ConfigUtil.class);

    private static Properties properties = null;
    private static String filePath;

    public static void init(String filePath) throws IOException {
        ConfigUtil.filePath = filePath;
//        if (properties != null) {
//            return;
//        }
        properties = new Properties();
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            properties.load(bufferedReader);
        } catch (IOException e) {
            logger.error("other exception:", e);
            throw new IOException();
        }
        try {
            LogbackConfigLoader.load(getProperty("log.settings"));
        } catch (Exception e) {
            logger.error("other exception:", e);
        }
    }

    public static String getConfigFile() {
        return filePath;
    }

    public static String getProperty(String propertyName) {
        String res = properties.getProperty(propertyName);
        if (res == null) {
            logger.error("key do not exist:" + propertyName);
            return null;
        }
        return res.trim();
    }

    public static boolean getSplitTag() {
        return Boolean.parseBoolean(getProperty("http.split"));
    }

    public static boolean getZipProperties() {
        return Boolean.parseBoolean(getProperty("http.zip"));
    }

    public static DbConfig getDbProperties() {
        DbConfig dbConfig = new DbConfig();
        String dbType = getProperty("db.type");
        dbConfig.setDbType(DbType.valueOf(dbType));
        dbConfig.setDriver(getProperty("db.driver"));
        dbConfig.setUrl(getProperty("db.url"));
        dbConfig.setUsername(getProperty("db.username"));
        dbConfig.setPassword(getProperty("db.password"));
        dbConfig.setMaxPoolSize("200");
        dbConfig.setMinIdle("20");
        dbConfig.setLeakDetectionThreshold("60000");
        return dbConfig;
    }

    public static int getPortElseDefault() {
        String strPort = getProperty("app.port");
        if (strPort != null) {
            return Integer.parseInt(strPort);
        }
        return Constant.DEFAULT_PORT;
    }

    public static boolean getJdChainAvailable(){
        return Boolean.parseBoolean(getProperty("jdchain.available"));
    }

    /**
    * @description: 加载jdchain的配置
    * @param
    * @return: com.jdt.fedlearn.coordinator.jdchain.JdChainConfig
    * @author: geyan29
    * @date: 2021/2/2 4:32 下午
    */
    public static JdChainConfig getJdChainConfig(){
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
        jdChainConfig.setUserTableAddress(getProperty("jdchain.user_table_address"));
        jdChainConfig.setTaskTableAddress(getProperty("jdchain.task_table_address"));
        jdChainConfig.setTrainTableAddress(getProperty("jdchain.train_table_address"));
        jdChainConfig.setInferenceTableAddress(getProperty("jdchain.inference_table_address"));
        return jdChainConfig;
    }

    public static String getPubKeyDir() {
        String pubKeyPath = getProperty("pubKey.dir");
        if (pubKeyPath != null) {
            return pubKeyPath;
        }
        return Constant.PUB_PATH;
    }
}
