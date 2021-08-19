package com.jdt.fedlearn.coordinator.service.train;

import com.jdt.fedlearn.common.entity.SingleParameter;
import com.jdt.fedlearn.common.enums.RunningType;
import com.jdt.fedlearn.coordinator.dao.db.TrainMapper;
import com.jdt.fedlearn.coordinator.entity.common.CommonQuery;
import com.jdt.fedlearn.coordinator.entity.train.*;
import com.jdt.fedlearn.core.dispatch.FederatedGB;
import com.jdt.fedlearn.core.parameter.FgbParameter;
import mockit.Mock;
import mockit.MockUp;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TrainListServiceImplTest {

    @BeforeClass
    public void init() {
        // a用户没有自己创建的任务，有两个加入的任务
        mockGetModelsByTaskId();
    }

    @Test
    public void testQueryTrainList() {
        Map<String, TrainContext> trainContextMap = new ConcurrentHashMap<>();
        StartValues startValues = new StartValues();
        String trainStartTime = "";
        List<SingleParameter> parameterFieldList = new ArrayList<>();
        TrainContext trainContext = new TrainContext(startValues, RunningType.RUNNING, trainStartTime,
                parameterFieldList);
        trainContext.setPercent(10);
        trainContextMap.put("1-FederatedGB", trainContext);
        TrainCommonServiceImpl.trainContextMap = trainContextMap;
        CommonQuery query = new CommonQuery( Arrays.asList("1"),"COMPLETE");
        TrainListServiceImpl trainListServiceImpl = new TrainListServiceImpl();
        List<TrainListRes> trainListRes = trainListServiceImpl.queryTrainList(query);
        Assert.assertEquals(trainListRes.size(), 1);
        Assert.assertEquals(trainListRes.get(0).getModelToken(),"1-FederatedGB");

    }




    private static void mockGetModelsByTaskId() {
        new MockUp<TrainMapper>() {
            @Mock
            public List<String> getModelsByTaskId(Integer taskId) {
                return new ArrayList<>();
            }
        };
    }
}