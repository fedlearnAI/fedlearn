package com.jdt.fedlearn.coordinator.service.system;

import com.jdt.fedlearn.coordinator.dao.db.PartnerMapper;
import com.jdt.fedlearn.coordinator.dao.db.TrainMapper;
import com.jdt.fedlearn.coordinator.entity.system.DeleteModelReq;
import com.jdt.fedlearn.coordinator.entity.table.PartnerProperty;
import com.jdt.fedlearn.coordinator.network.SendAndRecv;
import com.jdt.fedlearn.core.entity.ClientInfo;
import mockit.Mock;
import mockit.MockUp;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.testng.Assert.*;

public class ModelDeleteServiceImplTest {
    private static PartnerProperty C1;
    private static PartnerProperty C2;
    private static PartnerProperty C3;

    private static final Random random = new Random();

    @BeforeClass
    public void init() {
        initClientInfo();
        mockGetClientInfoFromDb(C1, C2, C3);
        mockDeleteModel();
    }

    private void initClientInfo() {
        C1 = new PartnerProperty("", "http", "127.0.0.1", 80, 1, "train0.csv");
        C2 = new PartnerProperty("", "http", "127.0.0.1", 81, 2, "train1.csv");
        C3 = new PartnerProperty("", "http", "127.0.0.1", 82, 3, "train2.csv");

    }
    @Test
    public void testDeleteModel() {
        DeleteModelReq deleteModelReq = new DeleteModelReq("1-FederatedGB-5522228", "lijingxi");
        mockSend(true);
        ModelDeleteServiceImpl modelDeleteService = new ModelDeleteServiceImpl();
        boolean b = modelDeleteService.deleteModel(deleteModelReq);
        mockSend(false);
        boolean b2 = modelDeleteService.deleteModel(deleteModelReq);
        Assert.assertEquals(b, true);
        Assert.assertEquals(b2, false);

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
        };
    }

    private static void mockDeleteModel() {
        new MockUp<TrainMapper>() {
            @Mock
            public void deleteModel(String modelId) {
                System.out.println("从数据库中删除模型");
            }
        };
    }

    private static void mockSend(boolean ifZero) {
        new MockUp<SendAndRecv>() {
            @Mock
            public String send(ClientInfo Client, String path, String httpType, Map<String, Object> context) {
                if (ifZero) {
                    String content = "{\"code\":\"0\",\"status\":\"success\",\"data\":\"0\"}";
                    return content;
                } else {
                    String content = "{\"code\":\"-1\",\"status\":\"success\",\"data\":\"0\"}";
                    return content;
                }
            }
        };
    }
}