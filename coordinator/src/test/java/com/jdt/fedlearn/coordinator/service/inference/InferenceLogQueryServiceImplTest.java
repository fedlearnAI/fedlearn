package com.jdt.fedlearn.coordinator.service.inference;

import com.jdt.fedlearn.coordinator.dao.db.InferenceLogMapper;
import com.jdt.fedlearn.coordinator.entity.inference.InferenceDto;
import com.jdt.fedlearn.coordinator.entity.inference.InferenceResp;
import com.jdt.fedlearn.coordinator.entity.table.InferenceEntity;
import mockit.Mock;
import mockit.MockUp;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import scala.collection.generic.BitOperations;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.testng.Assert.*;

public class InferenceLogQueryServiceImplTest {

    @BeforeClass
    public void init() {
        mockGetInferenceList();
        mockGetInferenceCount();
    }

    @Test
    public void testQueryInferenceLog() {
        InferenceDto inferenceDto = new InferenceDto();
        InferenceLogQueryServiceImpl inferenceLogQueryServiceImpl = new InferenceLogQueryServiceImpl();
        InferenceResp inferenceResp = inferenceLogQueryServiceImpl.queryInferenceLog(inferenceDto);
        Assert.assertEquals(inferenceResp.getInferenceCount(), (Integer)1);
        Assert.assertEquals(inferenceResp.getInferenceList().size(), 1);

    }

    public void mockGetInferenceList() {
        new MockUp<InferenceLogMapper>() {
            @Mock
            public List<InferenceEntity> getInferenceList(InferenceDto inferenceDto) {
                List<InferenceEntity> inferenceEntityList = new ArrayList<>();
                InferenceEntity inferenceEntity = new InferenceEntity("username", "1-FederatedGB", "Inf1", new Date(4000000), new Date(4500000), "inference result", 1, 1);
                inferenceEntityList.add(inferenceEntity);
                return inferenceEntityList;
            }
        };
    }

    public void mockGetInferenceCount() {
        new MockUp<InferenceLogMapper>() {
            @Mock
            public Integer getInferenceCount(InferenceDto inferenceDto) {
                return 1;
            }
        };
    }
}