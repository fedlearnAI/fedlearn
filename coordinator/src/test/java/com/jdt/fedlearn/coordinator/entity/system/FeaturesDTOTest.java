package com.jdt.fedlearn.coordinator.entity.system;

import org.testng.Assert;
import org.testng.annotations.Test;

public class FeaturesDTOTest {
    @Test
    public void testAll() {
        FeaturesDTO featuresDto = new FeaturesDTO();
        featuresDto.setDtype("String");
        featuresDto.setName("name");
        Assert.assertEquals(featuresDto.getDtype(), "String");
        Assert.assertEquals(featuresDto.getName(), "name");
    }

}