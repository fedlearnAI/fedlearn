package com.jdt.fedlearn.coordinator.db;


import com.jdt.fedlearn.coordinator.util.ConfigUtil;
import com.jdt.fedlearn.coordinator.util.DbUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @className: DbTest
 * @description:
 * @author: geyan29
 * @date: 2020/11/11 11:16
 **/
public class DbTest {
    private static final Logger logger = LoggerFactory.getLogger(DbTest.class);
    public static void main(String[] args) throws SQLException, IOException {
        int count = 40;
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(count);
        ConfigUtil.init("./src/main/assembly/conf/master.properties");
        for(int i=0;i<count;i++){
            fixedThreadPool.submit(new Runnable() {
                @Override
                public void run() {
                    Connection connection = null;
                    try {
                        connection = DbUtil.getConnection();
                        System.out.println(connection);
                        Thread.sleep(2000);
                        connection.close();
                    } catch (Exception e) {
                        logger.error("DB connection error: "+ e);
                    }
                }
            });
        }
    }
}
