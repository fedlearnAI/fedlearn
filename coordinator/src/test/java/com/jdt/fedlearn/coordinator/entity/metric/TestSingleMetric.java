package com.jdt.fedlearn.coordinator.entity.metric;

import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class TestSingleMetric {

    @Test
    public void testGetX() {
        int x = 1;
        double y = 0.5;
        SingleMetric singleMetric = new SingleMetric(x,y);
        int res = singleMetric.getX();
        Assert.assertEquals(res,x);
    }

    @Test
    public void testGetY() {
        int x = 1;
        double y = 0.5;
        SingleMetric singleMetric = new SingleMetric(x,y);
        double res = singleMetric.getY();
        Assert.assertEquals(res,y);
    }

    @Test
    public void testRoundString() {
        int x = 1;
        double y = 0.5;
        SingleMetric singleMetric = new SingleMetric(x,y);
        String res = singleMetric.roundString();
        Assert.assertEquals(res,String.valueOf(x));
    }

    @Test
    public void testMetricString() {
        int x = 1;
        double y = 0.5;
        SingleMetric singleMetric = new SingleMetric(x,y);
        String res = singleMetric.metricString();
        Assert.assertEquals(res,String.valueOf(y));
    }
}