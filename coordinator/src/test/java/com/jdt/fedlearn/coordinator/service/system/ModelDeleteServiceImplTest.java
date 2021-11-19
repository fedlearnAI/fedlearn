package com.jdt.fedlearn.coordinator.service.system;

import com.jdt.fedlearn.coordinator.dao.db.TrainMapper;
import com.jdt.fedlearn.coordinator.entity.system.DeleteModelReq;
import com.jdt.fedlearn.coordinator.entity.table.PartnerProperty;
import com.jdt.fedlearn.coordinator.network.SendAndRecv;
import com.jdt.fedlearn.common.entity.core.ClientInfo;
import mockit.Mock;
import mockit.MockUp;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.Random;

public class ModelDeleteServiceImplTest {
    private static PartnerProperty C1;
    private static PartnerProperty C2;
    private static PartnerProperty C3;

    private static final Random random = new Random();

    @BeforeClass
    public void init() {
        initClientInfo();
        mockDeleteModel();
    }

    private void initClientInfo() {
        C1 = new PartnerProperty("", "http", "127.0.0.1", 80, 1, "train0.csv");
        C2 = new PartnerProperty("", "http", "127.0.0.1", 81, 2, "train1.csv");
        C3 = new PartnerProperty("", "http", "127.0.0.1", 82, 3, "train2.csv");

    }

    @Test
    public void testDeleteModel() {
        DeleteModelReq deleteModelReq = new DeleteModelReq();
        deleteModelReq.setModelToken("1-FederatedGB-5522228");
        mockSend(true);
        ModelDeleteServiceImpl modelDeleteService = new ModelDeleteServiceImpl();
        boolean b = modelDeleteService.deleteModel(deleteModelReq);
        Assert.assertTrue(b);
    }


    private static void mockDeleteModel() {
        new MockUp<TrainMapper>() {
            @Mock
            public boolean deleteModel(String modelId) {
                System.out.println("从数据库中删除模型");
                return true;
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