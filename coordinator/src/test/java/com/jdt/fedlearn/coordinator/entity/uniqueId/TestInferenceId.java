package com.jdt.fedlearn.coordinator.entity.uniqueId;


import org.testng.Assert;
import org.testng.annotations.Test;

import java.text.ParseException;


public class TestInferenceId {
    @Test
    public void construct() throws ParseException {
        TrainId modelId = new TrainId("10-FederatedGB-21042601113010");
        InferenceId inferenceId = new InferenceId(modelId);
        String inferStr = inferenceId.getInferenceId();
        System.out.println("inferenceId: " + inferStr);
        Assert.assertEquals(inferenceId.getModelId(), modelId);
    }

    @Test
    public void construct2() throws ParseException {
        String inferId = "10-FederatedGB-210426020110-5fb9c073";
        InferenceId inferenceId= new InferenceId(inferId);
        TrainId trainId = new TrainId("10-FederatedGB-210426020110");
        Assert.assertEquals(inferenceId.getModelId().getTrainId(), trainId.getTrainId());
        Assert.assertEquals(inferenceId.getInferenceId(), inferId);
//        Assert.assertEquals(mappingId.getCreateTime(), UniqueId.df.parse("20110201113010"));
    }

}