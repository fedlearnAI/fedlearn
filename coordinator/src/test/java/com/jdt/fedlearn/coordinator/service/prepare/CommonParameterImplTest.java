package com.jdt.fedlearn.coordinator.service.prepare;

import com.jdt.fedlearn.core.parameter.common.ParameterField;
import com.jdt.fedlearn.core.type.ParameterType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.*;

public class CommonParameterImplTest {

    @Test
    public void testGetCommonParams() {
        CommonParameterImpl commonParameterImpl = new CommonParameterImpl();
        List<ParameterField> parameterFields = commonParameterImpl.getCommonParams();
        Assert.assertEquals(parameterFields.size(), 1);
        Assert.assertEquals(parameterFields.get(0).getType(), ParameterType.STRING);
        Assert.assertEquals(parameterFields.get(0).getField(), "matchAlgorithm");
        System.out.println(parameterFields.get(0));

    }
}