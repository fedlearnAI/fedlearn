package com.jdt.fedlearn.coordinator.service.train;

import com.jdt.fedlearn.common.util.JsonUtil;
import com.jdt.fedlearn.coordinator.dao.db.TrainMapper;
import com.jdt.fedlearn.coordinator.entity.table.TrainInfo;
import com.jdt.fedlearn.coordinator.entity.train.SingleParameter;
import com.jdt.fedlearn.coordinator.entity.train.TrainContext;
import com.jdt.fedlearn.coordinator.entity.train.TrainParameterQuery;
import com.jdt.fedlearn.coordinator.entity.train.TrainParameterRes;
import com.jdt.fedlearn.coordinator.type.RunningType;
import com.jdt.fedlearn.core.dispatch.FederatedGB;
import com.jdt.fedlearn.core.entity.common.MetricValue;
import com.jdt.fedlearn.core.parameter.FgbParameter;
import com.jdt.fedlearn.core.type.AlgorithmType;
import com.jdt.fedlearn.core.type.MetricType;
import com.jdt.fedlearn.core.type.data.Pair;
import mockit.Mock;
import mockit.MockUp;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TrainParameterServiceImplTest {

    @BeforeClass
    public void init() {
        mockInsertTrainInfo();
    }

    @Test
    public void TestQueryTrainParam() {
        // 三种不同的input
        TrainParameterQuery trainParameterQuery1 = new TrainParameterQuery("1-FederatedGB-100000", "1");
        String s1 = JsonUtil.object2json(trainParameterQuery1);
        TrainParameterQuery trainParameterQuery2 = new TrainParameterQuery("1-FederatedGB-100000", "2");
        String s2 = JsonUtil.object2json(trainParameterQuery2);
        TrainParameterQuery trainParameterQuery3 = new TrainParameterQuery("1-FederatedGB-100000", "3");
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
        Assert.assertEquals(((TrainParameterRes)service1.get("data")).getAlgorithmParams().size(), 3);
        // TODO check crosspParameter是否还用
        Assert.assertEquals(((TrainParameterRes)service1.get("data")).getCrosspParams().size(), 1);
        // test TrainParameterRes
         TrainParameterRes trainParameterRes = ((TrainParameterRes)service1.get("data"));
        System.out.println(trainParameterRes.getAlgorithmParams());
        System.out.println(trainParameterRes.getCrosspParams());
        System.out.println(trainParameterRes.getTrainInfo());
        System.out.println(trainParameterRes.getModel());
        System.out.println(trainParameterRes.getTrainEndTime());
        System.out.println(trainParameterRes.getModelToken());
        System.out.println(trainParameterRes.getPercent());
        System.out.println(trainParameterRes.getRunningStatus());
        System.out.println(trainParameterRes.getTaskId());
        System.out.println(trainParameterRes.getTaskName());
        System.out.println(trainParameterRes.getTrainStartTime());

        trainParameterRes.setAlgorithmParams(null);
        trainParameterRes.setCrosspParams(null);
        trainParameterRes.setTrainInfo(null);
        trainParameterRes.setModel("");
        trainParameterRes.setTrainEndTime("");
        trainParameterRes.setModelToken("");
        trainParameterRes.setPercent(0);
        trainParameterRes.setRunningStatus(null);
        trainParameterRes.setTaskId("");
        trainParameterRes.setTaskName("");
        trainParameterRes.setTrainStartTime("");

        // 内存里查不到，需要查询数据库


    }

    private static void mockInsertTrainInfo() {
        new MockUp<TrainMapper>() {
            @Mock
            public TrainInfo getTrainInfoByToken(String token) {
                String modelToken = "model1";
                TrainInfo model = new TrainInfo();
                model.setModelToken(modelToken);
                model.setAlgorithmType(AlgorithmType.FederatedGB);
                return model;
            }
        };
    }

}