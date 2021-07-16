package com.jdt.fedlearn.core.parameter;

import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.ObjectiveType;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author zhangwenxi
 */
public class MixGBParameterTest {

    @Test
    public void testFetchMetric() {
        MetricType[] metrics = new MetricType[]{MetricType.MAPE};
        MixGBParameter parameter = new MixGBParameter(1.0,1.0, 10, 1, 0, ObjectiveType.regSquare,metrics, 3,2,1,0.3,33,0.6,"", 512);
        MetricType[] getmetrics = parameter.fetchMetric();
        Assert.assertEquals(getmetrics, metrics);
    }

    @Test
    public void testSerialize() {
        String content = "MixGBParameter{trainingEpoch=1, maxTreeNum=2, verticalFeatureSampling=1.0, maxBinNum=33, minSampleSplit=10, lambda=1.0, gamma=0.0, evalMetric=[MAPE], maxDepth=3, eta=0.3, objective='regSquare', evalDataRatio=0.6, evalIdRemoval=false, horizontalFeaturesRatio=1.0, needVerticalSplitRatio=0.0, numClass=0, bitLength=512, catFeatures=''}";
        MetricType[] metrics = new MetricType[]{MetricType.MAPE};
        MixGBParameter parameter = new MixGBParameter(1.0,1.0, 10, 1, 0, ObjectiveType.regSquare,metrics, 3,2,1,0.3,33,0.6,"", 512);
        Assert.assertEquals(parameter.serialize(), content);
    }

    @Test
    public void testGetMaxTreeNum() {
        MixGBParameter parameter = new MixGBParameter(1.0, 1.0, 10, 1, 0, ObjectiveType.regSquare, new MetricType[]{MetricType.MAPE}, 3, 5, 1, 0.3, 33, 0.6, "", 512);
        Assert.assertEquals(parameter.getMaxTreeNum(), 5);
    }

    @Test
    public void testGetTrainingEpoch() {
        MixGBParameter parameter = new MixGBParameter(1.0, 1.0, 10, 1, 0, ObjectiveType.regSquare, new MetricType[]{MetricType.MAPE}, 3, 5, 1, 0.3, 33, 0.6, "", 512);
        Assert.assertEquals(parameter.getTrainingEpoch(), 1);
    }

    @Test
    public void testGetMaxBinNum() {
        MixGBParameter parameter = new MixGBParameter(1.0, 1.0, 10, 1, 0, ObjectiveType.regSquare, new MetricType[]{MetricType.MAPE}, 3, 5, 1, 0.3, 33, 0.6, "", 512);
        Assert.assertEquals(parameter.getMaxBinNum(), 33);
    }

    @Test
    public void testGetMinSampleSplit() {
        MixGBParameter parameter = new MixGBParameter(1.0, 1.0, 10, 1, 0, ObjectiveType.regSquare, new MetricType[]{MetricType.MAPE}, 3, 5, 1, 0.3, 33, 0.6, "", 512);
        Assert.assertEquals(parameter.getMinSampleSplit(), 10);
    }

    @Test
    public void testGetMaxDepth() {
        MixGBParameter parameter = new MixGBParameter(1.0, 1.0, 10, 1, 0, ObjectiveType.regSquare, new MetricType[]{MetricType.MAPE}, 3, 5, 1, 0.3, 33, 0.6, "", 512);
        Assert.assertEquals(parameter.getMaxDepth(), 3);
    }

    @Test
    public void testGetVerticalFeatureSampling() {
        MixGBParameter parameter = new MixGBParameter(1.0, 1.0, 10, 1, 0, ObjectiveType.regSquare, new MetricType[]{MetricType.MAPE}, 3, 5, 1, 0.3, 33, 0.6, "", 512);
        Assert.assertEquals(parameter.getVerticalFeatureSampling(), 1.0);
    }

    @Test
    public void testGetLambda() {
        MixGBParameter parameter = new MixGBParameter(1.0, 1.0, 10, 1, 0, ObjectiveType.regSquare, new MetricType[]{MetricType.MAPE}, 3, 5, 1, 0.3, 33, 0.6, "", 512);
        Assert.assertEquals(parameter.getLambda(), 1);
    }

    @Test
    public void testGetGamma() {
        MixGBParameter parameter = new MixGBParameter(1.0, 1.0, 10, 1, 0, ObjectiveType.regSquare, new MetricType[]{MetricType.MAPE}, 3, 5, 1, 0.3, 33, 0.6, "", 512);
        Assert.assertEquals(parameter.getGamma(), 0);
    }

    @Test
    public void testGetEta() {
        MixGBParameter parameter = new MixGBParameter(1.0, 1.0, 10, 1, 0, ObjectiveType.regSquare, new MetricType[]{MetricType.MAPE}, 3, 5, 1, 0.3, 33, 0.6, "", 512);
        Assert.assertEquals(parameter.getEta(), 0.3);
    }

    @Test
    public void testGetEvalDataRatio() {
        MixGBParameter parameter = new MixGBParameter(1.0, 1.0, 10, 1, 0, ObjectiveType.regSquare, new MetricType[]{MetricType.MAPE}, 3, 5, 1, 0.3, 33, 0.6, "", 512);
        Assert.assertEquals(parameter.getEvalDataRatio(), 0.6);
    }

    @Test
    public void testGetHorizontalFeaturesRatio() {
        MixGBParameter parameter = new MixGBParameter(1.0, 1.0, 10, 1, 0, ObjectiveType.regSquare, new MetricType[]{MetricType.MAPE}, 3, 5, 1, 0.3, 33, 0.6, "", 512);
        Assert.assertEquals(parameter.getHorizontalFeaturesRatio(), 1.0);
    }

    @Test
    public void testGetNeedVerticalSplitRatio() {
        MixGBParameter parameter = new MixGBParameter(1.0, 1.0, 10, 1, 0, ObjectiveType.regSquare, new MetricType[]{MetricType.MAPE}, 3, 5, 1, 0.3, 33, 0.6, "", 512);
        Assert.assertEquals(parameter.getNeedVerticalSplitRatio(), 0);
    }

    @Test
    public void testGetEvalMetric() {
        MetricType[] metrics = new MetricType[]{MetricType.MAPE};
        MixGBParameter parameter = new MixGBParameter(1.0, 1.0, 10, 1, 0, ObjectiveType.regSquare, new MetricType[]{MetricType.MAPE}, 3, 5, 1, 0.3, 33, 0.6, "", 512);
        Assert.assertEquals(parameter.getEvalMetric(), metrics);
    }

    @Test
    public void testGetEvalIdRemoval() {
        MixGBParameter parameter = new MixGBParameter(1.0, 1.0, 10, 1, 0, ObjectiveType.regSquare, new MetricType[]{MetricType.MAPE}, 3, 5, 1, 0.3, 33, 0.6, "", 512);
        Assert.assertFalse(parameter.getEvalIdRemoval());
    }

    @Test
    public void testGetObjective() {
        MixGBParameter parameter = new MixGBParameter(1.0, 1.0, 10, 1, 0, ObjectiveType.regSquare, new MetricType[]{MetricType.MAPE}, 3, 5, 1, 0.3, 33, 0.6, "", 512);
        Assert.assertEquals(parameter.getObjective(), ObjectiveType.regSquare);
    }

    @Test
    public void testGetNumClass() {
        MixGBParameter parameter = new MixGBParameter(1.0, 1.0, 10, 1, 0, ObjectiveType.regSquare, new MetricType[]{MetricType.MAPE}, 3, 5, 1, 0.3, 33, 0.6, "", 512);
        Assert.assertEquals(parameter.getNumClass(), 0);
    }

    @Test
    public void testGetBitLength() {
        MixGBParameter parameter = new MixGBParameter(1.0, 1.0, 10, 1, 0, ObjectiveType.regSquare, new MetricType[]{MetricType.MAPE}, 3, 5, 1, 0.3, 33, 0.6, "", 512);
        Assert.assertEquals(parameter.getBitLength(), 512);
    }
}