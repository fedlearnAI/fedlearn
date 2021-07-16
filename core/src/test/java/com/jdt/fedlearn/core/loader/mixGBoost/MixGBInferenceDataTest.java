package com.jdt.fedlearn.core.loader.mixGBoost;

import com.jdt.fedlearn.core.fake.StructureGenerate;
import com.jdt.fedlearn.core.type.data.Tuple3;
import org.testng.Assert;
import org.testng.annotations.Test;
import scala.util.parsing.combinator.testing.Str;

import java.util.*;

import static org.testng.Assert.*;

public class MixGBInferenceDataTest {
    @Test
    public void testConstruct() {
        Tuple3<String[][], String, String[]> compoundInput = StructureGenerate.inferenceInputStd();
        String[][] input = compoundInput._1().get();

        MixGBInferenceData data = new MixGBInferenceData(input);
        Map<String, Integer> idIndexMap = data.getIdIndexMap();

        Assert.assertEquals(idIndexMap.size(), 4);
        Assert.assertEquals(data.getFeatureDim(), 3);
        Assert.assertEquals(data.getDatasetSize(), 4);
        Assert.assertEquals(data.getUid(), new String[]{"1", "100", "10003", "8088"});
    }

    @Test
    public void testComputeUidIndex() {
        Tuple3<String[][], String, String[]> compoundInput = StructureGenerate.inferenceInputStd();
        String[][] input = compoundInput._1().get();
        MixGBInferenceData data = new MixGBInferenceData(input);
        String[] partId = new String[]{"10003", "8088", "1", "100"};
        data.computeUidIndex(partId);
        int[] fake = data.getFakeIdIndex();
        Assert.assertEquals(fake, new int[]{2, 3, 0, 1});
    }

    @Test
    public void testGetInstanceFeatureValue() {
        Tuple3<String[][], String, String[]> compoundInput = StructureGenerate.inferenceInputStd();
        String[][] input = compoundInput._1().get();
        MixGBInferenceData data = new MixGBInferenceData(input);
        double value = data.getInstanceFeatureValue("1", "Longitude");
        Assert.assertEquals(value, -122.22);
        double value1 = data.getInstanceFeatureValue("01", "Longitude");
        Assert.assertEquals(value1, Double.MAX_VALUE);
        double value2 = data.getInstanceFeatureValue("1", "no");
        Assert.assertEquals(value2, Double.MAX_VALUE);
    }

    @Test
    public void testTestGetInstanceFeatureValue() {
        Tuple3<String[][], String, String[]> compoundInput = StructureGenerate.inferenceInputStd();
        String[][] input = compoundInput._1().get();
        MixGBInferenceData data = new MixGBInferenceData(input);
        double[] value = data.getInstanceFeatureValue(new String[]{"1", "8088", "??"}, "Longitude");
        Assert.assertEquals(value.length, 3);
        Assert.assertEquals(value[0], -122.22);
        Assert.assertEquals(value[1], -118.21);
        Assert.assertEquals(value[2], Double.MAX_VALUE);
        double[] value1 = data.getInstanceFeatureValue(new String[]{"1", "8088", "??"}, "no");
        Assert.assertEquals(value1.length, 3);
        Assert.assertEquals(value1, new double[]{Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE});
    }

    @Test
    public void testGetLeftInstance() {
        Tuple3<String[][], String, String[]> compoundInput = StructureGenerate.inferenceInputStd();
        String[][] input = compoundInput._1().get();
        MixGBInferenceData data = new MixGBInferenceData(input);
        Set<String> ids = new HashSet<>(Arrays.asList("100", "8088"));
        Set<String> left = data.getLeftInstance(ids, "AveOccup", 2);
        Assert.assertEquals(left, new HashSet<>(Collections.singletonList("100")));
        Set<String> left1 = data.getLeftInstance(ids, "no", 2);
        Assert.assertEquals(left1.size(), 0);

    }
}