package com.jdt.fedlearn.client.db;


import com.jdt.fedlearn.client.dao.MysqlReader;
import com.jdt.fedlearn.client.entity.source.DbSourceConfig;
import com.jdt.fedlearn.client.util.ConfigUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @className: DbTest
 * @description:
 * @author: geyan29
 * @date: 2020/11/11 11:16
 **/
public class DbTest {
    public static void main(String[] args) throws SQLException, Exception {
        int count = 2;
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(count);
        ConfigUtil.init("./src/main/assembly/conf/client.properties");
        for(int i=0;i<count;i++){
            int finalI = i;
            fixedThreadPool.submit(new Runnable() {
                @Override
                public void run() {
                    Connection connection = null;
                    try {
                        DbSourceConfig dbConfig = null;
                        if(finalI == 0){
                             dbConfig = new DbSourceConfig("com.mysql.jdbc.Driver","root","root",
                                    "jdbc:mysql://127.0.0.1:3306/nlp_test?characterEncoding=utf8&useSSL=false&autoReconnect=true","client_info");
                        }else{
                            dbConfig = new DbSourceConfig("com.mysql.jdbc.Driver","j_nlp","C1fFT0rqVn2u",
                                    "jdbc:mysql://10.222.46.113:3306/nlp?characterEncoding=utf8&useSSL=false&autoReconnect=true","client_info");
                        }
                        MysqlReader mysqlReader = new MysqlReader();
                        Arrays.stream(mysqlReader.loadHeader(dbConfig)).forEach(c -> System.out.println(c));
//                        Arrays.stream(mysqlReader.loadTrain(dbConfig)).forEach(c -> System.out.println(c));
                        System.out.println("=======================");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        fixedThreadPool.shutdown();
    }
}
