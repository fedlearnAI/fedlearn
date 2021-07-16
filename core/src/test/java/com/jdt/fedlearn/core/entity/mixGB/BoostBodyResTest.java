package com.jdt.fedlearn.core.entity.mixGB;

import com.jdt.fedlearn.core.type.MessageType;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.data.DoubleTuple2;
import com.jdt.fedlearn.core.type.data.StringTuple2;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zhangwenxi
 */
public class BoostBodyResTest {

    @Test
    public void testFeaturesIL() {
        BoostBodyRes boostBodyRes = new BoostBodyRes(MessageType.EpochInit);
        Map<String, Integer[]> iLMap = new HashMap<>();
        boostBodyRes.setFeaturesIL(iLMap);
        Assert.assertEquals(boostBodyRes.getFeaturesIL(), iLMap);
    }

    @Test
    public void testEvalResult() {
        BoostBodyRes boostBodyRes = new BoostBodyRes(MessageType.EpochInit);
        boostBodyRes.setEvalResult(0);
        Assert.assertEquals(boostBodyRes.getEvalResult(), 0);
    }

    @Test
    public void testFeatureGlHl() {
        BoostBodyRes boostBodyRes = new BoostBodyRes(MessageType.EpochInit);
        StringTuple2[][] featureGh = new StringTuple2[2][1];
        featureGh[0] = new StringTuple2[]{new StringTuple2("fdasfdsa", "fdsafdsag")};
        featureGh[1] = new StringTuple2[]{new StringTuple2("dsa", "fdsg")};
        boostBodyRes.setFeatureGlHl(featureGh);
        Assert.assertEquals(boostBodyRes.getFeatureGlHl(), featureGh);
    }

    @Test
    public void testMsgType() {
        BoostBodyRes boostBodyRes = new BoostBodyRes(MessageType.EpochInit);
        Assert.assertEquals(boostBodyRes.getMsgType(), MessageType.EpochInit);
    }

    @Test
    public void testGh() {
        BoostBodyRes boostBodyRes = new BoostBodyRes(MessageType.EpochInit);
        StringTuple2[] gh = new StringTuple2[2];
        gh[0] = new StringTuple2("fdasfdsa", "fdsafdsag");
        gh[1] = new StringTuple2("dsa", "fdsg");
        boostBodyRes.setGh(gh);
        Assert.assertEquals(boostBodyRes.getGh(), gh);
    }

    @Test
    public void testInstId() {
        BoostBodyRes boostBodyRes = new BoostBodyRes(MessageType.EpochFinish);
        int[] id = new int[]{1,2,3};
        boostBodyRes.setInstId(id);
        Assert.assertEquals(boostBodyRes.getInstId(), id);
    }

    @Test
    public void testFvMap() {
        BoostBodyRes boostBodyRes = new BoostBodyRes(MessageType.EpochFinish);
        Map<String, Double> fv = new HashMap<>();
        fv.put("test", 0.0);
        boostBodyRes.setFvMap(fv);
        Assert.assertEquals(boostBodyRes.getFvMap(), fv);
    }

    @Test
    public void testBodies() {
        BoostBodyRes boostBodyRes = new BoostBodyRes(MessageType.EpochFinish);
        BoostInferQueryResBody[] bodies = new BoostInferQueryResBody[0];
        boostBodyRes.setBodies(bodies);
        Assert.assertEquals(boostBodyRes.getBodies(), bodies);
    }

    @Test
    public void testTrainMetric() {
        BoostBodyRes boostBodyRes = new BoostBodyRes(MessageType.EpochFinish);
        Map<MetricType, DoubleTuple2> metric = new HashMap<>();
        metric.put(MetricType.ACC, new DoubleTuple2(0.9, 0.98));
        boostBodyRes.setTrainMetric(metric);
        Assert.assertEquals(boostBodyRes.getTrainMetric(), metric);
    }
}