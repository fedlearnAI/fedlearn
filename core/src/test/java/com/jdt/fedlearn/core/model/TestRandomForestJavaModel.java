package com.jdt.fedlearn.core.model;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.common.InferenceInit;
import com.jdt.fedlearn.core.entity.common.InferenceInitRes;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.entity.feature.SingleFeature;
import com.jdt.fedlearn.core.entity.randomForest.DataUtils;
import com.jdt.fedlearn.core.entity.randomForest.RandomForestTrainReq;
import com.jdt.fedlearn.core.entity.randomForest.RandomForestTrainRes;
import com.jdt.fedlearn.core.entity.randomForest.TreeNodeRF;
import com.jdt.fedlearn.core.loader.randomForest.RFTrainData;
import com.jdt.fedlearn.core.loader.randomForest.RFInferenceData;
import com.jdt.fedlearn.core.parameter.RandomForestParameter;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.core.type.EncryptionType;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.RFDispatchPhaseType;
import com.jdt.fedlearn.core.util.DataParseUtil;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.testng.Assert.*;

public class TestRandomForestJavaModel {
    RandomForestJavaModel model = new RandomForestJavaModel();
    RFTrainData trainData;
    @BeforeTest
    public void init(){
        List<SingleFeature> features0 = new ArrayList<>();
        features0.add(new SingleFeature("uid", "String"));
        features0.add(new SingleFeature("x1", "String"));
        features0.add(new SingleFeature("x2", "String"));
        features0.add(new SingleFeature("y", "String"));
        Features features = new Features(features0, "y");

        String[] ids = new String[]{"1B", "2A", "3A",};
        String[] x0 = new String[]{"uid", "x1", "x2", "y"};
        String[] x1 = new String[]{"1B", "6", "148", "1"};
        String[] x2 = new String[]{"2A", "1", "85", "0"};
        String[] x3 = new String[]{"3A", "8", "183", "1"};
        String[][] input = new String[][]{x0, x1, x2, x3};
        Map<String, Object> others  = new HashMap<>();
        Map<Integer, String> sampleIds = new HashMap<>();
        for (int i = 0; i < 2; i++) {
            List<Integer> sampleId;
            sampleId = DataUtils.choice(3, 3, new Random(666));
            Collections.sort(sampleId);
            String strSampleIds = DataUtils.sampleIdToString(DataUtils.asSortedList(sampleId), 400000);
            sampleIds.put(i, strSampleIds);
        }
        others.put("sampleIds", sampleIds);
        others.put("featureAllocation", "2,2");
        MetricType[] metrics = new MetricType[]{MetricType.AUC, MetricType.ACC};
        String loss = "Regression:MSE";
        RandomForestParameter parameter = new RandomForestParameter(
                2,
                3,
                3,
                50,
                0.8,
                30,
                30,
                "Null",
                10,
                EncryptionType.IterativeAffine,
                metrics,
                loss,
                666);
        trainData = model.trainInit(input, ids, new int[0], parameter,  features, others);
    }
    @Test
    public void testTrainInit(){
        RandomForestJavaModel model = new RandomForestJavaModel();
        List<SingleFeature> features0 = new ArrayList<>();
        features0.add(new SingleFeature("uid", "String"));
        features0.add(new SingleFeature("x1", "String"));
        features0.add(new SingleFeature("x2", "String"));
        features0.add(new SingleFeature("y", "String"));
        Features features = new Features(features0, "y");

        String[] ids = new String[]{"1B", "2A", "3A",};
        String[] x0 = new String[]{"uid", "x1", "x2", "y"};
        String[] x1 = new String[]{"1B", "6", "148", "1"};
        String[] x2 = new String[]{"2A", "1", "85", "0"};
        String[] x3 = new String[]{"3A", "8", "183", "1"};
        String[][] input = new String[][]{x0, x1, x2, x3};
        Map<String, Object> others  = new HashMap<>();
        Map<Integer, String> sampleIds = new HashMap<>();
        for (int i = 0; i < 2; i++) {
            List<Integer> sampleId;
            sampleId = DataUtils.choice(3, 3, new Random(666));
            Collections.sort(sampleId);
            String strSampleIds = DataUtils.sampleIdToString(DataUtils.asSortedList(sampleId), 400000);
            sampleIds.put(i, strSampleIds);
        }
        others.put("sampleIds", sampleIds);
        others.put("featureAllocation", "2,2");
        MetricType[] metrics = new MetricType[]{MetricType.AUC, MetricType.ACC};
        String loss = "Regression:MSE";
        RandomForestParameter parameter = new RandomForestParameter(
                2,
                3,
                3,
                50,
                0.8,
                30,
                30,
                "Null",
                10,
                EncryptionType.IterativeAffine,
                metrics,
                loss,
                666);
        RFTrainData trainData = model.trainInit(input, ids, new int[0], parameter,  features, others);
        Assert.assertEquals(trainData.numCols(), 2);
        Assert.assertEquals(trainData.numRows(), 3);
        //TODO add more Assert
    }

    @Test
    public void testTrainPhase1Init(){
        RandomForestTrainReq randomForestTrainReq = new RandomForestTrainReq();
        model.setInitTrain(true);
        RandomForestTrainRes randomForestTrainRes = (RandomForestTrainRes)model.train(1, randomForestTrainReq,trainData);
        assertEquals(randomForestTrainRes.getBody(), "");
        assertEquals(randomForestTrainRes.getEncryptionLabel().length, 3);
        randomForestTrainRes = (RandomForestTrainRes)model.train(1, randomForestTrainReq,trainData);
        assertEquals(randomForestTrainRes.getBody(), "");
    }

    @Test
    public void testTrainPhase1(){
        RandomForestTrainReq randomForestTrainReq = new RandomForestTrainReq(new ClientInfo("127.0.0.1", 80, "HTTP", "", "0"));
        model.setInitTrain(false);
        RandomForestTrainRes randomForestTrainRes = (RandomForestTrainRes)model.train(1, randomForestTrainReq,trainData);
    }

    @Test
    public void testTrainPhase2Active(){
        init();
        RandomForestTrainReq randomForestTrainReq = new RandomForestTrainReq(new ClientInfo("127.0.0.1", 80, "HTTP", "", "0"));
        randomForestTrainReq.setClientFeatureMap(new HashMap<>());
        Map<Integer, TreeNodeRF> currentNodeMap = new HashMap<>();
        List<Integer> sampleIds = new ArrayList<>();
        sampleIds.add(1);
        currentNodeMap.put(0, new TreeNodeRF(sampleIds, 0, 1));
        model.setCurrentNodeMap(currentNodeMap);
        RandomForestTrainRes randomForestTrainRes = (RandomForestTrainRes)model.train(2, randomForestTrainReq, trainData);
//        Assert.assertEquals(randomForestTrainRes.getTrainMetric().get(MetricType.ACC).get(0).toString(), "1=0.6666666666666666")
    }

    @Test
    public void testTrainPhase2Passive(){
        RandomForestTrainReq randomForestTrainReq = new RandomForestTrainReq(new ClientInfo("127.0.0.1", 80, "HTTP", "", "0"));
        randomForestTrainReq.setClientFeatureMap(new HashMap<>());
        Map<Integer, TreeNodeRF> currentNodeMap = new HashMap<>();
        List<Integer> sampleIds = new ArrayList<>();
        sampleIds.add(1);
        currentNodeMap.put(0, new TreeNodeRF(sampleIds, 0, 1));
        Map<Integer, List<Integer>> tidToSampleID = new HashMap<>();
        List<Integer> list = new ArrayList<>();
        list.add(0);
        tidToSampleID.put(0, list);
        randomForestTrainReq.setTidToSampleID(tidToSampleID);
        randomForestTrainReq.setEncryptY(new String[]{"273528226727648925423170667841758849463700135880489494825533923025024880259788:63455457545184325638614290698892501826867760554590297940153703143543331363464199167661252644065086782867228673307273467237084074922033318461128466682579785891095097603442017943526080021558872370124591800005450175413005098612111991774119926700449193788087689780656730490782485010375387424102729547719578258174:92107868912499681809813834703129068392570839898502438937010495023204930248090080865859505101124126475992987573575847275655252567537864685238781976656902641966408470686445589766976138653634849670333899288796819026407362409703046040230876159580198409013324761920634222736578148823043046811641741332726171899503:1125899906842624:1125899906842624:0:1125899906842624"});
        randomForestTrainReq.setPublickey("{a1\u0003830780530034885498861952145659012758497346332380048319425236632933107256860100539089363151039829892868103060126445793512249735944758756184008547050311729893342957966341195251508067219720143358582879200840058501987803501608753813875777149758407\u0002n0\u000310019513461850466042843216497049817520077473038151342172062787792060385693365679340105675989749518434413828886462863065848278037093509097683029097971827940634993554319709962392970517057953750288799041830714101301489952767003250084499645287109661598305\u0002encodedPrecision\u00039223372036854775808\u0002n1\u000392107868912499681809813834703129068392570839898502438937010495023204930248090080865859505101124126475992987573575847275655252567537864685238781976656902641966408470686445589766976138653634849670333899288796819026407362409703046040230876159580198409013324761920634222736578148823043046811641741332726171899503\u0002key_round\u00032\u0002g\u0003371299234601250649431571412842\u0002x\u00031212622101150891960289782033326297194857032745566\u0002ainv0\u00035505190552734840070831159532442676049497307863432721020872276088900734627211877202311074568223674081439264423128508739048798738700152434192808846739965269716385967035615615213293469157516036039944806648817130749074788057661580428143006698980985777598\u0002ainv1\u000343815585411269525130358535366333722066045872993377304849706852114583465801685983892284946172269660128842887264278744408018662460972221395128422128004606792445706485528875462258231139125264540869137565729755600706146549664196898066218453949338606298166911973135315703656607054813279197102601283355452984356350\u0002a0\u00032137152869400823138041411552307}");
        model.setCurrentNodeMap(currentNodeMap);
        model.setActive(false);
        RandomForestTrainRes randomForestTrainRes = (RandomForestTrainRes)model.train(2, randomForestTrainReq, trainData);
//        Assert.assertEquals(randomForestTrainRes.getTrainMetric().get(MetricType.ACC).get(0).toString(), "1=0.6666666666666666")
    }

    @Test
    public void testTrainPhase3Active(){
        init();
        RandomForestTrainReq randomForestTrainReq = new RandomForestTrainReq(new ClientInfo("127.0.0.1", 80, "HTTP", "", "0"));
        randomForestTrainReq.setClientFeatureMap(new HashMap<>());
        Map<Integer, TreeNodeRF> currentNodeMap = new HashMap<>();
        List<Integer> sampleIds = new ArrayList<>();
        sampleIds.add(1);
        currentNodeMap.put(0, new TreeNodeRF(sampleIds, 0, 0));
        currentNodeMap.put(1, new TreeNodeRF(sampleIds, 0, 1));
        randomForestTrainReq.setBodyAll(new String[]{"active", "273528226727648925423170667841758849463700135880489494825533923025024880259788:63455457545184325638614290698892501826867760554590297940153703143543331363464199167661252644065086782867228673307273467237084074922033318461128466682579785891095097603442017943526080021558872370124591800005450175413005098612111991774119926700449193788087689780656730490782485010375387424102729547719578258174:92107868912499681809813834703129068392570839898502438937010495023204930248090080865859505101124126475992987573575847275655252567537864685238781976656902641966408470686445589766976138653634849670333899288796819026407362409703046040230876159580198409013324761920634222736578148823043046811641741332726171899503:1125899906842624:1125899906842624:0:1125899906842624::273528226727648925423170667841758849463700135880489494825533923025024880259788:63455457545184325638614290698892501826867760554590297940153703143543331363464199167661252644065086782867228673307273467237084074922033318461128466682579785891095097603442017943526080021558872370124591800005450175413005098612111991774119926700449193788087689780656730490782485010375387424102729547719578258174:92107868912499681809813834703129068392570839898502438937010495023204930248090080865859505101124126475992987573575847275655252567537864685238781976656902641966408470686445589766976138653634849670333899288796819026407362409703046040230876159580198409013324761920634222736578148823043046811641741332726171899503:1125899906842624:1125899906842624:0:1125899906842624:::273528226727648925423170667841758849463700135880489494825533923025024880259788:63455457545184325638614290698892501826867760554590297940153703143543331363464199167661252644065086782867228673307273467237084074922033318461128466682579785891095097603442017943526080021558872370124591800005450175413005098612111991774119926700449193788087689780656730490782485010375387424102729547719578258174:92107868912499681809813834703129068392570839898502438937010495023204930248090080865859505101124126475992987573575847275655252567537864685238781976656902641966408470686445589766976138653634849670333899288796819026407362409703046040230876159580198409013324761920634222736578148823043046811641741332726171899503:1125899906842624:1125899906842624:0:1125899906842624::273528226727648925423170667841758849463700135880489494825533923025024880259788:63455457545184325638614290698892501826867760554590297940153703143543331363464199167661252644065086782867228673307273467237084074922033318461128466682579785891095097603442017943526080021558872370124591800005450175413005098612111991774119926700449193788087689780656730490782485010375387424102729547719578258174:92107868912499681809813834703129068392570839898502438937010495023204930248090080865859505101124126475992987573575847275655252567537864685238781976656902641966408470686445589766976138653634849670333899288796819026407362409703046040230876159580198409013324761920634222736578148823043046811641741332726171899503:1125899906842624:1125899906842624:0:1125899906842624"});
        model.setCurrentNodeMap(currentNodeMap);
        model.setActivePhase2body(new String[]{"1.0,1.0::1.0,0.0","0.0,1.0::1.0,0.0"});
        List<ClientInfo> clientInfos = new ArrayList<>();
        clientInfos.add(new ClientInfo("127.0.0.1", 80, "HTTP", "", "0"));
        randomForestTrainReq.setClientInfos(clientInfos);


        RandomForestTrainRes randomForestTrainRes = (RandomForestTrainRes)model.train(3, randomForestTrainReq, trainData);
//        Assert.assertEquals(randomForestTrainRes.getTrainMetric().get(MetricType.ACC).get(0).toString(), "1=0.6666666666666666")
    }

    @Test
    public void testTrainPhase3Passive(){
        RandomForestTrainReq randomForestTrainReq = new RandomForestTrainReq(new ClientInfo("127.0.0.1", 80, "HTTP", "", "0"));
        model.setActive(false);
        RandomForestTrainRes randomForestTrainRes = (RandomForestTrainRes)model.train(3, randomForestTrainReq, trainData);
        Assert.assertEquals(randomForestTrainRes.getMessageType(), RFDispatchPhaseType.SPLIT_NODE);
    }

    @Test
    public void testTrainPhase4(){
        RandomForestTrainReq randomForestTrainReq = new RandomForestTrainReq(new ClientInfo("127.0.0.1", 80, "HTTP", "", "0"));
        model.setActive(false);
        Map<Integer, List<Integer>> tidToSampleID = new HashMap<>();
        List<Integer> list = new ArrayList<>();
        list.add(0);
        list.add(1);
        list.add(2);
        tidToSampleID.put(0, list);
        randomForestTrainReq.setTidToSampleID(tidToSampleID);
        randomForestTrainReq.setBody("{\"treeId\":0.0,\"percentile\":86.66666666666667,\"nodeId\":0.0,\"featureId\":0.0}||");
        RandomForestTrainRes randomForestTrainRes = (RandomForestTrainRes)model.train(4, randomForestTrainReq, trainData);
    }

    @Test
    public void testTrainPhase5(){

        RandomForestTrainReq randomForestTrainReq = new RandomForestTrainReq(new ClientInfo("127.0.0.1", 80, "HTTP", "", "0"));
        Map<Integer, List<Integer>> tidToSampleID = new HashMap<>();
        List<Integer> list = new ArrayList<>();
        list.add(0);
        list.add(1);
        list.add(2);
        tidToSampleID.put(0, list);
        randomForestTrainReq.setTidToSampleID(tidToSampleID);

        List<String[]> allTreeIds = new ArrayList<>();
        allTreeIds.add(new String[]{"0"});
        randomForestTrainReq.setAllTreeIds(allTreeIds);
        List<Map<Integer, double[]>> maskLefts = new ArrayList<>();
        Map<Integer, double[]> map = new HashMap<>();
        map.put(0, new double[]{0.0});
        maskLefts.add(map);
        randomForestTrainReq.setMaskLefts(maskLefts);
        List<String[]> splitMesses = new ArrayList<>();
        splitMesses.add(new String[]{"{\"is_leaf\": 0, \"feature_opt\": 0, \"value_opt\": 0}"});
        randomForestTrainReq.setSplitMessages(splitMesses);
        List<ClientInfo> clientInfos = new ArrayList<>();
        clientInfos.add(new ClientInfo("127.0.0.1", 80, "HTTP", "", "0"));
        randomForestTrainReq.setClientInfos(clientInfos);
        model.setMaskLeft(maskLefts.get(0));
        model.setMess(splitMesses.get(0));
        Map<ClientInfo, List<Integer>[]> clientFeatureMap = new HashMap<>();
        List<Integer>[] lists = new ArrayList[1];
        lists[0] = new ArrayList<>();
        lists[0].add(0);
        clientFeatureMap.put(new ClientInfo("127.0.0.1", 80, "HTTP", "", "0"), lists);
        model.setClientFeatureMap(clientFeatureMap);
        model.setActive(true);

        Map<Integer, TreeNodeRF> currentNodeMap = new HashMap<>();
        List<Integer> sampleIds = new ArrayList<>();
        sampleIds.add(1);
        TreeNodeRF treeNodeRF = new TreeNodeRF(sampleIds, 0, 1);
        treeNodeRF.score = 1.0;
        currentNodeMap.put(0, treeNodeRF);
        model.setCurrentNodeMap(currentNodeMap);

        RandomForestTrainRes randomForestTrainRes = (RandomForestTrainRes)model.train(5, randomForestTrainReq, trainData);
    }

    @Test
    public void testUpdateModel(){
        RandomForestTrainReq randomForestTrainReq = new RandomForestTrainReq(new ClientInfo("127.0.0.1", 80, "HTTP", "", "0"));
        model.setActive(true);
        List<ClientInfo> clientInfos = new ArrayList<>();
        clientInfos.add(new ClientInfo("127.0.0.1", 80, "HTTP", "", "0"));

        model.setClientInfos(clientInfos);

        randomForestTrainReq.setBody("init");
        RandomForestTrainRes randomForestTrainRes = (RandomForestTrainRes)model.train(99, randomForestTrainReq, trainData);
        randomForestTrainReq.setBody("");

        randomForestTrainRes = (RandomForestTrainRes)model.train(99, randomForestTrainReq, trainData);
    }


    @Test
    public void testInferenceInit(){
        RandomForestJavaModel model = new RandomForestJavaModel();
        String[] uidList = new String[]{"aa", "1a", "c3"};
        String[][] data = new String[2][];
        data[0] = new String[]{"aa", "10", "12.1"};
        data[1] = new String[]{"1a", "10", "12.1"};
        Message msg = model.inferenceInit(uidList, data ,new HashMap<>());
        InferenceInitRes res = (InferenceInitRes) msg;
        Assert.assertFalse(res.isAllowList());
        Assert.assertEquals(res.getUid(), new int[]{2});
    }


    @Test
    public void testInferencePhase1(){
        RandomForestJavaModel model = new RandomForestJavaModel();
        String[] subUid = {"aa","1a"};
        InferenceInit init = new InferenceInit(subUid);
        String[][] data = new String[3][3];
        data[0] = new String[]{"uid","age","height"};
        data[1] = new String[]{"aa", "10", "1.2"};
        data[2] = new String[]{"1a", "8", "1.1"};
        RFInferenceData rfInferenceData  = new RFInferenceData(data);
        String[] res = model.inferencePhase1(rfInferenceData, init);
        Assert.assertEquals(subUid,res);
    }

    @Test
    public void testDeserializeSerialize(){
        RandomForestJavaModel model = new RandomForestJavaModel();
        String input = "{Tree3={\"0\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":151.0001514171516}\",\"isLeaf\":\"0\",\"nodeId\":\"0\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"1\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":99.99998002234719}\",\"isLeaf\":\"0\",\"nodeId\":\"1\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"7\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":43.999921054931626}\",\"isLeaf\":\"0\",\"nodeId\":\"7\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"9\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":124.00012914274178}\",\"isLeaf\":\"0\",\"nodeId\":\"9\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"30\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":155.00012467329694}\",\"isLeaf\":\"0\",\"nodeId\":\"30\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"}}, numTrees=5, Tree2={\"0\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":151.00027697217268}\",\"isLeaf\":\"0\",\"nodeId\":\"0\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"1\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":99.9999472645977}\",\"isLeaf\":\"0\",\"nodeId\":\"1\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"16\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":-8.048749544327601E-7}\",\"isLeaf\":\"0\",\"nodeId\":\"16\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"7\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":-9.680471521253467E-7}\",\"isLeaf\":\"0\",\"nodeId\":\"7\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"8\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":43.99999614858515}\",\"isLeaf\":\"0\",\"nodeId\":\"8\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"19\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":124.99982322738359}\",\"isLeaf\":\"0\",\"nodeId\":\"19\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"9\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":124.99984795553796}\",\"isLeaf\":\"0\",\"nodeId\":\"9\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"30\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":155.00027031865756}\",\"isLeaf\":\"0\",\"nodeId\":\"30\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"21\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":126.9999433362098}\",\"isLeaf\":\"0\",\"nodeId\":\"21\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"}}, Tree4={\"22\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":3.0000075248494786}\",\"isLeaf\":\"0\",\"nodeId\":\"22\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"0\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":151.0001365676909}\",\"isLeaf\":\"0\",\"nodeId\":\"0\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"1\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":99.9999366657892}\",\"isLeaf\":\"0\",\"nodeId\":\"1\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"29\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":155.0001897353887}\",\"isLeaf\":\"0\",\"nodeId\":\"29\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"20\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":127.99992401629248}\",\"isLeaf\":\"0\",\"nodeId\":\"20\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"}}, Tree1={\"11\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":145.00003989208258}\",\"isLeaf\":\"0\",\"nodeId\":\"11\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"0\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":127.99985305695758}\",\"isLeaf\":\"0\",\"nodeId\":\"0\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"5\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":160.9997883838923}\",\"isLeaf\":\"0\",\"nodeId\":\"5\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"28\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":153.99981309357239}\",\"isLeaf\":\"0\",\"nodeId\":\"28\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"6\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":154.99983717684108}\",\"isLeaf\":\"0\",\"nodeId\":\"6\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"10\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":99.00010749733747}\",\"isLeaf\":\"0\",\"nodeId\":\"10\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"}}, Tree0={\"22\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":7.999994188589154}\",\"isLeaf\":\"0\",\"nodeId\":\"22\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"0\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":151.00017897553693}\",\"isLeaf\":\"0\",\"nodeId\":\"0\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"1\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":99.9999725963405}\",\"isLeaf\":\"0\",\"nodeId\":\"1\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"3\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":10.000011611323714}\",\"isLeaf\":\"0\",\"nodeId\":\"3\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"15\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":-1.6991660774055133E-6}\",\"isLeaf\":\"0\",\"nodeId\":\"15\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"26\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":7.000009888437513}\",\"isLeaf\":\"0\",\"nodeId\":\"26\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"16\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":44.00001916744659}\",\"isLeaf\":\"0\",\"nodeId\":\"16\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"5\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":155.00001821716612}\",\"isLeaf\":\"0\",\"nodeId\":\"5\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"21\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":123.99996211692273}\",\"isLeaf\":\"0\",\"nodeId\":\"21\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"}}, localModelType=Null, localModel={alpha\u00030.0\u0002beta\u00030.0\u00040.0}}";
        model.deserialize(input);
        String res = model.serialize();
        String target = "{numTrees=5, Tree3={\"0\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":151.0001514171516}\",\"isLeaf\":\"0\",\"nodeId\":\"0\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"1\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":99.99998002234719}\",\"isLeaf\":\"0\",\"nodeId\":\"1\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"7\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":43.999921054931626}\",\"isLeaf\":\"0\",\"nodeId\":\"7\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"9\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":124.00012914274178}\",\"isLeaf\":\"0\",\"nodeId\":\"9\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"30\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":155.00012467329694}\",\"isLeaf\":\"0\",\"nodeId\":\"30\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"}}, Tree2={\"0\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":151.00027697217268}\",\"isLeaf\":\"0\",\"nodeId\":\"0\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"1\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":99.9999472645977}\",\"isLeaf\":\"0\",\"nodeId\":\"1\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"16\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":-8.048749544327601E-7}\",\"isLeaf\":\"0\",\"nodeId\":\"16\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"7\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":-9.680471521253467E-7}\",\"isLeaf\":\"0\",\"nodeId\":\"7\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"8\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":43.99999614858515}\",\"isLeaf\":\"0\",\"nodeId\":\"8\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"19\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":124.99982322738359}\",\"isLeaf\":\"0\",\"nodeId\":\"19\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"9\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":124.99984795553796}\",\"isLeaf\":\"0\",\"nodeId\":\"9\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"30\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":155.00027031865756}\",\"isLeaf\":\"0\",\"nodeId\":\"30\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"21\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":126.9999433362098}\",\"isLeaf\":\"0\",\"nodeId\":\"21\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"}}, Tree4={\"22\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":3.0000075248494786}\",\"isLeaf\":\"0\",\"nodeId\":\"22\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"0\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":151.0001365676909}\",\"isLeaf\":\"0\",\"nodeId\":\"0\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"1\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":99.9999366657892}\",\"isLeaf\":\"0\",\"nodeId\":\"1\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"29\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":155.0001897353887}\",\"isLeaf\":\"0\",\"nodeId\":\"29\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"20\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":127.99992401629248}\",\"isLeaf\":\"0\",\"nodeId\":\"20\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"}}, Tree1={\"11\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":145.00003989208258}\",\"isLeaf\":\"0\",\"nodeId\":\"11\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"0\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":127.99985305695758}\",\"isLeaf\":\"0\",\"nodeId\":\"0\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"5\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":160.9997883838923}\",\"isLeaf\":\"0\",\"nodeId\":\"5\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"28\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":153.99981309357239}\",\"isLeaf\":\"0\",\"nodeId\":\"28\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"6\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":154.99983717684108}\",\"isLeaf\":\"0\",\"nodeId\":\"6\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"10\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":99.00010749733747}\",\"isLeaf\":\"0\",\"nodeId\":\"10\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"}}, Tree0={\"22\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":7.999994188589154}\",\"isLeaf\":\"0\",\"nodeId\":\"22\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"0\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":151.00017897553693}\",\"isLeaf\":\"0\",\"nodeId\":\"0\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"1\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":99.9999725963405}\",\"isLeaf\":\"0\",\"nodeId\":\"1\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"3\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":10.000011611323714}\",\"isLeaf\":\"0\",\"nodeId\":\"3\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"15\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":-1.6991660774055133E-6}\",\"isLeaf\":\"0\",\"nodeId\":\"15\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"26\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":7.000009888437513}\",\"isLeaf\":\"0\",\"nodeId\":\"26\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"16\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":44.00001916744659}\",\"isLeaf\":\"0\",\"nodeId\":\"16\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"5\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":155.00001821716612}\",\"isLeaf\":\"0\",\"nodeId\":\"5\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"21\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":123.99996211692273}\",\"isLeaf\":\"0\",\"nodeId\":\"21\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"}}, localModelType=Null, localModel={alpha\u00030.0\u0002beta\u00030.0\u00040.0}}";
        assertEquals(res, target);
    }

    @Test
    public void testInferenceOneShot() throws IOException {
        RandomForestJavaModel model = new RandomForestJavaModel();
        String input = "{Tree3={\"0\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":151.0001514171516}\",\"isLeaf\":\"0\",\"nodeId\":\"0\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"1\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":99.99998002234719}\",\"isLeaf\":\"0\",\"nodeId\":\"1\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"7\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":43.999921054931626}\",\"isLeaf\":\"0\",\"nodeId\":\"7\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"9\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":124.00012914274178}\",\"isLeaf\":\"0\",\"nodeId\":\"9\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"30\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":155.00012467329694}\",\"isLeaf\":\"0\",\"nodeId\":\"30\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"}}, numTrees=5, Tree2={\"0\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":151.00027697217268}\",\"isLeaf\":\"0\",\"nodeId\":\"0\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"1\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":99.9999472645977}\",\"isLeaf\":\"0\",\"nodeId\":\"1\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"16\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":-8.048749544327601E-7}\",\"isLeaf\":\"0\",\"nodeId\":\"16\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"7\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":-9.680471521253467E-7}\",\"isLeaf\":\"0\",\"nodeId\":\"7\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"8\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":43.99999614858515}\",\"isLeaf\":\"0\",\"nodeId\":\"8\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"19\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":124.99982322738359}\",\"isLeaf\":\"0\",\"nodeId\":\"19\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"9\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":124.99984795553796}\",\"isLeaf\":\"0\",\"nodeId\":\"9\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"30\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":155.00027031865756}\",\"isLeaf\":\"0\",\"nodeId\":\"30\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"21\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":126.9999433362098}\",\"isLeaf\":\"0\",\"nodeId\":\"21\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"}}, Tree4={\"22\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":3.0000075248494786}\",\"isLeaf\":\"0\",\"nodeId\":\"22\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"0\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":151.0001365676909}\",\"isLeaf\":\"0\",\"nodeId\":\"0\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"1\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":99.9999366657892}\",\"isLeaf\":\"0\",\"nodeId\":\"1\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"29\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":155.0001897353887}\",\"isLeaf\":\"0\",\"nodeId\":\"29\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"20\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":127.99992401629248}\",\"isLeaf\":\"0\",\"nodeId\":\"20\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"}}, Tree1={\"11\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":145.00003989208258}\",\"isLeaf\":\"0\",\"nodeId\":\"11\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"0\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":127.99985305695758}\",\"isLeaf\":\"0\",\"nodeId\":\"0\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"5\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":160.9997883838923}\",\"isLeaf\":\"0\",\"nodeId\":\"5\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"28\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":153.99981309357239}\",\"isLeaf\":\"0\",\"nodeId\":\"28\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"6\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":154.99983717684108}\",\"isLeaf\":\"0\",\"nodeId\":\"6\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"10\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":99.00010749733747}\",\"isLeaf\":\"0\",\"nodeId\":\"10\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"}}, Tree0={\"22\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":7.999994188589154}\",\"isLeaf\":\"0\",\"nodeId\":\"22\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"0\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":151.00017897553693}\",\"isLeaf\":\"0\",\"nodeId\":\"0\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"1\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":99.9999725963405}\",\"isLeaf\":\"0\",\"nodeId\":\"1\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"3\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":10.000011611323714}\",\"isLeaf\":\"0\",\"nodeId\":\"3\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"15\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":-1.6991660774055133E-6}\",\"isLeaf\":\"0\",\"nodeId\":\"15\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"26\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":0,\\\"value_opt\\\":7.000009888437513}\",\"isLeaf\":\"0\",\"nodeId\":\"26\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"16\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":44.00001916744659}\",\"isLeaf\":\"0\",\"nodeId\":\"16\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"5\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":155.00001821716612}\",\"isLeaf\":\"0\",\"nodeId\":\"5\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"},\"21\":{\"referenceJson\":\"{\\\"is_leaf\\\":0,\\\"feature_opt\\\":1,\\\"value_opt\\\":123.99996211692273}\",\"isLeaf\":\"0\",\"nodeId\":\"21\",\"party\":\"{\\\"ip\\\":\\\"127.0.0.1\\\",\\\"port\\\":8891,\\\"path\\\":null,\\\"protocol\\\":\\\"http\\\",\\\"uniqueId\\\":0}\"}}, localModelType=Null, localModel={alpha\u00030.0\u0002beta\u00030.0\u00040.0}}";
        model.deserialize(input);
        String baseDir = "./src/test/resources/classificationA/";
        String[] subUid = DataParseUtil.loadInferenceUidList(baseDir + "inference0.csv");
        InferenceInit init = new InferenceInit(subUid);
        RFInferenceData rfInferenceData  = new RFInferenceData((DataParseUtil.loadTrainFromFile(baseDir + "inference0.csv")));
        model.inference(-1,init,rfInferenceData);

    }

    @Test
    public void testGetModelType(){
        RandomForestJavaModel model = new RandomForestJavaModel();
        assertEquals(model.getModelType(), AlgorithmType.RandomForestJava);
    }

}