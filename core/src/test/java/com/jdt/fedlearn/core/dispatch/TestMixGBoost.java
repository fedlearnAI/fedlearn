//package com.jdt.fedlearn.core.dispatch;
//
//import com.jdt.fedlearn.common.entity.core.ClientInfo;
//import com.jdt.fedlearn.common.entity.core.Message;
//import com.jdt.fedlearn.core.entity.base.SingleElement;
//import com.jdt.fedlearn.core.entity.common.*;
//import com.jdt.fedlearn.common.entity.core.feature.Features;
//import com.jdt.fedlearn.core.entity.mixGBoost.*;
//import com.jdt.fedlearn.core.fake.StructureGenerate;
//import com.jdt.fedlearn.core.math.MathExt;
//import com.jdt.fedlearn.core.model.common.tree.MixTreeNode;
//import com.jdt.fedlearn.core.parameter.MixGBParameter;
//import com.jdt.fedlearn.core.psi.MatchResult;
//import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;
//import com.jdt.fedlearn.core.type.MessageType;
//import com.jdt.fedlearn.core.type.MetricType;
//import com.jdt.fedlearn.core.type.ObjectiveType;
//import com.jdt.fedlearn.core.type.data.*;
//import com.jdt.fedlearn.core.util.Tool;
//import org.testng.Assert;
//import org.testng.annotations.BeforeMethod;
//import org.testng.annotations.Test;
//
//import java.util.*;
//import java.util.stream.Collectors;
//import java.util.stream.IntStream;
//
///**
// * @author zhangwenxi
// */
//public class TestMixGBoost {
//   private List<ClientInfo> clientInfos;
//   private Map<String, Set<ClientInfo>> allFeatures;
//   private Map<Integer, IntDoubleTuple3> dupIdGHMap;
//   private Map<MetricType, List<Pair<Integer, Double>>> metricMap;
//
//   @BeforeMethod
//   public void setUp() {
//      clientInfos = StructureGenerate.threeClients();
//      allFeatures = new HashMap<>();
//      allFeatures.put("fea1", new HashSet<>(clientInfos));
//      allFeatures.put("fea2", new HashSet<>(clientInfos));
//      allFeatures.put("fea3", new HashSet<>(clientInfos));
//      allFeatures.put("fea4", new HashSet<>(clientInfos));
//      dupIdGHMap = new HashMap<>();
//      dupIdGHMap.put(0, new IntDoubleTuple3(2,0,1));
//      dupIdGHMap.put(1, new IntDoubleTuple3(2,1,1));
//      dupIdGHMap.put(2, new IntDoubleTuple3(2,2,1));
//      List<Pair<Integer, Double>> tmpRoundMetric = new ArrayList<>();
//      metricMap = new HashMap<>();
//      metricMap.put(MetricType.ACC, tmpRoundMetric);
//   }
//
//   @Test
//   public void testInitControl() {
//      MatchResult matchResult = new MatchResult(10);
//
//      Map<ClientInfo, Features> features = StructureGenerate.mixGbFeatures(clientInfos);
//
//      MixGBParameter[] mixGBParameters = new MixGBParameter[4];
//      mixGBParameters[0] = new MixGBParameter();
//      mixGBParameters[1] = new MixGBParameter(1.0,1.0, 10, 1, 0, ObjectiveType.regLogistic,null, 3,2,0.3,33,0.6,"", 512);
//      mixGBParameters[2] = new MixGBParameter(1.0,1.0, 10, 1, 0, ObjectiveType.multiSoftmax,null, 3,2,0.3,33,0.6,"", 512);
//      mixGBParameters[3] = new MixGBParameter(1.0,1.0, 10, 1, 0, ObjectiveType.binaryLogistic,null, 3,2,0.3,33,0.6,"", 512);
//      for (MixGBParameter parameter: mixGBParameters) {
//         MixGBoost mixGBoost = new MixGBoost(parameter);
//         List<CommonRequest> requests = mixGBoost.initControl(clientInfos, matchResult, features, new HashMap<>());
//         Assert.assertEquals(clientInfos.size(), requests.size());
//         CommonRequest first = requests.get(0);
//         Assert.assertEquals(first.getPhase(), 0);
//         Assert.assertFalse(first.isSync());
//         Message message = first.getBody();
//         TrainInit body = (TrainInit) message;
//         Assert.assertEquals(body.getFeatureList(), features.get(clientInfos.get(0)));
//      }
//   }
//
//   @Test
//   public void testResultControl() {
//      MixGBoost mixGBoost = new MixGBoost(null, dupIdGHMap, metricMap, clientInfos, null, null, null, null);
//
//      Map<MetricType, DoubleTuple2> metricRes0 = new HashMap<>();
//      metricRes0.put(MetricType.ACC, new DoubleTuple2(0.9, 0.99));
//      Map<MetricType, DoubleTuple2> metricRes1 = new HashMap<>();
//      metricRes1.put(MetricType.ACC, new DoubleTuple2(0.9, 0.99));
//      Map<MetricType, DoubleTuple2> metricRes2 = new HashMap<>();
//      metricRes2.put(MetricType.ACC, new DoubleTuple2(0.9, 0.99));
//
//      BoostBodyRes[] boostBodyRes = new BoostBodyRes[3];
//      boostBodyRes[0] = new BoostBodyRes(MessageType.MetricValue);
////      boostBodyRes[0].setTrainMetric(metricRes0);
//      boostBodyRes[1] = new BoostBodyRes(MessageType.MetricValue);
////      boostBodyRes[1].setTrainMetric(metricRes1);
//      boostBodyRes[2] = new BoostBodyRes(MessageType.MetricValue);
////      boostBodyRes[2].setTrainMetric(metricRes2);
//
//      List<CommonResponse> responses = IntStream.range(0, 3)
//              .mapToObj(i -> new CommonResponse(clientInfos.get(i), boostBodyRes[i]))
//              .collect(Collectors.toList());
//
//      // phase number doesn't matter
//      List<CommonRequest> requests = mixGBoost.control(responses);
//      List<Pair<Integer, Double>> metricValue = mixGBoost.readMetrics().getMetrics().get(MetricType.ACC);
//      double value = metricValue.get(0).getValue();
//      Assert.assertEquals(value, 0.9);
//      Assert.assertEquals(clientInfos.size(), requests.size());
//      boolean isContinue = mixGBoost.isContinue();
//      Assert.assertFalse(isContinue);
//   }
//
//   @Test
//   public void testControlTreeInitInfo() {
//      MixGBoost mixGBoost = new MixGBoost(null, null, null, clientInfos, null, null, null, null);
//      List<CommonResponse> resList = new ArrayList<>();
//      for (ClientInfo clientInfo: clientInfos) {
//         BoostBodyRes res = new BoostBodyRes(MessageType.Wj);
//         CommonResponse commonResponse = new CommonResponse(clientInfo, res);
//         resList.add(commonResponse);
//      }
//      List<CommonRequest> requests = mixGBoost.control(resList);
//      Assert.assertEquals(clientInfos.size(), requests.size());
//      CommonRequest first = requests.get(0);
//      Message message = first.getBody();
//      BoostBodyReq req = (BoostBodyReq) message;
//      MessageType msg = req.getMsgType();
//      Assert.assertEquals(msg,MessageType.TreeInit);
//   }
//
//   @Test
//   public void testControlEpochInitInfo() {
//      MixGBoost mixGBoost = new MixGBoost(new MixGBParameter());
//
//      List<CommonResponse> responses = new ArrayList<>();
//      clientInfos.forEach(client -> responses.add(new CommonResponse(client, new SingleElement("init_success"))));
//      List<CommonRequest> requests = mixGBoost.control(responses);
//      System.out.println(requests.size() );
//      Assert.assertEquals(clientInfos.size(), requests.size());
//      CommonRequest first = requests.get(0);
//      Message message = first.getBody();
//      BoostBodyReq req = (BoostBodyReq) message;
//      MessageType msg = req.getMsgType();
//      Assert.assertEquals(msg, MessageType.GlobalInit);
//   }
//
//   @Test
//   public void testControlUpdateGiHi() {
////      Map<MetricType, List<Pair<Integer, Double>>> metricMap = new HashMap<>();
////      List<Pair<Integer, Double>> tmpRoundMetric = new ArrayList<>();
////      tmpRoundMetric.add(new Pair<>(1, -Double.MAX_VALUE));
////      metricMap.put(MetricType.ACC, tmpRoundMetric);
////      List<MixTreeNode> treeNodeList = new ArrayList<>();
////      treeNodeList.add(new MixTreeNode(1));
////      MixTreeNode node = new MixTreeNode(1);
////      treeNodeList.add(node);
////      MixGBoost mixGBoost = new MixGBoost(null, new HashMap<>(), metricMap, clientInfos, null, node, treeNodeList, null);
////      List<CommonResponse> responses = new ArrayList<>();
////      // no GH updated
////      responses.add(new CommonResponse(clientInfos.get(0), new BoostBodyRes(MessageType.GiHi)));
////      List<CommonRequest> requests = mixGBoost.control(responses);
////
////      // normal GH processing
////      Map<MetricType, DoubleTuple2> metric = new HashMap<>();
////      metric.put(MetricType.ACC, new DoubleTuple2(0.8, 0.8));
////      Map<MetricType, DoubleTuple2> metric1 = new HashMap<>();
////      metric1.put(MetricType.ACC, new DoubleTuple2(0.9, 0.9));
////      int[] instId = new int[]{1,2};
////      int[] instId1 = new int[]{1,2};
////      StringTuple2[] ghCouple = new StringTuple2[2];
////      StringTuple2[] ghCouple1 = new StringTuple2[2];
////      ghCouple[0] = new StringTuple2("-22", "1.0");
////      ghCouple[1] = new StringTuple2("-16", "1.0");
////      ghCouple1[0] = new StringTuple2("-20", "1.0");
////      ghCouple1[1] = new StringTuple2("-18", "1.0");
////      BoostBodyRes res0 = new BoostBodyRes(MessageType.GiHi);
////      res0.setGh(ghCouple);
////      res0.setInstId(instId);
////      res0.setTrainMetric(metric);
////      BoostBodyRes res1 = new BoostBodyRes(MessageType.GiHi);
////      res1.setGh(ghCouple1);
////      res1.setInstId(instId1);
////      res1.setTrainMetric(metric1);
////      responses.add(new CommonResponse(clientInfos.get(1), res0));
////      responses.add(new CommonResponse(clientInfos.get(2), res1));
////      requests = mixGBoost.control(responses);
////      Assert.assertEquals(requests.size(), responses.size());
////      BoostBodyReq req = (BoostBodyReq) requests.get(0).getBody();
////      Assert.assertEquals(req.getMsgType(), MessageType.UpdateGiHi);
////      Assert.assertEquals(req.getInstId(), instId);
////      Assert.assertEquals(req.getCntList(), new double[]{2.0, 2.0});
////      StringTuple2[] ghRes = new StringTuple2[2];
////      ghRes[0] = new StringTuple2("-21.0", "1.0");
////      ghRes[1] = new StringTuple2("-17.0", "1.0");
////      Assert.assertEquals(req.getGh(), ghRes);
//   }
//
//   @Test
//   public void testStartNode() {
//      MixGBoost mixGBoost = new MixGBoost(null, null, null, clientInfos, null, null, null, null);
//      List<CommonResponse> responses = new ArrayList<>();
//      responses.add(new CommonResponse(clientInfos.get(0), new BoostBodyRes(MessageType.UpdateGiHi)));
//      // curTreeNode = null
//      List<CommonRequest> requests = mixGBoost.control(responses);
//      Assert.assertEquals(requests.size(), responses.size());
//      CommonRequest first = requests.get(0);
//      Message message = first.getBody();
//      BoostBodyReq req = (BoostBodyReq) message;
//      MessageType msg = req.getMsgType();
//      Assert.assertEquals(msg, MessageType.EpochInit);
//   }
//
//   @Test
//   public void testAfterLeafIntoStartNode() {
//      MixTreeNode node = new MixTreeNode(1);
//      node.setSplitFeatureType(1);
//      node.setInstanceIdSpaceSet(new HashSet<>(Arrays.asList(0, 1, 2, 3, 4)));
//      node.setNodeGH(new DoubleTuple2(10, 5));
//      MixGBParameter parameter = new MixGBParameter(1.0,1.0, 1, 1, 0, ObjectiveType.regLogistic,null, 3,2,0.3,33,0.6,"", 512);
//
//      Set<Integer> commonIdSet = new HashSet<>(Arrays.asList(0, 1));
//
//      MixGBoost mixGBoost = new MixGBoost(parameter, dupIdGHMap, null, clientInfos, null, node, null, commonIdSet);
//      List<CommonResponse> responses = new ArrayList<>();
//      responses.add(new CommonResponse(clientInfos.get(0), new BoostBodyRes(MessageType.Wj)));
//      // curTreeNode = null
//      List<CommonRequest> requests = mixGBoost.control(responses);
//      Assert.assertEquals(requests.size(), clientInfos.size());
//      CommonRequest first = requests.get(0);
//      Message message = first.getBody();
//      BoostBodyReq req = (BoostBodyReq) message;
//      MessageType msg = req.getMsgType();
//      Assert.assertEquals(msg, MessageType.GkvHkv);
//   }
//
//   @Test
//   public void testControlActuallyDealWithWj() {
//      MixTreeNode node = new MixTreeNode(2);
//      node.setNodeGH(new DoubleTuple2(200, 4));
//      node.setInstanceIdSpaceSet(new HashSet<>(Arrays.asList(0, 1, 2, 3)));
//      MixTreeNode parNode = new MixTreeNode(1);
//      parNode.setInstanceIdSpaceSet(new HashSet<>(Arrays.asList(0, 1, 2, 3)));
//      node.setParent(parNode);
//      parNode.setLeftChild(node);
//      MixGBoost mixGBoost = new MixGBoost(null, null, null, clientInfos, null, node, null, null);
//      List<CommonResponse> responses = new ArrayList<>();
//      responses.add(new CommonResponse(clientInfos.get(0), new BoostBodyRes(MessageType.UpdateGiHi)));
//      List<CommonRequest> requests = mixGBoost.control(responses);
//      Assert.assertEquals(requests.size(), clientInfos.size());
//      CommonRequest first = requests.get(0);
//      Message message = first.getBody();
//      BoostBodyReq req = (BoostBodyReq) message;
//      MessageType msg = req.getMsgType();
//      Assert.assertEquals(msg, MessageType.Wj);
//      Assert.assertEquals(req.getWj(), -4);
//   }
//
//   @Test
//   public void testControlHorizontalFeatureSet() {
//      MixTreeNode node = new MixTreeNode(1);
//      node.setSplitFeatureType(0);
//      node.setInstanceIdSpaceSet(new HashSet<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)));
//      MixGBoost mixGBoost = new MixGBoost(null, null, null, clientInfos, allFeatures, node, null, null);
//      List<CommonResponse> responses = new ArrayList<>();
//      responses.add(new CommonResponse(clientInfos.get(0), new BoostBodyRes(MessageType.UpdateGiHi)));
//      List<CommonRequest> requests = mixGBoost.control(responses);
//      Assert.assertEquals(requests.size(), clientInfos.size());
//      CommonRequest first = requests.get(0);
//      Message message = first.getBody();
//      BoostBodyReq req = (BoostBodyReq) message;
//      MessageType msg = req.getMsgType();
//      Assert.assertEquals(msg, MessageType.FeaturesSet);
//      Set<String> features = Arrays.stream(req.getFeaturesSet()).collect(Collectors.toSet());
//      Set<String>  correctFeatures = allFeatures.keySet();
//      Assert.assertEquals(features, correctFeatures);
//   }
//
//   @Test
//   public void testGetAlgorithmType() {
//      MixGBoost mixGBoost = new MixGBoost(new MixGBParameter());
//      AlgorithmType type = mixGBoost.getAlgorithmType();
//      Assert.assertEquals(type, AlgorithmType.MixGBoost);
//   }
//
//   @Test
//   public void testControlHorizontalIL() {
//      Map<String, Double> fvMap = new HashMap<>();
//      fvMap.put("fea1", 1.0);
//      fvMap.put("fea2", 2.0);
//      fvMap.put("fea3", 3.0);
//      Map<String, Double> fvMap1 = new HashMap<>();
//      fvMap1.put("fea1", 2.0);
//      fvMap1.put("fea2", 7.0);
//      fvMap1.put("fea3", 5.0);
//      fvMap1.put("fea4", 6.0);
//      MixGBoost mixGBoost = new MixGBoost(null, null, null, clientInfos, null, null, null, null);
//      List<CommonResponse> responses = new ArrayList<>();
//      BoostBodyRes boostBodyRes1 = new BoostBodyRes(MessageType.FeatureValue);
//      boostBodyRes1.setFvMap(fvMap);
//      responses.add(new CommonResponse(clientInfos.get(0), boostBodyRes1));
//      BoostBodyRes boostBodyRes2 = new BoostBodyRes(MessageType.FeatureValue);
//      boostBodyRes1.setFvMap(fvMap1);
//      responses.add(new CommonResponse(clientInfos.get(1), boostBodyRes2));
//      List<CommonRequest> requests = mixGBoost.control(responses);
//      Assert.assertEquals(requests.size(), responses.size());
//      BoostBodyReq req = (BoostBodyReq) requests.get(0).getBody();
//      Assert.assertEquals(req.getMsgType(), MessageType.H_IL);
////      Assert.assertEquals(req.getfVMap().size(), 4);
//   }
//
//   @Test
//   public void testHorizontalFeatureSplitAll() {
////      MixTreeNode node = new MixTreeNode(1);
////      node.setSplitFeatureType(0);
////      node.setInstanceIdSpaceSet(new HashSet<>(Arrays.asList(0, 1, 2, 3, 4)));
////      node.setNodeGH(new DoubleTuple2(10, 5));
////
////      Set<Integer>[] commonIdSets = new HashSet[3];
////      commonIdSets[0] = new HashSet<>(node.getInstanceIdSpaceSet());
////      commonIdSets[1] = new HashSet<>(Arrays.asList(0, 1));
////      commonIdSets[2] = new HashSet<>(Arrays.asList(0, 1, 2));
////
////      for (Set<Integer> commonIdSet: commonIdSets) {
////         MixGBoost mixGBoost = new MixGBoost(null, dupIdGHMap, null, clientInfos, allFeatures, node, null, commonIdSet);
////         List<CommonResponse> responses = new ArrayList<>();
////         BoostBodyRes boostBodyRes1 = new BoostBodyRes(MessageType.H_IL);
////         Map<String, Integer[]> featuresIL1 = new HashMap<>();
////         featuresIL1.put("fea1", new Integer[]{0});
////         boostBodyRes1.setFeaturesIL(featuresIL1);
////         BoostBodyRes boostBodyRes2 = new BoostBodyRes(MessageType.H_IL);
////         Map<String, Integer[]> featuresIL2 = new HashMap<>();
////         featuresIL2.put("fea1", new Integer[]{1, 2});
////         featuresIL2.put("fea2", new Integer[]{4});
////         boostBodyRes2.setFeaturesIL(featuresIL2);
////         responses.add(new CommonResponse(clientInfos.get(0), boostBodyRes1));
////         responses.add(new CommonResponse(clientInfos.get(1), boostBodyRes2));
////         List<CommonRequest> requests = mixGBoost.control(responses);
////         Assert.assertEquals(requests.size(), clientInfos.size());
////         BoostBodyReq req = (BoostBodyReq) requests.get(0).getBody();
////         Assert.assertEquals(req.getMsgType(), MessageType.GkvHkv);
////      }
//   }
//
//   @Test
//   public void testNeedVerticalControlTrue() {
//      MixTreeNode node = new MixTreeNode(2);
//      node.setNodeGH(new DoubleTuple2(60, 3));
//      node.setInstanceIdSpaceSet(new HashSet<>(Arrays.asList(0, 1)));
//      MixGBParameter parameter = new MixGBParameter(1.0,1.0, 1, 1, 0, ObjectiveType.regLogistic,null, 3,2,0.3,33,0.6,"", 512);
//      Set<Integer> commonIdSet = new HashSet<>(Arrays.asList(0, 1));
//
//      MixGBoost mixGBoost = new MixGBoost(parameter, null, null, clientInfos, null, node, null, commonIdSet);
//      List<CommonResponse> responses = new ArrayList<>();
//      BoostBodyRes boostBodyRes = new BoostBodyRes(MessageType.VerticalSplit);
//      responses.add(new CommonResponse(clientInfos.get(0), boostBodyRes));
//
//      List<CommonRequest> requests = mixGBoost.control(responses);
//      Assert.assertEquals(requests.size(), clientInfos.size());
//      BoostBodyReq req = (BoostBodyReq) requests.get(0).getBody();
//      Assert.assertEquals(req.getMsgType(), MessageType.GkvHkv);
//   }
//
//   @Test
//   public void testNeedVerticalControlFalse() {
//      MixTreeNode node = new MixTreeNode(2);
//      node.setNodeGH(new DoubleTuple2(60, 3));
//      node.setInstanceIdSpaceSet(new HashSet<>(Arrays.asList(0, 1, 2, 3, 4)));
//      MixGBParameter parameter = new MixGBParameter(1.0,1.0, 1, 1, 0, ObjectiveType.regLogistic,null, 3,2,0.3,33,0.6,"", 512, 1);
//      Set<Integer> commonIdSet = new HashSet<>(Arrays.asList(0, 1));
//      Map<String, Set<ClientInfo>> allFeatures = new HashMap<>();
//      allFeatures.put("fea1", new HashSet<>(Collections.singletonList(clientInfos.get(0))));
//      allFeatures.put("fea2", new HashSet<>(clientInfos));
//      allFeatures.put("fea3", new HashSet<>(clientInfos));
//      allFeatures.put("fea4", new HashSet<>(clientInfos));
//      MixGBoost mixGBoost = new MixGBoost(parameter, null, null, clientInfos, allFeatures, node, null, commonIdSet);
//      List<CommonResponse> responses = new ArrayList<>();
//      BoostBodyRes boostBodyRes = new BoostBodyRes(MessageType.VerticalSplit);
//      responses.add(new CommonResponse(clientInfos.get(0), boostBodyRes));
//
//      List<CommonRequest> requests = mixGBoost.control(responses);
//      Assert.assertEquals(requests.size(), clientInfos.size());
//      BoostBodyReq req = (BoostBodyReq) requests.get(0).getBody();
//      Assert.assertEquals(req.getMsgType(), MessageType.FeaturesSet);
//   }
//
//   @Test
//   public void testControlVerticalKVGain() {
//      // normal featureGlHl
//      MixTreeNode node = new MixTreeNode(2);
//      node.setNodeGH(new DoubleTuple2(60, 3));
//      node.setInstanceIdSpaceSet(new HashSet<>(Arrays.asList(0, 1, 2)));
//      StringTuple2[][] featureGlHl = new StringTuple2[2][];
//      featureGlHl[0] = new StringTuple2[]{new StringTuple2("20", "1")};
//      featureGlHl[1] = new StringTuple2[]{new StringTuple2("30", "1"), new StringTuple2("40", "2")};
//
//      MixGBoost mixGBoost = new MixGBoost(null, null, null, clientInfos, null, node, null, null);
//      List<CommonResponse> responses = new ArrayList<>();
//      BoostBodyRes boostBodyRes0 = new BoostBodyRes(MessageType.GkvHkv);
////      boostBodyRes0.setFeatureGlHl(featureGlHl);
//      responses.add(new CommonResponse(clientInfos.get(0), boostBodyRes0));
//
//      List<CommonRequest> requests = mixGBoost.control(responses);
//      Assert.assertEquals(requests.size(), clientInfos.size());
//      BoostBodyReq req = (BoostBodyReq) requests.get(0).getBody();
//      Assert.assertEquals(req.getMsgType(), MessageType.Wj);
//      Assert.assertEquals(req.getWj(), -1.5);
//   }
//
//   @Test
//   public void testControlHorizontalSplitIntoWj() {
//      // null featureGlHl for ControlVerticalKVGain, gain < gamma
//      MixTreeNode node = new MixTreeNode(2);
//      node.setNodeGH(new DoubleTuple2(60, 3));
//      node.setInstanceIdSpaceSet(new HashSet<>(Arrays.asList(0, 1, 2)));
//      MixGBParameter parameter = new MixGBParameter(1.0,1.0, 10, 1, 0, ObjectiveType.regLogistic,null, 3,2,0.3,33,0.6,"", 512);
//
//      MixGBoost mixGBoost = new MixGBoost(parameter, null, null, clientInfos, null, node, null, null);
//      List<CommonResponse> responses = new ArrayList<>();
//      BoostBodyRes boostBodyRes0 = new BoostBodyRes(MessageType.GkvHkv);
//      responses.add(new CommonResponse(clientInfos.get(0), boostBodyRes0));
//
//      List<CommonRequest> requests = mixGBoost.control(responses);
//      Assert.assertEquals(requests.size(), clientInfos.size());
//      BoostBodyReq req = (BoostBodyReq) requests.get(0).getBody();
//      Assert.assertEquals(req.getMsgType(), MessageType.Wj);
//      Assert.assertEquals(req.getWj(), -4.5);
//   }
//
//   @Test
//   public void testControlHorizontalSplit() {
//      // null featureGlHl for ControlVerticalKVGain
//      MixTreeNode node = new MixTreeNode(2);
//      node.setNodeGH(new DoubleTuple2(60, 3));
//      node.setInstanceIdSpaceSet(new HashSet<>(Arrays.asList(0, 1, 2)));
//      node.setTempHorizontalGain(10);
//      node.setSplitFeatureName("fea1");
//      MixGBParameter parameter = new MixGBParameter(1.0,1.0, 10, 1, -100, ObjectiveType.regLogistic,null, 3,2,0.3,33,0.6,"", 512);
//      Map<String, Set<ClientInfo>> allFeatures = new HashMap<>();
//      allFeatures.put("fea1", new HashSet<>(Collections.singletonList(clientInfos.get(0))));
//      allFeatures.put("fea2", new HashSet<>(clientInfos));
//      allFeatures.put("fea3", new HashSet<>(clientInfos));
//      allFeatures.put("fea4", new HashSet<>(clientInfos));
//
//      MixGBoost mixGBoost = new MixGBoost(parameter, null, null, clientInfos, allFeatures, node, null, null);
//      List<CommonResponse> responses = new ArrayList<>();
//      BoostBodyRes boostBodyRes0 = new BoostBodyRes(MessageType.GkvHkv);
//      responses.add(new CommonResponse(clientInfos.get(0), boostBodyRes0));
//
//      List<CommonRequest> requests = mixGBoost.control(responses);
//      Assert.assertEquals(requests.size(), clientInfos.size());
//      BoostBodyReq req = (BoostBodyReq) requests.get(0).getBody();
//      Assert.assertEquals(req.getMsgType(), MessageType.HorizontalSplit);
//      Assert.assertEquals(req.getFeatureName(), "fea1");
//   }
//
//   @Test
//   public void testVerticalSplitFetchIL() {
//      // null featureGlHl for ControlVerticalKVGain
//      MixTreeNode node = new MixTreeNode(2);
//      node.setNodeGH(new DoubleTuple2(60, 3));
//      node.setInstanceIdSpaceSet(new HashSet<>(Arrays.asList(0, 1, 2)));
//      StringTuple2[][] featureGlHl = new StringTuple2[2][];
//      featureGlHl[0] = new StringTuple2[]{new StringTuple2("20", "1")};
//      featureGlHl[1] = new StringTuple2[]{new StringTuple2("30", "1"), new StringTuple2("40", "2")};
//      MixGBParameter parameter = new MixGBParameter(1.0,1.0, 10, 1, -100, ObjectiveType.regLogistic,null, 3,2,0.3,33,0.6,"", 512);
//
//      MixGBoost mixGBoost = new MixGBoost(parameter, null, null, clientInfos, null, node, null, null);
//      List<CommonResponse> responses = new ArrayList<>();
//      BoostBodyRes boostBodyRes0 = new BoostBodyRes(MessageType.GkvHkv);
////      boostBodyRes0.setFeatureGlHl(featureGlHl);
//      responses.add(new CommonResponse(clientInfos.get(0), boostBodyRes0));
//
//      List<CommonRequest> requests = mixGBoost.control(responses);
//      Assert.assertEquals(requests.size(), 1);
//      BoostBodyReq req = (BoostBodyReq) requests.get(0).getBody();
//      Assert.assertEquals(req.getMsgType(), MessageType.KVGain);
//      Assert.assertEquals(req.getK(), 0);
//      Assert.assertEquals(req.getV(), -1.0);
//      Assert.assertEquals(req.getGain(), 0);
//   }
//
//   @Test
//   public void testControlVerticalSplit() {
//      MixTreeNode node = new MixTreeNode(2);
//      node.setClient(clientInfos.get(0));
//      node.setInstanceIdSpaceSet(new HashSet<>(Arrays.asList(0, 1, 2)));
//
//      MixGBoost mixGBoost = new MixGBoost(null, dupIdGHMap, null, clientInfos, null, node, null, null);
//      List<CommonResponse> responses = new ArrayList<>();
//      BoostBodyRes boostBodyRes0 = new BoostBodyRes(MessageType.KVGain);
//      Map<String, Double> featuresValueSet = new HashMap<>();
//      featuresValueSet.put("fea1", 1.0);
//      boostBodyRes0.setFvMap(featuresValueSet);
//      int[] left = new int[]{0, 1};
//      boostBodyRes0.setInstId(left);
//      responses.add(new CommonResponse(clientInfos.get(0), boostBodyRes0));
//
//      List<CommonRequest> requests = mixGBoost.control(responses);
//      Assert.assertEquals(requests.size(), 2);
//      BoostBodyReq req = (BoostBodyReq) requests.get(0).getBody();
//      Assert.assertEquals(req.getMsgType(), MessageType.VerticalSplit);
//      Assert.assertEquals(req.getInstId(), left);
//   }
//
//   @Test
//   public void testControlVerticalSplitIntoStartNode() {
//      MixTreeNode node = new MixTreeNode(2);
//      node.setClient(clientInfos.get(0));
//      node.setInstanceIdSpaceSet(new HashSet<>(Arrays.asList(0, 1, 2)));
//
//      List<ClientInfo> singleClient = new ArrayList<>(clientInfos);
//      singleClient.remove(2);
//      singleClient.remove(1);
//      MixGBoost mixGBoost = new MixGBoost(null, dupIdGHMap, null, singleClient, null, node, null, null);
//      List<CommonResponse> responses = new ArrayList<>();
//      BoostBodyRes boostBodyRes0 = new BoostBodyRes(MessageType.KVGain);
//      Map<String, Double> featuresValueSet = new HashMap<>();
//      featuresValueSet.put("fea1", 1.0);
//      boostBodyRes0.setFvMap(featuresValueSet);
//      int[] left = new int[]{0, 1};
//      boostBodyRes0.setInstId(left);
//      responses.add(new CommonResponse(clientInfos.get(0), boostBodyRes0));
//
//      List<CommonRequest> requests = mixGBoost.control(responses);
//      Assert.assertEquals(requests.size(), 1);
//      BoostBodyReq req = (BoostBodyReq) requests.get(0).getBody();
//      Assert.assertEquals(req.getMsgType(), MessageType.Wj);
//      double expected = -1.0 / 30;
//      Assert.assertEquals(req.getWj(), expected);
//   }
//
//   @Test
//   public void testControlFinalModelSingleClient() {
//      // SingleClient participates training
//      List<MixTreeNode> treeNodeList = new ArrayList<>();
//      MixTreeNode root1 = new MixTreeNode(1, 1);
//      root1.setAsLeaf(1.0);
//      treeNodeList.add(root1);
//      MixTreeNode root2 = new MixTreeNode(1,2);
//      treeNodeList.add(root2);
//      MixTreeNode node3 = new MixTreeNode(2,3);
//      MixTreeNode node4 = new MixTreeNode(2,4);
//      MixTreeNode node5 = new MixTreeNode(3,5);
//      MixTreeNode node6 = new MixTreeNode(3,6);
//      MixTreeNode node7 = new MixTreeNode(3,7);
//      MixTreeNode node8 = new MixTreeNode(3,8);
//
//      root2.setClient(clientInfos.get(0));
//      root2.setSplitFeatureType(1);
//      root2.setLeftChild(node3);
//      root2.setRightChild(node4);
//      node3.setParent(root2);
//      node3.setSplitFeatureType(0);
//      node3.setLeftChild(node5);
//      node3.setRightChild(node6);
//      node4.setParent(root2);
//      node4.setSplitFeatureType(1);
//      node4.setClient(clientInfos.get(0));
//      node4.setLeftChild(node7);
//      node4.setRightChild(node8);
//      node5.setAsLeaf(2.0);
//      node5.setParent(node3);
//      node6.setAsLeaf(2.0);
//      node6.setParent(node3);
//      node7.setAsLeaf(2.0);
//      node7.setParent(node4);
//      node8.setAsLeaf(2.0);
//      node8.setParent(node4);
//      List<ClientInfo> singleClient = new ArrayList<>(clientInfos);
//      singleClient.remove(2);
//      singleClient.remove(1);
//      MixGBParameter parameter = new MixGBParameter(1.0,1.0, 1, 1, 0, ObjectiveType.regLogistic,null, 3,2,0.3,33,0.6,"", 512);
//
//      MixGBoost mixGBoost = new MixGBoost(parameter, null, null, singleClient, null, null, treeNodeList, null);
//      List<CommonResponse> responses = new ArrayList<>();
//      responses.add(new CommonResponse(clientInfos.get(0), new BoostBodyRes(MessageType.Wj)));
//      // curTreeNode = null
//      List<CommonRequest> requests = mixGBoost.control(responses);
//      Assert.assertEquals(requests.size(), singleClient.size());
//      CommonRequest first = requests.get(0);
//      Message message = first.getBody();
//      BoostBodyReq req = (BoostBodyReq) message;
//      MessageType msg = req.getMsgType();
//      Assert.assertEquals(msg, MessageType.EpochFinish);
//   }
//
//   @Test
//   public void testControlFinalModel() {
//      // multiple Client participates training
//      List<MixTreeNode> treeNodeList = new ArrayList<>();
//      MixTreeNode root1 = new MixTreeNode(1, 1);
//      root1.setAsLeaf(1.0);
//      treeNodeList.add(root1);
//      MixTreeNode root2 = new MixTreeNode(1,2);
//      treeNodeList.add(root2);
//      MixTreeNode node3 = new MixTreeNode(2,3);
//      MixTreeNode node4 = new MixTreeNode(2,4);
//      MixTreeNode node5 = new MixTreeNode(3,5);
//      MixTreeNode node6 = new MixTreeNode(3,6);
//      MixTreeNode node7 = new MixTreeNode(3,7);
//      MixTreeNode node8 = new MixTreeNode(3,8);
//
//      root2.setClient(clientInfos.get(0));
//      root2.setSplitFeatureType(1);
//      root2.setLeftChild(node3);
//      root2.setRightChild(node4);
//      node3.setParent(root2);
//      node3.setSplitFeatureType(0);
//      node3.setLeftChild(node5);
//      node3.setRightChild(node6);
//      node4.setParent(root2);
//      node4.setSplitFeatureType(1);
//      node4.setClient(clientInfos.get(1));
//      node4.setLeftChild(node7);
//      node4.setRightChild(node8);
//      node5.setAsLeaf(2.0);
//      node5.setParent(node3);
//      node6.setAsLeaf(2.0);
//      node6.setParent(node3);
//      node7.setAsLeaf(2.0);
//      node7.setParent(node4);
//      node8.setAsLeaf(2.0);
//      node8.setParent(node4);
//
//      MixGBParameter parameter = new MixGBParameter(1.0,1.0, 1, 1, 0, ObjectiveType.regLogistic,null, 3,2,0.3,33,0.6,"", 512);
//
//      MixGBoost mixGBoost = new MixGBoost(parameter, null, null, clientInfos, null, null, treeNodeList, null);
//      List<CommonResponse> responses = new ArrayList<>();
//      responses.add(new CommonResponse(clientInfos.get(0), new BoostBodyRes(MessageType.Wj)));
//      // curTreeNode = null
//      List<CommonRequest> requests = mixGBoost.control(responses);
//      Assert.assertEquals(requests.size(), clientInfos.size());
//      CommonRequest first = requests.get(0);
//      Message message = first.getBody();
//      BoostBodyReq req = (BoostBodyReq) message;
//      MessageType msg = req.getMsgType();
//      Assert.assertEquals(msg, MessageType.EpochFinish);
//   }
//
//
//   @Test
//   public void testPostInferenceControl() {
//      String[] predUid = new String[]{"0A", "1B", "2C"};
//      double[] predV = new double[]{0.1, 0.2, Double.NaN};
//      Map<String, Double> inferScoreMap = new HashMap<>();
//      inferScoreMap.put(predUid[0], predV[0]);
//      inferScoreMap.put(predUid[1], predV[1]);
//      MixGBoost mixGBoost = new MixGBoost(predUid, inferScoreMap, true, null, null, clientInfos);
//      List<CommonResponse> responses = new ArrayList<>();
//      double[][] testRes = mixGBoost.postInferenceControl(responses).getPredicts();
//      Assert.assertEquals(MathExt.transpose(testRes)[0], predV);
//      Assert.assertFalse(mixGBoost.isInferenceContinue());
//   }
//
//   @Test
//   public void testInitInference() {
//      MixGBoost mixGBoost = new MixGBoost(new MixGBParameter());
//      String[] predUid = new String[]{"0A", "1B", "2C"};
//      List<CommonRequest> requests = mixGBoost.initInference(clientInfos, predUid, false);
//      Assert.assertEquals(clientInfos.size(), requests.size());
//      CommonRequest first = requests.get(0);
//      Message message = first.getBody();
//      String[] predOriginId = ((InferenceInit) message).getUid();
//      Assert.assertEquals(Arrays.toString(predOriginId), Arrays.toString(predUid));
//   }
//
//   @Test
//   public void testInferenceQueryprocessEmpty() {
//      String[] predUid = new String[]{"1L", "2L", "3L"};
//      double[] predV = new double[]{0.1, 0.2, 0.3};
//      Map<String, Double> inferScoreMap = new HashMap<>();
//      inferScoreMap.put(predUid[0], predV[0]);
//      inferScoreMap.put(predUid[1], predV[1]);
//      inferScoreMap.put(predUid[2], predV[2]);
//
//      List<MixTreeNode> inferRootNodeList = new ArrayList<>();
//      Map<Integer, MixTreeNode> recordIdNodeMap = new HashMap<>();
//      MixTreeNode node1 = new MixTreeNode(1, 1);
//      MixTreeNode node2 = new MixTreeNode(2, 2);
//      node2.setAsLeaf(0.5);
//      node2.setParent(node1);
//      MixTreeNode node3 = new MixTreeNode(2, 3);
//      node3.setParent(node1);
//      node3.setClient(clientInfos.get(2));
//      node1.setLeftChild(node2);
//      node1.setRightChild(node3);
//      inferRootNodeList.add(node1);
//      recordIdNodeMap.put(1, node1);
//      recordIdNodeMap.put(2, node2);
//      recordIdNodeMap.put(3, node3);
//      MixGBoost mixGBoost = new MixGBoost(predUid, inferScoreMap, true, inferRootNodeList, recordIdNodeMap, clientInfos);
//
//      BoostInferQueryResBody[] bodies = new BoostInferQueryResBody[1];
//      bodies[0] = new BoostInferQueryResBody(new String[]{"1L", "2L"}, 5, 0.2);
//      BoostInferQueryRes res = new BoostInferQueryRes(bodies);
//      List<CommonResponse> resList = new ArrayList<>();
//      resList.add(new CommonResponse(clientInfos.get(0), res));
//      List<CommonRequest> requests = mixGBoost.inferenceControl(resList);
//      Assert.assertEquals(requests.size(), 0);
//      Assert.assertFalse(mixGBoost.isInferenceContinue());
//   }
//
//   @Test
//   public void testInferenceQueryprocess() {
//      String[] predUid = new String[]{"1L", "2L", "3L", "4L", "5L"};
//      double[] predV = new double[]{0.1, 0.2, 0.3, 0.4, 0.5};
//      Map<String, Double> inferScoreMap = new HashMap<>();
//      inferScoreMap.put(predUid[0], predV[0]);
//      inferScoreMap.put(predUid[1], predV[1]);
//      inferScoreMap.put(predUid[2], predV[2]);
//      inferScoreMap.put(predUid[3], predV[3]);
//      inferScoreMap.put(predUid[4], predV[4]);
//
//      List<MixTreeNode> inferRootNodeList = new ArrayList<>();
//      Map<Integer, MixTreeNode> recordIdNodeMap = new HashMap<>();
//      MixTreeNode node1 = new MixTreeNode(1, 1);
//      MixTreeNode node2 = new MixTreeNode(2, 2);
//      node2.setAsLeaf(0.5);
//      node2.setParent(node1);
//      MixTreeNode node3 = new MixTreeNode(2, 3);
//      node3.setParent(node1);
//      node3.setClient(clientInfos.get(2));
//      node1.setLeftChild(node2);
//      node1.setRightChild(node3);
//      inferRootNodeList.add(node1);
//      recordIdNodeMap.put(1, node1);
//      recordIdNodeMap.put(2, node2);
//      recordIdNodeMap.put(3, node3);
//
//      MixGBoost mixGBoost = new MixGBoost(predUid, inferScoreMap, true, inferRootNodeList, recordIdNodeMap, clientInfos);
//
//      BoostInferQueryResBody[] bodies = new BoostInferQueryResBody[3];
//      // bodies[0] a leave return body
//      bodies[0] = new BoostInferQueryResBody(new String[]{"1L", "2L"}, -1, 0.2);
//      // bodies[1] an internal node return body
//      bodies[1] = new BoostInferQueryResBody(new String[]{"3L", "4L"}, 1, 0);
//      // bodies[2] an internal node return body
//      bodies[2] = new BoostInferQueryResBody(new String[]{"5L"}, 1, 1);
//      BoostInferQueryRes res = new BoostInferQueryRes(bodies);
//      List<CommonResponse> resList = new ArrayList<>();
//      resList.add(new CommonResponse(clientInfos.get(0), res));
//      List<CommonRequest> requests = mixGBoost.inferenceControl(resList);
//      Assert.assertEquals(requests.size(), 1);
//      Assert.assertTrue(mixGBoost.isInferenceContinue());
//
//      double[][] testRes = mixGBoost.postInferenceControl(new ArrayList<>()).getPredicts();
//      double[] correctPredV = new double[]{0.3, 0.4, 0.8, 0.9, 0.5};
//      Assert.assertTrue(Tool.approximate(testRes[0][0], correctPredV[0]));
//      Assert.assertTrue(Tool.approximate(testRes[1][0], correctPredV[1]));
//      Assert.assertTrue(Tool.approximate(testRes[2][0], correctPredV[2]));
//      Assert.assertTrue(Tool.approximate(testRes[3][0], correctPredV[3]));
//      Assert.assertTrue(Tool.approximate(testRes[4][0], correctPredV[4]));
//   }
//
//   @Test
//   public void testIsContinue() {
//      MixGBoost mixGBoost = new MixGBoost(new MixGBParameter());
//      boolean isContinue = mixGBoost.isContinue();
//      Assert.assertTrue(isContinue);
//   }
//
//   @Test
//   public void testIsInferenceContinue() {
//      MixGBoost mixGBoost = new MixGBoost(new MixGBParameter());
//      boolean isInferenceContinue = mixGBoost.isInferenceContinue();
//      Assert.assertTrue(isInferenceContinue);
//   }
//
//   @Test
//   public void testMetric() {
//      MixGBoost mixGBoost = new MixGBoost(new MixGBParameter());
//      Assert.assertEquals(mixGBoost.readMetrics().getMetrics(), null);
//
//      mixGBoost = new MixGBoost(null, null, metricMap, clientInfos, null, null, null, null);
//      MetricValue emptyMetrics = mixGBoost.readMetrics();
//      Assert.assertEquals(emptyMetrics.getMetrics().size(), 1);
//   }
//}