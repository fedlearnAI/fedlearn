package com.jdt.fedlearn.coordinator.entity;

import com.jdt.fedlearn.coordinator.entity.common.Response;
import org.testng.annotations.Test;

public class ResponseTest {
    @Test
    public void testParse(){
        String line = "{\"code\":1,\"status\":\"\", \"data\":\"{}\" }";
        Response response = new Response(line);
        System.out.println(response);
    }

    @Test
    public void testParse2(){
        String line = "{\"code\":0,\"data\":\"{\\\"ids\\\":[],\\\"client\\\":null}\",\"status\":\"success\"}";
        Response response = new Response(line);
        System.out.println(response);
    }

    @Test
    public void testParse3(){
        String line = "{\"code\":0,\"data\":\"{\\\"ids\\\":[],\\\"client\\\":null}\",\"status\":\"success\"}";
        Response response = new Response(line);
        System.out.println(response);
    }
}
