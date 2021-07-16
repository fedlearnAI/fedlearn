package com.jdt.fedlearn.coordinator.entity.metric;

import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class TestArrayMetric {


    @Test
    public void testGetX() {
        int x = 1;
        String y = "[[0.2,0.5]]";
        ArrayMetric arrayMetric = new ArrayMetric(x,y);
        int res = arrayMetric.getX();
        Assert.assertEquals(res,x);
    }

    @Test
    public void testGetY() {
        int x = 1;
        String y = "[[0.2,0.5]]";
        ArrayMetric arrayMetric = new ArrayMetric(x,y);
        String res = arrayMetric.getY();
        Assert.assertEquals(res,y);
    }

    @Test
    public void testRoundString() {
        int x = 1;
        String y = "[[0.2,0.5]]";
        ArrayMetric arrayMetric = new ArrayMetric(x,y);
        String res = arrayMetric.roundString();
        Assert.assertEquals(res,String.valueOf(x));
    }

    @Test
    public void testMetricString() {
        int x = 1;
        String y = "[[0.2,0.5]]";
        ArrayMetric arrayMetric = new ArrayMetric(x,y);
        String res = arrayMetric.metricString();
        Assert.assertEquals(res,y);
    }
}