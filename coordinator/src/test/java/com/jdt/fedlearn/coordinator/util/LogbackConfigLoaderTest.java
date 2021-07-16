package com.jdt.fedlearn.coordinator.util;

import ch.qos.logback.core.joran.spi.JoranException;
import org.testng.annotations.Test;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class LogbackConfigLoaderTest {

    @Test
    public void testLoad() throws IOException, JoranException {
        String filePath = "./src/test/resources/coordinator.properties";
        Properties properties = new Properties();
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            properties.load(bufferedReader);
        } catch (IOException e) {
            throw new IOException();
        }
//        LogbackConfigLoader.load(ConfigUtil.getProperty("log.settings"));
        try {
            LogbackConfigLoader.load(ConfigUtil.getProperty("log.settings"));
        } catch (Exception e) {
            System.out.println("error :" + e);
        }
    }
}