package com.jdt.fedlearn.core.dispatch;

import com.jdt.fedlearn.core.dispatch.mixLinear.LinearRegression;
import com.jdt.fedlearn.core.encryption.distributedPaillier.DistributedPaillierNative.signedByteArray;
import com.jdt.fedlearn.core.encryption.distributedPaillier.HomoEncryptionDebugUtil;
import com.jdt.fedlearn.core.encryption.distributedPaillier.HomoEncryptionUtil;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.base.Double2dArray;
import com.jdt.fedlearn.core.entity.base.EmptyMessage;
import com.jdt.fedlearn.core.entity.base.IntArray;
import com.jdt.fedlearn.core.entity.base.StringArray;
import com.jdt.fedlearn.core.entity.common.*;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.entity.mixedLinearRegression.*;
import com.jdt.fedlearn.core.entity.psi.MatchResourceLinReg;
import com.jdt.fedlearn.core.fake.StructureGenerate;
import com.jdt.fedlearn.core.math.MathExt;
import com.jdt.fedlearn.core.parameter.LinearParameter;
import com.jdt.fedlearn.core.type.AlgorithmType;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

import static com.jdt.fedlearn.core.util.TypeConvUtils.toJsons;

public class TestLinearRegression {
    // encryption and decryption settings
    private static final int n = 3;
    private static final int l = 1024;
    private static final long maxNegAbs = Long.MAX_VALUE;
    private static final boolean usingFakeEnc = true;
    private static final HomoEncryptionUtil key = new HomoEncryptionUtil(n, l, usingFakeEnc);
    private static HomoEncryptionDebugUtil decHelper = null;

    // common mock settings for LinReg
    private static LinearRegression.LinRegTrainInitParams linRegTrainInitParams;
    private static final List<ClientInfo> clientInfos = StructureGenerate.threeClients();
    private static final Map<ClientInfo, double[]> clientFeatMap = new HashMap<>();

    @BeforeMethod
    public void setUp() {
        key.generateKeys();
        if(usingFakeEnc){
            decHelper = new HomoEncryptionDebugUtil(true) ;
        } else {
            decHelper = new HomoEncryptionDebugUtil(key.getPk(), key.getSkAll(), key.getN(), maxNegAbs);
        }

        // clientFeatMap has the same size as M.
        clientFeatMap.put(clientInfos.get(0), new double[]{1, 1, 1, 0, 0, 0, 0, 1});
        clientFeatMap.put(clientInfos.get(1), new double[]{0, 0, 1, 1, 1, 0, 0, 1});
        clientFeatMap.put(clientInfos.get(2), new double[]{0, 0, 0, 0, 1, 1, 1, 1});
        linRegTrainInitParams = new LinearRegression.LinRegTrainInitParams(3, 7, 2, 1, 1,
                new LinearParameter(), new double[]{0, 0, 0, 0, 0, 0, 0}, 7, clientFeatMap);
    }


    @Test
    public void getNextPhase(){
        Map<Integer,Integer> inOutPair = new HashMap<>();
        inOutPair.put(0,1);
        inOutPair.put(1,2);
        inOutPair.put(9,1);
        LinearRegression linearRegression = new LinearRegression(new LinearParameter());
        for (Map.Entry<Integer, Integer> entry:inOutPair.entrySet()){
            int p = linearRegression.getNextPhase(entry.getKey());
            Assert.assertEquals(p, entry.getValue().intValue());
        }
    }

    @Test
    public void initControl(){
        Map<ClientInfo, Features> features = StructureGenerate.linRegFeatures(clientInfos);
        LinearRegression linearRegression = new LinearRegression(new LinearParameter());

        List<CommonRequest> requests = linearRegression.initControl(clientInfos, null, features, new HashMap<>());
        Assert.assertEquals(clientInfos.size(), requests.size());
        CommonRequest first = requests.get(0);
        Assert.assertEquals(first.getPhase(),0);
        Assert.assertFalse(first.isSync());
        Message message = first.getBody();
        TrainInit body = (TrainInit) message;
        Assert.assertEquals(body.getFeatureList(), features.get(clientInfos.get(0)));
    }

    @Test
    public void trainIdMatchingPhase2GetIdMatchingRes(){
        int phase = 2;
        LinearRegression linearRegression = new LinearRegression(new LinearParameter());

        // mock client response
        List<Message> msgs =  new ArrayList<Message>(){};
        msgs.add(new MatchResourceLinReg(new String[]{"f1", "f2", "f3"}, new String[]{"u1", "u2", "u3"},  new double[]{1, 2, 3}));
        msgs.add(new MatchResourceLinReg(new String[]{"f3", "f4", "f5"}, new String[]{"u3", "u4", "u5"},  new double[]{1, 2, 3}));
        msgs.add(new MatchResourceLinReg(new String[]{"f5", "f6", "f7"}, new String[]{"u5", "u6", "u7"},  new double[]{1, 2, 3}));
        List<CommonResponse> response = packageThreeMsgsIntoCommonResponses(msgs);

        linearRegression.setPhase(phase-1);
        List<CommonRequest> requests = linearRegression.control(response);
        CommonRequest first = requests.get(0);
        LinearRegressionTrainInitOthers message = (LinearRegressionTrainInitOthers) first.getBody();

        Assert.assertEquals(message.getFullM(), 7+1); // M is plussed one for b in wx+b
        Assert.assertEquals(message.getFullN(), 7);
    }

    @Test
    public void controlPhase1SendEmptyReq(){
        int phase = 3;
        LinearRegression linearRegression = new LinearRegression(new LinearParameter());

        List<Message> msgs =  new ArrayList<Message>(){};
        msgs.add(new EmptyMessage());
        msgs.add(new EmptyMessage());
        msgs.add(new EmptyMessage());
        List<CommonResponse> responses = packageThreeMsgsIntoCommonResponses(msgs);

        linearRegression.setPhase(phase-1);
        List<CommonRequest> requests = linearRegression.control(responses);
        CommonRequest first = requests.get(0);
        Message message =  first.getBody();

        Assert.assertTrue(message instanceof EmptyMessage);
    }

    @Test
    public void controlPhase2GetPartialYHat(){
        int phase = 4;
        LinearRegression linearRegression = new LinearRegression(new LinearParameter());
        linearRegression.setKeyForTest(key.getPk(), key.getSk(), key.getSkAll());
        linearRegression.trainParamInit(linRegTrainInitParams);


        // mock client response
        signedByteArray[][] y_hat_part_enc_lst = new signedByteArray[3][];
        y_hat_part_enc_lst[0] = key.encryption(new double[]{1d, 2d, 3d}, key.getPk());
        y_hat_part_enc_lst[1] = key.encryption(new double[]{1d, 2d, 3d}, key.getPk());
        y_hat_part_enc_lst[2] = key.encryption(new double[]{1d, 2d, 3d}, key.getPk());
        List<Message> msgs =  new ArrayList<Message>(){};
        msgs.add(new CypherMessage(y_hat_part_enc_lst[0]));
        msgs.add(new CypherMessage(y_hat_part_enc_lst[1]));
        msgs.add(new CypherMessage(y_hat_part_enc_lst[2]));
        List<CommonResponse> responses = packageThreeMsgsIntoCommonResponses(msgs);

        // calling target func
        linearRegression.setPhase(phase-1);
        List<CommonRequest> requests = linearRegression.control(responses);

        // assertions
        Assert.assertEquals(
                decHelper.decDouble( ((CypherMessage)(requests.get(0).getBody())).getBody()),
                new double[]{3, 6, 9});
    }

    @Test
    public void controlPhase3ComputeGEnc(){
        int phase = 5;
        LinearRegression linearRegression = new LinearRegression(new LinearParameter());
        linearRegression.setKeyForTest(key.getPk(), key.getSk(), key.getSkAll());
        linearRegression.trainParamInit(linRegTrainInitParams);

        // mock client response
        signedByteArray[][] gNonPrivLstEnc = new signedByteArray[3][];
        gNonPrivLstEnc[0] = key.encryption(new double[]{1d, 2d, 3d, 1d, 2d, 3d, 1d, 2d}, key.getPk());
        gNonPrivLstEnc[1] = key.encryption(new double[]{1d, 2d, 3d, 1d, 2d, 3d, 1d, 2d}, key.getPk());
        gNonPrivLstEnc[2] = key.encryption(new double[]{1d, 2d, 3d, 1d, 2d, 3d, 1d, 2d}, key.getPk());
        List<Message> msgs =  new ArrayList<Message>(){};
        msgs.add(new TwoCypherMessage( new CypherMessage(gNonPrivLstEnc[0]), new CypherMessage(gNonPrivLstEnc[0])));
        msgs.add(new TwoCypherMessage( new CypherMessage(gNonPrivLstEnc[1]), new CypherMessage(gNonPrivLstEnc[1])));
        msgs.add(new TwoCypherMessage( new CypherMessage(gNonPrivLstEnc[2]), new CypherMessage(gNonPrivLstEnc[2])));
        List<CommonResponse> responses = packageThreeMsgsIntoCommonResponses(msgs);

        // calling target func
        linearRegression.setPhase(phase-1);
        List<CommonRequest> requests = linearRegression.control(responses);

        // assertions
        // computeNonPrivGEnc: g->2*3*g; adding privG; 6*g->6*g+3*g
        Assert.assertEquals(
                decHelper.decDouble( ((CypherMessage)(requests.get(0).getBody())).getBody()),
                Arrays.stream(new double[]{1d, 2d, 3d, 1d, 2d, 3d, 1d, 2d}).map(x->x*9).toArray());
    }

    @Test
    public void controlPhase4BCastPartialDecG(){
        int phase = 6;
        LinearRegression linearRegression = new LinearRegression(new LinearParameter());
        linearRegression.setKeyForTest(key.getPk(), key.getSk(), key.getSkAll());
        linearRegression.trainParamInit(linRegTrainInitParams);

        // mock client response
        Map<String, signedByteArray[]> gInfo1 = new HashMap<>();
        Map<String, signedByteArray[]> gInfo2 = new HashMap<>();
        Map<String, signedByteArray[]> gInfo3 = new HashMap<>();
        signedByteArray [] mockEncDat1 = key.encryption(new double[]{1d, 1d}, key.getPk());
        signedByteArray [] mockEncDat2 = key.encryption(new double[]{2d, 2d}, key.getPk());
        signedByteArray [] mockEncDat3 = key.encryption(new double[]{3d, 3d}, key.getPk());
        gInfo1.put(clientInfos.get(1).getIp()+clientInfos.get(1).getPort(), mockEncDat1);
        gInfo1.put(clientInfos.get(2).getIp()+clientInfos.get(2).getPort(), mockEncDat1);
        gInfo2.put(clientInfos.get(0).getIp()+clientInfos.get(0).getPort(), mockEncDat2);
        gInfo2.put(clientInfos.get(2).getIp()+clientInfos.get(2).getPort(), mockEncDat2);
        gInfo3.put(clientInfos.get(0).getIp()+clientInfos.get(0).getPort(), mockEncDat3);
        gInfo3.put(clientInfos.get(1).getIp()+clientInfos.get(1).getPort(), mockEncDat3);
        List<Message> msgs =  new ArrayList<Message>(){};
        msgs.add(new PartialDecMessage(gInfo1));
        msgs.add(new PartialDecMessage(gInfo2));
        msgs.add(new PartialDecMessage(gInfo3));
        List<CommonResponse> responses = packageThreeMsgsIntoCommonResponses(msgs);

        // calling target func
        linearRegression.setPhase(phase-1);
        List<CommonRequest> requests = linearRegression.control(responses);

        // assertions
        Assert.assertEquals(
                new double[]{
                        decHelper.decDouble( ((CypherMessageList)(requests.get(0).getBody())).getBody().get(0))[0],
                        decHelper.decDouble( ((CypherMessageList)(requests.get(0).getBody())).getBody().get(1))[0]
                },
                new double[]{2d, 3d});
    }

    @Test
    public void controlPhase5UpdateHessInv(){
        int phase = 7;
        LinearRegression linearRegression = new LinearRegression(new LinearParameter());
        linearRegression.setKeyForTest(key.getPk(), key.getSk(), key.getSkAll());
        linearRegression.trainParamInit(linRegTrainInitParams);

        String new_masked_g = toJsons( key.encryption(new double[]{1d, 2d, 3d, 4d, 5d, 6d, 7d, 8d}, key.getPk()) );
        String[] imRes = new String[6];
        imRes[0] = "[0.14285714285714285,0.2857142857142857,0.42857142857142855,0.5714285714285714,0.7142857142857142,0.8571428571428571,1.0,1.1428571428571428]";
        imRes[1] = "[[0.14285714285714285,0.2857142857142857,0.42857142857142855,0.5714285714285714,0.7142857142857142,0.8571428571428571,1.0,1.1428571428571428],[0.14285714285714285,0.2857142857142857,0.42857142857142855,0.5714285714285714,0.7142857142857142,0.8571428571428571,1.0,1.1428571428571428],[0.14285714285714285,0.2857142857142857,0.42857142857142855,0.5714285714285714,0.7142857142857142,0.8571428571428571,1.0,1.1428571428571428],[0.14285714285714285,0.2857142857142857,0.42857142857142855,0.5714285714285714,0.7142857142857142,0.8571428571428571,1.0,1.1428571428571428],[0.14285714285714285,0.2857142857142857,0.42857142857142855,0.5714285714285714,0.7142857142857142,0.8571428571428571,1.0,1.1428571428571428],[0.14285714285714285,0.2857142857142857,0.42857142857142855,0.5714285714285714,0.7142857142857142,0.8571428571428571,1.0,1.1428571428571428],[0.14285714285714285,0.2857142857142857,0.42857142857142855,0.5714285714285714,0.7142857142857142,0.8571428571428571,1.0,1.1428571428571428],[0.14285714285714285,0.2857142857142857,0.42857142857142855,0.5714285714285714,0.7142857142857142,0.8571428571428571,1.0,1.1428571428571428]]";
        imRes[2] = "[[0.14285714285714285,0.14285714285714285,0.14285714285714285,0.14285714285714285,0.14285714285714285,0.14285714285714285,0.14285714285714285,0.14285714285714285],[0.2857142857142857,0.2857142857142857,0.2857142857142857,0.2857142857142857,0.2857142857142857,0.2857142857142857,0.2857142857142857,0.2857142857142857],[0.42857142857142855,0.42857142857142855,0.42857142857142855,0.42857142857142855,0.42857142857142855,0.42857142857142855,0.42857142857142855,0.42857142857142855],[0.5714285714285714,0.5714285714285714,0.5714285714285714,0.5714285714285714,0.5714285714285714,0.5714285714285714,0.5714285714285714,0.5714285714285714],[0.7142857142857142,0.7142857142857142,0.7142857142857142,0.7142857142857142,0.7142857142857142,0.7142857142857142,0.7142857142857142,0.7142857142857142],[0.8571428571428571,0.8571428571428571,0.8571428571428571,0.8571428571428571,0.8571428571428571,0.8571428571428571,0.8571428571428571,0.8571428571428571],[1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0],[1.1428571428571428,1.1428571428571428,1.1428571428571428,1.1428571428571428,1.1428571428571428,1.1428571428571428,1.1428571428571428,1.1428571428571428]]";
        imRes[3] = "[[1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0],[1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0],[1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0],[1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0],[1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0],[1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0],[1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0],[1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0]]";
        imRes[4] = "[1,1,1,0,0,0,0,1]";
        imRes[5] = "[[1,1,1,0,0,0,0,1],[1,1,1,0,0,0,0,1],[1,1,1,0,0,0,0,1],[0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0],[0,0,0,0,0,0,0,0],[1,1,1,0,0,0,0,1]]";

        String[] res = new String[2];
        res[0] = toJsons(imRes);
        res[1] = new_masked_g;

        List<Message> msgs =  new ArrayList<Message>(){};
        msgs.add(new StringArray(res));
        msgs.add(new StringArray(res));
        msgs.add(new StringArray(res));
        List<CommonResponse> responses = packageThreeMsgsIntoCommonResponses(msgs);
        Map<String, double[]> clientFeatRandWeight = new HashMap<>();
        for(ClientInfo entry: clientInfos) {
            clientFeatRandWeight.put(entry.getIp()+entry.getPort(), new double[]{1.0,1.0,1.0,1.0,1.0,1.0,1.0,1.0});
        }
        linearRegression.setClientFeatRandWeight(clientFeatRandWeight);

        // calling target func
        linearRegression.setPhase(phase-1);
        linearRegression.setIterNum(10);
        List<CommonRequest> requests = linearRegression.control(responses);

        // assertions
        // TODO: need to validate the result
        if(!usingFakeEnc) {
            Assert.assertEquals(
                    decHelper.decDouble(((CypherMessage) (requests.get(0).getBody())).getBody()),
                    new double[]{-7, -7, -7, -7, -7, -7, -7, -7},
                    1);
        } else {
            Assert.assertEquals(
                    decHelper.decDouble(((CypherMessage) (requests.get(0).getBody())).getBody()),
                    new double[]{-0.7, -0.7, -0.7, -0.7, -0.7, -0.7, -0.7, -0.7},
                    0.000001);
        }
    }

    @Test
    public void controlPhase7ReportTrainRes(){
        int phase = 9;
        LinearRegression linearRegression = new LinearRegression(new LinearParameter());
        linearRegression.setKeyForTest(key.getPk(), key.getSk(), key.getSkAll());
        linearRegression.trainParamInit(linRegTrainInitParams);

        // mock client response
        String[]  mockClientResp =  new String[5];
        mockClientResp[0] = toJsons(new double[]{-0.1, -0.2, -0.3, -0.4, -0.5, -0.6, -0.7, -0.8});
        mockClientResp[1] = toJsons(new double[]{1, 2, 3, 4, 5, 6, 7, 8});
        mockClientResp[2] = toJsons(new double[]{1, 1, 1, 1, 1, 2, 2, 1});
        mockClientResp[3] = toJsons(key.encryption(new double[]{-0.1, -0.2, -0.3, -0.4, -0.5, -0.6, -0.7, -0.8}, key.getPk()));
        mockClientResp[4] = toJsons(0.875);
        List<Message> msgs =  new ArrayList<Message>(){};
        msgs.add(new StringArray(mockClientResp));
        msgs.add(new StringArray(mockClientResp));
        msgs.add(new StringArray(mockClientResp));
        List<CommonResponse> responses = packageThreeMsgsIntoCommonResponses(msgs);

        // calling target func
        linearRegression.setPhase(phase-1);
        linearRegression.control(responses);

        // this func output EmptyRequest
    }

    @Test
    public void initInference() {
        LinearRegression linearRegression = new LinearRegression(new LinearParameter());
        linearRegression.setKeyForTest(key.getPk(), key.getSk(), key.getSkAll());
        linearRegression.trainParamInit(linRegTrainInitParams);

        List<CommonRequest> requests = linearRegression.initInference(clientInfos, new String[]{"u1", "u2", "u3"});
        Assert.assertEquals(clientInfos.size(), requests.size());
        CommonRequest first = requests.get(0);
        Assert.assertEquals(first.getPhase(),-1);
        Assert.assertFalse(first.isSync());
        Message message = first.getBody();
        InferenceInit body = (InferenceInit) message;
        Assert.assertEquals(body.getOthers().get("numP"), 3);
        Assert.assertEquals(body.getOthers().get("encBits"), 1024);
    }

    @Test
    public void testInferIdMatchingPhase1GetIllegalData() {
        int phase = -2;
        LinearRegression linearRegression = new LinearRegression(new LinearParameter());
        linearRegression.setKeyForTest(key.getPk(), key.getSk(), key.getSkAll());

        // mock client response
        InferenceInitRes initMsg1 = new InferenceInitRes(true, new int[]{1, 2, 3});
        InferenceInitRes initMsg2 = new InferenceInitRes(true, new int[]{1, 12, 13});
        InferenceInitRes initMsg3 = new InferenceInitRes(true, new int[]{1, 22, 33});
        List<Message> msgs =  new ArrayList<Message>(){};
        msgs.add(initMsg1);
        msgs.add(initMsg2);
        msgs.add(initMsg3);
        List<CommonResponse> responses = packageThreeMsgsIntoCommonResponses(msgs);
        // calling target func
        linearRegression.setPhase(phase+1);
        List<CommonRequest> requests = linearRegression.inferenceControl(responses);
        Assert.assertEquals(((IntArray) requests.get(0).getBody()).getData()[0], 1);
    }

    @Test
    public void inferIdMatchingPhase2() {
        int phase = -3;
        LinearRegression linearRegression = new LinearRegression(new LinearParameter());
        linearRegression.setKeyForTest(key.getPk(), key.getSk(), key.getSkAll());

        // mock client response
        List<Message> msgs =  new ArrayList<Message>(){};
        msgs.add(new MatchResourceLinReg(new String[]{"f1", "f2", "f3"}, new String[]{"u1", "u2", "u3"},  new double[]{1, 2, 3}));
        msgs.add(new MatchResourceLinReg(new String[]{"f3", "f4", "f5"}, new String[]{"u3", "u4", "u5"},  new double[]{1, 2, 3}));
        msgs.add(new MatchResourceLinReg(new String[]{"f5", "f6", "f7"}, new String[]{"u5", "u6", "u7"},  new double[]{1, 2, 3}));
        List<CommonResponse> response = packageThreeMsgsIntoCommonResponses(msgs);

        linearRegression.setPhase(phase+1);
        List<CommonRequest> requests = linearRegression.inferenceControl(response);
        CommonRequest first = requests.get(0);
        LinearRegressionInferInitOthers message = (LinearRegressionInferInitOthers) first.getBody();

        Assert.assertEquals(message.getFullM(), 7+1); // M is plussed one for b in wx+b
        Assert.assertEquals(message.getFullN(), 7);
    }

    @Test
    public void inferPhase2SumOverAllYHatP() {
        int phase = -5;
        LinearRegression linearRegression = new LinearRegression(new LinearParameter());
        linearRegression.setKeyForTest(key.getPk(), key.getSk(), key.getSkAll());
        linearRegression.trainParamInit(linRegTrainInitParams);

        // mock client response
        signedByteArray[][] y_hat_part_enc_lst = new signedByteArray[3][];
        y_hat_part_enc_lst[0] = key.encryption(new double[]{1d, 2d, 3d}, key.getPk());
        y_hat_part_enc_lst[1] = key.encryption(new double[]{1d, 2d, 3d}, key.getPk());
        y_hat_part_enc_lst[2] = key.encryption(new double[]{1d, 2d, 3d}, key.getPk());
        List<Message> msgs =  new ArrayList<Message>(){};
        msgs.add(new CypherMessage(y_hat_part_enc_lst[0]));
        msgs.add(new CypherMessage(y_hat_part_enc_lst[1]));
        msgs.add(new CypherMessage(y_hat_part_enc_lst[2]));
        List<CommonResponse> responses = packageThreeMsgsIntoCommonResponses(msgs);

        // calling target func
        linearRegression.setPhase(phase+1);
        List<CommonRequest> requests = linearRegression.inferenceControl(responses);

        // assertions
        Assert.assertEquals(
                decHelper.decDouble( ((CypherMessage)(requests.get(0).getBody())).getBody()),
                new double[]{3, 6, 9});
    }

    @Test
    public void postInferenceControl() {
        double[][] yHat = new double[2][];
        yHat[0] = new double[]{1d, 2d, 3d};
        yHat[1] = new double[]{1d, 2d, 3d};
        List<Message> msgs =  new ArrayList<Message>(){};
        List<double[]> list = new ArrayList<>();
        list.add(yHat[0]);
        list.add(yHat[1]);
        list.add(yHat[1]);
        Double2dArray double2DArray = new Double2dArray(list);
//        msgs.add(new DoubleMessageTriple(new DoubleArray(yHat[0]), new DoubleArray(yHat[1]), new DoubleArray(yHat[1])));
//        msgs.add(new DoubleMessageTriple(new DoubleArray(yHat[0]), new DoubleArray(yHat[1]), new DoubleArray(yHat[1])));
//        msgs.add(new DoubleMessageTriple(new DoubleArray(yHat[0]), new DoubleArray(yHat[1]), new DoubleArray(yHat[1])));
        msgs.add(double2DArray);
        msgs.add(double2DArray);
        msgs.add(double2DArray);
        List<CommonResponse> response = packageThreeMsgsIntoCommonResponses(msgs);

        LinearRegression linearRegression = new LinearRegression(new LinearParameter());
        double[][] res = linearRegression.postInferenceControl(response).getPredicts();
        Assert.assertEquals(MathExt.transpose(res)[0], new double[]{1d, 2d, 3d, 1d, 2d, 3d, 1d, 2d, 3d});
    }

    @Test
    public void serialize() {
        LinearRegression linearRegression = new LinearRegression(new LinearParameter());
        linearRegression.trainParamInit(linRegTrainInitParams);

        Assert.assertEquals(linearRegression.serialize(),
                "modelToken=" + 1 + "\n" +
                        "M=" + 8 + "\n" +
                        "numP=" + 3 + "\n");
    }

    @Test
    public void isContinue() {
        LinearRegression linearRegression = new LinearRegression(new LinearParameter());
        linearRegression.trainParamInit(linRegTrainInitParams);

        Assert.assertTrue(linearRegression.isContinue());
    }

    @Test
    public void testGetAlgorithmType() {
        LinearRegression linearRegression = new LinearRegression(new LinearParameter());
        Assert.assertEquals(linearRegression.getAlgorithmType(), AlgorithmType.LinearRegression);
    }

    @Test
    public void testIsInferenceContinue() {
        LinearRegression linearRegression = new LinearRegression(new LinearParameter());
        Assert.assertTrue(linearRegression.isInferenceContinue());
    }


    @Test
    public void testMetric() {
        LinearRegression linearRegression = new LinearRegression(new LinearParameter());
        Assert.assertEquals(linearRegression.readMetrics().getMetrics(), new HashMap<>());
    }

    private List<CommonResponse> packageThreeMsgsIntoCommonResponses(List<Message> in){
        List<CommonResponse> out = new ArrayList<>();
        out.add(new CommonResponse(TestLinearRegression.clientInfos.get(0), in.get(0)));
        out.add(new CommonResponse(TestLinearRegression.clientInfos.get(1), in.get(1)));
        out.add(new CommonResponse(TestLinearRegression.clientInfos.get(2), in.get(2)));
        return out;
    }
}
