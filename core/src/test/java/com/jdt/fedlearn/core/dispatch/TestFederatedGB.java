package com.jdt.fedlearn.core.dispatch;

import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.base.EmptyMessage;
import com.jdt.fedlearn.core.entity.base.SingleElement;
import com.jdt.fedlearn.core.entity.base.StringArray;
import com.jdt.fedlearn.core.entity.boost.*;
import com.jdt.fedlearn.core.entity.common.*;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.exception.NotMatchException;
import com.jdt.fedlearn.core.fake.StructureGenerate;
import com.jdt.fedlearn.core.model.common.tree.Tree;
import com.jdt.fedlearn.core.model.common.tree.TreeNode;
import com.jdt.fedlearn.core.parameter.FgbParameter;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.core.type.BitLengthType;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.ObjectiveType;
import com.jdt.fedlearn.core.type.data.Pair;
import com.jdt.fedlearn.core.type.data.StringTuple2;
import org.testng.Assert;
import org.testng.annotations.Test;
import scala.Tuple4;

import java.util.*;

public class TestFederatedGB {
    private static final FgbParameter fp = new FgbParameter.Builder(3, new MetricType[]{MetricType.ACC}, ObjectiveType.regLogistic).build();

    @Test
    public void getNextPhase() {
        Map<Integer, Integer> inOutPair = new HashMap<>();
        inOutPair.put(0, 1);
        inOutPair.put(1, 2);
        inOutPair.put(2, 3);
        inOutPair.put(3, 4);
        inOutPair.put(4, 5);
        inOutPair.put(5, 1);
        FederatedGB federatedGB = new FederatedGB(fp);
        for (Map.Entry<Integer, Integer> entry : inOutPair.entrySet()) {
            int p = federatedGB.getNextPhase(entry.getKey(), new ArrayList<>());
            Assert.assertEquals(p, entry.getValue().intValue());
        }
    }

    @Test
    public void getNextPhase2() {
        /**
         * Test unsupported phase
         */
        FederatedGB federatedGB = new FederatedGB(fp);
        try {
            federatedGB.getNextPhase(6, new ArrayList<>());
        } catch (NotMatchException e) {
            Assert.assertEquals(e.getMessage(), "phase iteration error");
        }
    }

    @Test
    public void controlElseBranch() {
        /**
         * Test else branch
         */
        FederatedGB federatedGB = new FederatedGB(fp);
        List<CommonResponse> responses = new ArrayList<>();
        try {
            federatedGB.control(responses);
        } catch (UnsupportedOperationException e) {
            Assert.assertEquals(e.getMessage(), "unsupported control message com.jdt.fedlearn.core.dispatch.FederatedGB$1");
        }

    }


    @Test
    public void initControl() {
        List<ClientInfo> clientInfos = StructureGenerate.threeClients(); // client
        MatchResult matchResult = new MatchResult(10);
        Map<ClientInfo, Features> features = StructureGenerate.fgbFeatures(clientInfos); // client -> feature

        FederatedGB federatedGB = new FederatedGB(fp);

        List<CommonRequest> requests = federatedGB.initControl(clientInfos, matchResult, features, new HashMap<>());
        Assert.assertEquals(clientInfos.size(), requests.size());
        CommonRequest first = requests.get(0); // get request for the first client(has label)
        Assert.assertEquals(first.getPhase(), 0); // trainInitialPhase=0
        Assert.assertFalse(first.isSync());
        Message message = first.getBody(); // TrainInit
        TrainInit body = (TrainInit) message;
        Assert.assertEquals(body.getFeatureList(), features.get(clientInfos.get(0))); // first client's features
    }

    @Test
    public void fromInit() {
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        FederatedGB federatedGB = new FederatedGB(fp);
        List<CommonResponse> responses = new ArrayList<>();
        responses.add(new CommonResponse(clientInfos.get(0), new SingleElement("init_success")));
        responses.add(new CommonResponse(clientInfos.get(1), new SingleElement("init_success")));
        responses.add(new CommonResponse(clientInfos.get(2), new SingleElement("init_success")));
        List<CommonRequest> requests = federatedGB.fromInit(responses);
        // 3 clients
        Assert.assertEquals(requests.size(), 3);
        Assert.assertEquals(requests.get(0).getClient(), clientInfos.get(0));
        Assert.assertEquals(requests.get(0).getPhase(), 1);
        Assert.assertEquals(requests.get(1).getClient(), clientInfos.get(1));
        Assert.assertEquals(requests.get(0).getPhase(), 1);
        BoostP1Req bp1r1 = (BoostP1Req) requests.get(0).getBody();
        BoostP1Req bp1r2 = (BoostP1Req) requests.get(1).getBody();
        Assert.assertEquals(bp1r1.getClient(), clientInfos.get(0));
        Assert.assertEquals(bp1r2.getClient(), clientInfos.get(1));

    }

    @Test
    public void control1FromInit() {
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        FederatedGB federatedGB = new FederatedGB(fp);
        List<CommonResponse> responses = new ArrayList<>();
        responses.add(new CommonResponse(clientInfos.get(0), new SingleElement("init_success")));
        responses.add(new CommonResponse(clientInfos.get(1), new SingleElement("init_success")));
        responses.add(new CommonResponse(clientInfos.get(2), new SingleElement("init_success")));
        List<CommonRequest> requests = federatedGB.control(responses);
        // 3 clients
        Assert.assertEquals(requests.size(), 3);
        Assert.assertEquals(requests.get(0).getClient(), clientInfos.get(0));
        Assert.assertEquals(requests.get(0).getPhase(), 1);
        Assert.assertEquals(requests.get(1).getClient(), clientInfos.get(1));
        Assert.assertEquals(requests.get(0).getPhase(), 1);
        BoostP1Req bp1r1 = (BoostP1Req) requests.get(0).getBody();
        BoostP1Req bp1r2 = (BoostP1Req) requests.get(1).getBody();
        Assert.assertEquals(bp1r1.getClient(), clientInfos.get(0));
        Assert.assertEquals(bp1r2.getClient(), clientInfos.get(1));
    }


    @Test
    public void control1FromLastRound() {
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        FederatedGB federatedGB = new FederatedGB(fp);
        List<CommonResponse> responses = new ArrayList<>(); // from model.train phase5
        Map<MetricType, List<Pair<Integer, Double>>> tm1 = new HashMap<MetricType, List<Pair<Integer, Double>>>() {
        };
        Map<MetricType, List<Pair<Integer, Double>>> tm2 = new HashMap<MetricType, List<Pair<Integer, Double>>>() {
        };
        Map<MetricType, List<Pair<Integer, Double>>> tm3 = new HashMap<MetricType, List<Pair<Integer, Double>>>() {
        };
        List<Double> tm11 = new ArrayList<Double>();
        tm11.add(3.2);
        tm11.add(2.6);
        tm11.add(1.5);
        List<Double> tm21 = new ArrayList<Double>();
        tm21.add(2.6);
        List<Double> tm31 = new ArrayList<Double>();
        tm31.add(5.6);
        tm31.add(2.6);
        responses.add(new CommonResponse(clientInfos.get(0), new BoostP5Res(true, 3, new MetricValue(tm1))));
        responses.add(new CommonResponse(clientInfos.get(1), new BoostP5Res(false, 1, new MetricValue(tm2))));
        responses.add(new CommonResponse(clientInfos.get(2), new BoostP5Res(false, 2, new MetricValue(tm3))));
        // result
        List<CommonRequest> requests = federatedGB.control(responses);
        Assert.assertEquals(requests.size(), 3);
        Assert.assertEquals(requests.get(0).getPhase(), 1);
        BoostP1Req bp1r1 = (BoostP1Req) requests.get(0).getBody();
        BoostP1Req bp1r2 = (BoostP1Req) requests.get(1).getBody();
        BoostP1Req bp1r3 = (BoostP1Req) requests.get(2).getBody();
        Assert.assertEquals(bp1r1.getClient(), clientInfos.get(0));
        Assert.assertEquals(bp1r1.isNewTree(), true);
        Assert.assertEquals(bp1r2.getClient(), clientInfos.get(1));
        Assert.assertEquals(bp1r2.isNewTree(), false);
        Assert.assertEquals(bp1r3.getClient(), clientInfos.get(2));
        Assert.assertEquals(bp1r3.isNewTree(), false);
    }

    @Test
    public void controlPhase2() {
        FederatedGB federatedGB = new FederatedGB(fp);
        List<CommonResponse> responses = new ArrayList<>();
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        // client 1 new tree; client2&3 existed tree
        EncryptedGradHess egh1 = new EncryptedGradHess(clientInfos.get(0), new int[]{0, 1},
                new StringTuple2[]{new StringTuple2("10", "5"), new StringTuple2("9", "6")},
                "PublicKey", true);
        EncryptedGradHess egh2 = new EncryptedGradHess(clientInfos.get(1), new int[]{2, 4});
        EncryptedGradHess egh3 = new EncryptedGradHess(clientInfos.get(2), new int[]{2, 3});
        responses.add(new CommonResponse(clientInfos.get(0), egh1));
        responses.add(new CommonResponse(clientInfos.get(1), egh2));
        responses.add(new CommonResponse(clientInfos.get(2), egh3));
        List<CommonRequest> requests = federatedGB.control(responses);
        Assert.assertEquals(requests.size(), 3);
        Assert.assertEquals(requests.get(0).getPhase(), 2);
        EmptyMessage egh_out1 = (EmptyMessage) requests.get(0).getBody();
        EncryptedGradHess egh_out2 = (EncryptedGradHess) requests.get(1).getBody();
        EncryptedGradHess egh_out3 = (EncryptedGradHess) requests.get(2).getBody();
        Assert.assertEquals(egh_out1, EmptyMessage.message());
        Assert.assertEquals(egh_out2.getPubKey(), "PublicKey");
        Assert.assertEquals(egh_out3.getPubKey(), "PublicKey");
        Assert.assertEquals(egh_out2.getNewTree(), true);
        Assert.assertEquals(egh_out3.getNewTree(), true);
        Assert.assertEquals(egh_out2.getInstanceSpace(), new int[]{0, 1});
        Assert.assertEquals(egh_out3.getInstanceSpace(), new int[]{0, 1});
    }

    @Test
    public void controlPhase3() {
        FederatedGB federatedGB = new FederatedGB(fp);
        List<CommonResponse> responses = new ArrayList<>();
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        FeatureLeftGH fgh1 = new FeatureLeftGH(clientInfos.get(0), "feat1",
                new StringTuple2[]{new StringTuple2("10", "5"), new StringTuple2("15", "8")});
        BoostP2Res bp2r1 = new BoostP2Res(new FeatureLeftGH[]{fgh1}); // G,H info of features from client1
        BoostP2Res bp2r2 = new BoostP2Res(null); // G,H info of features from client2
        responses.add(new CommonResponse(clientInfos.get(0), bp2r1));
        responses.add(new CommonResponse(clientInfos.get(1), bp2r2));
        List<CommonRequest> requests = federatedGB.control(responses);
        BoostP3Req bp3q1 = (BoostP3Req) requests.get(0).getBody();
        BoostP3Req bp3q2 = (BoostP3Req) requests.get(1).getBody();
        Assert.assertEquals(requests.size(), 2);
        Assert.assertEquals(requests.get(0).getPhase(), 3);
        Assert.assertEquals(bp3q1.getClient(), clientInfos.get(0));
        Assert.assertEquals(bp3q2.getClient(), clientInfos.get(1));
        Assert.assertEquals(bp3q1.getDataList().size(), 1);
        Assert.assertEquals(bp3q1.getDataList().get(0).size(), 1);
        Assert.assertEquals(bp3q1.getDataList().get(0).get(0), fgh1);
        Assert.assertEquals(bp3q2.getDataList().size(), 1);
        Assert.assertEquals(bp3q2.getDataList().get(0).size(), 1);
        Assert.assertEquals(bp3q2.getDataList().get(0).get(0), fgh1);
    }

    @Test
    public void controlPhase4() {
        FederatedGB federatedGB = new FederatedGB(fp);
        List<CommonResponse> responses = new ArrayList<>();
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        BoostP3Res bp3r1 = new BoostP3Res(clientInfos.get(0), "1", 3); // client1有label，有output from phase3
        BoostP3Res bp3r2 = new BoostP3Res();
        BoostP3Res bp3r3 = new BoostP3Res();
        responses.add(new CommonResponse(clientInfos.get(0), bp3r1));
        responses.add(new CommonResponse(clientInfos.get(1), bp3r2));
        responses.add(new CommonResponse(clientInfos.get(2), bp3r3));
        // request for all clients
        List<CommonRequest> requests = federatedGB.control(responses);
        Assert.assertEquals(requests.size(), 3);
        BoostP4Req bp4r1 = (BoostP4Req) requests.get(0).getBody();
        BoostP4Req bp4r2 = (BoostP4Req) requests.get(1).getBody();
        BoostP4Req bp4r3 = (BoostP4Req) requests.get(2).getBody();
        Assert.assertEquals(bp4r1.isAccept(), true);
        Assert.assertEquals(bp4r1.getClient(), clientInfos.get(0));
        Assert.assertEquals(bp4r1.getkOpt(), 1);
        Assert.assertEquals(bp4r1.getvOpt(), 3);
        Assert.assertEquals(bp4r2.isAccept(), false);
        Assert.assertEquals(bp4r3.isAccept(), false);
    }

    @Test
    public void controlPhase5() {
        FederatedGB federatedGB = new FederatedGB(fp);
        List<CommonResponse> responses = new ArrayList<>();
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        LeftTreeInfo lti1 = new LeftTreeInfo(1, new int[]{1, 2});
        LeftTreeInfo lti2 = new LeftTreeInfo(0, null);
        responses.add(new CommonResponse(clientInfos.get(0), lti1));
        responses.add(new CommonResponse(clientInfos.get(1), lti2));
        List<CommonRequest> requests = federatedGB.control(responses);
        Assert.assertEquals(requests.size(), 2);
        Assert.assertEquals(requests.get(0).getPhase(), 5);
        LeftTreeInfo lti1_out = (LeftTreeInfo) requests.get(0).getBody();
        LeftTreeInfo lti2_out = (LeftTreeInfo) requests.get(1).getBody();
        Assert.assertEquals(lti1_out.getLeftInstances(), new int[]{1, 2});
        Assert.assertEquals(lti2_out.getLeftInstances(), new int[]{1, 2});
        Assert.assertEquals(requests.get(0).getClient(), clientInfos.get(0));
        Assert.assertEquals(lti1_out.getRecordId(), 1);
        Assert.assertEquals(lti2_out.getRecordId(), 1);

    }

    @Test
    public void inferenceControlElseBranch() {
        /**
         * Test else branch
         */
        FederatedGB federatedGB = new FederatedGB(fp);
        federatedGB.setInferencePhase(4);
        List<CommonResponse> responses = new ArrayList<>();
        try {
            federatedGB.inferenceControl(responses);
        } catch (UnsupportedOperationException e) {
            Assert.assertEquals(e.getMessage(), null);
        }
    }

    @Test
    public void inferenceInit() {
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        FederatedGB federatedGB = new FederatedGB(fp);
        String[] predictUid = new String[]{"1", "2"};
        List<CommonRequest> requests = federatedGB.initInference(clientInfos, predictUid,new HashMap<>());

        Assert.assertEquals(requests.size(), 3);
        CommonRequest request = requests.stream().findAny().get();
        Assert.assertEquals(((InferenceInit) request.getBody()).getUid(), predictUid);
    }

    @Test
    public void inferenceControl1() {
        FederatedGB federatedGB = new FederatedGB(fp);
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        List<CommonResponse> responses = new ArrayList<>();
        List<Integer> idList1 = new ArrayList<>();
        List<Integer> idList2 = new ArrayList<>();
        List<Integer> idList3 = new ArrayList<>();
        idList1.add(1);
        idList1.add(2);
        idList2.add(3);
        idList2.add(4);
        idList3.add(0);

        responses.add(new CommonResponse(clientInfos.get(0), new InferenceInitRes(false, idList1)));
        responses.add(new CommonResponse(clientInfos.get(1), new InferenceInitRes(false, idList2)));
        responses.add(new CommonResponse(clientInfos.get(2), new InferenceInitRes(false, idList3)));

        int inferencePhase = -1;
        int numClass = 1;
        double[][] scores = new double[][]{{}};
        Boolean isStopInference = false;
        String[] originIdArray = new String[]{"0", "1", "2", "3", "4"};
        Tuple4<List<CommonRequest>, Boolean, double[][], int[]> res = federatedGB.inferenceControl1(
                responses,
                originIdArray,
                isStopInference,
                scores,
                numClass,
                inferencePhase);
        Assert.assertTrue(res._2());
        Assert.assertEquals(res._1().get(0).getClient(), clientInfos.get(0));
        String[] sa = ((StringArray) res._1().get(0).getBody()).getData();
        Assert.assertEquals(sa.length, 0);

    }

    @Test
    public void inferenceControl() {
        FederatedGB federatedGB = new FederatedGB(fp);
        federatedGB.setInferencePhase(2);
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        List<CommonResponse> responses = new ArrayList<>();
        responses.add(new CommonResponse(clientInfos.get(0), new EmptyMessage()));
        try {
            federatedGB.inferenceControl(responses);
        } catch (UnsupportedOperationException e) {
            Assert.assertNull(e.getMessage());
        }


    }

    @Test
    public void inference2_1() {
        /**
         * Root for all trees from train are leaves
         */
        List<Tree> queryTree = null;
        double[][] scores = new double[][]{};
        boolean isStopInference = false;
        int inferencePhase = -1;
        int numClass = 1;
        double firstRoundPred = 0.0;
        int[] idIndexArray = {0, 1, 2, 3};
        List<Double> multiClassUniqueLabelList = new ArrayList<>();
        multiClassUniqueLabelList.add(0.0);
        multiClassUniqueLabelList.add(1.0);
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        FederatedGB federatedGB = new FederatedGB(fp, queryTree, scores, isStopInference, inferencePhase, numClass,
                firstRoundPred, multiClassUniqueLabelList, idIndexArray, clientInfos);
        List<CommonResponse> responses = new ArrayList<>();
        // tree from train
        List<Tree> trainTree = new ArrayList<>();
        // root1
        TreeNode root1 = new TreeNode(1, 1, clientInfos.get(0), 1, 0.0);
        root1.isLeaf = true;
        // root2
        TreeNode root2 = new TreeNode(1, 2, clientInfos.get(0), 1, 0.0);
        root2.isLeaf = true;

        trainTree.add(new Tree(root1));
        trainTree.add(new Tree(root2));
        BoostN1Res bn1r = new BoostN1Res(trainTree, firstRoundPred, multiClassUniqueLabelList);
        responses.add(new CommonResponse(clientInfos.get(0), bn1r));
        List<CommonRequest> cr = federatedGB.inferenceControl(responses);
        Assert.assertEquals(cr.size(), 0);
    }


    @Test
    public void inference2_2() {

        List<Tree> queryTree = null;
        double[][] scores = new double[][]{};
        boolean isStopInference = false;
        int inferencePhase = -1;
        int numClass = 1;
        double firstRoundPred = 0.0;
        int[] idIndexArray = {0, 1, 2, 3};
        List<Double> multiClassUniqueLabelList = new ArrayList<>();
        multiClassUniqueLabelList.add(0.0);
        multiClassUniqueLabelList.add(1.0);
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        FederatedGB federatedGB = new FederatedGB(fp, queryTree, scores, isStopInference, inferencePhase, numClass,
                firstRoundPred, multiClassUniqueLabelList, idIndexArray, clientInfos);
        List<CommonResponse> responses = new ArrayList<>();
        // tree from train
        List<Tree> trainTree = new ArrayList<>();
        // root1
        TreeNode root1 = new TreeNode(1, 1, clientInfos.get(0), 1, 0.0);
        // root2
        TreeNode root2 = new TreeNode(1, 2, clientInfos.get(0), 1, 0.0);
        trainTree.add(new Tree(root1));
        trainTree.add(new Tree(root2));
        BoostN1Res bn1r = new BoostN1Res(trainTree, firstRoundPred, multiClassUniqueLabelList);
        responses.add(new CommonResponse(clientInfos.get(0), bn1r));
        List<CommonRequest> cr = federatedGB.inferenceControl(responses);
        Assert.assertEquals(cr.size(), 1); // only 1 client
        Assert.assertEquals(cr.get(0).getPhase(), -2);
//        Assert.assertEquals(cr.get(0).getBody());
        System.out.println(cr.get(0).getBody());
        System.out.println(cr.get(0).getPhase());
    }

    @Test
    public void inference2_3() {
        /**
         * QueryTree already initiated
         */
//        double[][] scores = new double[][]{};
//        boolean isStopInference = false;
//        int inferencePhase = -1;
//        int numClass = 1;
//        double firstRoundPred = 0.0;
//        int[] idIndexArray = {0, 1, 2, 3};
//        List<Double> multiClassUniqueLabelList = new ArrayList<>();
//        multiClassUniqueLabelList.add(0.0);
//        multiClassUniqueLabelList.add(1.0);
//        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
//
//
//        List<CommonResponse> responses = new ArrayList<>();
//        // tree from train
//        List<Tree> trainTree = new ArrayList<>();
//        // root1
//        TreeNode root1 = new TreeNode(1, 1, clientInfos.get(0), 1, 0.0);
//        // root2
//        TreeNode root2 = new TreeNode(1, 2, clientInfos.get(0), 1, 0.0);
//        trainTree.add(new Tree(root1));
//        trainTree.add(new Tree(root2));
//        List<Tree> queryTree = trainTree;
//
//        BoostN1Res bn1r = new BoostN1Res(trainTree, firstRoundPred, multiClassUniqueLabelList);
//        FederatedGB federatedGB = new FederatedGB(new FgbParameter(), queryTree, scores, isStopInference, inferencePhase, numClass,
//                firstRoundPred, multiClassUniqueLabelList, idIndexArray, clientInfos);
//
//
//        responses.add(new CommonResponse(clientInfos.get(0), bn1r));
//        List<CommonRequest> cr = federatedGB.inferenceControl(responses);

    }

    @Test
    public void postInferenceControl() {
        List<ClientInfo> clientInfos = StructureGenerate.threeClients();
        List<Tree> queryTree = null;
        boolean isStopInference = false;
        int inferencePhase = -1;
        int numClass = 1;
        double firstRoundPred = 0.0;
        int[] idIndexArray = {0, 1};
        List<Double> multiClassUniqueLabelList = new ArrayList<>();
        multiClassUniqueLabelList.add(0.0);
        multiClassUniqueLabelList.add(1.0);
        String[] originIdArray = {"0", "1", "2", "3"};
        // all nan
        double[][] scores = new double[][]{{Double.NaN, Double.NaN, Double.NaN, Double.NaN}};
        FederatedGB federatedGB = new FederatedGB(fp, queryTree, scores, isStopInference, inferencePhase, numClass,
                firstRoundPred, multiClassUniqueLabelList, idIndexArray, clientInfos);
        List<CommonResponse> responses = new ArrayList<>();
        responses.add(new CommonResponse(clientInfos.get(0), null));
        double[][] res = federatedGB.postInferenceControl(responses).getPredicts();
        for (double[] d : res) {
            Assert.assertEquals(d[0], Double.NaN);
        }
        Assert.assertEquals(res.length, 4);
        //
        double[][] scores2 = new double[][]{{0.3, 0.8, 0.9, 0.2}};
//        double[][] scores2 = new double[][]{{100,200,22,222}};

        ObjectiveType[] types = new ObjectiveType[]{ObjectiveType.regLogistic, ObjectiveType.regSquare, ObjectiveType.countPoisson,
                ObjectiveType.binaryLogistic};
        MetricType[] metricTypes = new MetricType[]{MetricType.CROSS_ENTRO, MetricType.ACC, MetricType.AUC};
        for (ObjectiveType ot : types) {
            FgbParameter fp = new FgbParameter.Builder(50, metricTypes, ot).maxDepth(5).build();
            FederatedGB federatedGB2 = new FederatedGB(fp, queryTree, scores2, isStopInference, inferencePhase, numClass,
                    firstRoundPred, multiClassUniqueLabelList, idIndexArray, clientInfos, originIdArray);
            double[][] res2 = federatedGB2.postInferenceControl(responses).getPredicts();
            System.out.println("res2" + Arrays.deepToString(res2));
            Assert.assertEquals(res2.length, 4);
            // TODO more Assert

            // TODO  ObjectiveType Null
        }


    }

    @Test
    public void isContinue() {
        FederatedGB federatedGB = new FederatedGB(fp);
        federatedGB.setNumRound(100);
        Assert.assertFalse(federatedGB.isContinue());
        federatedGB.setNumRound(0);
        Assert.assertTrue(federatedGB.isContinue());
    }

    @Test
    public void isInferenceContinue() {
        FederatedGB federatedGB = new FederatedGB(fp);
        boolean res = federatedGB.isInferenceContinue();
        Assert.assertEquals(res, !federatedGB.isStopInference());
    }

    @Test
    public void getAlgorithmType() {
        FederatedGB federatedGB = new FederatedGB(fp);
        Assert.assertEquals(federatedGB.getAlgorithmType(), AlgorithmType.FederatedGB);
    }

    @Test
    public void metric() {
        FgbParameter fgbParameter = new FgbParameter.Builder(3, new MetricType[]{MetricType.RMSE}, ObjectiveType.regSquare).build();
        FederatedGB federatedGB = new FederatedGB(fgbParameter);
        Map<MetricType, List<Pair<Integer, Double>>> target = new HashMap<>();
        List<Pair<Integer, Double>> l = new ArrayList<Pair<Integer, Double>>();
        l.add(new Pair<>(0, -Double.MAX_VALUE));
        target.put(MetricType.RMSE, l);
        Assert.assertEquals(federatedGB.readMetrics(), new MetricValue(target));

    }
}
