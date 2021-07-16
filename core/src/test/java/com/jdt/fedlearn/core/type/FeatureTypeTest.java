package com.jdt.fedlearn.core.type;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class FeatureTypeTest {
    private final FeatureType featureType1 =  FeatureType.String;
    private final FeatureType featureType2 =  FeatureType.Float;
    private final FeatureType featureType3 =  FeatureType.Bool;
    private final FeatureType featureType4 =  FeatureType.Int;
    @Test
    public void testGetFeatureType() {
        String res1  = featureType1.getFeatureType();
        String target1 = "String";
        assertEquals(res1,target1);

        String res2  = featureType2.getFeatureType();
        String target2 = "Float";
        assertEquals(res2,target2);

        String res3  = featureType3.getFeatureType();
        String target3 = "Bool";
        assertEquals(res3,target3);

        String res4  = featureType4.getFeatureType();
        String target4 = "Int";
        assertEquals(res4,target4);
    }
}