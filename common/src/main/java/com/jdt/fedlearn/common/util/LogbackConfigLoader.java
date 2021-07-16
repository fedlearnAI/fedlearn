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
package com.jdt.fedlearn.common.util;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;


/**
 *加载logback配置文件
 */
public class LogbackConfigLoader {

    /**
     * 加载外部的logback配置文件
     * @param externalConfigFileLocation 配置文件路径
     * @throws IOException 未读取到文件
     * @throws JoranException 解析错误
     */
    public static void load(String externalConfigFileLocation) throws IOException, JoranException {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        File externalConfigFile = new File(externalConfigFileLocation);
        if (!externalConfigFile.exists()) {
            throw new IOException("Logback External Config File Parameter does not reference a file that exists");
        }
        if (!externalConfigFile.isFile()) {
            throw new IOException("Logback External Config File Parameter exists, but does not reference a file");
        }
        if (!externalConfigFile.canRead()) {
            throw new IOException("Logback External Config File exists and is a file, but cannot be read.");
        }
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(lc);
        lc.reset();
        configurator.doConfigure(externalConfigFileLocation);
        StatusPrinter.printInCaseOfErrorsOrWarnings(lc);
    }
}
