package com.jdt.fedlearn.client.entity;

import org.testng.annotations.Test;


public class HttpRespTest {
    @Test
    public void Test(){
        String reponse = "{\"code\": 0,\"status\": \"success\",\"message\": \"success\",\"data\": {\"header\": [],\"result\": [{}, {}]}}";
//        String reponse = "{\"code\": 0,\"status\": \"success\",\"message\": \"success\"}";
//        String reponse ="{\"code\": 0,\"status\": \"success\",\"message\": \"success\",\"data\": {\"header\": [\"md042m\", \"md000g\"],\"result\": [{\"uid\": \"test\",\"feature\": [\"-1\", \"20\"]}, { \"uid\": \"test1\", \"feature\": [\"-1\", \"20\"]}]}}";
        new HttpResp(reponse);
    }
}