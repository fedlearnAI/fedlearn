//package com.jdt.fedlearn.core.model;
//
//import com.jdt.fedlearn.core.encryption.common.Ciphertext;
//import com.jdt.fedlearn.core.encryption.common.EncryptionTool;
//import com.jdt.fedlearn.core.encryption.fake.FakeTool;
//import com.jdt.fedlearn.common.entity.core.Message;
//import com.jdt.fedlearn.core.entity.common.InferenceInitRes;
//import com.jdt.fedlearn.common.entity.core.feature.Features;
//import com.jdt.fedlearn.core.entity.mixGBoost.*;
//import com.jdt.fedlearn.core.fake.StructureGenerate;
//import com.jdt.fedlearn.core.loader.common.CommonLoad;
//import com.jdt.fedlearn.core.loader.common.InferenceData;
//import com.jdt.fedlearn.core.loader.mixGBoost.MixGBTrainData;
//import com.jdt.fedlearn.core.model.common.tree.MixTreeNode;
//import com.jdt.fedlearn.core.parameter.MixGBParameter;
//import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;
//import com.jdt.fedlearn.core.type.MessageType;
//import com.jdt.fedlearn.core.type.MetricType;
//import com.jdt.fedlearn.core.type.ObjectiveType;
//import com.jdt.fedlearn.core.type.data.DoubleTuple2;
//import com.jdt.fedlearn.core.type.data.StringTuple2;
//import com.jdt.fedlearn.core.type.data.Tuple3;
//import org.testng.Assert;
//import org.testng.annotations.Test;
//
//import java.util.*;
//
///**
// * @author zhangwenxi
// */
//public class TestMixGBModel {
//    @Test
//    public void testTrainInit() {
//        MixGBModel model = new MixGBModel(null, null, new MixTreeNode(1), new double[3], null);
//        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainInputStd();
//        String[][] raw = compoundInput._1().get();
//        System.out.println(raw.length);
//
//        String[] result = compoundInput._2().get();
//        System.out.println(Arrays.toString(result));
//        Features features = compoundInput._3().get();
//        Map<String, Object> others = new HashMap<>();
//        int[] testIndex = new int[0];
//
//        others.put("pubkey", "512");
//        others.put("commonId", new ArrayList<>(Arrays.asList(0, 1)));
//        MixGBTrainData data = model.trainInit(raw, result, testIndex, new MixGBParameter(), features, others);
//        Assert.assertEquals(data.getDatasetSize(), 4);
//        Assert.assertEquals(data.getFeatureDim(), 3);
//
//        MixGBParameter parameter = new MixGBParameter(1.0,1.0, 10, 1, 0, ObjectiveType.regLogistic,null, 3,2,0.3,33,0.6,"", 512);
//        data = model.trainInit(raw, result, testIndex, parameter, features, others);
//        Assert.assertEquals(data.getLabel(), new double[]{3.585, 2.578, 1.952,1.393});
//        parameter = new MixGBParameter(1.0,1.0, 10, 1, 0, ObjectiveType.multiSoftmax,null, 3,2, 0.3,33,0.6,"", 512);
//        data = model.trainInit(raw, result, testIndex, parameter, features, others);
//
//    }
//
//    @Test
//    public void testTrain() {
//        // has label
//        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainInputStd();
//        String[][] raw4 = compoundInput._1().get();
//        String[] result = compoundInput._2().get();
//        Features features = compoundInput._3().get();
//        int[] testIndex = new int[0];
//
//        String[][] raw = new String[4][];
//        for (int i = 0; i < 4; i ++) {
//            raw[i] = raw4[i];
//        }
//
//        Map<String, Object> others = new HashMap<>();
//        others.put("pubkey", "512");
//        others.put("commonId", new ArrayList<>(Arrays.asList(0, 1)));
//        EncryptionTool tool = new FakeTool();
//        Map<Integer, Tuple3<Integer, Ciphertext, Ciphertext>> ghEnc = new HashMap<>();
//        ghEnc.put(0, new Tuple3<>(1, tool.restoreCiphertext("10"), tool.restoreCiphertext("1.0")));
//        ghEnc.put(1, new Tuple3<>(1, tool.restoreCiphertext("20"), tool.restoreCiphertext("1.0")));
//        ghEnc.put(2, new Tuple3<>(1, tool.restoreCiphertext("30"), tool.restoreCiphertext("1.0")));
//
//        MixGBModel model = new MixGBModel(new ArrayList<>(), ghEnc, new MixTreeNode(1), null, null);
//        MixGBTrainData data = model.trainInit(raw, result, testIndex, new MixGBParameter(), features, others);
//
//        BoostBodyReq request = new BoostBodyReq(MessageType.EpochInit);
//        BoostBodyRes message = (BoostBodyRes) model.train(0, request, data);
//        StringTuple2[] gh = new StringTuple2[3];
//        gh[0] = new StringTuple2("-3.585", "1.0");
//        gh[1] = new StringTuple2("-2.578", "1.0");
//        gh[2] = new StringTuple2("-1.952", "1.0");
//
//        Assert.assertEquals(message.getMsgType(), MessageType.GiHi);
//        Assert.assertEquals(message.getInstId(), new int[]{0,1,2});
//        Assert.assertEquals(message.getGh(), gh);
//
//        BoostBodyReq request1 = new BoostBodyReq(MessageType.TreeInit);
//        BoostBodyRes message1 = (BoostBodyRes) model.train(0, request1, data);
//        Assert.assertEquals(message1.getMsgType(), MessageType.GiHi);
//        Assert.assertEquals(message1.getInstId(), new int[]{0,1,2});
//        Assert.assertEquals(message1.getGh(), gh);
//        Map<MetricType, DoubleTuple2> metric = new HashMap<>();
//        metric.put(MetricType.RMSE, new DoubleTuple2(23.308612999999998, 23.308612999999998));
//    }
//
//    @Test
//    public void testTrainGiHi() {
//        // no label
//        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainInputStdNoLabel();
//        String[][] raw = compoundInput._1().get();
//        String[] result = compoundInput._2().get();
//        Features features = compoundInput._3().get();
//        Map<String, Object> others = new HashMap<>();
//        others.put("pubkey", "512");
//        others.put("commonId", new ArrayList<>(Arrays.asList(0, 1)));
//        int[] testIndex = new int[]{0, 1, 2};
//
//        MixGBModel model = new MixGBModel(new ArrayList<>(), null, new MixTreeNode(1), null, null);
//        MixGBTrainData data = model.trainInit(raw, result, testIndex, new MixGBParameter(), features, others);
//
//        BoostBodyReq request = new BoostBodyReq(MessageType.GiHi);
//        BoostBodyRes message = (BoostBodyRes) model.train(0, request, data);
//        Assert.assertEquals(message.getMsgType(), MessageType.GiHi);
//        Assert.assertNull(message.getInstId());
//        Assert.assertNull(message.getGh());
//    }
//
//    @Test
//    public void testTrainUpdateGiHi() {
//        // has label
////        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainInputStd();
////
////        String[][] raw = compoundInput._1().get();
////        String[] result = compoundInput._2().get();
////        Features features = compoundInput._3().get();
////        Map<String, Object> others = new HashMap<>();
////        int[] testIndex = new int[0];
////
////        others.put("pubkey", "512");
////        others.put("commonId", new ArrayList<>(Arrays.asList(0, 1)));
////        EncryptionTool tool = new FakeTool();
////        Map<Integer, Tuple3<Integer, Ciphertext, Ciphertext>> ghEnc = new HashMap<>();
////        ghEnc.put(0, new Tuple3<>(1, tool.restoreCiphertext("10"), tool.restoreCiphertext("1.0")));
////        ghEnc.put(1, new Tuple3<>(1, tool.restoreCiphertext("20"), tool.restoreCiphertext("1.0")));
////        ghEnc.put(2, new Tuple3<>(1, tool.restoreCiphertext("30"), tool.restoreCiphertext("1.0")));
////
////        MixGBModel model = new MixGBModel(new ArrayList<>(), ghEnc, new MixTreeNode(1), null, null);
////        MixGBTrainData data = model.trainInit(raw, result, testIndex, new MixGBParameter(), features, others);
////
////        BoostBodyReq request = new BoostBodyReq(MessageType.UpdateGiHi);
////        StringTuple2[] gh = new StringTuple2[3];
////        gh[0] = new StringTuple2("-3.585", "1.0");
////        gh[1] = new StringTuple2("-2.578", "1.0");
////        gh[2] = new StringTuple2("-1.952", "1.0");
////        request.setGh(gh);
////        request.setCntList(new double[]{1, 1, 1});
////        request.setInstId(new int[]{0, 1, 2});
////        BoostBodyRes message = (BoostBodyRes) model.train(0, request, data);
////        Assert.assertEquals(message.getMsgType(), MessageType.UpdateGiHi);
//    }
//
//    @Test
//    public void testTrainHorizontalFeatureValue() {
//        // random feature
//        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainInputStd();
//        int[] testIndex = new int[0];
//
//        String[][] raw = compoundInput._1().get();
//        String[] result = compoundInput._2().get();
//        Features features = compoundInput._3().get();
//        Map<String, Object> others = new HashMap<>();
//        others.put("pubkey", "512");
//        others.put("commonId", new ArrayList<>(Arrays.asList(0, 1)));
//        MixTreeNode node = new MixTreeNode(1);
//        node.setInstanceIdSpaceSet(new HashSet<>(Arrays.asList(0, 1, 2)));
//        MixGBModel model = new MixGBModel(new ArrayList<>(), null, node, null, null);
//        MixGBTrainData data = model.trainInit(raw, result, testIndex, new MixGBParameter(), features, others);
//
//        BoostBodyReq request = new BoostBodyReq(MessageType.FeaturesSet);
//        request.setFeaturesSet(new String[]{"HouseAge", "Longitude"});
//        BoostBodyRes message = (BoostBodyRes) model.train(0, request, data);
//        Assert.assertEquals(message.getMsgType(), MessageType.FeatureValue);
//        Assert.assertEquals(message.getFvMap().size(), 2);
//        Assert.assertTrue(message.getFvMap().containsKey("Longitude"));
//        Assert.assertTrue(message.getFvMap().containsKey("HouseAge"));
//    }
//
//    @Test
//    public void testTrainHorizontalNoFeatureValue() {
//        // no feature
//        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainInputStdNoFeature();
//        int[] testIndex = new int[0];
//
//
//        String[][] raw = compoundInput._1().get();
//        String[] result = compoundInput._2().get();
//        Features features = compoundInput._3().get();
//        Map<String, Object> others = new HashMap<>();
//        others.put("pubkey", "512");
//        others.put("commonId", new ArrayList<>(Arrays.asList(0, 1)));
//
//        MixGBModel model = new MixGBModel(new ArrayList<>(), null, new MixTreeNode(1), null, null);
//        MixGBTrainData data = model.trainInit(raw, result, testIndex, new MixGBParameter(), features, others);
//
//        BoostBodyReq request = new BoostBodyReq(MessageType.FeaturesSet);
//        BoostBodyRes message = (BoostBodyRes) model.train(0, request, data);
//        Assert.assertEquals(message.getMsgType(), MessageType.FeatureValue);
//        Assert.assertNull(message.getFvMap());
//    }
//
//    @Test
//    public void testTrainHorizontalIL() {
//        // random feature
////        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainInputStd();
////        int[] testIndex = new int[0];
////
////        String[][] raw = compoundInput._1().get();
////        String[] result = compoundInput._2().get();
////        Features features = compoundInput._3().get();
////        Map<String, Object> others = new HashMap<>();
////        others.put("pubkey", "512");
////        others.put("commonId", new ArrayList<>(Arrays.asList(0, 1)));
////
////        MixTreeNode node = new MixTreeNode(1);
////        node.setInstanceIdSpaceSet(new HashSet<>(Arrays.asList(0, 1, 2)));
////        MixGBModel model = new MixGBModel(new ArrayList<>(), null, node, null, null);
////        MixGBTrainData data = model.trainInit(raw, result, testIndex, new MixGBParameter(), features, others);
////
////
////        BoostBodyReq request = new BoostBodyReq(MessageType.H_IL);
////        Map<String, Double> fVMap = new HashMap<>();
////        fVMap.put("AveOccup", 2.0);
////        fVMap.put("Longitude", -122.0);
////        request.setfVMap(fVMap);
////        BoostBodyRes message = (BoostBodyRes) model.train(0, request, data);
////        Assert.assertEquals(message.getMsgType(), MessageType.H_IL);
////        Map<String, Integer[]> tuple2IL = message.getFeaturesIL();
////        Assert.assertEquals(tuple2IL.size(), 2);
////        Assert.assertEquals(tuple2IL.get("AveOccup"), new Integer[]{1});
////        Assert.assertEquals(tuple2IL.get("Longitude"), new Integer[]{0, 1});
//    }
//
//    @Test
//    public void testTrainWj() {
//        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainInputStd();
//        int[] testIndex = new int[0];
//
//        String[][] raw = compoundInput._1().get();
//        String[] result = compoundInput._2().get();
//        Features features = compoundInput._3().get();
//        Map<String, Object> others = new HashMap<>();
//        others.put("pubkey", "512");
//        others.put("commonId", new ArrayList<>(Arrays.asList(0, 1)));
//        MixTreeNode node = new MixTreeNode(1);
//        node.setInstanceIdSpaceSet(new HashSet<>(Arrays.asList(0, 1, 2)));
//        MixGBModel model = new MixGBModel(new ArrayList<>(), null, node, new double[3], null);
//        MixGBTrainData data = model.trainInit(raw, result, testIndex, new MixGBParameter(), features, others);
//
//
//        BoostBodyReq request = new BoostBodyReq(MessageType.Wj);
//        request.setWj(0.5);
//        BoostBodyRes message = (BoostBodyRes) model.train(0, request, data);
//        Assert.assertEquals(message.getMsgType(), MessageType.Wj);
//    }
//
//    @Test
//    public void testTrainVerticalGkvHkvNoFeature() {
//        // no feature
//        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainInputStdNoFeature();
//        int[] testIndex = new int[0];
//
//        String[][] raw = compoundInput._1().get();
//        String[] result = compoundInput._2().get();
//        Features features = compoundInput._3().get();
//        Map<String, Object> others = new HashMap<>();
//        others.put("pubkey", "512");
//        others.put("commonId", new ArrayList<>(Arrays.asList(0, 1)));
//
//        MixGBModel model = new MixGBModel(new ArrayList<>(), null, new MixTreeNode(1), null, null);
//        MixGBTrainData data = model.trainInit(raw, result, testIndex, new MixGBParameter(), features, others);
//        BoostBodyReq request = new BoostBodyReq(MessageType.GkvHkv);
//        BoostBodyRes message = (BoostBodyRes) model.train(0, request, data);
//        Assert.assertEquals(message.getMsgType(), MessageType.GkvHkv);
//    }
//
//    @Test
//    public void testTrainVerticalGkvHkvNoCommonIds() {
//        // common Id size < params.minSampleSplit
//        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainInputStd();
//        int[] testIndex = new int[0];
//
//        String[][] raw = compoundInput._1().get();
//        String[] result = compoundInput._2().get();
//        Features features = compoundInput._3().get();
//        Map<String, Object> others = new HashMap<>();
//        others.put("pubkey", "512");
//        others.put("commonId", new ArrayList<>(Arrays.asList(0, 1)));
//
//        MixTreeNode node = new MixTreeNode(1);
//        node.setInstanceIdSpaceSet(new HashSet<>(Collections.singletonList(0)));
//        MixGBModel model = new MixGBModel(new ArrayList<>(), null, node, null, null);
//        MixGBTrainData data = model.trainInit(raw, result, testIndex, new MixGBParameter(), features, others);
//        BoostBodyReq request = new BoostBodyReq(MessageType.GkvHkv);
//        BoostBodyRes message = (BoostBodyRes) model.train(0, request, data);
//        Assert.assertEquals(message.getMsgType(), MessageType.GkvHkv);
//    }
//
//    @Test
//    public void testTrainVerticalGkvHkv() {
//        // normal vertical Gkv and Hkv computation
//        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainInputMissingValue();
//        int[] testIndex = new int[0];
//
//        String[][] raw4 = compoundInput._1().get();
//        String[] result = compoundInput._2().get();
//        Features features = compoundInput._3().get();
//
//        String[][] raw = new String[4][];
//        for (int i = 0; i < 4; i ++) {
//            raw[i] = raw4[i];
//        }
//
//        Map<String, Object> others = new HashMap<>();
//        others.put("pubkey", "512");
//        others.put("commonId", new ArrayList<>(Arrays.asList(0, 1, 2)));
//        // set min sample split = 1
//        MixGBParameter parameter = new MixGBParameter(1.0,1.0, 1, 1, 0, ObjectiveType.regLogistic,null, 3,2,0.3,1,0.6,"", 512);
//        EncryptionTool tool = new FakeTool();
//        Map<Integer, Tuple3<Integer, Ciphertext, Ciphertext>> ghEnc = new HashMap<>();
//        ghEnc.put(0, new Tuple3<>(1, tool.restoreCiphertext("10"), tool.restoreCiphertext("1.0")));
//        ghEnc.put(1, new Tuple3<>(1, tool.restoreCiphertext("20"), tool.restoreCiphertext("1.0")));
//        ghEnc.put(2, new Tuple3<>(1, tool.restoreCiphertext("30"), tool.restoreCiphertext("1.0")));
//        MixTreeNode node = new MixTreeNode(1);
//        node.setInstanceIdSpaceSet(new HashSet<>(Arrays.asList(0, 1, 2)));
//
//        MixGBModel model = new MixGBModel(new ArrayList<>(), ghEnc, node, null, null);
//        MixGBTrainData data = model.trainInit(raw, result, testIndex, parameter, features, others);
//        System.out.println(data.getDatasetSize());
//
//        BoostBodyReq request = new BoostBodyReq(MessageType.GkvHkv);
//        BoostBodyRes message = (BoostBodyRes) model.train(0, request, data);
//        Assert.assertEquals(message.getMsgType(), MessageType.GkvHkv);
////        StringTuple2[][] feaGlHlRes = message.getFeatureGlHl();
////        Assert.assertEquals(feaGlHlRes.length, 2);
//        int[] feaResIndex = data.getFeatureIndexList();
//        System.out.println("fea " + feaResIndex.length);
//
//        double[][] feaThres = data.getFeatureThresholdList();
//        double[][] samle = data.getSample();
//        for (double[] sa: samle) {
//            for (double c: sa) {
//                System.out.printf(c + " ");
//            }
//            System.out.println();
//        }
//        StringTuple2[][] correctGlHlRes = new StringTuple2[3][];
//        correctGlHlRes[0] = new StringTuple2[]{new StringTuple2("60.0", "3.0")};
//        correctGlHlRes[1] = new StringTuple2[]{};
//        correctGlHlRes[2] = new StringTuple2[]{new StringTuple2("40.0", "2.0"), new StringTuple2("60.0", "3.0")};
////        Assert.assertEquals(feaGlHlRes[0], correctGlHlRes[feaResIndex[0]]);
////        Assert.assertEquals(feaGlHlRes[1], correctGlHlRes[feaResIndex[1]]);
//    }
//
//    @Test
//    public void testTrainVerticalGVGain() {
//        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainInputStd();
//        int[] testIndex = new int[0];
//
//        String[][] raw = compoundInput._1().get();
//        String[] result = compoundInput._2().get();
//        Features features = compoundInput._3().get();
//        Map<String, Object> others = new HashMap<>();
//        others.put("pubkey", "512");
//        others.put("commonId", new ArrayList<>(Arrays.asList(0, 1)));
//
//        MixTreeNode node = new MixTreeNode(1);
//        node.setInstanceIdSpaceSet(new HashSet<>(Arrays.asList(0, 1, 2)));
//        MixGBModel model = new MixGBModel(new ArrayList<>(), null, node, null, null);
//        MixGBTrainData data = model.trainInit(raw, result, testIndex, new MixGBParameter(), features, others);
//
//        data.setFeatureIndexList(new int[]{2, 1, 0});
//        data.setFeatureThresholdList(new double[][]{new double[]{0, 2.5}, new double[]{0.2}, new double[]{0.3}});
//
//        BoostBodyReq request = new BoostBodyReq(MessageType.KVGain);
//        request.setK(0);
//        request.setV(1);
//        request.setGain(30.0);
//        BoostBodyRes message = (BoostBodyRes) model.train(0, request, data);
//        Assert.assertEquals(message.getMsgType(), MessageType.KVGain);
//
//        Map<String, Double> fVMap = new HashMap<>();
//        fVMap.put("AveOccup", 2.5);
//        Assert.assertEquals(message.getFvMap(), fVMap);
//        Assert.assertEquals(message.getInstId(), new int[]{0, 1});
//
//        data.setFeatureIndexList(new int[]{2, 1, 0});
//        data.setFeatureThresholdList(new double[][]{new double[]{0, 2.5}, new double[]{0.2}, new double[]{0.3}});
//        BoostBodyReq request1 = new BoostBodyReq(MessageType.KVGain);
//        request1.setK(0);
//        request1.setV(0);
//        request1.setGain(30.0);
//        message = (BoostBodyRes) model.train(0, request1, data);
//        Map<String, Double> fVMap1 = new HashMap<>();
//        fVMap1.put("AveOccup", 0.0);
//        Assert.assertEquals(message.getFvMap(), fVMap1);
//        Assert.assertEquals(message.getInstId().length, 0);
//    }
//
//    @Test
//    public void testTrainSplit() {
//        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainInputStd();
//        int[] testIndex = new int[0];
//
//        String[][] raw = compoundInput._1().get();
//        String[] result = compoundInput._2().get();
//        Features features = compoundInput._3().get();
//        Map<String, Object> others = new HashMap<>();
//        others.put("pubkey", "512");
//        others.put("commonId", new ArrayList<>(Arrays.asList(0, 1)));
//
//        MixTreeNode node = new MixTreeNode(1);
//        node.setInstanceIdSpaceSet(new HashSet<>(Arrays.asList(0, 1, 2)));
//        MixGBModel model = new MixGBModel(new ArrayList<>(), null, node, null, null);
//        MixGBTrainData data = model.trainInit(raw, result, testIndex, new MixGBParameter(), features, others);
//        MessageType[] messageTypes = new MessageType[2];
//        messageTypes[0] = MessageType.HorizontalSplit;
//        messageTypes[1] = MessageType.VerticalSplit;
//        for (MessageType type: messageTypes) {
//            BoostBodyReq request = new BoostBodyReq(type);
//            request.setInstId(new int[]{0, 1});
//            BoostBodyRes message = (BoostBodyRes) model.train(0, request, data);
//            Assert.assertEquals(message.getMsgType(), type);
//        }
//    }
//
//    @Test
//    public void testTrainFinalModel() {
//        Tuple3<String[][], String[], Features> compoundInput = StructureGenerate.trainInputStd();
//        int[] testIndex = new int[0];
//
//        String[][] raw4 = compoundInput._1().get();
//        String[] result = compoundInput._2().get();
//        Features features = compoundInput._3().get();
//        Map<String, Object> others = new HashMap<>();
//        others.put("pubkey", "512");
//        others.put("commonId", new ArrayList<>(Arrays.asList(0, 1)));
//
//        String[][] raw = new String[4][];
//        for (int i = 0; i < 4; i ++) {
//            raw[i] = raw4[i];
//            System.out.println(Arrays.toString(raw[i]));
//        }
//
//        MixTreeNode node1 = new MixTreeNode(1, -1);
//        MixTreeNode node2 = new MixTreeNode(2, 2);
//        MixTreeNode node3 = new MixTreeNode(3, 3);
//        MixTreeNode node4 = new MixTreeNode(4, 4);
//
//        Map<Integer, MixTreeNode> curRecordIdTreeNodeMap = new HashMap<>();
//        curRecordIdTreeNodeMap.put(1, node1);
//        curRecordIdTreeNodeMap.put(2, node2);
//        node2.setAsLeaf(1);
//        node2.setParent(node1);
//        curRecordIdTreeNodeMap.put(3, node3);
//        curRecordIdTreeNodeMap.put(4, node4);
//        EncryptionTool tool = new FakeTool();
//        Map<Integer, Tuple3<Integer, Ciphertext, Ciphertext>> ghEnc = new HashMap<>();
//        ghEnc.put(0, new Tuple3<>(1, tool.restoreCiphertext("10"), tool.restoreCiphertext("1.0")));
//        ghEnc.put(1, new Tuple3<>(1, tool.restoreCiphertext("20"), tool.restoreCiphertext("1.0")));
//        ghEnc.put(2, new Tuple3<>(1, tool.restoreCiphertext("30"), tool.restoreCiphertext("1.0")));
//        double[] predV = new double[]{0.1, 0.2, 0.3};
//
//        MixGBModel model = new MixGBModel(new ArrayList<>(), ghEnc, node1, predV, curRecordIdTreeNodeMap);
//        MixGBTrainData data = model.trainInit(raw, result, testIndex, new MixGBParameter(), features, others);
//
//        BoostBodyReq request = new BoostBodyReq(MessageType.EpochFinish);
//        request.setSaveNodes(new int[]{1, 2});
//        request.setDeleteNodes(new int[]{3, 4});
//        request.setInstId(new int[]{0, 1});
//        BoostBodyRes message = (BoostBodyRes) model.train(0, request, data);
//        Assert.assertEquals(message.getMsgType(), MessageType.MetricValue);
//    }
//
//    @Test
//    public void testInference() {
//        String content = "first_round_predict=0\n" +
//                "squareloss\n" +
//                "finalRecordIdTreeNodeMap\n" +
//                "recordId=1,depth=1,leaf=27.3802053742102270\n" +
//                "recordId=2,depth=1,leaf=19.1943707777968630\n" +
//                "recordId=3,depth=1,leaf=13.4558475555689370\n" +
//                "recordId=4,depth=1,splitFeatureType=1,splitFeatureName=x2,splitThreshold=0.5622296929359436,parentRecordId=-1,leftRecordId=5,rightRecordId=6\n" +
//                "recordId=5,depth=2,leaf=8.6931842621821640,parentRecordId=-1\n" +
//                "recordId=6,depth=2,leaf=10.2701544666010240,parentRecordId=-1\n" +
//                "recordId=7,depth=1,splitFeatureType=1,splitFeatureName=x1,splitThreshold=0.41782528162002563,parentRecordId=-1,leftRecordId=-1,rightRecordId=11\n" +
//                "recordId=11,depth=2,splitFeatureType=1,splitFeatureName=x2,splitThreshold=0.4836706221103668,parentRecordId=7,leftRecordId=12,rightRecordId=13\n" +
//                "recordId=12,depth=3,splitFeatureType=1,splitFeatureName=x2,splitThreshold=0.1,parentRecordId=11,leftRecordId=-1,rightRecordId=-1\n" +
//                "recordId=13,depth=3,leaf=7.5293233076657040,parentRecordId=11\n";
//        MixGBModel model = new MixGBModel();
//        model.deserialize(content);
//        String[][] data = new String[3][];
//        data[0] = new String[]{"uid", "x1", "x2"};
//        data[1] = new String[]{"aa", "10", "0.2"};
//        data[2] = new String[]{"1a", "0.1", "12.1"};
//        InferenceData inferenceData = CommonLoad.constructInference(AlgorithmType.MixGBoost, data);
//        BoostInferQueryReqBody req = new BoostInferQueryReqBody(new String[0], 18);
//        Message answer = model.inference(0, req, inferenceData);
//        Assert.assertNull(((BoostInferQueryRes) answer).getBodies());
//
//        BoostInferQueryReqBody req1 = new BoostInferQueryReqBody(new String[]{"aa", "1a"}, 11);
//        Message answer1 = model.inference(0, req1, inferenceData);
//        BoostInferQueryResBody[] queryReqBodies = ((BoostInferQueryRes) answer1).getBodies();
//        Assert.assertEquals(queryReqBodies.length, 2);
//        Assert.assertEquals(queryReqBodies[1].getInstanceId(), new String[]{"aa"});
//        Assert.assertEquals(queryReqBodies[1].getRecordId(), 12);
//        Assert.assertEquals(queryReqBodies[1].getValue(), 1);
//        Assert.assertEquals(queryReqBodies[0].getInstanceId(), new String[]{"1a"});
//        Assert.assertEquals(queryReqBodies[0].getRecordId(), -1);
//        Assert.assertEquals(queryReqBodies[0].getValue(), 7.5293233076657040);
//
//        BoostInferQueryReqBody req2 = new BoostInferQueryReqBody(new String[]{"aa", "1a"}, 7);
//        Message answer2 = model.inference(0, req2, inferenceData);
//        Assert.assertEquals(((BoostInferQueryRes) answer2).getBodies().length, 2);
//
//    }
//
//    @Test
//    public void testDeserialize() {
//        String content = "first_round_predict=0\n" +
//                "squareloss\n" +
//                "finalRecordIdTreeNodeMap\n" +
//                "recordId=1,depth=1,leaf=27.3802053742102270\n" +
//                "recordId=2,depth=1,leaf=19.1943707777968630\n" +
//                "recordId=3,depth=1,leaf=13.4558475555689370\n" +
//                "recordId=4,depth=1,splitFeatureType=1,splitFeatureName=x2,splitThreshold=0.5622296929359436,parentRecordId=-1,leftRecordId=5,rightRecordId=6\n" +
//                "recordId=5,depth=2,leaf=8.6931842621821640,parentRecordId=-1\n" +
//                "recordId=6,depth=2,leaf=10.2701544666010240,parentRecordId=-1\n" +
//                "recordId=7,depth=1,splitFeatureType=1,splitFeatureName=x1,splitThreshold=0.41782528162002563,parentRecordId=-1,leftRecordId=-1,rightRecordId=-1\n" +
//                "recordId=8,depth=2,splitFeatureType=1,splitFeatureName=x3,splitThreshold=0.08397725969552994,parentRecordId=-1,leftRecordId=9,rightRecordId=10\n" +
//                "recordId=9,depth=3,leaf=4.2775852887019380,parentRecordId=8\n" +
//                "recordId=10,depth=3,leaf=6.0629614916842150,parentRecordId=8\n" +
//                "recordId=11,depth=2,splitFeatureType=1,splitFeatureName=x2,splitThreshold=0.4836706221103668,parentRecordId=-1,leftRecordId=12,rightRecordId=13\n" +
//                "recordId=12,depth=3,leaf=6.3551207837939880,parentRecordId=11\n" +
//                "recordId=13,depth=3,leaf=7.5293233076657040,parentRecordId=11\n";
//        MixGBModel model = new MixGBModel();
//        model.deserialize(content);
//        String afterContent = model.serialize();
//        Assert.assertEquals(afterContent, content);
//    }
//
//    @Test
//    public void testInferenceInit() {
//        MixGBModel model = new MixGBModel();
//        String[] uidList = new String[]{"aa", "1a", "c3"};
//        String[][] data = new String[2][];
//        data[0] = new String[]{"aa", "10", "12.1"};
//        data[1] = new String[]{"1a", "10", "12.1"};
//        Message msg = model.inferenceInit(uidList, data ,new HashMap<>());
//        InferenceInitRes res = (InferenceInitRes) msg;
//        Assert.assertFalse(res.isAllowList());
//        Assert.assertEquals(res.getUid(), new int[]{2});
//    }
//
//    @Test
//    public void testGetModelType() {
//        MixGBModel model = new MixGBModel();
//        Assert.assertEquals(model.getModelType(), AlgorithmType.MixGBoost);
//    }
//}