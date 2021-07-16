package com.jdt.fedlearn.core.entity.boost;

import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class TestBucket {

    double[] ids = {1.1,2.1,3.1,4.1};
    double[] values = {2.1,3.1,4.1,5.1};
    Bucket bucket1 = new Bucket(ids,values);
    double[][] mat = {{1.1,2.3},{5.6,6.6},{2,4.5}};
    Bucket bucket2 = new Bucket(mat);
    @Test
    public void testTestToString() {
        String res1 = bucket1.toString();
        System.out.println(res1);
        assertEquals(res1,"Bucket{ids=[1.1, 2.1, 3.1, 4.1], values=[2.1, 3.1, 4.1, 5.1], splitValue=5.1}");
        String res2 = bucket2.toString();
        System.out.println(res2);
        assertEquals(res2,"Bucket{ids=[1.1, 5.6, 2.0], values=[2.3, 6.6, 4.5], splitValue=6.6}");
    }


    @Test
    public void jsonConstruct(){
        Bucket bucket = new Bucket(new double[]{1.0}, new double[]{2.0});

        Assert.assertEquals(bucket.getIds(), new double[]{1.0});
        Assert.assertEquals(bucket.getValues(), new double[]{2.0});
    }

}
