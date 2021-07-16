package com.jdt.fedlearn.coordinator.entity;

import com.jdt.fedlearn.coordinator.entity.train.SingleParameter;
import com.jdt.fedlearn.coordinator.entity.train.TrainContext;
import com.jdt.fedlearn.coordinator.service.prepare.AlgorithmParameterImpl;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class TrainContextTest {
    @Test
    public void extractSplitRatio(){
        List<SingleParameter> parameterList = new ArrayList<>();
        SingleParameter singleParameter = new SingleParameter(AlgorithmParameterImpl.CROSS_VALIDATION, 1);
        parameterList.add(singleParameter);

        TrainContext context = new TrainContext();
        float splitRatio = context.extractSplitRatio(parameterList);
        System.out.println(splitRatio);
        Assert.assertEquals(1.0F, splitRatio ,1e-8);
    }
}
