package com.jdt.fedlearn.core.entity.common;

import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.data.Pair;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;

import static org.testng.Assert.*;

public class MetricValueTest {

    @Test
    public void testToJson() {
        Map<MetricType, List<Pair<Integer, Double>>> metrics = new HashMap<>();
        List<Pair<Integer, Double>> pairList = new ArrayList<>();
        pairList.add(new Pair<>(1,0.8));
        pairList.add(new Pair<>(2,0.85));
        metrics.put(MetricType.ACC,pairList);
        MetricValue metricValue = new MetricValue(metrics);
        String metricValueStr = metricValue.toJson();
        System.out.println(metricValueStr);
        String res = "{\"metrics\":{\"ACC\":[{\"key\":1,\"value\":0.8},{\"key\":2,\"value\":0.85}]},\"metricsArr\":null,\"validateMetrics\":null,\"validateMetricsArr\":null,\"featureImportance\":null,\"bestRound\":0}";
        Assert.assertEquals(metricValueStr,res);
    }

    @Test
    public void testParseJson() {
        String metricValueStr = "{\"metrics\":{\"ACC\":[{\"key\":1,\"value\":0.8},{\"key\":2,\"value\":0.85}]},\"metricsArr\":null,\"validateMetrics\":null,\"validateMetricsArr\":null,\"featureImportance\":null,\"bestRound\":0}";
        MetricValue metricValue = MetricValue.parseJson(metricValueStr);
        Map<MetricType, List<Pair<Integer, Double>>> metrics = new HashMap<>();
        List<Pair<Integer, Double>> pairList = new ArrayList<>();
        pairList.add(new Pair<>(1,0.8));
        pairList.add(new Pair<>(2,0.85));
        metrics.put(MetricType.ACC,pairList);
        MetricValue metricValueEx = new MetricValue(metrics);
        Assert.assertEquals(metricValue,metricValueEx);
    }
}