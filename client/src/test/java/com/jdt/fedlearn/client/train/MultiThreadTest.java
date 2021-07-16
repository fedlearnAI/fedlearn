package com.jdt.fedlearn.client.train;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @className: MultiThreadTest
 * @description:
 * @author: geyan29
 * @date: 2020/12/15 9:38
 **/
public class MultiThreadTest {
    private static Logger logger = LoggerFactory.getLogger(MultiThreadTest.class);
    private static Map<String,String[][]> cacheFileMap = new ConcurrentHashMap<>(16);
    AtomicReference<Set> atomicStudent = new AtomicReference<Set>();
    private static final int threads = 5;

    public static void main(String[] args) {
        for (int i = 0; i < threads; i++) {
            String dataName = "cl" + i + "_train.csv";
//            String dataName = "cl0_train.csv";
            if( i == 3 || i==5 || i==7){
                dataName = "cl0_train.csv";
            }
            String finalDataName = dataName;
            Thread t = new Thread(() -> {
                try {
                    getTrainData("", finalDataName, null);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            t.start();
        }
    }

    /**
     * 根据文件名加载数据
     * @param modelToken
     * @param dataName
     * @param idMap
     */
    public static void getTrainData(String modelToken, String dataName, Map<String, String> idMap) throws IOException {
        if(!cacheFileMap.containsKey(dataName)){
            synchronized (dataName){
                if(!cacheFileMap.containsKey(dataName)){
                    logger.info("开始加载~{}",dataName);
                    String[][] cacheFile = loadFullData(dataName);
                    cacheFileMap.put(dataName,cacheFile);
                    logger.info("加载结束~{}",dataName);
                }
            }
        }
        logger.info("do something........{}",cacheFileMap.get(dataName));
    }

    /**
     * 一个耗时操作
     *
     * @param dataName
     * @return
     */
    private static String[][] loadFullData(String dataName) {
        logger.info("加载中~{}",dataName);
        try {
            Thread.sleep(5000l);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String[][] result = new String[1][10];
        result[0][0] = "1";
        result[0][1] = dataName;
        return result;
    }
}
