package com.jdt.fedlearn.coordinator.service.task;

import com.jdt.fedlearn.coordinator.dao.db.PartnerMapper;
import com.jdt.fedlearn.coordinator.dao.db.TaskMapper;
import com.jdt.fedlearn.coordinator.entity.table.PartnerProperty;
import com.jdt.fedlearn.coordinator.entity.task.CreateQuery;
import com.jdt.fedlearn.coordinator.entity.task.JoinFeatures;
import com.jdt.fedlearn.coordinator.entity.task.SingleJoinFeature;
import com.jdt.fedlearn.coordinator.entity.task.JoinQuery;
import mockit.Mock;
import mockit.MockUp;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskJoinImplTest {
    private static final int TaskID = 0;

    @BeforeClass
    public void init() {
        mocknsertTask();
        mockInsertPartner();
        mockInsertFeatures();
        mockSelectTaskPartner();
        mockUpdateTaskPartner();
    }


    @Test
    public void testPartnerCompose() {
        String current = "[a,b]";
        String currentNull = null;
        String newUser = "c";
        TaskJoinImpl taskJoinImpl = new TaskJoinImpl();
        String s1 = taskJoinImpl.partnerCompose(current, newUser);
        String s2 = taskJoinImpl.partnerCompose(currentNull, newUser);
        Assert.assertTrue(s1.equals("[a,b,c]"));
        Assert.assertTrue(s2.equals("[c]"));

    }

    @Test
    public void testTaskJoin() {
        Map<String, String> clientInfo = new HashMap<>();
        clientInfo.put("ip", "1.0.0.1");
        clientInfo.put("protocol", "http");
        clientInfo.put("port", "8006");

        TaskJoinImpl taskJoinImpl = new TaskJoinImpl();
        List<SingleJoinFeature> features = new ArrayList<>();
        JoinFeatures joinFeatures = new JoinFeatures(features);
        JoinQuery joinQuery = new JoinQuery(TaskID, "lijingxi", "dataset", clientInfo, joinFeatures, "taskPwd", "merCode");
        taskJoinImpl.taskJoin(joinQuery);

    }


    private static void mocknsertTask() {
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
                System.out.println("更新特征");
            }
        };
    }

    private static void mockSelectTaskPartner() {
        new MockUp<TaskMapper>() {
            @Mock
            public String selectTaskPartner(int taskId) {
                return "[user1,user2]";
            }
        };
    }

    private static void mockUpdateTaskPartner() {
        new MockUp<TaskMapper>() {
            @Mock
            public void updateTaskPartner(int taskId, String partners) {
                System.out.println("更新数据库");
            }
        };
    }
}