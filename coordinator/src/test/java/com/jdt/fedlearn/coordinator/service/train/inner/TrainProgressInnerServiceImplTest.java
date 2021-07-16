package com.jdt.fedlearn.coordinator.service.train.inner;

import com.jdt.fedlearn.coordinator.entity.metric.ArrayMetric;
import com.jdt.fedlearn.coordinator.entity.metric.Metric;
import com.jdt.fedlearn.coordinator.entity.metric.MetricPair;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;

public class TrainProgressInnerServiceImplTest {

    @Test
    public void testQueryTrainProgress() {
    }

    @Test
    public void testAddDesc() {
        TrainProgressInnerServiceImpl trainProgressInnerService = new TrainProgressInnerServiceImpl();
        List<String> res1 = trainProgressInnerService.addDesc(5, new ArrayList<>(), "1-FederatedGB");
        Assert.assertEquals(res1.size(), 2);
        List<String> res2 = trainProgressInnerService.addDesc(7, new ArrayList<>(), "1-FederatedGB");
        Assert.assertEquals(res2.size(), 3);
        List<Metric> metrics = new ArrayList<>();
        List<MetricPair> metric = new ArrayList<>();
        metric.add(new ArrayMetric(1, "0.55"));
        Metric metricRes = new Metric("AUC",  metric);
        metrics.add(metricRes);
        List<String> res3 = trainProgressInnerService.addDesc(100, metrics, "1-FederatedGB");
        Assert.assertEquals(res3.size(), 5);
        Assert.assertEquals(res3.get(4), "训练结束");
    }


}