package com.jdt.fedlearn.core.parameter;

import com.jdt.fedlearn.core.parameter.common.ParameterField;
import com.jdt.fedlearn.core.type.EncryptionType;
import com.jdt.fedlearn.core.type.MetricType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.*;

public class RandomForestParameterTest {
    @Test
    public void testFetchMetric() {
        MetricType[] metrics = new MetricType[]{MetricType.MAPE};
        String loss = "Regression:MSE";
        RandomForestParameter parameter = new RandomForestParameter(
                2,
                5,
                1000,
                25,
                .8,
                30,
                30,
                "Null",
                10,
                EncryptionType.Javallier,
                metrics,
                loss,
                1024);;
        MetricType[] getmetrics = parameter.fetchMetric();
        Assert.assertEquals(getmetrics, metrics);
    }

    @Test
    public void testSerialize() {
        MetricType[] metrics = new MetricType[]{MetricType.MAPE};
        String loss = "Regression:MSE";
        RandomForestParameter parameter = new RandomForestParameter(
                2,
                5,
                1000,
                25,
                0.8,
                30,
                30,
                "Null",
                10,
                EncryptionType.Javallier,
                metrics,
                loss,
                1024);
        parameter.obtainPara();
    }

    @Test
    public void testAverageEpsilon(){
        MetricType[] metrics = new MetricType[]{MetricType.MAPE};
        String loss = "Regression:MSE";
        RandomForestParameter parameter = new RandomForestParameter(
                2,
                5,
                1000,
                25,
                0.8,
                30,
                30,
                "Null",
                10,
                EncryptionType.Javallier,
                metrics,
                loss,
                1024,
                "true",
                0.8);
        parameter.averageEpsilon();
        assertEquals(parameter.getDpEpsilon(), 0.8 / (2 * 30));
    }
}