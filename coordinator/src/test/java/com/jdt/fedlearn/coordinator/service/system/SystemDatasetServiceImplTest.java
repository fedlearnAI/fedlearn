package com.jdt.fedlearn.coordinator.service.system;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jdt.fedlearn.coordinator.dao.db.TaskMapper;
import com.jdt.fedlearn.coordinator.entity.common.Response;
import com.jdt.fedlearn.coordinator.entity.system.FeatureMapDTO;
import com.jdt.fedlearn.coordinator.entity.system.FeatureReq;
import com.jdt.fedlearn.coordinator.entity.table.TaskAnswer;
import com.jdt.fedlearn.coordinator.exception.UnauthorizedException;
import com.jdt.fedlearn.coordinator.network.SendAndRecv;
import mockit.Mock;
import mockit.MockUp;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Map;

public class SystemDatasetServiceImplTest {

    @BeforeClass
    public void init() {
        mockSelectTaskById();
        mockSendPost();
    }

    @Test
    public void testQueryDataset() throws JsonProcessingException {
        FeatureReq featureReq1 = new FeatureReq("task_name", "clientUrl", "0", "w");
        FeatureReq featureReq2 = new FeatureReq("task_name", "clientUrl", "0", "hasPwd");

        SystemDatasetServiceImpl systemDatasetService = new SystemDatasetServiceImpl();
        try {
            FeatureMapDTO featureMapDto = systemDatasetService.queryDataset(featureReq1);
        } catch (UnauthorizedException e) {
            Assert.assertEquals(e.getMessage(), "任务密码错误,无法加入任务！");
        }
        FeatureMapDTO featureMapDto2 = systemDatasetService.queryDataset(featureReq2);
        Assert.assertEquals(featureMapDto2.getList(), null);
    }

    @Test
    public void testQueryDatasetWithCorrectPassword() throws JsonProcessingException {
        FeatureReq featureReq2 = new FeatureReq("task_name", "clientUrl", "0", "hasPwd");
        SystemDatasetServiceImpl systemDatasetService = new SystemDatasetServiceImpl();
        FeatureMapDTO featureMapDto2 = systemDatasetService.queryDataset(featureReq2);
        Assert.assertEquals(featureMapDto2.getList(), null);
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

    public void mockSendPost() {
        new MockUp<SendAndRecv>() {
            @Mock
            public Response sendPost(String url, Map map) {
                Response featureResp = new Response(0, "success", new FeatureMapDTO());
                return featureResp;
            }
        };
    }
}