//package com.jdt.fedlearn.client.inference;
//
//import ch.qos.logback.core.joran.spi.JoranException;
//import com.jdt.fedlearn.client.entity.inference.InferenceRequest;
//import com.jdt.fedlearn.client.service.InferenceService;
//import com.jdt.fedlearn.client.util.ConfigUtil;
//import com.jdt.fedlearn.client.util.HttpUtil;
//import com.jdt.fedlearn.client.util.JsonUtil;
//import com.jdt.fedlearn.common.entity.core.Message;
//import com.jdt.fedlearn.core.entity.boost.*;
//import com.jdt.fedlearn.core.entity.common.InferenceInit;
//import com.jdt.fedlearn.core.entity.mixGB.BoostEvalQueryReqBody;
//import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;
//import com.jdt.fedlearn.core.util.SerializeUtil;
//import com.jdt.fedlearn.core.util.TokenUtil;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.testng.Assert;
//import org.testng.annotations.BeforeClass;
//
//import java.io.IOException;
//import java.util.*;
//
///**
// * @className: InferenceTest
// * @description:
// * @author: geyan29
// * @date: 2020/12/8 10:52
// **/
//public class InferenceTest {
//
//    private Logger logger = LoggerFactory.getLogger(this.getClass());
//    private static final Random random = new Random();
//    private static final String MODEL_TOKEN_KEY = "modelToken";
//    private static final String ALGORITHM_KEY = "algorithm";
//    private static final String INFERENCE_ID_KEY = "inferenceId";
//    private static final String DATA_KEY = "data";
//    private static final String PHASE_KEY = "phase";
//    private static final String UID_KEY = "uid";
//    private static final String EXPECT_255 = "{\"instanceId\":[],\"recordId\":0}";
//    private static final String EXPECT_1 = "{\"trees\":null,\"firstRoundPred\":0.0,\"multiClassUniqueLabelList\":null}";
//    private static final String EXPECT_2 = "{\"client\":null,\"bodies\":[{\"uid\":\"0\",\"treeIndex\":0,\"split\":\"left\"},{\"uid\":\"0\",\"treeIndex\":1,\"split\":\"left\"},{\"uid\":\"1\",\"treeIndex\":0,\"split\":\"right\"},{\"uid\":\"1\",\"treeIndex\":1,\"split\":\"right\"},{\"uid\":\"2\",\"treeIndex\":0,\"split\":\"right\"},{\"uid\":\"2\",\"treeIndex\":1,\"split\":\"right\"},{\"uid\":\"3\",\"treeIndex\":0,\"split\":\"right\"},{\"uid\":\"3\",\"treeIndex\":1,\"split\":\"right\"},{\"uid\":\"4\",\"treeIndex\":0,\"split\":\"left\"},{\"uid\":\"4\",\"treeIndex\":1,\"split\":\"left\"},{\"uid\":\"5\",\"treeIndex\":0,\"split\":\"right\"},{\"uid\":\"5\",\"treeIndex\":1,\"split\":\"right\"},{\"uid\":\"6\",\"treeIndex\":0,\"split\":\"left\"},{\"uid\":\"6\",\"treeIndex\":1,\"split\":\"left\"},{\"uid\":\"7\",\"treeIndex\":0,\"split\":\"left\"},{\"uid\":\"7\",\"treeIndex\":1,\"split\":\"left\"},{\"uid\":\"8\",\"treeIndex\":0,\"split\":\"right\"},{\"uid\":\"8\",\"treeIndex\":1,\"split\":\"right\"},{\"uid\":\"9\",\"treeIndex\":0,\"split\":\"left\"},{\"uid\":\"9\",\"treeIndex\":1,\"split\":\"left\"},{\"uid\":\"10\",\"treeIndex\":0,\"split\":\"right\"},{\"uid\":\"10\",\"treeIndex\":1,\"split\":\"right\"},{\"uid\":\"11\",\"treeIndex\":0,\"split\":\"right\"},{\"uid\":\"11\",\"treeIndex\":1,\"split\":\"right\"},{\"uid\":\"12\",\"treeIndex\":0,\"split\":\"right\"},{\"uid\":\"12\",\"treeIndex\":1,\"split\":\"right\"},{\"uid\":\"13\",\"treeIndex\":0,\"split\":\"left\"},{\"uid\":\"13\",\"treeIndex\":1,\"split\":\"left\"},{\"uid\":\"14\",\"treeIndex\":0,\"split\":\"right\"},{\"uid\":\"14\",\"treeIndex\":1,\"split\":\"right\"},{\"uid\":\"15\",\"treeIndex\":0,\"split\":\"right\"},{\"uid\":\"15\",\"treeIndex\":1,\"split\":\"right\"},{\"uid\":\"16\",\"treeIndex\":0,\"split\":\"right\"},{\"uid\":\"16\",\"treeIndex\":1,\"split\":\"right\"},{\"uid\":\"17\",\"treeIndex\":0,\"split\":\"right\"},{\"uid\":\"17\",\"treeIndex\":1,\"split\":\"right\"},{\"uid\":\"18\",\"treeIndex\":0,\"split\":\"left\"},{\"uid\":\"18\",\"treeIndex\":1,\"split\":\"left\"},{\"uid\":\"19\",\"treeIndex\":0,\"split\":\"right\"},{\"uid\":\"19\",\"treeIndex\":1,\"split\":\"right\"},{\"uid\":\"20\",\"treeIndex\":0,\"split\":\"right\"},{\"uid\":\"20\",\"treeIndex\":1,\"split\":\"right\"},{\"uid\":\"21\",\"treeIndex\":0,\"split\":\"right\"},{\"uid\":\"21\",\"treeIndex\":1,\"split\":\"right\"},{\"uid\":\"22\",\"treeIndex\":0,\"split\":\"left\"},{\"uid\":\"22\",\"treeIndex\":1,\"split\":\"left\"},{\"uid\":\"23\",\"treeIndex\":0,\"split\":\"left\"},{\"uid\":\"23\",\"treeIndex\":1,\"split\":\"left\"},{\"uid\":\"24\",\"treeIndex\":0,\"split\":\"left\"},{\"uid\":\"24\",\"treeIndex\":1,\"split\":\"left\"}]}";
//    private static Map<Integer, String> expectMap = new HashMap<>(64);
//    private static int[] allUids = new int[]{8088, 19459, 17750, 13821, 16683, 10760, 19790, 13415, 1692, 9656, 18683, 16680, 10018, 7284,
//            14483, 15498, 11060, 8875, 17274, 9810, 6334, 11606, 2680, 8484, 5599};
//
//    private static final String cfgFilePath = "src/test/resources/client.properties";
//
//    @BeforeClass
//    public void init() throws IOException, JoranException {
//        ConfigUtil.init(cfgFilePath);
//        initExpectMap();
//    }
//
//    private static final String MODEL_TOKEN = "111-FederatedGB-210119152702";
//    private static final String ALGORITHM = "FederatedGB";
//
////    @Test(invocationCount = 1, threadPoolSize = 3)
//    public void inference() throws IOException {
//        /*复制一个数组，因为randomUids方法会修改数组结构*/
//        int[] uids = Arrays.copyOf(allUids, allUids.length);
//        int count = random.nextInt(uids.length) + 1;//每次推理的uid的个数
//        logger.info("推理uid个数={}", count);
//        String[] randomUids = randomUids(uids, count);
//
//        /*构造请求参数*/
//        AlgorithmType algorithm = AlgorithmType.valueOf(ALGORITHM);
//        String inferenceId = TokenUtil.getInferenceId(MODEL_TOKEN);
//        Map<String, Object> context = new HashMap<>(8);
//        context.put(MODEL_TOKEN_KEY, MODEL_TOKEN);
//        context.put(ALGORITHM_KEY, algorithm);
//        context.put(INFERENCE_ID_KEY, inferenceId);
//        Map<String, Object> dataMap = new HashMap<>(8);
//        dataMap.put(MODEL_TOKEN_KEY, MODEL_TOKEN);
//        dataMap.put(UID_KEY, randomUids);
//        InferenceInit init = new InferenceInit(MODEL_TOKEN, randomUids);
//        /* phase=-255的请求 */
//        String predict = toInference(context, init, "-255");
//        System.out.println("predict: " + predict);
//        BoostEvalQueryReqBody boostEvalQueryReqBody = new BoostEvalQueryReqBody();
//        boostEvalQueryReqBody.parseJson(EXPECT_255);
//        String str = SerializeUtil.serializeToString(boostEvalQueryReqBody);
//        System.out.println("str:"+str);
//        Assert.assertTrue(str.equalsIgnoreCase(predict));
//
//        /*phase=-1的请求 请求参数与-255相同*/
//        String predict1 = toInference(context, init, "-1");
//        logger.info("phase=-1，返回结果：{}", predict1);
//        BoostN1Res boostN1Res = new BoostN1Res();
//        boostN1Res.parseJson(EXPECT_1);
//        String boostN1ResStr = SerializeUtil.serializeToString(boostN1Res);
//
//        Assert.assertTrue(boostN1ResStr.equalsIgnoreCase(predict1));
//
//        /*phase=-2的请求*/
//        Map<String, Object> requestMap = new HashMap<>(8);
//        List<Bodie> bodies = new ArrayList<>();
//        List<BodieResult> resultBodies = new ArrayList<>();
//
//        List<String[]> boostN2ReqBodyList = new ArrayList<>();
//        List<String[]> boostN2ResBodies = new ArrayList<>();
//        for (int i = 0; i < randomUids.length; i++) {
//            /* 构造phase=-2请求参数*/
//            String[] bodie = new String[]{i + "", 0 + "", 1 + ""};
////            Bodie bodie1 = new Bodie(i + "", 1, 4);
//            boostN2ReqBodyList.add(bodie);
////            bodies.add(bodie1);
//            /* 构造phase=-2预期的返回结果*/
//            String[] bodieResult = new String[]{i + "", 0 + "", expectMap.get(Integer.parseInt(randomUids[i]))};
////            BoostN2ResBody bodieResult1 = new BoostN2ResBody(i + "", 1, expectMap.get(i+2).getSplit());
////            BodieResult bodieResult1 = new BodieResult(i + "", 1, expectMap.get(randomUids[i]));
//            boostN2ResBodies.add(bodieResult);
////            boostN2ResBodies.add(bodieResult1);
//        }
//        System.out.println("boostN2ResBodies size : " + boostN2ResBodies.size());
//        BoostN2Req boostN2Req = new BoostN2Req(boostN2ReqBodyList);
//        requestMap.put(BODIES_KEY, bodies);
//        String predict2 = toInference(context, boostN2Req, "-2");
//        logger.info("phase2，返回结果：{}", predict2);
//        Message predictMessage = SerializeUtil.deserializeToObject(predict2);
//        BoostN2Res boostN2Res = new BoostN2Res(boostN2ResBodies);
//
//        Assert.assertEquals(SerializeUtil.serializeToString(boostN2Res),predict2);
//
//        Map map = JsonUtil.parseJson(predict2);
//        List list = (List) map.get(BODIES_KEY);
//        String expect2 = getExpect2Detail(resultBodies);
//        Map map1 = JsonUtil.parseJson(predict2);
//        List list1 = (List) map1.get(BODIES_KEY);
//        logger.info("预期结果：        {}", expect2);
//        Assert.assertTrue(list.size() == list1.size());
//
//    }
//
//    private static final String BODIES_KEY = "bodies";
//    private static final String CLIENT_KEY = "client";
//
//    /**
//     * @param resultBodies
//     * @className InferenceTest
//     * @description: 构造phase=-2的预期返回值
//     * @return: java.lang.String
//     * @author: geyan29
//     * @date: 2020/12/8 17:02
//     **/
//    private String getExpect2Detail(List<BodieResult> resultBodies) {
//        Map<String, Object> expectMap = new HashMap<>(8);
//        expectMap.put(CLIENT_KEY, null);
//        expectMap.put(BODIES_KEY, resultBodies);
//        String expect2 = JsonUtil.object2json(expectMap);
//        return expect2;
//    }
//
//    /**
//     * @className InferenceTest
//     * @description:初始化所有uid的返回结果
//     * @return: void
//     * @author: geyan29
//     * @date: 2020/12/8 16:52
//     **/
//    private static final String SPLIT_KEY = "split";
//
//    private void initExpectMap() {
//        String allResult = "{\"client\":null,\"bodies\":[{\"uid\":\"0\",\"treeIndex\":0,\"split\":\"left\"},{\"uid\":\"0\",\"treeIndex\":1,\"split\":\"left\"},{\"uid\":\"1\",\"treeIndex\":0,\"split\":\"right\"},{\"uid\":\"1\",\"treeIndex\":1,\"split\":\"right\"},{\"uid\":\"2\",\"treeIndex\":0,\"split\":\"right\"},{\"uid\":\"2\",\"treeIndex\":1,\"split\":\"right\"},{\"uid\":\"3\",\"treeIndex\":0,\"split\":\"right\"},{\"uid\":\"3\",\"treeIndex\":1,\"split\":\"right\"},{\"uid\":\"4\",\"treeIndex\":0,\"split\":\"left\"},{\"uid\":\"4\",\"treeIndex\":1,\"split\":\"left\"},{\"uid\":\"5\",\"treeIndex\":0,\"split\":\"right\"},{\"uid\":\"5\",\"treeIndex\":1,\"split\":\"right\"},{\"uid\":\"6\",\"treeIndex\":0,\"split\":\"left\"},{\"uid\":\"6\",\"treeIndex\":1,\"split\":\"left\"},{\"uid\":\"7\",\"treeIndex\":0,\"split\":\"left\"},{\"uid\":\"7\",\"treeIndex\":1,\"split\":\"left\"},{\"uid\":\"8\",\"treeIndex\":0,\"split\":\"right\"},{\"uid\":\"8\",\"treeIndex\":1,\"split\":\"right\"},{\"uid\":\"9\",\"treeIndex\":0,\"split\":\"left\"},{\"uid\":\"9\",\"treeIndex\":1,\"split\":\"left\"},{\"uid\":\"10\",\"treeIndex\":0,\"split\":\"right\"},{\"uid\":\"10\",\"treeIndex\":1,\"split\":\"right\"},{\"uid\":\"11\",\"treeIndex\":0,\"split\":\"right\"},{\"uid\":\"11\",\"treeIndex\":1,\"split\":\"right\"},{\"uid\":\"12\",\"treeIndex\":0,\"split\":\"right\"},{\"uid\":\"12\",\"treeIndex\":1,\"split\":\"right\"},{\"uid\":\"13\",\"treeIndex\":0,\"split\":\"left\"},{\"uid\":\"13\",\"treeIndex\":1,\"split\":\"left\"},{\"uid\":\"14\",\"treeIndex\":0,\"split\":\"right\"},{\"uid\":\"14\",\"treeIndex\":1,\"split\":\"right\"},{\"uid\":\"15\",\"treeIndex\":0,\"split\":\"right\"},{\"uid\":\"15\",\"treeIndex\":1,\"split\":\"right\"},{\"uid\":\"16\",\"treeIndex\":0,\"split\":\"right\"},{\"uid\":\"16\",\"treeIndex\":1,\"split\":\"right\"},{\"uid\":\"17\",\"treeIndex\":0,\"split\":\"right\"},{\"uid\":\"17\",\"treeIndex\":1,\"split\":\"right\"},{\"uid\":\"18\",\"treeIndex\":0,\"split\":\"left\"},{\"uid\":\"18\",\"treeIndex\":1,\"split\":\"left\"},{\"uid\":\"19\",\"treeIndex\":0,\"split\":\"right\"},{\"uid\":\"19\",\"treeIndex\":1,\"split\":\"right\"},{\"uid\":\"20\",\"treeIndex\":0,\"split\":\"right\"},{\"uid\":\"20\",\"treeIndex\":1,\"split\":\"right\"},{\"uid\":\"21\",\"treeIndex\":0,\"split\":\"right\"},{\"uid\":\"21\",\"treeIndex\":1,\"split\":\"right\"},{\"uid\":\"22\",\"treeIndex\":0,\"split\":\"left\"},{\"uid\":\"22\",\"treeIndex\":1,\"split\":\"left\"},{\"uid\":\"23\",\"treeIndex\":0,\"split\":\"left\"},{\"uid\":\"23\",\"treeIndex\":1,\"split\":\"left\"},{\"uid\":\"24\",\"treeIndex\":0,\"split\":\"left\"},{\"uid\":\"24\",\"treeIndex\":1,\"split\":\"left\"}]}";
//        Map<String, Object> resultMap = JsonUtil.parseJson(allResult);
////        BoostN2Res boostN2Res = new BoostN2Res();
////        boostN2Res.parseJson(allResult);
//////        expectMap  = boostN2Res.getBodies();
////        for (BoostN2ResBody bodieResult : list) {
////            expectMap.put(allUids[Integer.parseInt(bodieResult.getUid())], bodieResult.getTreeIndex(),bodieResult.getSplit());
////        }
//
//        List<LinkedHashMap> list = (List) resultMap.get(BODIES_KEY);
//        for (LinkedHashMap bodieResult : list) {
//            expectMap.put(allUids[Integer.parseInt((String) bodieResult.get(UID_KEY))], (String) bodieResult.get(SPLIT_KEY));
//        }
//    }
//
//    /**
//     * @param context
//     * @param data
//     * @param phase
//     * @className InferenceTest
//     * @description:调用推理函数
//     * @return: java.lang.String
//     * @author: geyan29
//     * @date: 2020/12/8 16:52
//     **/
//    private String toInference(Map<String, Object> context, Message data, String phase) throws IOException {
//        logger.info("data={}", SerializeUtil.serializeToString(data));
//        context.put(DATA_KEY, HttpUtil.compress(SerializeUtil.serializeToString(data)));
//        context.put(PHASE_KEY, phase);
//        InferenceRequest subRequest = new InferenceRequest(JsonUtil.object2json(context));
//        InferenceService inferenceService = new InferenceService();
//        String predict = inferenceService.inference(subRequest);
//        return predict;
//    }
//
//
//    /**
//     * @param allUids
//     * @param n
//     * @className InferenceTest
//     * @description: 从一个数组中，随机取出不重复的n个值
//     * @return: int[]
//     * @author: geyan29
//     * @date: 2020/11/30 15:00
//     **/
//    private static String[] randomUids(int[] allUids, int n) {
//        String result[] = new String[n];
//        int total = allUids.length;
//        for (int i = 0; i < n; i++) {
//            int temp = random.nextInt(total);
//            result[i] = String.valueOf(allUids[temp]);
//            allUids[temp] = allUids[total - 1];
//            total--;
//        }
//        return result;
//    }
//
//    /**
//     * 创建内部类 用于构建返回值
//     **/
//    public static class Bodie {
//        private String uid;
//        private int treeIndex;
//        private int recordId;
//
//        public Bodie(String uid, int treeIndex, int recordId) {
//            this.uid = uid;
//            this.treeIndex = treeIndex;
//            this.recordId = recordId;
//        }
//
//        public String getUid() {
//            return uid;
//        }
//
//        public void setUid(String uid) {
//            this.uid = uid;
//        }
//
//        public int getTreeIndex() {
//            return treeIndex;
//        }
//
//        public void setTreeIndex(int treeIndex) {
//            this.treeIndex = treeIndex;
//        }
//
//        public int getRecordId() {
//            return recordId;
//        }
//
//        public void setRecordId(int recordId) {
//            this.recordId = recordId;
//        }
//    }
//
//    public static class BodieResult {
//        private String uid;
//        private int treeIndex;
//        private String split;
//
//        public BodieResult(String uid, int treeIndex, String split) {
//            this.uid = uid;
//            this.treeIndex = treeIndex;
//            this.split = split;
//        }
//
//        public String getUid() {
//            return uid;
//        }
//
//        public void setUid(String uid) {
//            this.uid = uid;
//        }
//
//        public int getTreeIndex() {
//            return treeIndex;
//        }
//
//        public void setTreeIndex(int treeIndex) {
//            this.treeIndex = treeIndex;
//        }
//
//        public String getSplit() {
//            return split;
//        }
//
//        public void setSplit(String split) {
//            this.split = split;
//        }
//    }
//
//}
