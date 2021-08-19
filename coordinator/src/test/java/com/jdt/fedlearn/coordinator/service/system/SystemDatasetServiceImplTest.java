package com.jdt.fedlearn.coordinator.service.system;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jdt.fedlearn.common.tool.internel.ResponseInternal;
import com.jdt.fedlearn.coordinator.entity.system.FeatureMapDTO;
import com.jdt.fedlearn.coordinator.entity.system.FeatureReq;

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
        mockSendPost();
    }

    @Test
    public void testQueryDataset() throws JsonProcessingException {
        FeatureReq featureReq1 = new FeatureReq( "clientUrl");
        FeatureReq featureReq2 = new FeatureReq("clientUrl");

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
        FeatureReq featureReq2 = new FeatureReq("clientUrl");
        SystemDatasetServiceImpl systemDatasetService = new SystemDatasetServiceImpl();
        FeatureMapDTO featureMapDto2 = systemDatasetService.queryDataset(featureReq2);
        Assert.assertEquals(featureMapDto2.getList(), null);
    }


    public void mockSendPost() {
        new MockUp<SendAndRecv>() {
            @Mock
            public ResponseInternal sendPost(String url, Map map) {
                ResponseInternal featureResp = new ResponseInternal(0, "success", new FeatureMapDTO());
                return featureResp;
            }
        };
    }
}