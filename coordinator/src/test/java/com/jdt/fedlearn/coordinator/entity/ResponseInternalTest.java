package com.jdt.fedlearn.coordinator.entity;

import com.jdt.fedlearn.common.tool.internel.ResponseInternal;
import org.testng.annotations.Test;

public class ResponseInternalTest {
    @Test
    public void testParse(){
        String line = "{\"code\":1,\"status\":\"\", \"data\":\"{}\" }";
        ResponseInternal responseInternal = new ResponseInternal(line);
        System.out.println(responseInternal);
    }

    @Test
    public void testParse2(){
        String line = "{\"code\":0,\"data\":\"{\\\"ids\\\":[],\\\"client\\\":null}\",\"status\":\"success\"}";
        ResponseInternal responseInternal = new ResponseInternal(line);
        System.out.println(responseInternal);
    }

    @Test
    public void testParse3(){
        String line = "{\"code\":0,\"data\":\"{\\\"ids\\\":[],\\\"client\\\":null}\",\"status\":\"success\"}";
        ResponseInternal responseInternal = new ResponseInternal(line);
        System.out.println(responseInternal);
    }
}
