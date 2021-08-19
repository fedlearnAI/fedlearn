package com.jdt.fedlearn.coordinator.service.validate;

import com.jdt.fedlearn.coordinator.allocation.ResourceManager;
import com.jdt.fedlearn.coordinator.entity.table.PartnerProperty;
import com.jdt.fedlearn.coordinator.entity.table.TrainInfo;
import com.jdt.fedlearn.common.entity.SingleParameter;
import com.jdt.fedlearn.coordinator.entity.validate.ValidateRequest;
import com.jdt.fedlearn.coordinator.network.SendAndRecv;
import com.jdt.fedlearn.coordinator.util.ConfigUtil;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.base.Int2dArray;
import com.jdt.fedlearn.core.entity.boost.BoostN1Res;
import com.jdt.fedlearn.core.entity.common.CommonResponse;
import com.jdt.fedlearn.core.entity.common.InferenceInitRes;
import com.jdt.fedlearn.core.entity.psi.MatchInitRes;
import com.jdt.fedlearn.core.entity.serialize.JavaSerializer;
import com.jdt.fedlearn.core.entity.serialize.Serializer;
import com.jdt.fedlearn.core.model.common.tree.Tree;
import com.jdt.fedlearn.core.model.common.tree.TreeNode;
import com.jdt.fedlearn.core.type.AlgorithmType;
import mockit.Mock;
import mockit.MockUp;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ValidateServiceTest {
    String username = "lijingxi";
    String taskId = "1";
    String matchAlgorithm = "VERTICAL_MD5";
    private static PartnerProperty C1;
    private static PartnerProperty C2;
    private static PartnerProperty C3;
    Serializer serializer = new JavaSerializer();

    public static final String PERCENT = "percent";
    public static final String PREDICT_INFO = "predictInfo";
    public static final String END_TIME = "endTime";
    public static final String INFERENCE_SUCCESS = "验证完成";
    public static final String ONE_HUNDRED = "100";
    public static final String LABEL = "label";
    public static final String MODEL = "1-FederatedGB-10000";
    private static TrainInfo trainInfo;
    private static final String PRE = "inference_";
    public static final String INFERENCE_ID = "inferenceId";

    private static List<PartnerProperty> partnerPropertyList = null;
    private static Map<String, String[]> uidsMap = new ConcurrentHashMap<>(32);


    @BeforeClass
    public void setUp() {
        // 参与推理的客户端
        C1 = new PartnerProperty("", "http", "127.0.0.1", 80, 1, "train0.csv");
        C2 = new PartnerProperty("", "http", "127.0.0.1", 81, 2, "train1.csv");
        C3 = new PartnerProperty("", "http", "127.0.0.1", 82, 3, "train2.csv");
        // mock get client from database
//        this.partnerPropertyList = mockGetClientInfoFromDb(C1, C2, C3);
        // mock send
        mockPostClientInfo();
        // mock config
        mockCongfigInit();
        this.trainInfo = GetTrainInfo();

    }

    @Test
    public void testBatchValidate() {
    }

    @Test
    public void testCommonValidate() {
        // CACHE中储存，不用调取数据库
        //    public String commonValidate(ValidateRequest query, Map<String, Object> percentMap)
        Date endTime = new Date();
        Map<String, Object> percentMap = new HashMap<>();
        percentMap.put(PERCENT, ONE_HUNDRED);
        percentMap.put(PREDICT_INFO, INFERENCE_SUCCESS);
        percentMap.put(END_TIME, endTime.getTime());

//        ResourceManager.CACHE.putValue(INFERENCE_ID, percentMap);
        // test ValidateRequest实体
        ValidateRequest validateRequest = new ValidateRequest();
        validateRequest.setLabelName(LABEL);
        validateRequest.setModel(MODEL);
        validateRequest.setMetricType(new String[]{"acc"});
        System.out.println(validateRequest.getLabelName());
        System.out.println(validateRequest.getMetricType());
        System.out.println(validateRequest.getModel());
        ResourceManager.CACHE.putValue(PRE+MODEL, trainInfo);
        String taskId = trainInfo.getModelToken().split("-")[0];
        final String taskIdKey = PRE + taskId;
        ResourceManager.CACHE.putValue(taskIdKey, partnerPropertyList);




    }

    /**
     * PREPARE FOR TESTS ABOVE
     */
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

//
//    private static List<PartnerProperty> mockGetClientInfoFromDb(PartnerProperty c1, PartnerProperty c2, PartnerProperty c3) {
//        List<PartnerProperty> clientInfos = new ArrayList<>();
//        clientInfos.add(c1);
//        clientInfos.add(c2);
//        clientInfos.add(c3);
//        // 将PartnerMapper传入MockUp类
//        new MockUp<PartnerMapper>() {
//            @Mock
//            public List<PartnerProperty> selectPartnerList(String taskId, String username) {
//                return clientInfos;
//            }
//        };
//        return clientInfos;
//
//    }

    // mock doValidate
//    private void MockDoValidate() {
//        new MockUp<ValidateService>() {
//            @Mock
//            public Map<String, Double> doValidate(String[] existUidHasFiltered, TrainInfo model, AlgorithmType supportedAlgorithm, List<ClientInfo> clientList, Map<String, Object> percentMap, String inferenceId, Control algorithm, String labelName) {
//                Map<String, Double> map = new HashMap<>();
//                String[] uids = new String[]{"1B","59255", "2A", "3A","4A"};
//                for (int i = 0; i < uids.length; i++) {
//                    map.put()
//                }
//
//            }
//        };
//    }
    //mock send方法
    private void mockPostClientInfo() {
        new MockUp<SendAndRecv>() {
            @Mock
            public String send(ClientInfo client, String matchToken, String dataset, int phase, String matchAlgorithm, Message body) {
                // 模拟三个客户端，每个客户端对齐的id相同
                if (phase == 0) {
                    Message m1 = new MatchInitRes(C1.toClientInfo(), new String[]{"1B", "2A", "3A", "4A", "5C", "6C", "7C", "8B", "9B", "10B"});
                    return serializer.serialize(m1);
                } else {
                    return "error";
                }
            }

//            @Mock
//            public List<CommonResponse> broadcastValidate(List<CommonRequest> initRequests, String modelToken,  AlgorithmType algorithm, String inferenceId, String labelName) {
//                int phase = initRequests.get(0).getPhase();
//                if (phase == -255) {
//                    List<CommonResponse> fullRet255 = buildResponse(uidsMap.get(modelToken), -255);
//                    return fullRet255;
//                } else if (phase == -1) {
//                    List<CommonResponse> fullRet1 = buildResponse(uidsMap.get(modelToken), -1);
//                    return fullRet1;
//                } else if (phase == -2) {
//                    List<CommonResponse> fullRet2 = buildResponse(uidsMap.get(modelToken), -2);
//                    return fullRet2;
//                }
//                return null;
//            }
//
//            @Mock
//            public String sendValidate(ClientInfo client, String modelToken, int phase, AlgorithmType algorithm, Message data, String inferenceId, String labelName) throws IOException {
//                Serializer serializer = new JavaSerializer();
//                if (phase == -1) {
//                    List<CommonResponse> fullRet1 = buildResponse(uidsMap.get(modelToken), -1);
//                    return serializer.serialize(fullRet1.get(0).getBody());
//                } else if (phase == -2) {
//                    List<CommonResponse> fullRet2 = buildResponse(uidsMap.get(modelToken), -2);
//                    return serializer.serialize(fullRet2.get(0).getBody());
//                }
//                return null;
//            }
//
//            @Mock
//            public String send(ClientInfo Client, String path, String httpType, Map<String, Object> context) {
//                if (context.get("data")!=null) {
//                    // pushResultToClient
//                    return "{\"status\":null,\"code\":0,\"path\":\"/root/path/\"}";
//                } else {
//                    // getUidList
//                    InferenceFetchDTO inferenceFetchDTO = new InferenceFetchDTO();
//                    inferenceFetchDTO.setUid(new String[]{"1B","59255", "2A", "3A","4A"});
//                    inferenceFetchDTO.setModel("1-FederatedGB-4522552445");
//                    String s = JsonUtil.object2json(inferenceFetchDTO);
//                    return s;
//                }
//            }
        };
    }

    // TrainInfo
    private static TrainInfo GetTrainInfo() {
        TrainInfo model = new TrainInfo();
        model.setModelToken(MODEL);
        model.setAlgorithmType(AlgorithmType.FederatedGB);
        List<SingleParameter> singleParameterList = new ArrayList<>();
        singleParameterList.add(new SingleParameter("numBoostRound", "1"));
        singleParameterList.add(new SingleParameter("firstRoundPred", "AVG"));
        model.setHyperParameter(singleParameterList);
        return model;
    }

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
            TreeNode root1 = new TreeNode(1, 4, new ClientInfo(null, 0, "null", ""), 1, 1.0);
            TreeNode root2 = new TreeNode(1, 4, new ClientInfo(null, 0, "null", ""), 2, 1.0);
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
            BoostN1Res bn1r1 = new BoostN1Res(trees, 0.0, new ArrayList<Double>());
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
}