package com.jdt.fedlearn.coordinator.service.task;

import com.jdt.fedlearn.common.util.JsonUtil;
import com.jdt.fedlearn.coordinator.dao.db.FeatureMapper;
import com.jdt.fedlearn.coordinator.dao.db.PartnerMapper;
import com.jdt.fedlearn.coordinator.dao.db.TaskMapper;
import com.jdt.fedlearn.coordinator.entity.table.FeatureAnswer;
import com.jdt.fedlearn.coordinator.entity.table.PartnerProperty;
import com.jdt.fedlearn.coordinator.entity.table.TaskAnswer;
import com.jdt.fedlearn.coordinator.entity.task.TaskDetailQuery;
import com.jdt.fedlearn.coordinator.entity.task.TaskDetailRes;
import mockit.Mock;
import mockit.MockUp;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TaskDetailImplTest {
    private static PartnerProperty C1;
    private static PartnerProperty C2;
    private static PartnerProperty C3;
    private static final Integer TaskID = 1;

    @BeforeClass
    public void setUp() {
        // 参与推理的客户端
        C1 = new PartnerProperty("", "http", "127.0.0.1", 80, 1, "train0.csv");
        C2 = new PartnerProperty("", "http", "127.0.0.1", 81, 2, "train1.csv");
        C3 = new PartnerProperty("", "http", "127.0.0.1", 82, 3, "train2.csv");
        // mock从数据库获取clientInfo
        mockGetClientInfoFromDb(C1, C2, C3);
        mockSelectTaskById();
        mockGetFeaturesFromDb(true);


    }

    @Test
    public void testService() {
        TaskDetailImpl taskDetail = new TaskDetailImpl();
        TaskDetailQuery taskDetailQuery = new TaskDetailQuery("1", "u1");
        String s = JsonUtil.object2json(taskDetailQuery);
        Map<String, Object> serviceRes = taskDetail.service(s);
        Assert.assertEquals(serviceRes.get("code"), 0);
        Assert.assertEquals(serviceRes.get("status"), "success");

    }

    @Test
    public void testQueryTaskDetails() {
        TaskDetailQuery taskDetailQuery = new TaskDetailQuery("1", "u1");
        String s = JsonUtil.object2json(taskDetailQuery);
        TaskDetailQuery taskDetailQuery1 = new TaskDetailQuery(s);
        Assert.assertEquals(taskDetailQuery1.getTaskId(), taskDetailQuery.getTaskId());
        Assert.assertEquals(taskDetailQuery1.getUsername(), taskDetailQuery.getUsername());

        TaskDetailImpl taskDetail = new TaskDetailImpl();
        TaskDetailRes taskDetailRes = taskDetail.queryTaskDetails(taskDetailQuery1);
        Assert.assertEquals(taskDetailRes.getTaskId(), 0);
        Assert.assertEquals(taskDetailRes.getTaskName(), "task_name");


    }

    public void mockSelectTaskById() {
        new MockUp<TaskMapper>() {
            @Mock
            public TaskAnswer selectTaskById(Integer taskId) {
                TaskAnswer taskAnswer = new TaskAnswer(0, "task_name", "task_owner", "[a,b]", "hasPwd", "merCode", "0", "0", "0");
                taskAnswer.setTaskPwd("hasPwd");
                taskAnswer.setTaskId(0);
                taskAnswer.setParticipants("[a,b,c]");
                taskAnswer.setInferenceFlag("0");
                System.out.println(taskAnswer.getInferenceFlag());
                taskAnswer.setVisibleMerCode("0");
                taskAnswer.setVisible("0");
                taskAnswer.setMerCode("0");
                System.out.println(taskAnswer.getMerCode());
                taskAnswer.setHasPwd("hasPwd");
                return taskAnswer;
            }
        };
    }

    private static void mockGetClientInfoFromDb(PartnerProperty c1, PartnerProperty c2, PartnerProperty c3) {
        List<PartnerProperty> clientInfos = new ArrayList<>();
        clientInfos.add(c1);
        clientInfos.add(c2);
        clientInfos.add(c3);
        // 将PartnerMapper传入MockUp类
        new MockUp<PartnerMapper>() {
            @Mock
            public List<PartnerProperty> selectPartnerList(String taskId, String username) {
                return clientInfos;
            }

            @Mock
            public PartnerProperty selectClientByToken(String modelToken, String username) {
                return c1;
            }
        };

    }

    private static void mockGetFeaturesFromDb(boolean y) {
        new MockUp<FeatureMapper>() {
            @Mock
            public List<FeatureAnswer> selectFeatureListByTaskId(Integer taskId) {
                List<FeatureAnswer> faList = new ArrayList<>();
                FeatureAnswer fa1 = new FeatureAnswer(TaskID, C1.getUsername(), "1", "type",  "feature_describe");
                FeatureAnswer fa2 = new FeatureAnswer(TaskID, C1.getUsername(), "2", "type",  "feature_describe");
                FeatureAnswer fa3 = new FeatureAnswer(TaskID, C2.getUsername(), "1", "type",  "feature_describe");
                FeatureAnswer fa4 = new FeatureAnswer(TaskID, C3.getUsername(), "y", "type",  "feature_describe");
                faList.add(fa1);
                faList.add(fa2);
                faList.add(fa3);
                if (!y) {
                    return faList;
                }
                faList.add(fa4);
                return faList;
            }
        };
    }
}