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
package com.jdt.fedlearn.manager.util;

import ch.qos.logback.core.joran.spi.JoranException;
import com.jdt.fedlearn.common.util.LogbackConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;


/**
 * 配置文件相关加载
 */
public class ConfigUtil {
    private static final Logger logger = LoggerFactory.getLogger(ConfigUtil.class);
    /**
     * 配置文件必须参数
     */
    private static Properties properties = null;
    private static String confFilePath = "";

    public static void init(String filePath){
        confFilePath = filePath;
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
     * 获取workers的地址
     * @return
     */
    public static String getWorkerProperties() {
        return getProperty("workers.address");
    }

    /**
     * 获取默认的worker
     * @return
     */
    public static String getDefaultWorker() {
        return getProperty("default.worker");
    }

    /**
     * 从配置文件获取启动端口号
     * @return
     */
    public static int getPort() {
        try {
            String portStr = getProperty("app.port");
            return Integer.parseInt(portStr);
        } catch (Exception e) {
            throw new RuntimeException("未定义的app.port");
        }
    }

    /**
     * 返回配置文件路径
     * @return
     */
    public static String getConfigFile() {
        return confFilePath;
    }
}
