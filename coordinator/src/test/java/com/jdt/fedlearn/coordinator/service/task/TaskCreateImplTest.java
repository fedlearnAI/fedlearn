package com.jdt.fedlearn.coordinator.service.task;

import com.jdt.fedlearn.coordinator.dao.db.PartnerMapper;
import com.jdt.fedlearn.coordinator.dao.db.TaskMapper;
import com.jdt.fedlearn.coordinator.entity.table.PartnerProperty;
import com.jdt.fedlearn.coordinator.entity.task.CreateFeatures;
import com.jdt.fedlearn.coordinator.entity.task.CreateQuery;
import com.jdt.fedlearn.coordinator.entity.task.SingleCreateFeature;
import com.jdt.fedlearn.coordinator.entity.task.SingleJoinFeature;
import mockit.Mock;
import mockit.MockUp;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskCreateImplTest {
    private static final int TaskID = 0;

    @BeforeClass
    public void init() {
        mockInsertTask();
        mockInsertPartner();
        mockInsertFeatures();
    }


    @Test
    public void testGenerateIdAndInsert() {
        Map<String, String> clientInfo = new HashMap<>();
        clientInfo.put("ip", "1.0.0.1");
        clientInfo.put("protocol", "http");
        clientInfo.put("port", "8006");
        List<SingleCreateFeature> features = new ArrayList<>();
        CreateFeatures createFeatures = new CreateFeatures(features);
        CreateQuery query = new CreateQuery("lijingxi", clientInfo, createFeatures, "");
        TaskCreateImpl taskCreateImpl = new TaskCreateImpl();
        int res = taskCreateImpl.generateIdAndInsert(query);
        Assert.assertEquals(res, TaskID);

    }

    private static void mockInsertTask() {
        new MockUp<TaskMapper>() {
            @Mock
            public int insertTask(CreateQuery createQuery){
                System.out.println("插入数据库");
                return TaskID;
            }
        };
    }

    private static void mockInsertPartner() {
        new MockUp<PartnerMapper>() {
            @Mock
            public void insertPartner(PartnerProperty cp) {
                System.out.println("将PartnerProperty信息插入数据库");
            }
        };
    }

    private static void mockInsertFeatures() {
        new MockUp<TaskCommon>() {
            @Mock
            public void insertFeatures(int taskId, String username, List<SingleJoinFeature> features, String insertType) {
                System.out.println("插入特征");
            }
        };
    }
}