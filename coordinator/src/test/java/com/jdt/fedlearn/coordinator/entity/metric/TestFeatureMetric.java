package com.jdt.fedlearn.coordinator.entity.metric;

import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class TestFeatureMetric {

    @Test
    public void testGetX() {
        String x = "fea1";
        double y = 0.1;
        FeatureMetric featureMetric = new FeatureMetric(x,y);
        String res = featureMetric.getX();
        Assert.assertEquals(res,x);
    }

    @Test
    public void testGetY() {
        String x = "fea1";
        double y = 0.1;
        FeatureMetric featureMetric = new FeatureMetric(x,y);
        double res = featureMetric.getY();
        Assert.assertEquals(res,y);
    }

    @Test
    public void testRoundString() {
        String x = "fea1";
        double y = 0.1;
        FeatureMetric featureMetric = new FeatureMetric(x,y);
        String res = featureMetric.roundString();
        Assert.assertEquals(res,x);
    }

    @Test
    public void testMetricString() {
        String x = "fea1";
        double y = 0.1;
        FeatureMetric featureMetric = new FeatureMetric(x,y);
        String res = featureMetric.metricString();
        Assert.assertEquals(res,String.valueOf(y));
    }

}