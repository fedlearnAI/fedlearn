package com.jdt.fedlearn.coordinator.Inference;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.common.entity.project.PartnerInfoNew;
import com.jdt.fedlearn.common.util.JsonUtil;
import com.jdt.fedlearn.coordinator.dao.db.InferenceLogMapper;
import com.jdt.fedlearn.coordinator.entity.inference.InferenceFetchDTO;
import com.jdt.fedlearn.coordinator.entity.inference.RemotePredict;
import com.jdt.fedlearn.coordinator.entity.table.InferenceEntity;
import com.jdt.fedlearn.coordinator.entity.table.TrainInfo;
import com.jdt.fedlearn.common.entity.SingleParameter;
import com.jdt.fedlearn.coordinator.service.inference.InferenceRemoteServiceImpl;
import com.jdt.fedlearn.coordinator.service.inference.InferenceCommonService;
import com.jdt.fedlearn.coordinator.util.ConfigUtil;
import com.jdt.fedlearn.coordinator.network.SendAndRecv;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.base.Int2dArray;
import com.jdt.fedlearn.core.entity.boost.BoostN1Res;
import com.jdt.fedlearn.core.entity.common.CommonRequest;
import com.jdt.fedlearn.core.entity.common.CommonResponse;
import com.jdt.fedlearn.core.entity.common.InferenceInitRes;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.entity.feature.SingleFeature;
import com.jdt.fedlearn.core.entity.serialize.JavaSerializer;
import com.jdt.fedlearn.core.entity.serialize.Serializer;
import com.jdt.fedlearn.core.model.common.tree.Tree;
import com.jdt.fedlearn.core.model.common.tree.TreeNode;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.coordinator.dao.db.TrainMapper;
import com.jdt.fedlearn.coordinator.entity.table.PartnerProperty;
import com.jdt.fedlearn.coordinator.service.inference.InferenceBatchServiceImpl;
import mockit.Mock;
import mockit.MockUp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * InferenceTest
 * 推理是master端的单元测试, 使用testng+Jmock,支持并发测试
 * @author geyan29
 * 2020/12/1 17:18
 **/
public class InferenceTest {

    private static final Logger logger = LoggerFactory.getLogger(InferenceTest.class);

    private static final Random random = new Random();
    private static final String MODEL_KEY = "modelToken";
    private static final String UID_KEY = "uid";
    private static final String PRE_MODEL = "3-FederatedGB-";
    private static final String PATH = "path";

    private static PartnerProperty C1;
    private static PartnerProperty C2;
    private static PartnerProperty C3;
    private static Map<String, String[]> uidsMap = new ConcurrentHashMap<>(32);

    @BeforeClass
    public void setUp() {
        // 参与推理的客户端
        C1 = new PartnerProperty("", "http", "127.0.0.1", 80, 1, "train0.csv");
        C2 = new PartnerProperty("", "http", "127.0.0.1", 81, 2, "train1.csv");
        C3 = new PartnerProperty("", "http", "127.0.0.1", 82, 3, "train2.csv");
        // mockConfig
        mockCongfigInit();
        // mock从数据库获取token
        mockGetTokenFromDb();
        // mock从数据库获取clientInfo
        mockGetClientInfoFromDb(C1, C2, C3);
        // mock从数据库获取Features
        mockGetFeaturesFromDb(C1, C2, C3);
        // mock与客户端交互返回值
        mockPostClientInfo();
        // 远端推理获取推理id
        mockGetUidList();
        mockInsertInference();
    }


    private static final String CODE_KEY = "code";
    private static final String DATA_KEY = "data";
    private static final String PREDICT_KEY = "predict";

    // 这个多线程没有意义
    @Test(invocationCount = 3, threadPoolSize = 2)
    public void testBatch() throws Exception {
        String[] allUids = {"1B","59255", "2A", "3A","4A"};
        StringBuilder stringBuilder = new StringBuilder(PRE_MODEL);
        String model = stringBuilder.append(getSubUUID()).toString();
//        int count = random.nextInt(allUids.length) + 1;//每次推理的uid的个数
        int count = allUids.length;
        logger.info("推理uid个数={}", count);
//        String[] uids = randomUids(allUids, count, model);
        String[] uids = allUids;

        Map<String, Object> paramMap = new HashMap<>();
//        paramMap.put(USERNAME_KEY, USERNAME_VALUE);
        paramMap.put(MODEL_KEY, model);
        paramMap.put(UID_KEY, uids);
        List<PartnerInfoNew> clientList = new ArrayList<>();
        clientList.add(new PartnerInfoNew("http://127.0.0.1:80", "a"));
        clientList.add(new PartnerInfoNew("http://127.0.0.1:81", "b"));
        clientList.add(new PartnerInfoNew("http://127.0.0.1:82", "c"));
        paramMap.put("clientList", clientList);

        InferenceBatchServiceImpl inferenceBatchService = new InferenceBatchServiceImpl();
        Map<String, Object> serviceResult = inferenceBatchService.service(JsonUtil.object2json(paramMap));
        logger.info("推理结果:{}", serviceResult);
        int code = (int) serviceResult.get(CODE_KEY);
        Map<String, List> data = (Map) serviceResult.get(DATA_KEY);
        List predict = data.get(PREDICT_KEY);
        Assert.assertTrue(0 == code);
        Assert.assertEquals(uids.length+1, predict.size());
    }

    @Test
    public void testRemote() throws JsonProcessingException {
        String[] allUids = {"1B","59255", "2A", "3A","4A"};
//        int count = random.nextInt(allUids.length) + 1;//每次推理的uid的个数
        int count = allUids.length;
        logger.info("推理uid个数={}", count);
//        String[] uids = randomUids(allUids, count, model);
        String[] uids = allUids;

        Map<String, Object> paramMap = new HashMap<>();
//        paramMap.put(USERNAME_KEY, USERNAME_VALUE);
        paramMap.put(MODEL_KEY, "1-FederatedGB-4522552445");
        paramMap.put("path", "/root/path");

        List<PartnerInfoNew> clientList = new ArrayList<>();
        clientList.add(new PartnerInfoNew("http://127.0.0.1:80", "a"));
        clientList.add(new PartnerInfoNew("http://127.0.0.1:81", "b"));
        clientList.add(new PartnerInfoNew("http://127.0.0.1:82", "c"));
        paramMap.put("clientList", clientList);
        paramMap.put("userAddress", "http://127.0.0.1:80");

        String content = JsonUtil.object2json(paramMap);
        InferenceRemoteServiceImpl inferenceRemoteService = new InferenceRemoteServiceImpl();
        Map serviceResult = inferenceRemoteService.service(content);
        logger.info("推理结果:{}", serviceResult);
        System.out.println(serviceResult);
        int code = (int) serviceResult.get(CODE_KEY);
        Map<String, List> data = (Map) serviceResult.get(DATA_KEY);
        List predict = data.get(PREDICT_KEY);

        // TODO 当前待修改
        Assert.assertEquals(code, 0);
    }

//    @Test
//    public void readJson() throws JsonProcessingException {
//        PushRsultDTO pushRsultDTO = new PushRsultDTO("/root/path/");
//        String s = JsonUtil.object2json(pushRsultDTO);
//        System.out.println(s);
//        ObjectMapper mapper = new ObjectMapper();
//        PushRsultDTO resp = mapper.readValue(s, PushRsultDTO.class);
//        System.out.println(resp.getPath());
//
//        RemotePredict remotePredict = new RemotePredict("/root/path", "lijingxi", "1-FederatedGB");
//        String s1 = JsonUtil.object2json(remotePredict);
//        System.out.println(s1);
//
//    }


    private void mockCongfigInit() {
        new MockUp<ConfigUtil>() {
            @Mock
            public boolean getSplitTag() {
                return true;
            }

            @Mock
            public boolean getZipProperties() {
                return true;
            }

            @Mock
            public boolean getJdChainAvailable() {
                return false;
            }
        };
    }

    /**
     * @className InferenceTest
     * @description: mock master与client的交互过程
     * @return: void
     * @author: geyan29
     * @date: 2020/11/30 19:52
     **/
    private void mockPostClientInfo() {
        new MockUp<SendAndRecv>() {
            @Mock
            public List<CommonResponse> broadcastInference(List<CommonRequest> intiRequests, String modelToken, AlgorithmType algorithm, String inferenceId, List<PartnerInfoNew> partnerInfoNews) {
                int phase = intiRequests.get(0).getPhase();
                if (phase == -255) {
                    List<CommonResponse> fullRet255 = buildResponse(uidsMap.get(modelToken), -255);
                    return fullRet255;
                } else if (phase == -1) {
                    List<CommonResponse> fullRet1 = buildResponse(uidsMap.get(modelToken), -1);
                    return fullRet1;
                } else if (phase == -2) {
                    List<CommonResponse> fullRet2 = buildResponse(uidsMap.get(modelToken), -2);
                    return fullRet2;
                }
                return null;
            }

            @Mock
            public String sendInference(ClientInfo client, String modelToken, int phase, AlgorithmType algorithm, Message data, String inferenceId) throws IOException {
                Serializer serializer = new JavaSerializer();
                if (phase == -1) {
                    List<CommonResponse> fullRet1 = buildResponse(uidsMap.get(modelToken), -1);
                    return serializer.serialize(fullRet1.get(0).getBody());
                } else if (phase == -2) {
                    List<CommonResponse> fullRet2 = buildResponse(uidsMap.get(modelToken), -2);
                    return serializer.serialize(fullRet2.get(0).getBody());
                }
                return null;
            }

            @Mock
            public String send(ClientInfo Client, String path, String httpType, Map<String, Object> context) {
                if (context.get("data")!=null) {
                    // pushResultToClient
                    return "{\"status\":null,\"code\":0,\"path\":\"/root/path/\"}";
                } else {
                    // getUidList
                    InferenceFetchDTO inferenceFetchDTO = new InferenceFetchDTO();
                    inferenceFetchDTO.setUid(new String[]{"1B","59255", "2A", "3A","4A"});
                    inferenceFetchDTO.setModel("1-FederatedGB-4522552445");
                    String s = JsonUtil.object2json(inferenceFetchDTO);
                    return s;
                }
            }

        };
    }



    private static void mockGetUidList() {
        InferenceFetchDTO inferenceFetchDTO = new InferenceFetchDTO();
        inferenceFetchDTO.setUid(new String[]{"1B","59255", "2A", "3A","4A"});
        inferenceFetchDTO.setModel("1-FederatedGB-4522552445");
        new MockUp<InferenceCommonService>() {
            @Mock
            private InferenceFetchDTO getUidList(RemotePredict remotePredict, ObjectMapper mapper, ClientInfo clientInfo) throws JsonProcessingException {
                return inferenceFetchDTO;
            }
        };
    }

    private static void mockInsertInference() {
        new MockUp<InferenceLogMapper>() {
            @Mock
            public Boolean insertInference(InferenceEntity inferenceEntity) {
                return true;
            }
        };
    }

    /**
     * @param c1
     * @param c2
     * @className InferenceTest
     * @description: 通过client信息获取feature
     * @return: void
     * @author: geyan29
     * @date: 2020/11/27 20:48
     **/
    private static void mockGetFeaturesFromDb(PartnerProperty c1, PartnerProperty c2, PartnerProperty c3) {
        List<SingleFeature> featureList = new ArrayList<>();
        featureList.add(new SingleFeature("uid", "float"));
        featureList.add(new SingleFeature("Pregnancies", "float"));
        featureList.add(new SingleFeature("Glucose", "float"));
        featureList.add(new SingleFeature("Outcome", "float"));
        Features features = new Features(featureList, "Outcome");

        List<SingleFeature> featureList2 = new ArrayList<>();
        featureList2.add(new SingleFeature("uid", "float"));
        featureList2.add(new SingleFeature("SkinThickness", "float"));
        featureList2.add(new SingleFeature("Insulin", "float"));
        Features features2 = new Features(featureList2);

        List<SingleFeature> featureList3 = new ArrayList<>();
        featureList3.add(new SingleFeature("uid", "float"));
        featureList3.add(new SingleFeature("Insulin", "float"));
        featureList3.add(new SingleFeature("BMI", "float"));
        featureList3.add(new SingleFeature("DiabetesPedigreeFunction", "float"));
        featureList3.add(new SingleFeature("Age", "float"));
        Features features3 = new Features(featureList3);

//        new MockUp<FeatureMapper>() {
//            @Mock
//            public Features selectFeatureListByTaskIdAndCli(String taskId, PartnerProperty clientInfo) {
//                if (clientInfo.equals(c1)) {
//                    return features;
//                } else if (clientInfo.equals(c2)) {
//                    return features2;
//                } else {
//                    return features3;
//                }
//            }
//        };
    }

    /**
     * @param c1
     * @param c2
     * @className InferenceTest
     * @description:mock 从数据库获取client
     * @return: void
     * @author: geyan29
     * @date: 2020/11/27 20:48
     **/
    private static void mockGetClientInfoFromDb(PartnerProperty c1, PartnerProperty c2, PartnerProperty c3) {
        List<PartnerProperty> clientInfos = new ArrayList<>();
        clientInfos.add(c1);
        clientInfos.add(c2);
        clientInfos.add(c3);
        // 将PartnerMapper传入MockUp类
//        new MockUp<PartnerMapper>() {
//            @Mock
//            public List<PartnerProperty> selectPartnerList(String taskId, String username) {
//                return clientInfos;
//            }
//
//            @Mock
//            public PartnerProperty selectClientByToken(String modelToken, String username) {
//                return c1;
//            }
//        };

    }


    /**
     * @className InferenceTest
     * @description: mock从数据库获取token
     * @return: void
     * @author: geyan29
     * @date: 2020/11/27 20:49
     */
    private static void mockGetTokenFromDb() {
        new MockUp<TrainMapper>() {
            @Mock
            public TrainInfo getTrainInfoByToken(String token) {
                TrainInfo modelToken = new TrainInfo();
                modelToken.setModelToken(token);
                modelToken.setAlgorithmType(AlgorithmType.valueOf("FederatedGB"));
                List<SingleParameter> singleParameterList = new ArrayList<>();
                singleParameterList.add(new SingleParameter("numBoostRound", "1"));
                singleParameterList.add(new SingleParameter("firstRoundPred", "AVG"));
                modelToken.setHyperParameter(singleParameterList);
                return modelToken;
                // 需要包含正确的client information， taskId,AlgorithmType
            }
        };
    }


    /**
     * @param allUids
     * @param n
     * @className InferenceTest
     * @description: 从一个数组中，随机取出不重复的n个值
     * @return: String[]
     * @author: geyan29
     * @date: 2020/11/30 15:00
     **/
    private static String[] randomUids(String[] allUids, int n, String model) {
        String result[] = new String[n];
        int total = allUids.length;
        for (int i = 0; i < n; i++) {
            int temp = random.nextInt(total);
            result[i] = allUids[temp];
            allUids[temp] = allUids[total - 1];
            total--;
        }
        uidsMap.put(model, result);
        return result;
    }

    /**
     * @param uids
     * @param phase
     * @className InferenceTest
     * @description: 构建推理不同阶段，调用client的返回值
     * @return: java.util.List<com.jdt.fedlearn.core.entity.common.CommonResponse>
     * @author: geyan29
     * @date: 2020/11/30 15:51
     **/
    private static List<CommonResponse> buildResponse(String[] uids, int phase) {
        List<CommonResponse> fullRet = new ArrayList<>();
        if (phase == -255) {
            CommonResponse cr255One = new CommonResponse(C1.toClientInfo(), new InferenceInitRes(false, new int[]{1}));
            CommonResponse cr255Two = new CommonResponse(C2.toClientInfo(), new InferenceInitRes(false, new int[]{1}));
            CommonResponse cr255Three = new CommonResponse(C3.toClientInfo(), new InferenceInitRes(false, new int[]{1}));

            fullRet.add(cr255One);
            fullRet.add(cr255Two);
            fullRet.add(cr255Three);
        } else if (phase == -1) {
            ArrayList<Tree> trees = new ArrayList<>();
            TreeNode root1 = new TreeNode(1, 4, new ClientInfo("127.0.0.1", 80, "http", ""), 1, 1.0);
            TreeNode root2 = new TreeNode(1, 4, new ClientInfo("127.0.0.1", 81, "http", ""), 2, 1.0);
            // left
            TreeNode left1 = new TreeNode(2, -0.6666666666666666);
            TreeNode left2 = new TreeNode(2, -0.5590094712505657);

            // right
            TreeNode right1 = new TreeNode(4, 1.1111111111111112);
            TreeNode right2 = new TreeNode(4, 0.9418921388697138);
            root1.internalNodeSetterSecure(0.0, null, left1, right1, false);
            root2.internalNodeSetterSecure(0.0, null, left1, right1, false);
            trees.add(new Tree(root1));
            trees.add(new Tree(root2));
            BoostN1Res bn1r1 = new BoostN1Res(trees, 0.0, new ArrayList<>());
            CommonResponse cr = new CommonResponse(C1.toClientInfo(), bn1r1);
            CommonResponse cr2 = new CommonResponse(C2.toClientInfo(), null);
            CommonResponse cr3 = new CommonResponse(C3.toClientInfo(), null);
            fullRet.add(cr);
            fullRet.add(cr2);
            fullRet.add(cr3);
        } else if (phase == -2) {
            Int2dArray i2a = new Int2dArray(new int[][]{{0,0,2}, {1,0,1}, {2,0,2}, {3,0,1}, {0,1,2}, {1,1,1}, {2,1,2}, {3,1,1}});
            CommonResponse cr = new CommonResponse(C3.toClientInfo(), i2a);
            fullRet.add(cr);
        }
        return fullRet;
    }

    /**
     * @className InferenceTest
     * @description: 根据uuid动态生成modelToken
     * @return: java.lang.String
     * @author: geyan29
     * @date: 2020/11/30 20:59
     **/
    private static String getSubUUID() {
        UUID uuid = UUID.randomUUID();
        String timeStr = uuid.toString().replace("-", "");
        String subStr = timeStr.substring(timeStr.length() - 12);
        return subStr;
    }

}
