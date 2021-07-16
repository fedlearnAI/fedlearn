package com.jdt.fedlearn.core.research.mpc;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.*;

public class TestEasySharing {

    @Test
    public void testGenerate() {
        int n = 3;
        double secretNum = 100.0;
        double[] res = EasySharing.generate(n,secretNum);
        System.out.println("res: "+ Arrays.toString(res));
        double[] doubles = new double[]{7.305990027427673E7, 6.384376453399658E7, -1.3690356480827332E8};
        Assert.assertEquals(res,doubles);
    }

    @Test
    public void testInterpolate() {
        double[][] points = new double[][]{{0,1},{0,9}};
        double res = EasySharing.interpolate(points);
        Assert.assertEquals(res,10);

    }

    @Test
    public void testSplit() {
        double[] secretNums = new double[]{100,20,92};
        int n =3;
        Map<Integer, double[]> res = EasySharing.split(secretNums,n);
        System.out.println(res);
        Map<Integer,double[]> map = new HashMap<>();
        double[] double1 = new double[]{7.305990027427673E7, 7.305990027427673E7, 7.305990027427673E7};
        double[] double2 = new double[]{6.384376453399658E7, 6.384376453399658E7, 6.384376453399658E7};
        double[] double3 = new double[]{-1.3690356480827332E8, -1.3690364480827332E8, -1.3690357280827332E8};
        map.put(1,double1);
        map.put(2,double2);
        map.put(3,double3);
        Assert.assertEquals(res.get(0),map.get(0));
        Assert.assertEquals(res.get(1),map.get(1));
        Assert.assertEquals(res.get(2),map.get(2));
    }

    @Test
    public void testJoin() {
        Map<Integer,double[]> map = new HashMap<>();
        double[] double1 = new double[]{7.305990027427673E7, 7.305990027427673E7, 7.305990027427673E7};
        double[] double2 = new double[]{6.384376453399658E7, 6.384376453399658E7, 6.384376453399658E7};
        double[] double3 = new double[]{-1.3690356480827332E8, -1.3690364480827332E8, -1.3690357280827332E8};
        map.put(1,double1);
        map.put(2,double2);
        map.put(3,double3);
        double[] res = EasySharing.join(map);
        System.out.println(Arrays.toString(res));
        double[] secretNums = new double[]{100,20,92};
        Assert.assertEquals(res,secretNums);
    }
}