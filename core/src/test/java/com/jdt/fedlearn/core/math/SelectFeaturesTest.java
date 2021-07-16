package com.jdt.fedlearn.core.math;

import com.jdt.fedlearn.core.entity.feature.Features;
import com.jdt.fedlearn.core.entity.feature.SingleFeature;
import com.jdt.fedlearn.core.loader.randomForest.DataFrame;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;

import static com.jdt.fedlearn.core.math.SelectFeatures.featuresRelation;
import static com.jdt.fedlearn.core.math.SelectFeatures.getPearson;

public class SelectFeaturesTest {

    @Test
    public void testGetPearson() {
        List<Double> list1 = new ArrayList<Double>(Arrays.asList(1.0,2.0,3.0));
        List<Double> list2 = new ArrayList<Double>(Arrays.asList(3.0,2.0,1.0));
        double res = getPearson(list1, list2);
        double target = -1.0;
        Assert.assertEquals(res,target);
    }

    @Test
    public void testFeaturesRelation() {
        List<SingleFeature> features0 = new ArrayList<>();
        features0.add(new SingleFeature("uid", "String"));
        features0.add(new SingleFeature("HouseAge", "String"));
        features0.add(new SingleFeature("Longitude", "String"));
        features0.add(new SingleFeature("label", "String"));
        Features features = new Features(features0, "label");
        String[] idMap = new String[]{"1", "100"};
        String[] x0 = new String[]{"uid", "HouseAge", "Longitude", "label"};
        String[] x1 = new String[]{"1", "0", "1", "1.0"};
        String[] x2 = new String[]{"100", "1", "0", "0.0"};
        String[][] input = new String[][]{x0, x1, x2};
        DataFrame trainData = new DataFrame(input,idMap,features);

        List<Map.Entry<Integer, Double>> selectedFeature = featuresRelation(trainData);
        Assert.assertEquals(selectedFeature.get(0).getKey(),new Integer(1));
        Assert.assertEquals(selectedFeature.get(0).getValue(),new Double(1.0));
        Assert.assertEquals(selectedFeature.get(1).getKey(),new Integer(0));
        Assert.assertEquals(selectedFeature.get(1).getValue(),new Double(-1.0));


    }

}