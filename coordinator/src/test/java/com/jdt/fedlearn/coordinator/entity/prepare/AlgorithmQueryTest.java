package com.jdt.fedlearn.coordinator.entity.prepare;

import com.jdt.fedlearn.tools.serializer.JsonUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

public class AlgorithmQueryTest {

    @Test
    public void testALL() {
        AlgorithmQuery algorithmQuery = new AlgorithmQuery();
        AlgorithmQuery algorithmQuery1 = new AlgorithmQuery("FederatedGB", 1);
        String s = JsonUtil.object2json(algorithmQuery1);
        AlgorithmQuery algorithmQuery2 = new AlgorithmQuery(s);
        Assert.assertEquals(algorithmQuery2.getAlgorithmType(), "FederatedGB");
        Assert.assertEquals(algorithmQuery2.getTaskId(), (Integer) 1);
    }


}