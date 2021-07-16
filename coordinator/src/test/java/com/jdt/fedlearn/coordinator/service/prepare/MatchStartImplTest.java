package com.jdt.fedlearn.coordinator.service.prepare;

import com.jdt.fedlearn.common.util.JsonUtil;
import com.jdt.fedlearn.coordinator.dao.UniversalMapper;
import com.jdt.fedlearn.coordinator.dao.db.MatchMapper;
import com.jdt.fedlearn.coordinator.entity.prepare.MatchStartReq;
import com.jdt.fedlearn.coordinator.entity.table.PartnerProperty;
import com.jdt.fedlearn.coordinator.network.SendAndRecv;
import com.jdt.fedlearn.coordinator.util.ConfigUtil;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.Message;
import com.jdt.fedlearn.core.entity.psi.MatchInitRes;
import com.jdt.fedlearn.core.entity.serialize.JavaSerializer;
import com.jdt.fedlearn.core.entity.serialize.Serializer;
import mockit.Mock;
import mockit.MockUp;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MatchStartImplTest {

    private static PartnerProperty C1;
    private static PartnerProperty C2;
    private static PartnerProperty C3;
    private static final Serializer serializer = new JavaSerializer();


    private void initClientInfo() {
        C1 = new PartnerProperty("", "http", "127.0.0.1", 80, 1, "train0.csv");
        C2 = new PartnerProperty("", "http", "127.0.0.1", 81, 2, "train1.csv");
        C3 = new PartnerProperty("", "http", "127.0.0.1", 82, 3, "train2.csv");

    }

    @BeforeClass
    public void init() {
        initClientInfo();
        mockGetClientInfoFromDb(C1, C2, C3);
        mockPostClientInfo();
        mockisContainMatchModel();
        mockConfigInit();
    }



    @Test
    public void testMatch() {
        String projectId = "1";
        MatchStartImpl matchStartImpl = new MatchStartImpl();
        MatchStartReq query = new MatchStartReq("lijingxi", projectId, "VERTICAL_MD5");
        String content = JsonUtil.object2json(query);
        MatchStartReq query2 = new MatchStartReq(content);
        Map<String, Object> res = matchStartImpl.match(query2);
        String res2 = (String) res.get("matchToken");
        Assert.assertEquals(res2.split("-").length, 3);
        Assert.assertEquals(res2.split("-")[0], projectId);
    }

    /**
     * mock客户端信息
     * @param c1 第一个客户端
     * @param c2 第二个客户端
     * @param c3 第三个客户端
     */
    private static void mockGetClientInfoFromDb(PartnerProperty c1, PartnerProperty c2, PartnerProperty c3) {
        List<PartnerProperty> clientInfos = new ArrayList<>();
        clientInfos.add(c1);
        clientInfos.add(c2);
        clientInfos.add(c3);
        // 将UniversalMapper传入MockUp类
        new MockUp<UniversalMapper>() {
            @Mock
            public List<PartnerProperty> read(String taskId) {
                return clientInfos;
            }
        };
    }

    /**
     * mock send方法
     */
    private void mockPostClientInfo() {
        new MockUp<SendAndRecv>() {
            @Mock
            public String send(ClientInfo client, String matchToken, String dataset, int phase, String matchAlgorithm, Message body) {
                // 模拟三个客户端，每个客户端对齐的id相同
                if (phase == 0) {
                    Message m1 = new MatchInitRes(C1.toClientInfo(), new String[]{"1B", "2A", "3A", "4A", "5C", "6C", "7C", "8B", "9B", "10B"});
                    return serializer.serialize(m1);
                } else {
                    return "error";
                }
            }
        };
    }

    private void mockisContainMatchModel() {
        new MockUp<MatchMapper>() {
            @Mock
            public boolean isContainMatchModel(String token) {
                return false;
            }

            @Mock
            public String isContainMatch(String tasKId, String matchType) {
                return "1-MD5-399494";
            }
        };
    }

    private void mockConfigInit() {
        new MockUp<ConfigUtil>() {
            @Mock
            public boolean getSplitTag() {
                return true;
            }

            @Mock
            public boolean getZipProperties() {
                return true;
            }

            @Mock
            public boolean getJdChainAvailable() {
                return false;
            }
        };
    }




}