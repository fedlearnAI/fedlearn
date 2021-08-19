package com.jdt.fedlearn.coordinator.service.prepare;

import com.jdt.fedlearn.coordinator.entity.table.PartnerProperty;
import com.jdt.fedlearn.coordinator.util.ConfigUtil;
import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.entity.feature.SingleFeature;
import com.jdt.fedlearn.core.parameter.common.CategoryParameter;
import com.jdt.fedlearn.core.parameter.common.ParameterField;
import com.jdt.fedlearn.core.type.AlgorithmType;
import mockit.Mock;
import mockit.MockUp;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.testng.Assert.*;

public class AlgorithmParameterImplTest {

    private static PartnerProperty C1;
    private static PartnerProperty C2;
    private static PartnerProperty C3;
    private static final int TaskID = 0;

    private static final Random random = new Random();

    @BeforeClass
    public void init() {
        initClientInfo();
        mockConfigInit();
    }


    @Test
    public void testQueryAlgoParams() {
        AlgorithmParameterImpl algorithmParameterImpl = new AlgorithmParameterImpl();
        List<ParameterField> parameterFields = algorithmParameterImpl.queryAlgoParams(AlgorithmType.FederatedGB);
        Assert.assertEquals(parameterFields.size(), 23);
    }


    @Test
    public void testReadCrossValidation() {
        AlgorithmParameterImpl algorithmParameterImpl = new AlgorithmParameterImpl();
        CategoryParameter categoryParameter = algorithmParameterImpl.readCrossValidation();
        Assert.assertEquals(categoryParameter.getDefaultValue(), "1");
        Assert.assertEquals(categoryParameter.getName(), "交叉验证参数");

    }

    private void initClientInfo() {
        C1 = new PartnerProperty("C1", "http", "127.0.0.1", 80, 1, "train0.csv");
        C2 = new PartnerProperty("C2", "http", "127.0.0.1", 81, 2, "train1.csv");
        C3 = new PartnerProperty("C3", "http", "127.0.0.1", 82, 3, "train2.csv");

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