package com.jdt.fedlearn.coordinator.entity.uniqueId;

import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.text.ParseException;

public class TestTrainId {

    @Test
    public void construct() {
        String taskId = "10";
        AlgorithmType algorithm = AlgorithmType.valueOf("FederatedGB");
        TrainId trainId = new TrainId(taskId, algorithm);
        Assert.assertEquals(trainId.getProjectId(), taskId);
        Assert.assertEquals(trainId.getAlgorithm(), AlgorithmType.FederatedGB);
        System.out.println("trainId : " + trainId.getTrainId());
    }

    @Test
    public void construct2() throws ParseException {
        String trainIdStr = "10-FederatedGB-210426113010";

        TrainId trainId = new TrainId(trainIdStr);
        Assert.assertEquals(trainId.getProjectId(), "10");
        Assert.assertEquals(trainId.getAlgorithm(), AlgorithmType.FederatedGB);
        Assert.assertEquals(trainId.getCreateTime(), UniqueId.df.get().parse("210426113010"));
    }
}
