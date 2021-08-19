package com.jdt.fedlearn.coordinator.allocation;

import com.jdt.fedlearn.common.entity.project.MatchPartnerInfo;
import com.jdt.fedlearn.coordinator.entity.prepare.MatchStartReq;
import com.jdt.fedlearn.coordinator.service.prepare.MatchStartImpl;
import com.jdt.fedlearn.common.entity.uniqueId.MappingId;
import com.jdt.fedlearn.core.entity.ClientInfo;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.core.type.MappingType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiMatchTest {

    @Test
    public void testSingleMatch() throws ParseException {
        String projectId = "Test001";
        MappingType mappingType = MappingType.MD5;
        MappingId mappingId = new MappingId(projectId, mappingType);
        String matchId = mappingId.getMappingId();
        MatchStartReq matchStartReq = new MatchStartReq("123",mappingType.getType());
        MultiMatch multiMatch = new MultiMatch(matchId, matchStartReq);

        MatchPartnerInfo clientInfo = new MatchPartnerInfo("http://127.0.0.1:8080","train0.csv", "uid");
        List<MatchPartnerInfo> partnerInfos = new ArrayList<>();
        partnerInfos.add(clientInfo);
//        List<String> datasetMap = new ArrayList<>();
//        datasetMap.add("train0.csv");
        MatchResult res = multiMatch.singleMatch(matchId,partnerInfos);
        Assert.assertEquals(res.getLength(), 3);
        Assert.assertEquals(res.getReport(), "IdMatch is complete!! \n Match num is 10");
    }

    // todo mock
}