package com.jdt.fedlearn.coordinator.service.prepare;

import com.jdt.fedlearn.common.enums.RunningType;
import com.jdt.fedlearn.common.util.JsonUtil;
import com.jdt.fedlearn.coordinator.dao.db.MatchMapper;
import com.jdt.fedlearn.coordinator.entity.prepare.MatchQueryReq;
import com.jdt.fedlearn.coordinator.entity.table.MatchEntity;
import com.jdt.fedlearn.coordinator.entity.table.PartnerProperty;
import com.jdt.fedlearn.coordinator.util.ConfigUtil;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.entity.serialize.JavaSerializer;
import com.jdt.fedlearn.core.entity.serialize.Serializer;
import com.jdt.fedlearn.common.entity.uniqueId.MappingId;
import com.jdt.fedlearn.core.type.MappingType;
import mockit.Mock;
import mockit.MockUp;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MatchProgressImplTest {
    private static PartnerProperty C1;
    private static PartnerProperty C2;
    private static PartnerProperty C3;
    List<ClientInfo> clientInfos = new ArrayList<ClientInfo>();
    private static final Serializer serializer = new JavaSerializer();


    private void initClientInfo() {
        C1 = new PartnerProperty("", "http", "127.0.0.1", 80, 1, "train0.csv");
        C2 = new PartnerProperty("", "http", "127.0.0.1", 81, 2, "train1.csv");
        C3 = new PartnerProperty("", "http", "127.0.0.1", 82, 3, "train2.csv");
        clientInfos.add(C1.toClientInfo());
        clientInfos.add(C2.toClientInfo());
        clientInfos.add(C3.toClientInfo());
    }

    @BeforeClass
    public void init() {
        initClientInfo();
        mockConfigInit();
        mockisContainMatchModel();

    }

    @Test
    public void testQuery() throws ParseException {
        String projectId = "Test001";
        MappingType mappingType = MappingType.MD5;
        MappingId mappingId = new MappingId(projectId, mappingType);
        String matchToken = mappingId.getMappingId();
        MatchQueryReq q = new MatchQueryReq(matchToken);
        MatchProgressImpl matchProgressimpl = new MatchProgressImpl();
        Map<String, Object> resNull = matchProgressimpl.query(q);
        System.out.println(JsonUtil.object2json(resNull));
        Assert.assertEquals(resNull.get("describe"), "任务不存在");
        Assert.assertEquals(resNull.get("percent"), 0);

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

    private void mockisContainMatchModel() {
        new MockUp<MatchMapper>() {
            @Mock
            public boolean isContainMatchModel(String token) {
                return false;
            }

        };
    }
}