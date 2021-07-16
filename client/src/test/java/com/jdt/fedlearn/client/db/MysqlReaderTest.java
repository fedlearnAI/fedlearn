//package com.jdt.fedlearn.client.db;
//
//import com.jdt.fedlearn.client.dao.MysqlReader;
//import com.jdt.fedlearn.client.entity.source.DbSourceConfig;
//import com.jdt.fedlearn.client.util.ConfigUtil;
//import org.testng.annotations.Test;
//
//import java.util.Arrays;
//
//
//public class MysqlReaderTest {
//
//    @Test
//    public void main() throws Exception {
//        ConfigUtil.init("./src/test/resources/client.properties");
//        DbSourceConfig dbConfig = new DbSourceConfig("com.mysql.jdbc.Driver","j_nlp","C1fFT0rqVn2u",
//                    "jdbc:mysql://10.222.46.113:3306/nlp?characterEncoding=utf8&useSSL=false&autoReconnect=true","train_2_225");
//
//        MysqlReader mysqlReader = new MysqlReader();
//        Arrays.stream(mysqlReader.loadHeader(dbConfig)).forEach(System.out::println);
//
//        Arrays.stream(mysqlReader.loadTrain(dbConfig)).forEach(System.out::println);
////                        Arrays.stream(mysqlReader.loadTrain(dbConfig)).forEach(c -> System.out.println(c));
//        System.out.println("=======================");
//
//    }
//}
