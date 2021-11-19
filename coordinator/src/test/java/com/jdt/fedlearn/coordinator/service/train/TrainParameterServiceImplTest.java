package com.jdt.fedlearn.coordinator.service.train;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdt.fedlearn.common.enums.RunningType;
import com.jdt.fedlearn.tools.serializer.JsonUtil;
import com.jdt.fedlearn.coordinator.dao.UniversalMapper;
import com.jdt.fedlearn.coordinator.dao.db.TrainMapper;
import com.jdt.fedlearn.coordinator.entity.table.TrainInfo;
import com.jdt.fedlearn.common.entity.SingleParameter;
import com.jdt.fedlearn.coordinator.entity.train.TrainContext;
import com.jdt.fedlearn.coordinator.entity.train.TrainParameterQuery;
import com.jdt.fedlearn.coordinator.entity.train.TrainParameterRes;
import com.jdt.fedlearn.coordinator.util.ConfigUtil;
import com.jdt.fedlearn.core.dispatch.FederatedGB;
import com.jdt.fedlearn.core.entity.common.MetricValue;
import com.jdt.fedlearn.core.parameter.FgbParameter;
import com.jdt.fedlearn.common.entity.core.type.AlgorithmType;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.data.Pair;
import mockit.Mock;
import mockit.MockUp;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TrainParameterServiceImplTest {

    @BeforeClass
    public void init() throws IOException {
        ConfigUtil.init("src/test/resources/coordinator.properties");
        mockInsertTrainInfo();
        mockisContainModel();
        mockGetStaticTrainInfo();
    }

    @Test
    public void TestQueryTrainParam() {
        // 三种不同的input
        TrainParameterQuery trainParameterQuery1 = new TrainParameterQuery("1-FederatedGB-100000");
        String s1 = JsonUtil.object2json(trainParameterQuery1);
        TrainParameterQuery trainParameterQuery2 = new TrainParameterQuery("1-FederatedGB-100000");
        String s2 = JsonUtil.object2json(trainParameterQuery2);
        TrainParameterQuery trainParameterQuery3 = new TrainParameterQuery("1-FederatedGB-100000");
        String s3 = JsonUtil.object2json(trainParameterQuery3);
        // 内存里可以查得到，不需要查询数据库
        Map trainContextMap = new ConcurrentHashMap<>();
        List<SingleParameter> parameterFieldList = new ArrayList<>();
        parameterFieldList.add(new SingleParameter("maxDepth", "5"));
        parameterFieldList.add(new SingleParameter("label", "1"));
        parameterFieldList.add(new SingleParameter("crossValidation", "1"));
//        List<SingleParameter> crosspParameterFieldList = new ArrayList<>();
        parameterFieldList.add(new SingleParameter("matchAlgorithm", "1"));


        Map<MetricType, List<Pair<Integer, Double>>> metrics = new HashMap<>();
        metrics.put(MetricType.ACC, new ArrayList<>());
        TrainContext trainContext = new TrainContext(RunningType.COMPLETE, "200000", 100, new MetricValue(metrics), parameterFieldList);
//        trainContext.setCrosspParameterFieldList(crosspParameterFieldList);
        trainContext.setDispatcher(new FederatedGB(new FgbParameter()));

        trainContextMap.put("1-FederatedGB-100000", trainContext);
        TrainCommonServiceImpl.trainContextMap = trainContextMap;
        TrainParameterServiceImpl trainParameterService = new TrainParameterServiceImpl();
        Map<String, Object> service1 = trainParameterService.service(s1);
        Assert.assertEquals(service1.get("code"), 0);
        Assert.assertEquals(service1.get("status"), "success");
        Assert.assertEquals(((TrainParameterRes) service1.get("data")).getAlgorithmParams().size(), 1);
        // TODO check crosspParameter是否还用
//        Assert.assertEquals(((TrainParameterRes)service1.get("data")).getCrosspParams().size(), 1);
        // test TrainParameterRes
        TrainParameterRes trainParameterRes = ((TrainParameterRes) service1.get("data"));
        System.out.println(trainParameterRes.getAlgorithmParams());
//        System.out.println(trainParameterRes.getCrosspParams());
//        System.out.println(trainParameterRes.getTrainInfo());
        System.out.println(trainParameterRes.getAlgorithmType());
        System.out.println(trainParameterRes.getEndTime());
        System.out.println(trainParameterRes.getTaskId());
//        System.out.println(trainParameterRes.getTaskName());
        System.out.println(trainParameterRes.getStartTime());

//        trainParameterRes.setAlgorithmParams(null);
////        trainParameterRes.setCrosspParams(null);
////        trainParameterRes.setTrainInfo(null);
//        trainParameterRes.setModel("");
//        trainParameterRes.setTrainEndTime("");
//        trainParameterRes.setTrainId("");
//        trainParameterRes.setPercent(0);
//        trainParameterRes.setRunningStatus(null);
//        trainParameterRes.setTaskId("");
//        trainParameterRes.setTrainStartTime("");

        // 内存里查不到，需要查询数据库


    }

    private static void mockInsertTrainInfo() {
        new MockUp<TrainMapper>() {
            @Mock
            public TrainInfo getTrainInfoByToken(String token) {
                String modelToken = "model1";
                TrainInfo model = new TrainInfo();
                model.setModelToken(modelToken);
                model.setHyperParameter(new ArrayList<>());
                model.setAlgorithmType(AlgorithmType.FederatedGB);
                return model;
            }
        };
    }

    private static void mockisContainModel() {
        new MockUp<TrainMapper>() {
            @Mock
            public boolean isContainModel(String token) {
                String modelToken = "model1";
                TrainInfo model = new TrainInfo();
                model.setModelToken(modelToken);
                model.setAlgorithmType(AlgorithmType.FederatedGB);
                return true;
            }
        };
    }

    private static void mockIsModelExist() {
        new MockUp<UniversalMapper>() {
            @Mock
            public boolean isModelExist(String token) {
                String modelToken = "model1";
                TrainInfo model = new TrainInfo();
                model.setModelToken(modelToken);
                model.setAlgorithmType(AlgorithmType.FederatedGB);
                return true;
            }
        };
    }

    private static void mockGetStaticTrainInfo() {
        new MockUp<UniversalMapper>() {
            @Mock
            public TrainInfo getStaticTrainInfo(String token) throws JsonProcessingException {
                ObjectMapper objectMapper = new ObjectMapper();
                String algorithm = "FederatedGB";
                String describe = "[{\"field\":\"numBoostRound\",\"value\":2}]";
                final List<SingleParameter> finshParameterFields = objectMapper.readValue(describe, new TypeReference<List<SingleParameter>>() {
                });
                //兼容sqlite
                long trainStartTime = 1635334388071l;
                long trainEndTime = 1635334598193l;
                AlgorithmType algorithmType = AlgorithmType.valueOf(algorithm);
                String matchId = "111-MD-2021000";
                TrainInfo trainInfo = new TrainInfo(token, matchId, algorithmType, finshParameterFields, trainStartTime, trainEndTime);
                return trainInfo;
            }
        };
    }
}