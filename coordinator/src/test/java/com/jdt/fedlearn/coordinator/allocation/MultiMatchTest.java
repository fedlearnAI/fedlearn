package com.jdt.fedlearn.coordinator.allocation;

import com.jdt.fedlearn.coordinator.service.prepare.MatchStartImpl;
import com.jdt.fedlearn.common.entity.uniqueId.MappingId;
import com.jdt.fedlearn.core.psi.MatchResult;
import com.jdt.fedlearn.core.type.MappingType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.text.ParseException;

public class MultiMatchTest {

    @Test
    public void testSingleMatch() throws ParseException {
        String projectId = "Test001";
        MappingType mappingType = MappingType.VERTICAL_MD5;
        MappingId mappingId = new MappingId(projectId, mappingType);
        String matchToken = mappingId.getMappingId();
        MultiMatch multiMatch = new MultiMatch(matchToken, "nlp");
        MatchStartImpl.ID_MATCH_FLAG.put(matchToken, 10);
        MatchResult res = multiMatch.singleMatch(matchToken);
        Assert.assertEquals(res.getLength(), 3);
        Assert.assertEquals(res.getMappingReport().getReport(), "IdMatch is complete!! \n Match num is 10");
    }

    // todo mock
}