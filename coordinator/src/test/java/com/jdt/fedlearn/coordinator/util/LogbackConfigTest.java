package com.jdt.fedlearn.coordinator.util;


import ch.qos.logback.core.joran.spi.JoranException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Date;

public class LogbackConfigTest {

    @Test
    public void test1() throws IOException, JoranException {
        LogbackConfigLoader.load("src/test/resources/logback-test.xml");
        Logger logger = LoggerFactory.getLogger(LogbackConfigLoader.class);
        logger.debug("现在的时间是 {}", new Date().toString());
        logger.info(" This time is {}", new Date().toString());
        logger.warn(" This time is {}", new Date().toString());
        logger.error(" This time is {}", new Date().toString());
//        @SuppressWarnings("unused")
//        int n = 1 / 0;
    }
}
