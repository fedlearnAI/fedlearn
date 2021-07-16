package com.jdt.fedlearn.coordinator.entity.system;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;

public class FeaturesDatasetDTOTest {
    @Test
    public void testAll() {
        FeaturesDatasetDTO featuresDatasetDto = new FeaturesDatasetDTO();
        featuresDatasetDto.setDataset("a.csv");
        featuresDatasetDto.setFeatures(new ArrayList<>());
        Assert.assertEquals(featuresDatasetDto.getDataset(), "a.csv");
        Assert.assertEquals(featuresDatasetDto.getFeatures().size(), 0);
    }

}