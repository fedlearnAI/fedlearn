package com.jdt.fedlearn.coordinator.service.train;

import com.jdt.fedlearn.coordinator.dao.db.TaskMapper;
import com.jdt.fedlearn.coordinator.dao.db.TrainMapper;
import com.jdt.fedlearn.coordinator.entity.table.TaskAnswer;
import com.jdt.fedlearn.coordinator.entity.train.*;
import com.jdt.fedlearn.coordinator.type.RunningType;
import com.jdt.fedlearn.core.dispatch.FederatedGB;
import com.jdt.fedlearn.core.parameter.FgbParameter;
import mockit.Mock;
import mockit.MockUp;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TrainListServiceImplTest {

    @BeforeClass
    public void init() {
        // a用户没有自己创建的任务，有两个加入的任务
        mockSelectNotOwnTask();
        mockSelectCreatedTask();
        mockGetModelsByTaskId();
    }

    @Test
    public void testQueryTrainList() {
        Map<String, TrainContext> trainContextMap = new ConcurrentHashMap<>();
        FederatedGB federatedGB = new FederatedGB(new FgbParameter());
        StartValues startValues = new StartValues();
        String trainStartTime = "";
        List<SingleParameter> parameterFieldList = new ArrayList<>();
        List<SingleParameter> crosspParameterFieldList = new ArrayList<>();
        TrainContext trainContext = new TrainContext(startValues, RunningType.RUNNING, trainStartTime,
                parameterFieldList, crosspParameterFieldList);
        trainContext.setPercent(10);
        trainContextMap.put("1-FederatedGB", trainContext);
        TrainCommonServiceImpl.trainContextMap = trainContextMap;
        TrainListReq query = new TrainListReq("a", "1");

        TrainListServiceImpl trainListServiceImpl = new TrainListServiceImpl();
        List<TrainListRes> trainListRes = trainListServiceImpl.queryTrainList(query);
        Assert.assertEquals(trainListRes.size(), 1);
        Assert.assertEquals(trainListRes.get(0).getModelToken(),"1-FederatedGB");

    }

    @Test
    public void testQueryMyTask() {
        TrainListServiceImpl trainListServiceImpl = new TrainListServiceImpl();
        List<TaskAnswer> list = trainListServiceImpl.queryMyTask("a");
        Assert.assertEquals(list.size(), 2);

    }


    private static void mockSelectNotOwnTask() {
        new MockUp<TaskMapper>() {
            @Mock
            public List<TaskAnswer> selectNotOwnTask(String username) {
                // 两个公开的任务
                TaskAnswer taskAnswer1 = new TaskAnswer(0, "taskName", "c", "[a,b,c]", "hasPwd", "merCode", "1", "[merCode]", "inferenceFlag");
                TaskAnswer taskAnswer2 = new TaskAnswer(1, "taskName", "c", "[a,b,c]", "hasPwd", "merCode", "1", "[merCode]", "inferenceFlag");
                List<TaskAnswer> list = new ArrayList<>();
                list.add(taskAnswer1);
                list.add(taskAnswer2);
                return list;
            }
        };
    }

    private static void mockSelectCreatedTask() {
        new MockUp<TaskMapper>() {
            @Mock
            public List<TaskAnswer> selectCreatedTask(String username) {
                // 两个公开的任务
                List<TaskAnswer> list = new ArrayList<>();
                return list;
            }
        };
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