package com.jdt.fedlearn.coordinator.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.common.entity.jdchain.JdChainConfig;
import com.jdt.fedlearn.coordinator.entity.DbConfig;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class ConfigUtilTest {

    @Test
    public void testgetSplitTag(){
        String filePath = "./src/test/resources/master.properties";
        try {
            ConfigUtil.init(filePath) ;
            boolean splitTag = ConfigUtil.getSplitTag();
            System.out.println("getSplitTag: "+ splitTag);
            assertEquals(splitTag,true);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void testGetZipProperties(){
        String filePath = "./src/test/resources/master.properties";
        try {
            ConfigUtil.init(filePath) ;
            boolean zipProperties = ConfigUtil.getZipProperties();
            System.out.println("getZipProperties: "+ zipProperties);
            assertEquals(zipProperties,true);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void testGetProperties(){
        String filePath = "./src/test/resources/master.properties";
        try {
            ConfigUtil.init(filePath) ;
            String prop = ConfigUtil.getProperty("prop");
            System.out.println("getProperty: "+ prop);
//            assertEquals(prop,true);
        }catch(Exception e){
            e.printStackTrace();
            Assert.assertEquals(e.getMessage(), "key do not exist:prop");
        }
    }

    @Test
    public void testGetProperties2(){
        String filePath = "./src/test/resources/master.properties";
        try {
            ConfigUtil.init(filePath) ;
            String prop = ConfigUtil.getProperty("db.type");
            System.out.println("db.type: "+ prop);
            assertEquals(prop, "sqlite");
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public  void testGetDbProperties(){
        String filePath = "./src/test/resources/master.properties";
        try {
            ConfigUtil.init(filePath) ;
            DbConfig dbConfig = ConfigUtil.getDbProperties();
            ObjectMapper mapper = new ObjectMapper();
            String strRes = mapper.writeValueAsString(dbConfig);
            System.out.println("dbConfig: " + mapper.writeValueAsString(dbConfig));
            assertEquals(strRes,"{\"dbType\":\"sqlite\",\"driver\":\"org.sqlite.JDBC\",\"url\":\"jdbc:sqlite:conf/fl.db\",\"username\":\"\",\"password\":\"\",\"maxPoolSize\":\"200\",\"minIdle\":\"20\",\"leakDetectionThreshold\":\"60000\"}");
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void testgetPortElseDefault(){
        String filePath = "./src/test/resources/master.properties";
        try {
            ConfigUtil.init(filePath) ;
            int portElseDefault = ConfigUtil.getPortElseDefault();
            System.out.println("portElseDefault: "+ portElseDefault);
            assertEquals(portElseDefault,8092);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void testGetJdChainAvailable() {
        String filePath = "./src/test/resources/master.properties";
        try {
            ConfigUtil.init(filePath) ;
            boolean jdChainAvailable = ConfigUtil.getJdChainAvailable();
            Assert.assertEquals(jdChainAvailable, false);

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void testGetJdChainConfig() {
        String filePath = "./src/test/resources/master.properties";
        try {
            ConfigUtil.init(filePath) ;
            JdChainConfig jdChainConfig = ConfigUtil.getJdChainConfig();

        }catch(Exception e){
            e.printStackTrace();
        }
    }

}