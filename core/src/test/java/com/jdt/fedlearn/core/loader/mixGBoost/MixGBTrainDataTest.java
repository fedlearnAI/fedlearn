//package com.jdt.fedlearn.core.loader.mixGBoost;
//
//import com.jdt.fedlearn.common.entity.core.feature.Features;
//import com.jdt.fedlearn.core.fake.StructureGenerate;
//import com.jdt.fedlearn.core.type.data.Tuple2;
//import com.jdt.fedlearn.core.type.data.Tuple3;
//import org.testng.Assert;
//import org.testng.annotations.Test;
//
//import java.util.*;
//import java.util.stream.IntStream;
//
///**
// * @author zhangwenxi3
// */
//public class MixGBTrainDataTest {
//
//    @Test
//    public void testGetFeatureRandValue() {
//        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.mixgbTrainInputStd();
//        String[][] input = compoundInput._1().get();
//        String[] uids = compoundInput._2().get();
//        Features features = compoundInput._3().get();
//
//        MixGBTrainData data = new MixGBTrainData(input, uids, features, new ArrayList<>());
////        Tuple2<String, Double> res = data.getFeatureRandValue("HouseAge", new HashSet<Integer>(Arrays.asList(0, 1)));
////        Assert.assertTrue(res._2() <= 34);
////        Assert.assertTrue(res._2() >= 12);
//    }
//
//    @Test
//    public void testGetInstanceLabels() {
//        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.mixgbTrainInputStd();
//        String[][] input = compoundInput._1().get();
//        String[] uids = compoundInput._2().get();
//        Features features = compoundInput._3().get();
//
//        MixGBTrainData data = new MixGBTrainData(input, uids, features, new ArrayList<>());
//        double[] labels = data.getInstanceLabels(new int[]{0,1});
//        Assert.assertEquals(labels[0], 3.585);
//        Assert.assertEquals(labels[1], 2.578);
//    }
//
//    @Test
//    public void testGetLeftInstance() {
//        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.mixgbTrainInputStd();
//        String[][] input = compoundInput._1().get();
//        String[] uids = compoundInput._2().get();
//        Features features = compoundInput._3().get();
//
//        MixGBTrainData data = new MixGBTrainData(input, uids, features, new ArrayList<>());
//        double[] labels = data.getInstanceLabels(new int[]{0,1});
//        Assert.assertEquals(labels[0], 3.585);
//        Assert.assertEquals(labels[1], 2.578);
//    }
//
//    @Test
//    public void testTestGetLeftInstance() {
//        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.mixgbTrainInputStd();
//        String[][] input = compoundInput._1().get();
//        String[] uids = compoundInput._2().get();
//        Features features = compoundInput._3().get();
//
//        MixGBTrainData data = new MixGBTrainData(input, uids, features, new ArrayList<>());
//        int[] left = data.getLeftInstance(new int[]{0, 1, 2, 3}, "AveOccup", 2);
//        Assert.assertEquals(left, new int[]{1});
//    }
//
//    @Test
//    public void testTestGetLeftInstance1() {
//        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.mixgbTrainInputStd();
//        String[][] input = compoundInput._1().get();
//        String[] uids = compoundInput._2().get();
//        Features features = compoundInput._3().get();
//
//        MixGBTrainData data = new MixGBTrainData(input, uids, features, new ArrayList<>());
//        Integer[] left = data.getLeftInstance(new HashSet<>(Arrays.asList(0,1,2,3)), "AveOccup", 2);
//        Assert.assertEquals(left, new Integer[]{1});
//    }
//
//    @Test
//    public void testTestGetLeftInstance2() {
//        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.mixgbTrainInputStd();
//        String[][] input = compoundInput._1().get();
//        String[] uids = compoundInput._2().get();
//        Features features = compoundInput._3().get();
//
//        MixGBTrainData data = new MixGBTrainData(input, uids, features, new ArrayList<>());
//        Integer[] left = data.getLeftInstance(new HashSet<>(Arrays.asList(0,1,2,3)), 0, 2);
//        Assert.assertEquals(left, new Integer[0]);
//    }
//
//    @Test
//    public void testGetLeftInstanceForFeaSplit() {
//        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.mixgbTrainInputStd();
//        String[][] input = compoundInput._1().get();
//        String[] uids = compoundInput._2().get();
//        Features features = compoundInput._3().get();
//
//        MixGBTrainData data = new MixGBTrainData(input, uids, features, new ArrayList<>());
//        int[] left = data.getLeftInstanceForFeaSplit(new HashSet<>(Arrays.asList(0,1,2,3)), 0, 2, true);
//        Assert.assertEquals(left, new int[0]);
//    }
//
//    @Test
//    public void testGetUsageIdFeatureValueByIndex() {
//        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.mixgbTrainInputStd();
//        String[][] input = compoundInput._1().get();
//        String[] uids = compoundInput._2().get();
//        Features features = compoundInput._3().get();
//
//        MixGBTrainData data = new MixGBTrainData(input, uids, features, new ArrayList<>());
//        Map<Integer, Double> res = data.getUsageIdFeatureValueByIndex(new HashSet<>(Arrays.asList(0,1,2,3)), 0);
//        Map<Integer, Double> trueMap = new HashMap<>();
//        trueMap.put(0, 21.0);
//        trueMap.put(1, 29.0);
//        trueMap.put(2, 12.0);
//        trueMap.put(3, 34.0);
//        Assert.assertEquals(res, trueMap);
//    }
//
//    @Test
//    public void testSetLabel() {
//        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.mixgbTrainInputStd();
//        String[][] input = compoundInput._1().get();
//        String[] uids = compoundInput._2().get();
//        Features features = compoundInput._3().get();
//
//        MixGBTrainData data = new MixGBTrainData(input, uids, features, new ArrayList<>());
//        double[] label = new double[]{1.0,2.0,3.0,4.0};
//        data.setLabel(label);
//        Assert.assertEquals(data.getLabel(), label);
//    }
//
//    @Test
//    public void testHasLabel() {
//        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.mixgbTrainInputStd();
//        String[][] input = compoundInput._1().get();
//        String[] uids = compoundInput._2().get();
//        Features features = compoundInput._3().get();
//
//        MixGBTrainData data = new MixGBTrainData(input, uids, features, new ArrayList<>());
//        Assert.assertTrue(data.hasLabel());
//    }
//
//    @Test
//    public void testGetAllFeatureNamesToIndex() {
//        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.mixgbTrainInputStd();
//        String[][] input = compoundInput._1().get();
//        String[] uids = compoundInput._2().get();
//        Features features = compoundInput._3().get();
//
//        MixGBTrainData data = new MixGBTrainData(input, uids, features, new ArrayList<>());
//        Map<String, Integer> feaNameIndex = data.getAllFeatureNamesToIndex();
//        Map<String, Integer> truefeaNameIndex = new HashMap<>();
//        truefeaNameIndex.put("AveOccup", 2);
//        truefeaNameIndex.put("HouseAge", 0);
//        truefeaNameIndex.put("Longitude", 1);
//        Assert.assertEquals(feaNameIndex, truefeaNameIndex);
//    }
//
//    @Test
//    public void testGetFirstPredictValue() {
//        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.mixgbTrainInputStd();
//        String[][] input = compoundInput._1().get();
//        String[] uids = compoundInput._2().get();
//        Features features = compoundInput._3().get();
//
//        MixGBTrainData data = new MixGBTrainData(input, uids, features, new ArrayList<>());
//        double firstPredictValue = data.getFirstPredictValue();
//        Assert.assertEquals(firstPredictValue, 0);
//    }
//
//    @Test
//    public void testGetFeatureIndexList() {
//        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.mixgbTrainInputStd();
//        String[][] input = compoundInput._1().get();
//        String[] uids = compoundInput._2().get();
//        Features features = compoundInput._3().get();
//
//        MixGBTrainData data = new MixGBTrainData(input, uids, features, new ArrayList<>());
//        int[] feaIndex = data.getFeatureIndexList();
//        Assert.assertEquals(feaIndex, null);
//    }
//
//    @Test
//    public void testGetFeatureThresholdList() {
//        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.mixgbTrainInputStd();
//        String[][] input = compoundInput._1().get();
//        String[] uids = compoundInput._2().get();
//        Features features = compoundInput._3().get();
//
//        MixGBTrainData data = new MixGBTrainData(input, uids, features, new ArrayList<>());
//        double[][] feaIndex = data.getFeatureThresholdList();
//        Assert.assertEquals(feaIndex, null);
//    }
//
//    @Test
//    public void testGetLocalLabeledId() {
//        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.mixgbTrainInputStd();
//        String[][] input = compoundInput._1().get();
//        String[] uids = compoundInput._2().get();
//        Features features = compoundInput._3().get();
//
//        MixGBTrainData data = new MixGBTrainData(input, uids, features, new ArrayList<>());
//        List<Integer> indexMap = data.getLocalLabeledId();
//        List<Integer> trueMap = new ArrayList<>();
//        trueMap.add(0);
//        trueMap.add(1);
//        trueMap.add(2);
//        trueMap.add(3);
//        Assert.assertEquals(indexMap, trueMap);
//    }
//
//    @Test
//    public void testSetFeatureIndexList() {
//        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.mixgbTrainInputStd();
//        String[][] input = compoundInput._1().get();
//        String[] uids = compoundInput._2().get();
//        Features features = compoundInput._3().get();
//
//        MixGBTrainData data = new MixGBTrainData(input, uids, features, new ArrayList<>());
//        data.setFeatureIndexList(new int[0]);
//        Assert.assertEquals(data.getFeatureIndexList(), new int[0]);
//    }
//
//    @Test
//    public void testSetFeatureThresholdList() {
//        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.mixgbTrainInputStd();
//        String[][] input = compoundInput._1().get();
//        String[] uids = compoundInput._2().get();
//        Features features = compoundInput._3().get();
//
//        MixGBTrainData data = new MixGBTrainData(input, uids, features, new ArrayList<>());
//        data.setFeatureThresholdList(new double[0][]);
//        Assert.assertEquals(data.getFeatureThresholdList(), new double[0][]);
//    }
//
//    @Test
//    public void testGetFeatureMissValueInstIdMap() {
//        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.mixgbTrainInputStd();
//        String[][] input = compoundInput._1().get();
//        String[] uids = compoundInput._2().get();
//        Features features = compoundInput._3().get();
//
//        MixGBTrainData data = new MixGBTrainData(input, uids, features, new ArrayList<>());
//        Set<Integer>[] res = data.getFeatureMissValueInstIdMap();
//        HashSet[] trueRes = new HashSet[3];
//        Arrays.fill(trueRes, new HashSet<>());
//        Assert.assertEquals(res, trueRes);
//    }
//}