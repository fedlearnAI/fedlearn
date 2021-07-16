package com.jdt.fedlearn.coordinator.entity.metric;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;

public class TestMetric {

    @Test
    public void testGetName() {
        String name = "RMSE";
        List<MetricPair> metricPairs = new ArrayList<>();
        metricPairs.add(new SingleMetric(1,0.5));
        Metric metric = new Metric(name,metricPairs);
        String res = metric.getName();
        Assert.assertEquals(res,name);
    }

    @Test
    public void testGetMetric() {
        String name = "RMSE";
        List<MetricPair> metricPairs = new ArrayList<>();
        metricPairs.add(new SingleMetric(1,0.5));
        Metric metric = new Metric(name,metricPairs);
        List<MetricPair> res = metric.getMetric();
        Assert.assertEquals(res,metricPairs);
    }
}