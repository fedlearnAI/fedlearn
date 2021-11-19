package com.jdt.fedlearn.coordinator.entity.train;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jdt.fedlearn.tools.serializer.JsonUtil;
import com.jdt.fedlearn.common.exception.DeserializeException;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TrainProgressResQueryTest {

    @Test
    public void testTrainProgressQuery() {
        TrainParameterQuery trainParameterQuery = new TrainParameterQuery("1");
        String s = JsonUtil.object2json(trainParameterQuery);
        try {
            TrainParameterQuery trainParameterQuery1 = TrainParameterQuery.parseJson(s);
        } catch (DeserializeException | JsonProcessingException e) {
            Assert.assertEquals(e.getMessage(), "train query");
        }
        Assert.assertEquals(trainParameterQuery.getModelToken(), "1");

    }

}