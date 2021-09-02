package com.jdt.fedlearn.coordinator.entity.inference;

import org.testng.annotations.Test;

public class TestInferenceRequest {
    @Test
    public void parse(){
        String str= "{\"modelToken\":\"1-FederatedGB-210802162214\",\"uid\":[\"1\",\"2\",\"3\"],\"clientList\":[{\"url\":\"http://10.222.54.141:8094\",\"dataset\":\"reg0_train.csv\",\"features\":null},{\"url\":\"http://10.222.54.142:8094\",\"dataset\":\"reg1_train.csv\",\"features\":null},{\"url\":\"http://10.222.54.143:8094\",\"dataset\":\"reg2_train.csv\",\"features\":null}],\"secureMode\":false}";
        InferenceRequest inferenceRequest = new InferenceRequest(str);
        System.out.println(inferenceRequest.getModelToken());
    }
}
