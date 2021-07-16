package com.jdt.fedlearn.client.dao;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.client.entity.HttpResp;
import org.testng.annotations.Test;

import java.util.*;

public class HttpReaderTest {


    public HttpReaderTest(){}
    @Test
    public void testHttp(){
//        String reponse = "{\"code\": 0,\"status\": \"success\",\"message\": \"success\",\"data\": {\"header\": [],\"result\": []}}";
//        String reponse ="{\"code\": 0,\"status\": \"success\",\"message\": \"success\",\"data\": {\"header\": [\"md042m\", \"md000g\"],\"result\": [{\"uid\": \"test\",\"feature\": [\"-1\", \"20\"]}, { \"uid\": \"test1\", \"feature\": [\"-1\", \"20\"]}]}}";
        String reponse ="{\"code\": 0,\"status\": \"success\",\"message\": \"success\",\"data\": {\"header\": [\"md042m\", \"md000g\"],\"result\": [{\"uid\": \"test\",\"feature\": [\"-1\", \"20\"]}, { \"uid\": \"test1\", \"feature\": [\"-1\", \"\"]}]}}";

        ObjectMapper mapper = new ObjectMapper();

        try {
            HttpReader httpReader = new HttpReader(){};
            final HttpResp response = mapper.readValue(reponse, HttpResp.class);
            String[][] data = httpReader.transerData(response);
//            String[][] data = transerData1(response);
            System.out.println("data: "  + Arrays.deepToString(data));
        }catch (Exception e){
            System.out.println("error: "+e);
        }
    }

    @Test
    public void testFetchData(){
        String[] header = new String[]{"uid","x1","x2","x3"};
        String[][] oriData = new String[][]{{"uid","x3","x1","x2"},{"0ab","0","1","2"},{"1ac","1","1","2"},{"2ad","1","2","2"}};
        HttpReader httpReader = new HttpReader(){};
        String[][] data = httpReader.dataFetch(header,oriData);
//        System.out.println("origData: " + Arrays.deepToString(oriData));
//        System.out.println("data: " + Arrays.deepToString(data));
    }

    @Test
    public void testFetchDataPer(){
        int num = 1000;
        long[] res = new long[num];
        for (int i = 0; i < num; i++){
            long start = System.currentTimeMillis();
            testFetchData();
            long end = System.currentTimeMillis();
            res[i] = end - start;
        }
        long maxNum = Arrays.stream(res).max().getAsLong();
        long minNum = Arrays.stream(res).min().getAsLong();
        double avgNum = Arrays.stream(res).average().getAsDouble();

        System.out.println("res  max : " + maxNum + " ms, res min : " + minNum + " ms, res avg : " + avgNum + " ms");
    }


}